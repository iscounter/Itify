package com.openreachtube.app.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.openreachtube.app.data.network.UpdateChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

/** Polls the GitHub releases feed for a newer APK (phase 6 wires the installer). */
@HiltWorker
class AppUpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateChecker: UpdateChecker,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val latest = updateChecker.latest() ?: return Result.retry()
        // TODO(phase-6): compare with BuildConfig.VERSION_NAME, download asset via
        // the download service, then request package install.
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "update_check", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(12, TimeUnit.HOURS).build(),
            )
        }
    }
}

/** Applies cache policies: trims HTTP + feed caches; Smart Cleanup hooks in phase 4. */
@HiltWorker
class CacheCleanerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        applicationContext.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "cache_clean", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CacheCleanerWorker>(3, TimeUnit.DAYS).build(),
            )
        }
    }
}

/** Re-enqueues periodic work after reboot (RECEIVE_BOOT_COMPLETED). */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AppUpdateCheckWorker.schedule(context)
            CacheCleanerWorker.schedule(context)
        }
    }
}
