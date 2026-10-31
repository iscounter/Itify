package com.openreachtube.app.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/**
 * Boots NewPipeExtractor with an OkHttp-backed Downloader implementation.
 * Runs once from [com.openreachtube.app.OpenReachApp.onCreate].
 */
@Singleton
class ExtractorInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    @Volatile private var initialized = false

    fun initialize() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            NewPipe.init(OkHttpDownloader(okHttpClient))
            initialized = true
        }
    }
}

/** Minimal Downloader bridging NewPipeExtractor requests onto OkHttp. */
private class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())
        request.headers().forEach { (name, value) -> builder.header(name, value) }
        val body = request.dataToSend()
        when (request.httpMethod().uppercase()) {
            "POST" -> builder.post(okhttp3.RequestBody.create(null, body ?: ByteArray(0)))
            "PUT" -> builder.put(okhttp3.RequestBody.create(null, body ?: ByteArray(0)))
            "PATCH" -> builder.patch(okhttp3.RequestBody.create(null, body ?: ByteArray(0)))
            "HEAD" -> builder.head()
            else -> builder.get()
        }
        val resp = client.newCall(builder.build()).execute()
        resp.use {
            val bytes = it.body?.bytes() ?: ByteArray(0)
            val headers = it.headers.toMultimap().mapValues { (_, v) -> v.joinToString(", ") }
            if (it.code == 429) throw ReCaptchaException("rate limited", request.url())
            return Response(it.code, it.message, headers, String(bytes), bytes)
        }
    }
}
