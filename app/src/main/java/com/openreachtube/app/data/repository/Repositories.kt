package com.openreachtube.app.data.repository

import com.openreachtube.app.data.local.dao.UserInterestDao
import com.openreachtube.app.data.local.entity.UserInterestEntity
import com.openreachtube.app.domain.model.CommentItem
import com.openreachtube.app.domain.model.PaginatedList
import com.openreachtube.app.domain.model.SearchTab
import com.openreachtube.app.domain.model.StreamBundle
import com.openreachtube.app.domain.model.StreamOption
import com.openreachtube.app.domain.model.SubtitleItem
import com.openreachtube.app.domain.model.VideoItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import org.schabi.newpipe.extractor.stream.VideoStream

/**
 * Repository layer over NewPipeExtractor's YouTube service (original adapter code).
 * Every call runs on IO and degrades to empty results on extraction failures so the
 * UI can always render something (with a retry affordance instead of a dead end).
 */
@Singleton
class VideoRepository @Inject constructor(
    private val userInterestDao: UserInterestDao,
) {
    data class HomeFeed(
        val categories: List<String> = listOf("All", "Music", "Gaming", "News", "Movies"),
        val trending: List<VideoItem> = emptyList(),
    )

    companion object {
        private const val TRENDING_URL = "https://www.youtube.com/feed/trending"
    }

    /** Category chips: "All" (trending kiosk) + the service's own kiosk lineup. */
    suspend fun availableCategories(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            listOf("All") + ServiceList.YouTube.kioskList.availableKiosks
                .filter { !it.equals("Trending", ignoreCase = true) }
                .take(5)
        }.getOrDefault(listOf("All", "Music", "Gaming", "News", "Movies"))
    }

    /** "All" loads the trending kiosk; any other category searches YouTube for it. */
    suspend fun loadCategory(category: String): List<VideoItem> = withContext(Dispatchers.IO) {
        if (category.equals("All", ignoreCase = true)) {
            runCatching {
                KioskInfo.getInfo(ServiceList.YouTube, TRENDING_URL).relatedItems
                    .mapNotNull { it.toVideoItem() }
            }.getOrDefault(emptyList())
        } else {
            searchExtracted(category, YoutubeSearchQueryHandlerFactory.VIDEOS)
        }
    }

    suspend fun subscriptionsFeed(channelUrls: List<String>): List<VideoItem> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                channelUrls.take(10).map { url ->
                    async {
                        runCatching {
                            val channel = ChannelInfo.getInfo(ServiceList.YouTube, url)
                            val videosTab = channel.tabs.firstOrNull { tab ->
                                tab.contentFilters.any { it.equals("videos", ignoreCase = true) }
                            }
                            if (videosTab == null) emptyList()
                            else ChannelTabInfo.getInfo(ServiceList.YouTube, videosTab)
                                .relatedItems.mapNotNull { it.toVideoItem() }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
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

    /** Records a keyword signal (searches and watched titles) for interest ranking. */
    suspend fun recordInterest(keyword: String) {
        val cleaned = keyword.trim().lowercase().take(40)
        if (cleaned.length < 3) return
        runCatching { userInterestDao.upsert(UserInterestEntity(keyword = cleaned)) }
    }
}

@Singleton
class SearchRepository @Inject constructor() {
    suspend fun search(query: String, tab: SearchTab): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val filter = when (tab) {
                SearchTab.VIDEOS -> YoutubeSearchQueryHandlerFactory.VIDEOS
                SearchTab.CHANNELS -> YoutubeSearchQueryHandlerFactory.CHANNELS
                else -> null
            }
            searchExtracted(query, filter).let { items ->
                if (tab == SearchTab.SHORTS) items.filter { it.durationSeconds in 1..90 }
                else items
            }
        }
}

@Singleton
class ShortsRepository @Inject constructor() {
    /** Shorts feed: the #shorts hashtag search filtered to vertical-length videos. */
    suspend fun shortsFeed(continuation: String?): PaginatedList<VideoItem> =
        withContext(Dispatchers.IO) {
            val items = runCatching {
                searchExtracted("#shorts", YoutubeSearchQueryHandlerFactory.VIDEOS)
                    .filter { it.durationSeconds in 1..90 }
                    .map { it.copy(isShort = true) }
                    .take(30)
            }.getOrDefault(emptyList())
            PaginatedList(items, null)
        }
}

@Singleton
class StreamRepository @Inject constructor() {
    suspend fun resolve(videoUrl: String): StreamBundle = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val streams = buildList {
            addAll(info.videoStreams.map { it.toOption(isAudioOnly = false, videoOnly = false) })
            addAll(info.videoOnlyStreams.map { it.toOption(isAudioOnly = false, videoOnly = true) })
            addAll(info.audioStreams.map { it.toOption(isAudioOnly = true, videoOnly = false) })
        }
        StreamBundle(
            videoId = info.url,
            title = info.name,
            uploader = info.uploaderName ?: "Unknown",
            uploaderUrl = info.uploaderUrl,
            thumbnailUrl = info.thumbnails.bestUrl(),
            durationSeconds = info.duration,
            viewCount = info.viewCount,
            uploadDate = info.textualUploadDate,
            streams = streams,
            hlsUrl = info.hlsUrl.takeIf { it.isNotBlank() },
            subtitles = info.subtitles.map { it.toItem() },
            related = info.relatedItems.mapNotNull { it.toVideoItem() },
        )
    }
}

