package com.openreachtube.app.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LibraryScreen(onOpenVideo: (String) -> Unit, viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize()) {
        item { Text("You", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge) }
        item { Text("Liked Videos (${state.liked.size})", Modifier.padding(horizontal = 16.dp)) }
        items(state.liked.size) { i ->
            Text(state.liked[i].title, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        item { Text("History (${state.history.size})", Modifier.padding(16.dp)) }
        items(state.history.size) { i ->
            Text(state.history[i].title, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        item { Text("Playlists: ${state.playlists.joinToString()}", Modifier.padding(16.dp)) }
    }
}
