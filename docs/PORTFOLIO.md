# Android Secure Toolkit — 作品集精读

> 这份文档不是"如何使用"——它是给招聘方 / 客户 / 协作方看的"作者怎么思考的"精读版。**用例 / API 在主 [README](../README.md)**；这里只解释**为什么这样设计**。

📦 **GitHub**: <https://github.com/visiongem/android-secure-toolkit>
🪪 **License**: Apache 2.0
📅 **公开发版**: 2026-05-09

---

## 一句话定位

把"Android 平台用户敏感数据保护"这件官方文档分散、StackOverflow 答案过时、生产环境真实事故频发的事，封装成 3 个**互不依赖**、**零反射**、**零 DI 框架依赖**的小库。

---

## 1. 我希望你从这个项目看到什么

| 维度 | 体现在 |
|---|---|
| 系统层 Android API 掌握度 | `AndroidKeyStore` / `BiometricPrompt` / `KeyGenParameterSpec` 三件套精确配置；`AUTH_BIOMETRIC_STRONG` 与锁屏密码 fallback 的取舍；StrongBox 检测降级 |
| 密码学工程素养 | NIST SP 800-38D 认证加密规范的落地；GCM 12-byte IV / 128-bit tag 选型理由；EVP-KDF 跨端兼容性分析（项目里没用，但博客里点出了它的局限） |
| 可读性优先的 API 设计 | 三种风格的 Result 表达：失败返回 `null` 的"安静模式" / 抛 `XxxException` 的"严格模式" / `sealed class VaultResult` 的"分支驱动模式"。**每种都明确说什么时候选什么** |
| 工程化基建 | Gradle KTS + Version Catalog + KSP + Compose + JitPack 完整发布链路；GitHub Actions CI；Dependabot 自动依赖升级；多模块独立可发布 |
| 文档与可维护性 | 每个模块 3 层文档（模块 README / docs/ 详细文档 / Kotlin KDoc）；CLAUDE.md 项目宪法（"硬约束"4 节让任何接手者立刻知道哪些是不可变的）；ROADMAP 明确"不在路线图上"避免读者期待落空 |
| 真机验证证据 | OnePlus 9 / OxygenOS / Android 14 三模块全部跑通；`:securestore` 10/10 instrumented test 在真 KeyStore 上绿；FLAG_SECURE 系统拦截 toast 实拍 |
| 商业合规意识 | LICENSE / NOTICE / SECURITY.md 三件套；威胁模型显式声明范围（不假装能防 root、不假装能防物理翻拍） |

---

## 2. 关键设计取舍

下面是 7 个我在这个项目上**主动**做的决定（不是被默认值带着走的）。每条都给出"备选方案"和"选当前这个的理由"——这才是工程取舍的真实形态。

### 2.1 不引入 Hilt / Koin

**备选**：用 Hilt 注入 KeyStore 实例、BiometricManager、ExecutorService。

**选当前**：所有依赖都通过 `Context` 参数 / `companion object` 取得；调用方爱用 Hilt / Koin / 手动 factory 都自由。

