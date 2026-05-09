# CLAUDE.md

## 项目身份

- 名字：Android Secure Toolkit
- 用途：Android 平台用户敏感数据保护——AES-GCM Keystore 加密 / 强生物识别绑定密钥 / Compose 防截屏
- 包前缀：`com.securetoolkit`
- License：Apache 2.0
- 仓库：<https://github.com/visiongem/android-secure-toolkit>
- 已发版：v0.1.0（JitPack）

## 模块状态

| 模块 | 一句话定位 |
|---|---|
| `:securestore` | AES-256-GCM + AndroidKeyStore，加密任意敏感字符串落盘 |
| `:biometricvault` | 强生物识别绑定 AES key，KeyPermanentlyInvalidatedException 自动处理 |
| `:screensecure` | Compose 一行 `ScreenshotProtector()` 自动开关 FLAG_SECURE + Android 13+ 最近任务 |
| `:sample` | 单 Activity Compose Demo，演示三模块 happy path |

## 技术栈硬约束

**永远使用**：
- Kotlin 2.0.21（不接受 Java 源文件）
- Jetpack Compose（不接受 XML Layout / findViewById / DataBinding）
- Gradle KTS + Version Catalog（不接受 Groovy）
- KSP（不接受 kapt）
- JVM 17 toolchain
- AGP 8.11.0
- minSdk 24 / targetSdk 36 / compileSdk 36

**不引入**：Hilt / Koin / RxJava / 任何 DI 框架——库要保持 DI 中立。

**错误处理**：默认 API 不抛异常（返回 null 或 sealed class 分支）；需要详细错误用 `xxxOrThrow` 变体。

## 设计原则

- 公开 API 极简：每个模块对外暴露 ≤2 个 object/class，内部细节 `internal`
- 不为"通用性"做抽象：3 模块不共享基类。重复就重复
- 每个模块独立可发布：模块间不互相 implementation
- 配置不可调弱：算法/keysize/IV 全部硬编码

## TODO

- [ ] 真机审核代码逻辑（作者要为代码负责，不能只靠 AI 协助生成）
- [ ] `:biometricvault` instrumented test（KeyInvalidated 重建路径）
- [ ] `:screensecure` Compose UI test（DisposableEffect 清理时机）
- [ ] 多设备真机矩阵：补 1 台 Pixel + 1 台其他 ROM

## 知识产权声明

本仓库为完全独立创作。所有代码基于：

- Android 平台公开 API（Keystore / BiometricPrompt / FLAG_SECURE 等）
- 公开标准与文档（NIST SP 800-38D / Android Developers / AOSP）
- 作者本人的工程经验抽象

不携带任何第三方专有标识、内部 SDK、私有协议、商业逻辑。

## 与姊妹仓库的关系

- [android-kotlin-utils](https://github.com/visiongem/android-kotlin-utils)：通用工具（hex / mask / 防抖 / Lifecycle Flow）
- [android-message-pipeline](https://github.com/visiongem/android-message-pipeline)：通信抽象（小通道传大消息）

三者互不依赖。理论上可组合（kotlin-utils 的 hex + pipeline 的分片 + secure-toolkit 的 AES-GCM = 端到端加密管道），但默认不强制。
