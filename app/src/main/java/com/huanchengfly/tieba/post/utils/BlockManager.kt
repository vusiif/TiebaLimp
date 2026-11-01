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
    )

    private fun getOrCompileRegex(pattern: String): Regex? {
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

    fun isWhitelistedUser(userId: Long = 0L, userName: String? = null): Boolean {
        if (userId <= 0L && userName.isNullOrBlank()) return false
        for (block in blockList) {
            if (block.category == Block.CATEGORY_WHITE_LIST && block.type == Block.TYPE_USER) {
                if ((userId > 0L && block.uid == userId.toString()) ||
                    (!userName.isNullOrBlank() && block.username == userName)
                ) {
                    return true
                }
            }
        }
        return false
    }

    fun shouldBlockContent(content: CharSequence?): Boolean {
        if (content.isNullOrEmpty()) return false
        val contentStr = content.toString()

        // 1. Whitelist pass (no temporary allocations)
        for (block in blockList) {
            if (block.category != Block.CATEGORY_WHITE_LIST) continue
            when (block.type) {
                Block.TYPE_KEYWORD -> {
                    val kws = getOrParseKeywords(block.keywords)
                    if (kws.isNotEmpty() && kws.all { contentStr.contains(it, ignoreCase = true) }) {
                        return false
                    }
                }
                Block.TYPE_REGEX -> {
                    val pattern = block.pattern
                    if (!pattern.isNullOrBlank()) {
                        val regex = getOrCompileRegex(pattern)
                        val boundedContent = if (content.length > 4000) content.subSequence(0, 4000) else content
                        if (regex != null && regex.containsMatchIn(boundedContent)) {
                            return false
                        }
                    }
                }
            }
        }

        // 2. Blacklist pass (no temporary allocations)
        for (block in blockList) {
            if (block.category != Block.CATEGORY_BLACK_LIST) continue
            when (block.type) {
                Block.TYPE_KEYWORD -> {
                    val kws = getOrParseKeywords(block.keywords)
                    if (kws.isNotEmpty() && kws.all { contentStr.contains(it, ignoreCase = true) }) {
                        return true
                    }
                }
                Block.TYPE_REGEX -> {
                    val pattern = block.pattern
                    if (!pattern.isNullOrBlank()) {
                        val regex = getOrCompileRegex(pattern)
                        val boundedContent = if (content.length > 4000) content.subSequence(0, 4000) else content
                        if (regex != null && regex.containsMatchIn(boundedContent)) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    fun shouldBlockUser(userId: Long = 0L, userName: String? = null): Boolean {
        if (isWhitelistedUser(userId, userName)) return false
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_USER) {
                if ((userId > 0L && block.uid == userId.toString()) ||
                    (!userName.isNullOrBlank() && block.username == userName)
                ) {
                    return true
                }
            }
        }
        return false
    }

    fun shouldBlockIp(rawIp: String?): Boolean {
        val cleanIp = normalizeIpAddress(rawIp)
        if (cleanIp.isEmpty()) return false

        // Check whitelist
        for (block in blockList) {
            if (block.category == Block.CATEGORY_WHITE_LIST && block.type == Block.TYPE_IP_REGION) {
                val target = block.ipRegion?.trim().orEmpty()
                if (target.isNotEmpty() && (cleanIp.contains(target, ignoreCase = true) || target.contains(cleanIp, ignoreCase = true))) {
                    return false
                }
            }
        }

        // Check blacklist
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_IP_REGION) {
                val target = block.ipRegion?.trim().orEmpty()
                if (target.isNotEmpty() && (cleanIp.contains(target, ignoreCase = true) || target.contains(cleanIp, ignoreCase = true))) {
                    return true
                }
            }
        }

        return false
    }

    fun shouldBlockLevel(level: Int): Boolean {
        if (level <= 0) return false
        for (block in blockList) {
            if (block.category == Block.CATEGORY_BLACK_LIST && block.type == Block.TYPE_USER_LEVEL && block.minLevel > 0) {
                if (level < block.minLevel) {
                    return true
                }
            }
        }
        return false
    }

    fun shouldBlock(content: String): Boolean = shouldBlockContent(content)

    fun shouldBlock(userId: Long = 0L, userName: String? = null): Boolean = shouldBlockUser(userId, userName)

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
            )
        }
        return GsonUtil.getGson().toJson(dtos)
    }

    fun isDuplicateRule(newBlock: Block): Boolean {
        return blockList.any { existing ->
            existing.category == newBlock.category &&
            existing.type == newBlock.type &&
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
        if (json.isBlank()) return 0
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
                if (p.isNullOrBlank() || p.length > 200 || runCatching { Regex(p) }.isFailure) {
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

    fun ThreadInfo.shouldBlock(): Boolean {
        val authorId = this.authorId
        val authorName = this.author?.name
        if (isWhitelistedUser(authorId, authorName)) return false
        if (shouldBlockUser(authorId, authorName)) return true

        val authorLevel = this.author?.level_id ?: 0
        if (shouldBlockLevel(authorLevel)) return true

        val authorIp = this.author?.ip_address
        if (shouldBlockIp(authorIp)) return true

        if (shouldBlockContent(title)) return true
        if (shouldBlockContent(abstractText)) return true

        return false
    }

    fun Post.shouldBlock(): Boolean {
        val authorId = this.author_id
        val authorName = this.author?.name
        if (isWhitelistedUser(authorId, authorName)) return false
        if (shouldBlockUser(authorId, authorName)) return true

        val authorLevel = this.author?.level_id ?: 0
        if (shouldBlockLevel(authorLevel)) return true

        val authorIp = this.author?.ip_address
        if (shouldBlockIp(authorIp)) return true

        if (shouldBlockContent(content.plainText)) return true

        return false
    }

    fun SubPostList.shouldBlock(): Boolean {
        val authorId = this.author_id
        val authorName = this.author?.name
        if (isWhitelistedUser(authorId, authorName)) return false
        if (shouldBlockUser(authorId, authorName)) return true

        val authorLevel = this.author?.level_id ?: 0
        if (shouldBlockLevel(authorLevel)) return true

        val authorIp = this.author?.ip_address
        if (shouldBlockIp(authorIp)) return true

        if (shouldBlockContent(content.plainText)) return true

        return false
    }

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean {
        val replyerId = this.replyer?.id?.toLongOrNull() ?: -1L
        val replyerName = this.replyer?.name
        if (isWhitelistedUser(replyerId, replyerName)) return false
        if (shouldBlockUser(replyerId, replyerName)) return true
        if (shouldBlockContent(content.orEmpty())) return true
        return false
    }
}