@Singleton
class CommentsRepository @Inject constructor() {
    suspend fun commentsFor(videoUrl: String): List<CommentItem> = withContext(Dispatchers.IO) {
        runCatching {
            CommentsInfo.getInfo(ServiceList.YouTube, videoUrl).relatedItems
                .filterIsInstance<CommentsInfoItem>()
                .map { item ->
                    CommentItem(
                        id = item.commentId,
                        author = item.name ?: "Anonymous",
                        text = item.commentText?.content.orEmpty(),
                        likeCount = item.likeCount.toLong(),
                        publishedAgo = runCatching {
                            item.uploadDate?.offsetDateTime()?.toLocalDate()?.toString()
                        }.getOrNull(),
                    )
                }
        }.getOrDefault(emptyList())
    }
}

/** Runs a YouTube search with an optional filter and maps recognized items. */
private suspend fun searchExtracted(query: String, filter: String?): List<VideoItem> =
    withContext(Dispatchers.IO) {
        runCatching {
            val qh = ServiceList.YouTube.searchQHFactory
            val handler = if (filter == null) qh.fromQuery(query)
            else qh.fromQuery(query, listOf(filter), "")
            SearchInfo.getInfo(ServiceList.YouTube, handler).relatedItems
                .mapNotNull { it.toVideoItem() }
        }.getOrDefault(emptyList())
    }

private fun InfoItem.toVideoItem(): VideoItem? = when (infoType) {
    InfoItem.InfoType.STREAM -> (this as StreamInfoItem).toVideoItem()
    InfoItem.InfoType.CHANNEL -> VideoItem(
        id = url, title = name ?: "Channel", uploader = "Channel", uploaderUrl = url,
        thumbnailUrl = thumbnails.bestUrl(),
    )
    else -> null
}

private fun StreamInfoItem.toVideoItem() = VideoItem(
    id = url,
    title = name ?: "",
    uploader = uploaderName ?: "",
    uploaderUrl = uploaderUrl,
    thumbnailUrl = thumbnails.bestUrl(),
    durationSeconds = duration,
    viewCount = viewCount,
    uploadDate = textualUploadDate,
    isShort = duration in 1..90,
)

private fun VideoStream.toOption(isAudioOnly: Boolean, videoOnly: Boolean) = StreamOption(
    url = url.orEmpty(),
    mimeType = format?.mimeType.orEmpty(),
    width = width,
    height = height,
    isAudioOnly = isAudioOnly,
    isVideoOnly = videoOnly,
)

private fun AudioStream.toOption(isAudioOnly: Boolean, videoOnly: Boolean) = StreamOption(
    url = url.orEmpty(),
    mimeType = format?.mimeType.orEmpty(),
    isAudioOnly = isAudioOnly,
    isVideoOnly = videoOnly,
)

private fun SubtitlesStream.toItem() = SubtitleItem(
    languageTag = languageTag,
    url = url.orEmpty(),
    isAutoGenerated = isAutoGenerated,
)

/** Picks the largest known-resolution thumbnail (falls back to the last entry). */
private fun List<Image>?.bestUrl(): String? = when {
    this == null || isEmpty() -> null
    else -> maxByOrNull { it.height.coerceAtLeast(0) * 10_000 + it.width.coerceAtLeast(0) }?.url
}
