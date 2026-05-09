# 未来仓库规划：第 2、3 个开源库的 idea backlog

> 这是 **idea backlog，不是承诺**。本仓库 (secure-toolkit) v0.2 完成 + 收到第一批真实使用反馈后才会启动下一个仓库。"先做精一个，再扩张多个" > "同时铺三个都做不完"。

---

## 一、决策框架：什么样的库值得做

启动一个新开源仓库前用这 4 条评估，全过才动手：

1. **稀缺性**：开源世界已有的同类方案是否真覆盖了痛点？如果有 ≥1 个高质量竞品（活跃维护、文档齐全、社区规模），**不要做**——除非你有明确差异化点
2. **受众清晰度**：能在一句话内讲清"谁会用这个"。如果只能描述为"做 Android 开发的人都可能用"——太宽，等于没受众
3. **可独立验证**：能在 1 个开发者周（约 1-2 周业余时间）做出可发版的 MVP，并且 MVP 自身有完整使用价值（而非"等到 v0.3 才有用"）
4. **作者亲历**：作者本人在过往工作中**真实**遇到过它解决的问题，能写出一手避坑笔记。**这一点比技术深度更重要**——决定了博客 / 文档的真实度

---

## 二、候选仓库 #2：`android-message-pipeline`

### 一句话

把"小通道传大消息 + 多客户端 + 跨传输介质"这件事抽象成 3 个 Kotlin 接口，让 Android 开发者再不用自己撸分片协议。

### 解决什么问题

Android 开发者经常遇到这种场景，每次都从零写分片+重组+异步分发：

| 场景 | 通道限制 | 常见错误 |
|---|---|---|
| BLE GATT Characteristic | MTU 默认 23B，最大 517B | 分片头格式不统一 / 接收侧组装时序混乱 |
| USB Accessory Bulk Transfer | 64KB 一包，但要可靠确认 | 无 ACK / 无超时清理 / 内存泄漏 |
| 二维码动图传文件 | 每帧 < 2KB，需要前端识别顺序 | 帧间无校验 / 用户切走再切回断流 |
| WebRTC DataChannel | 16KB / msg | reliable vs unreliable 没区分 |
| 串口 / NFC | 各自的 MTU 限制 | 重复造轮子 |

每个团队都自己写一套——**错漏百出**。

### 设计概要：3 个核心抽象

```kotlin
// 1. 抽象的消息管道
interface MessagePipeline<Req, Rsp> {
    fun send(req: Req): Flow<Rsp>
    fun close()
}

// 2. 编解码（业务消息 ↔ 字节流）
interface Codec<T> {
    fun encode(value: T): ByteArray
    fun decode(bytes: ByteArray): T
}

// 3. 分片 / 重组（字节流 ↔ MTU 限制下的物理帧）
interface Chunker {
    fun split(bytes: ByteArray, mtu: Int): Sequence<Frame>
    fun assemble(frames: Iterable<Frame>): ByteArray?  // null 表示尚不完整
}

// Transport（实际 IO）作为参数注入，与协议解耦
class DefaultMessagePipeline<Req, Rsp>(
    private val codec: Codec<Req>,
    private val chunker: Chunker,
    private val transport: Transport,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MessagePipeline<Req, Rsp>
```

### 模块拆分（5 个）

| 模块 | 内容 | 依赖 |
|---|---|---|
| `:pipeline-core` | 上述 3 个抽象 + `DefaultMessagePipeline` + 默认 Chunker（`[idx:total:hash:client]` 格式） | 仅 kotlinx.coroutines |
| `:pipeline-codec-json` | Gson Codec | gson |
| `:pipeline-codec-protobuf` | Protobuf Codec | protobuf-javalite |
| `:pipeline-transport-ble` | GATT Server + Client + 多 Central 隔离 | androidx.bluetooth |
| `:pipeline-transport-usb` | USB Accessory + Bulk Transfer 管道 | 仅 Android API |

调用方按需装。**`:pipeline-core` 单独可用**——拿来配自己的 transport 也行。

### 与已有方案对比

| 方案 | 优点 | 不足 | 我的差异化 |
|---|---|---|---|
| Nordic Android BLE Library | BLE 框架成熟 | 只解决 BLE 的低层 IO，不管消息分片 | 我覆盖"分片+重组+RPC 风格请求响应配对" |
| Apache Camel | 路由/集成模型完整 | 服务端思路，移动端太重 | 我面向移动端实际限制（小 MTU + 异构传输） |
| Tink | 加密原语 | 与通信无关 | 不重叠，可配合 |
| RxAndroidBle | RxJava 风格 BLE | RxJava 已不流行；不管消息分片 | 我用 Coroutine + Flow，原生 |
| 各团队自撸 | 贴合业务 | 错漏百出，无测试 | 我提供经过 instrumented test 验证的标准实现 |

