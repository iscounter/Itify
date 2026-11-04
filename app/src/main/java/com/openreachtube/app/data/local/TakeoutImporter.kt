package com.openreachtube.app.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.openreachtube.app.data.local.dao.HistoryDao
import com.openreachtube.app.data.local.dao.SubscriptionDao
import com.openreachtube.app.data.local.entity.HistoryEntity
import com.openreachtube.app.data.local.entity.SubscriptionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports Google-Takeout-style CSV exports (watch history and subscriptions),
 * optionally inside a ZIP archive. Mirrors the import feature observed in the
 * original app ("Malformed history file", "Malformed subscriptions file").
 *
 * Parsing is deliberately tolerant: cells are located by content (the first
 * http(s) cell is the URL) instead of a fixed column order, so Google Takeout
 * and NewPipe-style exports both load.
 */
@Singleton
class TakeoutImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyDao: HistoryDao,
    private val subscriptionDao: SubscriptionDao,
) {

    /** @return number of history rows imported. */
    suspend fun importHistory(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            readCsvRows(uri, prefer = "history").mapNotNull { row ->
                val url = row.firstOrNull { it.startsWith("http") } ?: return@mapNotNull null
                val watchUrl = normalizeWatchUrl(url) ?: return@mapNotNull null
                val title = row.firstOrNull { it.isNotBlank() && !it.startsWith("http") }
                    ?: "Watched video"
                HistoryEntity(videoId = watchUrl, title = title, uploader = "", thumbnailUrl = null)
            }.also { rows -> rows.forEach { historyDao.upsert(it) } }.size
        }
    }

    /** @return number of subscriptions imported. */
    suspend fun importSubscriptions(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            readCsvRows(uri, prefer = "subscription").mapNotNull { row ->
                val url = row.firstOrNull { it.startsWith("http") } ?: return@mapNotNull null
                if (!url.contains("youtube.com") && !url.contains("youtu.be")) return@mapNotNull null
                val name = row.lastOrNull { it.isNotBlank() && !it.startsWith("http") }
                    ?: "Channel"
                SubscriptionEntity(
                    channelId = url.substringAfter("channel/").ifEmpty { url },
                    channelUrl = url,
                    name = name,
                    avatarUrl = null,
                )
            }.also { rows -> rows.forEach { subscriptionDao.upsert(it) } }.size
        }
    }

    /** Normalizes watch URLs to https://www.youtube.com/watch?v=ID; null for non-videos. */
    private fun normalizeWatchUrl(url: String): String? = when {
        url.contains("watch?v=") ->
            "https://www.youtube.com/watch?v=" + url.substringAfter("watch?v=").substringBefore('&')
        url.contains("youtu.be/") ->
            "https://www.youtube.com/watch?v=" + url.substringAfter("youtu.be/").substringBefore('?')
        else -> null
    }

    /**
     * Streams every CSV row — from the picked document itself, or from the
     * ZIP entry whose name matches [prefer] (falling back to all CSV entries).
     */
    private fun readCsvRows(uri: Uri, prefer: String): List<List<String>> {
        val input: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open the selected file")
        return input.use { stream ->
            if (queryDisplayName(uri)?.endsWith(".zip", ignoreCase = true) == true) {
                ZipInputStream(stream).use { zip ->
                    val fallback = mutableListOf<List<String>>()
                    val preferred = mutableListOf<List<String>>()
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory && entry.name.endsWith(".csv", ignoreCase = true)) {
                            val rows = readAll(BufferedReader(InputStreamReader(zip, Charsets.UTF_8)))
                            if (entry.name.lowercase().contains(prefer)) preferred += rows
                            else fallback += rows
                        }
                        zip.closeEntry()
                    }
                    if (preferred.isNotEmpty()) preferred else fallback
                }
            } else {
                readAll(BufferedReader(InputStreamReader(stream, Charsets.UTF_8)))
            }
        }
    }

    private fun readAll(reader: BufferedReader): List<List<String>> =
        reader.useLines { lines ->
            lines.filter { it.isNotBlank() }.map { splitCsvLine(it) }.toList()
        }

    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        cell.append('"'); i++
                    } else inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> { cells += cell.toString().trim(); cell.clear() }
                else -> cell.append(c)
            }
            i++
        }
        cells += cell.toString().trim()
        return cells
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull()
}
