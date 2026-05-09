# 简历 / 个人主页项目描述模板

> 直接复制粘贴的文案，按字数与场景分两版。**先复制再改细节**——比如把 GitHub URL 换成你想露出的具体页（PORTFOLIO 还是主 README）。

---

## 一、300 字版（约 270 中文字符）

**用于**：传统 PDF 简历"项目经验"主条目、Boss 直聘"工作经历"详细段、个人作品集主条目、求职信附录、外包平台个人介绍。

```text
Android Secure Toolkit · 个人开源项目 · 2026.05

3 个独立可发布的 Android 安全库（securestore / biometricvault / screensecure），
覆盖金融/医疗类 App 对敏感数据保护的核心需求：AES-256-GCM Keystore 加密、强生
物识别绑定密钥（自动处理 KeyPermanentlyInvalidatedException）、Compose 一行
FLAG_SECURE 防截屏。

技术栈：Kotlin 2.0.21、Jetpack Compose、Gradle KTS + Version Catalog、
KSP、JVM 17、AndroidKeyStore、BiometricPrompt、JitPack 发版、Apache 2.0。

关键设计取舍：(1) 公开 API 默认不抛异常 + OrThrow 双形式，符合 ViewModel /
协程调用习惯；(2) BiometricVault 用 sealed class VaultResult 替代 nullable
Cipher，强制 4 分支编译期穷尽；(3) 算法/keysize/IV 全部硬编码，弱配置编译期
不可能；(4) 3 模块互不依赖，调用方按需装。

工程化：GitHub Actions CI、Dependabot 周更、10 个 instrumented test 在真
AndroidKeyStore 端到端通过、OnePlus 9 / Android 14 真机验证三模块全部生效。

仓库:  https://github.com/visiongem/android-secure-toolkit
精读:  docs/PORTFOLIO.md（设计取舍精读，给招聘方/客户看的版本）
```

### 几个变种小贴士

- **如果简历空间紧张**，删除"工程化"段，余下约 200 字
- **如果投递的是大厂资深岗**，把"关键设计取舍"前置（招聘方 30 秒能看到的位置）
- **如果投递的是创业公司**，把"3 模块互不依赖，调用方按需装"前置（暗示"轻量、不绑架技术栈"）

---

## 二、150 字版（约 150 中文字符）

**用于**：领英 About、GitHub 主页 bio、知乎/掘金作者简介、微信公众号介绍、私信开场白、推特/X 个人简介中文版。

```text
Android Secure Toolkit · 个人开源项目

3 个独立可发布的 Android 安全库，覆盖 AES-256-GCM Keystore 加密、强生物识别
绑定密钥、Compose 防截屏。Kotlin 2.0.21 + Jetpack Compose + KSP + Version
Catalog 现代栈；OnePlus 9 / Android 14 真机验证 + 10 instrumented test
端到端通过；GitHub Actions CI + JitPack 发版。亮点：sealed class API 设
计 + 算法硬编码避免弱配置 + 模块互不依赖。

🔗 github.com/visiongem/android-secure-toolkit
```

---

## 三、超短版（约 50 字，限 1 行）

**用于**：领英 Headline、推特/X bio 单行、邮件签名、名片项目栏。

```text
android-secure-toolkit · Kotlin/Compose 编写的 3 个 Android 安全工具库
（AES-GCM Keystore / 生物识别 / 防截屏）· github.com/visiongem/android-secure-toolkit
```

---

## 四、ATS 关键词清单（让简历过 HR 系统）

许多大厂 / 外企用 Applicant Tracking System (ATS) 自动过滤简历。简历里**至少出现一次**以下关键词，能让你过 ATS 第一轮筛选。如果某些词当前简历没有，建议补进项目描述或技术栈段：

