# Anilarper

A personal, native Android anime and manga companion designed around rediscovery, tracking, and low-pressure exploration.

## Current build: 0.3.0

- Kotlin + Jetpack Compose
- Modern dark interface with Home, Discover, Library, Quests, and Profile navigation
- Live anime trending and title search through the AniList GraphQL API
- Anime quick-view details, cover art, format, episode count, score, and synopsis
- Save/remove titles in a personal library
- Track episodes watched for each saved anime
- Update watch status: Planning, Watching, Completed, Paused, or Dropped
- Progress bars and completed-title count in the library
- Saved titles, watch status, and episode progress persist locally on the device
- Discovery excludes entries marked as adult by AniList
- Android API 26+; Java 17; compile/target SDK 35
- GitHub Actions workflow builds and uploads a debug APK

## Data and privacy

- Public anime metadata and cover images are requested from AniList.
- The current saved list is stored locally with Android app preferences.
- No AniList sign-in or account linking is required for the current build.
- The app does not provide unauthorized streaming or host video.

## Build

Open this repository in Android Studio, allow Gradle sync, then run the `app` configuration.

Or open **Actions → Build Anilarper APK** and download the `Anilarper-debug` artifact from a successful workflow run.

## Next milestones

1. Manga catalog and chapter tracking
2. Ratings, custom lists, and richer title details
3. Room database migration and backup/export
4. Optional MyAnimeList export ZIP import
5. Revival mode and recommendations based on the user's own preferences
6. Quests, XP, achievements, and collectibles
7. Legitimate watch/read availability links where reliable provider data exists
8. UI polish, accessibility, and testing

Design references are used for general UX patterns only. Anilarper uses its own interface and does not copy proprietary source code, branding, or artwork.
