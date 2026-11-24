package com.huanchengfly.tieba.post.ui.page.webview

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * High-performance resource interceptor and static asset cache engine for WebView.
 *
 * 1. Blocks intrusive ad domains and tracking pixels before network requests leave the device.
 * 2. Intercepts static resources (JS, CSS, Web Fonts, Images) and serves them via an OkHttp disk cache
 *    with HTTP/2 multiplexing and TiebaDns resolution.
 */
object WebResourceInterceptor {
    private const val TAG = "WebResourceInterceptor"

    val AD_HOSTS = setOf(
        "pos.baidu.com",
        "mobads.baidu.com",
        "cpro.baidustatic.com",
        "dup.baidustatic.com",
        "union.baidu.com",
        "hm.baidu.com"
    )

    fun isAdHost(host: String): Boolean {
        val lowerHost = host.lowercase()
        return AD_HOSTS.any { lowerHost == it || lowerHost.endsWith(".$it") }
    }

    fun getMimeType(url: String): String? {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val ext = cleanUrl.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "js" -> "application/javascript"
            "css" -> "text/css"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "woff2" -> "font/woff2"
            "woff" -> "font/woff"
            "ttf" -> "font/ttf"
            else -> null
        }
    }

    fun isStaticResource(url: String): Boolean {
        return getMimeType(url) != null
    }

    private var okHttpCache: Cache? = null

    val webOkHttpClient: OkHttpClient by lazy {
        val cacheDir = File(App.INSTANCE.cacheDir, "webview_http_cache")
        val cache = Cache(cacheDir, 25L * 1024 * 1024)
        okHttpCache = cache
        RetrofitTiebaApi.baseOkHttpClient.newBuilder()
            .cache(cache)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun clearCache() {
        runCatching {
            okHttpCache?.evictAll()
        }
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            200,
            "OK",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        if (!request.method.equals("GET", ignoreCase = true)) return null
        val uri = request.url ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase() ?: return null

        if (isAdHost(host)) {
            return createEmptyResponse()
        }

        val url = uri.toString()
        val mimeType = getMimeType(url) ?: return null

        return runCatching {
            val reqBuilder = Request.Builder().url(url).get()
            request.requestHeaders?.forEach { (k, v) ->
                reqBuilder.header(k, v)
            }
            runCatching {
                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrBlank()) {
                    reqBuilder.header("Cookie", cookie)
                }
            }

            val response = webOkHttpClient.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val body = response.body
                val stream = body?.byteStream()
                if (stream != null) {
                    val responseHeaders = mutableMapOf<String, String>()
                    for (i in 0 until response.headers.size) {
                        responseHeaders[response.headers.name(i)] = response.headers.value(i)
                    }
                    val encoding = body.contentType()?.charset()?.name() ?: "utf-8"
                    return WebResourceResponse(
                        mimeType,
                        encoding,
                        response.code,
                        response.message.ifBlank { "OK" },
                        responseHeaders,
                        stream
                    )
                }
            }
            response.close()
            null
        }.getOrNull()
    }
}
