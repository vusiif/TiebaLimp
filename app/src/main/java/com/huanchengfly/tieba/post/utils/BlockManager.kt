package com.huanchengfly.tieba.post.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.models.database.Block
import org.litepal.LitePal
import org.litepal.extension.delete
import org.litepal.extension.findAllAsync
import java.util.concurrent.CopyOnWriteArrayList

object BlockManager {
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

    var rulesVersion by mutableIntStateOf(0)
        private set

    private val blockList: MutableList<Block> = CopyOnWriteArrayList()

    val blackList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_BLACK_LIST }

    val whiteList: List<Block>
        get() = blockList.filter { it.category == Block.CATEGORY_WHITE_LIST }

    fun isDangerousRegexPattern(pattern: String?): Boolean =
        BlockFilterUtil.isDangerousRegexPattern(pattern)

    fun normalizeIpAddress(ip: String?): String =
        BlockFilterUtil.normalizeIpAddress(ip)

    fun matchesForumScope(ruleForum: String?, currentForum: String?): Boolean =
        BlockFilterUtil.matchesForumScope(ruleForum, currentForum)

    fun addBlock(block: Block) {
        block.save()
        blockList.add(block)
        if (block.type == Block.TYPE_REGEX && !block.pattern.isNullOrBlank()) {
            BlockRuleMatcher.evictPattern(block.pattern)
        }
        rulesVersion++
    }

    fun addBlockAsync(
        block: Block,
        callback: ((Boolean) -> Unit)? = null,
    ) {
        block.saveAsync()
            .listen { success ->
                callback?.invoke(success)
                if (success) {
                    blockList.add(block)
                    if (block.type == Block.TYPE_REGEX && !block.pattern.isNullOrBlank()) {
                        BlockRuleMatcher.evictPattern(block.pattern)
                    }
                    rulesVersion++
                }
            }
    }

    fun removeBlock(id: Long) {
        LitePal.delete<Block>(id)
        val removed = blockList.firstOrNull { it.id == id }
        blockList.removeAll { it.id == id }
        if (removed?.type == Block.TYPE_REGEX && !removed.pattern.isNullOrBlank()) {
            BlockRuleMatcher.evictPattern(removed.pattern)
        }
        rulesVersion++
    }

    fun clearAll(category: Int? = null) {
        if (category == null) {
            LitePal.deleteAll(Block::class.java)
            blockList.clear()
        } else {
            LitePal.deleteAll(Block::class.java, "category = ?", category.toString())
            blockList.removeAll { it.category == category }
        }
        BlockRuleMatcher.clearCache()
        rulesVersion++
    }

    fun init() {
        LitePal.findAllAsync<Block>().listen { blocks ->
            blockList.clear()
            blockList.addAll(blocks)
            BlockRuleMatcher.clearCache()
            rulesVersion++
        }
    }

    fun isWhitelistedUser(userId: Long = 0L, userName: String? = null, forumName: String? = null): Boolean =
        BlockRuleMatcher.isWhitelistedUser(blockList, userId, userName, forumName)

    fun checkBlockContent(content: CharSequence?, forumName: String? = null): FilterResult =
        BlockRuleMatcher.checkBlockContent(blockList, content, forumName)

    fun checkBlockUser(userId: Long = 0L, userName: String? = null, forumName: String? = null): FilterResult =
        BlockRuleMatcher.checkBlockUser(blockList, userId, userName, forumName)

    fun checkBlockIp(rawIp: String?, forumName: String? = null): FilterResult =
        BlockRuleMatcher.checkBlockIp(blockList, rawIp, forumName)

    fun checkBlockLevel(level: Int, forumName: String? = null): FilterResult =
        BlockRuleMatcher.checkBlockLevel(blockList, level, forumName)

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
    ): SimulationResult = BlockRuleMatcher.simulateCheck(
        blockList = blockList,
        content = content,
        userId = userId,
        userName = userName,
        userLevel = userLevel,
        userIp = userIp,
        forumName = forumName,
    )

    fun exportRulesJson(): String =
        BlockRuleJsonHelper.exportRulesJson(blockList)

    fun isDuplicateRule(newBlock: Block): Boolean =
        BlockRuleJsonHelper.isDuplicateRule(blockList, newBlock)

    fun importRulesFromJson(json: String): Int {
        val blocksToSave = BlockRuleJsonHelper.parseRulesFromJson(json, blockList)
        if (blocksToSave.isNotEmpty()) {
            LitePal.saveAll(blocksToSave)
            blockList.addAll(blocksToSave)
            BlockRuleMatcher.clearCache()
            rulesVersion++
        }
        return blocksToSave.size
    }

    fun ThreadInfo.checkBlock(forumName: String? = null): FilterResult =
        BlockExtensions.checkBlockThread(this, forumName)

    fun ThreadInfo.shouldBlock(forumName: String? = null): Boolean =
        checkBlock(forumName) is FilterResult.Blocked

    fun Post.checkBlock(forumName: String? = null): FilterResult =
        BlockExtensions.checkBlockPost(this, forumName)

    fun Post.shouldBlock(forumName: String? = null): Boolean =
        checkBlock(forumName) is FilterResult.Blocked

    fun SubPostList.checkBlock(forumName: String? = null): FilterResult =
        BlockExtensions.checkBlockSubPost(this, forumName)

    fun SubPostList.shouldBlock(forumName: String? = null): Boolean =
        checkBlock(forumName) is FilterResult.Blocked

    fun MessageListBean.MessageInfoBean.shouldBlock(): Boolean =
        BlockExtensions.shouldBlockMessage(this)
}
