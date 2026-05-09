# biometricvault

> 强生物识别（指纹 / 人脸）绑定 AES-256-GCM 密钥，自动处理"指纹增删导致密钥失效"。

## 何时用

- 助记词 / 私钥 / 主密码 / 单次大额转账签名密钥的本地存储
- "每次访问都要用户手动授权"的高敏感数据
- 应用启动后第一次访问敏感数据的解锁场景

## 何时不用

- 不需要每次授权的常规敏感字段（用 [securestore](securestore.md) 即可，体验更顺）
- 设备生物识别尚未注册的场景——此时 `BiometricVault` 不可用，应当回退到 PIN / 密码
- 想做"密码备选 + 生物识别快捷"——这是更复杂的 fallback 流程，本库只覆盖核心加解密，UI 决策由调用方组合

## 安装

```kotlin
dependencies {
    implementation("com.securetoolkit:biometricvault:0.1.0")
}
```

传递依赖：`androidx.biometric:biometric:1.1.0`（包含 `androidx.fragment`）。minSdk 24，但实际触发认证需 API 28+ 且设备已注册强生物识别。

## 工作模型

`BiometricVault` 不是 "存值 → 取值" 的简单 KV。它返回一把 `Cipher`，**调用方需要把这把 cipher 喂给 `BiometricPrompt` 触发系统弹窗**。用户认证成功后，cipher 才被授权用于实际 `doFinal`。

```
调用方                 BiometricVault                BiometricPrompt
   │ obtainEncryptCipher │                                 │
   ├────────────────────>│                                 │
   │  VaultResult.Cipher │                                 │
   │<────────────────────┤                                 │
   │                                                       │
   │ authenticate(CryptoObject(cipher))                    │
   ├──────────────────────────────────────────────────────>│ [指纹弹窗]
   │                                                       │
   │ onAuthenticationSucceeded(result.cryptoObject.cipher) │
   │<──────────────────────────────────────────────────────┤
   │                                                       │
   │ cipher.doFinal(plaintext.toByteArray())               │
   │ → 落盘                                                │
```

`BiometricUiHelper` 把右半边封装好了——多数情况下你只需要写第一步和最后"成功回调"。

## 快速开始：加密

```kotlin
import com.securetoolkit.biometricvault.*

// 1. 取一把 "准备加密" 的 cipher
when (val r = BiometricVault.obtainEncryptCipher(activity, alias = "wallet_key")) {

    // 2. happy path: 弹出指纹框，认证成功后自动 doFinal 并把结果传回
    is VaultResult.Cipher -> BiometricUiHelper.authenticateAndEncrypt(
        activity = activity,
        cipher   = r.cipher,
        plaintext = mnemonic,
        title    = "解锁钱包",
        subtitle = "需要使用指纹",
        onSuccess = { encoded ->
            prefs.edit().putString("wallet_blob", encoded).apply()
        },
        onError = { code, msg -> showToast("失败 $code: $msg") },
        onCancel = { /* 用户主动取消，不报错 */ }
    )

    // 3. 边缘情况
    VaultResult.KeyInvalidated -> {
        // 用户增删了指纹 → 旧 key 已失效，请引导用户用密码恢复并重新设置
        showReEnrollDialog()
    }
    VaultResult.Unavailable -> {
        // 设备无生物识别 / 未注册 / 被禁用
        showFallbackPasswordEntry()
    }
    is VaultResult.Failure -> {
        // 其他异常
        crashlytics.log(r.throwable)
    }
}
```

## 快速开始：解密

```kotlin
val encoded = prefs.getString("wallet_blob", null) ?: return

when (val r = BiometricVault.obtainDecryptCipher(activity, "wallet_key", encoded)) {
    is VaultResult.Cipher -> BiometricUiHelper.authenticateAndDecrypt(
        activity = activity,
        cipher   = r.cipher,
        encodedCiphertext = encoded,
        title    = "解锁钱包",
        onSuccess = { plain -> useMnemonic(plain) },
        onError   = { _, _ -> },
    )
    VaultResult.KeyInvalidated -> {
        // 解密路径**不能**重建密钥——旧密文用旧 key 加密，重建后救不回来。
        // 请引导用户走密码恢复。
        showRecoveryFlow()
    }
    VaultResult.Unavailable -> showFallback()
    is VaultResult.Failure -> Unit
}
```

