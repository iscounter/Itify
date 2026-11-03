package com.openreachtube.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.dao.FavoriteDao
import com.openreachtube.app.data.local.dao.HistoryDao
import com.openreachtube.app.data.local.dao.SubscriptionDao
import com.openreachtube.app.data.local.entity.FavoriteEntity
import com.openreachtube.app.data.local.entity.HistoryEntity
import com.openreachtube.app.data.local.entity.SubscriptionEntity
import com.openreachtube.app.data.network.SponsorBlockClient
import com.openreachtube.app.data.repository.CommentsRepository
import com.openreachtube.app.data.repository.StreamRepository
import com.openreachtube.app.domain.model.CommentItem
import com.openreachtube.app.domain.model.SponsorSegment
import com.openreachtube.app.domain.model.StreamBundle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val bundle: StreamBundle? = null,
    val sponsorSegments: List<SponsorSegment> = emptyList(),
    val comments: List<CommentItem> = emptyList(),
    val subscribed: Boolean = false,
    val liked: Boolean = false,
    val sponsorBlockEnabled: Boolean = true,
    val error: String? = null,
)

/**
 * Resolves streams, SponsorBlock segments, comments and subscription state for
 * one video; records watch history for the continue-playing rail.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamRepository: StreamRepository,
    private val sponsorBlockClient: SponsorBlockClient,
    private val commentsRepository: CommentsRepository,
    private val historyDao: HistoryDao,
    private val subscriptionDao: SubscriptionDao,
    private val favoriteDao: FavoriteDao,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    fun load(videoUrl: String) {
        viewModelScope.launch {
            _state.value = PlayerUiState(loading = true)
            runCatching { streamRepository.resolve(videoUrl) }
                .onSuccess { bundle ->
                    _state.value = _state.value.copy(
                        loading = false,
                        bundle = bundle,
                        error = null,
                        subscribed = runCatching {
                            subscriptionDao.all().any { it.channelUrl == bundle.uploaderUrl }
                        }.getOrDefault(false),
                        liked = runCatching {
                            favoriteDao.all().any { it.videoId == bundle.videoId }
                        }.getOrDefault(false),
                    )
                    recordWatch(bundle.videoId, bundle.title, bundle.uploader, bundle.thumbnailUrl)
                    launch {
                        _state.value = _state.value.copy(
                            sponsorSegments = runCatching {
                                sponsorBlockClient.segmentsFor(extractVideoId(bundle.videoId))
                            }.getOrDefault(emptyList()),
                        )
                    }
                    launch {
                        _state.value = _state.value.copy(
                            comments = runCatching {
                                commentsRepository.commentsFor(bundle.videoId)
                            }.getOrDefault(emptyList()),
                        )
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun recordWatch(videoId: String, title: String, uploader: String, thumbnailUrl: String?) {
        viewModelScope.launch {
            runCatching {
                historyDao.upsert(
                    HistoryEntity(
                        videoId = videoId,
                        title = title,
                        uploader = uploader,
                        thumbnailUrl = thumbnailUrl,
                    ),
                )
            }
        }
    }

    /** Persists playback position for the continue-playing progress bar. */
    fun savePosition(videoId: String, positionSeconds: Long, durationSeconds: Long) {
        val bundle = _state.value.bundle ?: return
        viewModelScope.launch {
            runCatching {
                historyDao.upsert(
                    HistoryEntity(
                        videoId = videoId,
                        title = bundle.title,
                        uploader = bundle.uploader,
                        thumbnailUrl = bundle.thumbnailUrl,
                        positionSeconds = positionSeconds,
                        durationSeconds = durationSeconds,
                    ),
                )
            }
        }
    }

    fun toggleSubscribe() {
        val bundle = _state.value.bundle ?: return
        val uploaderUrl = bundle.uploaderUrl ?: return
        viewModelScope.launch {
            val isSubscribed = runCatching {
                subscriptionDao.all().any { it.channelUrl == uploaderUrl }
            }.getOrDefault(false)
            if (isSubscribed) {
                subscriptionDao.remove(channelIdOf(uploaderUrl))
            } else {
                runCatching {
                    subscriptionDao.upsert(
                        SubscriptionEntity(
                            channelId = channelIdOf(uploaderUrl),
                            channelUrl = uploaderUrl,
                            name = bundle.uploader,
                            avatarUrl = null,
                        ),
                    )
                }
            }
            _state.value = _state.value.copy(subscribed = !isSubscribed)
        }
    }

    /** Toggles the video in local favorites (the "Liked videos" library rail). */
    fun toggleLike() {
        val bundle = _state.value.bundle ?: return
        viewModelScope.launch {
            val isLiked = runCatching {
                favoriteDao.all().any { it.videoId == bundle.videoId }
            }.getOrDefault(false)
            if (isLiked) {
                favoriteDao.remove(bundle.videoId)
            } else {
                runCatching {
                    favoriteDao.upsert(
                        FavoriteEntity(
                            videoId = bundle.videoId,
                            title = bundle.title,
                            uploader = bundle.uploader,
                            thumbnailUrl = bundle.thumbnailUrl,
                        ),
                    )
                }
            }
            _state.value = _state.value.copy(liked = !isLiked)
        }
    }

    private fun channelIdOf(channelUrl: String): String =
        channelUrl.substringAfter("channel/").ifEmpty { channelUrl }
}

/** Bare video id from any watch URL form (watch?v=, youtu.be/, raw id). */
internal fun extractVideoId(videoUrl: String): String = when {
    videoUrl.contains("watch?v=") -> videoUrl.substringAfter("watch?v=").substringBefore('&')
    videoUrl.contains("youtu.be/") -> videoUrl.substringAfter("youtu.be/").substringBefore('?')
    videoUrl.contains("/shorts/") -> videoUrl.substringAfter("/shorts/").substringBefore('?')
    else -> videoUrl
}
