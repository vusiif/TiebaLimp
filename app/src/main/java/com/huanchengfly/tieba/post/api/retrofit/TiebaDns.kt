package com.huanchengfly.tieba.post.api.retrofit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dns
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Intelligent caching DNS resolver for Baidu Tieba APIs and CDN endpoints.
 * Provides in-memory IP caching with TTL, background pre-warming, and stale-record fallback.
 */
open class TiebaDns(
    private val delegate: Dns = Dns.SYSTEM,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val maxStaleMillis: Long = DEFAULT_MAX_STALE_MILLIS,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : Dns {

    private data class CachedDns(
        val addresses: List<InetAddress>,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val cache = ConcurrentHashMap<String, CachedDns>()
    private val revalidatingHosts = ConcurrentHashMap.newKeySet<String>()

    override fun lookup(hostname: String): List<InetAddress> {
        val cached = cache[hostname]
        val now = System.currentTimeMillis()

        // 1. Fresh cache: within ttlMillis -> return immediately
        if (cached != null && (now - cached.timestamp < ttlMillis)) {
            return cached.addresses
        }

        // 2. Stale-While-Revalidate: past ttlMillis but within maxStaleMillis
        // Return stale cached IP immediately (0ms blocking latency) and trigger async revalidation
        if (cached != null && (now - cached.timestamp < maxStaleMillis)) {
            triggerRevalidation(hostname)
            return cached.addresses
        }

        // 3. Fully expired or no cache: synchronous lookup
        return try {
            val addresses = delegate.lookup(hostname)
            if (addresses.isNotEmpty()) {
                cache[hostname] = CachedDns(addresses, now)
            }
            addresses
        } catch (e: Exception) {
            // Disaster recovery: return stale cached addresses if network/DNS is flaky
            cached?.addresses?.takeIf { it.isNotEmpty() } ?: throw e
        }
    }

    /**
     * Triggers asynchronous DNS revalidation in the background to update cache without blocking requests.
     */
    private fun triggerRevalidation(hostname: String) {
        if (revalidatingHosts.add(hostname)) {
            coroutineScope.launch {
                try {
                    val addresses = delegate.lookup(hostname)
                    if (addresses.isNotEmpty()) {
                        cache[hostname] = CachedDns(addresses, System.currentTimeMillis())
                    }
                } catch (_: Throwable) {
                    // Revalidation failure is non-fatal; preserve existing cache for disaster recovery
                } finally {
                    revalidatingHosts.remove(hostname)
                }
            }
        }
    }

    /**
     * Pre-resolves core hostnames in background to achieve 0ms DNS latency on first request.
     */
    fun prewarm(vararg hostnames: String = CORE_HOSTS) {
        coroutineScope.launch {
            for (host in hostnames) {
                runCatching {
                    val current = cache[host]
                    val now = System.currentTimeMillis()
                    if (current == null || (now - current.timestamp >= ttlMillis)) {
                        val addresses = delegate.lookup(host)
                        if (addresses.isNotEmpty()) {
                            cache[host] = CachedDns(addresses, now)
                        }
                    }
                }
            }
        }
    }

    /**
     * Clears all cached DNS records (e.g., when network connection changes).
     */
    fun clear() {
        cache.clear()
        revalidatingHosts.clear()
    }

    fun isCached(hostname: String): Boolean {
        val cached = cache[hostname] ?: return false
        return (System.currentTimeMillis() - cached.timestamp) < ttlMillis
    }

    fun isStale(hostname: String): Boolean {
        val cached = cache[hostname] ?: return false
        val age = System.currentTimeMillis() - cached.timestamp
        return age in ttlMillis until maxStaleMillis
    }

    fun getCachedAddresses(hostname: String): List<InetAddress>? {
        return cache[hostname]?.addresses
    }

    fun getCacheSize(): Int = cache.size

    fun isRevalidating(hostname: String): Boolean = revalidatingHosts.contains(hostname)

    companion object : TiebaDns() {
        const val DEFAULT_TTL_MILLIS: Long = 10 * 60 * 1000L // 10 minutes cache TTL
        const val DEFAULT_MAX_STALE_MILLIS: Long = 24 * 60 * 60 * 1000L // 24 hours SWR limit

        val CORE_HOSTS = arrayOf(
            "tiebac.baidu.com",
            "c.tieba.baidu.com",
            "tieba.baidu.com",
            "sofire.baidu.com",
            "tb2.bdstatic.com",
            "tiebapic.baidu.com"
        )
    }
}
