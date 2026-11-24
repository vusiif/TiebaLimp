package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.AppCrashManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AppCrashManagerTest {

    @Test
    fun testCalculateCrashLoop_firstCrash() {
        val lastCrashTime = 0L
        val prevCount = 0
        val currentTime = 100_000L

        val (count, isLoop) = AppCrashManager.calculateCrashLoop(lastCrashTime, prevCount, currentTime)
        assertEquals(1, count)
        assertFalse(isLoop)
    }

    @Test
    fun testCalculateCrashLoop_subsequentCrashWithinWindow() {
        val lastCrashTime = 100_000L
        val prevCount = 1
        val currentTime = 120_000L // 20s later, within 60s window

        val (count, isLoop) = AppCrashManager.calculateCrashLoop(lastCrashTime, prevCount, currentTime)
        assertEquals(2, count)
        assertTrue(isLoop) // >= threshold (2)
    }

    @Test
    fun testCalculateCrashLoop_crashOutsideWindowResets() {
        val lastCrashTime = 100_000L
        val prevCount = 2
        val currentTime = 180_000L // 80s later, > 60s window

        val (count, isLoop) = AppCrashManager.calculateCrashLoop(lastCrashTime, prevCount, currentTime)
        assertEquals(1, count)
        assertFalse(isLoop) // reset to 1
    }

    @Test
    fun testBuildCrashReport_containsExpectedSections() {
        val rootCause = IOException("Network socket closed prematurely")
        val exception = IllegalStateException("Failed to load thread details", rootCause)

        val report = AppCrashManager.buildCrashReport(null, exception, isLoop = true)

        assertTrue(report.contains("TiebaLimp Crash Report"))
        assertTrue(report.contains("App Version:"))
        assertTrue(report.contains("OS Version:"))
        assertTrue(report.contains("Device:"))
        assertTrue(report.contains("Crash Loop Detected: YES"))
        assertTrue(report.contains("IllegalStateException: Failed to load thread details"))
        assertTrue(report.contains("Caused by: java.io.IOException: Network socket closed prematurely"))
    }

    @Test
    fun testBuildCrashReport_nonLoopReport() {
        val exception = NullPointerException("View holder is null")
        val report = AppCrashManager.buildCrashReport(null, exception, isLoop = false)

        assertTrue(report.contains("Crash Loop Detected: NO"))
        assertTrue(report.contains("NullPointerException: View holder is null"))
    }
}
