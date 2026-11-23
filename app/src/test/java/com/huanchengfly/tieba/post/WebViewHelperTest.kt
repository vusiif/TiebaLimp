package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.WebViewHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class WebViewHelperTest {

    @Test
    fun testCalculateTextZoomStandard() {
        assertEquals(100, WebViewHelper.calculateTextZoom(1.0f))
        assertEquals(120, WebViewHelper.calculateTextZoom(1.2f))
        assertEquals(150, WebViewHelper.calculateTextZoom(1.5f))
        assertEquals(85, WebViewHelper.calculateTextZoom(0.85f))
    }

    @Test
    fun testCalculateTextZoomBoundaries() {
        // Values lower than 0.5f should be clamped to 50
        assertEquals(50, WebViewHelper.calculateTextZoom(0.1f))
        assertEquals(50, WebViewHelper.calculateTextZoom(0.49f))

        // Values higher than 2.0f should be clamped to 200
        assertEquals(200, WebViewHelper.calculateTextZoom(2.01f))
        assertEquals(200, WebViewHelper.calculateTextZoom(4.0f))
    }
}
