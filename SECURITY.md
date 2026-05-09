# Security

如果你在本库代码中发现安全漏洞，**请不要直接开 GitHub issue**（漏洞会公开）。

请走 [GitHub Private Vulnerability Reporting](https://github.com/visiongem/android-secure-toolkit/security/advisories/new) 提交。

我会尽量在 7 天内响应。补丁发布后会在 GitHub Security Advisories 公开漏洞详情，并在 advisory 中署名报告者（除非你要求匿名）。

## 范围之外

下列情况**不**视为本库漏洞：

- AndroidKeyStore / BiometricPrompt 平台层漏洞 → 报 [Android Security Bulletin](https://source.android.com/docs/security/overview/acknowledgements)
- 厂商 ROM 对 `FLAG_SECURE` 实现不彻底 → 报对应 ROM 厂商
- 用户主动拍照 / 屏幕翻拍 → 不在软件防御范围
- root 后用户主动 dump 内存 → 超出本库威胁模型
