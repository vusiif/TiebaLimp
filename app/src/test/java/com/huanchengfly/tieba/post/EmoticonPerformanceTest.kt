package com.huanchengfly.tieba.post

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.huanchengfly.tieba.post.utils.EmoticonUtil
import com.huanchengfly.tieba.post.utils.EmoticonUtil.emoticonString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EmoticonPerformanceTest {

    @Before
    fun setUp() {
        EmoticonUtil.clearCache()
    }

    @Test
    fun testGetPattern_returnsPrecompiledSingletons() {
        val standard1 = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_ALL_TYPE)
        val standard2 = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_CLASSIC_TYPE)
        val standard3 = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_EMOJI_TYPE)

        assertSame(standard1, standard2)
        assertSame(standard2, standard3)

        val web1 = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_ALL_WEB_TYPE)
        val web2 = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_CLASSIC_WEB_TYPE)
        val web3 = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_EMOJI_WEB_TYPE)

        assertSame(web1, web2)
        assertSame(web2, web3)
    }

    @Test
    fun testPrecompiledPattern_matchingAccuracy() {
        val standardPattern = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_ALL_TYPE)
        val matcher1 = standardPattern.matcher("Hello #(滑稽) world #(呵呵) end")
        assertTrue(matcher1.find())
        assertEquals("滑稽", matcher1.group(1))
        assertTrue(matcher1.find())
        assertEquals("呵呵", matcher1.group(1))
        assertFalse(matcher1.find())

        val webPattern = EmoticonUtil.getPattern(EmoticonUtil.EMOTICON_ALL_WEB_TYPE)
        val matcherWeb = webPattern.matcher("Hello (#滑稽) web (#haha) end")
        assertTrue(matcherWeb.find())
        assertEquals("滑稽", matcherWeb.group(1))
        assertTrue(matcherWeb.find())
        assertEquals("haha", matcherWeb.group(1))
        assertFalse(matcherWeb.find())
    }

    @Test
    fun testAnnotatedString_emoticonString_withoutEmoticons() {
        val plain = AnnotatedString("This is a plain text without any emoticon symbols.")
        val result = plain.emoticonString

        assertEquals(plain.text, result.text)
        assertTrue(result.hasStringAnnotations("Emoticon", 0, result.length))
        assertFalse(result.hasStringAnnotations(EmoticonUtil.INLINE_CONTENT_TAG, 0, result.length))
    }

    @Test
    fun testAnnotatedString_emoticonString_withEmoticons_caching() {
        val textWithEmoticon = AnnotatedString("Look at this #(滑稽) test")
        val result1 = textWithEmoticon.emoticonString
        val result2 = textWithEmoticon.emoticonString

        // Cached instance should be returned for same text
        assertSame(result1, result2)
        assertTrue(result1.hasStringAnnotations("Emoticon", 0, result1.length))
        assertTrue(result1.hasStringAnnotations(EmoticonUtil.INLINE_CONTENT_TAG, 0, result1.length))
    }

    @Test
    fun testClearCache_clearsEmoticonCacheSafely() {
        val textWithEmoticon = AnnotatedString("Look at this #(滑稽) test")
        val result1 = textWithEmoticon.emoticonString

        EmoticonUtil.clearCache()

        val result2 = textWithEmoticon.emoticonString
        assertEquals(result1.text, result2.text)
        // After clear, new instance is generated
        assertEquals(result1, result2)
    }
}