### MVP 范围（v0.1）

- `:pipeline-core` 完整：抽象 + 默认 Chunker + 单元测试
- `:pipeline-codec-json` 可用
- `:pipeline-transport-ble` 跑通 1 个真机端到端 demo（手机 ↔ ESP32 BLE 设备发 1MB JSON）
- 文档：README + 设计文档 + 1 篇博客（题目候选："为什么你的 BLE 大文件传输总掉帧——一个被低估的 23 字节问题"）

**不在 MVP**：USB transport / Protobuf codec / 二维码 transport（按反馈补）

### 启动条件（必须全部满足）

- [ ] secure-toolkit 仓库 ≥ 50 GitHub star
- [ ] secure-toolkit 收到 ≥ 3 个真实用户 issue（说明真有人在用）
- [ ] 至少 1 位陌生开发者在 issue / 邮件中问过"BLE 大消息怎么传"或类似场景
- [ ] 我有 ≥ 2 周业余时间投入

### 风险

- **真机测试成本高**：BLE 测试需要至少 2 台 Android 设备 + 1 个 BLE 外设（ESP32 / Nordic dev kit）。我目前只有 OnePlus 9
- **协议设计的不可逆性**：分片头格式一旦发版，后续改动是 breaking change。MVP 前要花时间审 spec
- **Nordic 等专业方案的存在**：可能让"做这个"显得没必要——但 Nordic 解决的是 BLE 这一个传输，**这个库的价值在于"统一抽象 + 多传输"**

---

## 三、候选仓库 #3：`android-kotlin-utils`

### 一句话

Compose-first 时代的 Android Kotlin 扩展函数库，专攻"主流 Ktx 不覆盖 + 金融/企业场景特有"两类高频痛点。

### 解决什么问题

每个 Android 项目都会积累 200+ 私有扩展函数。常见公开库要么过时 (Anko 已弃用)、要么 Compose 支持弱 (Splitties 活跃度下降)、要么覆盖窄 (官方 Ktx)。**项目之间无法迁移这些扩展**——每次都从零造，每次都犯同样的边界错误。

### 模块拆分（4 个）

| 模块 | 内容 | 灵感来源 |
|---|---|---|
| `:utils-numeric` | `String.safeToLong()`（含范围/格式校验）、`hexToDec()`、`decToHex()`、`String.toBigDecimalSafe()` | 加密钱包项目里高频踩坑：金额溢出、Hex 字符串大小写不一 |
| `:utils-compose` | `Modifier.click(debounce = 300ms)`、`Modifier.ifThen { ... }`、`StatusBarPadding`、`collectAsStateWithLifecycle` 增强 | Compose 防抖、生命周期感知订阅、Insets 处理 |
| `:utils-lifecycle` | `Activity.collectLatestLifecycleFlow()`、`ViewModel.launchSafe { }`、生命周期感知的 BroadcastReceiver | repeatOnLifecycle 写起来繁琐，封装常见用法 |
| `:utils-string` | BIP39 词表前缀建议、敏感字段 masking（`String.maskMiddle()`）、CharSequence 安全清零 | 钱包/医疗 App 高频敏感字段处理 |

### 与已有方案对比

| 方案 | 状态 | 不足 | 我的差异化 |
|---|---|---|---|
| Splitties | 活跃度下降 | Compose 支持弱、Compose 时代设计风格滞后 | Compose-first |
| Anko | **已弃用** | View 时代产物 | / |
| androidx.core.ktx | 官方但保守 | 仅覆盖 Android 标准 API 的 ktx 化，不含业务场景常用 | 补金融/企业高频痛点 |
| 项目内部私有扩展 | 高度贴合 | 不可复用 | 提供经过测试的标准实现 |

### MVP 范围（v0.1）

仅 `:utils-numeric` + `:utils-compose` 两个模块。每个模块内部 ≤ 20 个函数。**宁少勿多**——扩展函数库的死法是堆量，每个都没人用。

### 启动条件

- [ ] secure-toolkit 用户在 issue 里问过"有没有配套的 utils 库"或类似（**有真实需求才做**）
- [ ] 至少 5 个 secure-toolkit 用户（按 JitPack 下载量估）
- [ ] 我有 ≥ 1 周业余时间

### 风险

