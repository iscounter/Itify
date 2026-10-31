package com.openreachtube.app.ui.screens.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.SubscriptionDao
import com.openreachtube.app.data.repository.VideoRepository
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SubscriptionsUiState(
    val loading: Boolean = true,
    val items: List<VideoItem> = emptyList(),
    val empty: Boolean = false,
)

/** Subscriptions feed backed by local SubscriptionDao + FeedCache for offline. */
@HiltViewModel
class SubscriptionsFeedViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val subscriptionDao: SubscriptionDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionsUiState())
    val state: StateFlow<SubscriptionsUiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val subs = subscriptionDao.all()
            if (subs.isEmpty()) {
                _state.value = SubscriptionsUiState(loading = false, empty = true)
                return@launch
            }
            runCatching { videoRepository.subscriptionsFeed(subs.map { it.channelUrl }) }
                .onSuccess { _state.value = SubscriptionsUiState(false, it, false) }
                .onFailure { _state.value = SubscriptionsUiState(false, emptyList(), subs.isEmpty()) }
        }
    }
}
