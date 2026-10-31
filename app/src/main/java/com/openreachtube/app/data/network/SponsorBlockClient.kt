package com.openreachtube.app.data.network

import com.openreachtube.app.domain.model.SponsorSegment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable

/**
 * Community SponsorBlock API consumer (https://sponsor.ajay.app).
 * SponsorBlock is AGPL-3.0 community data — attribute it in the About screen.
 */
@Singleton
class SponsorBlockClient @Inject constructor() {

    private val http = HttpClient { install(ContentNegotiation) { json() } }

    @Serializable
    data class SegmentDto(
        val category: String,
        val segment: List<Float>,
        val UUID: String,
    )

    suspend fun segmentsFor(videoId: String): List<SponsorSegment> = runCatching {
        val raw: List<SegmentDto> = http.get("https://sponsor.ajay.app/api/skipSegments") {
            parameter("videoID", videoId)
            parameter("categories", "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"music_offtopic\",\"preview\"]")
        }.body()
        raw.map { SponsorSegment(it.segment.getOrNull(0) ?: 0f, it.segment.getOrNull(1) ?: 0f, it.category, it.UUID) }
    }.getOrDefault(emptyList())
}
