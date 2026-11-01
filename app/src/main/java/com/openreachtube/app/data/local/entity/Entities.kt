package com.openreachtube.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val positionSeconds: Long = 0,
    val durationSeconds: Long = 0,
    val watchedAt: Long = System.currentTimeMillis(),
) {
    fun toDomain() = com.openreachtube.app.domain.model.VideoItem(
        id = videoId, title = title, uploader = uploader, thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds, progressSeconds = positionSeconds,
    )
}

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun toDomain() = com.openreachtube.app.domain.model.VideoItem(
        id = videoId, title = title, uploader = uploader, thumbnailUrl = thumbnailUrl,
    )
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    val channelUrl: String,
    val name: String,
    val avatarUrl: String?,
    val subscribedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val channelId: String,
    val feedJson: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "user_interests")
data class UserInterestEntity(
    @PrimaryKey val keyword: String,
    val weight: Float = 1f,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "blacklist")
data class BlacklistEntity(
    @PrimaryKey val pattern: String,
    val type: String, // CHANNEL | KEYWORD
)

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_playlist_videos")
data class LocalPlaylistVideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val position: Int,
)

@Entity(tableName = "download_missions")
data class DownloadMissionEntity(
    @PrimaryKey val missionId: String,
    val videoId: String,
    val title: String,
    val url: String,
    val isAudioOnly: Boolean,
    val totalBytes: Long = 0,
    val status: String = "QUEUED", // QUEUED | RUNNING | DONE | FAILED | WAITING_NETWORK
)

@Entity(tableName = "download_chunks")
data class DownloadChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: String,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val downloadedBytes: Long = 0,
    val done: Boolean = false,
)