### 必含（与本项目相关的高频招聘关键词）
- Android / Kotlin / Jetpack Compose / Android Studio
- AndroidKeyStore / BiometricPrompt / 生物识别 / Biometric
- AES-GCM / 加密 / Encryption / Cipher / Cryptography
- Gradle / KTS / Version Catalog / KSP
- JVM / JDK 17
- 单元测试 / Instrumented Test / 测试覆盖率
- CI/CD / GitHub Actions
- 开源 / Open Source / 代码审查 / Code Review

### 加分（体现资深度）
- 系统层 API / Platform API
- 协程 / Coroutine / Flow
- Sealed Class / Type-Safe API Design
- 安全编码 / Secure Coding / 威胁建模 / Threat Model
- ProGuard / R8 / 代码混淆
- 移动安全 / Mobile Security / OWASP MASVS
- 多模块架构 / Modularization

---

## 五、可放进简历的"硬数字"（量化成果）

招聘方喜欢看数字。本项目可以正经放进简历的硬数字：

| 数字 | 出处 |
|---|---|
| **3 个独立可发布模块** | securestore / biometricvault / screensecure |
| **10/10 instrumented test 通过** | 真 AndroidKeyStore 端到端验证 |
| **39 个源文件** | 仓库总文件数（不含构建产物） |
| **0 个传递依赖**（securestore） | 仅 Android 平台 API |
| **支持 minSdk 24 ~ targetSdk 36** | 全 13 年 Android 版本覆盖 |
| **6 个治理文件** | LICENSE / NOTICE / SECURITY / CONTRIBUTING / ROADMAP / CLAUDE.md |
| **3 张真机验证截图** | OnePlus 9 / OxygenOS / Android 14 |
| **2 段 NIST 公开规范引用** | SP 800-38D（GCM 模式）+ Android Keystore Provider |

---

## 六、面试时可能被追问的问题（提前准备）

如果简历上写了这个项目，面试官大概率问：

1. "你为什么不直接用 androidx.security.crypto?" → 答："1. EncryptedSharedPreferences 已弃用并停止维护；2. 它强依赖 Tink 又不暴露底层 Cipher；3. 这个库的 BiometricVault + ScreenSecure 是它根本不覆盖的部分。"
2. "为什么 securestore 失败返回 null 而不是抛异常?" → 答案在 [PORTFOLIO.md 第 2.2 节](PORTFOLIO.md#22-公开-api-默认不抛异常)
3. "你怎么测试 BiometricVault 的 KeyInvalidated 分支?" → 答："系统设置删/加指纹后跑 instrumented test。当前 :securestore 已 10/10 通过，:biometricvault 这个分支待补，是 ROADMAP v0.2 的明确项。"（**坦诚未做的事 > 假装都做了**）
4. "AI 在你这个项目里做了什么?" → 答案在 [PORTFOLIO.md 第 5 节](PORTFOLIO.md#5-关于-ai-协作的诚实说明)
5. "为什么不发到 Maven Central?" → 答："JitPack 起步成本低、迭代快；Maven Central 需要 GnuPG + Sonatype + JIRA 三步审核流程，规划在 v0.2 切换。"

---

## 七、避免的写法（反面教材）

❌ **"使用 Android 系统提供的 AES-GCM 加密"** —— 这是大学生项目级别描述，没体现取舍

❌ **"高性能、高可用、高安全的安全库"** —— 三个"高"字的形容词不传递信息

❌ **"基于 MVVM 架构"** —— 这是个**库**不是个 App，根本没有 MVVM 概念

❌ **"代码量 5000+ 行"** —— 行数本身不是优点，简洁更值得吹嘘（这个项目核心代码就 700 行）

❌ **"性能提升 50%"** —— 跟谁比？没基准就别写量化

❌ **"独立完成全部 39 个文件"** —— 在 AI 协作时代显得防御性过强；说"自主主导设计与实现，AI 协助文档梳理与测试用例覆盖"更准确

---

> 这份模板配合 [docs/PORTFOLIO.md](PORTFOLIO.md) 食用最佳——简历短描述钓住 HR 注意，PORTFOLIO 长文展开技术深度。
