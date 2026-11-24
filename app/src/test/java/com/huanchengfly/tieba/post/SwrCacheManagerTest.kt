package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.SwrCacheManager
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SwrCacheManagerTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "swr_test_${System.nanoTime()}")
        tempDir.mkdirs()
        SwrCacheManager.setCustomBaseDirForTesting(tempDir)
        SwrCacheManager.clearAll()
    }

    @After
    fun tearDown() {
        SwrCacheManager.clearAll()
        SwrCacheManager.setCustomBaseDirForTesting(null)
        tempDir.deleteRecursively()
    }

    @Test
    fun testMemoryAndDiskPersistence() {
        val key = "test_key_1"
        val payload = "Hello SWR World!"

        SwrCacheManager.put(
            key = key,
            data = payload,
            serializer = { it.toByteArray(Charsets.UTF_8) },
            ttlMillis = 60_000L
        )

        // 1. In-memory read
        val memEntry = SwrCacheManager.get(
            key = key,
            deserializer = { String(it, Charsets.UTF_8) }
        )
        assertNotNull(memEntry)
        assertEquals(payload, memEntry?.data)
        assertFalse(memEntry?.isStale ?: true)

        // 2. Clear L1 memory to force L2 disk read
        SwrCacheManager.clearMemoryCache()

        val diskEntry = SwrCacheManager.get(
            key = key,
            deserializer = { String(it, Charsets.UTF_8) }
        )
        assertNotNull(diskEntry)
        assertEquals(payload, diskEntry?.data)
        assertFalse(diskEntry?.isStale ?: true)
    }

    @Test
    fun testExpirationAndStaleTolerate() {
        val key = "test_stale_key"
        val payload = "Stale Content"
        val now = System.currentTimeMillis()

        // Create an entry that is 5 seconds old with 2s TTL
        SwrCacheManager.put(
            key = key,
            data = payload,
            serializer = { it.toByteArray(Charsets.UTF_8) },
            ttlMillis = 2_000L,
            timestamp = now - 5_000L
        )

        val entry = SwrCacheManager.get(
            key = key,
            deserializer = { String(it, Charsets.UTF_8) },
            staleTolerateMillis = 10_000L
        )
        assertNotNull(entry)
        assertTrue(entry!!.isStale)
        assertFalse(entry.isExpired(10_000L))

        // Exceed stale tolerance (e.g. tolerate only 3 seconds)
        SwrCacheManager.clearMemoryCache()
        val expiredEntry = SwrCacheManager.get(
            key = key,
            deserializer = { String(it, Charsets.UTF_8) },
            staleTolerateMillis = 3_000L
        )
        assertNull(expiredEntry)
    }

    @Test
    fun testSwrFlow_CacheHitAndBackgroundRevalidate() = runBlocking {
        val key = "feed_cache_key"
        val now = System.currentTimeMillis()

        // Pre-populate stale cache entry (older than 100ms TTL)
        SwrCacheManager.put(
            key = key,
            data = "cached_feed_data",
            serializer = { it.toByteArray(Charsets.UTF_8) },
            ttlMillis = 100L,
            timestamp = now - 500L
        )

        val emitted = SwrCacheManager.flowWithSwr(
            key = key,
            fetcher = {
                flow {
                    emit("network_fresh_feed_data")
                }
            },
            serializer = { it.toByteArray(Charsets.UTF_8) },
            deserializer = { String(it, Charsets.UTF_8) },
            ttlMillis = 10_000L,
            forceRefresh = true
        ).toList()

        assertEquals(2, emitted.size)
        assertEquals("cached_feed_data", emitted[0])
        assertEquals("network_fresh_feed_data", emitted[1])

        // Verify cache was updated with fresh data
        val updated = SwrCacheManager.get(
            key = key,
            deserializer = { String(it, Charsets.UTF_8) }
        )
        assertEquals("network_fresh_feed_data", updated?.data)
    }

    @Test
    fun testSwrFlow_NetworkFailureGracefulDegradation() = runBlocking {
        val key = "offline_resilience_key"

        SwrCacheManager.put(
            key = key,
            data = "cached_offline_snapshot",
            serializer = { it.toByteArray(Charsets.UTF_8) },
            ttlMillis = 100L
        )

        var errorCallbackTriggered = false

        val emitted = SwrCacheManager.flowWithSwr(
            key = key,
            fetcher = {
                flow<String> {
                    throw IOException("Simulated network disconnection")
                }
            },
            serializer = { it.toByteArray(Charsets.UTF_8) },
            deserializer = { String(it, Charsets.UTF_8) },
            forceRefresh = true,
            onRevalidationError = {
                errorCallbackTriggered = true
            }
        ).toList()

        // Must still emit the cached snapshot
        assertEquals(1, emitted.size)
        assertEquals("cached_offline_snapshot", emitted[0])
        assertTrue(errorCallbackTriggered)
    }

    @Test
    fun testSwrFlow_CacheMissAndNetworkFailureRethrow() = runBlocking {
        val key = "empty_cache_key"

        try {
            SwrCacheManager.flowWithSwr(
                key = key,
                fetcher = {
                    flow<String> {
                        throw IOException("Fatal server error")
                    }
                },
                serializer = { it.toByteArray(Charsets.UTF_8) },
                deserializer = { String(it, Charsets.UTF_8) }
            ).toList()
            fail("Expected IOException to be rethrown on cache miss")
        } catch (e: IOException) {
            assertEquals("Fatal server error", e.message)
        }
    }

    @Test
    fun testEvictAndClear() {
        val key = "evict_key"
        SwrCacheManager.put(
            key = key,
            data = "content",
            serializer = { it.toByteArray(Charsets.UTF_8) }
        )

        assertNotNull(SwrCacheManager.get(key, { String(it) }))
        SwrCacheManager.evict(key)
        assertNull(SwrCacheManager.get(key, { String(it) }))
    }

    @Test
    fun testCorruptedDiskFileHandling() {
        val key = "corrupted_file_key"
        val dir = tempDir
        val hash = com.huanchengfly.tieba.post.utils.MD5Util.toMd5(key)
        val file = File(dir, "$hash.swr")

        // Write invalid corrupted bytes
        FileOutputStream(file).use {
            it.write(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04))
        }

        // Must safely return null without throwing exception
        val result = SwrCacheManager.get(
            key = key,
            deserializer = { String(it) }
        )
        assertNull(result)
    }
}
