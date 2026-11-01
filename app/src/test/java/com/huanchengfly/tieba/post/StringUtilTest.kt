package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.StringUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class StringUtilTest {
    @Test
    fun getAvatarUrl_returnsHttpsUrl() {
        val portrait = "tb.1.test.avatar"
        val url = StringUtil.getAvatarUrl(portrait)
        assertEquals("https://tb.himg.baidu.com/sys/portrait/item/tb.1.test.avatar", url)
    }

    @Test
    fun getAvatarUrl_preservesExistingHttpAndHttps() {
        val httpUrl = "http://example.com/pic.jpg"
        val httpsUrl = "https://example.com/pic.jpg"
        assertEquals(httpUrl, StringUtil.getAvatarUrl(httpUrl))
        assertEquals(httpsUrl, StringUtil.getAvatarUrl(httpsUrl))
    }

    @Test
    fun getAvatarUrl_emptyInputReturnsEmpty() {
        assertEquals("", StringUtil.getAvatarUrl(null))
        assertEquals("", StringUtil.getAvatarUrl(""))
    }

    @Test
    fun getBigAvatarUrl_returnsHttpsUrl() {
        val portrait = "tb.1.test.avatar"
        val url = StringUtil.getBigAvatarUrl(portrait)
        assertEquals("https://tb.himg.baidu.com/sys/portraith/item/tb.1.test.avatar", url)
    }
}
