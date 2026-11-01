package com.openreachtube.app.ui.screens.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.repository.ShortsRepository
import com.openreachtube.app.data.repository.StreamRepository
import com.openreachtube.app.domain.model.ShortsFeedResult
import com.openreachtube.app.domain.model.StreamBundle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Vertical Shorts feed with continuation support and per-page stream resolve. */
@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val shortsRepository: ShortsRepository,
    private val streamRepository: StreamRepository,
) : ViewModel() {

    private val _feed = MutableStateFlow(ShortsFeedResult(emptyList()))
    val feed: StateFlow<ShortsFeedResult> = _feed

    private val _playing = MutableStateFlow<StreamBundle?>(null)
    val playing: StateFlow<StreamBundle?> = _playing

    private var loading = false

    init { loadMore() }

    fun loadMore() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            runCatching { shortsRepository.shortsFeed(_feed.value.continuation) }
                .onSuccess { result ->
                    _feed.value = ShortsFeedResult(
                        items = _feed.value.items + result.items,
                        continuation = result.nextPageUrl,
                    )
                }
            loading = false
        }
    }

    fun resolve(videoUrl: String) {
        if (_playing.value?.videoId == videoUrl) return
        _playing.value = null
        viewModelScope.launch {
            runCatching { streamRepository.resolve(videoUrl) }
                .onSuccess { _playing.value = it }
        }
    }
}
