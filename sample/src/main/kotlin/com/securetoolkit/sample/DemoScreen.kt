package com.securetoolkit.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.securetoolkit.biometricvault.BiometricUiHelper
import com.securetoolkit.biometricvault.BiometricVault
import com.securetoolkit.biometricvault.VaultResult
import com.securetoolkit.screensecure.ScreenshotProtector
import com.securetoolkit.securestore.SecureStore

private const val SECURE_STORE_ALIAS = "demo_secure_store"
private const val BIOMETRIC_ALIAS = "demo_biometric_vault"

@Composable
fun DemoScreen() {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var screenSecured by remember { mutableStateOf(false) }
    var plaintext by remember { mutableStateOf("hello secure world") }
    var ciphertext by remember { mutableStateOf("") }
    var decrypted by remember { mutableStateOf("") }

    var bioCiphertext by remember { mutableStateOf("") }
    var bioPlaintext by remember { mutableStateOf("") }
    var bioStatus by remember { mutableStateOf("idle") }

    if (screenSecured) ScreenshotProtector()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Android Secure Toolkit", fontWeight = FontWeight.Bold)
        HorizontalDivider()

        // ── 1. SecureStore ─────────────────────────────────────────
        Text("1. SecureStore", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = plaintext,
            onValueChange = { plaintext = it },
            label = { Text("plaintext") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { ciphertext = SecureStore.encrypt(SECURE_STORE_ALIAS, plaintext).orEmpty() }) {
            Text("Encrypt → AES-GCM")
        }
        Text("ciphertext: $ciphertext")
        Button(onClick = { decrypted = SecureStore.decrypt(SECURE_STORE_ALIAS, ciphertext).orEmpty() }) {
            Text("Decrypt")
        }
        Text("decrypted: $decrypted")

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()

        // ── 2. BiometricVault ──────────────────────────────────────
        Text("2. BiometricVault", fontWeight = FontWeight.SemiBold)
        Text("status: $bioStatus")
        Button(onClick = {
            val a = activity ?: return@Button
            when (val r = BiometricVault.obtainEncryptCipher(a, BIOMETRIC_ALIAS)) {
                is VaultResult.Cipher -> BiometricUiHelper.authenticateAndEncrypt(
                    activity = a,
                    cipher = r.cipher,
                    plaintext = plaintext,
                    onSuccess = { encoded ->
                        bioCiphertext = encoded
                        bioStatus = "encrypted"
                    },
                    onError = { code, msg -> bioStatus = "error $code: $msg" },
                    onCancel = { bioStatus = "cancelled" }
                )
                VaultResult.KeyInvalidated -> bioStatus = "key invalidated, please re-enroll"
                VaultResult.Unavailable -> bioStatus = "biometric unavailable"
                is VaultResult.Failure -> bioStatus = "failure: ${r.throwable.message}"
            }
        }) { Text("Biometric Encrypt") }

        Button(onClick = {
            val a = activity ?: return@Button
            if (bioCiphertext.isEmpty()) { bioStatus = "encrypt first"; return@Button }
            when (val r = BiometricVault.obtainDecryptCipher(a, BIOMETRIC_ALIAS, bioCiphertext)) {
                is VaultResult.Cipher -> BiometricUiHelper.authenticateAndDecrypt(
                    activity = a,
                    cipher = r.cipher,
                    encodedCiphertext = bioCiphertext,
                    onSuccess = { plain ->
                        bioPlaintext = plain
                        bioStatus = "decrypted"
                    },
                    onError = { code, msg -> bioStatus = "error $code: $msg" },
                    onCancel = { bioStatus = "cancelled" }
                )
                VaultResult.KeyInvalidated -> bioStatus = "key invalidated"
                VaultResult.Unavailable -> bioStatus = "biometric unavailable"
                is VaultResult.Failure -> bioStatus = "failure: ${r.throwable.message}"
            }
        }) { Text("Biometric Decrypt") }
        Text("bio ciphertext: $bioCiphertext")
        Text("bio plaintext: $bioPlaintext")

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()

        // ── 3. ScreenshotProtector ────────────────────────────────
        Text("3. ScreenshotProtector", fontWeight = FontWeight.SemiBold)
        Text("当前: ${if (screenSecured) "已开启 (FLAG_SECURE)" else "关闭"}")
        Switch(checked = screenSecured, onCheckedChange = { screenSecured = it })
        Text("开启后请尝试截屏，应被系统拦截。")
    }
}
