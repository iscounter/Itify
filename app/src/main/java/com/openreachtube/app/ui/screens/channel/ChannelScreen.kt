package com.openreachtube.app.ui.screens.channel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.repository.VideoRepository
import com.openreachtube.app.domain.model.ChannelInfo
import com.openreachtube.app.domain.model.VideoItem
import com.openreachtube.app.ui.components.ChannelAvatar
import com.openreachtube.app.ui.components.VideoRow
import com.openreachtube.app.ui.components.formatViews
import com.openreachtube.app.ui.theme.ZRed
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChannelUiState(
    val loading: Boolean = true,
    val header: ChannelInfo? = null,
    val tab: String = "Videos",
    val videos: List<VideoItem> = emptyList(),
    val error: String? = null,
)

/** Channel page: header (avatar/name/subscribers/description) + content tabs. */
@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelUiState())
    val state: StateFlow<ChannelUiState> = _state

    private var channelUrl: String? = null

    fun load(url: String) {
        if (channelUrl == url && _state.value.header != null) return
        channelUrl = url
        viewModelScope.launch {
            _state.value = ChannelUiState(loading = true)
            runCatching { videoRepository.channelHeader(url) }
                .onSuccess { header ->
                    _state.value = _state.value.copy(loading = false, header = header, error = null)
                    selectTab(_state.value.tab)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    fun selectTab(tab: String) {
        val url = channelUrl ?: return
        _state.value = _state.value.copy(tab = tab)
        viewModelScope.launch {
            runCatching { videoRepository.channelTab(url, tab.lowercase()) }
                .onSuccess { videos -> _state.value = _state.value.copy(videos = videos) }
                .onFailure { _state.value = _state.value.copy(videos = emptyList()) }
        }
    }
}

private val CHANNEL_TABS = listOf("Videos", "Shorts")

@Composable
fun ChannelScreen(
    url: String,
    onOpenVideo: (String) -> Unit,
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(url) { viewModel.load(url) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Couldn't open this channel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    state.error ?: "Network error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = { viewModel.load(url) }, Modifier.padding(top = 8.dp)) {
                    Text("Retry")
                }
            }

            else -> {
                val header = state.header
                if (header != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChannelAvatar(header.name, header.avatarUrl, size = 56.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                header.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (header.subscriberCount >= 0) {
                                Text(
                                    "${formatViews(header.subscriberCount)} subscribers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (!header.description.isNullOrBlank()) {
                        Text(
                            header.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }

                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    CHANNEL_TABS.forEach { tab ->
                        val selected = tab == state.tab
                        TextButton(onClick = { viewModel.selectTab(tab) }) {
                            Text(
                                tab,
                                color = if (selected) ZRed else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.videos.size, key = { state.videos[it].id }) { i ->
                        val video = state.videos[i]
                        VideoRow(video) { onOpenVideo(video.id) }
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }
}
