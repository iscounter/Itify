package com.openreachtube.app.data.repository

import com.openreachtube.app.data.local.dao.UserInterestDao
import com.openreachtube.app.data.local.entity.UserInterestEntity
import com.openreachtube.app.data.network.PoTokenEngine
import com.openreachtube.app.domain.model.ChannelInfo
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
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo as NpChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Repository layer over NewPipeExtractor's YouTube service (original adapter
 * code, v0.3.0). Every call degrades gracefully: failures map to empty results
 * so one broken rail never takes down the screen.
 */

/** Picks the largest image URL; extractor returns ordered-agnostic Image lists. */
private fun List<Image>.bestUrl(): String? =
    maxByOrNull { (it.height.toLong() + 1) * (it.width.toLong() + 1) }?.url

/** Maps any extractor InfoItem onto the domain model; null when unsupported. */
private fun InfoItem.toVideoItem(): VideoItem? = runCatching {
    when (infoType) {
        InfoItem.InfoType.STREAM -> {
            val s = this as StreamInfoItem
            VideoItem(
                id = s.url,
                title = s.name,
                uploader = s.uploaderName ?: "Unknown",
                uploaderUrl = s.uploaderUrl,
                thumbnailUrl = s.thumbnails.bestUrl(),
                durationSeconds = if (s.duration > 0) s.duration / 1000 else 0,
                viewCount = s.viewCount,
                uploadDate = s.textualUploadDate,
                isShort = s.duration in 1..SHORT_MAX_MILLIS,
            )
        }
        InfoItem.InfoType.CHANNEL -> {
            val c = this as ChannelInfoItem
            VideoItem(
                id = c.url,
                title = c.name,
                uploader = c.name,
                thumbnailUrl = c.thumbnails.bestUrl(),
                viewCount = c.subscriberCount,
                isChannel = true,
            )
        }
        else -> null
    }
}.getOrNull()

private const val SHORT_MAX_MILLIS = 3 * 60 * 1000L

@Singleton
class VideoRepository @Inject constructor(
    private val userInterestDao: UserInterestDao,
) {
    data class HomeFeed(
        val forYou: List<VideoItem> = emptyList(),
        val latest: List<VideoItem> = emptyList(),
        val trending: List<VideoItem> = emptyList(),
    )

    /** Trending kiosk feeds the three home rails (For You is interest-ranked). */
    suspend fun loadHomeFeed(): HomeFeed = withContext(Dispatchers.IO) {
        val trending = runCatching {
            KioskInfo.getInfo(ServiceList.YouTube, ServiceList.YouTube.kioskList.defaultKioskId)
                .relatedItems.mapNotNull { it.toVideoItem() }
        }.getOrDefault(emptyList())
        HomeFeed(
            forYou = trending,
            latest = trending.sortedByDescending { it.uploadDate ?: "" },
            trending = trending,
        )
    }

    /** Per-channel latest uploads, fetched in parallel (max 10 channels). */
    suspend fun subscriptionsFeed(channelUrls: List<String>): List<VideoItem> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                channelUrls.take(10).map { url ->
                    async {
                        runCatching {
                            val channel = NpChannelInfo.getInfo(ServiceList.YouTube, url)
                            val videosTab = channel.tabs.firstOrNull { tab ->
                                tab.url.endsWith("/videos")
                            }
                            if (videosTab == null) emptyList()
                            else ChannelTabInfo.getInfo(ServiceList.YouTube, videosTab)
                                .relatedItems.mapNotNull { it.toVideoItem() }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
        }

    /** Channel page header, mapped onto the pure domain model. */
    suspend fun channelHeader(url: String): ChannelInfo = withContext(Dispatchers.IO) {
        val info = NpChannelInfo.getInfo(ServiceList.YouTube, url)
        ChannelInfo(
            id = info.url,
            name = info.name,
            avatarUrl = info.avatars.bestUrl(),
            subscriberCount = info.subscriberCount,
            description = info.description,
        )
    }

    /** One channel content tab ("videos", "shorts", …) as a video list. */
    suspend fun channelTab(channelUrl: String, tabFilter: String): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val info = NpChannelInfo.getInfo(ServiceList.YouTube, channelUrl)
            val handler = info.tabs.firstOrNull { tab -> tab.url.endsWith("/$tabFilter") }
            if (handler == null) emptyList()
            else runCatching {
                ChannelTabInfo.getInfo(ServiceList.YouTube, handler)
                    .relatedItems.mapNotNull { it.toVideoItem() }
            }.getOrDefault(emptyList())
        }

    /** Local "For You" ranking: boosts titles matching recorded user interests. */
    suspend fun rankByInterest(items: List<VideoItem>): List<VideoItem> {
        val interests = runCatching { userInterestDao.top() }.getOrDefault(emptyList())
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

    /** Full-text search; the tab maps to an extractor content filter. */
    suspend fun search(query: String, tab: SearchTab): List<VideoItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val contentFilter = when (tab) {
                    SearchTab.ALL -> ""
                    SearchTab.VIDEOS -> "videos"
                    SearchTab.CHANNELS -> "channels"
                    SearchTab.SHORTS -> "shorts"
                }
                val handler = ServiceList.YouTube.searchQHFactory.fromQuery(
                    query, listOf(contentFilter), "",
                )
                SearchInfo.getInfo(ServiceList.YouTube, handler)
                    .relatedItems.mapNotNull { it.toVideoItem() }
            }.getOrDefault(emptyList())
        }
}

