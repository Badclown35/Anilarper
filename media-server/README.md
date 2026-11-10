# Anilarper Home Media Server

A small MP4 library server for Android/Termux. It uses Python's standard library and stores uploaded files in a `media/` folder beside `server.py`.

## Start on the REVVL 8

1. Install Termux from its official F-Droid or GitHub distribution.
2. Run `pkg update && pkg install python curl`.
3. Run `mkdir -p ~/anilarper-media && cd ~/anilarper-media`.
4. Download the current `media-server/server.py` from this repository into this folder.
5. Run `python -m py_compile server.py` to check syntax.
6. Run `python server.py`. The first start creates `.admin_token`; keep it private.
7. Find the REVVL's Wi-Fi IP in Android Wi-Fi details. From another device on the same Wi-Fi, open `http://PHONE_IP:8765`.
8. Enter the admin token, choose MP4 files, set a display title and category, then upload.

Keep the Termux session running while the server is in use. Android battery optimization, Wi-Fi sleep, and the phone being offline can interrupt service.

## Organizing videos

- Upload multiple MP4 files in one session.
- Set a display title and category before upload.
- Search the library and filter by category.
- Use **Organize** beside a video to change its title or category later.
- Use **Delete** to remove a video from the server. Deletion cannot be undone.
- Original filenames are retained as video IDs; display titles/categories are metadata stored in `video_metadata.json`.
- Existing filenames are not overwritten. Rename the source file before uploading if you need a different stored filename.

## API

- `GET /api/health` — health check.
- `GET /api/videos` — read-only catalog with title, category, size, and stream path.
- `GET` or `HEAD /media/FILENAME.mp4` — MP4 stream with byte-range support for seeking.
- `POST /api/upload?name=FILENAME.mp4&title=TITLE&category=CATEGORY` — raw MP4 body with `Authorization: Bearer ADMIN_TOKEN`.
- `POST /api/videos/FILENAME.mp4/metadata` — update title/category with the admin token and a JSON body.
- `DELETE /api/videos/FILENAME.mp4` — protected deletion with the admin token.

Uploads are limited to 5 GiB per file. Metadata requests are limited to 4 KiB.

## Security and reliability

This is a LAN prototype. It uses plain HTTP, and the video catalog/stream endpoints are readable by anyone who can reach the server on that network. The admin token protects upload, organization, and deletion only.

**Do not port-forward this server or expose its plain HTTP listener directly to the public internet.** Before friends/family can use it remotely, add a secure HTTPS tunnel and a suitable access-control layer (for example, an invite-only private network or authenticated edge access). The Android app intentionally accepts HTTPS server addresses only.

Home upload speed, router/ISP restrictions, Android battery management, and ISP terms can limit streaming. Only distribute media you own or have permission to share.