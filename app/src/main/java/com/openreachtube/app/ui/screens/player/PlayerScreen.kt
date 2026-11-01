package com.openreachtube.app.ui.screens.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.openreachtube.app.services.DownloadServiceBase
import com.openreachtube.app.ui.components.ChannelAvatar
import com.openreachtube.app.ui.components.SectionTitle
import com.openreachtube.app.ui.components.VideoRow
import com.openreachtube.app.ui.components.formatDuration
import com.openreachtube.app.ui.components.formatViews
import com.openreachtube.app.ui.theme.ZTextMuted
import kotlinx.coroutines.delay

/**
 * Foreground player: resolves the stream (HLS preferred, then progressive),
 * auto-skips SponsorBlock segments, records watch history for the continue
 * playing rail and lists comments + related videos.
 */
@Composable
fun PlayerScreen(url: String, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val bundle = state.bundle

    LaunchedEffect(url) { viewModel.load(url) }

    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(bundle) {
        if (bundle != null) {
            val hls = bundle.hlsUrl
            val progressive = bundle.streams
                .filter { !it.isAudioOnly && !it.isVideoOnly }
                .maxByOrNull { it.height }
            val media = when {
                hls != null -> MediaItem.Builder()
                    .setUri(Uri.parse(hls))
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build()
                progressive != null -> MediaItem.Builder().setUri(Uri.parse(progressive.url)).build()
                else -> MediaItem.Builder().setUri(Uri.parse(url)).build()
            }
            player.setMediaItem(media)
            player.prepare()
            player.playWhenReady = true
        }
    }

    if (state.sponsorBlockEnabled && state.sponsorSegments.isNotEmpty()) {
        LaunchedEffect(player, state.sponsorSegments) {
            while (true) {
                delay(500)
                val positionSeconds = player.currentPosition / 1000f
                val hit = state.sponsorSegments.firstOrNull {
                    positionSeconds >= it.startSeconds && positionSeconds < it.endSeconds
                }
                if (hit != null) player.seekTo((hit.endSeconds * 1000).toLong())
            }
        }
    }

    LaunchedEffect(bundle) {
        if (bundle != null) {
            while (true) {
                delay(5_000)
                if (player.isPlaying) {
                    viewModel.savePosition(
                        bundle.videoId,
                        player.currentPosition / 1000,
                        bundle.durationSeconds,
                    )
                }
            }
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (state.loading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }

        when {
            state.error != null -> item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Playback failed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        state.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZTextMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    TextButton(onClick = { viewModel.load(url) }, Modifier.padding(top = 8.dp)) {
                        Text("Retry")
                    }
                }
            }

            bundle != null -> {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            bundle.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            listOf(
                                formatViews(bundle.viewCount),
                                bundle.uploadDate ?: "",
                                if (bundle.durationSeconds > 0) formatDuration(bundle.durationSeconds) else "",
                            ).filter { it.isNotBlank() }.joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = ZTextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChannelAvatar(bundle.uploader, null, size = 40.dp)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(
                                    bundle.uploader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (bundle.uploaderUrl != null) {
                                if (state.subscribed) {
                                    OutlinedButton(onClick = viewModel::toggleSubscribe) {
                                        Text("Subscribed")
                                    }
                                } else {
                                    Button(
                                        onClick = viewModel::toggleSubscribe,
                                        colors = ButtonDefaults.buttonColors(),
                                    ) {
                                        Text("Subscribe")
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val downloadUrl = bundle.streams
                                .filter { !it.isAudioOnly && !it.isVideoOnly }
                                .maxByOrNull { it.height }?.url
                                ?: bundle.streams.firstOrNull { it.isAudioOnly }?.url
                            AssistChip(
                                onClick = {
                                    downloadUrl?.let { streamUrl ->
                                        context.startForegroundService(
                                            DownloadServiceBase.videoIntent(
                                                context,
                                                extractVideoId(bundle.videoId),
                                                bundle.title,
                                                streamUrl,
                                            ),
                                        )
                                    }
                                },
                                label = { Text("Download") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Download, contentDescription = null)
                                },
                            )
                            AssistChip(
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, bundle.videoId)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(send, "Share video"),
                                    )
                                },
                                label = { Text("Share") },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            )
                        }
                    }
                }

                item { SectionTitle("Comments") }
                if (state.comments.isEmpty()) {
                    item {
                        Text(
                            "No comments loaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZTextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    items(state.comments.size, key = { state.comments[it].id }) { i ->
                        val comment = state.comments[i]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ChannelAvatar(comment.author, null, size = 32.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    comment.author,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    buildString {
                                        if (comment.likeCount >= 0) append("${comment.likeCount} likes")
                                        comment.publishedAgo?.let {
                                            if (isNotEmpty()) append("  ·  ")
                                            append(it)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZTextMuted,
                                )
                            }
                        }
                    }
                }

                if (bundle.related.isNotEmpty()) {
                    item { SectionTitle("Up next") }
                    items(bundle.related.size, key = { bundle.related[it].id }) { i ->
                        val related = bundle.related[i]
                        VideoRow(related) { viewModel.load(related.id) }
                    }
                }

                item { Spacer(Modifier.height(64.dp)) }
            }
        }
    }
}
