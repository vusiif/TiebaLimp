package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyLoadTest {

    @Test
    fun testCurrentPage_alwaysWithinPreloadRange() {
        assertTrue(LazyLoadHelper.isWithinPreloadRange(pageIndex = 0, currentPage = 0, preloadDistance = 0))
        assertTrue(LazyLoadHelper.isWithinPreloadRange(pageIndex = 2, currentPage = 2, preloadDistance = 0))
        assertTrue(LazyLoadHelper.isWithinPreloadRange(pageIndex = 5, currentPage = 5, preloadDistance = 1))
    }

    @Test
    fun testAdjacentPage_withinPreloadDistance() {
        // Distance 1: adjacent next (page 1 from current 0)
        assertTrue(LazyLoadHelper.isWithinPreloadRange(pageIndex = 1, currentPage = 0, preloadDistance = 1))
        // Distance 1: adjacent previous (page 0 from current 1)
        assertTrue(LazyLoadHelper.isWithinPreloadRange(pageIndex = 0, currentPage = 1, preloadDistance = 1))
        // 2 pages away: page 2 from current 0 must be false
        assertFalse(LazyLoadHelper.isWithinPreloadRange(pageIndex = 2, currentPage = 0, preloadDistance = 1))
        // 2 pages away: page 3 from current 1 must be false
        assertFalse(LazyLoadHelper.isWithinPreloadRange(pageIndex = 3, currentPage = 1, preloadDistance = 1))
    }

    @Test
    fun testPreloadDistanceZero_strictlyCurrentPageOnly() {
        // Distance 0 means strict current page only
        assertTrue(LazyLoadHelper.isWithinPreloadRange(pageIndex = 0, currentPage = 0, preloadDistance = 0))
        assertFalse(LazyLoadHelper.isWithinPreloadRange(pageIndex = 1, currentPage = 0, preloadDistance = 0))
        assertFalse(LazyLoadHelper.isWithinPreloadRange(pageIndex = 2, currentPage = 1, preloadDistance = 0))
    }

    @Test
    fun testPreloadDistanceNegative_safelyDefaultsToCurrentOnly() {
        assertTrue(LazyLoadHelper.isWithinPreloadRange(pageIndex = 3, currentPage = 3, preloadDistance = -1))
        assertFalse(LazyLoadHelper.isWithinPreloadRange(pageIndex = 2, currentPage = 3, preloadDistance = -1))
    }

    @Test
    fun testMultiPagePreloadDistance() {
        val current = 5
        val distance = 2
        // Allowed: 3, 4, 5, 6, 7
        for (p in 3..7) {
            assertTrue("Page $p should be within preload range", LazyLoadHelper.isWithinPreloadRange(p, current, distance))
        }
        // Blocked: 2, 8
        assertFalse(LazyLoadHelper.isWithinPreloadRange(2, current, distance))
        assertFalse(LazyLoadHelper.isWithinPreloadRange(8, current, distance))
    }
}
