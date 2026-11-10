import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

class MemoryBucket {
  constructor() { this.objects = new Map(); this.sessions = new Map(); }
  async get(key, options = {}) {
    const value = this.objects.get(key);
    if (!value) return null;
    const full = value.bytes;
    let bytes = full;
    if (options.range) {
      const start = options.range.offset || 0;
      bytes = full.slice(start, start + options.range.length);
    }
    return {
      key, size: full.length, httpEtag: '"memory-etag"',
      body: new Blob([bytes]).stream(),
      async json() { return JSON.parse(new TextDecoder().decode(full)); }
    };
  }
  async put(key, value, options = {}) {
    let bytes;
    if (typeof value === "string") bytes = new TextEncoder().encode(value);
    else if (value instanceof ArrayBuffer) bytes = new Uint8Array(value);
    else if (ArrayBuffer.isView(value)) bytes = new Uint8Array(value);
    else bytes = new Uint8Array(await new Response(value).arrayBuffer());
    this.objects.set(key, { bytes, options });
    return { key, size: bytes.length, httpEtag: '"memory-etag"' };
  }
  async delete(key) { this.objects.delete(key); }
  async createMultipartUpload(key, options) {
    const uploadId = "upload-" + Math.random().toString(16).slice(2);
    this.sessions.set(uploadId, { key, options, parts: new Map() });
    return this.multipart(key, uploadId);
  }
  resumeMultipartUpload(key, uploadId) {
    const session = this.sessions.get(uploadId);
    if (!session || session.key !== key) throw new Error("No such upload");
    return this.multipart(key, uploadId);
  }
  multipart(key, uploadId) {
    const bucket = this;
    return {
      async uploadPart(number, stream) {
        const session = bucket.sessions.get(uploadId);
        const bytes = new Uint8Array(await new Response(stream).arrayBuffer());
        const etag = '"part-' + number + '"';
        session.parts.set(number, bytes);
        return { partNumber: number, etag };
      },
      async complete(parts) {
        const session = bucket.sessions.get(uploadId);
        const chunks = parts.map(p => {
          const chunk = session.parts.get(p.partNumber);
          if (!chunk) throw new Error("Missing part " + p.partNumber);
          return chunk;
        });
        const bytes = new Uint8Array(await new Blob(chunks).arrayBuffer());
        bucket.objects.set(key, { bytes, options: session.options });
        bucket.sessions.delete(uploadId);
        return { key, size: bytes.length, httpEtag: '"memory-etag"' };
      },
      async abort() { bucket.sessions.delete(uploadId); }
    };
  }
}
function request(path, options = {}) {
  return new Request("https://anilarper.test" + path, options);
}
function env() {
  return { MEDIA_BUCKET: new MemoryBucket(), ADMIN_TOKEN: "test-admin-token",
    ASSETS: { async fetch() { return new Response("admin"); } } };
}
const adminHeaders = { Authorization: "Bearer test-admin-token" };

test("health and public catalog are available without admin credentials", async () => {
  const e = env();
  const health = await worker.fetch(request("/api/health"), e);
  assert.equal(health.status, 200);
  assert.equal((await health.json()).ok, true);
  const catalog = await worker.fetch(request("/api/videos"), e);
  assert.deepEqual((await catalog.json()).videos, []);
});

test("uploads require admin auth and public media supports byte ranges", async () => {
  const e = env();
  const denied = await worker.fetch(request("/api/upload/start", {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ filename: "clip.mp4", size: 3 })
  }), e);
  assert.equal(denied.status, 401);

  const started = await worker.fetch(request("/api/upload/start", {
    method: "POST", headers: { ...adminHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ filename: "clip.mp4", title: "Test Clip", category: "Clips", size: 3 })
  }), e);
  assert.equal(started.status, 201);
  const session = await started.json();
  const catalogAfterStart = await (await e.MEDIA_BUCKET.get("_anilarper/catalog.json")).json();

  const part = await worker.fetch(request("/api/upload/part?id=" + encodeURIComponent(session.id) +
    "&uploadId=" + encodeURIComponent(session.uploadId) + "&partNumber=1", {
      method: "PUT", headers: { ...adminHeaders, "Content-Length": "3" }, body: new Uint8Array([65, 66, 67])
    }), e);
  assert.equal(part.status, 200, await part.clone().text() + " session=" + JSON.stringify(session) + " pending=" + JSON.stringify(catalogAfterStart.pending));
  const partData = await part.json();

  const completed = await worker.fetch(request("/api/upload/complete", {
    method: "POST", headers: { ...adminHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ id: session.id, uploadId: session.uploadId, parts: [partData] })
  }), e);
  assert.equal(completed.status, 201);

  const catalog = await worker.fetch(request("/api/videos"), e);
  const video = (await catalog.json()).videos[0];
  assert.equal(video.title, "Test Clip");
  assert.equal(video.size, 3);

  const range = await worker.fetch(request(video.streamPath, { headers: { Range: "bytes=1-2" } }), e);
  assert.equal(range.status, 206);
  assert.equal(range.headers.get("Content-Range"), "bytes 1-2/3");
  assert.equal(await range.text(), "BC");
});

test("storage cap rejects new uploads and admin can edit/delete", async () => {
  const e = env();
  await e.MEDIA_BUCKET.put("_anilarper/catalog.json", JSON.stringify({
    videos: [{ id: "old.mp4", size: 10_000_000_000, title: "Old", category: "Anime" }], pending: []
  }));
  const full = await worker.fetch(request("/api/upload/start", {
    method: "POST", headers: { ...adminHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ filename: "next.mp4", size: 1 })
  }), e);
  assert.equal(full.status, 413);

  await e.MEDIA_BUCKET.put("media/sample.mp4", new Uint8Array([1, 2, 3]));
  await e.MEDIA_BUCKET.put("_anilarper/catalog.json", JSON.stringify({
    videos: [{ id: "sample.mp4", size: 3, title: "Before", category: "Anime" }], pending: []
  }));
  const edit = await worker.fetch(request("/api/videos/sample.mp4/metadata", {
    method: "POST", headers: { ...adminHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ title: "After", category: "Clips" })
  }), e);
  assert.equal(edit.status, 200);
  const del = await worker.fetch(request("/api/videos/sample.mp4", { method: "DELETE", headers: adminHeaders }), e);
  assert.equal(del.status, 200);
  assert.equal((await (await worker.fetch(request("/api/videos"), e)).json()).videos.length, 0);
});
