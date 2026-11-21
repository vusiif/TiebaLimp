package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.utils.BlockManager.FilterResult
import com.huanchengfly.tieba.post.utils.BlockManager.SimulationResult
import java.util.concurrent.ConcurrentHashMap

object BlockRuleMatcher {
    private val compiledRegexCache = ConcurrentHashMap<String, Regex?>()
    private val parsedKeywordsCache = ConcurrentHashMap<String, List<String>>()

    fun evictPattern(pattern: String) {
        compiledRegexCache.remove(pattern)
    }

    fun clearCache() {
        compiledRegexCache.clear()
        parsedKeywordsCache.clear()
    }

    private fun getOrCompileRegex(pattern: String): Regex? {
        if (BlockFilterUtil.isDangerousRegexPattern(pattern)) return null
        return compiledRegexCache.computeIfAbsent(pattern) {
            runCatching {
                Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
            }.getOrNull()
        }
    }

    private fun getOrParseKeywords(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return parsedKeywordsCache.computeIfAbsent(raw) {
            runCatching { raw.fromJson<List<String>>() }.getOrNull() ?: emptyList()
        }
    }

    fun isWhitelistedUser(
        blockList: List<Block>,
        userId: Long = 0L,
        userName: String? = null,
        forumName: String? = null,
    ): Boolean {
        if (userId <= 0L && userName.isNullOrBlank()) return false
        for (block in blockList) {
            if (block.category == Block.CATEGORY_WHITE_LIST && block.type == Block.TYPE_USER) {
                if (!BlockFilterUtil.matchesForumScope(block.forum, forumName)) continue
                if ((userId > 0L && block.uid == userId.toString()) ||
                    (!userName.isNullOrBlank() && block.username == userName)
                ) {
                    return true
                }
            }
        }
        return false
    }

    fun checkBlockContent(
        blockList: List<Block>,
        content: CharSequence?,
        forumName: String? = null,
    ): FilterResult {
        if (content.isNullOrEmpty()) return FilterResult.Allowed
        val contentStr = content.toString()

        // 1. Whitelist pass
        for (block in blockList) {
            if (block.category != Block.CATEGORY_WHITE_LIST) continue
            if (!BlockFilterUtil.matchesForumScope(block.forum, forumName)) continue
            when (block.type) {
                Block.TYPE_KEYWORD -> {
                    val kws = getOrParseKeywords(block.keywords)
                    if (kws.isNotEmpty() && kws.all { contentStr.contains(it, ignoreCase = true) }) {
                        return FilterResult.Allowed
                    }
                }
                Block.TYPE_REGEX -> {
                    val pattern = block.pattern
                    if (!pattern.isNullOrBlank()) {
                        val regex = getOrCompileRegex(pattern)
                        val boundedContent = if (content.length > 4000) content.subSequence(0, 4000) else content
                        if (regex != null && regex.containsMatchIn(boundedContent)) {
                            return FilterResult.Allowed
                        }
                    }
                }
            }
        }

        // 2. Blacklist pass
        for (block in blockList) {
            if (block.category != Block.CATEGORY_BLACK_LIST) continue
            if (!BlockFilterUtil.matchesForumScope(block.forum, forumName)) continue
            when (block.type) {
                Block.TYPE_KEYWORD -> {
                    val kws = getOrParseKeywords(block.keywords)
                    if (kws.isNotEmpty() && kws.all { contentStr.contains(it, ignoreCase = true) }) {
                        val forumTag = if (!block.forum.isNullOrBlank()) " (${block.forum}吧)" else ""
                        return FilterResult.Blocked(
                            type = Block.TYPE_KEYWORD,
                            reason = "关键词: ${kws.joinToString(" ")}$forumTag",
                            ruleId = block.id,
                            matchedTarget = kws.joinToString(" ")
                        )
                    }
                }
                Block.TYPE_REGEX -> {
                    val pattern = block.pattern
                    if (!pattern.isNullOrBlank()) {
                        val regex = getOrCompileRegex(pattern)
                        val boundedContent = if (content.length > 4000) content.subSequence(0, 4000) else content
                        if (regex != null && regex.containsMatchIn(boundedContent)) {
                            val forumTag = if (!block.forum.isNullOrBlank()) " (${block.forum}吧)" else ""
                            return FilterResult.Blocked(
                                type = Block.TYPE_REGEX,
                                reason = "正则表达式: $pattern$forumTag",
                                ruleId = block.id,
                                matchedTarget = pattern
                            )
                        }
                    }
                }
            }
        }

        return FilterResult.Allowed
    }

