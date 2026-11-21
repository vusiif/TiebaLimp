package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.MessageListBean
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.abstractText
import com.huanchengfly.tieba.post.api.models.protos.plainText
import com.huanchengfly.tieba.post.utils.BlockManager.FilterResult

object BlockExtensions {
    fun checkBlockThread(thread: ThreadInfo, forumName: String? = null): FilterResult {
        val effectiveForum = forumName ?: thread.forumName
        val authorId = thread.authorId
        val authorName = thread.author?.name
        val userResult = BlockManager.checkBlockUser(authorId, authorName, effectiveForum)
        if (userResult is FilterResult.Blocked) return userResult

        val authorLevel = thread.author?.level_id ?: 0
        val levelResult = BlockManager.checkBlockLevel(authorLevel, effectiveForum)
        if (levelResult is FilterResult.Blocked) return levelResult

        val authorIp = thread.author?.ip_address
        val ipResult = BlockManager.checkBlockIp(authorIp, effectiveForum)
        if (ipResult is FilterResult.Blocked) return ipResult

        val titleResult = BlockManager.checkBlockContent(thread.title, effectiveForum)
        if (titleResult is FilterResult.Blocked) return titleResult

        val abstractResult = BlockManager.checkBlockContent(thread.abstractText, effectiveForum)
        if (abstractResult is FilterResult.Blocked) return abstractResult

        return FilterResult.Allowed
    }

    fun checkBlockPost(post: Post, forumName: String? = null): FilterResult {
        val authorId = post.author_id
        val authorName = post.author?.name
        val userResult = BlockManager.checkBlockUser(authorId, authorName, forumName)
        if (userResult is FilterResult.Blocked) return userResult

        val authorLevel = post.author?.level_id ?: 0
        val levelResult = BlockManager.checkBlockLevel(authorLevel, forumName)
        if (levelResult is FilterResult.Blocked) return levelResult

        val authorIp = post.author?.ip_address
        val ipResult = BlockManager.checkBlockIp(authorIp, forumName)
        if (ipResult is FilterResult.Blocked) return ipResult

        val contentResult = BlockManager.checkBlockContent(post.content.plainText, forumName)
        if (contentResult is FilterResult.Blocked) return contentResult

        return FilterResult.Allowed
    }

    fun checkBlockSubPost(subPost: SubPostList, forumName: String? = null): FilterResult {
        val authorId = subPost.author_id
        val authorName = subPost.author?.name
        val userResult = BlockManager.checkBlockUser(authorId, authorName, forumName)
        if (userResult is FilterResult.Blocked) return userResult

        val authorLevel = subPost.author?.level_id ?: 0
        val levelResult = BlockManager.checkBlockLevel(authorLevel, forumName)
        if (levelResult is FilterResult.Blocked) return levelResult

        val authorIp = subPost.author?.ip_address
        val ipResult = BlockManager.checkBlockIp(authorIp, forumName)
        if (ipResult is FilterResult.Blocked) return ipResult

        val contentResult = BlockManager.checkBlockContent(subPost.content.plainText, forumName)
        if (contentResult is FilterResult.Blocked) return contentResult

        return FilterResult.Allowed
    }

    fun shouldBlockMessage(message: MessageListBean.MessageInfoBean): Boolean {
        val replyerId = message.replyer?.id?.toLongOrNull() ?: -1L
        val replyerName = message.replyer?.name
        if (BlockManager.isWhitelistedUser(replyerId, replyerName)) return false
        if (BlockManager.shouldBlockUser(replyerId, replyerName)) return true
        if (BlockManager.shouldBlockContent(message.content.orEmpty())) return true
        return false
    }
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
