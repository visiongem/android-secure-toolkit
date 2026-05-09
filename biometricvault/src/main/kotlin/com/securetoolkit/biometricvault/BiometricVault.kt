package com.securetoolkit.biometricvault

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 生物识别保护下的 AES-256-GCM 加解密。
 *
 * 与 [com.securetoolkit.securestore.SecureStore] 的差异：
 * - 密钥要求 *用户每次操作必须通过强生物识别认证*（指纹 / 人脸，不接受锁屏密码 fallback）。
 * - 密钥在系统增删任意指纹时自动失效；解密旧密文将抛 [VaultResult.KeyInvalidated]。
 *
 * 用法（典型流程）：
 * ```
 * // 1. 加密前先取一个绑定 cipher
 * when (val r = BiometricVault.obtainEncryptCipher(context, alias = "wallet_secret")) {
 *     is VaultResult.Cipher -> {
 *         // 2. 把 cipher 喂给 BiometricPrompt 触发指纹弹窗，认证成功的 onAuthenticationSucceeded 回调里：
 *         val ct = r.cipher.doFinal(plaintext.toByteArray())
 *         val ivCt = r.cipher.iv + ct                  // IV 由系统在 init 时生成
 *         storeBlob(Base64.encodeToString(ivCt, Base64.NO_WRAP))
 *     }
 *     is VaultResult.KeyInvalidated -> { /* 引导用户重新设置 */ }
 *     is VaultResult.Unavailable -> { /* 设备无生物识别 / 未注册 */ }
 *     is VaultResult.Failure -> { /* 异常 */ }
 * }
 * ```
 *
 * @see BiometricUiHelper 用于触发系统弹窗的 UI 工具。
 */
object BiometricVault {

    private const val PROVIDER = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    /**
     * 设备能力检测。返回 [BiometricManager.BIOMETRIC_SUCCESS] 即可用强生物识别。
     */
    fun canAuthenticateStrong(context: Context): Int =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

    /**
     * 取一把"准备加密"的 cipher。需要传给 BiometricPrompt 触发认证。
     */
    fun obtainEncryptCipher(context: Context, alias: String): VaultResult {
        if (canAuthenticateStrong(context) != BiometricManager.BIOMETRIC_SUCCESS) {
            return VaultResult.Unavailable
        }
        return runCatching {
            val key = getOrCreateKey(context, alias)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key)
            }
            VaultResult.Cipher(cipher)
        }.recover { e ->
            when (e) {
                is KeyPermanentlyInvalidatedException -> {
                    deleteKey(alias)
                    VaultResult.KeyInvalidated
                }
                is UserNotAuthenticatedException -> {
                    // 用户当前未解锁；删 + 重建后下次重试
                    deleteKey(alias)
                    VaultResult.Unavailable
                }
                else -> VaultResult.Failure(e)
            }
        }.getOrDefault(VaultResult.Failure(IllegalStateException("unexpected null")))
    }

    /**
     * 取一把"准备解密"的 cipher。需要外部存储的 IV。
     *
     * @param ivPlusCiphertextB64 调用方在加密时用 `cipher.iv + ciphertext` 拼接后 Base64 存盘的字符串。
     */
    fun obtainDecryptCipher(context: Context, alias: String, ivPlusCiphertextB64: String): VaultResult {
        if (canAuthenticateStrong(context) != BiometricManager.BIOMETRIC_SUCCESS) {
            return VaultResult.Unavailable
        }
        return runCatching {
            val key = getOrCreateKey(context, alias)
            val raw = Base64.decode(ivPlusCiphertextB64, Base64.NO_WRAP)
            require(raw.size > 12) { "ciphertext too short" }
            val iv = raw.copyOfRange(0, 12)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
            VaultResult.Cipher(cipher)
        }.recover { e ->
            when (e) {
                is KeyPermanentlyInvalidatedException -> {
                    // 解密路径不能直接重建——旧密文用旧 key 加密，新 key 救不回来。
                    // 只标记失效；调用方应回退到密码恢复流程。
                    VaultResult.KeyInvalidated
                }
                else -> VaultResult.Failure(e)
            }
        }.getOrDefault(VaultResult.Failure(IllegalStateException("unexpected null")))
    }

    fun deleteKey(alias: String) {
        runCatching {
            KeyStore.getInstance(PROVIDER).apply { load(null) }.deleteEntry(alias)
        }
    }

    private fun getOrCreateKey(context: Context, alias: String): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        return createAuthBoundKey(context, alias)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createAuthBoundKey(context: Context, alias: String): SecretKey {
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // 仅强生物识别（不接受锁屏密码 fallback）
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(true)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
                ) {
                    setIsStrongBoxBacked(true)
                }
            }
            .build()
        gen.init(spec)
        return gen.generateKey()
    }
}

sealed class VaultResult {
    /** 拿到 cipher，请丢给 BiometricPrompt 触发认证。 */
    data class Cipher(val cipher: javax.crypto.Cipher) : VaultResult()

    /** 用户增删指纹 / 取消屏幕锁，密钥已不可用。需要调用方启动恢复流程。 */
    object KeyInvalidated : VaultResult()

    /** 设备暂不支持强生物识别（无传感器、未注册、被禁用等）。 */
    object Unavailable : VaultResult()

    /** 其他未预期的失败（堆栈在 [throwable]）。 */
    data class Failure(val throwable: Throwable) : VaultResult()
}
