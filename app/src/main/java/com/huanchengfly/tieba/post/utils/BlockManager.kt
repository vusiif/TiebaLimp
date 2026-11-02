package com.huanchengfly.tieba.post.utils

import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Block.Companion.getKeywords
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.findAllAsync
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object BlockManager {
    private val blockList: MutableList<Block> = CopyOnWriteArrayList()
    private val compiledRegexCache = ConcurrentHashMap<String, Regex?>()
    private val parsedKeywordsCache = ConcurrentHashMap<String, List<String>>()

    val blackList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_BLACK_LIST }

    val whiteList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_WHITE_LIST }

    data class BlockRuleDto(
        val category: Int,
        val type: Int,
        val keywords: List<String>? = null,
        val username: String? = null,
        val uid: String? = null,
        val pattern: String? = null,
        val minLevel: Int? = null,
        val ipRegion: String? = null,
        val forum: String? = null,
    )

    sealed interface FilterResult {
        data object Allowed : FilterResult
        data class Blocked(
            val type: Int,
            val reason: String,
            val ruleId: Long = 0L,
            val matchedTarget: String = "",
        ) : FilterResult
    }

    data class SimulationResult(
        val isBlocked: Boolean,
        val isWhitelisted: Boolean,
        val hitRule: Block? = null,
        val reason: String = "",
    )

    fun isDangerousRegexPattern(pattern: String?): Boolean {
        if (pattern.isNullOrBlank()) return false
        if (pattern.length > 200) return true
        val nestedQuantifiers = Regex("""(\+|\*|\{\d+,?\d*\})\s*(\+|\*|\{\d+,?\d*\})|\([^\)]*(\+|\*)[^\)]*\)\s*(\+|\*|\{\d+,?\d*\})""")
        return nestedQuantifiers.containsMatchIn(pattern)
    }

    private fun getOrCompileRegex(pattern: String): Regex? {
        if (isDangerousRegexPattern(pattern)) return null
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

    fun addBlock(block: Block) {
        block.save()
        blockList.add(block)
        if (block.type == Block.TYPE_REGEX && !block.pattern.isNullOrBlank()) {
            compiledRegexCache.remove(block.pattern)
        }
    }

    fun addBlockAsync(
        block: Block,
        callback: ((Boolean) -> Unit)? = null,
    ) {
        block.saveAsync()
            .listen {
                callback?.invoke(it)
                blockList.add(block)
                if (block.type == Block.TYPE_REGEX && !block.pattern.isNullOrBlank()) {
                    compiledRegexCache.remove(block.pattern)
                }
            }
    }

    fun removeBlock(id: Long) {
        LitePal.delete<Block>(id)
        val removed = blockList.firstOrNull { it.id == id }
        blockList.removeAll { it.id == id }
        if (removed?.type == Block.TYPE_REGEX && !removed.pattern.isNullOrBlank()) {
            compiledRegexCache.remove(removed.pattern)
        }
    }

    fun clearAll(category: Int? = null) {
        if (category == null) {
            LitePal.deleteAll(Block::class.java)
            blockList.clear()
        } else {
            LitePal.deleteAll(Block::class.java, "category = ?", category.toString())
            blockList.removeAll { it.category == category }
        }
        compiledRegexCache.clear()
        parsedKeywordsCache.clear()
    }

    fun init() {
        LitePal.findAllAsync<Block>().listen { blocks ->
            blockList.clear()
            blockList.addAll(blocks)
            compiledRegexCache.clear()
            parsedKeywordsCache.clear()
        }
    }

    fun normalizeIpAddress(ip: String?): String {
        if (ip.isNullOrBlank()) return ""
        return ip
            .removePrefix("IP属地:")
            .removePrefix("IP属地：")
            .removePrefix("IP属地")
            .trim()
    }

    fun matchesForumScope(ruleForum: String?, currentForum: String?): Boolean {
        if (ruleForum.isNullOrBlank()) return true
        if (currentForum.isNullOrBlank()) return false
        return ruleForum.trim().equals(currentForum.trim(), ignoreCase = true)
    }

    fun isWhitelistedUser(userId: Long = 0L, userName: String? = null, forumName: String? = null): Boolean {
        if (userId <= 0L && userName.isNullOrBlank()) return false
        for (block in blockList) {
            if (block.category == Block.CATEGORY_WHITE_LIST && block.type == Block.TYPE_USER) {
                if (!matchesForumScope(block.forum, forumName)) continue
                if ((userId > 0L && block.uid == userId.toString()) ||
                    (!userName.isNullOrBlank() && block.username == userName)
                ) {
                    return true
                }
            }
        }
        return false
    }

    fun checkBlockContent(content: CharSequence?, forumName: String? = null): FilterResult {
        if (content.isNullOrEmpty()) return FilterResult.Allowed
        val contentStr = content.toString()

        // 1. Whitelist pass
        for (block in blockList) {
            if (block.category != Block.CATEGORY_WHITE_LIST) continue
            if (!matchesForumScope(block.forum, forumName)) continue
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
            if (!matchesForumScope(block.forum, forumName)) continue
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

    fun checkBlockUser(userId: Long = 0L, userName: String? = null, forumName: String? = null): FilterResult {
        if (isWhitelistedUser(userId, userName, forumName)) return FilterResult.Allowed
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_USER) {
                if (!matchesForumScope(block.forum, forumName)) continue
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

    fun checkBlockIp(rawIp: String?, forumName: String? = null): FilterResult {
        val cleanIp = normalizeIpAddress(rawIp)
        if (cleanIp.isEmpty()) return FilterResult.Allowed

        // Check whitelist
        for (block in blockList) {
            if (block.category == Block.CATEGORY_WHITE_LIST && block.type == Block.TYPE_IP_REGION) {
                if (!matchesForumScope(block.forum, forumName)) continue
                val target = block.ipRegion?.trim().orEmpty()
                if (target.isNotEmpty() && (cleanIp.contains(target, ignoreCase = true) || target.contains(cleanIp, ignoreCase = true))) {
                    return FilterResult.Allowed
                }
            }
        }

        // Check blacklist
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_IP_REGION) {
                if (!matchesForumScope(block.forum, forumName)) continue
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

    fun checkBlockLevel(level: Int, forumName: String? = null): FilterResult {
        if (level <= 0) return FilterResult.Allowed
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_USER_LEVEL && block.minLevel > 0) {
                if (!matchesForumScope(block.forum, forumName)) continue
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

    fun shouldBlockContent(content: CharSequence?, forumName: String? = null): Boolean =
        checkBlockContent(content, forumName) is FilterResult.Blocked

    fun shouldBlockUser(userId: Long = 0L, userName: String? = null, forumName: String? = null): Boolean =
        checkBlockUser(userId, userName, forumName) is FilterResult.Blocked

    fun shouldBlockIp(rawIp: String?, forumName: String? = null): Boolean =
        checkBlockIp(rawIp, forumName) is FilterResult.Blocked

    fun shouldBlockLevel(level: Int, forumName: String? = null): Boolean =
        checkBlockLevel(level, forumName) is FilterResult.Blocked

    fun shouldBlock(content: String): Boolean = shouldBlockContent(content)

    fun shouldBlock(userId: Long = 0L, userName: String? = null): Boolean = shouldBlockUser(userId, userName)

    fun simulateCheck(
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
        val cleanIp = normalizeIpAddress(userIp)
        val boundedContent = if (content.length > 4000) content.substring(0, 4000) else content

        var hitWhitelistRule: Block? = null
        var hitWhitelistReason: String? = null

        // 1. User dimension (whitelist strictly exempts user blacklist)
        val isUserWhitelisted = isWhitelistedUser(effectiveUserId, effectiveUserName, trimmedForum)
        if (isUserWhitelisted) {
            val rule = blockList.firstOrNull {
                it.category == Block.CATEGORY_WHITE_LIST && it.type == Block.TYPE_USER &&
                        matchesForumScope(it.forum, trimmedForum) &&
                        ((effectiveUserId > 0L && it.uid == effectiveUserId.toString()) ||
                                (effectiveUserName != null && it.username == effectiveUserName))
            }
            if (rule != null) {
                hitWhitelistRule = rule
                hitWhitelistReason = "命中白名单用户: ${rule.username ?: rule.uid}"
            }
        } else {
            val userResult = checkBlockUser(effectiveUserId, effectiveUserName, trimmedForum)
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
        val levelResult = checkBlockLevel(userLevel, trimmedForum)
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
                        matchesForumScope(it.forum, trimmedForum) &&
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
            val ipResult = checkBlockIp(cleanIp, trimmedForum)
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
                if (block.category != Block.CATEGORY_WHITE_LIST || !matchesForumScope(block.forum, trimmedForum)) return@firstOrNull false
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
            val contentResult = checkBlockContent(boundedContent, trimmedForum)
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

    fun exportRulesJson(): String {
        val dtos = blockList.map { block ->
            BlockRuleDto(
                category = block.category,
                type = block.type,
                keywords = if (block.type == Block.TYPE_KEYWORD) block.getKeywords() else null,
                username = block.username,
                uid = block.uid,
                pattern = block.pattern,
                minLevel = if (block.minLevel > 0) block.minLevel else null,
                ipRegion = block.ipRegion,
                forum = block.forum,
            )
        }
        return GsonUtil.getGson().toJson(dtos)
    }

    fun isDuplicateRule(newBlock: Block): Boolean {
        return blockList.any { existing ->
            existing.category == newBlock.category &&
            existing.type == newBlock.type &&
            existing.forum == newBlock.forum &&
            when (newBlock.type) {
                Block.TYPE_KEYWORD -> existing.keywords == newBlock.keywords
                Block.TYPE_USER -> (existing.uid != null && existing.uid == newBlock.uid) ||
                        (!existing.username.isNullOrBlank() && existing.username == newBlock.username)
                Block.TYPE_REGEX -> existing.pattern == newBlock.pattern
                Block.TYPE_USER_LEVEL -> existing.minLevel == newBlock.minLevel
                Block.TYPE_IP_REGION -> existing.ipRegion?.trim().equals(newBlock.ipRegion?.trim(), ignoreCase = true)
                else -> false
            }
        }
    }

    fun importRulesFromJson(json: String): Int {
        if (json.isBlank() || json.length > 500_000) return 0
        val gson = GsonUtil.getGson()
        val type = object : TypeToken<List<BlockRuleDto>>() {}.type
        val importedList: List<BlockRuleDto> = runCatching {
            gson.fromJson<List<BlockRuleDto>>(json, type)
        }.getOrNull() ?: emptyList()

        if (importedList.isEmpty()) return 0

        var count = 0
        val validList = importedList.take(500)
        for (dto in validList) {
            val category = dto.category.takeIf { it == Block.CATEGORY_BLACK_LIST || it == Block.CATEGORY_WHITE_LIST } ?: continue
            val ruleType = dto.type.takeIf { it in Block.TYPE_KEYWORD..Block.TYPE_IP_REGION } ?: continue

            if (ruleType == Block.TYPE_REGEX) {
                val p = dto.pattern?.trim()
                if (p.isNullOrBlank() || p.length > 200 || isDangerousRegexPattern(p) || runCatching { Regex(p) }.isFailure) {
                    continue
                }
            }

            val block = Block(
                category = category,
                type = ruleType,
                keywords = dto.keywords?.let { gson.toJson(it) },
                username = dto.username?.trim(),
                uid = dto.uid?.trim(),
                pattern = dto.pattern?.trim(),
                minLevel = dto.minLevel ?: 0,
                ipRegion = dto.ipRegion?.trim(),
                forum = dto.forum?.trim(),
            )

            if (isDuplicateRule(block)) continue

            block.save()
            blockList.add(block)
            count++
        }
        compiledRegexCache.clear()
        parsedKeywordsCache.clear()
        return count
    }

    fun ThreadInfo.checkBlock(forumName: String? = null): FilterResult {
        val effectiveForum = forumName ?: this.forumName
        val authorId = this.authorId
        val authorName = this.author?.name
        val userResult = checkBlockUser(authorId, authorName, effectiveForum)
        if (userResult is FilterResult.Blocked) return userResult

        val authorLevel = this.author?.level_id ?: 0
        val levelResult = checkBlockLevel(authorLevel, effectiveForum)
        if (levelResult is FilterResult.Blocked) return levelResult

        val authorIp = this.author?.ip_address
        val ipResult = checkBlockIp(authorIp, effectiveForum)
        if (ipResult is FilterResult.Blocked) return ipResult

        val titleResult = checkBlockContent(title, effectiveForum)
        if (titleResult is FilterResult.Blocked) return titleResult

        val abstractResult = checkBlockContent(abstractText, effectiveForum)
        if (abstractResult is FilterResult.Blocked) return abstractResult

        return FilterResult.Allowed
    }

    fun ThreadInfo.shouldBlock(forumName: String? = null): Boolean =
        checkBlock(forumName) is FilterResult.Blocked

    fun Post.checkBlock(forumName: String? = null): FilterResult {
        val authorId = this.author_id
        val authorName = this.author?.name
        val userResult = checkBlockUser(authorId, authorName, forumName)
        if (userResult is FilterResult.Blocked) return userResult

        val authorLevel = this.author?.level_id ?: 0
        val levelResult = checkBlockLevel(authorLevel, forumName)
        if (levelResult is FilterResult.Blocked) return levelResult

        val authorIp = this.author?.ip_address
        val ipResult = checkBlockIp(authorIp, forumName)
        if (ipResult is FilterResult.Blocked) return ipResult

        val contentResult = checkBlockContent(content.plainText, forumName)
        if (contentResult is FilterResult.Blocked) return contentResult

        return FilterResult.Allowed
    }

    fun Post.shouldBlock(forumName: String? = null): Boolean =
        checkBlock(forumName) is FilterResult.Blocked

    fun SubPostList.checkBlock(forumName: String? = null): FilterResult {
        val authorId = this.author_id
        val authorName = this.author?.name
        val userResult = checkBlockUser(authorId, authorName, forumName)
        if (userResult is FilterResult.Blocked) return userResult

        val authorLevel = this.author?.level_id ?: 0
        val levelResult = checkBlockLevel(authorLevel, forumName)
        if (levelResult is FilterResult.Blocked) return levelResult

        val authorIp = this.author?.ip_address
        val ipResult = checkBlockIp(authorIp, forumName)
        if (ipResult is FilterResult.Blocked) return ipResult

        val contentResult = checkBlockContent(content.plainText, forumName)
        if (contentResult is FilterResult.Blocked) return contentResult

        return FilterResult.Allowed
    }

    fun SubPostList.shouldBlock(forumName: String? = null): Boolean =
        checkBlock(forumName) is FilterResult.Blocked

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean {
        val replyerId = this.replyer?.id?.toLongOrNull() ?: -1L
        val replyerName = this.replyer?.name
        if (isWhitelistedUser(replyerId, replyerName)) return false
        if (shouldBlockUser(replyerId, replyerName)) return true
        if (shouldBlockContent(content.orEmpty())) return true
        return false
    }
}
