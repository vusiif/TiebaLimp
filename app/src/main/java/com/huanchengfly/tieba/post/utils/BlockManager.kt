package com.huanchengfly.tieba.post.utils

import com.google.gson.reflect.TypeToken
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.plainText
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
    }

    fun init() {
        LitePal.findAllAsync<Block>().listen { blocks ->
            blockList.clear()
            blockList.addAll(blocks)
            compiledRegexCache.clear()
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
        return whiteList.any { block ->
            block.type == Block.TYPE_USER && (
                (userId > 0L && block.uid == userId.toString()) ||
                (!userName.isNullOrBlank() && block.username == userName)
            )
        }
    }

    fun shouldBlockContent(content: CharSequence?): Boolean {
        if (content.isNullOrEmpty()) return false
        val contentStr = content.toString()

        val blackKeywords = blackList.filter { it.type == Block.TYPE_KEYWORD }
        val whiteKeywords = whiteList.filter { it.type == Block.TYPE_KEYWORD }
        val blackRegexes = blackList.filter { it.type == Block.TYPE_REGEX && !it.pattern.isNullOrBlank() }
        val whiteRegexes = whiteList.filter { it.type == Block.TYPE_REGEX && !it.pattern.isNullOrBlank() }

        val matchesWhiteKeyword = whiteKeywords.any { block ->
            val kws = block.getKeywords()
            kws.isNotEmpty() && kws.all { contentStr.contains(it, ignoreCase = true) }
        }
        val matchesWhiteRegex = whiteRegexes.any { block ->
            val regex = getOrCompileRegex(block.pattern!!)
            regex != null && regex.containsMatchIn(content)
        }
        if (matchesWhiteKeyword || matchesWhiteRegex) {
            return false
        }

        val matchesBlackKeyword = blackKeywords.any { block ->
            val kws = block.getKeywords()
            kws.isNotEmpty() && kws.all { contentStr.contains(it, ignoreCase = true) }
        }
        if (matchesBlackKeyword) {
            return true
        }

        val matchesBlackRegex = blackRegexes.any { block ->
            val regex = getOrCompileRegex(block.pattern!!)
            regex != null && regex.containsMatchIn(content)
        }
        return matchesBlackRegex
    }

    fun shouldBlockUser(userId: Long = 0L, userName: String? = null): Boolean {
        if (isWhitelistedUser(userId, userName)) return false
        return blackList.any { block ->
            block.type == Block.TYPE_USER && (
                (userId > 0L && block.uid == userId.toString()) ||
                (!userName.isNullOrBlank() && block.username == userName)
            )
        }
    }

    fun shouldBlockIp(rawIp: String?): Boolean {
        val cleanIp = normalizeIpAddress(rawIp)
        if (cleanIp.isEmpty()) return false

        val blackIps = blackList.filter { it.type == Block.TYPE_IP_REGION && !it.ipRegion.isNullOrBlank() }
        val whiteIps = whiteList.filter { it.type == Block.TYPE_IP_REGION && !it.ipRegion.isNullOrBlank() }

        val matchesWhite = whiteIps.any { block ->
            val target = block.ipRegion!!.trim()
            cleanIp.contains(target, ignoreCase = true) || target.contains(cleanIp, ignoreCase = true)
        }
        if (matchesWhite) return false

        return blackIps.any { block ->
            val target = block.ipRegion!!.trim()
            cleanIp.contains(target, ignoreCase = true) || target.contains(cleanIp, ignoreCase = true)
        }
    }

    fun shouldBlockLevel(level: Int): Boolean {
        val blackLevelRules = blackList.filter { it.type == Block.TYPE_USER_LEVEL && it.minLevel > 0 }
        if (blackLevelRules.isEmpty()) return false

        return blackLevelRules.any { block ->
            level < block.minLevel
        }
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

    fun importRulesFromJson(json: String): Int {
        if (json.isBlank()) return 0
        val gson = GsonUtil.getGson()
        val type = object : TypeToken<List<BlockRuleDto>>() {}.type
        val importedList: List<BlockRuleDto> = runCatching {
            gson.fromJson<List<BlockRuleDto>>(json, type)
        }.getOrNull() ?: emptyList()

        var count = 0
        for (dto in importedList) {
            val block = Block(
                category = dto.category,
                type = dto.type,
                keywords = dto.keywords?.let { gson.toJson(it) },
                username = dto.username,
                uid = dto.uid,
                pattern = dto.pattern,
                minLevel = dto.minLevel ?: 0,
                ipRegion = dto.ipRegion,
            )
            block.save()
            blockList.add(block)
            count++
        }
        compiledRegexCache.clear()
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
