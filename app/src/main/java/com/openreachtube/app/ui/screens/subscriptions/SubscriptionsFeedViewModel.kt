package com.openreachtube.app.ui.screens.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.dao.SubscriptionDao
import com.openreachtube.app.data.local.entity.SubscriptionEntity
import com.openreachtube.app.data.repository.VideoRepository
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SubscriptionsUiState(
    val loading: Boolean = true,
    val channels: List<SubscriptionEntity> = emptyList(),
    val items: List<VideoItem> = emptyList(),
    val empty: Boolean = false,
    val error: String? = null,
)

/** Subscriptions feed backed by local SubscriptionDao + per-channel uploads. */
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
            val subs = runCatching { subscriptionDao.all() }.getOrDefault(emptyList())
            if (subs.isEmpty()) {
                _state.value = SubscriptionsUiState(loading = false, empty = true)
                return@launch
            }
            _state.value = SubscriptionsUiState(loading = true, channels = subs)
            runCatching { videoRepository.subscriptionsFeed(subs.map { it.channelUrl }) }
                .onSuccess {
                    _state.value = SubscriptionsUiState(
                        loading = false,
                        channels = subs,
                        items = it,
                        empty = false,
                    )
                }
                .onFailure {
                    _state.value = SubscriptionsUiState(
                        loading = false,
                        channels = subs,
                        empty = false,
                        error = it.message,
                    )
                }
        }
    }
}
