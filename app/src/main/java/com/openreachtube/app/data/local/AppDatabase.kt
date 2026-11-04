package com.openreachtube.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.openreachtube.app.data.local.dao.ChunkDao
import com.openreachtube.app.data.local.dao.FavoriteDao
import com.openreachtube.app.data.local.dao.FeedCacheDao
import com.openreachtube.app.data.local.dao.HistoryDao
import com.openreachtube.app.data.local.dao.LocalPlaylistDao
import com.openreachtube.app.data.local.dao.MissionDao
import com.openreachtube.app.data.local.dao.SearchHistoryDao
import com.openreachtube.app.data.local.dao.SubscriptionDao
import com.openreachtube.app.data.local.dao.UserInterestDao
import com.openreachtube.app.data.local.entity.BlacklistEntity
import com.openreachtube.app.data.local.entity.DownloadChunkEntity
import com.openreachtube.app.data.local.entity.DownloadMissionEntity
import com.openreachtube.app.data.local.entity.FavoriteEntity
import com.openreachtube.app.data.local.entity.FeedCacheEntity
import com.openreachtube.app.data.local.entity.HistoryEntity
import com.openreachtube.app.data.local.entity.LocalPlaylistEntity
import com.openreachtube.app.data.local.entity.LocalPlaylistVideoEntity
import com.openreachtube.app.data.local.entity.SearchHistoryEntity
import com.openreachtube.app.data.local.entity.SubscriptionEntity
import com.openreachtube.app.data.local.entity.UserInterestEntity

/** Rebuilt schema mirroring the eleven-entity design observed in the teardown. */
@Database(
    entities = [
        HistoryEntity::class,
        FavoriteEntity::class,
        SearchHistoryEntity::class,
        SubscriptionEntity::class,
        FeedCacheEntity::class,
        UserInterestEntity::class,
        BlacklistEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistVideoEntity::class,
        DownloadMissionEntity::class,
        DownloadChunkEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun feedCacheDao(): FeedCacheDao
    abstract fun userInterestDao(): UserInterestDao
    abstract fun localPlaylistDao(): LocalPlaylistDao
    abstract fun missionDao(): MissionDao
    abstract fun chunkDao(): ChunkDao

    companion object { const val NAME = "openreachtube.db" }
}
