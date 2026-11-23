package com.huanchengfly.tieba.post

import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.utils.AccountCookieUtil
import com.huanchengfly.tieba.post.utils.AccountSecurityUtil
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.decryptSensitiveFields
import com.huanchengfly.tieba.post.utils.toEncryptedCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountUtilTest {

    @Test
    fun parseCookie_handlesStandardAndWhitespaceFormattedCookies() {
        val cookie = "BDUSS=test_bduss_val; STOKEN=test_stoken_val; BAIDUID=123456"
        val parsed = AccountCookieUtil.parseCookie(cookie)

        assertEquals("test_bduss_val", parsed["BDUSS"])
        assertEquals("test_stoken_val", parsed["STOKEN"])
        assertEquals("123456", parsed["BAIDUID"])
    }

    @Test
    fun parseCookie_handlesBase64AndMultipleEquals() {
        val cookie = "token=abc==; other=xyz=123; single="
        val parsed = AccountCookieUtil.parseCookie(cookie)

        assertEquals("abc==", parsed["token"])
        assertEquals("xyz=123", parsed["other"])
    }

    @Test
    fun extractTokens_caseInsensitiveLookup() {
        val cookie = "bduss=sample_bduss; stoken=sample_stoken; other=value"
        assertEquals("sample_bduss", AccountCookieUtil.extractBduss(cookie))
        assertEquals("sample_stoken", AccountCookieUtil.extractSToken(cookie))

        val emptyCookie = "other=value"
        assertNull(AccountCookieUtil.extractBduss(emptyCookie))
        assertNull(AccountCookieUtil.extractSToken(emptyCookie))
    }

    @Test
    fun getBdussCookie_formatsExpectedFormat() {
        val bduss = "my_sample_bduss_token"
        val cookie = AccountCookieUtil.getBdussCookie(bduss)

        assertTrue(cookie.startsWith("BDUSS=my_sample_bduss_token;"))
        assertTrue(cookie.contains("Domain=.baidu.com"))
        assertTrue(cookie.contains("Httponly"))
        assertTrue(cookie.contains("Secure"))
    }

    @Test
    fun accountExtensions_encryptAndDecryptRoundTrip() {
        val original = Account(
            uid = "10086",
            name = "TestUser",
            bduss = "raw_bduss_secret_123",
            sToken = "raw_stoken_secret_456",
            cookie = "BDUSS=raw_bduss_secret_123; STOKEN=raw_stoken_secret_456",
            nameShow = "Test User Display",
            portrait = "portrait_hash",
            tbs = "tbs_token"
        )

        val encrypted = original.toEncryptedCopy()
        assertTrue(encrypted.bduss.startsWith(AccountSecurityUtil.ENCRYPTED_PREFIX))
        assertTrue(encrypted.sToken.startsWith(AccountSecurityUtil.ENCRYPTED_PREFIX))
        assertTrue(encrypted.cookie.startsWith(AccountSecurityUtil.ENCRYPTED_PREFIX))
        assertEquals("10086", encrypted.uid)
        assertEquals("TestUser", encrypted.name)

        val decrypted = encrypted.decryptSensitiveFields()
        assertEquals("raw_bduss_secret_123", decrypted.bduss)
        assertEquals("raw_stoken_secret_456", decrypted.sToken)
        assertEquals("BDUSS=raw_bduss_secret_123; STOKEN=raw_stoken_secret_456", decrypted.cookie)
    }

    @Test
    fun isLoggedIn_validatesAccountFields() {
        AccountUtil.updateCurrentAccount(null)
        assertFalse(AccountUtil.isLoggedIn())

        AccountUtil.updateCurrentAccount(Account(uid = "", bduss = ""))
        assertFalse(AccountUtil.isLoggedIn())

        AccountUtil.updateCurrentAccount(Account(uid = "12345", bduss = ""))
        assertFalse(AccountUtil.isLoggedIn())

        AccountUtil.updateCurrentAccount(Account(uid = "12345", bduss = "valid_bduss"))
        assertTrue(AccountUtil.isLoggedIn())
        assertEquals("12345", AccountUtil.getUid())
        assertEquals("valid_bduss", AccountUtil.getBduss())

        AccountUtil.updateCurrentAccount(null)
    }

    @Test
    fun updateAllAccounts_and_multiAccountFiltering() {
        val acc1 = Account(uid = "1", name = "User1", bduss = "b1")
        val acc2 = Account(uid = "2", name = "User2", bduss = "b2")

        AccountUtil.updateAllAccounts(listOf(acc1, acc2))
        assertEquals(2, AccountUtil.allAccounts.size)

        val remaining = AccountUtil.allAccounts.filter { it.uid != acc1.uid }
        AccountUtil.updateAllAccounts(remaining)
        assertEquals(1, AccountUtil.allAccounts.size)
        assertEquals("User2", AccountUtil.allAccounts[0].name)

        AccountUtil.updateAllAccounts(emptyList())
    }
}
