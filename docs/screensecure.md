# screensecure

> Compose 一行 `ScreenshotProtector()` 自动开关 `FLAG_SECURE` + Android 13+ 最近任务截图禁用，离开页面自动清理。

## 何时用

- 助记词 / 私钥 / 二维码展示页
- 银行卡正反面、身份证页
- 一次性密码 / 短信验证码展示页
- "财务详情"、转账金额、余额等敏感数字
- 企业 App 的内部文档预览页

## 何时不用

- 整个 App 都加 `FLAG_SECURE`——会显著降低用户体验（截屏分享、客服截图都被阻；最近任务变黑）。仅在"敏感页"开。
- 需要在敏感页内允许"用户主动分享"——`FLAG_SECURE` 会让所有截屏失败，包括用户合法操作。需要细分时，请在分享 sheet 触发瞬间 `disable()`、用户取消后 `enable()`。

## 安装

```kotlin
dependencies {
    implementation("com.securetoolkit:screensecure:0.1.0")
}
```

minSdk 24。依赖 Compose Runtime（项目用 Compose 时已经有）。

## 快速开始（Compose）

```kotlin
import com.securetoolkit.screensecure.ScreenshotProtector

@Composable
fun MnemonicScreen(seed: String) {
    ScreenshotProtector()                  // 整页禁止截屏
    Column { Text(seed) }
}
```

仅当某条件为 true 时启用：

```kotlin
@Composable
fun TransferDetailScreen(state: State) {
    ScreenshotProtector(active = state.containsSensitiveAmount)
    // ...
}
```

## 快速开始（命令式 / 非 Compose）

```kotlin
import com.securetoolkit.screensecure.ScreenSecure

class LegacyActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        ScreenSecure.enable(this)
    }
    override fun onPause() {
        super.onPause()
        ScreenSecure.disable(this)
    }
}
```

## API 参考

```kotlin
@Composable
fun ScreenshotProtector(active: Boolean = true)

object ScreenSecure {
    fun enable(activity: Activity)
    fun disable(activity: Activity)
}
```

`ScreenshotProtector` 的实现细节：

- 进入组合时：`window.setFlags(FLAG_SECURE, FLAG_SECURE)`，API 33+ 额外 `setRecentsScreenshotEnabled(false)`
- 退出组合时（`DisposableEffect.onDispose`）：清掉 `FLAG_SECURE`，恢复 `setRecentsScreenshotEnabled(true)`
- `active` 切换时：触发 onDispose + 重新 init，等价"flag toggle"

## FLAG_SECURE 作用范围

| 场景 | 是否阻止 |
|---|---|
| 用户截屏（电源 + 音量下） | 是 |
| 屏幕录制（系统录屏 / 三方 SDK MediaProjection） | 是（黑屏） |
| 投屏 / Cast / Miracast | 是（黑屏） |
| 远程协助 / TeamViewer 抓帧 | 是 |
| 最近任务卡片预览（Android 12 及更早） | 是 |
| 最近任务卡片预览（Android 13+） | **需配合 `setRecentsScreenshotEnabled(false)`**——`ScreenshotProtector` 已自动处理 |
| 用户系统截屏（root） | 否 |
| 物理相机拍照 | 否 |
| Accessibility Service 抓屏 | 否（这是设计） |

## 设计决策

### 为什么用 `DisposableEffect` 而不是 `LaunchedEffect`

`FLAG_SECURE` 是窗口级标志位——必须在组合销毁时**确定地**清掉，否则会"传染"到下个使用同一 Activity 的页面。`LaunchedEffect` 的 `onDispose` 没那么明确；`DisposableEffect` 是为这种"成对调用"设计的。

### 为什么 onDispose 时还原 `setRecentsScreenshotEnabled(true)`

Android 13+ 的最近任务截图开关是 Activity 级的——一旦关掉，整个 Activity 后续所有页面的最近任务卡片都是黑屏。这通常不是想要的。`onDispose` 把它还原回来，保证只有"敏感页"那段时间黑屏。

### 为什么不一上来就在 `Activity.onCreate` 全局开 FLAG_SECURE

这是**蛮力**做法。问题：
- 影响整个 App 的截屏分享功能（客服支持、社交分享）
- 在低敏感页面影响 UX 完全没必要
- 团队后续"我哪里加错了"排查更难

