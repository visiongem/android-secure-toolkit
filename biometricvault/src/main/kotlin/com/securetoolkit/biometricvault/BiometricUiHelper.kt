package com.securetoolkit.biometricvault

import android.util.Base64
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.crypto.Cipher

/**
 * 把 BiometricPrompt 的回调地狱包成两个高阶函数。调用方只关心成败，不关心 callback。
 */
object BiometricUiHelper {

    private val executor: Executor by lazy { Executors.newSingleThreadExecutor() }

    fun authenticateAndEncrypt(
        activity: FragmentActivity,
        cipher: Cipher,
        plaintext: String,
        title: String = "Verify it's you",
        subtitle: String? = null,
        negativeButtonText: String = "Cancel",
        onSuccess: (encodedIvCiphertext: String) -> Unit,
        onError: (errorCode: Int, errorMsg: CharSequence) -> Unit,
        onCancel: () -> Unit = {},
    ) {
        prompt(activity, cipher, title, subtitle, negativeButtonText,
            onSuccess = { c ->
                runCatching {
                    val ct = c.doFinal(plaintext.toByteArray(Charsets.UTF_8))
                    val payload = (c.iv ?: ByteArray(0)) + ct
                    onSuccess(Base64.encodeToString(payload, Base64.NO_WRAP))
                }.onFailure { onError(-1, it.message ?: "doFinal failed") }
            },
            onError = onError,
            onCancel = onCancel
        )
    }

    fun authenticateAndDecrypt(
        activity: FragmentActivity,
        cipher: Cipher,
        encodedCiphertext: String,
        title: String = "Verify it's you",
        subtitle: String? = null,
        negativeButtonText: String = "Cancel",
        onSuccess: (plaintext: String) -> Unit,
        onError: (errorCode: Int, errorMsg: CharSequence) -> Unit,
        onCancel: () -> Unit = {},
    ) {
        prompt(activity, cipher, title, subtitle, negativeButtonText,
            onSuccess = { c ->
                runCatching {
                    val raw = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
                    val ct = raw.copyOfRange(12, raw.size) // IV 已在 init 时绑定到 cipher
                    onSuccess(String(c.doFinal(ct), Charsets.UTF_8))
                }.onFailure { onError(-1, it.message ?: "doFinal failed") }
            },
            onError = onError,
            onCancel = onCancel
        )
    }

    private fun prompt(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        subtitle: String?,
        negativeButtonText: String,
        onSuccess: (Cipher) -> Unit,
        onError: (Int, CharSequence) -> Unit,
        onCancel: () -> Unit,
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val c = result.cryptoObject?.cipher ?: cipher
                onSuccess(c)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onCancel()
                } else {
                    onError(errorCode, errString)
                }
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }
}
