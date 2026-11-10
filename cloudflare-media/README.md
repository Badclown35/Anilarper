# Anilarper public media (free-tier setup)

This replaces the always-on Termux video host with Cloudflare R2 + a Cloudflare Worker. The Worker URL is a free `workers.dev` HTTPS address; no custom domain, tunnel, or paid hosting plan is required. R2 Standard's free allowance is 10 GB-month storage, 1 million Class A operations, 10 million Class B operations, and free internet egress. Workers Free includes 100,000 requests/day. These are provider quotas, not unlimited service; exceeding included quotas can stop requests or incur charges depending on account billing configuration. Keep the bucket at or below the app's strict 10,000,000,000-byte cap.

## Public access and security

- `GET /api/videos` and `GET /media/{id}` are public. Anyone with the Worker URL can browse and stream the files.
- Upload, edit, delete, and multipart upload endpoints require the secret `ADMIN_TOKEN` as a Bearer token.
- Never put the admin token in the app, Git repository, screenshots, public links, or messages. Enter it only in the private admin page at `/admin` over HTTPS.
- Only upload MP4s that you own or have permission to distribute. Public files can be copied or shared by anyone.
- R2's free allowance is monthly storage, not a 10 GB monthly download cap. Egress is free, but Worker request quotas still apply.

## Deploy (one-time)

Use an account managed by a parent/guardian if Cloudflare account eligibility or payment setup requires an adult. Do not add a paid plan or enable paid add-ons.

1. Install Node.js on a computer or supported shell, then install Wrangler: `npm install -g wrangler`.
2. Sign in: `wrangler login`.
3. Create the bucket: `wrangler r2 bucket create anilarper-public-media`.
4. From this folder, set a long random admin secret: `wrangler secret put ADMIN_TOKEN`. Keep it private and do not commit it.
5. Deploy: `wrangler deploy`.
6. Wrangler prints a URL like `https://anilarper-media-api.<your-subdomain>.workers.dev`. Open that URL plus `/admin` to upload/manage videos. Enter the secret token there.
7. In Anilarper Community Videos, enter the Worker URL (without `/admin`) and tap Connect.

The free `workers.dev` hostname avoids domain purchase costs. R2's rate-limited `r2.dev` public bucket URL is not used; the Worker streams R2 objects and supports HTTP byte ranges for seeking.

## API

- `GET /api/health` — health and storage cap.
- `GET /api/videos` — public catalog in the format Anilarper already reads.
- `GET/HEAD /media/{id}` — public MP4 stream with byte-range support.
- `POST /api/upload/start`, `PUT /api/upload/part`, `POST /api/upload/complete`, `POST /api/upload/abort` — authenticated multipart upload.
- `POST /api/videos/{id}/metadata` — authenticated title/category edit.
- `DELETE /api/videos/{id}` — authenticated deletion.

Uploads use 8 MiB parts to stay below Workers Free request-body limits. Individual videos are capped at 5 GiB and completed plus pending uploads at 10,000,000,000 bytes. Incomplete multipart uploads are tracked in the catalog; R2 automatically expires abandoned multipart uploads after seven days. If the browser closes unexpectedly, an abandoned catalog reservation may need cleanup; the admin page attempts to abort uploads after ordinary upload errors.

## Free-tier limits

- R2 Standard: 10 GB-month storage, 1 million Class A operations/month, 10 million Class B operations/month; egress is free.
- Workers Free: 100,000 requests/day and 10 ms CPU time per invocation. Video players can make multiple range requests, so high public traffic can exhaust the daily request quota.
- No custom domain is configured.
- Configure usage notifications if available and do not upgrade to a paid plan. If Cloudflare requires payment verification, have a parent/guardian handle it; never enter payment information without them.

## Local checks

Run `node --check src/index.js`. Cloudflare runtime behavior, account activation, and actual playback still need testing after deployment. This repository cannot create a Cloudflare account, bucket, secret, or deployment on the user's behalf.
