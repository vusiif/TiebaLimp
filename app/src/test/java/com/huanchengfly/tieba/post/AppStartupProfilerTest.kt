package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.AppStartupProfiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppStartupProfilerTest {

    @Before
    fun setUp() {
        AppStartupProfiler.reset()
    }

    @Test
    fun testInitialStateAndReset() {
        assertEquals(-1L, AppStartupProfiler.getStageDuration("non_existent_stage"))
        assertEquals(-1L, AppStartupProfiler.getAppCreateDuration())
        assertEquals(-1L, AppStartupProfiler.getColdStartDuration())
        assertEquals(-1L, AppStartupProfiler.getTimeToFirstFrame())

        val report = AppStartupProfiler.getReport()
        assertNotNull(report)
        assertTrue(report.stages.isEmpty())
    }

    @Test
    fun testAppCreateTimings() {
        AppStartupProfiler.markAppCreateStart()
        val start1 = AppStartupProfiler.appCreateStartTimeMillis
        assertTrue(start1 > 0L)

        // Subsequent call must not overwrite start time
        AppStartupProfiler.markAppCreateStart()
        assertEquals(start1, AppStartupProfiler.appCreateStartTimeMillis)

        AppStartupProfiler.markAppCreateEnd()
        val end1 = AppStartupProfiler.appCreateEndTimeMillis
        assertTrue(end1 >= start1)

        val duration = AppStartupProfiler.getAppCreateDuration()
        assertTrue(duration >= 0L)
    }

    @Test
    fun testStageTracing() {
        val result = AppStartupProfiler.traceStage("compute_hash") {
            40 + 2
        }
        assertEquals(42, result)
        val stageDuration = AppStartupProfiler.getStageDuration("compute_hash")
        assertTrue(stageDuration >= 0L)

        AppStartupProfiler.recordStage("custom_step", 15L)
        assertEquals(15L, AppStartupProfiler.getStageDuration("custom_step"))
    }

    @Test
    fun testColdStartAndTimeToFirstFrameCalculation() {
        AppStartupProfiler.markAppCreateStart()
        AppStartupProfiler.markAppCreateEnd()
        AppStartupProfiler.markFirstActivityCreated()
        AppStartupProfiler.markFirstFrameRendered()

        val coldStart = AppStartupProfiler.getColdStartDuration()
        assertTrue(coldStart >= 0L)

        val ttff = AppStartupProfiler.getTimeToFirstFrame()
        assertTrue(ttff >= 0L)

        val report = AppStartupProfiler.getReport()
        assertTrue(report.coldStartDurationMs >= 0L)
        assertTrue(report.timeToFirstFrameMs >= 0L)
    }
}
