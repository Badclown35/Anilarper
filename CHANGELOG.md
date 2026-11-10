# Changelog

All notable changes to Anilarper are documented here.

## 1.0.0 — Phase 1 stabilization (verified by user)

- Fixed local music player disposal so the active player is released only when Music leaves composition.
- Hardened anime, manga/light-novel, and music JSON loaders to skip malformed individual entries instead of discarding the entire collection.
- Restore now checks backup format/version and rejects empty preferences, saves a safety snapshot in a separate preferences file before replacement, and refreshes in-memory library/profile state after success.
- Persisted anime season, season year, genres, and studios for statistics and metadata continuity.
- Corrected episode decrement/status behavior; marking a title completed fills known episode progress, and watch-status/progress changes update activity timestamps.
- Local audio picker now uses the provider's display filename; shuffle no longer overwrites saved track order.
- Music playback now requests audio focus and releases MediaPlayer instances on setup/error paths.
- Navigation selection survives rotation, Back returns to Home, and stale Discover searches are cancelled.

> Phase 1 was tested successfully by the user on-device.


## 1.0.0 — Phase 2 structure (in progress)

- Extracted Home, Discover, Library, Reading, Music, Quests, Backup, History, Statistics, and Profile into dedicated `ui/` packages.
- Moved the shared palette and Material theme into `theme/AppTheme.kt`; MainActivity is now focused on app setup and navigation.
- Added a LibraryRepository as the single persistence owner for anime and reading collections, retaining SharedPreferences until the planned Room migration in Phase 3.
- Added AnimeLibraryViewModel and ReadingLibraryViewModel with StateFlow, and connected collection screens to them.
- Added a DiscoverViewModel cache for result pages so search results survive tab changes; active search jobs are cancelled when Discover leaves composition.
- Made key screen inputs and profile editor drafts saveable across navigation.
- Replaced string-based screen switching with Navigation Compose, including saved bottom-tab back stacks and navigation-managed Back behavior.
- Remaining: verify the latest refactor build, review any regressions, and device-test navigation/state restoration. Phase 2 stays open until those checks pass.

## Phase 3 — Feature checkpoint (2026-11-08; build/device verification pending)

- Added Room collection storage and a one-time import from existing SharedPreferences JSON; original preferences remain available as a recovery source.
- Added a Room catalog cache with 30-day expiry and offline fallback for Discover and reading search.
- Added a Media3 playback service/session with queue, audio focus, background playback, notification and lock-screen transport controls.
- Backup export now includes Room collections; restore snapshots the current database-backed backup before applying and refreshes collection state.
- Added user-created anime lists.
- Added genre-based AniList recommendations.
- Added MyAnimeList anime CSV import with AniList ID matching, status/progress/score transfer, and a preview.
- Fixed bottom navigation to avoid restoring an unrelated More destination.

## Anime UI overhaul (2026-11-10; build/device verification pending)

- New "midnight indigo" theme with sakura and aqua accents; the five profile accent colors still drive the UI. Existing palette names are unchanged so all screens inherit the new look.
- Added shared anime-style components in `theme/AnimeUi.kt`: gradient rim-lit cards, section headers, pills, gradient progress bars, cropped poster images and a glowing backdrop.
- Redesigned Home (hero banner, gradient episode progress, poster rail), the bottom navigation bar and the More screen (two-column tile grid).
- Fixed cover art that was stretched or letterboxed (missing crop) in Library rows, Reading, History, Recommendations, Discover details and the profile avatar; images are now clipped to their rounded corners.
- Themed Material dialogs/menus (surface container colors) and set app-wide shapes and typography.
