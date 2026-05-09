# Security Policy

## Supported Versions

| Version | Status |
|---------|--------|
| 0.1.x   | ✅ Active |
| < 0.1   | ❌ N/A    |

## Reporting a Vulnerability

如果你在本库的代码中发现安全漏洞——尤其是涉及加密原语误用、密钥泄漏路径、生物识别绕过、`FLAG_SECURE` 被绕过等场景——**请不要直接开 GitHub issue**（会让漏洞在修复前公开）。

请走以下任一渠道：

1. **首选**：通过 GitHub 的 [Private Vulnerability Reporting](https://github.com/visiongem/android-secure-toolkit/security/advisories/new) 提交报告
2. **备选**：发送邮件到 `visiongem@users.noreply.github.com`，标题前加 `[SECURITY]` 前缀

## What to Include

- 受影响的模块（`securestore` / `biometricvault` / `screensecure`）和版本号
- 复现步骤（最好附上最小可复现 Demo）
- 影响评估：能拿到什么信息 / 能做什么操作
- 你期望的修复方向（如果你已经有思路）

## Response SLA

| 阶段 | 时间 |
|---|---|
| 收到通知确认 | 72 小时内 |
| 影响评估反馈 | 7 天内 |
| 补丁发布 | 视严重程度，**严重漏洞 14 天内**，中等严重 30 天内 |

## Disclosure Policy

- 我会和报告者同步修复进度
- 修复发布后：在 GitHub Security Advisories 公开漏洞详情 + 修复 commit
- 默认会在 advisory 中署名报告者（除非你要求匿名）

## Out of Scope

下列情况**不**视为本库漏洞，请走对应上游渠道：

- AndroidKeyStore / BiometricPrompt 平台层漏洞 → 报 [Android Security Bulletin](https://source.android.com/docs/security/overview/acknowledgements)
- 厂商 ROM 对 `FLAG_SECURE` 实现不彻底（如最近任务卡片仍可截屏）→ 报对应 ROM 厂商
- 用户主动拍照 / 屏幕翻拍 / 物理观察 → 不在软件防御范围
- root 后用户主动 dump 内存 → 超出本库设计威胁模型

## Threat Model

本库设计的威胁模型假设：
- 设备**未** root
- 系统层面（Kernel、TEE、StrongBox）**未**被攻破
- 攻击者无法物理访问解锁状态的设备超过几秒
- 攻击者无 USB 调试 / Frida / Xposed 等运行时注入能力

超出此假设的攻击场景（设备物理被取且解锁、root 后内存 dump、定制恶意 ROM 替换 KeyStore 实现等），本库**不**提供防护——任何号称能防这些的纯软件方案都是营销话术，请务必审慎评估。

## Coordinated Disclosure

如果你的漏洞同时影响其他类似库（如 [androidx.security.crypto](https://developer.android.com/jetpack/androidx/releases/security)、Tink-Android），欢迎做协同披露。我可以协助通联其他维护者。
