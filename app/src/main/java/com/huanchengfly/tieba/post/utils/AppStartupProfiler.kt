package com.huanchengfly.tieba.post.utils

import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * High-precision startup profiler for TiebaLimp.
 * Measures application process launch, App.onCreate duration, component initialization latency,
 * and time-to-first-frame (TTFF) rendering.
 */
object AppStartupProfiler {
    private const val TAG = "StartupProfiler"

    @Volatile
    var processStartTimeMillis: Long = 0L
        internal set

    @Volatile
    var appCreateStartTimeMillis: Long = 0L
        internal set

    @Volatile
    var appCreateEndTimeMillis: Long = 0L
        internal set

    @Volatile
    var firstActivityCreatedTimeMillis: Long = 0L
        internal set

    @Volatile
    var firstFrameRenderedTimeMillis: Long = 0L
        internal set

    private val stageTimings = ConcurrentHashMap<String, Long>()

    init {
        reset()
    }

    @PublishedApi
    internal fun nowUptime(): Long {
        return try {
            SystemClock.uptimeMillis()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }

    @Synchronized
    fun reset() {
        processStartTimeMillis = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Process.getStartUptimeMillis()
            } else {
                nowUptime()
            }
        } catch (_: Throwable) {
            nowUptime()
        }
        appCreateStartTimeMillis = 0L
        appCreateEndTimeMillis = 0L
        firstActivityCreatedTimeMillis = 0L
        firstFrameRenderedTimeMillis = 0L
        stageTimings.clear()
    }

    fun markAppCreateStart() {
        if (appCreateStartTimeMillis == 0L) {
            appCreateStartTimeMillis = nowUptime()
        }
    }

    fun markAppCreateEnd() {
        if (appCreateEndTimeMillis == 0L) {
            appCreateEndTimeMillis = nowUptime()
        }
    }

    fun markFirstActivityCreated() {
        if (firstActivityCreatedTimeMillis == 0L) {
            firstActivityCreatedTimeMillis = nowUptime()
        }
    }

    fun markFirstFrameRendered() {
        if (firstFrameRenderedTimeMillis == 0L) {
            firstFrameRenderedTimeMillis = nowUptime()
            logSummary()
        }
    }

    inline fun <T> traceStage(stageName: String, block: () -> T): T {
        val start = nowUptime()
        return try {
            block()
        } finally {
            val duration = nowUptime() - start
            recordStage(stageName, duration)
        }
    }

    fun recordStage(stageName: String, durationMs: Long) {
        stageTimings[stageName] = durationMs
    }

    fun getStageDuration(stageName: String): Long {
        return stageTimings[stageName] ?: -1L
    }

    fun getAppCreateDuration(): Long {
        if (appCreateStartTimeMillis <= 0L || appCreateEndTimeMillis <= 0L) return -1L
        return (appCreateEndTimeMillis - appCreateStartTimeMillis).coerceAtLeast(0L)
    }

    fun getTimeToFirstFrame(): Long {
        val start = if (processStartTimeMillis > 0L) processStartTimeMillis else appCreateStartTimeMillis
        if (start <= 0L || firstFrameRenderedTimeMillis <= 0L) return -1L
        return (firstFrameRenderedTimeMillis - start).coerceAtLeast(0L)
    }

    fun getColdStartDuration(): Long {
        val start = if (appCreateStartTimeMillis > 0L) appCreateStartTimeMillis else processStartTimeMillis
        if (start <= 0L || firstActivityCreatedTimeMillis <= 0L) return -1L
        return (firstActivityCreatedTimeMillis - start).coerceAtLeast(0L)
    }

    fun getReport(): StartupReport {
        return StartupReport(
            processStartTime = processStartTimeMillis,
            appCreateDurationMs = getAppCreateDuration(),
            timeToFirstFrameMs = getTimeToFirstFrame(),
            coldStartDurationMs = getColdStartDuration(),
            stages = HashMap(stageTimings)
        )
    }

    private fun logSummary() {
        try {
            val report = getReport()
            Log.i(TAG, "Cold Start Complete: TTFF=${report.timeToFirstFrameMs}ms, App.onCreate=${report.appCreateDurationMs}ms, Stages=${report.stages}")
        } catch (_: Throwable) {
        }
    }

    data class StartupReport(
        val processStartTime: Long,
        val appCreateDurationMs: Long,
        val timeToFirstFrameMs: Long,
        val coldStartDurationMs: Long,
        val stages: Map<String, Long>
    )
}
