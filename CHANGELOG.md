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
