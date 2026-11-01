package com.openreachtube.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.dao.SubscriptionDao
import com.openreachtube.app.data.repository.VideoRepository
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val loading: Boolean = true,
    val items: List<VideoItem> = emptyList(),
    val empty: Boolean = false,
)

/** In-app notification feed: latest uploads from subscribed channels. */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val subscriptionDao: SubscriptionDao,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val subs = runCatching { subscriptionDao.all() }.getOrDefault(emptyList())
            if (subs.isEmpty()) {
                _state.value = NotificationsUiState(loading = false, empty = true)
                return@launch
            }
            _state.value = NotificationsUiState(loading = true)
            val items = runCatching {
                videoRepository.subscriptionsFeed(subs.map { it.channelUrl })
            }.getOrDefault(emptyList())
            _state.value = NotificationsUiState(loading = false, items = items, empty = items.isEmpty())
        }
    }
}
