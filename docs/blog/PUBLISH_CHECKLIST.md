# 博客发版 Checklist

> 把 [`01-android-biometric-pitfalls.md`](01-android-biometric-pitfalls.md) 从草稿推到对外发布的一次性步骤清单。完成后此文件可保留为模板用于后续博客。

---

## A. 内容润色（30-60 分钟）

- [ ] 把第一段从"模板腔"改成你**自己的故事**：1-2 句"我曾经踩过哪个坑导致 X 后果"。这是文章可信度最高 ROI 的改动
- [ ] 通读一遍：每个坑都问自己"读者看完能不能直接抄走代码？"如果不能，补一两行上下文
- [ ] 检查：每段代码是否能**独立**编译（不引用未在文中出现过的类型 / 函数）
- [ ] 检查：是否有非中性表达（如"前公司"、"前雇主"等）需要中立化—— 已经按 `CLAUDE.md` 第 10 节脱敏过，复查一次

## B. 视觉资产（30-45 分钟）

需要 **2 张关键截图**，建议尺寸 1080×2400（手机原生分辨率）：

- [ ] **截图 1：BiometricPrompt 弹窗**
  来源：跑 sample → 点 "Biometric Encrypt" → 弹窗出现时按 **Volume Down + Power** 截屏
  在文章："坑 5" 段最适合放这张图
- [ ] **截图 2：FLAG_SECURE 拦截截屏的系统提示**
  来源：sample → ScreenshotProtector 开启 → 按截屏组合键 → 截下"无法截图"那个系统 toast
  在文章："坑 8" 段插入

可选增强：

- [ ] 一张**架构图**（mermaid 渲染好截图嵌入 / 或直接写 mermaid 代码块——掘金 / 知乎 / Medium 支持度不同）
- [ ] 一个**短视频 / GIF**：截一段 sample 完整的 encrypt → decrypt → 截屏被拦截的 30 秒演示。可以传 GitHub release 或 imgur，文章嵌入。**这个互动率最高**。

## C. SEO + 可读性（10 分钟）

- [ ] 给文章敲定一个**主标题**（候选见草稿末尾），副标题里塞 1-2 个长尾关键词：
  - 主标候选：`Android 生物识别集成的 8 个真实坑（含可直接套用的代码）`
  - SEO 关键词：`Android Biometric`、`BiometricPrompt`、`KeyPermanentlyInvalidatedException`、`FLAG_SECURE`、`AndroidKeyStore`
- [ ] 第一段加 TL;DR 一句话：让没耐心的读者立刻知道是否值得读下去
- [ ] 文中至少 5 处插入开源库链接 <https://github.com/visiongem/android-secure-toolkit>（每出现一个对应模块代码就提一次）
- [ ] 文末加"求 star + issue + PR"号召

## D. 平台发布（每平台 10-20 分钟）

按**预期受众密度**排序：

### 1. 掘金（首发推荐，中文 Android 流量最大）

- [ ] 注册 / 登录 <https://juejin.cn>
- [ ] 标签：`Android`、`Kotlin`、`安全`、`Jetpack`
- [ ] 分类：`Android`
- [ ] 封面图：用截图 1 或一张高对比度图
- [ ] **绑定 GitHub 链接**到作者卡片，被点赞后能引流到 repo

### 2. 公众号（如果你有）

- [ ] 排版用 [秀米](https://xiumi.us/) 或 [墨滴](https://mdnice.com/) 把代码块加阴影
- [ ] 模板：开头 50 字摘要 → TL;DR → 8 个坑 → 文末加 GitHub 二维码

### 3. 知乎专栏（备选）

- [ ] 标题党"为什么你的 BiometricPrompt 在用户加了指纹后崩了？"系列更适合知乎
- [ ] 互动率：高，但流量不如掘金

### 4. Medium / dev.to（英文，扩国际受众）

- [ ] 标题改英文：`8 Real-World Pitfalls Integrating Android Biometric Authentication`
- [ ] 平台：Medium（关注 Android 的 publication 不少）/ dev.to（社区活跃）
- [ ] 这一步可以拖到中文版反响好之后再做

## E. 发布后 24 小时（推广）

- [ ] 转 Twitter/X 带 `#AndroidDev #Kotlin #Security` 标签 + 截图
- [ ] 转 Reddit `r/androiddev`（需要小心 self-promotion 规则——直接发文章链接可能被删，按格式：先在 comment 里讲背景再贴链）
- [ ] 转 V2EX `programmer` 节点（中文）
- [ ] 个人朋友圈 / Slack / Discord 工作群（如果有）
- [ ] 找 1-2 个 Android 圈大 V 私聊看能否转发（务必带库的 GitHub 链接，不光是文章）

## F. 24-72 小时反馈循环

- [ ] 监控 GitHub Issues / Discussions（在 repo Settings → Features 启用 Discussions）
- [ ] 监控 JitPack 下载量：<https://jitpack.io/com/github/visiongem/android-secure-toolkit/>
- [ ] 评论区高频问题加进文章末尾的 FAQ 段
- [ ] **如果有人提 issue 修代码 → 接住，不要拖**——头几个 issue 的响应速度决定了仓库给人的"活跃度"印象

## G. 后续运营（每周 / 每月）

- [ ] 第 1 周末看：文章阅读量 / repo star / 关注者增长
- [ ] 阅读量 < 500 但代码质量你确信 OK → 标题/平台问题，换一个角度重发
- [ ] 阅读量 > 5000 + 至少 5 个 star → 验证了"安全工具受众存在"，可以考虑做第 2 个仓库（参考之前讨论过的 message-pipeline / kotlin-utils）

---

## 写作过程的反人类陷阱（避免）

- ❌ 不要为了"看起来专业"加无关的"技术展望"段落——读者要的是能解决问题的代码
- ❌ 不要在文章开头说"在开始前，我们先了解 BiometricPrompt 的原理"——直接进坑
- ❌ 不要每个章节都尾部加"小结"——读者在视觉上会感到 8 倍重复
- ❌ 不要在 GitHub repo 里放过分美化的设计图（README badges 够了，不要 banner / mascot——独立开源库的"专业感"来自代码质量而非视觉）
- ✅ 文章长度控制在 2500-3500 字之间（手机滑屏 5-7 屏）。再长读完率断崖

---

## 草稿当前进度

- [x] 大纲完成（[`01-android-biometric-pitfalls.md`](01-android-biometric-pitfalls.md)）
- [x] `YOUR_USERNAME` 占位已替换为 `visiongem`
- [ ] 第一段加自己的故事
- [ ] 截图 1（BiometricPrompt 弹窗）
- [ ] 截图 2（截屏被拦截）
- [ ] 选定最终标题
- [ ] 掘金首发
- [ ] 24h 内同步至少 2 个其他平台

完成上面 8 个勾选 → 算正式发版。
