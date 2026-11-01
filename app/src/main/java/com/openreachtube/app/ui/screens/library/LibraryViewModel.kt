package com.openreachtube.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openreachtube.app.data.local.dao.FavoriteDao
import com.openreachtube.app.data.local.dao.HistoryDao
import com.openreachtube.app.data.local.dao.LocalPlaylistDao
import com.openreachtube.app.data.local.dao.MissionDao
import com.openreachtube.app.domain.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LibraryUiState(
    val liked: List<VideoItem> = emptyList(),
    val history: List<VideoItem> = emptyList(),
    val playlists: List<String> = emptyList(),
    val downloads: Int = 0,
)

/** The "You" tab: watch history, liked videos, playlists and downloads count. */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    favoriteDao: FavoriteDao,
    historyDao: HistoryDao,
    playlistDao: LocalPlaylistDao,
    missionDao: MissionDao,
) : ViewModel() {

    val state: StateFlow<LibraryUiState> = combine(
        favoriteDao.allAsFlow(),
        historyDao.recentAsFlow(),
        playlistDao.allAsFlow(),
        missionDao.allAsFlow().map { missions ->
            missions.count { it.status == "DONE" || it.status == "RUNNING" }
        },
    ) { favs, history, playlists, downloads ->
        LibraryUiState(
            liked = favs.map { it.toDomain() },
            history = history.map { it.toDomain() },
            playlists = playlists.map { it.title },
            downloads = downloads,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
