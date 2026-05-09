# `:securestore`

> Android Keystore 包装的 AES-256-GCM 字符串加密。一行调用，硬件密钥保护。

[![JitPack](https://jitpack.io/v/visiongem/android-secure-toolkit.svg)](https://jitpack.io/#visiongem/android-secure-toolkit)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](../LICENSE)

## 安装

```kotlin
dependencies {
    implementation("com.github.visiongem.android-secure-toolkit:securestore:0.1.0")
}
```

minSdk 24。无传递依赖（仅 Android 平台 API）。

## 三段就够用

### 1. 加密落盘

```kotlin
import com.securetoolkit.securestore.SecureStore

val payload = SecureStore.encrypt(alias = "auth.access_token", plaintext = rawToken)
prefs.edit().putString("token", payload).apply()
```

### 2. 解密读取

```kotlin
val raw = SecureStore.decrypt(
    alias = "auth.access_token",
    encoded = prefs.getString("token", null) ?: return
)
```

### 3. 失效清理（用户登出时）

```kotlin
SecureStore.deleteKey("auth.access_token")
```

## 想知道更多

- 完整 API + 算法细节 + 7 个常见坑 → [docs/securestore.md](../docs/securestore.md)
- 主仓库 README → [../README.md](../README.md)
- 真机调试 → [docs/RUNNING_SAMPLE.md](../docs/RUNNING_SAMPLE.md)

## 何时**不要**用 `:securestore`

- 私钥 / 助记词 / 主密码：每次访问都要用户手动授权 → 用 [`:biometricvault`](../biometricvault/README.md)
- 大文件：`String` API 不适合流式 → 自己封装 `CipherOutputStream`
- 跨设备同步的密文：Keystore 密钥与设备绑定，换机即失效

## License

Apache 2.0，详见仓库根 [LICENSE](../LICENSE)。
