#!/usr/bin/env python3
"""Anilarper phone media server. Standard-library only; private-by-default."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit, unquote, parse_qs
import hashlib, hmac, json, os, re, secrets, shutil, tempfile, threading, time

DATA = Path(os.environ.get("ANILARPER_DATA_DIR", str(Path.home() / "anilarper-storage"))).expanduser()
MEDIA = Path(os.environ.get("ANILARPER_MEDIA_DIR", str(DATA / "media"))).expanduser()
DATA.mkdir(parents=True, exist_ok=True)
MEDIA.mkdir(parents=True, exist_ok=True)
TOKEN_FILE = DATA / ".admin_token"
META_FILE = DATA / "video_metadata.json"
MAX_UPLOAD = 5 * 1024 * 1024 * 1024
CHUNK = 1024 * 1024
RESERVE_BYTES = 512 * 1024 * 1024
PORT = int(os.environ.get("ANILARPER_PORT", "8765"))
HOST = os.environ.get("ANILARPER_HOST", "127.0.0.1")
LOCK = threading.RLock()

if not TOKEN_FILE.exists():
    fd = os.open(str(TOKEN_FILE), os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as out:
        out.write(secrets.token_urlsafe(48))
try:
    TOKEN_FILE.chmod(0o600)
except OSError:
    pass
TOKEN = TOKEN_FILE.read_text(encoding="utf-8").strip()
if len(TOKEN) < 32:
    raise RuntimeError("Invalid admin token. Back up and replace the .admin_token file.")

def read_metadata():
    try:
        value = json.loads(META_FILE.read_text(encoding="utf-8"))
        return value if isinstance(value, dict) else {}
    except (OSError, json.JSONDecodeError):
        return {}

def write_metadata(value):
    fd, temp_name = tempfile.mkstemp(prefix=".metadata-", suffix=".tmp", dir=str(DATA))
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as out:
            json.dump(value, out, ensure_ascii=False, indent=2)
            out.flush()
            os.fsync(out.fileno())
        os.replace(temp_name, META_FILE)
    finally:
        try:
            os.unlink(temp_name)
        except FileNotFoundError:
            pass

def clean_text(value, limit=100):
    return re.sub(r"\s+", " ", str(value or "")).strip()[:limit]

def safe_name(value):
    raw = unquote(str(value or ""))
    if "/" in raw or "\\" in raw or raw in (".", ".."):
        raise ValueError("Invalid filename.")
    name = re.sub(r"[^A-Za-z0-9._ -]", "_", raw).strip(" .")
    if not name or not name.lower().endswith(".mp4"):
        raise ValueError("Only .mp4 files are accepted.")
    return name[:180]

def media_file(name):
    candidate = MEDIA / name
    if candidate.parent != MEDIA or candidate.is_symlink() or not candidate.is_file():
        return None
    return candidate

def storage_stats():
    total = used = pending = 0
    for entry in MEDIA.iterdir():
        try:
            if entry.is_symlink() or not entry.is_file():
                continue
            size = entry.stat().st_size
            if entry.name.startswith(".upload-"):
                pending += size
            elif entry.suffix.lower() == ".mp4":
                used += size
        except OSError:
            continue
    free = shutil.disk_usage(str(MEDIA)).free
    usable = max(0, free - RESERVE_BYTES)
    return {"usedBytes": used, "pendingBytes": pending,
            "availableBytes": usable, "maxBytes": used + pending + usable,
            "reserveBytes": RESERVE_BYTES}

def video_record(path, meta):
    st = path.stat()
    item = meta.get(path.name, {})
    if not isinstance(item, dict):
        item = {}
    return {"id": path.name, "title": clean_text(item.get("title"), 120) or path.stem,
            "category": clean_text(item.get("category"), 40) or "Uncategorized",
            "size": st.st_size, "modified": int(st.st_mtime),
            "streamPath": "/media/" + path.name,
            "visibility": "public" if item.get("visibility") == "public" else "private"}

class Handler(BaseHTTPRequestHandler):
    server_version = "AnilarperPhone/1.0"

    def log_message(self, fmt, *args):
        # Avoid logging authorization headers or request query strings.
        super().log_message("%s", args[0] if args else "")

    def send_json(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def authorized(self):
        supplied = self.headers.get("Authorization", "")
        return hmac.compare_digest(supplied, "Bearer " + TOKEN)

    def require_admin(self):
        if self.authorized():
            return True
        self.send_json(401, {"error": "Unauthorized"})
        return False

    def catalog(self, admin=False):
        videos = []
        meta = read_metadata()
        for path in sorted(MEDIA.glob("*.mp4"), key=lambda p: p.name.lower()):
            try:
                if path.is_symlink() or not path.is_file():
                    continue
                item = video_record(path, meta)
                if admin or item["visibility"] == "public":
                    videos.append(item)
            except OSError:
                continue
        return videos

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET,HEAD,POST,DELETE,OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Authorization,Content-Type,Range,If-Range")
        self.send_header("Access-Control-Max-Age", "600")
        self.end_headers()

    def do_GET(self):
        path = urlsplit(self.path).path
        if path == "/":
            page = PAGE.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(page)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(page)
            return
        if path == "/api/health":
            self.send_json(200, {"ok": True, "server": "AnilarperPhone", "version": 2,
                                 "storage": storage_stats()})
            return
        if path == "/api/videos":
            videos = [v for v in self.catalog(False)]
            self.send_json(200, {"videos": videos, "storage": storage_stats()})
            return
        if path == "/api/admin/videos":
            if not self.require_admin():
                return
            self.send_json(200, {"videos": self.catalog(True), "storage": storage_stats()})
            return
        if path.startswith("/media/"):
            self.stream_public(path[len("/media/"):])
            return
        self.send_error(404)

    def do_HEAD(self):
        path = urlsplit(self.path).path
        if path.startswith("/media/"):
            self.stream_public(path[len("/media/"):], True)
            return
        self.send_error(404)

    def stream_public(self, raw_name, head_only=False):
        try:
            name = safe_name(raw_name)
        except ValueError:
            self.send_error(404)
            return
        with LOCK:
            meta = read_metadata()
            item = meta.get(name, {})
            if not isinstance(item, dict) or item.get("visibility") != "public":
                self.send_error(404)
                return
            target = media_file(name)
        if target is None:
            self.send_error(404)
            return
        self.stream_file(target, head_only)

    def stream_file(self, target, head_only=False):
        try:
            size = target.stat().st_size
            start, end, status = 0, size - 1, 200
            rh = self.headers.get("Range")
            if rh:
                match = re.fullmatch(r"bytes=(\d*)-(\d*)", rh.strip())
                if not match or (not match.group(1) and not match.group(2)):
                    self.send_error(416)
                    return
                a, b = match.groups()
                if not a:
                    suffix = int(b)
                    if suffix <= 0:
                        self.send_error(416)
                        return
                    start = max(0, size - suffix)
                else:
                    start = int(a)
                    if b:
                        end = min(int(b), size - 1)
                if start >= size or end < start:
                    self.send_response(416)
                    self.send_header("Content-Range", "bytes */" + str(size))
                    self.send_header("Content-Length", "0")
                    self.end_headers()
                    return
                status = 206
            length = max(0, end - start + 1)
            self.send_response(status)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Content-Length", str(length))
            self.send_header("Cache-Control", "public, max-age=300")
            if status == 206:
                self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, size))
            self.end_headers()
            if head_only:
                return
            with target.open("rb") as src:
                src.seek(start)
                remaining = length
                while remaining:
                    block = src.read(min(CHUNK, remaining))
                    if not block:
                        break
                    self.wfile.write(block)
                    remaining -= len(block)
        except (BrokenPipeError, ConnectionResetError):
            pass
        except OSError:
            if not self.wfile.closed:
                self.send_error(404)

    def read_body_json(self, max_bytes=4096):
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            length = 0
        if length <= 0 or length > max_bytes:
            raise ValueError("Request body must be between 1 and %d bytes." % max_bytes)
        payload = json.loads(self.rfile.read(length).decode("utf-8"))
        if not isinstance(payload, dict):
            raise ValueError("Expected a JSON object.")
        return payload

    def do_POST(self):
        if not self.require_admin():
            return
        parsed = urlsplit(self.path)
        if parsed.path.startswith("/api/videos/") and parsed.path.endswith("/metadata"):
            raw = parsed.path[len("/api/videos/"):-len("/metadata")].strip("/")
            try:
                name = safe_name(raw)
                payload = self.read_body_json()
                with LOCK:
                    if media_file(name) is None:
                        self.send_error(404)
                        return
                    metadata = read_metadata()
                    old = metadata.get(name, {})
                    if not isinstance(old, dict):
                        old = {}
                    visibility = payload.get("visibility", old.get("visibility", "private"))
                    if visibility not in ("private", "public"):
                        raise ValueError("Visibility must be private or public.")
                    metadata[name] = {
                        "title": clean_text(payload.get("title", old.get("title")), 120) or Path(name).stem,
                        "category": clean_text(payload.get("category", old.get("category")), 40) or "Uncategorized",
                        "visibility": visibility
                    }
                    write_metadata(metadata)
                self.send_json(200, {"ok": True, "id": name, **metadata[name]})
            except (ValueError, UnicodeDecodeError, json.JSONDecodeError) as exc:
                self.send_json(400, {"error": "Invalid metadata: " + str(exc)[:120]})
            return
        if parsed.path != "/api/upload":
            self.send_error(404)
            return
        try:
            query = parse_qs(parsed.query)
            name = safe_name(query.get("name", [""])[0])
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_UPLOAD:
                self.send_json(413, {"error": "Missing size or file exceeds 5 GiB limit."})
                return
            stats = storage_stats()
            if length > max(0, stats["availableBytes"]):
                self.send_json(507, {"error": "Not enough free storage while preserving the system safety reserve."})
                return
            dest = MEDIA / name
            if dest.exists() or dest.is_symlink():
                self.send_json(409, {"error": "File already exists."})
                return
            temp = MEDIA / (".upload-" + secrets.token_hex(12) + ".part")
            remaining = length
            try:
                with temp.open("xb") as out:
                    while remaining:
                        block = self.rfile.read(min(CHUNK, remaining))
                        if not block:
                            raise ConnectionError("Upload ended early.")
                        out.write(block)
                        remaining -= len(block)
                    out.flush()
                    os.fsync(out.fileno())
                with LOCK:
                    if dest.exists() or dest.is_symlink():
                        raise FileExistsError("File already exists.")
                    metadata = read_metadata()
                    metadata[name] = {
                        "title": clean_text(query.get("title", [Path(name).stem])[0], 120) or Path(name).stem,
                        "category": clean_text(query.get("category", ["Uncategorized"])[0], 40) or "Uncategorized",
                        "visibility": "private"
                    }
                    # Commit a private metadata record before the file becomes visible.
                    write_metadata(metadata)
                    temp.replace(dest)
                    if query.get("publish", ["false"])[0].lower() == "true":
                        metadata[name]["visibility"] = "public"
                        write_metadata(metadata)
            except Exception:
                temp.unlink(missing_ok=True)
                raise
            self.send_json(201, {"ok": True, "name": name, "size": length,
                                 "visibility": metadata[name]["visibility"]})
        except (ValueError, OSError, ConnectionError) as exc:
            self.send_json(400, {"error": "Upload failed: " + str(exc)[:120]})

    def do_DELETE(self):
        if not self.require_admin():
            return
        path = urlsplit(self.path).path
        if not path.startswith("/api/videos/"):
            self.send_error(404)
            return
        try:
            name = safe_name(path[len("/api/videos/"):])
        except ValueError:
            self.send_error(400)
            return
        with LOCK:
            target = media_file(name)
            if target is None:
                self.send_error(404)
                return
            target.unlink()
            metadata = read_metadata()
            metadata.pop(name, None)
            write_metadata(metadata)
        self.send_json(200, {"ok": True, "deleted": name})

PAGE = r"""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Anilarper Phone Storage</title><style>
body{font:16px system-ui;max-width:760px;margin:24px auto;padding:0 16px;background:#101318;color:#f2f4f8}
input,button,select{font:inherit;padding:11px;border-radius:9px;border:1px solid #555;background:#202630;color:inherit}
input[type=file]{width:100%;box-sizing:border-box;margin:12px 0}button{cursor:pointer;background:#315bd6;border:0}
.card{background:#1b222c;padding:16px;border-radius:12px;margin:12px 0}small{color:#aab4c2}
li{margin:14px 0;overflow-wrap:anywhere}progress{width:100%;height:18px}.private{color:#ffc078}.public{color:#83ddb0}
</style></head><body><h1>Anilarper Phone Storage</h1><p><small>Private by default • only published videos are visible to the community</small></p>
<div class="card"><label>Admin token<br><input id="token" type="password" autocomplete="off" style="width:100%;box-sizing:border-box" placeholder="Paste token from the private token file"></label>
<p><label>Display title (optional)<br><input id="title" style="width:100%;box-sizing:border-box"></label></p>
<p><label>Category<br><input id="category" value="Uncategorized" style="width:100%;box-sizing:border-box"></label></p>
<label><input id="publish" type="checkbox"> Publish after upload (visible to everyone)</label>
<input id="files" type="file" accept="video/mp4,.mp4" multiple><button onclick="upload()">Upload selected MP4s</button>
<p id="status"></p><progress id="progress" value="0" max="100" hidden></progress></div>
<div class="card"><h2>Manage phone videos</h2><p id="storage"></p><input id="search" placeholder="Search videos…" oninput="render()" style="width:100%;box-sizing:border-box"><p><button onclick="load()">Refresh</button></p><ul id="list"></ul></div>
<script>
const token=document.getElementById('token'),statusEl=document.getElementById('status'),bar=document.getElementById('progress');let videos=[];
function headers(){return {'Authorization':'Bearer '+token.value}}
async function load(){if(!token.value){statusEl.textContent='Enter your admin token to load private files.';return}
let r=await fetch('/api/admin/videos',{headers:headers()});if(!r.ok){statusEl.textContent='Could not load files ('+r.status+').';return}
let j=await r.json();videos=j.videos||[];let s=j.storage||{};document.getElementById('storage').textContent='Media: '+(s.usedBytes/1e9).toFixed(2)+' GB • Pending: '+(s.pendingBytes/1e9).toFixed(2)+' GB • Available now: '+(s.availableBytes/1e9).toFixed(2)+' GB';render()}
function render(){let ul=document.getElementById('list');ul.replaceChildren();let q=document.getElementById('search').value.toLowerCase();
for(let v of videos){if(q&&!((v.title+' '+v.id+' '+v.category).toLowerCase().includes(q)))continue;
let li=document.createElement('li'),label=document.createElement('span');label.textContent=v.title+' • '+v.category+' • '+(v.size/1048576).toFixed(1)+' MB • '+v.visibility.toUpperCase();label.className=v.visibility;li.append(label);
let toggle=document.createElement('button');toggle.textContent=v.visibility==='public'?'Unpublish':'Publish';toggle.style.marginLeft='8px';toggle.onclick=()=>update(v,{visibility:v.visibility==='public'?'private':'public'});
let edit=document.createElement('button');edit.textContent='Edit';edit.style.marginLeft='6px';edit.onclick=()=>{let title=prompt('Title:',v.title);if(title===null)return;let category=prompt('Category:',v.category);if(category===null)return;update(v,{title,category})};
let del=document.createElement('button');del.textContent='Delete';del.style.marginLeft='6px';del.onclick=async()=>{if(!confirm('Permanently delete '+v.title+'?'))return;let rr=await fetch('/api/videos/'+encodeURIComponent(v.id),{method:'DELETE',headers:headers()});statusEl.textContent=rr.ok?'Deleted.':'Delete failed.';if(rr.ok)load()};
li.append(document.createTextNode(' '),toggle,edit,del);ul.append(li)}
if(!videos.length)ul.textContent='No videos uploaded yet.'}
async function update(v,patch){let r=await fetch('/api/videos/'+encodeURIComponent(v.id)+'/metadata',{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({...v,...patch})});statusEl.textContent=r.ok?'Saved.':'Update failed.';if(r.ok)load()}
async function upload(){let fs=[...document.getElementById('files').files];if(!token.value||!fs.length){statusEl.textContent='Enter token and select MP4 files.';return}
for(let f of fs){statusEl.textContent='Uploading '+f.name+'…';bar.hidden=false;bar.value=0;
await new Promise(resolve=>{let x=new XMLHttpRequest(),title=document.getElementById('title').value.trim(),category=document.getElementById('category').value.trim()||'Uncategorized',publish=document.getElementById('publish').checked;
x.open('POST','/api/upload?name='+encodeURIComponent(f.name)+'&title='+encodeURIComponent(title||f.name.replace(/\.mp4$/i,''))+'&category='+encodeURIComponent(category)+'&publish='+publish);x.setRequestHeader('Authorization','Bearer '+token.value);x.upload.onprogress=e=>{if(e.lengthComputable)bar.value=e.loaded/e.total*100};x.onload=()=>{statusEl.textContent=x.status===201?'Uploaded '+f.name+' ('+JSON.parse(x.responseText).visibility+')':x.responseText;resolve()};x.onerror=()=>{statusEl.textContent='Network error: '+f.name;resolve()};x.send(f)})}
document.getElementById('files').value='';await load()}
</script></body></html>"""

if __name__ == "__main__":
    print("Anilarper phone media server starting")
    print("Listening on 127.0.0.1:%d (local tunnel access only)" % PORT if HOST == "127.0.0.1" else "Listening on %s:%d" % (HOST, PORT))
    print("Admin token file: %s (token is not printed)" % TOKEN_FILE)
    print("Media folder: %s" % MEDIA)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
