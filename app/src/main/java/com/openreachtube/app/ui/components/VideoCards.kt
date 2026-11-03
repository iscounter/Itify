package com.openreachtube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.openreachtube.app.domain.model.VideoItem
import com.openreachtube.app.ui.theme.ZRed
import com.openreachtube.app.ui.theme.ZTextMuted

/** Shared video UI primitives (measured component set of the original client). */

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Circular channel avatar: network image when available, initial letter otherwise. */
@Composable
fun ChannelAvatar(name: String, avatarUrl: String?, size: Dp) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Surface(
            shape = CircleShape,
            color = ZRed,
            modifier = Modifier.size(size),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.trim().take(1).uppercase().ifEmpty { "?" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value / 2.2f).sp,
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(video: VideoItem, width: Dp, height: Dp) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        video.thumbnailUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width, height),
            )
        }
        if (video.durationSeconds > 0) {
            Text(
                formatDuration(video.durationSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        if (video.progressSeconds > 0) {
            val fraction = if (video.durationSeconds > 0) {
                (video.progressSeconds.toFloat() / video.durationSeconds).coerceIn(0f, 1f)
            } else 0f
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .background(ZRed),
            )
        }
    }
}

/** Full-width list row: thumbnail + title + channel + meta (the main list cell). */
@Composable
fun VideoRow(video: VideoItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (video.isChannel) {
            ChannelAvatar(video.title, video.thumbnailUrl, size = 52.dp)
        } else {
            Thumbnail(video, width = 148.dp, height = 84.dp)
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                video.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (video.isChannel) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                if (video.isChannel) "Channel"
                else listOfNotNull(
                    video.uploader.takeIf { it.isNotBlank() },
                    formatViews(video.viewCount).takeIf { it.isNotBlank() },
                    video.uploadDate?.takeIf { it.isNotBlank() },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = ZTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** Horizontal-rail card (history / rail carousels). */
@Composable
fun RailCard(video: VideoItem, onClick: () -> Unit) {
    Column(
        Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
    ) {
        Thumbnail(video, width = 200.dp, height = 112.dp)
        Text(
            video.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
        Text(
            video.uploader,
            style = MaterialTheme.typography.bodySmall,
            color = ZTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp, start = 2.dp),
        )
    }
}

/** "1.2K views" style counters; empty string when the count is unknown (-1). */
fun formatViews(count: Long): String = when {
    count < 0 -> ""
    count >= 1_000_000_000 -> "${"%.1f".format(count / 1_000_000_000.0)}B views"
    count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000.0)}M views"
    count >= 1_000 -> "${"%.1f".format(count / 1_000.0)}K views"
    count == 1L -> "1 view"
    else -> "$count views"
}

/** mm:ss (or h:mm:ss above one hour) for badges and meta rows. */
fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "0:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
