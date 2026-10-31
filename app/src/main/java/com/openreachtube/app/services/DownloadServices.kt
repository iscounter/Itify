package com.openreachtube.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.openreachtube.app.data.local.dao.MissionDao
import com.openreachtube.app.data.local.entity.DownloadMissionEntity
import com.openreachtube.app.download.ChunkedDownloader
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * dataSync foreground services for video and M4A audio download missions
 * (mirrors the measured two-service topology). Missions are passed as primitive
 * extras and re-materialized into Room for crash-resumable transfers.
 */
sealed class DownloadServiceBase(
    private val channelId: String,
    private val notifTitle: String,
) : Service() {

    @Inject lateinit var downloader: ChunkedDownloader
    @Inject lateinit var missionDao: MissionDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var nextId = 1000

    protected fun downloadDir(): File = File(filesDir, "downloads").apply { mkdirs() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        val videoId = intent?.getStringExtra(EXTRA_VIDEO_ID) ?: ""
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "download"
        val audioOnly = intent?.getBooleanExtra(EXTRA_AUDIO_ONLY, false) ?: false
        if (url.isNullOrBlank()) { stopSelf(); return START_NOT_STICKY }

        ensureChannel(this, channelId, notifTitle)
        startForeground(nextId++, progressNotification(0))

        val mission = DownloadMissionEntity(
            missionId = UUID.randomUUID().toString(),
            videoId = videoId,
            title = title,
            url = url,
            isAudioOnly = audioOnly,
        )
        scope.launch {
            val ext = if (audioOnly) ".m4a" else ".mp4"
            val target = File(downloadDir(), "${mission.missionId}$ext")
            runCatching { downloader.enqueue(mission, target) }
                .onSuccess { missionDao.setStatus(mission.missionId, "DONE") }
                .onFailure { missionDao.setStatus(mission.missionId, "FAILED") }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun progressNotification(progress: Int) =
        NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(notifTitle)
            .setContentText("Downloading")
            .setOngoing(true)
            .build()

    private fun ensureChannel(context: Context, id: String, name: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_VIDEO_ID = "videoId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AUDIO_ONLY = "audioOnly"

        fun videoIntent(context: Context, videoId: String, title: String, url: String) =
            Intent(context, VideoDownloadService::class.java)
                .putExtra(EXTRA_URL, url).putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_AUDIO_ONLY, false)

        fun audioIntent(context: Context, videoId: String, title: String, url: String) =
            Intent(context, AudioDownloadService::class.java)
                .putExtra(EXTRA_URL, url).putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title).putExtra(EXTRA_AUDIO_ONLY, true)
    }
}

@AndroidEntryPoint
class VideoDownloadService : DownloadServiceBase("downloads_video", "Video download")

@AndroidEntryPoint
class AudioDownloadService : DownloadServiceBase("downloads_audio", "Audio download")
