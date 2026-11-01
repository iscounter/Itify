package com.openreachtube.app.di

import android.content.Context
import androidx.room.Room
import com.openreachtube.app.data.local.AppDatabase
import com.openreachtube.app.data.local.dao.ChunkDao
import com.openreachtube.app.data.local.dao.FavoriteDao
import com.openreachtube.app.data.local.dao.HistoryDao
import com.openreachtube.app.data.local.dao.LocalPlaylistDao
import com.openreachtube.app.data.local.dao.MissionDao
import com.openreachtube.app.data.local.dao.SearchHistoryDao
import com.openreachtube.app.data.local.dao.SubscriptionDao
import com.openreachtube.app.data.local.dao.UserInterestDao
import com.openreachtube.app.data.network.IPv4OnlyDns
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .dns(IPv4OnlyDns())
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun historyDao(db: AppDatabase): HistoryDao = db.historyDao()
    @Provides fun favoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun searchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()
    @Provides fun subscriptionDao(db: AppDatabase): SubscriptionDao = db.subscriptionDao()
    @Provides fun userInterestDao(db: AppDatabase): UserInterestDao = db.userInterestDao()
    @Provides fun localPlaylistDao(db: AppDatabase): LocalPlaylistDao = db.localPlaylistDao()
    @Provides fun missionDao(db: AppDatabase): MissionDao = db.missionDao()
    @Provides fun chunkDao(db: AppDatabase): ChunkDao = db.chunkDao()
}
