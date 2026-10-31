package com.openreachtube.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.FavoriteDao
import com.openreachtube.app.data.local.HistoryDao
import com.openreachtube.app.data.local.LocalPlaylistDao
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(
    val liked: List<VideoItem> = emptyList(),
    val history: List<VideoItem> = emptyList(),
    val playlists: List<String> = emptyList(),
)

/** The "You" tab: liked videos, history, local playlists, downloads entry. */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    favoriteDao: FavoriteDao,
    historyDao: HistoryDao,
    playlistDao: LocalPlaylistDao,
) : ViewModel() {

    val state: StateFlow<LibraryUiState> = kotlinx.coroutines.flow.combine(
        favoriteDao.allAsFlow(),
        historyDao.recentAsFlow(),
        playlistDao.allAsFlow(),
    ) { favs, history, playlists ->
        LibraryUiState(
            liked = favs.map { it.toDomain() },
            history = history.map { it.toDomain() },
            playlists = playlists.map { it.title },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
