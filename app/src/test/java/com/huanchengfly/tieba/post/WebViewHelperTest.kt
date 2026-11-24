package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.ui.page.webview.WebResourceInterceptor
import com.huanchengfly.tieba.post.utils.WebViewHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun testWebResourceInterceptor_isAdHost() {
        assertTrue(WebResourceInterceptor.isAdHost("pos.baidu.com"))
        assertTrue(WebResourceInterceptor.isAdHost("mobads.baidu.com"))
        assertTrue(WebResourceInterceptor.isAdHost("cpro.baidustatic.com"))
        assertTrue(WebResourceInterceptor.isAdHost("sub.dup.baidustatic.com"))
        assertTrue(WebResourceInterceptor.isAdHost("union.baidu.com"))
        assertTrue(WebResourceInterceptor.isAdHost("hm.baidu.com"))

        assertFalse(WebResourceInterceptor.isAdHost("tieba.baidu.com"))
        assertFalse(WebResourceInterceptor.isAdHost("tb1.bdstatic.com"))
        assertFalse(WebResourceInterceptor.isAdHost("wapp.baidu.com"))
        assertFalse(WebResourceInterceptor.isAdHost("c.tieba.baidu.com"))
    }

    @Test
    fun testWebResourceInterceptor_getMimeTypeAndIsStaticResource() {
        assertEquals("application/javascript", WebResourceInterceptor.getMimeType("https://tb1.bdstatic.com/tb/static-common/lib/mod_99217fa.js?v=2024"))
        assertEquals("text/css", WebResourceInterceptor.getMimeType("https://tb1.bdstatic.com/tb/static-frs/css/common_791448f.css#hash"))
        assertEquals("font/woff2", WebResourceInterceptor.getMimeType("https://tb1.bdstatic.com/font.woff2"))
        assertEquals("font/woff", WebResourceInterceptor.getMimeType("https://tb1.bdstatic.com/font.woff"))
        assertEquals("font/ttf", WebResourceInterceptor.getMimeType("https://tb1.bdstatic.com/font.ttf"))
        assertEquals("image/png", WebResourceInterceptor.getMimeType("https://tiebaimg.baidu.com/icon.png"))
        assertEquals("image/jpeg", WebResourceInterceptor.getMimeType("https://tiebaimg.baidu.com/photo.jpeg"))
        assertEquals("image/webp", WebResourceInterceptor.getMimeType("https://tiebaimg.baidu.com/banner.webp"))
        assertEquals("image/svg+xml", WebResourceInterceptor.getMimeType("https://tiebaimg.baidu.com/logo.svg"))
        assertEquals("image/x-icon", WebResourceInterceptor.getMimeType("https://tiebac.baidu.com/favicon.ico"))

        assertNull(WebResourceInterceptor.getMimeType("https://tieba.baidu.com/p/12345678"))
        assertNull(WebResourceInterceptor.getMimeType("https://tieba.baidu.com/f?kw=android"))
        assertNull(WebResourceInterceptor.getMimeType("https://wappass.baidu.com/passport"))

        assertTrue(WebResourceInterceptor.isStaticResource("https://static.tieba.com/script.js"))
        assertFalse(WebResourceInterceptor.isStaticResource("https://tieba.baidu.com/mo/q/m"))
    }
}

