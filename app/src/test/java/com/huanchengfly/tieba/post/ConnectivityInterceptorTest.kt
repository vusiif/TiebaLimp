package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.ConnectivityInterceptor
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class ConnectivityInterceptorTest {

    private class FakeChain(
        private val request: Request,
        private val handler: (Request, Int) -> Response
    ) : Interceptor.Chain {
        var callCount = 0

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            callCount++
            return handler(request, callCount)
        }

        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 1000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 1000
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 1000
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    private fun createDummyResponse(request: Request, code: Int = 200): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .build()
    }

    @Test
    fun testIdempotentGetRetriesOnSocketTimeoutAndSucceeds() {
        val request = Request.Builder().url("https://c.tieba.baidu.com/c/f/frs/page").get().build()
        val interceptor = ConnectivityInterceptor(networkConnectedProvider = { true })

        val chain = FakeChain(request) { req, count ->
            if (count == 1) {
                throw SocketTimeoutException("Read timed out on pooled socket")
            }
            createDummyResponse(req, 200)
        }

        val response = interceptor.intercept(chain)
        assertEquals(200, response.code)
        assertEquals(2, chain.callCount) // Attempt 1 failed with timeout, Attempt 2 succeeded
    }

    @Test
    fun testNonIdempotentPostDoesNotRetryOnSocketTimeout() {
        val request = Request.Builder()
            .url("https://c.tieba.baidu.com/c/c/post/add")
            .post("content=test".toRequestBody(null))
            .build()
        val interceptor = ConnectivityInterceptor(networkConnectedProvider = { true })

        val chain = FakeChain(request) { _, _ ->
            throw SocketTimeoutException("Stale socket connection")
        }

        try {
            interceptor.intercept(chain)
            fail("Expected NoConnectivityException")
        } catch (e: NoConnectivityException) {
            // Success
            assertEquals(1, chain.callCount) // Non-idempotent MUST NOT retry
        }
    }

    @Test
    fun testIdempotentGetDoesNotRetryWhenOffline() {
        val request = Request.Builder().url("https://c.tieba.baidu.com/c/f/frs/page").get().build()
        val interceptor = ConnectivityInterceptor(networkConnectedProvider = { false })

        val chain = FakeChain(request) { _, _ ->
            throw SocketException("Network unreachable")
        }

        try {
            interceptor.intercept(chain)
            fail("Expected NoConnectivityException")
        } catch (e: NoConnectivityException) {
            assertEquals(1, chain.callCount) // When offline, do not attempt retry
        }
    }

    @Test
    fun testIdempotentGetExhaustsMaxRetries() {
        val request = Request.Builder().url("https://c.tieba.baidu.com/c/f/frs/page").get().build()
        val interceptor = ConnectivityInterceptor(networkConnectedProvider = { true })

        val chain = FakeChain(request) { _, _ ->
            throw SocketException("Connection reset by peer")
        }

        try {
            interceptor.intercept(chain)
            fail("Expected NoConnectivityException")
        } catch (e: NoConnectivityException) {
            // Initial attempt + 1 retry = 2 attempts total
            assertEquals(2, chain.callCount)
        }
    }

    @Test
    fun testNormalSuccessfulRequestNoRetries() {
        val request = Request.Builder().url("https://c.tieba.baidu.com/c/f/frs/page").get().build()
        val interceptor = ConnectivityInterceptor(networkConnectedProvider = { true })

        val chain = FakeChain(request) { req, _ ->
            createDummyResponse(req, 200)
        }

        val response = interceptor.intercept(chain)
        assertEquals(200, response.code)
        assertEquals(1, chain.callCount)
    }
}
