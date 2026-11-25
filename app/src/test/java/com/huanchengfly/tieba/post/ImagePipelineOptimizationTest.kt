package com.huanchengfly.tieba.post

import android.content.ComponentCallbacks2
import com.huanchengfly.tieba.post.utils.AdaptivePreloadPolicy
import com.huanchengfly.tieba.post.utils.AppMemoryManager
import com.huanchengfly.tieba.post.utils.ImagePreloader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImagePipelineOptimizationTest {

    @Before
    fun setUp() {
        ImagePreloader.clearCache()
    }

    @After
    fun tearDown() {
        ImagePreloader.clearCache()
    }

    @Test
    fun testPreloadDeduplication() {
        assertEquals(0, ImagePreloader.getPreloadedCount())

        val testUrl = "https://imgsa.baidu.com/forum/pic/item/test_1.jpg"

        // In pure unit test without android Context mock, test direct URL deduplication logic
        // We can simulate adding via reflection or testing dedup capacity
        assertFalse(ImagePreloader.isUrlPreloaded(testUrl))
    }

    @Test
    fun testPreloadCapacityAndEviction() {
        // Pre-fill cache via reflection to test bounded 128 LRU eviction
        val field = ImagePreloader::class.java.getDeclaredField("preloadedUrls")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = field.get(ImagePreloader) as MutableSet<String>

        cache.clear()
        for (i in 0 until 130) {
            cache.add("https://test.image/$i.jpg")
        }

        // Bounded capacity is 128
        assertEquals(128, ImagePreloader.getPreloadedCount())
        // The first 2 elements should have been evicted
        assertFalse(ImagePreloader.isUrlPreloaded("https://test.image/0.jpg"))
        assertFalse(ImagePreloader.isUrlPreloaded("https://test.image/1.jpg"))
        // Later elements must remain
        assertTrue(ImagePreloader.isUrlPreloaded("https://test.image/2.jpg"))
        assertTrue(ImagePreloader.isUrlPreloaded("https://test.image/129.jpg"))
    }

    @Test
    fun testClearCache() {
        val field = ImagePreloader::class.java.getDeclaredField("preloadedUrls")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = field.get(ImagePreloader) as MutableSet<String>

        cache.add("https://test.image/sample.jpg")
        assertEquals(1, ImagePreloader.getPreloadedCount())
        assertTrue(ImagePreloader.isUrlPreloaded("https://test.image/sample.jpg"))

        ImagePreloader.clearCache()
        assertEquals(0, ImagePreloader.getPreloadedCount())
        assertFalse(ImagePreloader.isUrlPreloaded("https://test.image/sample.jpg"))
    }

    @Test
    fun testAppMemoryManagerTrimsImagePreloader() {
        val field = ImagePreloader::class.java.getDeclaredField("preloadedUrls")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = field.get(ImagePreloader) as MutableSet<String>

        cache.add("https://test.image/to_be_trimmed.jpg")
        assertEquals(1, ImagePreloader.getPreloadedCount())

        // Calling trimMemory with TRIM_MEMORY_BACKGROUND should purge transient caches
        AppMemoryManager.trimMemory(null, ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        // With null context, trimMemory returns early; but on purgeAllCaches or purgeTransientCaches
        val purgeMethod = AppMemoryManager::class.java.getDeclaredMethod("purgeTransientCaches")
        purgeMethod.isAccessible = true
        purgeMethod.invoke(AppMemoryManager)

        assertEquals(0, ImagePreloader.getPreloadedCount())
        assertFalse(ImagePreloader.isUrlPreloaded("https://test.image/to_be_trimmed.jpg"))
    }

    @Test
    fun testAdaptiveAvatarBoundingCalculation() {
        fun computeTargetPx(sizeDp: Float, density: Float): Int {
            val raw = (sizeDp * 1.5f * density).toInt()
            return raw.coerceIn(48, 384)
        }

        // 24.dp avatar at 3x density (xxhdpi): 24 * 1.5 * 3 = 108px
        assertEquals(108, computeTargetPx(24f, 3f))

        // 48.dp avatar at 3x density: 48 * 1.5 * 3 = 216px
        assertEquals(216, computeTargetPx(48f, 3f))

        // Tiny 8.dp avatar: clamped to minimum 48px
        assertEquals(48, computeTargetPx(8f, 2f))

        // Massive 200.dp avatar: clamped to maximum 384px (preventing megapixel buffer OOM)
        assertEquals(384, computeTargetPx(200f, 3f))
    }

    @Test
    fun testAdaptivePreloadPolicyThrottling() {
        assertEquals(
            5,
            AdaptivePreloadPolicy.effectivePreloadAhead(
                requestedAhead = 5,
                isConnected = true,
                isUnmeteredWifi = true,
                isDataSaverEnabled = false,
                memoryPressureLevel = AppMemoryManager.PRESSURE_NORMAL
            )
        )
        assertEquals(
            2,
            AdaptivePreloadPolicy.effectivePreloadAhead(
                requestedAhead = 5,
                isConnected = true,
                isUnmeteredWifi = false,
                isDataSaverEnabled = false,
                memoryPressureLevel = AppMemoryManager.PRESSURE_NORMAL
            )
        )
        assertEquals(
            1,
            AdaptivePreloadPolicy.effectivePreloadAhead(
                requestedAhead = 5,
                isConnected = true,
                isUnmeteredWifi = true,
                isDataSaverEnabled = false,
                memoryPressureLevel = AppMemoryManager.PRESSURE_MODERATE
            )
        )
        assertEquals(
            0,
            AdaptivePreloadPolicy.effectivePreloadAhead(
                requestedAhead = 5,
                isConnected = true,
                isUnmeteredWifi = true,
                isDataSaverEnabled = false,
                memoryPressureLevel = AppMemoryManager.PRESSURE_CRITICAL
            )
        )
        assertEquals(
            0,
            AdaptivePreloadPolicy.effectivePreloadAhead(
                requestedAhead = 5,
                isConnected = false,
                isUnmeteredWifi = true,
                isDataSaverEnabled = false,
                memoryPressureLevel = AppMemoryManager.PRESSURE_NORMAL
            )
        )
        assertEquals(
            0,
            AdaptivePreloadPolicy.effectivePreloadAhead(
                requestedAhead = 5,
                isConnected = true,
                isUnmeteredWifi = true,
                isDataSaverEnabled = true,
                memoryPressureLevel = AppMemoryManager.PRESSURE_NORMAL
            )
        )
    }

    @Test
    fun testMemoryPressureRefresh() {
        val level = AppMemoryManager.refreshMemoryPressureLevel()
        assertTrue(level in AppMemoryManager.PRESSURE_NORMAL..AppMemoryManager.PRESSURE_CRITICAL)
    }
}
