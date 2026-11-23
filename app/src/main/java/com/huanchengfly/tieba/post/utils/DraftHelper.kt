package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.toMD5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litepal.LitePal

object DraftHelper {

    /**
     * Compute a user-scoped draft hash to isolate drafts across different accounts.
     */
    fun getDraftHash(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null
    ): String {
        val uid = accountUid?.takeIf { it.isNotBlank() } ?: "0"
        return "${uid}_${threadId}_${postId ?: 0}_${subPostId ?: 0}".toMD5()
    }

    /**
     * Legacy draft hash computation for backwards compatibility.
     */
    fun getLegacyDraftHash(
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null
    ): String {
        return "${threadId}_${postId}_${subPostId}".toMD5()
    }

    /**
     * Asynchronously loads a draft: queries user-scoped draft first,
     * falling back to legacy unscoped draft if not found.
     */
    suspend fun loadDraft(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null
    ): String? = withContext(Dispatchers.IO) {
        val userHash = getDraftHash(accountUid, threadId, postId, subPostId)
        val userDraft = LitePal.where("hash = ?", userHash).findFirst(Draft::class.java)
        if (userDraft != null && userDraft.content.isNotBlank()) {
            return@withContext userDraft.content
        }

        // Fallback to legacy draft
        val legacyHash = getLegacyDraftHash(threadId, postId, subPostId)
        val legacyDraft = LitePal.where("hash = ?", legacyHash).findFirst(Draft::class.java)
        if (legacyDraft != null && legacyDraft.content.isNotBlank()) {
            // Upgrade legacy draft to user-scoped draft and clean up legacy
            saveDraft(accountUid, threadId, postId, subPostId, legacyDraft.content)
            LitePal.deleteAll(Draft::class.java, "hash = ?", legacyHash)
            return@withContext legacyDraft.content
        }
        null
    }

    /**
     * Save draft if content is non-blank, or delete draft entry if content is blank.
     * Prevents database bloat from empty draft rows.
     */
    suspend fun saveDraft(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null,
        content: String
    ) = withContext(Dispatchers.IO) {
        val userHash = getDraftHash(accountUid, threadId, postId, subPostId)
        if (content.isNotBlank()) {
            Draft(userHash, content).saveOrUpdate("hash = ?", userHash)
        } else {
            LitePal.deleteAll(Draft::class.java, "hash = ?", userHash)
        }
    }

    /**
     * Delete both user-scoped and legacy drafts upon successful reply.
     */
    suspend fun clearDraft(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null
    ) = withContext(Dispatchers.IO) {
        val userHash = getDraftHash(accountUid, threadId, postId, subPostId)
        val legacyHash = getLegacyDraftHash(threadId, postId, subPostId)
        LitePal.deleteAll(Draft::class.java, "hash = ? or hash = ?", userHash, legacyHash)
    }
}
