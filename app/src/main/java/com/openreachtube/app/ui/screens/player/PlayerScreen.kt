package com.openreachtube.app.ui.screens.player

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Compose wrapper around Media3 PlayerView. Long-running playback (background
 * audio / PiP) lives in PlaybackService; this screen resolves streams and drives
 * a local player instance for the foreground experience.
 */
@Composable
fun PlayerScreen(url: String, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(url) { viewModel.load(url) }

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(state.bundle) {
        val bundle = state.bundle
        if (bundle != null) {
            val best = bundle.streams.filter { !it.isAudioOnly }.maxByOrNull { it.height }
            val media = MediaItem.Builder()
                .setUri(Uri.parse(best?.url ?: url))
                .build()
            player.setMediaItem(media)
            player.prepare()
            player.playWhenReady = true
        }
        onDispose { player.release() }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error != null ->
                Text("Playback failed: ${state.error}", Modifier.align(Alignment.Center))
            else -> AndroidView(
                factory = { PlayerView(it).apply { this.player = player } },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }
    }
}