按页面级粒度控制是正确姿势。`screensecure` 设计初衷就是把这件事变得简单到"不会忘"。

### 为什么不暴露 "全局开关 / 全局关闭" 的 API

故意不暴露。如果你想要"每页都开"，那应当评估是否真的需要——见上一段。

## 命令式 API 的取舍

`ScreenSecure.enable / disable` 给老代码 / 非 Compose 场景用。但有几个坑：

1. **必须成对调用**——忘了 disable 就会传染到其他页
2. **不要在 `onCreate` enable + 不在 `onDestroy` disable**——`Activity` 切换时 onCreate/onDestroy 不一定按期望顺序触发
3. **推荐成对在 `onResume` / `onPause`**——可见即开，不可见即关，最贴近"页面级"语义

如果你的项目是 Compose，**强烈推荐用 `ScreenshotProtector()`**——它自动管理生命周期，没有第 1、2 条坑的可能。

## 常见坑

### 1. 主题需要支持半透明 / 背景

某些自定义 Window / Surface 上 `FLAG_SECURE` 表现不一致。如果你的 Activity 主题继承了 `android:Theme.Translucent` 或自定义 `windowIsFloating`，请实测验证。

### 2. Compose Dialog 的处理

`Dialog` 默认创建独立 Window。`FLAG_SECURE` 加在主 Window 上**不会传染到 Dialog**——如果你在敏感页弹了一个 Dialog 显示助记词，那个 Dialog 仍可被截屏。解决：

```kotlin
@Composable
fun SensitiveDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            securePolicy = SecureFlagPolicy.SecureOn   // ← 关键
        )
    ) {
        ScreenshotProtector()         // 同时给 Dialog 内部 Composition 加，防止某些设备的边缘行为
        // ...
    }
}
```

### 3. `BottomSheet` / `Popup` 也是独立 Window

同上。`ModalBottomSheet` 在 Compose 1.6+ 有 `properties = ModalBottomSheetProperties(securePolicy = ...)`。

### 4. 投屏到外部显示器（DisplayManager）

`FLAG_SECURE` 阻止内容上 secondary display。如果你的 App 有"主屏 + 外接屏"双屏功能，敏感页加了 `FLAG_SECURE` 会让外接屏黑屏——这通常是想要的，但如果不想要，需要在双屏场景细化。

### 5. 测试时怎么验证

**单元测试无法验证 `FLAG_SECURE`**——它是窗口管理器层级的标志，不在 JVM 内可观测。验证方式：

- 真机 / emulator 跑 sample，开启 ScreenshotProtector 后按截屏组合键
- 看到 toast "无法截图，应用程序或组织不允许截屏" 即生效
- 关闭后再截一次，应正常截屏

### 6. 误以为能防"屏幕翻拍"

`FLAG_SECURE` 不防物理相机。一台 iPhone 对着你的 Android 屏幕拍照，照样能拍。**没有任何软件方案**能防物理相机——这是产品层要接受的现实。可缓解措施：动态盲化（hover/touch 才显示）、分屏模糊、水印等。

### 7. ProGuard

`screensecure` 不用反射 / JNI。`consumer-rules.pro` 为空。

## 与其他模块协作

- 凡是用 [biometricvault](biometricvault.md) 解密展示的页面，都应当套 `ScreenshotProtector`——否则解密后的明文会被截屏拿走，等于白做生物识别。
- [securestore](securestore.md) 解密后的内容如果上 UI 也同理。

## 测试

由于 `FLAG_SECURE` 不在 JVM 可观测，单元测试只能覆盖"调用没崩"。建议在 sample 跑手工冒烟。

```kotlin
@Test
fun composableDoesNotCrash() {
    composeTestRule.setContent {
        ScreenshotProtector(active = true)
    }
    // 没异常即通过
}
```

## 进一步阅读

- [WindowManager.LayoutParams.FLAG_SECURE](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE)
- [Activity.setRecentsScreenshotEnabled](https://developer.android.com/reference/android/app/Activity#setRecentsScreenshotEnabled(boolean)) (API 33+)
- [Compose DialogProperties.securePolicy](https://developer.android.com/reference/kotlin/androidx/compose/ui/window/DialogProperties)
