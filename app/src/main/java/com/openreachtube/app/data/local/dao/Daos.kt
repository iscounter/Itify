package com.openreachtube.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.openreachtube.app.data.local.entity.DownloadChunkEntity
import com.openreachtube.app.data.local.entity.DownloadMissionEntity
import com.openreachtube.app.data.local.entity.FavoriteEntity
import com.openreachtube.app.data.local.entity.FeedCacheEntity
import com.openreachtube.app.data.local.entity.HistoryEntity
import com.openreachtube.app.data.local.entity.LocalPlaylistEntity
import com.openreachtube.app.data.local.entity.SearchHistoryEntity
import com.openreachtube.app.data.local.entity.SubscriptionEntity
import com.openreachtube.app.data.local.entity.UserInterestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 200")
    fun recentAsFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT positionSeconds FROM history WHERE videoId = :videoId")
    suspend fun positionFor(videoId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun allAsFlow(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE videoId = :videoId")
    suspend fun remove(videoId: String)
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT query FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY name")
    suspend fun all(): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId")
    suspend fun remove(channelId: String)
}

@Dao
interface FeedCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: FeedCacheEntity)

    @Query("SELECT * FROM feed_cache WHERE channelId = :channelId")
    suspend fun get(channelId: String): FeedCacheEntity?
}

@Dao
interface UserInterestDao {
    @Query("SELECT * FROM user_interests ORDER BY weight DESC LIMIT 50")
    suspend fun top(): List<UserInterestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserInterestEntity)
}

@Dao
interface LocalPlaylistDao {
    @Query("SELECT * FROM local_playlists ORDER BY createdAt DESC")
    fun allAsFlow(): Flow<List<LocalPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun create(entity: LocalPlaylistEntity): Long
}

@Dao
interface MissionDao {
    @Query("SELECT * FROM download_missions ORDER BY status")
    fun allAsFlow(): Flow<List<DownloadMissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mission: DownloadMissionEntity)

    @Query("UPDATE download_missions SET status = :status WHERE missionId = :missionId")
    suspend fun setStatus(missionId: String, status: String)
}

@Dao
interface ChunkDao {
    @Query("SELECT * FROM download_chunks WHERE missionId = :missionId ORDER BY chunkIndex")
    suspend fun chunksFor(missionId: String): List<DownloadChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chunk: DownloadChunkEntity)

    @Query("UPDATE download_chunks SET downloadedBytes = :bytes, done = :done WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, done: Boolean)
}
