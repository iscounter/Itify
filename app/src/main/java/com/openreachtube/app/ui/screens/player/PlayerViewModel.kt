package com.openreachtube.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.network.PoTokenEngine
import com.openreachtube.app.data.network.SponsorBlockClient
import com.openreachtube.app.data.repository.StreamRepository
import com.openreachtube.app.domain.model.SponsorSegment
import com.openreachtube.app.domain.model.StreamBundle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val loading: Boolean = true,
    val bundle: StreamBundle? = null,
    val sponsorSegments: List<SponsorSegment> = emptyList(),
    val error: String? = null,
)

/** Resolves streams (with poToken) and SponsorBlock segments for one video. */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamRepository: StreamRepository,
    private val sponsorBlockClient: SponsorBlockClient,
    private val poTokenEngine: PoTokenEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    fun load(videoUrl: String) {
        viewModelScope.launch {
            _state.value = PlayerUiState(loading = true)
            runCatching { streamRepository.resolve(videoUrl, poTokenEngine) }
                .onSuccess { bundle ->
                    _state.value = PlayerUiState(
                        loading = false,
                        bundle = bundle,
                        sponsorSegments = runCatching {
                            sponsorBlockClient.segmentsFor(bundle.videoId)
                        }.getOrDefault(emptyList()),
                    )
                }
                .onFailure {
                    _state.value = PlayerUiState(loading = false, error = it.message)
                }
        }
    }
}