**理由**：库本身不应该绑死调用方的 DI 选择。一个 1KB 的安全工具引入 250KB 的 Hilt 是反模式。我把"DI 中立"作为公开 [设计原则](../CLAUDE.md#6-设计原则修改代码前先读)写入项目宪法。

### 2.2 公开 API 默认不抛异常

**备选**：所有 API 用 `@Throws` + 抛 `SecurityException` / `KeyStoreException`。

**选当前**：默认 `encrypt` / `decrypt` 失败返回 `null`；同时提供 `encryptOrThrow` / `decryptOrThrow` 给需要详细错误的调用方。

**理由**：金融 App 的加解密路径多在 ViewModel / 协程中，调用方往往用 `?.let` / `?:` 处理失败。如果库默认抛异常，**调用方一定会忘 try-catch**，crash 量上线后才发现。把"安静失败"作为默认是反直觉但符合现实的。

### 2.3 sealed class `VaultResult` 而非 nullable cipher

**备选**：`fun obtainCipher(...): Cipher?` —— null 表示失败。

**选当前**：`sealed class VaultResult { Cipher / KeyInvalidated / Unavailable / Failure }`。

**理由**：BiometricVault 的"失败"有 4 种本质不同语义：

- `KeyInvalidated`（用户增删了指纹，要走重设流程）
- `Unavailable`（设备无传感器，回退密码）
- `Failure`（其他异常，上报 + 降级）
- `Cipher`（success）

如果用 nullable，调用方拿到 null 时**根本无法决定**该走哪个分支。`sealed class` 强制 `when` 穷尽——编译器会在你忘了某个分支时报警。这是把"思考责任"前置到编译期。

### 2.4 配置不可调弱

**备选**：让调用方传入 keysize / IV size / GCM tag bits。

**选当前**：256-bit / 12-byte IV / 128-bit tag 全部硬编码常量。

**理由**：能让调用方覆盖配置的 API，等于给"未来某个不熟密码学的同事写出 AES-128 + 8-byte IV 配置"开了口子。把"默认安全"硬编码进去，**让弱配置成为不可能**——这是 Bruce Schneier 在 _Cryptography Engineering_ 里反复强调的"failure mode 对齐安全"。

### 2.5 加密 vs 解密路径的 KeyInvalidated 处理不对称

**备选**：通用一套异常处理（删 + 重建）。

**选当前**：

- `obtainEncryptCipher` 抛 `KeyPermanentlyInvalidatedException` 时**自动 deleteKey + 重建**（对加密路径，旧 key 无意义）
- `obtainDecryptCipher` 抛同样异常时**只返回 `KeyInvalidated`，不重建**（对解密路径，重建只会让旧密文永远救不回来）

**理由**：这是 90% 教程都不区分的细节，但在生产中会**永久丢失用户数据**。这层非对称性体现的是"加密语义 ≠ 解密语义"。我在 [docs/biometricvault.md](biometricvault.md) 的常见坑 #2 详细解释。

### 2.6 Compose `DisposableEffect` 而不是 `LaunchedEffect` 管 FLAG_SECURE

**备选**：`LaunchedEffect` + 自己处理 onDispose。

**选当前**：`DisposableEffect` 显式成对 setup/teardown。

**理由**：`FLAG_SECURE` 是窗口级标志位——离开页面**必须确定地**清掉，否则会"传染"到下个页面。`DisposableEffect` 是为这种"成对调用"设计的，语义比 `LaunchedEffect` 更明确。这种细微的 API 选型差异决定了"接手代码的下个工程师能不能 5 分钟看懂"。

### 2.7 三模块互不依赖

**备选**：抽出公共 `:common` 模块放共享密钥派生 / Base64 工具。

**选当前**：三模块独立可发布，每个都从零实现自己的算法路径，不共享代码。

**理由**：调用方往往只需要一个模块（比如只装 `:screensecure`）。如果有 `:common` 依赖，那个 50 行的工具模块会拖着 250 行无关代码。**重复 100 行 < 引入一个抽象层**——尤其是在工具库这种"被引入比引入更重要"的场景。

---

## 3. 工程化基建（不只是会写代码）

| 项 | 实现位置 |
|---|---|
| Gradle KTS（不用 Groovy） | 全部 build script |
| Version Catalog 集中管理依赖 | [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) |
| KSP（不用 kapt，Kotlin 2.x 现代选择） | 当前 0 注解处理器，如未来需要也用 KSP |
| JVM 17 toolchain（明确锁定） | 每个模块 `kotlin { jvmToolchain(17) }` |
| GitHub Actions CI | [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)：assemble + test + lint |
| Dependabot 周更 Gradle / 月更 Actions | [`.github/dependabot.yml`](../.github/dependabot.yml)，依赖按 androidx/compose/test 分组 |
| JitPack 多模块发版 | [`jitpack.yml`](../jitpack.yml) + 3 库的 `maven-publish` 块 |
| Sample app 演示三模块 | [`sample/`](../sample/)，单 Activity 全 Compose |
| Robolectric 单元测试 + Instrumented test | `:securestore` 已实现 10 用例，端到端在真 AndroidKeyStore 上绿 |
| 项目宪法（CLAUDE.md） | [`CLAUDE.md`](../CLAUDE.md) 第 4 节"硬约束" + 第 6 节"设计原则" |

---

## 4. 真机验证

> **Verified on**: OnePlus 9 (LE2113) / OxygenOS / Android 14
> **`:securestore` instrumented tests**: 10/10 通过（real AndroidKeyStore，覆盖 round-trip / IV 唯一性 / GCM tag 篡改 / unicode / 长字符串 / wrong alias / 生命周期）

证据见主仓库 README 的 [真机验证段](../README.md#真机验证)，含三张截图：

1. Sample app 主页（SecureStore + BiometricVault 加解密往返成功）
2. ScreenshotProtector 生效（OxygenOS 系统 toast 拦截截屏，实拍）
3. 10/10 instrumented test 终端输出

---

## 5. 关于 AI 协作的诚实说明

本项目在以下环节使用了 AI 协助：

- 把已有工程经验抽象为开源工具的**结构化整理**
- 跨平台兼容性的**事实核查**（如 OpenSSL EVP-KDF 在 CryptoJS / Android 上的等价性）
- 文档**润色与一致性检查**
- 测试用例**覆盖度补全**

AI **没有**参与的：

- 设计取舍（每条都是我主动决策）
- 算法选型（基于密码学常识 + 平台约束）
- 真机验证与调试
- 项目所有权与责任承担

我认为这是 2026 年合理的工程师工作模式：**AI 提速 60-70%，剩下 30-40% 仍是工程师独立判断**。把 AI 当工具明示出来，比"假装全部手写"更职业。

---

## 6. 你能在这个项目里看到我会做什么

如果你在考虑邀请我加入团队 / 接私活 / 长期合作，下面这些是我能确定带过去的：

- ✅ **从零起一个生产级 Android 库的全链路能力**：架构 → API 设计 → 测试 → 文档 → 发版 → 推广
- ✅ **系统层 Android 安全 API 的精细掌握**：能写出而非只会调用
- ✅ **密码学工程素养**：知道什么时候用 GCM、什么时候用 CBC、什么时候 KDF 选错导致整体方案失败
- ✅ **API 设计的反直觉权衡能力**：默认不抛异常 / 配置不可调弱 / 不引入主流 DI 这种"违背习惯"的判断
- ✅ **现代 Kotlin / Compose 工程实践**：KTS、Version Catalog、KSP、JVM toolchain、sealed class、`DisposableEffect`
- ✅ **完整的工程治理**：LICENSE、SECURITY、CONTRIBUTING、ROADMAP、CLAUDE.md 不是装饰，是协作工具
- ✅ **AI 协作的成熟度**：把 AI 用在该用的地方，明示边界，承担最终责任

---

## 7. 联系

- GitHub: [@visiongem](https://github.com/visiongem)
- 仓库: <https://github.com/visiongem/android-secure-toolkit>
- 漏洞披露: 见 [SECURITY.md](../SECURITY.md)

---

> 这份 PORTFOLIO 文档专为外部展示设计，欢迎转发到简历、个人主页、招聘平台。
