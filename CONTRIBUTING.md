# Contributing

欢迎 issue、PR、用法反馈。本仓库目前业余维护，但作者会尽量在工作日 48 小时内响应。

## 开发环境

| 工具 | 版本 |
|---|---|
| Android Studio | Ladybug Feature Drop (2024.2.2) 或更新 |
| JDK | 17 (Android Studio 内置 Embedded JDK 即可) |
| Android SDK | Platform 36 |
| Kotlin | 2.0.21（由 Version Catalog 锁定） |
| Gradle | 8.14（wrapper 已固定） |

```bash
git clone git@github.com:visiongem/android-secure-toolkit.git
cd android-secure-toolkit
./gradlew help            # 验证 wrapper
./gradlew :sample:installDebug   # 装 sample 到连接的设备
```

## 提交流程

1. **先开 issue 讨论**——尤其是新 API / 行为变更，避免你写完才发现方向不对
2. Fork + 在 feature branch 开发：`git checkout -b feat/your-feature`
3. 跑通本地验证：
   ```bash
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```
4. 真机相关变更：在 `docs/RUNNING_SAMPLE.md` 描述的设备矩阵上验证至少 1 个机型
5. 提 PR，描述里说清：**改了什么 / 为什么 / 怎么测的**
6. 通过 CI + review → merge

## 代码风格（强制）

跟随 [`CLAUDE.md`](CLAUDE.md) 第 4 节"硬约束"：

- ✅ Kotlin 2.x，不接受 Java 源文件
- ✅ Compose（UI 部分），不接受 XML Layout
- ✅ Gradle KTS，不接受 Groovy
- ✅ KSP，不接受 kapt
- ❌ 不引入 Hilt / Koin / RxJava / Dagger 任何 DI 框架（库本身要保持 DI 中立）
- ❌ 不放任何业务标识、第三方专有 SDK、商业逻辑

公开 API 设计原则：

- 失败默认返回 `null` 或 `sealed class` 分支，不抛异常
- 提供 `xxxOrThrow` 变体给需要详细错误的调用方
- 算法 / keysize / IV size **硬编码**——不允许调用方传弱配置

## 测试要求

| 改动类型 | 必需测试 |
|---|---|
| 修 bug | 加一个能复现 bug 的测试用例 |
| 新公开 API | 至少 1 个单元测试 + 1 个 instrumented test |
| 修文档 / 注释 | 不需要 |
| 调整 build script | CI 跑通即可 |

`:securestore` 端到端测试在 `securestore/src/androidTest/`，跑：
```bash
./gradlew :securestore:connectedDebugAndroidTest
```

## Commit 信息约定

跟随 [Conventional Commits](https://www.conventionalcommits.org/)：

```
feat:     新特性
fix:      bug 修复
docs:     仅文档
refactor: 不改变行为的重构
test:     仅测试相关
chore:    构建 / 工具链 / 杂项
```

**不要**在 commit 里加 `Co-Authored-By: Claude` / `with AI` 之类的签名行——这是项目作者的偏好。

## 不接受的 PR 类型

- 把代码"重写得更现代"但没具体 bug fix / 性能依据
- 引入新依赖来"避免造轮子"——**这个项目本身就是反造轮子的造轮子**
- 把硬编码常量改成可配置（违背"配置不可调弱"原则）
- 给 README / docs 加 emoji 装饰（项目作者偏好）

## 安全漏洞

请**不要**在公开 issue 里报漏洞。走 [SECURITY.md](SECURITY.md) 的私密披露流程。

## License

提交即视为同意你的代码以 [Apache 2.0](LICENSE) 许可发布。
