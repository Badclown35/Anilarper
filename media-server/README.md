# Anilarper phone storage server

This Termux server adds the backup phone as a second media provider. It is designed to sit behind a Cloudflare Tunnel; it binds to **127.0.0.1** by default, so it is not exposed directly to the Wi-Fi or public internet.

## Storage and privacy model

- Media is stored outside the Git checkout in `~/anilarper-storage/media` by default.
- The admin token is created in `~/anilarper-storage/.admin_token`, with owner-only file permissions. The server never prints the token.
- New uploads are **private by default**. Only an upload explicitly marked for publishing, or later switched to Public in the admin page, appears in `GET /api/videos`.
- Private files are omitted from the public catalog and cannot be streamed, even if someone guesses their filename.
- All admin routes require `Authorization: Bearer ADMIN_TOKEN`.
- The server keeps a 512 MiB free-space reserve and rejects uploads that would consume it. Available capacity is recalculated from the phone's filesystem; it is not a guaranteed fixed quota.
- Only MP4 files are accepted. Maximum individual upload size is 5 GiB.

## Run on the backup phone

In Termux:

```sh
pkg update
pkg install python cloudflared
mkdir -p ~/anilarper-storage/media
cd ~/anilarper-storage/anilarper-repo
python -m py_compile media-server/server.py
ANILARPER_DATA_DIR="$HOME/anilarper-storage" ANILARPER_MEDIA_DIR="$HOME/anilarper-storage/media" python media-server/server.py
```

Keep this Termux session open. In another Termux session, a temporary Cloudflare test tunnel can point to the local-only server:

```sh
cloudflared tunnel --url http://127.0.0.1:8765
```

Cloudflare prints a temporary `trycloudflare.com` URL. Do not paste the admin token into chat. The temporary URL can change after restarting the tunnel. Do not run the tunnel until the server security and Worker integration are reviewed and the user is ready to test.

Open the tunnel URL in a browser to use the private admin page. Enter the token locally from the token file. Do not share the admin URL or token.

## API

- `GET /api/health` — health and live storage capacity.
- `GET /api/videos` — public-only catalog and storage stats.
- `GET/HEAD /media/FILENAME.mp4` — published MP4 stream with byte-range support.
- `GET /api/admin/videos` — all files; admin token required.
- `POST /api/upload?name=...&title=...&category=...&publish=false` — MP4 upload; admin token required. Publishing must be explicitly set to true.
- `POST /api/videos/FILENAME.mp4/metadata` — update title/category/visibility; admin token required.
- `DELETE /api/videos/FILENAME.mp4` — protected deletion.

## Android background reliability

Use Android's app battery settings to allow Termux unrestricted battery use, keep the phone powered, and keep the server session running. A wake lock can help, but Android may still stop background work or the phone may lose internet. Phone storage is therefore an additional provider, not a replacement for the existing Backblaze B2 storage.

## Safety

Never port-forward this server or bind it to `0.0.0.0` for public hosting. Use the Cloudflare Tunnel to the loopback listener only. Keep the admin token private and only distribute media you own or have permission to share.
