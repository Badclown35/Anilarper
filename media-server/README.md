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

## Publish phone videos to the public Community library

The local server is still not a public streaming host. To make a phone-stored video visible to all Anilarper users, open the phone server page on the backup phone and use **Publish to Community** beside that video.

1. Enter the phone server's local admin token.
2. Enter the Cloudflare Worker admin token in the password field on the page. Do not paste it into chat or save it in screenshots.
3. Confirm **Publish to Community** for the selected video.
4. The server copies the video to the public Worker using authenticated multipart HTTPS uploads. The original phone copy remains unchanged.
5. Refresh Community Videos in Anilarper; the published copy is now served by the public Worker.

The Worker admin token is sent only for that publish operation and is not written to the phone server's files. The destination is restricted to HTTPS `workers.dev` addresses. Publishing consumes the Worker bucket's existing 10 GB quota; local phone free space is not added to the public cloud quota.

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
