package com.openreachtube.app.data.repository

import android.util.Log
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

/** Single logcat tag for every data-layer failure; filter with `adb logcat -s ITIFY`. */
private fun logFailure(where: String, t: Throwable) {
    Log.e("ITIFY", "$where: ${t.javaClass.simpleName}: ${t.message}", t)
}

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
                isShort = s.isShortFormContent || s.duration in 1..SHORT_MAX_MILLIS,
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
}.onFailure { logFailure("map $infoType", it) }.getOrNull()

private const val SHORT_MAX_MILLIS = 3 * 60 * 1000L

@Singleton
class VideoRepository @Inject constructor(
    private val userInterestDao: UserInterestDao,
) {
    data class HomeFeed(
        val forYou: List<VideoItem> = emptyList(),
        val latest: List<VideoItem> = emptyList(),
        val trending: List<VideoItem> = emptyList(),
        /** Non-null when the kiosk fetch itself failed; surfaced in the UI. */
        val error: String? = null,
    )

    /**
     * Home feed: the "Trending" kiosk when YouTube still serves it (removed from
     * YouTube's interface in July 2025, so it may come back empty), falling back
     * to the service default kiosk ("live" in extractor v0.26+). Kiosk extractors
     * must be resolved by ID via KioskList — getInfo(service, kioskId) would
     * instead try to match the bare id as a URL and fail.
     */
    suspend fun loadHomeFeed(): HomeFeed = withContext(Dispatchers.IO) {
        val kioskList = ServiceList.YouTube.kioskList
        val trendingResult = runCatching {
            KioskInfo.getInfo(kioskList.getExtractorById("Trending", null))
                .relatedItems.mapNotNull { it.toVideoItem() }
        }.onFailure { logFailure("trending kiosk", it) }
        val trending = trendingResult.getOrElse { emptyList() }
        if (trending.isNotEmpty()) {
            return@withContext HomeFeed(
                forYou = trending,
                latest = trending.sortedByDescending { it.uploadDate ?: "" },
                trending = trending,
            )
        }

        val fallbackResult = runCatching {
            KioskInfo.getInfo(kioskList.getDefaultKioskExtractor(null))
                .relatedItems.mapNotNull { it.toVideoItem() }
        }.onFailure { logFailure("default kiosk (live)", it) }
        val fallback = fallbackResult.getOrElse { emptyList() }
        HomeFeed(
            forYou = fallback,
            latest = fallback.sortedByDescending { it.uploadDate ?: "" },
            trending = fallback,
            error = if (fallback.isEmpty()) {
                val cause = trendingResult.exceptionOrNull()?.message
                    ?: fallbackResult.exceptionOrNull()?.message
                (cause ?: "Trending is unavailable and the default kiosk returned no videos")
                    .take(160)
            } else null,
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
                        }.onFailure { logFailure("subscription channel $url", it) }
                            .getOrDefault(emptyList())
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
            }.onFailure { logFailure("channel tab $tabFilter", it) }
                .getOrDefault(emptyList())
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
                    SearchTab.VIDEOS -> "videos"
                    SearchTab.CHANNELS -> "channels"
                    // "shorts" is not a valid extractor content filter; the Shorts
                    // tab searches unfiltered and keeps short-form results only.
                    else -> ""
                }
                val handler = ServiceList.YouTube.searchQHFactory.fromQuery(
                    query, listOf(contentFilter), "",
                )
                val items = SearchInfo.getInfo(ServiceList.YouTube, handler)
                    .relatedItems.mapNotNull { it.toVideoItem() }
                if (tab == SearchTab.SHORTS) items.filter { it.isShort } else items
            }.onFailure { logFailure("search", it) }.getOrDefault(emptyList())
        }
}

@Singleton
class ShortsRepository @Inject constructor() {

    /**
     * Vertical Shorts feed with continuation. The extractor has no "shorts"
     * search filter, so the unfiltered web search is used and short-form items
     * (reel/lockup renderers or duration ≤ 3 min) are kept; if a page has none,
     * its regular results are shown rather than an empty feed.
     */
    suspend fun shortsFeed(continuation: String?): PaginatedList<VideoItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val service = ServiceList.YouTube
                val handler = service.searchQHFactory.fromQuery("shorts", listOf(""), "")
                val items: List<VideoItem>
                val next: Page?
                if (continuation == null) {
                    val info = SearchInfo.getInfo(service, handler)
                    items = info.relatedItems.mapNotNull { it.toVideoItem() }
                    next = info.nextPage
                } else {
                    val page = SearchInfo.getMoreItems(service, handler, Page(continuation))
                    items = page.items.mapNotNull { it.toVideoItem() }
                    next = page.nextPage
                }
                val shortsOnly = items.filter { it.isShort }
                PaginatedList(shortsOnly.ifEmpty { items }, next?.url)
            }.onFailure { logFailure("shorts feed", it) }
                // rethrow so the Shorts screen shows the cause instead of
                // spinning forever on a silently empty result
                .getOrThrow()
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
                        }.onFailure { logFailure("comment item", it) }.getOrNull()
                    }
            }.onFailure { logFailure("comments", it) }.getOrDefault(emptyList())
        }
}
