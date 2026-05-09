# Roadmap

> 务实路线图：写得出来的就在表上，写不出来的不在表上。**不承诺时间表**——本项目暂为业余维护。

---

## v0.1.0（已发布，2026-05-09）

- ✅ `:securestore` AES-256-GCM Keystore 加密
- ✅ `:biometricvault` 强生物识别绑定 AES key
- ✅ `:screensecure` Compose `ScreenshotProtector` + Android 13+ 最近任务截图禁用
- ✅ Sample app 演示三模块
- ✅ 三模块完整使用文档（`docs/*.md`）
- ✅ 真机验证（OnePlus 9 / OxygenOS / Android 14）
- ✅ JitPack 发版 + GitHub Actions CI

## v0.2.0（开发中）

聚焦**测试覆盖度 + 工具链完善**，不引入新模块。

- [ ] `:securestore` 单元测试 + Robolectric 测试覆盖率 ≥ 70%
- [ ] `:biometricvault` instrumented test（覆盖 KeyInvalidated 重建路径）
- [ ] `:screensecure` Compose UI test（验证 `DisposableEffect` 清理时机）
- [ ] 多设备真机矩阵验证：至少补 1 台 Pixel + 1 台主流国产 ROM
- [ ] sample app 加 instrumented end-to-end 测试（CI 跑 emulator）
- [ ] 把 `<table>` HTML 渲染兜底验证：Markdown / 公众号 / 知乎不同平台都试
- [ ] 在 `docs/` 加 `BENCHMARK.md`：encrypt/decrypt 耗时实测、不同设备 KeyStore 初始化代价

## v0.3.0（候选，按使用反馈裁剪）

候选项目按"是否真的有人在用 → 真的需要这个"决定取舍：

- [ ] `:securestore`：`encryptStream` / `decryptStream` 流式 API（应对大对象加密）
- [ ] `:biometricvault`：增加"Compose-first" API（`@Composable fun rememberBiometricVault()`）
- [ ] `:screensecure`：`DialogProperties` 自动注入工具，无需调用方手填 `securePolicy = SecureOn`
- [ ] 切换发布到 Maven Central（需 GnuPG + Sonatype 账号 + JIRA 流程）
- [ ] 多语言 README（英文版 → Medium / dev.to 同步）

## 长期可能（v1.0+）

不承诺，但保留思考方向：

- **`:secureprefs`** 第 4 个模块：基于 `:securestore` 包装一层 `SharedPreferences`-like API（与官方 EncryptedSharedPreferences 区别：纯 Compose-first、零反射）
- **`:obfuscation`** 第 5 个模块：字符串 / 资源 / 类名混淆增强（与 R8 互补）—— 但市场上 DexGuard / Bangcle 已经很强，需要明确差异化
- **Compose Multiplatform 支持**：`:screensecure` 的 `FLAG_SECURE` 在 iOS 没有等价 API（iOS 是 `isSecureTextEntry` + `UIScreen.captured`），可探索 KMP 适配

## 不在路线图上（明确不做）

避免读者期待落空：

- ❌ **JNI / Native 加密原语封装**：会破坏"零反射 / 纯 Kotlin"承诺
- ❌ **Hilt / Koin 内置依赖**：库本身保持 DI 中立
- ❌ **DexGuard 集成**：商业产品，与开源定位冲突
- ❌ **设备 root / 调试器检测**：超出本库威胁模型（见 [SECURITY.md](../SECURITY.md)）；推荐配合 SafetyNet / Play Integrity API

## 反馈渠道

- 功能请求：[GitHub Issues](https://github.com/visiongem/android-secure-toolkit/issues) 加 `enhancement` 标签
- 用法讨论：[GitHub Discussions](https://github.com/visiongem/android-secure-toolkit/discussions)
- 安全漏洞：[SECURITY.md](../SECURITY.md)

---

*最后更新：2026-05-09*
