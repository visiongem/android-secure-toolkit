# securestore

> 用 AndroidKeyStore 把字符串落盘，AES-256-GCM 认证加密，一行调用。

## 何时用

- 持久化 access token / refresh token
- 落盘缓存用户敏感字段（身份证号、手机号尾段、PIN 缓存）
- 给本地数据库的某些列做应用层加密（行级加密）
- 把会话密钥 / 三方 SDK 的 ApiKey 放进 SharedPreferences 前包一层

## 何时不用

- 助记词 / 私钥 / 主密码——这些应当用 [biometricvault](biometricvault.md)，确保每次访问都需要用户手动授权
- 大文件加密——`SecureStore` 只接受 `String`，且 GCM 一次性 doFinal 不适合流式数据
- 跨设备同步的数据——KeyStore 密钥与设备绑定，换机即失效

## 安装

当前未发版，先用 `includeBuild` 或 `project(":securestore")` 接入：

```kotlin
// settings.gradle.kts
includeBuild("path/to/android-secure-toolkit") {
    dependencySubstitution {
        substitute(module("com.securetoolkit:securestore")).using(project(":securestore"))
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.securetoolkit:securestore:0.1.0")
}
```

minSdk 24。无传递依赖（仅平台 API）。

## 快速开始

```kotlin
import com.securetoolkit.securestore.SecureStore

// 加密
val payload = SecureStore.encrypt(alias = "user_token", plaintext = rawToken)
prefs.edit().putString("token", payload).apply()

// 解密
val raw = SecureStore.decrypt(alias = "user_token", encoded = prefs.getString("token", null) ?: "")
```

## API 参考

```kotlin
object SecureStore {
    fun encrypt(alias: String, plaintext: String): String?
    fun decrypt(alias: String, encoded: String): String?

    @Throws(SecureStoreException::class)
    fun encryptOrThrow(alias: String, plaintext: String): String
    @Throws(SecureStoreException::class)
    fun decryptOrThrow(alias: String, encoded: String): String

    fun deleteKey(alias: String)
    fun containsKey(alias: String): Boolean
}

class SecureStoreException(message: String, cause: Throwable? = null) : RuntimeException
```

### `encrypt` / `decrypt` vs `encryptOrThrow` / `decryptOrThrow`

| 风格 | 失败行为 | 适合场景 |
|---|---|---|
| `encrypt` / `decrypt` | 任何异常 → `null` | 大多数业务代码，调用方 `?.let` 处理 |
| `encryptOrThrow` / `decryptOrThrow` | 抛 `SecureStoreException` | 启动期初始化、迁移逻辑、需要详细日志的路径 |

### `alias`

每条数据用唯一的 alias 标识。两条数据共用同一 alias 仅意味着"用同一密钥"——并不冲突，但建议拆分以便单独失效（`deleteKey`）。

命名建议：
- 业务前缀 + 数据语义：`auth.access_token`、`profile.idcard_tail`、`session.api_key`
- 多用户场景叠用户 ID：`auth.${userId}.access_token`

## 算法细节

```
密文格式（Base64 之前的字节流）：
┌──── 12 bytes ────┬─── plaintext.length bytes ───┬─── 16 bytes ───┐
│       IV         │         ciphertext           │   GCM auth tag │
└──────────────────┴──────────────────────────────┴────────────────┘
                               输出 = Base64.NO_WRAP(上述拼接)
```

- 算法：`AES/GCM/NoPadding`
- 密钥：256 bit，存于 `AndroidKeyStore` provider，硬件支持时由 TEE / StrongBox 持有
- IV：每次加密随机 12 字节（NIST SP 800-38D §5.2.1.1 推荐长度）
- GCM tag：128 bit
- 同 alias 多次加密：每次 IV 不同，密文不同

## 与 KeyStore 的边界

`SecureStore` 不会持有 `SecretKey` 实例。每次 `encrypt` / `decrypt` 都从 KeyStore 重新取——避免长期内存驻留，也避免线程安全问题。这是和"自己 cache 一个 SecretKey"写法的关键差异。

`AndroidKeyStore` 的密钥**不可导出**——你只能拿到 alias 句柄，不能从设备拷贝出原始 key bytes。意味着：

