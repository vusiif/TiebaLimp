package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.models.DraftModel
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
     * Asynchronously loads a structured draft model (text + images + settings).
     * Queries user-scoped draft first, falling back to legacy unscoped draft if not found.
     */
    suspend fun loadDraftModel(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null
    ): DraftModel? = withContext(Dispatchers.IO) {
        val userHash = getDraftHash(accountUid, threadId, postId, subPostId)
        val userDraft = LitePal.where("hash = ?", userHash).findFirst(Draft::class.java)
        if (userDraft != null && userDraft.content.isNotBlank()) {
            val model = DraftModel.deserialize(userDraft.content)
            if (!model.isBlank()) {
                return@withContext model
            }
        }

        // Fallback to legacy draft
        val legacyHash = getLegacyDraftHash(threadId, postId, subPostId)
        val legacyDraft = LitePal.where("hash = ?", legacyHash).findFirst(Draft::class.java)
        if (legacyDraft != null && legacyDraft.content.isNotBlank()) {
            val model = DraftModel.deserialize(legacyDraft.content)
            if (!model.isBlank()) {
                // Upgrade legacy draft to user-scoped draft and clean up legacy
                saveDraft(accountUid, threadId, postId, subPostId, model)
                LitePal.deleteAll(Draft::class.java, "hash = ?", legacyHash)
                return@withContext model
            }
        }
        null
    }

    /**
     * Asynchronously loads draft text: queries user-scoped draft first,
     * falling back to legacy unscoped draft if not found.
     */
    suspend fun loadDraft(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null
    ): String? = loadDraftModel(accountUid, threadId, postId, subPostId)?.text?.takeIf { it.isNotBlank() }

    /**
     * Save structured rich draft if content is non-blank, or delete draft entry if content is blank.
     * Prevents database bloat from empty draft rows.
     */
    suspend fun saveDraft(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null,
        draft: DraftModel
    ) = withContext(Dispatchers.IO) {
        val userHash = getDraftHash(accountUid, threadId, postId, subPostId)
        if (!draft.isBlank()) {
            Draft(userHash, draft.serialize()).saveOrUpdate("hash = ?", userHash)
        } else {
            LitePal.deleteAll(Draft::class.java, "hash = ?", userHash)
        }
    }

    /**
     * Save plain text draft (backward compatibility wrapper).
     */
    suspend fun saveDraft(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null,
        content: String
    ) = saveDraft(accountUid, threadId, postId, subPostId, DraftModel(text = content))

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

    /**
     * Check if a draft exists for the given target.
     */
    suspend fun hasDraft(
        accountUid: String?,
        threadId: Long,
        postId: Long? = null,
        subPostId: Long? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val userHash = getDraftHash(accountUid, threadId, postId, subPostId)
        if (LitePal.where("hash = ?", userHash).count(Draft::class.java) > 0) return@withContext true
        val legacyHash = getLegacyDraftHash(threadId, postId, subPostId)
        LitePal.where("hash = ?", legacyHash).count(Draft::class.java) > 0
    }

    /**
     * Purges expired drafts older than [maxAgeDays] to keep SQLite storage clean and compact.
     */
    suspend fun cleanExpiredDrafts(maxAgeDays: Int = 30): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - maxAgeDays * 24L * 60 * 60 * 1000
        val allDrafts = LitePal.findAll(Draft::class.java) ?: emptyList()
        var deletedCount = 0
        allDrafts.forEach { draft ->
            val model = DraftModel.deserialize(draft.content)
            if (model.isBlank() || (model.updateTimestamp in 1 until cutoff)) {
                LitePal.deleteAll(Draft::class.java, "hash = ?", draft.hash)
                deletedCount++
            }
        }
        deletedCount
    }
}
