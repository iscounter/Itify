package com.openreachtube.app.ui.screens.shorts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.openreachtube.app.domain.model.VideoItem

@Composable
fun ShortsScreen(viewModel: ShortsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val error = state.error

    when {
        state.items.isEmpty() && error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Couldn't load Shorts",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    OutlinedButton(
                        onClick = { viewModel.loadMore(null) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("Retry") }
                }
            }
        }
        state.items.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        else -> {
            val pagerState = rememberPagerState(pageCount = { state.items.size })
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val short = state.items.getOrNull(page) ?: return@VerticalPager
                if (page >= state.items.size - 2) viewModel.loadMore(state.continuation)
                ShortPage(short)
            }
        }
    }
}

@Composable
private fun ShortPage(short: VideoItem) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.padding(bottom = 96.dp)) {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = short.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                short.title,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomStart)
                    .padding(16.dp),
            )
        }
    }
}
