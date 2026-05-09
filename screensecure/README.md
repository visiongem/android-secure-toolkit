# `:screensecure`

> Compose 一行 `ScreenshotProtector()` 自动开关 `FLAG_SECURE` + Android 13+ 最近任务截图禁用。离开页面自动清理。

[![JitPack](https://jitpack.io/v/visiongem/android-secure-toolkit.svg)](https://jitpack.io/#visiongem/android-secure-toolkit)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](../LICENSE)

## 安装

```kotlin
dependencies {
    implementation("com.github.visiongem.android-secure-toolkit:screensecure:0.1.0")
}
```

minSdk 24。依赖 Compose Runtime（用 Compose 的项目已自带）。

## Compose 用法（推荐）

```kotlin
import com.securetoolkit.screensecure.ScreenshotProtector

@Composable
fun MnemonicScreen(seed: String) {
    ScreenshotProtector()                    // 整页禁止截屏
    Column { Text(seed) }
}
```

按条件启用：

```kotlin
@Composable
fun TransferScreen(state: TransferState) {
    ScreenshotProtector(active = state.containsSensitiveAmount)
    // ...
}
```

## 命令式用法（非 Compose）

```kotlin
import com.securetoolkit.screensecure.ScreenSecure

class LegacyActivity : AppCompatActivity() {
    override fun onResume()  { super.onResume();  ScreenSecure.enable(this) }
    override fun onPause()   { super.onPause();   ScreenSecure.disable(this) }
}
```

⚠️ 命令式必须**成对调用**——忘了 disable 会污染下个页面。Compose 版本通过 `DisposableEffect` 自动清理，没这个坑。

## 实际效果

OnePlus 9 / OxygenOS / Android 14 上开启后尝试截屏，系统弹出 toast：

> 由于该应用限制，涉及隐私/版权的界面不允许截屏

证据见 [主仓库 README 的真机验证段](../README.md#真机验证)。

## 拦截范围

| 场景 | 拦截 |
|---|:-:|
| 截屏（电源 + 音量下） | ✅ |
| 屏幕录制 / MediaProjection | ✅ |
| 投屏 / Cast | ✅（黑屏） |
| 最近任务卡片预览 | ✅（自动配合 Android 13+ `setRecentsScreenshotEnabled`） |
| Compose `Dialog` | ❌ 默认不继承——需要单独加 `DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)` |
| 物理相机翻拍 | ❌（没有任何软件方案能防） |
| Accessibility Service | ❌（设计如此） |

## 想知道更多

- 完整 API + Dialog/BottomSheet 陷阱 + 7 个常见坑 → [docs/screensecure.md](../docs/screensecure.md)
- 主仓库 README → [../README.md](../README.md)

## License

Apache 2.0，详见仓库根 [LICENSE](../LICENSE)。
