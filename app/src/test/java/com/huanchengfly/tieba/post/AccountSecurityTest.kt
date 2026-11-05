package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.utils.AccountSecurityUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSecurityTest {

    @Test
    fun encryptAndDecrypt_roundTripPreservesContent() {
        val original = "BDUSS_SECRET_TOKEN_EXAMPLE_1234567890abcdef"
        val encrypted = AccountSecurityUtil.encrypt(original)

        assertNotEquals(original, encrypted)
        assertTrue(encrypted.startsWith(AccountSecurityUtil.ENCRYPTED_PREFIX))

        val decrypted = AccountSecurityUtil.decrypt(encrypted)
        assertEquals(original, decrypted)
    }

    @Test
    fun encrypt_emptyOrBlank_handlesGracefully() {
        assertEquals("", AccountSecurityUtil.encrypt(""))
        assertEquals("", AccountSecurityUtil.encrypt(null))
        assertEquals("", AccountSecurityUtil.decrypt(""))
        assertEquals("", AccountSecurityUtil.decrypt(null))
    }

    @Test
    fun encrypt_idempotent_avoidsDoubleEncryption() {
        val original = "MY_STOKEN_VALUE"
        val once = AccountSecurityUtil.encrypt(original)
        val twice = AccountSecurityUtil.encrypt(once)

        assertEquals(once, twice)
        assertEquals(original, AccountSecurityUtil.decrypt(twice))
    }

    @Test
    fun decrypt_legacyPlaintext_returnsAsIsForBackwardCompatibility() {
        val legacyPlaintext = "raw_legacy_bduss_from_older_version"
        val result = AccountSecurityUtil.decrypt(legacyPlaintext)

        assertEquals(legacyPlaintext, result)
    }

    @Test
    fun isPlaintext_identifiesPlaintextVersusEncrypted() {
        assertTrue(AccountSecurityUtil.isPlaintext("regular_plaintext_token"))
        assertFalse(AccountSecurityUtil.isPlaintext(AccountSecurityUtil.encrypt("some_token")))
        assertFalse(AccountSecurityUtil.isPlaintext(""))
        assertFalse(AccountSecurityUtil.isPlaintext(null))
    }
}
