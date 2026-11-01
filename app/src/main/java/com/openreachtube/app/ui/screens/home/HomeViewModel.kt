package com.openreachtube.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.dao.HistoryDao
import com.openreachtube.app.data.repository.VideoRepository
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val categories: List<String> = listOf("All", "Music", "Gaming", "News", "Movies"),
    val selectedCategory: String = "All",
    val trending: List<VideoItem> = emptyList(),
    val forYou: List<VideoItem> = emptyList(),
    val continuePlaying: List<VideoItem> = emptyList(),
    val error: String? = null,
)

/**
 * Home feed: category chips backed by YouTube kiosks, a trending feed, a
 * "Continue Playing" rail from local watch history and an interest-ranked
 * "For You" selection of the same feed.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    historyDao: HistoryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private val continuePlaying = historyDao.recentAsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(continuePlaying, _state) { history, home ->
                home.copy(
                    continuePlaying = history
                        .filter { it.positionSeconds > 10 && it.durationSeconds > it.positionSeconds }
                        .map { it.toDomain() },
                )
            }.collect { _state.value = it }
        }
        viewModelScope.launch {
            val categories = videoRepository.availableCategories()
            _state.value = _state.value.copy(categories = categories)
        }
        refresh()
    }

    fun selectCategory(category: String) {
        if (category == _state.value.selectedCategory) return
        _state.value = _state.value.copy(selectedCategory = category)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val category = _state.value.selectedCategory
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { videoRepository.loadCategory(category) }
                .onSuccess { feed ->
                    val ranked = videoRepository.rankByInterest(feed)
                    _state.value = _state.value.copy(
                        loading = false,
                        trending = feed,
                        forYou = ranked.take(12),
                    )
                    videoRepository.recordInterest(category)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }
}
