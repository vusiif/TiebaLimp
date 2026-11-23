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
    private val ttlMillis: Long = 10 * 60 * 1000L // 10 minutes cache TTL
) : Dns {

    private data class CachedDns(
        val addresses: List<InetAddress>,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val cache = ConcurrentHashMap<String, CachedDns>()

    override fun lookup(hostname: String): List<InetAddress> {
        val cached = cache[hostname]
        val now = System.currentTimeMillis()
        if (cached != null && (now - cached.timestamp < ttlMillis)) {
            return cached.addresses
        }
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
     * Pre-resolves core hostnames in background to achieve 0ms DNS latency on first request.
     */
    fun prewarm(vararg hostnames: String = CORE_HOSTS) {
        CoroutineScope(Dispatchers.IO).launch {
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
    }

    fun isCached(hostname: String): Boolean {
        val cached = cache[hostname] ?: return false
        return (System.currentTimeMillis() - cached.timestamp) < ttlMillis
    }

    companion object : TiebaDns() {
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