- **受众广但竞品多**：扩展函数库门槛低，竞争激烈。差异化必须想清楚，否则会被 Splitties 等老牌库覆盖
- **维护成本指数上升**：扩展函数会持续被 issue 要求"再加一个"。**必须有清晰的纳入标准**（例如：①必须 ≥3 个项目都用过；②必须 ≥1 个测试；③不超过 20 行）
- **Kotlin 标准库可能内化**：今天的扩展函数明天可能进入 Kotlin stdlib（如 `String.toBooleanStrict()`）。要避开这种"快变化"区域

---

## 四、明确**不做**的方向（避免读者期待落空）

### ❌ 多链签名 / 加密钱包工具
**理由**：
- 受众极窄（只有真做钱包的开发者）
- Trust Wallet Core 已经是事实标准，做重复轮子无意义
- 涉及加密资产合规风险（MiCA / 各州 MTL / FinCEN 等）—— 个人开发者不应承担

### ❌ 助记词 / 私钥管理工具
**理由**：用户资产丢失 = 100% 责任。商业上不对称风险。如果真要做，加入有牌照的团队做。

### ❌ KYC / 实名认证 SDK
**理由**：法规重灾区。GDPR / 个保法 / CCPA 三方夹击，个人开源项目无法承担。

### ❌ DexGuard / Bangcle 替代
**理由**：商业产品壁垒高（10+ 年积累），个人开发不可能赶上。R8 + ProGuard 已经够大多数场景，差距由商业产品填补。

### ❌ AI / LLM Android 集成 SDK
**理由**：变化太快，开源库 6 个月就过时。这是大厂 MLOps 团队的事。

### ❌ "Android 全家桶" 通用模板项目
**理由**：每个团队的"标准模板"都不一样。GitHub 上已经有 100+ 这种项目，没一个真活下来。

---

## 五、启动顺序与决策矩阵

|  | message-pipeline | kotlin-utils |
|---|:-:|:-:|
| 稀缺性 | ⭐⭐⭐⭐ | ⭐⭐ |
| 受众清晰度 | ⭐⭐⭐⭐（明确"做 BLE/USB 设备的开发者"）| ⭐⭐（"Android 开发者"过宽）|
| 可独立验证 | ⭐⭐（需要外设硬件）| ⭐⭐⭐⭐⭐（纯软件）|
| 作者亲历 | ⭐⭐⭐⭐⭐（直接来自工作经验）| ⭐⭐⭐（部分来自经验，部分要补）|
| 商业潜力 | ⭐⭐⭐（IoT/物联网设备厂商有需求）| ⭐（开源习惯不付费）|
| **总分** | **18** | **15** |

**初步排序**：先 `message-pipeline`，后 `kotlin-utils`。

### 推迟启动的硬触发条件

只要下面任一**未**满足，就**不**启动新仓库：

- secure-toolkit v0.2 未发版
- secure-toolkit 没有任何来自陌生开发者的 issue
- 我个人时间不够每周投入 ≥ 5 小时
- 没有真机/外设测试条件（针对 message-pipeline）

---

## 六、与 secure-toolkit 的关系

这两个候选仓库**不依赖** secure-toolkit，也**不被** secure-toolkit 依赖。三者关系：

```
                           [开发者用户]
                                |
    ┌───────────────────────────┼───────────────────────────┐
    │                           │                           │
    ▼                           ▼                           ▼
secure-toolkit         message-pipeline               kotlin-utils
（敏感数据保护）         （消息管道传输）              （扩展函数库）
    │                           │                           │
    └─用 AES-GCM 包消息？──── 可组合 ─────调用 utils 的 hex 函数？─┘
```

三者**理论上可组合**（比如：用 `kotlin-utils` 的 hex 编解码 + `message-pipeline` 的分片传输 + `secure-toolkit` 的 AES-GCM 加密 = 一个端到端加密 BLE 通信栈），但**默认不强制依赖**——避免一个库被另一个库拖累。

---

## 七、撤回和重写的权利

如果在做 message-pipeline 的过程中发现：

- 某个开源方案突然活跃了（如 Nordic 推出官方消息分片协议）
- 真实使用场景比我想象的窄（实测发现 90% 的 BLE 应用根本不需要传 > 1KB）
- 我自己的工作场景不再需要这种工具

→ **直接砍掉这个规划**。本文档的存在是为了"用脑子先做一遍"，**不是签了卖身契**。

每年 1 月、7 月各 review 一次本文档。每次 review 都问：

1. 启动条件是否触发？
2. 候选项目是否还有意义？
3. 有没有新的候选项目需要加入？

---

*Last reviewed: 2026-05-09*
*Next scheduled review: 2026-07-01*
