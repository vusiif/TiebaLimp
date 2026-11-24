package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.models.DraftModel
import com.huanchengfly.tieba.post.utils.DraftHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testDraftModel_serializationAndDeserialization() {
        val original = DraftModel(
            text = "图文混排草稿测试\n第二行内容",
            imageUris = listOf("content://media/external/images/media/1001", "file:///sdcard/test.jpg"),
            isOriginImage = true,
            updateTimestamp = 1700000000123L
        )

        val serialized = original.serialize()
        assertTrue(serialized.contains("图文混排草稿测试"))
        assertTrue(serialized.contains("1001"))

        val deserialized = DraftModel.deserialize(serialized)
        assertEquals(original.text, deserialized.text)
        assertEquals(original.imageUris, deserialized.imageUris)
        assertEquals(original.isOriginImage, deserialized.isOriginImage)
        assertEquals(original.updateTimestamp, deserialized.updateTimestamp)
        assertFalse(deserialized.isBlank())
    }

    @Test
    fun testDraftModel_legacyPlainTextCompatibility() {
        val legacyPlainText = "这是一条来自老版本存储在数据库中的纯文本草稿"
        val model = DraftModel.deserialize(legacyPlainText)

        assertEquals(legacyPlainText, model.text)
        assertTrue(model.imageUris.isEmpty())
        assertFalse(model.isOriginImage)
        assertFalse(model.isBlank())
    }

    @Test
    fun testDraftModel_corruptedJsonFallback() {
        val corruptedJson = "{\"text\": \"unclosed json string..."
        val model = DraftModel.deserialize(corruptedJson)

        assertEquals(corruptedJson, model.text)
        assertTrue(model.imageUris.isEmpty())
    }

    @Test
    fun testDraftModel_isBlank() {
        assertTrue(DraftModel().isBlank())
        assertTrue(DraftModel("", emptyList(), false).isBlank())
        assertTrue(DraftModel("   ", emptyList(), true).isBlank())

        assertFalse(DraftModel("有内容", emptyList()).isBlank())
        assertFalse(DraftModel("", listOf("content://image/1")).isBlank())
    }
}
