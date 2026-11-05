package com.huanchengfly.tieba.post.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AccountSecurityUtil {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "tblite_account_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    const val ENCRYPTED_PREFIX = "enc:v1:"

    // Fallback key used when AndroidKeyStore provider is unavailable (e.g. host JVM unit tests)
    private val fallbackKey: SecretKey by lazy {
        val keyBytes = "TiebaLimp_Secure_Account_Key2026".toByteArray(Charsets.UTF_8)
        SecretKeySpec(keyBytes, "AES")
    }

    private fun getSecretKey(): SecretKey {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
            }
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }.getOrElse {
            fallbackKey
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return runCatching {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }.getOrElse {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return runCatching {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        }.getOrElse {
            java.util.Base64.getDecoder().decode(str)
        }
    }

    fun isPlaintext(value: String?): Boolean {
        return !value.isNullOrBlank() && !value.startsWith(ENCRYPTED_PREFIX)
    }

    fun encrypt(plaintext: String?): String {
        if (plaintext.isNullOrEmpty()) return ""
        if (plaintext.startsWith(ENCRYPTED_PREFIX)) return plaintext

        return runCatching {
            val key = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv ?: ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            ENCRYPTED_PREFIX + encodeBase64(combined)
        }.getOrElse {
            plaintext
        }
    }

    fun decrypt(encryptedString: String?): String {
        if (encryptedString.isNullOrEmpty()) return ""
        if (!encryptedString.startsWith(ENCRYPTED_PREFIX)) {
            // Legacy plaintext format, return as-is for backward compatibility
            return encryptedString
        }

        return runCatching {
            val base64Payload = encryptedString.removePrefix(ENCRYPTED_PREFIX)
            val combined = decodeBase64(base64Payload)
            if (combined.size <= GCM_IV_LENGTH) return encryptedString

            val iv = ByteArray(GCM_IV_LENGTH)
            val ciphertextLength = combined.size - GCM_IV_LENGTH
            val ciphertext = ByteArray(ciphertextLength)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertextLength)

            val key = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        }.getOrElse {
            encryptedString
        }
    }
}
