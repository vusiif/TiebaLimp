package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.addPost.AddPostResponse
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

object AddPostRepository {
    private val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    fun addPost(
        content: String,
        forumId: Long,
        forumName: String,
        threadId: Long,
        tbs: String? = null,
        nameShow: String? = null,
        postId: Long? = null,
        subPostId: Long? = null,
        replyUserId: Long? = null
    ): Flow<AddPostResponse> =
        TiebaApi.getInstance()
            .addPostFlow(
                content,
                forumId.toString(),
                forumName,
                threadId.toString(),
                tbs,
                nameShow,
                postId?.toString(),
                subPostId?.toString(),
                replyUserId?.toString()
            )
            .onEach {
                val newPostId = it.data_?.pid?.toLongOrNull() ?: return@onEach
                eventScope.launch {
                    if (postId != null) {
                        emitGlobalEvent(
                            GlobalEvent.ReplySuccess(
                                threadId,
                                postId,
                                postId,
                                subPostId,
                                newPostId
                            )
                        )
                    } else {
                        emitGlobalEvent(GlobalEvent.ReplySuccess(threadId, newPostId))
                    }
                }
            }
}