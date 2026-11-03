package com.openreachtube.app.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/**
 * GitHub Releases self-update channel. Point at YOUR repository before release
 * (the report's evidence used the original author's URL; this remake uses its own).
 */
@Singleton
class UpdateChecker @Inject constructor() {

    @Serializable
    data class ReleaseDto(
        val tag_name: String,
        val body: String? = null,
        val assets: List<AssetDto> = emptyList(),
    )

    @Serializable
    data class AssetDto(val name: String, val browser_download_url: String)

    private val http = HttpClient { install(ContentNegotiation) { json() } }

    // Releases feed for the in-app updater (UpdateInstaller) — this remake's repo.
    private val releasesUrl = "https://api.github.com/repos/Rahulhaldar/Itify/releases/latest"

    suspend fun latest(): ReleaseDto? = runCatching {
        http.get(releasesUrl).body<ReleaseDto>()
    }.getOrNull()
}
