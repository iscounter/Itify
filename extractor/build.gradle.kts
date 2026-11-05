// Vendored copy of NewPipeExtractor v0.26.5 (GPL-3.0-or-later,
// https://github.com/TeamNewPipe/NewPipeExtractor) with local patches:
//  - YoutubeChannelHelper: tolerate browse responses without
//    onResponseReceivedActions (live kiosk + all channel pages were NPE-ing)
//  - YoutubeSearchExtractor: collect shortsLockupViewModel items from
//    gridShelfViewModel shelves ("shorts" search returns no videoRenderer)
// The protobuf Java sources under services/youtube/protos are pre-generated
// with protoc 4.35.1 so no protobuf Gradle plugin is needed.
plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // nanojson fork pinned by the upstream extractor build (JitPack commit)
    api("com.github.TeamNewPipe:nanojson:e9d656ddb49a412a5a0a5d5ef20ca7ef09549996")
    // exposed in public API signatures (javax.annotation.@Nonnull)
    api("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("com.google.protobuf:protobuf-javalite:4.35.1")
    // rhino 1.9+ requires minSdk 26; keep 1.8.x while minSdk is 24
    implementation("org.mozilla:rhino:1.8.1")
    implementation("org.mozilla:rhino-engine:1.8.1")
}
