# Anilarper Home Media Server

A zero-subscription MP4 server for Android/Termux. Python standard library only.

## Start on the REVVL 8
1. Install Termux from its official F-Droid or GitHub distribution.
2. Run: pkg update
3. Run: pkg install python
4. Run: mkdir -p ~/anilarper-media && cd ~/anilarper-media
5. Download media-server/server.py from this repository into this folder.
6. Run: python server.py
7. The first start creates .admin_token and prints it in Termux. Keep it private.
8. Find the REVVL's Wi-Fi IP in Android Wi-Fi details. On another device on the same Wi-Fi, open http://PHONE_IP:8765.
9. Paste the token and upload a small MP4 to test.

## API
- GET /api/health — health check
- GET /api/videos — read-only video catalog
- GET /media/FILENAME.mp4 — MP4 stream with HTTP byte-range seeking
- POST /api/upload?name=FILENAME.mp4 — raw MP4 body with Authorization: Bearer TOKEN
- DELETE /api/videos/FILENAME.mp4 — protected deletion

Uploads are limited to 5 GiB per file. Existing names are not overwritten.

## Security and reliability
This first milestone is for testing on your own Wi-Fi only. It uses plain HTTP: do not port-forward it or expose it directly to the public internet. Before public access, add HTTPS, stronger access controls, request limits, and a secure remote-access route. Home upload speed, router/ISP restrictions, Android battery management, and ISP terms can limit service. Only distribute media you have permission to share.
