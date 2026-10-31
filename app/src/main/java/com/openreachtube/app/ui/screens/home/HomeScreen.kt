package com.openreachtube.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.openreachtube.app.domain.model.VideoItem

@Composable
fun HomeScreen(onOpenVideo: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Discover something new",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
        }
        items(state.forYou, key = { it.id }) { video ->
            VideoRow(video) { onOpenVideo(video.id) }
        }
        if (state.offline) {
            item { Text("No Internet Connection", Modifier.padding(16.dp)) }
        }
    }
}

@Composable
internal fun VideoRow(video: VideoItem, onClick: () -> Unit) {
    Surface(onClick = onClick, tonalElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.size(width = 140.dp, height = 79.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(video.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(video.uploader, style = MaterialTheme.typography.bodySmall)
                Text(
                    listOfNotNull(
                        if (video.viewCount >= 0) "${video.viewCount} views" else null,
                        video.uploadDate,
                    ).joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
