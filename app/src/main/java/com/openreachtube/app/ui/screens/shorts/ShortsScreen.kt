package com.openreachtube.app.ui.screens.shorts

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.openreachtube.app.domain.model.StreamBundle
import com.openreachtube.app.domain.model.VideoItem

/** Vertical Shorts feed; the settled page resolves its stream and autoplays. */
@Composable
fun ShortsScreen(viewModel: ShortsViewModel = hiltViewModel()) {
    val feed by viewModel.feed.collectAsState()
    val playing by viewModel.playing.collectAsState()
    val context = LocalContext.current

    val pagerState = rememberPagerState(pageCount = { feed.items.size })
    val currentPage by remember {
        derivedStateOf { pagerState.currentPage }
    }

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(currentPage, feed.items) {
        feed.items.getOrNull(currentPage)?.let { viewModel.resolve(it.id) }
    }

    LaunchedEffect(playing, feed.items.getOrNull(currentPage)?.id) {
        val current = feed.items.getOrNull(currentPage)
        val bundle = playing
        if (current != null && bundle != null && bundle.videoId == current.id) {
            val media = when (val hls = bundle.hlsUrl) {
                null -> MediaItem.Builder().setUri(Uri.parse(pickStreamUrl(bundle))).build()
                else -> MediaItem.Builder()
                    .setUri(Uri.parse(hls))
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
            }
            player.setMediaItem(media)
            player.prepare()
            player.playWhenReady = true
        } else {
            player.stop()
        }
    }

    if (feed.items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        val short = feed.items.getOrNull(page) ?: return@VerticalPager
        if (page >= feed.items.size - 2) viewModel.loadMore()
        ShortPage(
            short = short,
            isActive = page == currentPage,
            bundle = playing.takeIf { it?.videoId == short.id },
            player = player,
        )
    }
}

@Composable
private fun ShortPage(
    short: VideoItem,
    isActive: Boolean,
    bundle: StreamBundle?,
    player: ExoPlayer,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (isActive && bundle != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = short.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isActive && bundle == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        Text(
            short.title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }
}

/** Best standalone stream for a short: HLS if present, else best progressive. */
private fun pickStreamUrl(bundle: StreamBundle): String =
    bundle.streams
        .filter { !it.isAudioOnly && !it.isVideoOnly }
        .maxByOrNull { it.height }?.url
        ?: bundle.streams.firstOrNull()?.url.orEmpty()
