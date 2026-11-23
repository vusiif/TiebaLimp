package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {

    @Test
    fun testFixTimestampArithmetic() {
        // 10-digit second timestamp should be scaled to milliseconds (* 1000)
        val tenDigitSeconds = 1700000000L
        assertEquals(1700000000000L, DateTimeUtils.fixTimestamp(tenDigitSeconds))

        // 13-digit millisecond timestamp should remain unchanged
        val thirteenDigitMillis = 1700000000000L
        assertEquals(1700000000000L, DateTimeUtils.fixTimestamp(thirteenDigitMillis))

        // 0 and negative timestamps remain unchanged
        assertEquals(0L, DateTimeUtils.fixTimestamp(0L))
        assertEquals(-1L, DateTimeUtils.fixTimestamp(-1L))
    }

    @Test
    fun testFixTimestampString() {
        assertEquals(1700000000000L, DateTimeUtils.fixTimestamp("1700000000"))
        assertEquals(1700000000000L, DateTimeUtils.fixTimestamp("1700000000000"))
        assertEquals(0L, DateTimeUtils.fixTimestamp("not_a_number"))
    }

    @Test
    fun testIsToday() {
        val now = System.currentTimeMillis()
        assertTrue(DateTimeUtils.isToday(now))

        // 10-digit second timestamp of now
        assertTrue(DateTimeUtils.isToday(now / 1000L))

        // 10 days ago should not be today
        val tenDaysAgo = now - 10L * 24 * 3600 * 1000
        assertFalse(DateTimeUtils.isToday(tenDaysAgo))
    }

    @Test
    fun testClearCacheDoesNotThrow() {
        DateTimeUtils.clearCache()
    }
}
