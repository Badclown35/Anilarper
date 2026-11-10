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
- The optional Termux media server is a separate, user-operated MP4 server. Its first version is LAN-only; do not port-forward or expose its plain HTTP listener to the public internet.
- Community Videos expects an HTTPS server address. Remote access, invite controls, and end-to-end family/friends setup still require a secure tunnel/access configuration.
- Only share videos and other media that you own or have permission to distribute.
- No AniList sign-in is required for local tracking.

## Media server

See [`media-server/README.md`](media-server/README.md) for Termux setup, upload/organization instructions, API details, and security limitations.

## Verification notes

- GitHub Actions confirms that the Android project compiles; it does not replace device testing.
- Still validate audio playback/background controls, local and shared video seeking, backup/restore on a real device, navigation, and smaller-screen layouts.
- Public family/friends streaming is not ready until HTTPS remote access and appropriate access controls are configured.