# OpenReach Tube — clean-room starter project

A functionally equivalent rebuild blueprint for the app category represented by
ZyvoTube v1.0.0 (native Kotlin + Jetpack Compose YouTube client with Shorts,
SponsorBlock, background playback, PiP, and chunked video/M4A downloads).

**This project contains 100% original code.** It copies nothing from ZyvoTube's
code, resources or branding. The architecture mirrors publicly observable
mechanisms documented in the accompanying teardown report.

## Stack
Kotlin 2.x · Jetpack Compose (Material 3) · Hilt · Room · DataStore · WorkManager ·
Media3/ExoPlayer · Coil 3 · OkHttp · Ktor · kotlinx.serialization ·
NewPipeExtractor (JitPack) · Rhino (local poToken evaluation)

## Open in Android Studio
1. Install Android Studio Ladybug or newer + SDK 36.
2. `File > Open` this folder; let Gradle sync (JitPack fetches the extractor).
3. Verify the extractor tag in `gradle/libs.versions.toml` against the latest
   release at https://github.com/TeamNewPipe/NewPipeExtractor/releases
4. Run the `app` configuration on a device/emulator with Android 7.0+.

## Legal notes (read before distributing)
- **Licensing**: linking NewPipeExtractor (GPL v3 family) obligates this app to be
  published under a GPL-compatible license. Keep all sources open.
- **YouTube ToS**: downloading YouTube content violates YouTube's Terms of
  Service. This app does not use the official YouTube Data API. Distribute via
  GitHub/F-Droid (sideloading); Google Play would reject a YouTube downloader.
- **Trademark**: "OpenReach Tube" is a placeholder name — make sure your chosen
  name does not infringe YouTube's or ZyvoTube's trademarks.
- **User transparency**: the app shows a third-party terms notice (strings.xml).
- This skeleton ships no Firebase binary (telemetry is a no-op interface).

## Feature parity checklist
See `docs/REMAINING_WORK.md` for the phased plan (phases 0-6 from the report).
