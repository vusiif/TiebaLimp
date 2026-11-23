package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.DraftHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftHelperTest {

    @Test
    fun testUserScopedDraftHash_isolatesAccounts() {
        val threadId = 12345678L
        val postId = 888L
        val subPostId = 999L

        val hashUserA = DraftHelper.getDraftHash("10001", threadId, postId, subPostId)
        val hashUserB = DraftHelper.getDraftHash("10002", threadId, postId, subPostId)

        // Different user accounts in the same thread must have isolated hashes
        assertNotEquals(hashUserA, hashUserB)
        assertTrue(hashUserA.isNotEmpty())
        assertTrue(hashUserB.isNotEmpty())
    }

    @Test
    fun testUserScopedDraftHash_consistent() {
        val threadId = 12345678L
        val postId = 888L

        val hash1 = DraftHelper.getDraftHash("10001", threadId, postId, null)
        val hash2 = DraftHelper.getDraftHash("10001", threadId, postId, null)

        assertEquals(hash1, hash2)
    }

    @Test
    fun testNullOrEmptyUid_fallback() {
        val threadId = 12345678L
        val nullUidHash = DraftHelper.getDraftHash(null, threadId)
        val emptyUidHash = DraftHelper.getDraftHash("", threadId)
        val zeroUidHash = DraftHelper.getDraftHash("0", threadId)

        assertEquals(nullUidHash, emptyUidHash)
        assertEquals(emptyUidHash, zeroUidHash)
    }

    @Test
    fun testLegacyHashCompatibility() {
        val threadId = 12345678L
        val postId = 888L
        val subPostId = 999L

        val expectedLegacy = "${threadId}_${postId}_${subPostId}".toMD5()
        val legacyHash = DraftHelper.getLegacyDraftHash(threadId, postId, subPostId)

        assertEquals(expectedLegacy, legacyHash)
    }
}
