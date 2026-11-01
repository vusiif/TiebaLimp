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

    @Test
    fun matchesForumScope_validatesScopingCorrectly() {
        // Global rules (null or blank forum) match everything
        assertTrue(BlockManager.matchesForumScope(null, "孙笑川"))
        assertTrue(BlockManager.matchesForumScope("", "显卡"))
        assertTrue(BlockManager.matchesForumScope("   ", null))

        // Forum specific rules
        assertTrue(BlockManager.matchesForumScope("孙笑川", "孙笑川"))
        assertTrue(BlockManager.matchesForumScope("孙笑川", " 孙笑川 "))
        assertTrue(BlockManager.matchesForumScope("GPU", "gpu"))
        assertFalse(BlockManager.matchesForumScope("孙笑川", "显卡"))

        // Unspecified current forum lets forum-specific rule pass through
        assertTrue(BlockManager.matchesForumScope("孙笑川", null))
        assertTrue(BlockManager.matchesForumScope("孙笑川", ""))
    }

    @Test
    fun simulateCheck_defaultAllowsContentWhenNoRulesHit() {
        val res = BlockManager.simulateCheck(
            content = "大家好，这是一条正常测试发言",
            userId = 12345L,
            userName = "正常用户",
            userLevel = 8,
            userIp = "北京",
            forumName = "贴吧",
        )
        assertFalse(res.isBlocked)
        assertFalse(res.isWhitelisted)
        assertTrue(res.reason.contains("未命中"))
    }

    @Test
    fun filterResult_sealedInterfaceContract() {
        val allowed: BlockManager.FilterResult = BlockManager.FilterResult.Allowed
        assertTrue(allowed is BlockManager.FilterResult.Allowed)

        val blocked: BlockManager.FilterResult = BlockManager.FilterResult.Blocked(
            type = 0,
            reason = "关键词: 刷单",
            ruleId = 99L,
            matchedTarget = "刷单"
        )
        assertTrue(blocked is BlockManager.FilterResult.Blocked)
        assertEquals(99L, (blocked as BlockManager.FilterResult.Blocked).ruleId)
        assertEquals("关键词: 刷单", blocked.reason)
        assertEquals("刷单", blocked.matchedTarget)
    }
}
