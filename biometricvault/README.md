# `:biometricvault`

> 强生物识别（指纹 / 人脸）绑定的 AES-256-GCM 密钥库。自动处理"用户加新指纹导致旧密钥失效"。

[![JitPack](https://jitpack.io/v/visiongem/android-secure-toolkit.svg)](https://jitpack.io/#visiongem/android-secure-toolkit)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](../LICENSE)

## 安装

```kotlin
dependencies {
    implementation("com.github.visiongem.android-secure-toolkit:biometricvault:0.1.0")
}
```

minSdk 24，实际触发认证需 API 28+ 且设备已注册强生物识别。传递依赖：`androidx.biometric:biometric:1.1.0`。

## 完整 happy path（加密）

```kotlin
import com.securetoolkit.biometricvault.*

when (val r = BiometricVault.obtainEncryptCipher(activity, alias = "wallet_seed")) {
    is VaultResult.Cipher ->
        BiometricUiHelper.authenticateAndEncrypt(
            activity = activity,
            cipher   = r.cipher,
            plaintext = mnemonic,
            title    = "解锁钱包",
            onSuccess = { encoded -> prefs.edit().putString("blob", encoded).apply() },
            onError   = { code, msg -> log("$code: $msg") },
            onCancel  = { /* 用户主动取消 */ }
        )

    VaultResult.KeyInvalidated -> showReEnrollDialog()        // 用户增删指纹了
    VaultResult.Unavailable    -> fallbackToPassword()        // 设备无强生物识别
    is VaultResult.Failure     -> crashlytics.log(r.throwable)
}
```

## 解密同理

```kotlin
val encoded = prefs.getString("blob", null) ?: return

when (val r = BiometricVault.obtainDecryptCipher(activity, "wallet_seed", encoded)) {
    is VaultResult.Cipher -> BiometricUiHelper.authenticateAndDecrypt(
        activity, r.cipher, encoded,
        onSuccess = { plain -> useMnemonic(plain) },
        onError   = { _, _ -> },
    )
    VaultResult.KeyInvalidated -> /* 千万不要 deleteKey！旧密文用旧 key 加密，重建后救不回来 */ showRecovery()
    VaultResult.Unavailable    -> Unit
    is VaultResult.Failure     -> Unit
}
```

## VaultResult 四分支

| 分支 | 含义 | 处理 |
|---|---|---|
| `Cipher(cipher)` | 拿到 cipher，请丢给 BiometricPrompt 触发认证 | happy path |
| `KeyInvalidated` | 用户增删指纹 / 取消屏幕锁，旧密钥已失效 | 加密路径自动重建；解密路径必须走密码恢复 |
| `Unavailable` | 设备无传感器 / 未注册 / 暂时禁用 | 回退到 PIN / 密码 |
| `Failure(throwable)` | 其他异常 | 上报、视情况降级 |

## 想知道更多

- 完整 API + 算法细节 + 8 个常见坑 → [docs/biometricvault.md](../docs/biometricvault.md)
- 真机调试（含 emulator 指纹模拟）→ [docs/RUNNING_SAMPLE.md](../docs/RUNNING_SAMPLE.md)
- 主仓库 README → [../README.md](../README.md)

## License

Apache 2.0，详见仓库根 [LICENSE](../LICENSE)。
