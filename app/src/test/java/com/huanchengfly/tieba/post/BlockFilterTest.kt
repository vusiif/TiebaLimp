package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.BlockManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockFilterTest {
    @Test
    fun normalizeIpAddress_handlesPrefixesAndTrimming() {
        assertEquals("北京", BlockManager.normalizeIpAddress("IP属地:北京"))
        assertEquals("广东", BlockManager.normalizeIpAddress("IP属地：广东"))
        assertEquals("海外", BlockManager.normalizeIpAddress("IP属地 海外"))
        assertEquals("上海", BlockManager.normalizeIpAddress("上海"))
        assertEquals("", BlockManager.normalizeIpAddress(null))
        assertEquals("", BlockManager.normalizeIpAddress("   "))
    }

    @Test
    fun regexCompilation_validRegexDoesNotThrow() {
        val pattern = "^(广告|推广|兼职).*"
        val regex = Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
        assertTrue(regex.containsMatchIn("广告兼职日结"))
        assertFalse(regex.containsMatchIn("正常讨论帖子"))
    }
}