- root 也无法导出密钥（仅能"调用"它，没法"拿走"它）
- 设备工厂重置后所有密钥消失
- `adb backup` 拿走的密文回到另一台设备无法解密

## 常见坑

### 1. 不要把 `encoded` 字符串拼接修改后再传回

GCM 的 tag 校验非常严格——任何 1 bit 改动都会让 `decrypt` 返回 `null`。如果你在 SharedPreferences 写盘前做了任何 base64 变种（URL safe vs 标准）、trim 空格、字符集转换，都会导致解密失败。

```kotlin
// ❌ 错
val saved = SecureStore.encrypt(alias, plaintext).orEmpty().trim()
prefs.edit().putString("k", saved).apply()

// ✅ 对
val saved = SecureStore.encrypt(alias, plaintext) ?: return
prefs.edit().putString("k", saved).apply()
```

### 2. 不要用 alias 复用做 "同一密钥多版本" 把戏

如果你想"用相同密钥加密两批数据"，请直接用同一 alias 调用两次，**不要**自己拼接两段密文存到一个字段——拆字段 / 拆 key 都比这干净。

### 3. `KeyPermanentlyInvalidatedException` 不会发生

`SecureStore` 创建的密钥**不绑定生物识别**，因此不会因指纹增删而失效。这是和 `BiometricVault` 的关键差异。需要"指纹保护"请用 `BiometricVault`。

### 4. 加密 `null` 字段的取舍

`encrypt` 不接受 `null`（参数不可空）。建议在业务层用 sentinel 字符串（如 `""` 或自定义占位）；不要用 `"null"` 字符串作为占位——会被解释为合法的"四个字符的明文"。

### 5. 不同 App 之间不能共享

每个 App 有独立的 KeyStore namespace。即使两个 App 用相同 alias 写入相同明文，密文**不同**且**互不可解**。这是平台隔离设计，不是 bug。

### 6. KeyStore 初始化代价

第一次调用 `encrypt(alias, ...)`（特定 alias 不存在时）会触发 `KeyGenerator.generateKey()`——硬件支持的设备上耗时 100~500ms。建议在启动期或后台线程预热，不要在 UI 主线程首次调用关键 alias。

### 7. ProGuard

`SecureStore` 内部不用反射、不用 JNI 名字查找，混淆友好。库自带的 `consumer-rules.pro` 为空——调用方不用做任何特殊配置。

## 测试

库内单元测试在 `securestore/src/test/kotlin/`。Robolectric 对真实 KeyStore 的支持有限，所以单元测试只覆盖**纯逻辑契约**（参数校验、null 兜底、API 形态）。**端到端验证（真实加解密往返）请放在 `androidTest/`** 跑在真机或 emulator 上。

样板（待补到 `sample/src/androidTest/`）：

```kotlin
@RunWith(AndroidJUnit4::class)
class SecureStoreInstrumentedTest {
    private val alias = "instrumented_test_alias"

    @After fun tearDown() = SecureStore.deleteKey(alias)

    @Test fun roundTrip() {
        val cipher = SecureStore.encrypt(alias, "hello")
        assertNotNull(cipher)
        assertEquals("hello", SecureStore.decrypt(alias, cipher!!))
    }

    @Test fun differentIvEachTime() {
        val a = SecureStore.encrypt(alias, "x")
        val b = SecureStore.encrypt(alias, "x")
        assertNotEquals(a, b)        // GCM 同明文不同 IV → 不同密文
    }

    @Test fun tamperedCiphertextReturnsNull() {
        val cipher = SecureStore.encrypt(alias, "hello")!!
        val tampered = cipher.replaceFirst('A', 'B')
        assertNull(SecureStore.decrypt(alias, tampered))
    }
}
```

## 与其他模块协作

- 用 `SecureStore` 落盘的数据**仍然受系统截屏威胁**。如果加解密后的数据要在 UI 上展示，配合 [screensecure](screensecure.md) 给那个页面加 `ScreenshotProtector`。
- 当数据敏感到"每次访问都要用户授权"时，请改用 [biometricvault](biometricvault.md)，而不是 `SecureStore`。

## 进一步阅读

- [Android Keystore system](https://developer.android.com/training/articles/keystore)
- [NIST SP 800-38D — GCM mode](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf)
- [StrongBox keymaster](https://source.android.com/docs/security/features/keystore)
