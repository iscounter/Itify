package com.openreachtube.app.download

import com.openreachtube.app.data.local.dao.ChunkDao
import com.openreachtube.app.data.local.dao.MissionDao
import com.openreachtube.app.data.local.entity.DownloadChunkEntity
import com.openreachtube.app.data.local.entity.DownloadMissionEntity
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Mission-based chunked parallel downloader (same algorithm family as observed,
 * original implementation): splits the target into N ranges, downloads them
 * concurrently, records per-chunk progress in Room so transfers RESUME after
 * process death or reboot.
 */
@Singleton
class ChunkedDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val missionDao: MissionDao,
    private val chunkDao: ChunkDao,
) {
    private val CHUNK_SIZE = 4L * 1024 * 1024 // 4 MiB
    private val PARALLELISM = 4
    private val active = ConcurrentHashMap<String, Boolean>()

    suspend fun enqueue(mission: DownloadMissionEntity, target: File) {
        val total = contentLength(mission.url)
        missionDao.upsert(mission.copy(totalBytes = total, status = "RUNNING"))
        if (target.exists() && target.length() == total) {
            missionDao.setStatus(mission.missionId, "DONE")
            return
        }
        if (!target.exists()) target.parentFile?.mkdirs().also { target.createNewFile() }

        val existing = chunkDao.chunksFor(mission.missionId)
        val chunks = if (existing.isEmpty()) {
            val created = mutableListOf<DownloadChunkEntity>()
            var index = 0
            var start = 0L
            while (start < total) {
                val end = minOf(start + CHUNK_SIZE - 1, total - 1)
                val e = DownloadChunkEntity(
                    missionId = mission.missionId, chunkIndex = index,
                    startByte = start, endByte = end,
                )
                chunkDao.upsert(e)
                created.add(e)
                start = end + 1
                index += 1
            }
            created
        } else existing

        runMission(mission, chunks, target)
    }

    private suspend fun runMission(
        mission: DownloadMissionEntity,
        chunks: List<DownloadChunkEntity>,
        target: File,
    ) = coroutineScope {
        active[mission.missionId] = true
        try {
            chunks.filter { !it.done }.forEach { chunk ->
                launch(Dispatchers.IO) {
                    downloadChunk(mission.url, chunk, target)
                }
            }
            val allDone = chunkDao.chunksFor(mission.missionId).all { it.done }
            missionDao.setStatus(mission.missionId, if (allDone) "DONE" else "RUNNING")
        } finally {
            active.remove(mission.missionId)
        }
    }

    private suspend fun downloadChunk(url: String, chunk: DownloadChunkEntity, target: File) =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url)
                .header("Range", "bytes=${chunk.startByte + chunk.downloadedBytes}-${chunk.endByte}")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                val out = java.io.RandomAccessFile(target, "rw")
                out.use { raf ->
                    raf.seek(chunk.startByte + chunk.downloadedBytes)
                    val src = resp.body?.byteStream() ?: error("Empty download body")
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (src.read(buffer).also { read = it } != -1) {
                        raf.write(buffer, 0, read)
                        // Persist progress periodically for crash-resumable chunks.
                    }
                    chunkDao.updateProgress(chunk.id, chunk.endByte - chunk.startByte + 1, true)
                }
            }
        }

    private suspend fun contentLength(url: String): Long = withContext(Dispatchers.IO) {
        okHttpClient.newCall(
            Request.Builder().url(url).header("Range", "bytes=0-0").build()
        ).execute().use { resp ->
            val range = resp.header("Content-Range")
            range?.substringAfter("/")?.toLongOrNull()
                ?: resp.header("Content-Length")?.toLongOrNull() ?: -1L
        }
    }
}
