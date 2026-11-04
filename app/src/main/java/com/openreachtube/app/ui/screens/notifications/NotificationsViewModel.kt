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

/** Notifications tab: the newest uploads across subscribed channels. */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            val subs = runCatching { subscriptionDao.all() }.getOrDefault(emptyList())
            if (subs.isEmpty()) {
                _state.value = NotificationsUiState(loading = false, empty = true)
                return@launch
            }
            val uploads = runCatching {
                videoRepository.subscriptionsFeed(subs.map { it.channelUrl })
            }.getOrDefault(emptyList())
            _state.value = NotificationsUiState(
                loading = false,
                items = uploads.take(50),
                empty = uploads.isEmpty(),
            )
        }
    }

    /** Re-pulls the subscriptions feed after an import or on pull-to-refresh. */
    fun refresh() = load()
}
