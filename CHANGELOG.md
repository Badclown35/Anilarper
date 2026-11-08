# Changelog

All notable changes to Anilarper are documented here.

## 1.0.0 — Phase 1 stabilization (in progress)

- Fixed local music player disposal so the active player is released only when Music leaves composition.
- Hardened anime, manga/light-novel, and music JSON loaders to skip malformed individual entries instead of discarding the entire collection.
- Restore now checks backup format/version and rejects empty preferences, saves a safety snapshot in a separate preferences file before replacement, and refreshes in-memory library/profile state after success.
- Persisted anime season, season year, genres, and studios for statistics and metadata continuity.
- Corrected episode decrement/status behavior; marking a title completed fills known episode progress, and watch-status/progress changes update activity timestamps.
- Local audio picker now uses the provider's display filename.

> Device verification is still required; this phase is not considered complete until music and backup/restore are tested on a phone.
