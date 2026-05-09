package com.securetoolkit.screensecure

import android.app.Activity
import android.os.Build
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * 在敏感页面顶层放一行 [ScreenshotProtector]，自动开启 FLAG_SECURE。
 *
 * 作用范围：截屏（电源+音量）、屏幕录制、MediaProjection、投屏（Cast）、Android 13- 最近任务卡片预览。
 * Android 13+ 还需配合 [Activity.setRecentsScreenshotEnabled] 关闭最近任务截图——本工具会一起处理。
 *
 * 用法：
 * ```
 * @Composable
 * fun MnemonicScreen() {
 *     ScreenshotProtector()
 *     // ...你的敏感 UI...
 * }
 * ```
 *
 * 离开页面时 Compose 自动通过 [DisposableEffect] 调 onDispose 清理 flag，避免污染下个页面。
 */
@Composable
fun ScreenshotProtector(active: Boolean = true) {
    val activity = LocalActivity.current ?: return
    val window = activity.window ?: return

    DisposableEffect(active) {
        if (active) {
            window.setFlags(FLAG_SECURE, FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { activity.setRecentsScreenshotEnabled(false) }
            }
        }
        onDispose {
            window.clearFlags(FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { activity.setRecentsScreenshotEnabled(true) }
            }
        }
    }
}

/**
 * 给非 Compose 调用方的命令式接口（一行加 / 一行去）。
 */
object ScreenSecure {
    fun enable(activity: Activity) {
        activity.window?.setFlags(FLAG_SECURE, FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { activity.setRecentsScreenshotEnabled(false) }
        }
    }

    fun disable(activity: Activity) {
        activity.window?.clearFlags(FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { activity.setRecentsScreenshotEnabled(true) }
        }
    }
}
