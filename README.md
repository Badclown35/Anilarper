# Anilarper

A personal, native Android anime, manga, light-novel, and soundtrack companion built around rediscovery, tracking, and low-pressure exploration.

## Current build: 0.7.0

- Kotlin + Jetpack Compose; modern dark interface with violet accents
- Home, Discover, Library, Read, Music, Quests, and Profile sections
- Paginated anime trending and title search through the AniList GraphQL API
- Seasonal browsing by season and year, with genre, format, status, and sort filters
- Expanded anime details with genres, studios, season metadata, and synopsis
- Multi-hop franchise maps that follow AniList sequel, prequel, movie, OVA, special, side-story, and other relationship edges
- Franchise entries can be opened directly from the map, including titles not yet saved in your library
- Franchise scans show their coverage and disclose when AniList relationship data or scan limits may leave gaps
- Anime details, cover art, episode counts, scores, and synopsis
- Local anime library with episode progress and five watch statuses
- Manga and light-novel discovery through AniList
- Separate manga and light-novel shelves
- Chapter and volume progress tracking with local persistence
- Local soundtrack player: add audio files through Android's file picker, save file references, and play/pause/restart/remove tracks
- Discovery excludes entries marked as adult by AniList
- Android API 26+; Java 17; compile/target SDK 35
- GitHub Actions builds and uploads a debug APK

## Data and privacy

- Public catalog metadata and cover images are requested from AniList.
- Anime and reading progress are stored locally on the device.
- Soundtrack files are not copied into the app; Anilarper stores references to files selected by the user.
- No AniList sign-in is required for local tracking.
- The app does not host or distribute copyrighted anime episodes, books, or music.
- The current music player is in-app playback; background playback and streaming-service integrations are not implemented yet.

## Build

Open this repository in Android Studio, allow Gradle sync, then run the `app` configuration.

Or open **Actions → Build Anilarper APK** and download the `Anilarper-debug` artifact from a successful workflow run.

## Next milestones

1. Validate manga and light-novel catalog filtering and improve title details
2. Add reading status controls and custom lists
3. Build a richer music player, playlists, album metadata, and background playback
4. Room database migration and backup/export
5. Optional MyAnimeList export ZIP import
6. Revival mode and recommendations based on personal preferences
7. Quests, XP, achievements, and collectibles
8. Legitimate watch/read availability links where reliable provider data exists
9. UI polish, accessibility, and device testing

Design references are used for general UX patterns only. Anilarper uses its own interface and does not copy proprietary source code, branding, or artwork.


## 0.5.0 — Personalization Update

- Customizable local profile: display name, bio, avatar image URL, favorite titles, and banner style.
- Personal collection stats for saved anime, completed anime, and reading titles.
- Optional milestone quests with progress derived from your library and reading shelf.
- Claimable XP rewards saved locally on the device.
- No streaks, daily deadlines, or penalties for taking breaks.

Profile settings and quest progress are stored locally. Avatar images are loaded from the URL you provide.


### Music and navigation follow-up

- The Music area now supports saving HTTPS listening links (YouTube, SoundCloud, Spotify, Apple Music, YouTube Music, and other services) and opening them through the service's official app or website.
- Existing local audio-file playback remains available.
- The bottom navigation is reduced to Home, Discover, Library, and More. Read, Music, Quests, and Profile are grouped in a bottom sheet to reduce crowding on phones.

Streaming links are bookmarks/launch points; they are not downloaded or re-hosted by Anilarper. In-app playback for third-party services requires each service's permitted player/embed integration and is a separate follow-up.
\n\n## 0.6.0 — Discovery & Franchise Update\n\n- Paginated anime catalog results with a Load more control.\n- Browse by season and year, genre, format, release status, and sort order.\n- Expanded title details with genres, studios, season/year, and synopsis.\n- Connected anime entries with relationship labels for sequels, prequels, side stories, movies, and other franchise links.\n- Search and catalog loading errors now offer a retry path; AniList rate-limit responses are handled with a clear message.\n- Existing local anime, reading, profile, quest, and music data formats remain unchanged.\n

## 0.7.0 — Franchise Completeness Update

- Replaced the one-hop connected-title list with a multi-hop franchise graph scan.
- Follows AniList relationship edges across multiple connected anime, with duplicate protection and a bounded scan to avoid runaway requests.
- Shows relation type, format, year, and release status for discovered entries.
- Franchise maps include catalog entries whether or not they are in the user's library.
- Clearly labels potentially incomplete results because upstream relationship metadata is not guaranteed to be exhaustive.
