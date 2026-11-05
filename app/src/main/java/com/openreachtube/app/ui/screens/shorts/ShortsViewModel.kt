package com.openreachtube.app.ui.screens.shorts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.repository.ShortsRepository
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShortsUiState(
    val items: List<VideoItem> = emptyList(),
    val continuation: String? = null,
    val loading: Boolean = false,
    /** Real cause when the feed fetch failed; shown with a retry. */
    val error: String? = null,
)

/** Vertical Shorts feed with continuation support. */
@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val shortsRepository: ShortsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShortsUiState())
    val state: StateFlow<ShortsUiState> = _state

    init { loadMore(null) }

    fun loadMore(continuation: String?) {
        if (_state.value.loading) return
        // The feed ends when the extractor returns no continuation; reloading
        // the first page over an existing feed would just re-fetch the same items.
        if (continuation == null && _state.value.items.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { shortsRepository.shortsFeed(continuation) }
                .onSuccess { result ->
                    val existing = _state.value.items.map { it.id }.toSet()
                    val newItems = result.items.filter { it.id !in existing }
                    if (newItems.isEmpty() && _state.value.items.isEmpty()) {
                        // first fetch came back empty (no exception): never leave
                        // the pager spinning — surface it as an error state
                        _state.value = ShortsUiState(
                            items = emptyList(),
                            continuation = result.nextPageUrl,
                            loading = false,
                            error = "No shorts found right now",
                        )
                    } else {
                        _state.value = ShortsUiState(
                            items = _state.value.items + newItems,
                            continuation = result.nextPageUrl,
                            loading = false,
                        )
                    }
                }
                .onFailure {
                    Log.e("ITIFY", "shorts feed crashed", it)
                    _state.value = _state.value.copy(
                        loading = false,
                        error = (it.message ?: it.javaClass.simpleName).take(160),
                    )
                }
        }
    }

    fun onShortViewed(item: VideoItem) {
        // TODO(phase-3): record interest signal for For You ranking.
    }
}
