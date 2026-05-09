# Claude Code 项目交接文档

> 这是给"未来打开这个仓库的 Claude（即任何 Claude Code 会话）"看的项目说明书。Claude Code 会自动加载本文件作为项目级指令，所以请把它当作"项目身份证 + 协作守则 + 技术决策日志"三合一来读。
>
> 人类读者也欢迎，但下面有些段落是直接对 Claude 喊话的，请理解。

---

## 1. 项目身份

- **名字**：Android Secure Toolkit
- **包前缀**：`com.securetoolkit.*`（占位；用户提供 GitHub 用户名后可批量替换为 `io.github.<USERNAME>.*`）
- **License**：Apache 2.0
- **Maven 坐标占位**：`com.securetoolkit:securestore:0.1.0` 等（实际发布前再定）
- **起源**：作者基于过往生产环境中关于"用户敏感数据保护"的工程经验抽离的独立开源作品集
- **创建日期**：2026-05-08
- **当前版本**：0.1.0（MVP，未发版）

## 2. 长期愿景

为 Android 开发者提供"开箱即用、零外部依赖、零反射"的安全工具组合。3 个目标：

1. 让金融/医疗/企业 App 的"基础安全姿势"（落盘加密、生物识别、防截屏）从"自己研究 30 篇文章"降为"加 3 个依赖、写 3 行代码"
2. 成为作者展示"Android 平台/安全/工程化"能力的活样本（求职、接私活、写技术文章）
3. 一年内有 ≥1 个真实项目使用（自用 sample 不算）

## 3. 模块当前状态

| 模块 | 状态 | 一句话定位 | 关键文件 |
|---|---|---|---|
| `:securestore` | ✅ MVP | AES-256-GCM + AndroidKeyStore，加密任意敏感字符串落盘 | `securestore/src/main/kotlin/com/securetoolkit/securestore/SecureStore.kt` |
| `:biometricvault` | ✅ MVP | 强生物识别绑定 AES key，KeyPermanentlyInvalidated 自动处理 | `biometricvault/.../BiometricVault.kt` + `BiometricUiHelper.kt` |
| `:screensecure` | ✅ MVP | Compose 一行 `ScreenshotProtector()` 自动开关 FLAG_SECURE + setRecentsScreenshotEnabled | `screensecure/.../ScreenshotProtector.kt` |
| `:sample` | ✅ MVP | 单 Activity Compose Demo，演示三个模块的 happy path | `sample/.../MainActivity.kt` + `DemoScreen.kt` |

## 4. 技术栈与硬约束（这一节最重要，请逐条遵守）

**永远使用：**
- Kotlin 2.0.21（写代码）
- Jetpack Compose（写 UI）
- Gradle KTS（写构建脚本）
- Version Catalog（`gradle/libs.versions.toml`）
- KSP（如需注解处理）
- JVM 17 toolchain
- AGP 8.11.0
- minSdk 24 / targetSdk 36 / compileSdk 36

**永远不要使用（即使表面上是"小改动"）：**
- ❌ Java 源文件（`.java`）—— 这是用户明确要求摒弃的旧方式
- ❌ XML View / Layout（`res/layout/*.xml`）—— 一律 Compose
- ❌ kapt —— 一律 KSP
- ❌ Groovy `build.gradle` —— 一律 KTS
- ❌ findViewById / DataBinding / ViewBinding
- ❌ AsyncTask / Loader / Handler post 写业务（用 Coroutine + Flow）
- ❌ RxJava（库本身保持纯净；调用方爱用什么自己接）

**关于依赖注入**：库本身**不依赖任何 DI 框架**。调用方爱用 Hilt / Koin / 手动 factory 都可以——这是公开承诺，不要破坏。

**关于错误处理**：
- 公开 API 默认 *不抛异常*，失败返回 `null` 或 `sealed class` 的某个分支
- 需要详细错误时提供 `xxxOrThrow` 变体
- 用自定义 `XxxException` 而非 `RuntimeException`

## 5. 目录约定

```
android-secure-toolkit/
├── CLAUDE.md                 ← 你正在读的这份
├── README.md                 ← 给人类看的门面
├── LICENSE / NOTICE          ← Apache 2.0
├── settings.gradle.kts       ← include 4 个模块
├── build.gradle.kts          ← 仅声明 plugins，无业务
├── gradle.properties         ← 启用 parallel/caching/configuration-cache
├── gradle/libs.versions.toml ← 唯一版本号来源
├── gradle/wrapper/           ← Gradle 8.14（官方分发）
├── .github/workflows/ci.yml  ← assembleDebug + test + lint
├── securestore/              ← 库模块 1
├── biometricvault/           ← 库模块 2
├── screensecure/             ← 库模块 3（Compose）
└── sample/                   ← com.android.application 演示 app
```

每个库模块的标准结构：
```
moduleX/
├── build.gradle.kts          ← 仅 alias plugins + minSdk + namespace + jvmToolchain(17)
├── consumer-rules.pro        ← 透传给 app 端的 ProGuard 规则（多数为空）
└── src/
    ├── main/
    │   ├── AndroidManifest.xml  ← 仅声明权限，无 Activity
    │   └── kotlin/com/securetoolkit/<module>/*.kt
    └── test/kotlin/...        ← Robolectric 单元测试
```

## 6. 设计原则（修改代码前先读）

