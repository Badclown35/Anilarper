const MAX_STORAGE_BYTES = 10_000_000_000;
const MAX_FILE_BYTES = 5 * 1024 * 1024 * 1024;
const MAX_PART_BYTES = 10 * 1024 * 1024;
const CATALOG_KEY = "_anilarper/catalog.json";
const MEDIA_PREFIX = "media/";
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,HEAD,POST,PUT,DELETE,OPTIONS",
  "Access-Control-Allow-Headers": "Authorization,Content-Type,Range,If-Range",
  "Access-Control-Expose-Headers": "Accept-Ranges,Content-Length,Content-Range,ETag",
  "Vary": "Origin"
};
function respond(body, status, headers) {
  const h = new Headers(headers || {});
  for (const k in cors) h.set(k, cors[k]);
  return new Response(body, { status: status || 200, headers: h });
}
function json(data, status) {
  return respond(JSON.stringify(data), status || 200, {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store"
  });
}
function cleanText(value, max) {
  return String(value || "").replace(/\s+/g, " ").trim().slice(0, max || 120);
}
function safeFilename(value) {
  const name = String(value || "").split(/[\\/]/).pop()
    .replace(/[^A-Za-z0-9._ -]/g, "_").trim().replace(/^[. ]+|[. ]+$/g, "");
  if (!name || !/\.mp4$/i.test(name)) throw new Error("Only MP4 files are accepted.");
  return name.slice(0, 150);
}
function isAdmin(request, env) {
  const supplied = request.headers.get("Authorization") || "";
  const expected = "Bearer " + (env.ADMIN_TOKEN || "");
  if (!env.ADMIN_TOKEN || supplied.length !== expected.length) return false;
  let diff = 0;
  for (let i = 0; i < supplied.length; i++) diff |= supplied.charCodeAt(i) ^ expected.charCodeAt(i);
  return diff === 0;
}
async function readCatalog(bucket) {
  const obj = await bucket.get(CATALOG_KEY);
  if (!obj) return { videos: [], pending: [] };
  try {
    const data = await obj.json();
    return { videos: Array.isArray(data.videos) ? data.videos : [], pending: (Array.isArray(data.pending) ? data.pending : []).filter(function(p) { return Number(p.created || 0) > Date.now() - 8 * 24 * 60 * 60 * 1000; }) };
  } catch (_) {
    throw new Error("Media catalog is damaged. Stop uploads and restore a backup.");
  }
}
async function writeCatalog(bucket, catalog) {
  await bucket.put(CATALOG_KEY, JSON.stringify(catalog), {
    httpMetadata: { contentType: "application/json; charset=utf-8", cacheControl: "no-store" }
  });
}
function publicVideo(v) {
  return { id: v.id, title: v.title, category: v.category, size: v.size, modified: v.modified, streamPath: "/media/" + v.id };
}
async function streamMedia(request, env, id) {
  const catalog = await readCatalog(env.MEDIA_BUCKET);
  const video = catalog.videos.find(function(v) { return v.id === id; });
  if (!video) return json({ error: "Video not found." }, 404);
  const rh = request.headers.get("Range");
  let range, start = 0, end = video.size - 1, status = 200;
  if (rh) {
    const m = /^bytes=(\d*)-(\d*)$/.exec(rh.trim());
    if (!m || (!m[1] && !m[2])) return respond(null, 416, { "Content-Range": "bytes */" + video.size, "Accept-Ranges": "bytes" });
    if (!m[1]) {
      const suffix = Number(m[2]);
      if (!Number.isSafeInteger(suffix) || suffix <= 0) return respond(null, 416, { "Content-Range": "bytes */" + video.size });
      start = Math.max(0, video.size - suffix);
    } else {
      start = Number(m[1]);
      if (m[2]) end = Math.min(Number(m[2]), video.size - 1);
    }
    if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start >= video.size || end < start) {
      return respond(null, 416, { "Content-Range": "bytes */" + video.size, "Accept-Ranges": "bytes" });
    }
    status = 206;
    range = { offset: start, length: end - start + 1 };
  }
  const object = await env.MEDIA_BUCKET.get(MEDIA_PREFIX + id, range ? { range: range } : {});
  if (!object) return json({ error: "Video file is missing." }, 404);
  const headers = {
    "Content-Type": "video/mp4", "Accept-Ranges": "bytes",
    "Content-Length": String(range ? range.length : object.size),
    "Cache-Control": "public, max-age=86400", "ETag": object.httpEtag,
    "X-Content-Type-Options": "nosniff"
  };
  if (status === 206) headers["Content-Range"] = "bytes " + start + "-" + end + "/" + video.size;
  if (request.method === "HEAD") return respond(null, status, headers);
  return respond(object.body, status, headers);
}
async function handleApi(request, env, url) {
  const path = url.pathname;
  if (request.method === "GET" && path === "/api/health") {
    return json({ ok: true, service: "Anilarper R2 Media", version: 1, maxStorageBytes: MAX_STORAGE_BYTES });
  }
  if (request.method === "GET" && path === "/api/videos") {
    const catalog = await readCatalog(env.MEDIA_BUCKET);
    const usedBytes = catalog.videos.reduce(function(sum, v) { return sum + Number(v.size || 0); }, 0);
    const pendingBytes = catalog.pending.reduce(function(sum, p) { return sum + Number(p.size || 0); }, 0);
    return json({ videos: catalog.videos.map(publicVideo), storage: { usedBytes: usedBytes, pendingBytes: pendingBytes, maxBytes: MAX_STORAGE_BYTES } });
  }
  if (path.startsWith("/media/") && (request.method === "GET" || request.method === "HEAD")) {
    const id = decodeURIComponent(path.slice(7));
    if (!/^[A-Za-z0-9-]+_[A-Za-z0-9._ -]+\.mp4$/i.test(id)) return json({ error: "Invalid video ID." }, 404);
    return streamMedia(request, env, id);
  }
  if (!isAdmin(request, env)) return json({ error: "Admin authorization required." }, 401);

  if (request.method === "POST" && path === "/api/upload/start") {
    const body = await request.json();
    const size = Number(body.size);
    if (!Number.isSafeInteger(size) || size <= 0 || size > MAX_FILE_BYTES) return json({ error: "File size must be between 1 byte and 5 GiB." }, 400);
    let filename;
    try { filename = safeFilename(body.filename); } catch (e) { return json({ error: e.message }, 400); }
    const catalog = await readCatalog(env.MEDIA_BUCKET);
    const used = catalog.videos.reduce(function(sum, v) { return sum + Number(v.size || 0); }, 0);
    const reserved = catalog.pending.reduce(function(sum, p) { return sum + Number(p.size || 0); }, 0);
    if (used + reserved + size > MAX_STORAGE_BYTES) {
      return json({ error: "Storage limit reached. Completed and pending uploads use " + ((used + reserved) / 1e9).toFixed(2) + " GB of 10 GB." }, 413);
    }
    const id = crypto.randomUUID() + "_" + filename;
    const key = MEDIA_PREFIX + id;
    const title = cleanText(body.title, 120) || filename.replace(/\.mp4$/i, "");
    const category = cleanText(body.category, 40) || "Uncategorized";
    const upload = await env.MEDIA_BUCKET.createMultipartUpload(key, {
      httpMetadata: { contentType: "video/mp4", cacheControl: "public, max-age=86400" },
      customMetadata: { title: title, category: category }
    });
    catalog.pending.push({ id: id, key: key, uploadId: upload.uploadId, size: size, title: title, category: category, created: Date.now() });
    await writeCatalog(env.MEDIA_BUCKET, catalog);
    return json({ id: id, uploadId: upload.uploadId, partSize: 8 * 1024 * 1024 }, 201);
  }

  if (request.method === "PUT" && path === "/api/upload/part") {
    const id = url.searchParams.get("id") || "";
    const uploadId = url.searchParams.get("uploadId") || "";
    const partNumber = Number(url.searchParams.get("partNumber"));
    const catalog = await readCatalog(env.MEDIA_BUCKET);
    const pending = catalog.pending.find(function(p) { return p.id === id && p.uploadId === uploadId; });
    if (!pending) return json({ error: "Upload session not found." }, 404);
    const length = Number(request.headers.get("Content-Length") || 0);
    if (!Number.isSafeInteger(partNumber) || partNumber < 1 || partNumber > 10000 || !length || length > MAX_PART_BYTES || length > pending.size) {
      return json({ error: "Invalid part number or part size." }, 400);
    }
    const upload = env.MEDIA_BUCKET.resumeMultipartUpload(pending.key, pending.uploadId);
    const part = await upload.uploadPart(partNumber, request.body);
    return json({ partNumber: part.partNumber, etag: part.etag });
  }

  if (request.method === "POST" && path === "/api/upload/complete") {
    const body = await request.json();
    const catalog = await readCatalog(env.MEDIA_BUCKET);
    const pending = catalog.pending.find(function(p) { return p.id === body.id && p.uploadId === body.uploadId; });
    if (!pending) return json({ error: "Upload session not found." }, 404);
    if (!Array.isArray(body.parts) || !body.parts.length || body.parts.length > 10000) return json({ error: "Missing multipart parts." }, 400);
    const parts = body.parts.map(function(p) { return { partNumber: Number(p.partNumber), etag: String(p.etag || "") }; });
    if (parts.some(function(p, i) { return !Number.isInteger(p.partNumber) || p.partNumber !== i + 1 || !p.etag; })) {
      return json({ error: "Multipart parts must be complete and ordered." }, 400);
    }
    const upload = env.MEDIA_BUCKET.resumeMultipartUpload(pending.key, pending.uploadId);
    const object = await upload.complete(parts);
    catalog.pending = catalog.pending.filter(function(p) { return p.id !== pending.id; });
    catalog.videos.push({ id: pending.id, title: pending.title, category: pending.category, size: object.size, modified: Date.now() });
    await writeCatalog(env.MEDIA_BUCKET, catalog);
    return json({ ok: true, video: publicVideo(catalog.videos[catalog.videos.length - 1]) }, 201);
  }

  if (request.method === "POST" && path === "/api/upload/abort") {
    const body = await request.json();
    const catalog = await readCatalog(env.MEDIA_BUCKET);
    const index = catalog.pending.findIndex(function(p) { return p.id === body.id && p.uploadId === body.uploadId; });
    if (index < 0) return json({ error: "Upload session not found." }, 404);
    const pending = catalog.pending[index];
    await env.MEDIA_BUCKET.resumeMultipartUpload(pending.key, pending.uploadId).abort();
    catalog.pending.splice(index, 1);
    await writeCatalog(env.MEDIA_BUCKET, catalog);
    return json({ ok: true });
  }

  const metadataMatch = /^\/api\/videos\/([^/]+)\/metadata$/.exec(path);
  if (request.method === "POST" && metadataMatch) {
    const id = decodeURIComponent(metadataMatch[1]);
    const catalog = await readCatalog(env.MEDIA_BUCKET);
    const video = catalog.videos.find(function(v) { return v.id === id; });
    if (!video) return json({ error: "Video not found." }, 404);
    const body = await request.json();
    video.title = cleanText(body.title, 120) || video.title;
    video.category = cleanText(body.category, 40) || "Uncategorized";
    await writeCatalog(env.MEDIA_BUCKET, catalog);
    return json({ ok: true, id: id, title: video.title, category: video.category });
  }

  if (request.method === "DELETE" && path.startsWith("/api/videos/")) {
    const id = decodeURIComponent(path.slice("/api/videos/".length));
    const catalog = await readCatalog(env.MEDIA_BUCKET);
    const index = catalog.videos.findIndex(function(v) { return v.id === id; });
    if (index < 0) return json({ error: "Video not found." }, 404);
    await env.MEDIA_BUCKET.delete(MEDIA_PREFIX + id);
    catalog.videos.splice(index, 1);
    await writeCatalog(env.MEDIA_BUCKET, catalog);
    return json({ ok: true, deleted: id });
  }
  return json({ error: "Not found." }, 404);
}
export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return respond(null, 204);
    const url = new URL(request.url);
    try {
      if (url.pathname === "/" && request.method === "GET") {
        return respond("Anilarper public media API. Open /admin to manage uploads.", 200, { "Content-Type": "text/plain; charset=utf-8" });
      }
      if (url.pathname === "/admin" && request.method === "GET") {
        return env.ASSETS.fetch(new Request(new URL("/admin.html", request.url), { method: "GET" }));
      }
      return await handleApi(request, env, url);
    } catch (error) {
      return json({ error: String(error && error.message || error).slice(0, 240) }, 500);
    }
  }
};
