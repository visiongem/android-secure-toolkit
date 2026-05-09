# 真机调试 sample app

> 三个模块里只有 `:securestore` 能在 emulator/Robolectric 上完整验证。`:biometricvault` 必须真机（或带指纹模拟的 emulator），`:screensecure` 必须人眼观察截屏行为。本文给你一份从 0 到验证完三个模块的最短路径。

---

## 准备

### 设备要求

| 模块 | emulator 够用？ | 真机 | 备注 |
|---|:-:|:-:|---|
| `:securestore` | ✅ | ✅ | API 24+ 且 KeyStore 可用 |
| `:biometricvault` | ⚠️ | ✅ | emulator 需手动注册指纹（见下） |
| `:screensecure` | ✅ | ✅ | 但需要人工尝试截屏验证效果 |

### Emulator 注册虚拟指纹（仅 emulator 用户）

1. AVD Manager 创建 / 选一个 Pixel API 28+ image
2. 启动后 → Settings → Security → Add fingerprint
3. 系统提示触摸传感器时，**新开终端**跑：
   ```bash
   adb -e emu finger touch 1
   ```
   重复 5-6 次直至注册完成。这个 `1` 是模拟指纹 ID，记住它。

---

## 1. 安装 sample app

### Android Studio

1. 顶部 Run Configuration 下拉选 `sample`
2. 选目标设备（真机 / emulator）
3. 点 ▶ 绿色三角

### 命令行（不开 IDE）

```bash
cd /Users/nia/PersonalProjects/android-secure-toolkit
./gradlew :sample:installDebug
adb shell am start -n com.securetoolkit.sample/.MainActivity
```

---

## 2. 验证 `:securestore`

进入 App 看到三栏 Demo。在第 1 栏 **SecureStore**：

1. 在 plaintext 输入框输入任意字符串（比如 `hello secret`）
2. 点 **Encrypt → AES-GCM** —— ciphertext 字段出现 base64 字符串
3. 点 **Decrypt** —— decrypted 字段应回显原文

✅ 通过条件：解密结果 = 原文；多次点 Encrypt 同一明文产生**不同**密文（IV 随机性）

❌ 异常排查：

- ciphertext 为空 → KeyStore 不可用，看 logcat 找 `SecureStoreException`
- 解密结果为空 → 密文被篡改 / 或者你切了用户场景丢了 key

### 跑 instrumented test（更严格）

```bash
./gradlew :securestore:connectedDebugAndroidTest
```

会自动跑 10 个端到端用例（roundtrip、IV 唯一性、tag 篡改、wrong alias、unicode、长字符串等）。10 绿即模块完整正确。

---

## 3. 验证 `:biometricvault`

> **前置条件**：设备已注册至少一个指纹 / 人脸（emulator 见上文，真机走系统设置）。

进入 App 第 2 栏 **BiometricVault**：

1. 在第 1 栏的 plaintext 输入框输入要保护的字符串（如 `my mnemonic`）
2. 点 **Biometric Encrypt**
3. **指纹弹窗**出现：
   - 真机：用注册的手指点传感器
   - emulator：开终端 `adb -e emu finger touch 1`
4. 认证成功 → bio ciphertext 字段填上 base64
5. 点 **Biometric Decrypt** → 再次指纹认证 → bio plaintext 回显

✅ 通过条件：能 encrypt + decrypt 往返，status 显示 `decrypted`

### 故意制造失败场景（验证 KeyInvalidated 处理）

1. 完成上面的 encrypt（产生 bio ciphertext）
2. 系统设置 → 删除已注册的指纹
3. 重新注册一个新指纹
4. 回到 sample 点 **Biometric Decrypt**
5. ✅ 期望：status 显示 `key invalidated, please re-enroll`（而非崩溃）

如果状态显示 `unavailable`，说明设备不支持强生物识别——这个分支也是 sample 设计要测的。

---

## 4. 验证 `:screensecure`

进入 App 第 3 栏 **ScreenshotProtector**：

1. 默认 Switch 是关闭状态。截屏（电源 + 音量下，或 emulator 顶部相机按钮）→ 应该正常截到
2. 打开 Switch（变蓝）→ 再尝试截屏
3. ✅ 期望：系统提示 `无法截图，应用程序或组织不允许截屏` 或类似（设备语言决定）

### 验证最近任务卡片

1. 打开 Switch
2. 按 Home / Back 出 App，调出最近任务（手势上滑停留 / 三键导航的方块）
3. ✅ 期望：sample 的卡片显示**纯黑**或**应用图标遮罩**，看不到 UI 内容
4. 关闭 Switch 后再试 → 卡片正常显示

### 验证投屏（可选）

如果你有 Cast 设备 / 二屏 / 屏幕录制 App：

1. 开启 Switch + 启动 MediaProjection 录屏
2. ✅ 期望：录制结果是黑屏

---

## 三件事容易翻车

### 1. emulator 指纹突然失效

emulator 重启 / 切换 cold boot 后，已注册指纹**可能丢**。重新走 Settings → Security → Add fingerprint 即可。

### 2. 真机厂商定制 ROM 行为差异

某些厂商（如部分国产品牌）：
- "面部识别"被它们归类为弱生物识别 → `BiometricManager.canAuthenticateStrong()` 返回不可用
- "最近任务卡片预览"用了私有 API 渲染，`FLAG_SECURE` 不彻底

测试时建议至少覆盖：1 台 Pixel + 1 台主流国产（小米 / 华为 / OPPO 任一）。

### 3. App 主题不是 NoActionBar 导致 ScreenshotProtector 表现异常

sample 里用的是 `Theme.SecureToolkitSample`（继承 `Theme.Material.Light.NoActionBar`）。如果你把库引到自己 App 里，主题是 `Theme.AppCompat.Light` 或 `Theme.Material3.DayNight` 应该都正常。**自定义 `windowIsTranslucent=true` / `windowIsFloating=true` 主题需实测**。

---

## 通过这 3 步后

- `:securestore` 端到端 OK + instrumented test 全绿
- `:biometricvault` 真机指纹往返成功 + KeyInvalidated 分支也走通
- `:screensecure` 截屏被系统拦截 + 最近任务卡片黑屏

这 3 件事走完，本库的 v0.1.0 才算"真的能用"。建议把这份 checklist 跑过一遍后在 GitHub README 加一个 `Verified on: Pixel 7 / Android 14` 之类的说明。

Human review 之后下一步：发博客 → 看反馈 → 决定要不要做 v0.2。
