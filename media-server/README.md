# Anilarper Termux LAN Media Server (legacy option)

This Python standard-library server stores MP4 files in a local `media/` folder. It remains useful for testing or same-Wi-Fi sharing. It is **not** the public remote-hosting solution.

## Start on the REVVL 8

1. Install Termux from its official distribution.
2. Run `pkg update && pkg install python curl`.
3. Run `mkdir -p ~/anilarper-media && cd ~/anilarper-media`.
4. Download `media-server/server.py` from this repository into this folder.
5. Run `python -m py_compile server.py`.
6. Run `python server.py`. The first start creates `.admin_token`; keep it private.
7. From another device on the same Wi-Fi, open `http://PHONE_IP:8765`.
8. Enter the admin token to upload and manage MP4 files.

Keep the Termux session running while using the server. Android battery management, Wi-Fi sleep, and the phone being offline can interrupt service.

## API

- `GET /api/health` — health check.
- `GET /api/videos` — public catalog on the local network.
- `GET/HEAD /media/FILENAME.mp4` — MP4 stream with byte-range support.
- `POST /api/upload` — upload with `Authorization: Bearer ADMIN_TOKEN`.
- `POST /api/videos/FILENAME.mp4/metadata` — update title/category with the admin token.
- `DELETE /api/videos/FILENAME.mp4` — protected deletion.

## Security

This is a LAN prototype. The catalog and streams are readable by anyone who can reach the server on the local network. The admin token protects write operations only.

**Never port-forward this server or expose its plain HTTP listener to the public internet.** For public internet streaming, use the separate [Cloudflare R2 + Worker setup](../cloudflare-media/README.md), which provides HTTPS public reads and token-protected administration.

Only distribute media you own or have permission to share.
