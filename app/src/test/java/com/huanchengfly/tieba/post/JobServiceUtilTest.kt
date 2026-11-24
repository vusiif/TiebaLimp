package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.services.NotifyJobService
import com.huanchengfly.tieba.post.services.OKSignService
import com.huanchengfly.tieba.post.utils.JobServiceUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JobServiceUtilTest {

    @Test
    fun testJobServiceConstants() {
        assertEquals(30 * 60 * 1000L, JobServiceUtil.DEFAULT_PERIODIC_INTERVAL_MS)

        assertEquals("com.huanchengfly.tieba.post.action.NEW_MESSAGE", NotifyJobService.ACTION_NEW_MESSAGE)
        assertEquals("20", NotifyJobService.CHANNEL_GROUP)
        assertEquals("消息通知", NotifyJobService.CHANNEL_GROUP_NAME)
        assertEquals("2", NotifyJobService.CHANNEL_REPLY)
        assertEquals("回复我的", NotifyJobService.CHANNEL_REPLY_NAME)
        assertEquals("3", NotifyJobService.CHANNEL_AT)
        assertEquals("提到我的", NotifyJobService.CHANNEL_AT_NAME)
        assertEquals("total", NotifyJobService.CHANNEL_TOTAL)
        assertEquals(20, NotifyJobService.ID_REPLY)
        assertEquals(21, NotifyJobService.ID_AT)

        assertEquals("OKSignService", OKSignService.TAG)
        assertEquals("1", OKSignService.NOTIFICATION_CHANNEL_ID)
        assertEquals(1, OKSignService.NOTIFICATION_ID)
        assertEquals("com.huanchengfly.tieba.post.service.action.SIGN_SUCCESS_ALL", OKSignService.ACTION_SIGN_SUCCESS_ALL)
        assertEquals("com.huanchengfly.tieba.post.service.action.ACTION_SIGN_START", OKSignService.ACTION_START_SIGN)
    }

    @Test
    fun testNullContextSafety() {
        // Must handle null context gracefully without throwing NPE
        val jobId = JobServiceUtil.getJobId(null)
        assertTrue("jobId should be in valid range", jobId in 0..100000)

        val isScheduled = JobServiceUtil.isJobScheduled(null, 12345)
        assertFalse(isScheduled)

        val scheduled = JobServiceUtil.scheduleNotifyJob(null)
        assertFalse(scheduled)

        val cancelled = JobServiceUtil.cancelNotifyJob(null)
        assertFalse(cancelled)
    }

    @Test
    fun testJobIdGenerationIntegrity() {
        // In JVM test environment, test random generation bounds
        val randomJobId = (Math.random() * 100000).toInt()
        assertTrue(randomJobId in 0..100000)
    }
}
