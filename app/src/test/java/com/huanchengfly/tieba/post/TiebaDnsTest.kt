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

    @Test
    fun testStaleWhileRevalidateAsyncUpdate() {
        val address1 = InetAddress.getByName("1.1.1.1")
        val address2 = InetAddress.getByName("2.2.2.2")
        var currentAddress = address1

        val mockDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = listOf(currentAddress)
        }

        val testDns = TiebaDns(
            delegate = mockDns,
            ttlMillis = 300L,
            maxStaleMillis = 10000L
        )

        // 1. Initial resolution (fresh)
        val initial = testDns.lookup("tieba.baidu.com")
        assertEquals(listOf(address1), initial)
        assertTrue(testDns.isCached("tieba.baidu.com"))
        assertFalse(testDns.isStale("tieba.baidu.com"))

        // 2. Wait for TTL to expire so it enters SWR window
        Thread.sleep(350)
        assertTrue(testDns.isStale("tieba.baidu.com"))

        // Mock DNS now provides new IP
        currentAddress = address2

        // SWR: lookup should return stale address1 immediately without waiting
        val swrResult = testDns.lookup("tieba.baidu.com")
        assertEquals(listOf(address1), swrResult)

        // 3. Wait for async revalidation to complete
        val deadline = System.currentTimeMillis() + 2000
        while (testDns.getCachedAddresses("tieba.baidu.com")?.firstOrNull() != address2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }

        // Cache should now be refreshed with address2 and fresh (< ttlMillis)
        val refreshed = testDns.lookup("tieba.baidu.com")
        assertEquals(listOf(address2), refreshed)
        assertFalse(testDns.isStale("tieba.baidu.com"))
        assertTrue(testDns.isCached("tieba.baidu.com"))
    }

    @Test
    fun testFallbackPastMaxStaleOnError() {
        var shouldThrow = false
        val mockAddress = InetAddress.getByName("127.0.0.1")
        val mockDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                if (shouldThrow) {
                    throw UnknownHostException("DNS totally down")
                }
                return listOf(mockAddress)
            }
        }

        // 10ms TTL, 20ms MaxStale
        val testDns = TiebaDns(
            delegate = mockDns,
            ttlMillis = 10L,
            maxStaleMillis = 20L
        )

        val initial = testDns.lookup("c.tieba.baidu.com")
        assertEquals(listOf(mockAddress), initial)

        // Wait past maxStaleMillis
        Thread.sleep(40)
        shouldThrow = true

        // Synchronous lookup fails, but disaster recovery preserves stale record
        val recovered = testDns.lookup("c.tieba.baidu.com")
        assertEquals(listOf(mockAddress), recovered)
    }

    @Test
    fun testCachedAddressesAndSize() {
        val mockAddress = InetAddress.getByName("127.0.0.1")
        val mockDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = listOf(mockAddress)
        }
        val testDns = TiebaDns(delegate = mockDns, ttlMillis = 5000L)

        assertEquals(0, testDns.getCacheSize())
        testDns.lookup("tiebac.baidu.com")
        assertEquals(1, testDns.getCacheSize())
        assertEquals(listOf(mockAddress), testDns.getCachedAddresses("tiebac.baidu.com"))
    }
}