@Singleton
class ShortsRepository @Inject constructor() {

    /** Vertical Shorts feed via the "shorts" search filter, with continuation. */
    suspend fun shortsFeed(continuation: String?): PaginatedList<VideoItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val service = ServiceList.YouTube
                val handler = service.searchQHFactory.fromQuery("shorts", listOf("shorts"), "")
                val items: List<InfoItem>
                val next: Page?
                if (continuation == null) {
                    val info = SearchInfo.getInfo(service, handler)
                    items = info.relatedItems
                    next = info.nextPage
                } else {
                    val page = SearchInfo.getMoreItems(service, handler, Page(continuation))
                    items = page.items
                    next = page.nextPage
                }
                PaginatedList(items.mapNotNull { it.toVideoItem() }, next?.url)
            }.getOrDefault(PaginatedList(emptyList(), null))
        }
}

@Singleton
class StreamRepository @Inject constructor(
    private val poTokenEngine: PoTokenEngine,
) {

    /** Resolves one video into playable streams + metadata + related items. */
    suspend fun resolve(videoUrl: String): StreamBundle = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val poToken = runCatching { poTokenEngine.generate(info.id, info.id) }.getOrNull()

        val streams = buildList {
            info.videoStreams.forEach { v ->
                if (!v.isUrl) return@forEach
                add(
                    StreamOption(
                        url = v.content,
                        mimeType = v.format?.mimeType ?: "video/mp4",
                        width = v.width,
                        height = v.height,
                        averageBitrate = v.bitrate.toLong(),
                        isVideoOnly = v.isVideoOnly(),
                        poToken = poToken,
                    ),
                )
            }
            info.audioStreams.forEach { a ->
                if (!a.isUrl) return@forEach
                add(
                    StreamOption(
                        url = a.content,
                        mimeType = a.format?.mimeType ?: "audio/mp4",
                        averageBitrate = a.averageBitrate.toLong(),
                        isAudioOnly = true,
                        poToken = poToken,
                    ),
                )
            }
        }

        StreamBundle(
            videoId = info.url,
            title = info.name,
            uploader = info.uploaderName,
            uploaderUrl = info.uploaderUrl,
            thumbnailUrl = info.thumbnails.bestUrl(),
            durationSeconds = if (info.duration > 0) info.duration / 1000 else 0,
            viewCount = info.viewCount,
            uploadDate = info.textualUploadDate,
            streams = streams,
            hlsUrl = info.hlsUrl,
            subtitles = info.subtitles.mapNotNull { s ->
                runCatching {
                    SubtitleItem(
                        languageTag = s.languageTag,
                        url = s.content,
                        isAutoGenerated = s.isAutoGenerated,
                    )
                }.getOrNull()
            },
            related = info.relatedItems.mapNotNull { it.toVideoItem() },
        )
    }
}

@Singleton
class CommentsRepository @Inject constructor() {

    /** Top-level comments for a video; failures degrade to an empty list. */
    suspend fun commentsFor(videoUrl: String): List<CommentItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                CommentsInfo.getInfo(ServiceList.YouTube, videoUrl)
                    .relatedItems.mapNotNull { item ->
                        runCatching {
                            val c = item as CommentsInfoItem
                            CommentItem(
                                id = c.commentId ?: c.url ?: "",
                                author = c.uploaderName ?: "Unknown",
                                text = c.commentText.content,
                                likeCount = c.likeCount.toLong(),
                                publishedAgo = c.textualUploadDate,
                            )
                        }.getOrNull()
                    }
            }.getOrDefault(emptyList())
        }
}
