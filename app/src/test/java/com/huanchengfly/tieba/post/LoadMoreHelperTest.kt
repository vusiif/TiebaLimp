package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadMoreHelperTest {

    @Test
    fun testShouldTriggerPreload_withinRange() {
        val total = 20
        val preload = 4
        // Target boundary is total - preload = 16
        // If last visible is 16 (first=12, count=5 -> last=16), should trigger
        val result = LoadMoreHelper.shouldTriggerPreload(
            isEmpty = false,
            canLoadMore = true,
            isLoading = false,
            isError = false,
            preloadCount = preload,
            firstVisibleItemIndex = 12,
            visibleItemCount = 5,
            totalItemCount = total
        )
        assertTrue(result)
    }

    @Test
    fun testShouldTriggerPreload_farFromEnd() {
        val total = 20
        val preload = 4
        // last visible is 9 (first=5, count=5 -> last=9 < 16)
        val result = LoadMoreHelper.shouldTriggerPreload(
            isEmpty = false,
            canLoadMore = true,
            isLoading = false,
            isError = false,
            preloadCount = preload,
            firstVisibleItemIndex = 5,
            visibleItemCount = 5,
            totalItemCount = total
        )
        assertFalse(result)
    }

    @Test
    fun testShouldTriggerPreload_guards() {
        val baseArgs = { isEmpty: Boolean, canLoad: Boolean, loading: Boolean, error: Boolean, preload: Int, total: Int ->
            LoadMoreHelper.shouldTriggerPreload(
                isEmpty = isEmpty,
                canLoadMore = canLoad,
                isLoading = loading,
                isError = error,
                preloadCount = preload,
                firstVisibleItemIndex = 18,
                visibleItemCount = 2,
                totalItemCount = total
            )
        }

        // Empty list must not trigger
        assertFalse(baseArgs(true, true, false, false, 4, 20))
        // Cannot load more (end of list reached) must not trigger
        assertFalse(baseArgs(false, false, false, false, 4, 20))
        // Already loading must not trigger
        assertFalse(baseArgs(false, true, true, false, 4, 20))
        // In error state must not trigger automatically
        assertFalse(baseArgs(false, true, false, true, 4, 20))
        // Zero preload count must not trigger
        assertFalse(baseArgs(false, true, false, false, 0, 20))
        // Zero total count must not trigger
        assertFalse(baseArgs(false, true, false, false, 4, 0))
    }

    @Test
    fun testCanTriggerWithThrottle() {
        val throttleMs = 500L

        // First trigger: lastTriggerTime is 0, should allow immediate execution
        assertTrue(LoadMoreHelper.canTriggerWithThrottle(1000L, 0L, throttleMs))

        // Trigger within 500ms window (e.g., 200ms elapsed) must be throttled
        assertFalse(LoadMoreHelper.canTriggerWithThrottle(1200L, 1000L, throttleMs))

        // Trigger exactly at 500ms elapsed must be allowed
        assertTrue(LoadMoreHelper.canTriggerWithThrottle(1500L, 1000L, throttleMs))

        // Trigger well after throttle window must be allowed
        assertTrue(LoadMoreHelper.canTriggerWithThrottle(2000L, 1000L, throttleMs))
    }
}
