#!/usr/bin/env python3
"""Anilarper home media server. Python standard library only; LAN prototype."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit, unquote
import hashlib, hmac, json, os, re, secrets

ROOT = Path(__file__).resolve().parent
MEDIA = ROOT / "media"
MEDIA.mkdir(exist_ok=True)
TOKEN_FILE = ROOT / ".admin_token"
MAX_UPLOAD = 5 * 1024 * 1024 * 1024
CHUNK = 1024 * 1024
PORT = int(os.environ.get("ANILARPER_PORT", "8765"))

if not TOKEN_FILE.exists():
    TOKEN_FILE.write_text(secrets.token_urlsafe(32), encoding="utf-8")
    try: TOKEN_FILE.chmod(0o600)
    except OSError: pass
TOKEN = TOKEN_FILE.read_text(encoding="utf-8").strip()
if len(TOKEN) < 32: raise RuntimeError("Invalid admin token. Remove .admin_token and restart.")

def safe_name(value):
    name = Path(unquote(value or "")).name
    name = re.sub(r"[^A-Za-z0-9._ -]", "_", name).strip(" .")
    if not name or not name.lower().endswith(".mp4"): raise ValueError("Only .mp4 files are accepted.")
    return name[:180]

class Handler(BaseHTTPRequestHandler):
    server_version = "AnilarperMedia/0.1"
    def send_json(self, status, payload):
        body = json.dumps(payload).encode()
        self.send_response(status); self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body))); self.send_header("Cache-Control", "no-store")
        self.end_headers(); self.wfile.write(body)
    def authorized(self):
        return hmac.compare_digest(self.headers.get("Authorization", ""), "Bearer " + TOKEN)
    def do_GET(self):
        path = urlsplit(self.path).path
        if path == "/":
            page = PAGE.encode()
            self.send_response(200); self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(page))); self.send_header("Cache-Control", "no-store")
            self.end_headers(); self.wfile.write(page); return
        if path == "/api/health":
            self.send_json(200, {"ok": True, "server": "AnilarperMedia", "version": 1}); return
        if path == "/api/videos":
            videos = []
            for f in sorted(MEDIA.glob("*.mp4"), key=lambda p: p.name.lower()):
                if f.is_file():
                    st = f.stat()
                    videos.append({"id": f.name, "title": f.stem, "size": st.st_size,
                                   "modified": int(st.st_mtime), "streamPath": "/media/" + f.name})
            self.send_json(200, {"videos": videos}); return
        if path.startswith("/media/"):
            name = unquote(path[len("/media/"):])
            if not name or Path(name).name != name: self.send_error(404); return
            self.stream_file(MEDIA / name); return
        self.send_error(404)
    def do_HEAD(self):
        path = urlsplit(self.path).path
        if path.startswith("/media/"):
            name = unquote(path[len("/media/"):])
            if name and Path(name).name == name: self.stream_file(MEDIA / name, True); return
        self.send_error(404)
    def stream_file(self, file_path, head_only=False):
        if not file_path.is_file() or file_path.suffix.lower() != ".mp4": self.send_error(404); return
        size = file_path.stat().st_size
        start, end, status = 0, size - 1, 200
        rh = self.headers.get("Range")
        if rh:
            m = re.fullmatch(r"bytes=(\d*)-(\d*)", rh.strip())
            if not m or (not m.group(1) and not m.group(2)): self.send_error(416); return
            a, b = m.groups()
            if not a: start = max(0, size - int(b))
            else:
                start = int(a)
                if b: end = min(int(b), size - 1)
            if start >= size or end < start:
                self.send_response(416); self.send_header("Content-Range", f"bytes */{size}")
                self.send_header("Content-Length", "0"); self.end_headers(); return
            status = 206
        length = end - start + 1
        self.send_response(status); self.send_header("Content-Type", "video/mp4")
        self.send_header("Accept-Ranges", "bytes"); self.send_header("Content-Length", str(length))
        self.send_header("Cache-Control", "public, max-age=3600")
        if status == 206: self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.end_headers()
        if head_only: return
        try:
            with file_path.open("rb") as src:
                src.seek(start); remaining = length
                while remaining:
                    block = src.read(min(CHUNK, remaining))
                    if not block: break
                    self.wfile.write(block); remaining -= len(block)
        except (BrokenPipeError, ConnectionResetError): pass
    def do_POST(self):
        parsed = urlsplit(self.path)
        if parsed.path != "/api/upload": self.send_error(404); return
        if not self.authorized(): self.send_json(401, {"error": "Unauthorized"}); return
        from urllib.parse import parse_qs
        try: name = safe_name(parse_qs(parsed.query).get("name", [""])[0])
        except ValueError as exc: self.send_json(400, {"error": str(exc)}); return
        try: length = int(self.headers.get("Content-Length", "0"))
        except ValueError: length = 0
        if length <= 0 or length > MAX_UPLOAD:
            self.send_json(413, {"error": "Missing size or file exceeds 5 GiB limit."}); return
        dest = MEDIA / name
        if dest.exists(): self.send_json(409, {"error": "File already exists."}); return
        temp = MEDIA / (".upload-" + secrets.token_hex(8) + ".part")
        remaining = length
        try:
            with temp.open("xb") as out:
                while remaining:
                    block = self.rfile.read(min(CHUNK, remaining))
                    if not block: raise ConnectionError("Upload ended early.")
                    out.write(block); remaining -= len(block)
            temp.replace(dest)
        except Exception as exc:
            temp.unlink(missing_ok=True)
            self.send_json(400, {"error": "Upload failed: " + str(exc)[:120]}); return
        self.send_json(201, {"ok": True, "name": name, "size": length})
    def do_DELETE(self):
        if not self.authorized(): self.send_json(401, {"error": "Unauthorized"}); return
        path = urlsplit(self.path).path
        if not path.startswith("/api/videos/"): self.send_error(404); return
        name = unquote(path[len("/api/videos/"):])
        if not name or Path(name).name != name or not name.lower().endswith(".mp4"):
            self.send_error(400); return
        target = MEDIA / name
        if not target.is_file(): self.send_error(404); return
        target.unlink(); self.send_json(200, {"ok": True, "deleted": name})

PAGE = r"""<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Anilarper Media Server</title><style>
body{font:16px system-ui;max-width:720px;margin:28px auto;padding:0 16px;background:#101318;color:#f2f4f8}
input,button{font:inherit;padding:12px;border-radius:9px;border:1px solid #555;background:#202630;color:inherit}
input[type=file]{width:100%;box-sizing:border-box;margin:12px 0}button{cursor:pointer;background:#315bd6;border:0}
.card{background:#1b222c;padding:16px;border-radius:12px;margin:12px 0}small{color:#aab4c2}
li{margin:12px 0;overflow-wrap:anywhere}progress{width:100%;height:18px}</style></head><body>
<h1>Anilarper Media</h1><p><small>Home server • MP4 files only</small></p>
<div class="card"><label>Admin token<br><input id="token" type="password" autocomplete="off" style="width:100%;box-sizing:border-box" placeholder="Paste token from Termux"></label>
<input id="files" type="file" accept="video/mp4,.mp4" multiple><button onclick="upload()">Upload selected MP4s</button>
<p id="status"></p><progress id="progress" value="0" max="100" hidden></progress></div>
<div class="card"><h2>Shared videos</h2><button onclick="load()">Refresh list</button><ul id="list"></ul></div>
<script>
const token=document.getElementById('token'), statusEl=document.getElementById('status'), bar=document.getElementById('progress');
async function load(){let r=await fetch('/api/videos');let j=await r.json();let ul=document.getElementById('list');ul.replaceChildren();
for(let v of j.videos){let li=document.createElement('li'),a=document.createElement('a'),del=document.createElement('button');a.href=v.streamPath;a.textContent=v.title;a.style.color='#9bb8ff';a.target='_blank';del.textContent='Delete';del.style.marginLeft='10px';del.onclick=async()=>{if(!token.value){statusEl.textContent='Enter admin token to delete videos.';return}if(!confirm('Delete "'+v.title+'" from this server?'))return;del.disabled=true;let r=await fetch('/api/videos/'+encodeURIComponent(v.id),{method:'DELETE',headers:{'Authorization':'Bearer '+token.value}});statusEl.textContent=r.ok?'Deleted '+v.title:'Delete failed: '+await r.text();if(r.ok)await load();else del.disabled=false};li.append(a,document.createTextNode(' • '+(v.size/1048576).toFixed(1)+' MB '),del);ul.append(li)}
if(!j.videos.length)ul.textContent='No videos uploaded yet.'}
async function upload(){let fs=[...document.getElementById('files').files];if(!token.value||!fs.length){statusEl.textContent='Enter token and select MP4 files.';return}
for(let f of fs){statusEl.textContent='Uploading '+f.name+'…';bar.hidden=false;bar.value=0;
await new Promise(resolve=>{let x=new XMLHttpRequest();x.open('POST','/api/upload?name='+encodeURIComponent(f.name));x.setRequestHeader('Authorization','Bearer '+token.value);
x.upload.onprogress=e=>{if(e.lengthComputable)bar.value=e.loaded/e.total*100};
x.onload=()=>{statusEl.textContent=x.status===201?'Uploaded '+f.name:x.responseText;resolve()};
x.onerror=()=>{statusEl.textContent='Network error: '+f.name;resolve()};x.send(f)})}
document.getElementById('files').value='';await load()}load();
</script></body></html>"""

if __name__ == "__main__":
    print("Anilarper media server starting")
    print(f"LAN address: http://PHONE_IP:{PORT}")
    print(f"Admin token: {TOKEN}")
    print(f"Media folder: {MEDIA}")
    print("LAN prototype only. Do not expose plain HTTP directly to the public internet.")
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