## API 参考

### `BiometricVault`

```kotlin
object BiometricVault {
    /** 设备能力检测；返回 BiometricManager.BIOMETRIC_SUCCESS 即可用。 */
    fun canAuthenticateStrong(context: Context): Int

    /** 取一把准备加密的 cipher。需要传给 BiometricPrompt。 */
    fun obtainEncryptCipher(context: Context, alias: String): VaultResult

    /** 取一把准备解密的 cipher。IV 已绑定到 cipher 上。 */
    fun obtainDecryptCipher(context: Context, alias: String, ivPlusCiphertextB64: String): VaultResult

    /** 主动删除某 alias。用于"指纹失效"或"用户登出"清理。 */
    fun deleteKey(alias: String)
}
```

### `VaultResult`

```kotlin
sealed class VaultResult {
    data class Cipher(val cipher: javax.crypto.Cipher) : VaultResult()
    object KeyInvalidated : VaultResult()
    object Unavailable : VaultResult()
    data class Failure(val throwable: Throwable) : VaultResult()
}
```

### `BiometricUiHelper`

```kotlin
object BiometricUiHelper {
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
    )

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
    )
}
```

`Activity` 必须是 `FragmentActivity` 或其子类（`AppCompatActivity` 也可）——这是 `BiometricPrompt` 的硬要求。

## 算法细节

```
密文格式：与 SecureStore 一致
Base64( IV(12B) || ciphertext || GCM_TAG(16B) )
```

密钥配置（`KeyGenParameterSpec`）：

| 设置 | 值 | 作用 |
|---|---|---|
| `setBlockModes` | `GCM` | AEAD，认证 + 机密 |
| `setEncryptionPaddings` | `NONE` | GCM 不需要 padding |
| `setKeySize` | 256 | AES-256 |
| `setUserAuthenticationRequired` | `true` | 每次操作必须用户授权 |
| `setUserAuthenticationParameters` (API 30+) | `0, AUTH_BIOMETRIC_STRONG` | 仅强生物识别，不接受锁屏密码 fallback |
| `setUserAuthenticationValidityDurationSeconds` (API 23–29) | `-1` | 等价"仅生物识别 + 单次操作有效" |
| `setInvalidatedByBiometricEnrollment` | `true` | 增删任意指纹 → 旧 key 立即失效 |
| `setIsStrongBoxBacked` (API 28+，硬件支持时) | `true` | 安全芯片持有，物理隔离主 CPU |

### 为什么不接受锁屏密码 fallback

`BiometricPrompt` 默认允许"指纹失败 N 次后输入锁屏密码"作为 fallback。**强加密场景必须关掉这个**——否则攻击者拿到锁屏密码（社工、肩窥）就能解密所有数据。`AUTH_BIOMETRIC_STRONG` 排除了 `DEVICE_CREDENTIAL`，把唯一通路锁定在生物识别本身。

## 关键差异：加密 vs 解密的 KeyInvalidated 处理

`BiometricVault.obtainEncryptCipher` 在抛 `KeyPermanentlyInvalidatedException` 时**会自动 deleteKey + 重建**——下次调用即可继续工作，因为加密路径不需要保留旧密钥。

`BiometricVault.obtainDecryptCipher` 在抛该异常时**只返回 `KeyInvalidated`，不会 deleteKey**——因为旧密文用旧 key 加密，删了就彻底救不回来了。调用方必须引导用户走"密码恢复"流程，重建数据。

## 常见坑

### 1. 不要把 cipher 缓存到 ViewModel 字段

`Cipher` 实例使用一次后状态被消耗（GCM 模式尤其敏感）。每次操作都重新调 `obtainEncryptCipher` / `obtainDecryptCipher`。

