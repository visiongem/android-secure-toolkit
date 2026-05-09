package com.securetoolkit.securestore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 任意敏感字符串的本地落盘加密。
 *
 * 算法：AES-256-GCM；密钥位于 AndroidKeyStore（不可导出，硬件支持时由 TEE/StrongBox 持有）。
 *
 * 输出格式：Base64( IV(12B) || ciphertext || GCM_TAG(16B) )，便于单字段存储。
 *
 * 用法：
 * ```
 * val token = SecureStore.encrypt(alias = "user_token", plaintext = rawToken)
 * val raw   = SecureStore.decrypt(alias = "user_token", encoded   = token)
 * ```
 *
 * 设计权衡：
 * - 不强制每个 alias 一一对应"业务"——调用方自由命名。
 * - 失败统一返回 null（不抛异常），简化上层 if-let 风格。需要详细错误，请改用 [encryptOrThrow]/[decryptOrThrow]。
 * - 不直接持有 SecretKey，每次操作从 KeyStore 取，避免长期内存驻留。
 */
object SecureStore {

    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256
    private const val IV_SIZE_BYTES = 12
    private const val GCM_TAG_BITS = 128

    fun encrypt(alias: String, plaintext: String): String? = runCatching {
        encryptOrThrow(alias, plaintext)
    }.getOrNull()

    fun decrypt(alias: String, encoded: String): String? = runCatching {
        decryptOrThrow(alias, encoded)
    }.getOrNull()

    @Throws(SecureStoreException::class)
    fun encryptOrThrow(alias: String, plaintext: String): String {
        require(alias.isNotEmpty()) { "alias must not be empty" }
        try {
            val key = loadOrCreateKey(alias)
            val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
        } catch (t: Throwable) {
            throw SecureStoreException("encrypt failed for alias=$alias", t)
        }
    }

    @Throws(SecureStoreException::class)
    fun decryptOrThrow(alias: String, encoded: String): String {
        require(alias.isNotEmpty()) { "alias must not be empty" }
        try {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            require(raw.size > IV_SIZE_BYTES) { "ciphertext too short" }
            val iv = raw.copyOfRange(0, IV_SIZE_BYTES)
            val ct = raw.copyOfRange(IV_SIZE_BYTES, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, loadOrCreateKey(alias), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            return String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (t: Throwable) {
            throw SecureStoreException("decrypt failed for alias=$alias", t)
        }
    }

    /** 删除指定 alias 的密钥；之后再调 encrypt 会自动重建。 */
    fun deleteKey(alias: String) {
        runCatching {
            KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(alias)
        }
    }

    fun containsKey(alias: String): Boolean = runCatching {
        KeyStore.getInstance(PROVIDER).apply { load(null) }.containsAlias(alias)
    }.getOrDefault(false)

    private fun loadOrCreateKey(alias: String): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(false)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        return gen.generateKey()
    }
}

class SecureStoreException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
