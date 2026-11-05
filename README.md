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
NewPipeExtractor v0.26.5 (vendored as the `:extractor` module) · Rhino (local poToken evaluation)

## The vendored `:extractor` module
NewPipeExtractor v0.26.5 (the latest tag) still parses YouTube's *old* response
shapes, so against today's API two things break and are patched locally:

1. `YoutubeChannelHelper.getChannelResponse` unconditionally followed a channel
   redirect inside `onResponseReceivedActions` — YouTube no longer includes
   that key, which NPE-d the live kiosk and every channel page/tab
   (subscriptions included). The patch treats a missing key as "this response
   is already the final page".
2. YouTube search results for queries like "shorts" no longer contain
   `videoRenderer` items — they come as `gridShelfViewModel` shelves of
   `shortsLockupViewModel` items, which the stock search extractor ignored.
   The patch collects them (fixes the Shorts feed).
3. YouTube killed the Trending browse endpoint server-side (the app falls back
   to the default "live" kiosk).

App-side contract: `KioskInfo.getInfo(extractor)` requires `fetchPage()` to be
called on the extractor first — the home feed does this since v1.2 (an
unfetched extractor NPEs on null page data, which was the crash reported in
v1.0/v1.1).

The protobuf Java sources under `services/youtube/protos` are pre-generated
with protoc 4.35.1 so the module is a plain `java-library` with no extra
Gradle plugins. Keep the vendored sources GPL-3.0-or-later like the original
(see Legal notes).

## Open in Android Studio
1. Install Android Studio Ladybug or newer + SDK 36.
2. `File > Open` this folder; let Gradle sync (JitPack fetches the nanojson fork).
3. Run the `app` configuration on a device/emulator with Android 7.0+.

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
