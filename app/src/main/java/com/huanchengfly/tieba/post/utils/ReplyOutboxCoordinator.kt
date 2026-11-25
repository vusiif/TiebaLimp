package com.huanchengfly.tieba.post.utils

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.repository.AddPostRepository
import com.huanchengfly.tieba.post.toMD5
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.litepal.LitePal
import java.io.IOException

data class PendingReplyItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("account_uid")
    val accountUid: String?,
    @SerializedName("content")
    val content: String,
    @SerializedName("forum_id")
    val forumId: Long,
    @SerializedName("forum_name")
    val forumName: String,
    @SerializedName("thread_id")
    val threadId: Long,
    @SerializedName("tbs")
    val tbs: String?,
    @SerializedName("post_id")
    val postId: Long? = null,
    @SerializedName("sub_post_id")
    val subPostId: Long? = null,
    @SerializedName("reply_user_id")
    val replyUserId: Long? = null,
    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("retry_count")
    val retryCount: Int = 0,
    @SerializedName("last_error")
    val lastError: String? = null,
) {
    fun targetKey(): String {
        val uid = accountUid?.takeIf { it.isNotBlank() } ?: "0"
        return "${uid}_${threadId}_${postId ?: 0}_${subPostId ?: 0}".toMD5()
    }
}

interface OutboxStore {
    fun getAll(): List<PendingReplyItem>
    fun put(item: PendingReplyItem)
    fun remove(id: String)
}

class LitePalDraftOutboxStore : OutboxStore {
    override fun getAll(): List<PendingReplyItem> {
        val rows = LitePal.where("hash like ?", "${DraftHelper.OUTBOX_HASH_PREFIX}%")
            .find(Draft::class.java) ?: return emptyList()
        return rows.mapNotNull { row ->
            runCatching {
                GsonUtil.getGson().fromJson(row.content, PendingReplyItem::class.java)
            }.getOrNull()
        }
    }

    override fun put(item: PendingReplyItem) {
        val hash = "${DraftHelper.OUTBOX_HASH_PREFIX}${item.id}"
        Draft(hash, GsonUtil.getGson().toJson(item)).saveOrUpdate("hash = ?", hash)
    }

    override fun remove(id: String) {
        val hash = "${DraftHelper.OUTBOX_HASH_PREFIX}${id}"
        LitePal.deleteAll(Draft::class.java, "hash = ?", hash)
    }
}

/**
 * Transaction-safe outbox queue for replies that fail due to transient network errors.
 * Automatically retries queued replies when connectivity recovers and clears matching drafts.
 */
class ReplyOutboxManager(
    private val store: OutboxStore = LitePalDraftOutboxStore(),
    private val sender: suspend (PendingReplyItem) -> Unit = { item ->
        AddPostRepository.addPost(
            content = item.content,
            forumId = item.forumId,
            forumName = item.forumName,
            threadId = item.threadId,
            tbs = item.tbs,
            postId = item.postId,
            subPostId = item.subPostId,
            replyUserId = item.replyUserId
        ).first()
    },
    private val onDelivered: suspend (PendingReplyItem) -> Unit = { item ->
        DraftHelper.clearDraft(item.accountUid, item.threadId, item.postId, item.subPostId)
    },
    private val timeProvider: () -> Long = { System.currentTimeMillis() },
) {
    private val mutex = Mutex()

    suspend fun enqueue(item: PendingReplyItem): Boolean = mutex.withLock {
        if (item.content.isBlank()) return@withLock false
        val existing = store.getAll()
        val duplicate = existing.firstOrNull {
            it.targetKey() == item.targetKey() && it.content == item.content
        }
        if (duplicate != null) {
            return@withLock false
        }
        store.put(item)
        true
    }

    suspend fun removeByTarget(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null,
    ) = mutex.withLock {
        val uid = accountUid?.takeIf { it.isNotBlank() } ?: "0"
        val key = "${uid}_${threadId}_${postId ?: 0}_${subPostId ?: 0}".toMD5()
        store.getAll().filter { it.targetKey() == key }.forEach { store.remove(it.id) }
    }

    suspend fun pendingCount(): Int = mutex.withLock {
        store.getAll().size
    }

    suspend fun flushPendingReplies(isNetworkAvailable: Boolean = true): Int = mutex.withLock {
        if (!isNetworkAvailable) return@withLock 0
        val now = timeProvider()
        val items = store.getAll().sortedBy { it.createdAt }
        var deliveredCount = 0

        for (item in items) {
            if (now - item.createdAt > OUTBOX_TTL_MS || item.retryCount >= MAX_RETRIES) {
                store.remove(item.id)
                continue
            }
            val result = runCatching { sender(item) }
            if (result.isSuccess) {
                store.remove(item.id)
                runCatching { onDelivered(item) }
                deliveredCount++
            } else {
                val err = result.exceptionOrNull()
                if (err != null && isTransientNetworkError(err)) {
                    val nextRetry = item.retryCount + 1
                    if (nextRetry >= MAX_RETRIES) {
                        store.remove(item.id)
                    } else {
                        store.put(
                            item.copy(
                                retryCount = nextRetry,
                                lastError = err.message
                            )
                        )
                    }
                } else {
                    // Non-transient business/auth error: drop from retry queue so user draft stays untouched
                    store.remove(item.id)
                }
            }
        }
        deliveredCount
    }

    companion object {
        const val MAX_RETRIES = 3
        const val OUTBOX_TTL_MS = 24L * 60L * 60L * 1000L

        fun isTransientNetworkError(throwable: Throwable): Boolean {
            var cur: Throwable? = throwable
            var depth = 0
            while (cur != null && depth < 6) {
                if (cur is IOException) return true
                cur = cur.cause
                depth++
            }
            return false
        }
    }
}

object ReplyOutboxCoordinator {
    private const val TAG = "ReplyOutboxCoordinator"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val manager = ReplyOutboxManager()

    suspend fun enqueue(item: PendingReplyItem): Boolean =
        runCatching { manager.enqueue(item) }.getOrDefault(false)

    suspend fun removeByTarget(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null,
    ) {
        runCatching {
            manager.removeByTarget(accountUid, threadId, postId, subPostId)
        }
    }

    fun flushPendingRepliesAsync() {
        if (!AppNetworkManager.isConnected) return
        scope.launch {
            runCatching {
                val sent = manager.flushPendingReplies(isNetworkAvailable = AppNetworkManager.isConnected)
                if (sent > 0) {
                    Log.i(TAG, "Delivered $sent pending offline reply(s)")
                }
            }
        }
    }
}
