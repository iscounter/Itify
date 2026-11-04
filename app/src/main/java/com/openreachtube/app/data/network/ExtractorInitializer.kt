package com.openreachtube.app.data.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
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
        // Browser-like default UA: YouTube throttles generic extractor clients.
        builder.header(
            "User-Agent",
            request.headers()["User-Agent"]?.firstOrNull() ?: DEFAULT_USER_AGENT,
        )
        request.headers().forEach { (name, values) ->
            if (!name.equals("User-Agent", ignoreCase = true)) {
                values.firstOrNull()?.let { builder.header(name, it) }
            }
        }
        val body = request.dataToSend()
        when (request.httpMethod().uppercase()) {
            "POST" -> builder.post((body ?: ByteArray(0)).toRequestBody())
            "PUT" -> builder.put((body ?: ByteArray(0)).toRequestBody())
            "PATCH" -> builder.patch((body ?: ByteArray(0)).toRequestBody())
            "HEAD" -> builder.head()
            else -> builder.get()
        }
        val resp = client.newCall(builder.build()).execute()
        resp.use {
            val bytes = it.body.bytes()
            if (it.code == 429) throw ReCaptchaException("rate limited", request.url())
            return Response(
                it.code, it.message, it.headers.toMultimap(), String(bytes), request.url(),
            )
        }
    }

    private companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
