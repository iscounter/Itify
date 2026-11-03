package com.openreachtube.app.ui.screens.shorts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import coil3.compose.AsyncImage
import com.openreachtube.app.domain.model.VideoItem

@Composable
fun ShortsScreen(viewModel: ShortsViewModel = hiltViewModel()) {
    val feed by viewModel.feed.collectAsState()
    val pagerState = rememberPagerState(pageCount = { feed.items.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val short = feed.items.getOrNull(page) ?: return@VerticalPager
        if (page >= feed.items.size - 2) viewModel.loadMore(feed.continuation)
        ShortPage(short)
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
