package com.huanchengfly.tieba.post

import android.content.ComponentCallbacks2
import com.huanchengfly.tieba.post.api.models.protos.PollInfo
import com.huanchengfly.tieba.post.api.retrofit.TiebaDns
import com.huanchengfly.tieba.post.ui.common.theme.compose.clearThemeColorsCache
import com.huanchengfly.tieba.post.utils.AppMemoryManager
import com.huanchengfly.tieba.post.utils.BlockRuleMatcher
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.EmoticonUtil
import com.huanchengfly.tieba.post.utils.PollVoteStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMemoryManagerTest {

    @Test
    fun testMemorySnapshotNullContextSafety() {
        val snapshot = AppMemoryManager.getMemorySnapshot(null)
        assertNotNull(snapshot)
        assertTrue(snapshot.maxHeapBytes > 0L)
        assertTrue(snapshot.totalHeapBytes > 0L)
        assertTrue(snapshot.usedHeapBytes >= 0L)
        assertTrue(snapshot.heapUsagePercentage >= 0.0)
        assertTrue(snapshot.heapUsagePercentage <= 100.0)
        assertEquals(0L, snapshot.availSystemMemBytes)
        assertEquals(0L, snapshot.totalSystemMemBytes)
        assertEquals(false, snapshot.isSystemLowMemory)
    }

    @Suppress("DEPRECATION")
    @Test
    fun testTrimMemoryNullContextSafety() {
        // Must never throw with null Context across all trim levels
        AppMemoryManager.trimMemory(null, ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        AppMemoryManager.trimMemory(null, ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        AppMemoryManager.trimMemory(null, ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        AppMemoryManager.trimMemory(null, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        AppMemoryManager.trimMemory(null, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        AppMemoryManager.onLowMemory(null)
    }

    @Test
    fun testCacheEvictionCycle() {
        // Exercise individual cache clear APIs to verify memory purging
        EmoticonUtil.clearCache()
        EmoticonManager.clearCache()
        BlockRuleMatcher.clearCache()
        DateTimeUtils.clearCache()
        AppThemeDelegate.clearCache()
        clearThemeColorsCache()
        TiebaDns.clear()

        // Test PollVoteStore cache clearing
        val testPoll = PollInfo(title = "Test Poll")
        PollVoteStore.update(12345L, testPoll)
        assertEquals(testPoll, PollVoteStore.get(12345L, null))

        PollVoteStore.clearCache()
        assertNull(PollVoteStore.get(12345L, null))
    }
}
