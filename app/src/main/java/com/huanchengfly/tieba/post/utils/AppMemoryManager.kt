package com.huanchengfly.tieba.post.utils

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.util.Log
import com.github.panpf.sketch.sketch
import com.huanchengfly.tieba.post.AppThemeDelegate
import com.huanchengfly.tieba.post.api.retrofit.TiebaDns
import com.huanchengfly.tieba.post.ui.common.theme.compose.clearThemeColorsCache

/**
 * Centralized memory coordinator and OOM defense manager for TiebaLimp.
 * Executes tiered cache eviction upon system onTrimMemory/onLowMemory signals
 * and provides live memory diagnostics.
 */
object AppMemoryManager {
    private const val TAG = "AppMemoryManager"

    data class MemorySnapshot(
        val usedHeapBytes: Long,
        val totalHeapBytes: Long,
        val maxHeapBytes: Long,
        val heapUsagePercentage: Double,
        val availSystemMemBytes: Long,
        val totalSystemMemBytes: Long,
        val isSystemLowMemory: Boolean
    )

    /**
     * Executes tiered memory trimming according to ComponentCallbacks2 level.
     */
    @Suppress("DEPRECATION")
    @JvmStatic
    fun trimMemory(context: Context?, level: Int) {
        if (context == null) return
        try {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                    // Critical memory pressure: purge all dispensable in-memory caches
                    purgeAllCaches(context)
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                    // Severe pressure: trim image caches and clear transient text/date pools
                    trimImageCaches(context, level)
                    purgeTransientCaches()
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                    // UI moved to background: evict drawables and transient caches to avoid LMK kill
                    trimImageCaches(context, level)
                    purgeTransientCaches()
                }
                else -> {
                    // Running moderate or running low
                    trimImageCaches(context, level)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Exception during trimMemory(level=$level)", t)
        }
    }

    /**
     * Maximum eviction invoked during low-memory callbacks.
     */
    @JvmStatic
    fun onLowMemory(context: Context?) {
        if (context == null) return
        try {
            purgeAllCaches(context)
        } catch (t: Throwable) {
            Log.w(TAG, "Exception during onLowMemory", t)
        }
    }

    private fun trimImageCaches(context: Context, level: Int) {
        runCatching {
            context.sketch.memoryCache.trim(level)
        }
        runCatching {
            context.sketch.bitmapPool.trim(level)
        }
    }

    private fun purgeTransientCaches() {
        runCatching { EmoticonUtil.clearCache() }
        runCatching { EmoticonManager.clearCache() }
        runCatching { BlockRuleMatcher.clearCache() }
        runCatching { DateTimeUtils.clearCache() }
        runCatching { PollVoteStore.clearCache() }
    }

    private fun purgeAllCaches(context: Context) {
        runCatching { context.sketch.memoryCache.clear() }
        runCatching { context.sketch.bitmapPool.clear() }
        purgeTransientCaches()
        runCatching { AppThemeDelegate.clearCache() }
        runCatching { clearThemeColorsCache() }
        runCatching { TiebaDns.clear() }
    }

    /**
     * Captures current heap and system memory statistics for diagnostics and monitoring.
     */
    @JvmStatic
    fun getMemorySnapshot(context: Context?): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val totalHeap = runtime.totalMemory()
        val freeHeap = runtime.freeMemory()
        val maxHeap = runtime.maxMemory()
        val usedHeap = (totalHeap - freeHeap).coerceAtLeast(0L)
        val usagePercentage = if (maxHeap > 0L) (usedHeap.toDouble() / maxHeap.toDouble()) * 100.0 else 0.0

        var availSys = 0L
        var totalSys = 0L
        var isLow = false

        if (context != null) {
            runCatching {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (am != null) {
                    val memInfo = ActivityManager.MemoryInfo()
                    am.getMemoryInfo(memInfo)
                    availSys = memInfo.availMem
                    totalSys = memInfo.totalMem
                    isLow = memInfo.lowMemory
                }
            }
        }

        return MemorySnapshot(
            usedHeapBytes = usedHeap,
            totalHeapBytes = totalHeap,
            maxHeapBytes = maxHeap,
            heapUsagePercentage = usagePercentage,
            availSystemMemBytes = availSys,
            totalSystemMemBytes = totalSys,
            isSystemLowMemory = isLow
        )
    }
}
