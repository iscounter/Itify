# CI build fixes — 2026-11-01

This document lists every change made to make `assembleDebug` pass on GitHub Actions
(verified locally with Gradle 8.9 / AGP 8.7.3 / JDK 17 target against compileSdk 36).

## Compiler errors (the ones that failed the Actions run)

| # | File | Problem | Fix |
|---|------|---------|-----|
| 1 | `app/src/main/java/com/openreachtube/app/data/local/dao/Daos.kt` | `@Insert suspend fun insert(query: String)` — Room `@Insert` only accepts `@Entity` classes, so KSP failed with *"Type of the parameter must be a class annotated with @Entity"* | Method now inserts a `SearchHistoryEntity`; call site updated accordingly |
| 2 | `app/src/main/java/com/openreachtube/app/ui/screens/library/LibraryViewModel.kt` | DAOs imported from `data.local` instead of `data.local.dao` → unresolvable types → Hilt `error.NonExistentClass` (3 params) | Imports corrected to `com.openreachtube.app.data.local.dao.*` |
| 3 | `app/src/main/java/com/openreachtube/app/ui/screens/search/SearchViewModel.kt` | Same wrong import for `SearchHistoryDao`; plus the DAO insert call now wraps the query in `SearchHistoryEntity` | Import corrected; `searchHistoryDao.insert(SearchHistoryEntity(query = query))` |
| 4 | `app/src/main/java/com/openreachtube/app/ui/screens/subscriptions/SubscriptionsFeedViewModel.kt` | Same wrong import for `SubscriptionDao` | Import corrected |
| 5 | `app/src/main/java/com/openreachtube/app/data/network/ExtractorInitializer.kt` | NewPipeExtractor `Request.headers()` returns `Map<String, List<String>>` but the value list was passed straight into OkHttp's `header()`; `Response` constructor expects `Map<String, List<String>>` but a `Map<String, String>` was built | Header loop iterates the list; `Response(..., it.headers.toMultimap(), ...)` |
| 6 | `gradle/libs.versions.toml` + `app/build.gradle.kts` | `Icons.Filled.PlayCircle / Subscriptions / VideoLibrary` (NavGraph) live in `material-icons-extended`, which was not a dependency → unresolved references | Added BOM-managed `androidx.compose.material:material-icons-extended` |
| 11 | `app/src/main/java/com/openreachtube/app/data/network/ExtractorInitializer.kt` | `Response`'s 5th constructor parameter in NewPipeExtractor v0.24.6 is `latestUrl: String` (verified in the artifact), not `byte[]` — a `ByteArray` was passed | Passes `request.url()` as `latestUrl` |
| 12 | `app/src/main/java/com/openreachtube/app/download/ChunkedDownloader.kt` | `resp.body.byteStream()` — OkHttp 4.x `Response.body` is nullable | Null-safe with an explicit `IOException` on an empty body |
| 13 | `app/src/main/java/com/openreachtube/app/ui/screens/library/LibraryScreen.kt` | `Text("You", style = …, Modifier.padding(…))` — a positional argument after a named one maps by call position, so `Modifier` landed on the `color` parameter | `Modifier` moved before the named `style` argument |
| 14 | `app/src/main/java/com/openreachtube/app/ui/screens/shorts/ShortsViewModel.kt` | `PaginatedList` exposes `nextPageUrl`, not `continuation` | Uses `result.nextPageUrl` |
| 15 | `app/src/main/java/com/openreachtube/app/services/DownloadServices.kt` | `DownloadServiceBase` was `sealed` — Hilt's generated `Hilt_VideoDownloadService` Java class cannot extend a sealed class (javac 17 enforces the `permits` clause) | Changed to `abstract class` (no exhaustive `when` depends on it) |
| 16 | `app/src/main/java/com/openreachtube/app/di/AppModules.kt` | `[Dagger/MissingBinding]` — `DatabaseModule` never provided `MissionDao`, which `ChunkedDownloader` and the download services inject | Added `@Provides fun missionDao(db: AppDatabase)` |

## Resource / runtime errors (found during verification build)

| # | File | Problem | Fix |
|---|------|---------|-----|
| 7 | `app/src/main/res/drawable/ic_launcher.xml` | `<circle>` is not a valid VectorDrawable element — AAPT failed at `processDebugResources` (`'30' is incompatible with attribute radius`) | Replaced with an arc-based `<path>` drawing the same ring |
| 8 | `app/src/main/res/values/themes.xml` | `MainActivity` extends `AppCompatActivity` but the theme parent was the framework `android:Theme.Material.NoActionBar` → compiles fine but crashes on launch (*"You need to use a Theme.AppCompat theme"*) | Parent changed to `Theme.AppCompat.DayNight.NoActionBar` |

## Warnings

| # | File | Problem | Fix |
|---|------|---------|-----|
| 9 | `gradle.properties` | AGP 8.7.3 warns it was only tested up to compileSdk 35 | Added `android.suppressUnsupportedCompileSdk=36` (remove once AGP ≥ 8.9) |
| 10 | `app/.../data/local/AppDatabase.kt` | Room *"Schema export directory was not provided"* | `exportSchema = false` for the phase-1 starter; flip back to `true` + a `room.schemaLocation` KSP arg when migrations start |

## Build infrastructure

- **Gradle wrapper was missing** (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`). Added, pinned to Gradle 8.9 — GitHub Actions can now use the standard `./gradlew` entry point.
- **`.github/workflows/android.yml` added**: JDK 17 (Temurin) + `gradle/actions/setup-gradle@v4` (dependency caching) + `./gradlew assembleDebug` + APK uploaded as the `Itify-debug-apk` artifact on every push/PR.
- `local.properties` is intentionally absent (it is machine-specific; CI sets `ANDROID_HOME` itself, and the repo should keep it in `.gitignore`).

## Remaining known TODOs (by design, see docs/REMAINING_WORK.md)

- Repositories throw `UnsupportedOperationException` for extractor-backed feeds (phase-1 wiring).
- `UpdateChecker` still points at the `YOUR_ACCOUNT` placeholder releases URL.