    fun checkBlockUser(
        blockList: List<Block>,
        userId: Long = 0L,
        userName: String? = null,
        forumName: String? = null,
    ): FilterResult {
        if (isWhitelistedUser(blockList, userId, userName, forumName)) return FilterResult.Allowed
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_USER) {
                if (!BlockFilterUtil.matchesForumScope(block.forum, forumName)) continue
                if ((userId > 0L && block.uid == userId.toString()) ||
                    (!userName.isNullOrBlank() && block.username == userName)
                ) {
                    val forumTag = if (!block.forum.isNullOrBlank()) " (${block.forum}吧)" else ""
                    return FilterResult.Blocked(
                        type = Block.TYPE_USER,
                        reason = "黑名单用户: ${block.username ?: block.uid}$forumTag",
                        ruleId = block.id,
                        matchedTarget = block.username ?: block.uid.orEmpty()
                    )
                }
            }
        }
        return FilterResult.Allowed
    }

    fun checkBlockIp(
        blockList: List<Block>,
        rawIp: String?,
        forumName: String? = null,
    ): FilterResult {
        val cleanIp = BlockFilterUtil.normalizeIpAddress(rawIp)
        if (cleanIp.isEmpty()) return FilterResult.Allowed

        // Check whitelist
        for (block in blockList) {
            if (block.category == Block.CATEGORY_WHITE_LIST && block.type == Block.TYPE_IP_REGION) {
                if (!BlockFilterUtil.matchesForumScope(block.forum, forumName)) continue
                val target = block.ipRegion?.trim().orEmpty()
                if (target.isNotEmpty() && (cleanIp.contains(target, ignoreCase = true) || target.contains(cleanIp, ignoreCase = true))) {
                    return FilterResult.Allowed
                }
            }
        }

        // Check blacklist
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_IP_REGION) {
                if (!BlockFilterUtil.matchesForumScope(block.forum, forumName)) continue
                val target = block.ipRegion?.trim().orEmpty()
                if (target.isNotEmpty() && (cleanIp.contains(target, ignoreCase = true) || target.contains(cleanIp, ignoreCase = true))) {
                    val forumTag = if (!block.forum.isNullOrBlank()) " (${block.forum}吧)" else ""
                    return FilterResult.Blocked(
                        type = Block.TYPE_IP_REGION,
                        reason = "IP 属地: $target$forumTag",
                        ruleId = block.id,
                        matchedTarget = target
                    )
                }
            }
        }

        return FilterResult.Allowed
    }

    fun checkBlockLevel(
        blockList: List<Block>,
        level: Int,
        forumName: String? = null,
    ): FilterResult {
        if (level <= 0) return FilterResult.Allowed
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_USER_LEVEL && block.minLevel > 0) {
                if (!BlockFilterUtil.matchesForumScope(block.forum, forumName)) continue
                if (level < block.minLevel) {
                    val forumTag = if (!block.forum.isNullOrBlank()) " (${block.forum}吧)" else ""
                    return FilterResult.Blocked(
                        type = Block.TYPE_USER_LEVEL,
                        reason = "等级门限: 低于 Lv.${block.minLevel}$forumTag",
                        ruleId = block.id,
                        matchedTarget = "Lv.${block.minLevel}"
                    )
                }
            }
        }
        return FilterResult.Allowed
    }

    fun simulateCheck(
        blockList: List<Block>,
        content: String = "",
        userId: Long = 0L,
        userName: String = "",
        userLevel: Int = 0,
        userIp: String = "",
        forumName: String = "",
    ): SimulationResult {
        val trimmedForum = forumName.trim().ifEmpty { null }
        val effectiveUserId = userId.takeIf { it > 0L } ?: 0L
        val effectiveUserName = userName.trim().ifEmpty { null }
        val cleanIp = BlockFilterUtil.normalizeIpAddress(userIp)
        val boundedContent = if (content.length > 4000) content.substring(0, 4000) else content

        var hitWhitelistRule: Block? = null
        var hitWhitelistReason: String? = null

        // 1. User dimension (whitelist strictly exempts user blacklist)
        val isUserWhitelisted = isWhitelistedUser(blockList, effectiveUserId, effectiveUserName, trimmedForum)
        if (isUserWhitelisted) {
            val rule = blockList.firstOrNull {
                it.category == Block.CATEGORY_WHITE_LIST && it.type == Block.TYPE_USER &&
                        BlockFilterUtil.matchesForumScope(it.forum, trimmedForum) &&
                        ((effectiveUserId > 0L && it.uid == effectiveUserId.toString()) ||
                                (effectiveUserName != null && it.username == effectiveUserName))
            }
            if (rule != null) {
                hitWhitelistRule = rule
                hitWhitelistReason = "命中白名单用户: ${rule.username ?: rule.uid}"
            }
        } else {
            val userResult = checkBlockUser(blockList, effectiveUserId, effectiveUserName, trimmedForum)
            if (userResult is FilterResult.Blocked) {
                val rule = blockList.firstOrNull { it.id == userResult.ruleId }
                return SimulationResult(
                    isBlocked = true,
                    isWhitelisted = false,
                    hitRule = rule,
                    reason = userResult.reason
                )
            }
        }

        // 2. Level dimension (level threshold has no whitelist, blocks immediately)
        val levelResult = checkBlockLevel(blockList, userLevel, trimmedForum)
        if (levelResult is FilterResult.Blocked) {
            val rule = blockList.firstOrNull { it.id == levelResult.ruleId }
            return SimulationResult(
                isBlocked = true,
                isWhitelisted = false,
                hitRule = rule,
                reason = levelResult.reason
            )
        }

        // 3. IP dimension (IP whitelist strictly exempts IP blacklist)
        var isIpWhitelisted = false
        if (cleanIp.isNotEmpty()) {
            val ipWhitelistRule = blockList.firstOrNull {
                it.category == Block.CATEGORY_WHITE_LIST && it.type == Block.TYPE_IP_REGION &&
                        BlockFilterUtil.matchesForumScope(it.forum, trimmedForum) &&
                        !it.ipRegion.isNullOrBlank() &&
                        (cleanIp.contains(it.ipRegion.trim(), ignoreCase = true) || it.ipRegion.trim().contains(cleanIp, ignoreCase = true))
            }
            if (ipWhitelistRule != null) {
                isIpWhitelisted = true
                if (hitWhitelistRule == null) {
                    hitWhitelistRule = ipWhitelistRule
                    hitWhitelistReason = "命中白名单 IP 属地: ${ipWhitelistRule.ipRegion}"
                }
            }
        }
        if (!isIpWhitelisted) {
            val ipResult = checkBlockIp(blockList, cleanIp, trimmedForum)
            if (ipResult is FilterResult.Blocked) {
                val rule = blockList.firstOrNull { it.id == ipResult.ruleId }
                return SimulationResult(
                    isBlocked = true,
                    isWhitelisted = false,
                    hitRule = rule,
                    reason = ipResult.reason
                )
            }
        }

        // 4. Content dimension (Content whitelist strictly exempts content blacklist)
        var isContentWhitelisted = false
        if (content.isNotBlank()) {
            val contentWhitelistRule = blockList.firstOrNull { block ->
                if (block.category != Block.CATEGORY_WHITE_LIST || !BlockFilterUtil.matchesForumScope(block.forum, trimmedForum)) return@firstOrNull false
                when (block.type) {
                    Block.TYPE_KEYWORD -> {
                        val kws = getOrParseKeywords(block.keywords)
                        kws.isNotEmpty() && kws.all { content.contains(it, ignoreCase = true) }
                    }
                    Block.TYPE_REGEX -> {
                        val pattern = block.pattern
                        if (!pattern.isNullOrBlank()) {
                            val regex = getOrCompileRegex(pattern)
                            regex != null && regex.containsMatchIn(boundedContent)
                        } else false
                    }
                    else -> false
                }
            }
            if (contentWhitelistRule != null) {
                isContentWhitelisted = true
                if (hitWhitelistRule == null) {
                    hitWhitelistRule = contentWhitelistRule
                    hitWhitelistReason = when (contentWhitelistRule.type) {
                        Block.TYPE_KEYWORD -> "命中白名单关键词: ${contentWhitelistRule.keywords}"
                        Block.TYPE_REGEX -> "命中白名单正则: ${contentWhitelistRule.pattern}"
                        else -> "命中白名单内容"
                    }
                }
            }
        }
        if (!isContentWhitelisted && content.isNotBlank()) {
            val contentResult = checkBlockContent(blockList, boundedContent, trimmedForum)
            if (contentResult is FilterResult.Blocked) {
                val rule = blockList.firstOrNull { it.id == contentResult.ruleId }
                return SimulationResult(
                    isBlocked = true,
                    isWhitelisted = false,
                    hitRule = rule,
                    reason = contentResult.reason
                )
            }
        }

        // No dimension was blocked
        if (hitWhitelistRule != null) {
            return SimulationResult(
                isBlocked = false,
                isWhitelisted = true,
                hitRule = hitWhitelistRule,
                reason = hitWhitelistReason ?: "命中白名单放行"
            )
        }

        return SimulationResult(
            isBlocked = false,
            isWhitelisted = false,
            reason = "未命中任何过滤规则 (允许正常展示)"
        )
    }
}