### 2. 不要在 `obtainEncryptCipher` 后直接 `cipher.doFinal`

未经 `BiometricPrompt.authenticate(CryptoObject(cipher))` 授权的 cipher 调用 `doFinal` 会抛 `IllegalBlockSizeException`（实际是 `KeyStoreException` 包装的）。**必须先认证再 doFinal**——这正是密钥绑定生物识别的意义。

### 3. 用户在指纹弹窗外"完全没操作"

`BiometricPrompt` 不会自动超时；如果用户既不输指纹也不点取消而是直接 home 出 App，回来后 prompt 仍可能在那里挂着。建议在 `Activity.onPause` 里 dismiss prompt——目前 `BiometricUiHelper` 未处理，是已知 TODO。

### 4. `KeyPermanentlyInvalidatedException` 的所有触发条件

- 用户在系统设置里增删任意指纹 / 重新录入面部
- 用户把屏幕锁从"指纹"改成"无"或"图案/PIN/密码"（取消生物识别）
- 设备恢复出厂设置（这种情况整个 KeyStore 都没了）

前两种是用户主动行为，可恢复；第三种数据全丢，无解。

### 5. Robolectric 测不到真实生物识别

单元测试只能覆盖"参数校验 + sealed 分支"。**真实加解密 + 指纹弹窗**必须在真机或物理 emulator（带 fingerprint emulator）上跑 `androidTest/`。

### 6. StrongBox 不可用时的降级

代码已经做了 `hasSystemFeature(FEATURE_STRONGBOX_KEYSTORE)` 检测，没有 StrongBox 的设备自动用普通 TEE。**不要**自己额外检测——降级是隐式的，不影响 API 行为。

### 7. 不要在 `Application.onCreate` 调 `obtainEncryptCipher`

需要 `Context`（最好 `Activity`）。生物识别相关 API 在 Application 上下文不一定可用，且会触发 KeyStore 初始化在主线程的副作用。把首次调用放在 Activity 创建后。

### 8. 生物识别 vs LSKF（Lock Screen Knowledge Factor）

本库只支持纯生物识别 (`BIOMETRIC_STRONG`)。如果你的产品需要"指纹失败可用 PIN/密码 fallback"，请：

1. 用 `setUserAuthenticationParameters(0, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` 重写 keygen
2. 这意味着接受锁屏密码 fallback——和"高安全"的初衷冲突
3. 推荐做法：**应用层 PIN** (用 `SecureStore` 保存 hash) + 生物识别快捷，二者独立——不要把锁屏密码混进来

这是产品决策，本库不强加观点。

## 与其他模块协作

- 解密成功后展示助记词的页面**必须**配合 [screensecure](screensecure.md) 的 `ScreenshotProtector`，否则用户截屏直接拿走。
- `BiometricVault` 仅保护"敏感数据本身"，不保护"应用状态"。如果要做"App 后台 30 秒锁屏 + 生物识别解锁回来"，需要在 ViewModel 层自己实现状态机；本库不提供。

## 测试

```kotlin
@RunWith(AndroidJUnit4::class)
class BiometricVaultInstrumentedTest {
    private val alias = "test_vault"

    @After fun tearDown() = BiometricVault.deleteKey(alias)

    @Test fun unavailableOnEmulatorWithoutFingerprint() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // 期望模拟器没注册指纹时返回 Unavailable
        val r = BiometricVault.obtainEncryptCipher(ctx, alias)
        assertTrue(r is VaultResult.Unavailable || r is VaultResult.Cipher)
    }
}
```

完整端到端测试（含 BiometricPrompt 弹窗模拟）需要 UI Automator + 物理传感器，超出库责任范围；推荐手动验证。

## 进一步阅读

- [androidx.biometric overview](https://developer.android.com/jetpack/androidx/releases/biometric)
- [Cryptography in Android — auth-bound keys](https://developer.android.com/training/articles/keystore#UserAuthentication)
- [BiometricPrompt strong vs weak](https://source.android.com/docs/security/features/biometric/measure)
