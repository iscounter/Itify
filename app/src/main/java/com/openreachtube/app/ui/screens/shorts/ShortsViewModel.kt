package com.openreachtube.app.ui.screens.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.repository.ShortsRepository
import com.openreachtube.app.domain.model.ShortsFeedResult
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Vertical Shorts feed with continuation support. */
@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val shortsRepository: ShortsRepository,
) : ViewModel() {

    private val _feed = MutableStateFlow(ShortsFeedResult(emptyList()))
    val feed: StateFlow<ShortsFeedResult> = _feed

    init { loadMore(null) }

    fun loadMore(continuation: String?) {
        viewModelScope.launch {
            runCatching { shortsRepository.shortsFeed(continuation) }
                .onSuccess { result ->
                    _feed.value = ShortsFeedResult(
                        items = _feed.value.items + result.items,
                        continuation = result.nextPageUrl,
                    )
                }
        }
    }

    fun onShortViewed(item: VideoItem) {
        // TODO(phase-3): record interest signal for For You ranking.
    }
}
