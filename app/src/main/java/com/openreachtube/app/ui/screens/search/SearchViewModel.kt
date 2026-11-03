package com.openreachtube.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.SearchHistoryDao
import com.openreachtube.app.data.repository.SearchRepository
import com.openreachtube.app.domain.model.SearchTab
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val tab: SearchTab = SearchTab.ALL,
    val loading: Boolean = false,
    val results: List<VideoItem> = emptyList(),
    val recentSearches: List<String> = emptyList(),
)

/** Search across All / Videos / Channels / Shorts with persisted recent queries. */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val searchHistoryDao: SearchHistoryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow.debounce(350).collect { runSearch(it) }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(recentSearches = searchHistoryDao.recent(10))
        }
    }

    fun onQueryChanged(q: String) {
        _state.value = _state.value.copy(query = q)
        queryFlow.value = q
    }

    fun onTabChanged(tab: SearchTab) {
        _state.value = _state.value.copy(tab = tab)
        viewModelScope.launch { runSearch(_state.value.query) }
    }

    private suspend fun runSearch(query: String) {
        if (query.isBlank()) return
        _state.value = _state.value.copy(loading = true)
        runCatching { searchRepository.search(query, _state.value.tab) }
            .onSuccess { results ->
                _state.value = _state.value.copy(loading = false, results = results)
                searchHistoryDao.insert(query)
            }
            .onFailure { _state.value = _state.value.copy(loading = false) }
    }
}
