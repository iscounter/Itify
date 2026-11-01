# OpenReachTube v0.2.0 — Modern UI + Full Data Wiring + Toolchain Upgrade

This release turns the original scaffold (which showed "No Internet Connection"
because every repository method threw `UnsupportedOperationException`) into a
working video client with a ZyvoTube-inspired modern UI.

## Root cause of the "No Internet Connection" message

`VideoRepository.loadHomeFeed()` threw `UnsupportedOperationException` on every
call. `HomeViewModel` caught it and set `offline = true`, which the home screen
rendered as "No Internet Connection". The extractor, search, shorts and stream
layers were unwired stubs. All of them are now real implementations over
NewPipeExtractor v0.24.8 (verified against the actual artifact via javap).

## Data layer — now wired for real

- **Home feed**: category chips backed by YouTube kiosks (`KioskInfo.getInfo`
  on feed/trending for "All") plus search-based categories (Music/Gaming/News/
  Movies); kiosk list is read at runtime from `kioskList.availableKiosks`.
- **Search**: `SearchInfo.getInfo` with tab filters (`VIDEOS`, `CHANNELS`);
  Shorts tab filters results to <= 90 s.
- **Shorts feed**: `#shorts` search filtered to vertical-length videos.
- **Stream resolution**: `StreamInfo.getInfo` maps progressive, video-only and
  audio streams plus subtitles, related items and HLS URL.
- **Comments**: `CommentsInfo.getInfo` for the player screen.
- **Subscriptions / notifications**: per-channel uploads via `ChannelInfo`
  tabs + `ChannelTabInfo`, fetched in parallel with graceful per-channel
  failure.
- **Watch history**: recorded on playback, position persisted every 5 s, so
  "Continue Playing" works across restarts.
- **Interest ranking**: searches and categories feed local "For You" ranking.

## Modern UI (ZyvoTube reference)

- Light theme with red accent, warm-cream floating pill bottom bar with a red
  disc behind the active tab (Home / Shorts / Subscriptions / You /
  Notifications); bar hides on full-screen routes.
- Home: logo header (search + account actions), category pill chips,
  "Continue Playing" rail with red progress bars, "Trending Feed" large cards
  with duration badges.
- Player: 16:9 Media3 PlayerView, title/meta, subscribe toggle, Download
  (chunked, resumable service) and Share actions, comments list, "Up next"
  related videos, SponsorBlock auto-skip.
- Shorts: vertical pager with real stream playback on the settled page.
- Search: rounded search field, tab chips, recent searches.
- You: history rail, liked videos, downloads count, playlists.
- Subscriptions/Notifications: channel avatars row + latest upload cards.

## Toolchain — upgraded to latest stable (verified 2026-11-01)

| Component | Old | New |
| --- | --- | --- |
| Gradle | 8.9 | **9.6.0** (wrapper regenerated) |
| Android Gradle Plugin | 8.7.3 | **9.5.0** |
| Kotlin | 2.0.21 | **2.4.21** |
| KSP | 2.0.21-1.0.28 | **2.3.13** |
| Hilt (Dagger) | 2.52 | **2.60.1** |
| Compose BOM | 2024.12.01 | **2026.09.00** |
| Activity Compose | 1.9.3 | **1.13.0** |
| Navigation Compose | 2.8.5 | **2.10.2** |
| Lifecycle | 2.8.7 | **2.11.0** |
| Room | 2.6.1 | **2.8.5** |
| DataStore | 1.1.1 | **1.3.0** |
| WorkManager | 2.10.0 | **2.12.0** |
| Media3 | 1.5.1 | **1.11.1** |
| Coil | 3.0.4 | **3.5.0** |
| Ktor | 3.0.3 | **3.6.1** |
| OkHttp | 4.12.0 | **5.5.1** |
| kotlinx-serialization | 1.7.3 | **1.12.0** |
| NewPipeExtractor | v0.24.6 | **v0.24.8** |
| Rhino | 1.7.15 | **1.8.1** (1.9.x dexes only on minSdk 26+) |
| jsoup | 1.18.3 | **1.23.2** |
| androidx.hilt family | 1.2.0 | **1.4.0** |
| core-ktx / appcompat | 1.15.0 / 1.7.0 | **1.19.1 / 1.8.0** |

Notes on two version caps:
- **Coil 3.5.0** (not 3.6.x): Coil 3.6.0+ AAR metadata requires `compileSdk 37`,
  and the `platforms;android-37` package is not published yet. 3.5.0 is the
  newest buildable release; revisit when API 37 ships.
- **compileSdk = 36 / targetSdk = 36** (highest platform currently published).

AGP 9.5 enables built-in Kotlin + the new DSL by default, which conflicts with
the explicit `org.jetbrains.kotlin.android` plugin — `android.builtInKotlin=false`
and `android.newDsl=false` are set in `gradle.properties` (the documented
temporary bypass) so the project keeps explicit control of Kotlin 2.4.21.

## Build fixes carried into this tree

- `SearchHistoryDao.insert` now inserts a `SearchHistoryEntity` (Room `@Insert`
  cannot take a raw `String`).
- ViewModels import DAOs from `data.local.dao` (not `data.local`).
- `AppDatabase.exportSchema = false`; `MissionDao` provided in `DatabaseModule`.
- `ExtractorInitializer`: multi-value headers handled; NewPipe `Response`
  constructed with the correct v0.24.x signature (5th param `latestUrl`).
- `DownloadServiceBase` is `abstract` (Hilt's generated Java parent cannot
  extend a `sealed class`).
- `ChunkedDownloader` handles a nullable OkHttp response body.
- Launcher icon uses valid VectorDrawable paths only; app theme is
  `Theme.AppCompat.DayNight.NoActionBar` (AppCompatActivity requirement).
- `material-icons-extended` added for the tab/extended icons.
- Gradle wrapper 9.6.0 committed (`gradlew` + jar); GitHub Actions workflow
  builds and uploads the debug APK on every push.
- Release signing: a personal keystore is expected via `ORTUBE_KEYSTORE`
  (or the default path used during this build); replace with your own before
  publishing to a store.

## Known limits

- poToken (BotGuard) is not minted yet; some videos may need the HLS fallback
  (handled: the player prefers HLS when present).
- Shorts pagination is single-shot (30 items) — the extractor has no stable
  shorts kiosk in v0.24.8; revisit when upstream lands one.
- `UpdateChecker` still points at a placeholder GitHub releases URL — replace
  `YOUR_ACCOUNT` before publishing.
