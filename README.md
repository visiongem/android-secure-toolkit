# Android Secure Toolkit

> 一套面向"用户敏感数据保护"的 Android 库，全 Kotlin / 全 Compose / 零反射 / 零 DI 依赖。

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![minSdk](https://img.shields.io/badge/minSdk-24-3DDC84?logo=android)](https://developer.android.com/)

## 为什么造这个轮子

Android 平台层的安全 API（`Keystore`、`BiometricPrompt`、`FLAG_SECURE`）功能完整但官方文档分散、StackOverflow 答案普遍过时。本库把高安全场景下验证过的写法抽离为 3 个互相独立的小库，开箱即用。

| 模块 | 干什么 | 一句话使用 |
|---|---|---|
| `:securestore` | 用 AndroidKeyStore 把字符串落盘（AES-256-GCM） | `SecureStore.encrypt(alias, "secret")` |
| `:biometricvault` | 指纹保护任意 AES key，自动失效防换指纹绕过 | `BiometricVault.obtainEncryptCipher(ctx, alias)` |
| `:screensecure` | Compose 一行代码加 `FLAG_SECURE` 防截屏 | `ScreenshotProtector()` |

## 快速开始

```kotlin
// 1. 加密一段敏感字符串落盘
val token = SecureStore.encrypt(alias = "user_token", plaintext = rawToken)
prefs.edit().putString("token", token).apply()

// 2. 在敏感页加防截屏（Compose）
@Composable fun MnemonicScreen() {
    ScreenshotProtector()
    Text("your seed phrase here")
}

// 3. 用指纹保护一个 AES key（典型流程见 sample app）
when (val r = BiometricVault.obtainEncryptCipher(activity, alias = "wallet_key")) {
    is VaultResult.Cipher -> BiometricUiHelper.authenticateAndEncrypt(
        activity, r.cipher, plaintext = secret,
        onSuccess = { encoded -> /* 落盘 */ },
        onError   = { _, _ -> },
    )
    VaultResult.KeyInvalidated -> { /* 引导用户重新设置 */ }
    VaultResult.Unavailable    -> { /* 设备不支持 */ }
    is VaultResult.Failure     -> { /* 处理异常 */ }
}
```

## 安装

### 通过 JitPack（推荐，起步阶段）

`settings.gradle.kts`：
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

`app/build.gradle.kts`（按需选择）：
```kotlin
dependencies {
    implementation("com.github.visiongem.android-secure-toolkit:securestore:0.1.0")
    implementation("com.github.visiongem.android-secure-toolkit:biometricvault:0.1.0")
    implementation("com.github.visiongem.android-secure-toolkit:screensecure:0.1.0")
}
```

> 把 `visiongem` 换成实际用户名。每个模块独立可装，不强制三个一起引。

### Maven Central（计划中）

待 0.2 版本后切换到 Maven Central 发布，届时坐标会简化为 `com.securetoolkit:xxx:版本`。

## 设计原则

- **零强依赖**：库不引入 Hilt / Koin / RxJava；调用方按需自接 DI。
- **失败即 null**：默认 API 不抛异常，避免上层全到处 try-catch。需要详细错误用 `xxxOrThrow`。
- **不偷偷弱化**：`AES-256-GCM`、`12B IV`、`128bit GCM tag` 全部硬编码，不让调用方传弱配置。
- **跟着 Android 平台走**：API 30+ 用 `setUserAuthenticationParameters`，API 28+ 用 StrongBox，API 33+ 关 `RecentsScreenshot`，老 API 自动降级。

## 模块详细文档

- [securestore — AES-GCM Keystore 落盘](docs/securestore.md)
- [biometricvault — 生物识别保护 AES key](docs/biometricvault.md)
- [screensecure — Compose 防截屏](docs/screensecure.md)

## 已知约束

- minSdk 24（KeyStore Provider AES 模式可用最低版本）
- BiometricVault 实际生效需 API 28+ 且设备已注册强生物识别
- StrongBox 仅 Pixel 3+ / 三星 S 系列等高端机

## License

Apache 2.0 — 商用、修改、再发布皆可，保留 NOTICE 即可。详见 [LICENSE](LICENSE)。