1. **公开 API 极简**：每个模块对外暴露 ≤2 个 object/class。内部细节一律 `internal`。
2. **不为"通用性"做抽象**：3 个模块不共享任何基类或接口。重复就重复。
3. **每个模块独立可发布**：不要在 `:securestore` 里 `implementation(project(":biometricvault"))`。
4. **配置不可调弱**：算法/keysize/IV size 全部硬编码常量，不接受调用方覆盖。
5. **文档即门面**：README 必须有"快速开始"代码 + 一句话价值主张；新增模块必同步更新表格。
6. **测试用 Robolectric 跑**：`unitTests.isIncludeAndroidResources = true`；端到端用例放 sample 的 `androidTest/`（待补）。

## 7. 长期演进规划

- **v0.1（当前）**：MVP，3 个模块的核心 API。
- **v0.2**：单元测试覆盖率 ≥70%；端到端 instrumented test；docs/ 目录补 3 篇详细文档。
- **v0.3**：Maven Central 发版（先走 JitPack 过渡）。
- **v0.4**：StrongBox 可用性诊断工具；密钥导出/导入辅助 API。
- **v0.5**：Compose Multiplatform 探索（仅 ScreenSecure 可能有意义）。
- **v1.0**：Sample App 上 Play Store；至少有 1 个真实第三方项目使用。

## 8. 给未来 Claude 会话的协作约定

- **打招呼那一刻就读这份文件**：里面的"硬约束"不要协商，"演进规划"可以讨论。
- **修改代码前**：先 `find . -name "*.kt"` 看看有没有违反硬约束的地方；如果有，*这就是你要修的 bug*。
- **新增功能时**：先问"是不是也属于这 3 个模块之一"。如果不是，开第 4 个模块前先和用户确认（守"模块互不依赖"原则）。
- **触发 build 之前**：把 `./gradlew help` 跑一遍确认 wrapper 工作。如果失败，多半是 `local.properties` 里 `sdk.dir` 没填——提醒用户。
- **不要写多余的注释 / 测试桩 / TODO 占位**：项目刚起步，垃圾代码会复利。
- **commit 信息**：用 `feat: ...` / `fix: ...` / `docs: ...` 前缀；不要带"with Claude"之类签名。
- **如果用户问"这个项目能干什么"**：先读 README，再回答；不要靠记忆。

## 9. 已知 TODO（按优先级）

### 已完成
- [x] ~~`YOUR_GITHUB_USERNAME` 已替换为 `visiongem`~~（2026-05-09）
- [x] ~~`LICENSE` / `NOTICE` 的 `[YOUR NAME]` 已设为 `visiongem`~~（如需用真名作版权署名可再改）
- [x] ~~`local.properties` 的 `sdk.dir` 已配~~（2026-05-08）
- [x] ~~跑通 `./gradlew assembleDebug`~~（2026-05-08，sample + 3 库全部 BUILD SUCCESSFUL）
- [x] ~~补 `docs/` 目录三份详细使用文档~~（2026-05-08）
- [x] ~~JitPack 发版配置~~（2026-05-08，`jitpack.yml` + 3 库的 `publishing` 块）
- [x] ~~git init + push v0.1.0 到 GitHub~~（2026-05-09，commit `101ad7c`）
- [x] ~~v0.1.0 tag 推送 + 触发 JitPack 首次构建~~（2026-05-09）
- [x] ~~README badges + GitHub URL + JitPack 安装段~~（2026-05-09）
- [x] ~~`docs/RUNNING_SAMPLE.md` 真机验证指引~~（2026-05-09）
- [x] ~~`docs/blog/PUBLISH_CHECKLIST.md` 博客发布清单~~（2026-05-09）

### 进行中 / 待办
- [ ] 真机验证 sample 三模块（按 `docs/RUNNING_SAMPLE.md` 跑一遍）
- [ ] `securestore` 跑 `./gradlew :securestore:connectedDebugAndroidTest`（10 用例端到端）
- [ ] 截两张关键截图（`docs/blog/PUBLISH_CHECKLIST.md` B 节）
- [ ] 博客首发（掘金）
- [ ] GitHub repo 启用 Discussions（在 Settings → Features）
- [ ] 给三个模块各自补 `README.md`（一句话定位 + 三段示例）—— 优先级低
- [ ] 切换到 Maven Central（v0.2+ 时，需 gnupg、sonatype 账号）—— 长期

## 10. 知识产权声明

本仓库为完全独立创作。所有代码基于：

- Android 平台公开 API（`Keystore`、`BiometricPrompt`、`FLAG_SECURE` 等）
- 公开标准与文档（NIST SP 800-38D、Android Developers 官方文档、AOSP 公开源码）
- 作者本人的工程经验抽象

不携带任何第三方专有标识、内部 SDK、私有协议、商业逻辑。如未来 Claude 在 review 时发现疑似第三方业务标识，**请视为 bug 并立即修复并通知作者**。

## 11. 与作者的"潜规则"

- 作者偏好简短回复，不要每次都列 1234 大纲
- 修改前先告诉用户"打算改 X 文件的 Y 位置"，得到确认再动；除非是显然的 typo
- 跑命令前先 echo 一下命令本身，让用户看到
- 出错时先解释**为什么**错，再给修复方案
- 不要往代码里塞 emoji，用户讨厌

---

*这份文件本身就是项目的一部分。修改它需要 PR。但通常只有"模块清单"、"TODO"、"演进规划"三节会变；其他 8 节是宪法级别的，不要轻易动。*
