# Remaining work (phases map to report chapter 11)

- Phase 0 (done here): Gradle + Hilt graph + Compose shell + navigation + theming.
- Phase 1: Wire `ExtractorInitializer` into `OpenReachApp`, implement
  `StreamResolver` search/trending/streams via NewPipeExtractor services, validate
  `PoTokenEngine` BotGuard evaluation against live responses (TODO markers inline).
- Phase 2: `PlaybackService` already skeletons Media3 MediaSessionService — add
  PiP affordances, subtitle track selection, sleep timer, speed sheet.
- Phase 3: Room schema is complete — connect repositories to UI, build For You
  ranking from `UserInterestDao`, subscriptions feed cache.
- Phase 4: `ChunkedDownloader` is implemented generically — bind both services to
  mission queue UI, muxing strategy for DASH video+audio, M4A audio extraction.
- Phase 5: SponsorBlock client is implemented — integrate segment overlay into
  player; wire deep links, Takeout import, backup export.
- Phase 6: licensing audit, GPL source publication, release keystore, changelog,
  point `UpdateChecker` at your own GitHub releases URL.
