# Anilarper

A personal Android anime, manga, light-novel, music, and media companion built with Kotlin and Jetpack Compose.

## Android app

- Home dashboard, anime discovery, local library, watch history, statistics, profile, quests, and custom lists.
- AniList GraphQL search with pagination, season/year browsing, genre/format/status filters, title details, and franchise relationships.
- Local anime tracking with episode progress and watch statuses.
- Manga and light-novel discovery with chapter and volume tracking.
- MyAnimeList XML and CSV import with AniList ID matching and progress/status/score transfer.
- Local soundtrack library, listening-link bookmarks, and Media3 background playback controls (device playback still requires validation).
- Local video picker/player and Community Videos catalog with HTTPS server connections, search, category filters, and MP4 streaming.
- Room-backed collections and catalog cache, plus JSON backup and restore.
- Dark UI with customizable profile and accent colors.

## Build

- Android package: `app.anilarper.personal`
- Minimum Android: API 26
- Compile/target SDK: 35
- Java: 17
- Android build: GitHub Actions → **Build Anilarper APK**. Download the `Anilarper-debug` artifact from a successful run.
- The repository currently relies on the runner's installed Gradle; a Gradle wrapper is not checked in.

## Data and privacy

- Anime and reading collections are stored locally in Room. AniList catalog metadata and cover images are fetched from AniList.
- Music links open their official apps/sites; Anilarper does not download or re-host those streams.
- Local audio/video files remain in their original locations unless the user separately copies them.
- Community Videos supports a public Cloudflare R2 + Worker catalog. Public viewers can browse and stream; the admin token is required for uploads, organization, and deletion.
- The cloud media setup targets the free R2 Standard allowance (10 GB-month) and Workers Free request allowance. These are quotas, not unlimited hosting. Public streaming can exhaust daily Worker request quotas.
- The original Termux Python media server remains available for same-Wi-Fi use only. Do not port-forward or expose its plain HTTP listener to the public internet.
- Only upload videos and other media that you own or have permission to distribute.

## Media hosting

See [`cloudflare-media/README.md`](cloudflare-media/README.md) for the free-tier R2 Worker setup and admin uploader. See [`media-server/README.md`](media-server/README.md) for the optional LAN-only Termux server.

## Verification notes

- GitHub Actions confirms that the Android project compiles; it does not replace device testing.
- The Cloudflare Worker and admin page have syntax-validation automation; live Cloudflare deployment and video playback still require testing in the user's account.
- Still validate audio playback/background controls, local and shared video seeking, backup/restore on a real device, navigation, and smaller-screen layouts.

---

## AI-assisted development

> **Vibe coded with AI.** This project was built and iterated on with AI assistance, guided by human ideas, prompts, testing, and decisions. AI-generated contributions may contain mistakes; review and verify changes before use.
