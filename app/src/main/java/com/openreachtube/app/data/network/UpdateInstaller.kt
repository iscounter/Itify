package com.openreachtube.app.data.network

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * In-app update flow (completes phase 6 of the teardown plan): polls the
 * GitHub releases feed and, when the published tag differs from the installed
 * versionName, downloads the first APK asset into the cache dir. Installing
 * itself is delegated to the Android package installer via a FileProvider
 * VIEW intent (REQUEST_INSTALL_PACKAGES is declared in the manifest).
 */
@Singleton
class UpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
    private val checker: UpdateChecker,
) {

    data class StagedUpdate(val tag: String, val notes: String?, val apk: File)

    /**
     * Checks the releases feed and stages the APK when a different version is
     * published. Returns null when the app is already on the released version.
     */
    suspend fun checkAndStage(): Result<StagedUpdate?> = withContext(Dispatchers.IO) {
        runCatching {
            val release = checker.latest() ?: return@runCatching null
            val installed = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull().orEmpty()
            val tag = release.tag_name.trim().removePrefix("v").removePrefix("V")
            if (tag.equals(installed, ignoreCase = true)) return@runCatching null
            val asset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@runCatching null

            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(dir, "update-$tag.apk")
            if (!apk.exists() || apk.length() == 0L) {
                http.newCall(Request.Builder().url(asset.browser_download_url).build())
                    .execute().use { resp ->
                        check(resp.isSuccessful) { "Update download failed: HTTP ${resp.code}" }
                        val body = resp.body ?: error("Update download returned an empty body")
                        body.byteStream().use { input ->
                            apk.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
            }
            StagedUpdate(release.tag_name, release.body, apk)
        }
    }

    /** Opens the staged APK through the system package installer. */
    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
