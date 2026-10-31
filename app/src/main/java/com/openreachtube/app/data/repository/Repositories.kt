package com.openreachtube.app.data.repository

import com.openreachtube.app.data.local.dao.UserInterestDao
import com.openreachtube.app.data.network.PoTokenEngine
import com.openreachtube.app.domain.model.PaginatedList
import com.openreachtube.app.domain.model.SearchTab
import com.openreachtube.app.domain.model.StreamBundle
import com.openreachtube.app.domain.model.VideoItem
import javax.inject.Inject
import javax.inject.Singleton
import org.schabi.newpipe.extractor.ServiceList

/**
 * Repository layer over NewPipeExtractor's YouTube service (original adapter code).
 *
 * TODO(phase-1): implement the extractor calls (trending, search, streams,
 * comments) against ServiceList.YouTube and map InfoItems to domain models.
 * Call shapes change with extractor versions - validate against live responses.
 */
@Singleton
class VideoRepository @Inject constructor(
    private val userInterestDao: UserInterestDao,
) {
    data class HomeFeed(
        val forYou: List<VideoItem> = emptyList(),
        val latest: List<VideoItem> = emptyList(),
        val trending: List<VideoItem> = emptyList(),
    )

    suspend fun loadHomeFeed(): HomeFeed {
        // TODO(phase-1): ServiceList.YouTube trending + channel "latest" queries.
        throw UnsupportedOperationException("TODO(phase-1): wire extractor")
    }

    suspend fun subscriptionsFeed(channelUrls: List<String>): List<VideoItem> {
        // TODO(phase-3): per-channel feeds with FeedCache offline fallback.
        return emptyList()
    }

    /** Local "For You" ranking: boosts titles matching recorded user interests. */
    suspend fun rankByInterest(items: List<VideoItem>): List<VideoItem> {
        val interests = userInterestDao.top()
        if (interests.isEmpty()) return items
        return items.sortedByDescending { item ->
            val lower = item.title.lowercase()
            interests.count { lower.contains(it.keyword.lowercase()) }
        }
    }
}

@Singleton
class SearchRepository @Inject constructor() {
    suspend fun search(query: String, tab: SearchTab): List<VideoItem> {
        // TODO(phase-1): map tab -> searchFilter and execute via ServiceList.YouTube.
        throw UnsupportedOperationException("TODO(phase-1): wire extractor search")
    }
}

@Singleton
class ShortsRepository @Inject constructor() {
    suspend fun shortsFeed(continuation: String?): PaginatedList<VideoItem> {
        // TODO(phase-1): Shorts extraction with continuation handling.
        return PaginatedList(emptyList(), null)
    }
}

@Singleton
class StreamRepository @Inject constructor() {
    suspend fun resolve(videoUrl: String, poTokenEngine: PoTokenEngine): StreamBundle {
        // TODO(phase-1): StreamInfo.getInfo -> map audio/video streams; attach
        // poTokenEngine.generate(videoId, nonce) where the client requires it.
        throw UnsupportedOperationException("TODO(phase-1): wire extractor streams")
    }
}
