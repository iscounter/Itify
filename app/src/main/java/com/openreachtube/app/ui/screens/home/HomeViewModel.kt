package com.openreachtube.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.repository.VideoRepository
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val forYou: List<VideoItem> = emptyList(),
    val latest: List<VideoItem> = emptyList(),
    val trending: List<VideoItem> = emptyList(),
    val offline: Boolean = false,
)

/** Home feed: For You (local interest ranking), trending and latest rails. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { videoRepository.loadHomeFeed() }
                .onSuccess { feed ->
                    _state.value = HomeUiState(
                        loading = false,
                        forYou = videoRepository.rankByInterest(feed.trending),
                        latest = feed.latest,
                        trending = feed.trending,
                    )
                }
                .onFailure { _state.value = HomeUiState(loading = false, offline = true) }
        }
    }
}
