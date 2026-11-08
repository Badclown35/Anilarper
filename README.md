# Anilarper

A personal, native Android anime and manga companion.

## Foundation
- Kotlin + Jetpack Compose
- Modern dark interface with Home, Discover, Library, Quests, and Profile navigation
- Android API 26+; Java 17; compile/target SDK 35
- GitHub Actions workflow to build and upload a debug APK

## Build
Open this repository in Android Studio, allow Gradle sync, then run the `app` configuration.

Or push to GitHub and run **Actions → Build Anilarper APK**. Download the `Anilarper-debug` artifact from the workflow run.

## Roadmap
1. AniList GraphQL catalog for anime and manga search/details
2. Room database for local library, progress, ratings, and preferences
3. MAL export ZIP import
4. Personalized recommendations and Revival mode
5. Quests, XP, achievements, and collectibles
6. Backup/export and final UI polish

The first commit is the UI and build foundation. Catalog, tracking, and RPG systems are still to be implemented.
