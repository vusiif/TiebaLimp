package com.huanchengfly.tieba.post.api.retrofit.interceptors

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.utils.isNetworkConnected
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

open class ConnectivityInterceptor(
    private val networkConnectedProvider: () -> Boolean = {
        runCatching { isNetworkConnected() }.getOrDefault(false)
    }
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isIdempotent = request.method.equals("GET", ignoreCase = true) ||
                request.method.equals("HEAD", ignoreCase = true) ||
                request.method.equals("OPTIONS", ignoreCase = true)

        var attempt = 0
        while (true) {
            val response = runCatching { chain.proceed(request) }
            val exception = response.exceptionOrNull()

            if (exception == null) {
                return response.getOrThrow()
            }

            val connected = networkConnectedProvider()
            val isTransientSocketError = exception is SocketTimeoutException || exception is SocketException
            val canRetry = isIdempotent && isTransientSocketError && attempt < MAX_IDEMPOTENT_RETRIES && connected

            if (canRetry) {
                attempt++
                continue
            }

            return when {
                (exception is SocketTimeoutException || exception is SocketException || exception is SSLHandshakeException) && connected -> {
                    val timeoutMsg = runCatching { App.INSTANCE.getString(R.string.connectivity_timeout) }
                        .getOrDefault("Connection timed out")
                    throw NoConnectivityException(timeoutMsg)
                }

                exception is IOException && !connected -> {
                    val noConnMsg = runCatching { App.INSTANCE.getString(R.string.no_internet_connectivity) }
                        .getOrDefault("No network connectivity")
                    throw NoConnectivityException(noConnMsg)
                }

                else -> response.getOrThrow()
            }
        }
    }

    companion object : ConnectivityInterceptor() {
        const val MAX_IDEMPOTENT_RETRIES = 1
    }
}