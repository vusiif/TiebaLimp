package com.huanchengfly.tieba.post.utils

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.huanchengfly.tieba.post.App
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Collections
import java.util.LinkedHashMap

/**
 * Two-tier (L1 Memory LRU + L2 Disk) Stale-While-Revalidate (SWR) cache manager.
 * Provides instant 0ms cold-start rendering and feed switching with background asynchronous revalidation
 * and degraded offline fallback.
 */
object SwrCacheManager {
    private const val TAG = "SwrCacheManager"

    const val DEFAULT_TTL_MILLIS: Long = 10 * 60 * 1000L // 10 minutes fresh window
    const val DEFAULT_STALE_TOLERATE_MILLIS: Long = 7 * 24 * 60 * 60 * 1000L // 7 days stale tolerance
    private const val MAX_MEMORY_ENTRIES = 64
    private const val MAGIC_HEADER: Int = 0x53575231 // 'SWR1'

    data class CacheEntry<T>(
        val data: T,
        val createTimestamp: Long,
        val ttlMillis: Long,
    ) {
        val isStale: Boolean
            get() = (System.currentTimeMillis() - createTimestamp) > ttlMillis

        fun isExpired(staleTolerateMillis: Long): Boolean =
            (System.currentTimeMillis() - createTimestamp) > staleTolerateMillis
    }

    private data class DiskPayload(
        val bytes: ByteArray,
        val timestamp: Long,
        val ttlMillis: Long,
    )

    // L1 LRU Memory Cache
    private val memoryCache: MutableMap<String, CacheEntry<*>> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry<*>>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry<*>>?): Boolean {
                return size > MAX_MEMORY_ENTRIES
            }
        }
    )

    @Volatile
    private var customBaseDir: File? = null

    @VisibleForTesting
    fun setCustomBaseDirForTesting(dir: File?) {
        customBaseDir = dir
    }

    private fun getCacheDir(): File? {
        customBaseDir?.let {
            if (!it.exists()) it.mkdirs()
            return it
        }
        return runCatching {
            val dir = File(App.INSTANCE.filesDir, "swr_cache")
            if (!dir.exists()) dir.mkdirs()
            dir
        }.getOrNull()
    }

    private fun getCacheFile(key: String): File? {
        val dir = getCacheDir() ?: return null
        val hash = MD5Util.toMd5(key) ?: key.hashCode().toString()
        return File(dir, "$hash.swr")
    }

    /**
     * Put item into L1 memory cache and persist to L2 disk cache.
     */
    fun <T : Any> put(
        key: String,
        data: T,
        serializer: (T) -> ByteArray,
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = CacheEntry(data, timestamp, ttlMillis)
        memoryCache[key] = entry

        val file = getCacheFile(key) ?: return
        runCatching {
            val bytes = serializer(data)
            writeToDisk(file, bytes, timestamp, ttlMillis)
        }.onFailure {
            Log.w(TAG, "Failed to persist SWR cache entry to disk for key: $key", it)
        }
    }

    /**
     * Retrieve item from L1 memory cache or fallback to L2 disk cache.
     * Returns null if not found or if expired past staleTolerateMillis.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(
        key: String,
        deserializer: (ByteArray) -> T,
        staleTolerateMillis: Long = DEFAULT_STALE_TOLERATE_MILLIS
    ): CacheEntry<T>? {
        // 1. Try L1 memory cache
        val memEntry = memoryCache[key] as? CacheEntry<T>
        if (memEntry != null) {
            if (!memEntry.isExpired(staleTolerateMillis)) {
                return memEntry
            } else {
                memoryCache.remove(key)
            }
        }

        // 2. Try L2 disk cache
        val file = getCacheFile(key) ?: return null
        val diskPayload = readFromDisk(file, staleTolerateMillis) ?: return null
        return runCatching {
            val data = deserializer(diskPayload.bytes)
            val entry = CacheEntry(data, diskPayload.timestamp, diskPayload.ttlMillis)
            memoryCache[key] = entry
            entry
        }.getOrNull()
    }

    /**
     * Remove an entry from both L1 memory cache and L2 disk cache.
     */
    fun evict(key: String) {
        memoryCache.remove(key)
        runCatching {
            getCacheFile(key)?.delete()
        }
    }

    /**
     * Purge all in-memory caches upon low-memory signal.
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        Log.d(TAG, "Cleared SWR in-memory cache.")
    }

    /**
     * Clear all disk and memory caches.
     */
    fun clearAll() {
        clearMemoryCache()
        runCatching {
            getCacheDir()?.listFiles()?.forEach { it.delete() }
        }
    }

    private fun writeToDisk(file: File, bytes: ByteArray, timestamp: Long, ttlMillis: Long) {
        val parent = file.parentFile ?: return
        if (!parent.exists()) parent.mkdirs()
        val tempFile = File(parent, "${file.name}.${System.nanoTime()}.tmp")
        FileOutputStream(tempFile).use { fos ->
            DataOutputStream(fos).use { dos ->
                dos.writeInt(MAGIC_HEADER)
                dos.writeLong(timestamp)
                dos.writeLong(ttlMillis)
                dos.write(bytes)
                dos.flush()
            }
        }
        if (!tempFile.renameTo(file)) {
            file.delete()
            tempFile.renameTo(file)
        }
    }

    private fun readFromDisk(file: File, staleTolerateMillis: Long): DiskPayload? {
        if (!file.exists() || !file.isFile || file.length() < 20) return null
        return runCatching {
            FileInputStream(file).use { fis ->
                DataInputStream(fis).use { dis ->
                    val magic = dis.readInt()
                    if (magic != MAGIC_HEADER) return null
                    val timestamp = dis.readLong()
                    val ttlMillis = dis.readLong()
                    val age = System.currentTimeMillis() - timestamp
                    if (age > staleTolerateMillis) {
                        file.delete()
                        return null
                    }
                    val payloadBytes = dis.readBytes()
                    DiskPayload(payloadBytes, timestamp, ttlMillis)
                }
            }
        }.getOrNull()
    }

    /**
     * SWR Flow Operator:
     * 1. If cache hit and emitStaleFirst: immediately emits cached snapshot.
     * 2. If fresh and not forceRefresh: finishes immediately (0ms).
     * 3. Revalidates from fetcher() in background. Emits fresh data on success and updates cache.
     * 4. If revalidation fails: if cached data was emitted, suppresses error to keep UI usable;
     *    if no cached data, rethrows to trigger standard error handling.
     */
    fun <T : Any> flowWithSwr(
        key: String,
        fetcher: () -> Flow<T>,
        serializer: (T) -> ByteArray,
        deserializer: (ByteArray) -> T,
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
        staleTolerateMillis: Long = DEFAULT_STALE_TOLERATE_MILLIS,
        forceRefresh: Boolean = false,
        emitStaleFirst: Boolean = true,
        onRevalidationError: ((Throwable) -> Unit)? = null,
    ): Flow<T> = flow {
        val cached = get(key, deserializer, staleTolerateMillis)
        var hasEmittedCached = false

        if (cached != null && emitStaleFirst) {
            emit(cached.data)
            hasEmittedCached = true
            if (!forceRefresh && !cached.isStale) {
                return@flow
            }
        }

        try {
            fetcher().collect { freshData ->
                put(key, freshData, serializer, ttlMillis)
                emit(freshData)
            }
        } catch (e: Throwable) {
            if (!hasEmittedCached) {
                throw e
            }
            onRevalidationError?.invoke(e)
            Log.w(TAG, "SWR background revalidation failed for key: $key, falling back to cached snapshot", e)
        }
    }
}
