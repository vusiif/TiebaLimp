package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.api.retrofit.TiebaDns
import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class TiebaDnsTest {

    @Test
    fun testLookupCaching() {
        var lookupCount = 0
        val mockAddress = InetAddress.getByName("127.0.0.1")
        val mockDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                lookupCount++
                return listOf(mockAddress)
            }
        }

        val testDns = TiebaDns(delegate = mockDns, ttlMillis = 5000L)
        val firstResult = testDns.lookup("tiebac.baidu.com")
        assertEquals(1, lookupCount)
        assertEquals(listOf(mockAddress), firstResult)
        assertTrue(testDns.isCached("tiebac.baidu.com"))

        // Second lookup should hit cache and NOT invoke mockDns
        val secondResult = testDns.lookup("tiebac.baidu.com")
        assertEquals(1, lookupCount)
        assertEquals(listOf(mockAddress), secondResult)
    }

    @Test
    fun testClearCache() {
        val mockAddress = InetAddress.getByName("127.0.0.1")
        val mockDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = listOf(mockAddress)
        }
        val testDns = TiebaDns(delegate = mockDns, ttlMillis = 5000L)

        testDns.lookup("c.tieba.baidu.com")
        assertTrue(testDns.isCached("c.tieba.baidu.com"))

        testDns.clear()
        assertFalse(testDns.isCached("c.tieba.baidu.com"))
    }

    @Test
    fun testFallbackToStaleCacheOnError() {
        var shouldThrow = false
        val mockAddress = InetAddress.getByName("127.0.0.1")
        val mockDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (shouldThrow) {
                    throw UnknownHostException("DNS resolution failed for $hostname")
                }
                return listOf(mockAddress)
            }
        }

        // 1 millisecond TTL so it expires immediately
        val testDns = TiebaDns(delegate = mockDns, ttlMillis = 1L)
        val initial = testDns.lookup("tieba.baidu.com")
        assertEquals(listOf(mockAddress), initial)

        // Wait for TTL to expire
        Thread.sleep(10)

        // Enable exception on delegate
        shouldThrow = true

        // Should gracefully fall back to stale cache instead of crashing
        val fallback = testDns.lookup("tieba.baidu.com")
        assertEquals(listOf(mockAddress), fallback)
    }

    @Test(expected = UnknownHostException::class)
    fun testExceptionWhenNoCacheAvailable() {
        val mockDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                throw UnknownHostException("Not found: $hostname")
            }
        }
        val testDns = TiebaDns(delegate = mockDns, ttlMillis = 5000L)
        testDns.lookup("unknown.host.com")
    }
}
