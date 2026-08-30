# Ikaros V2 系统概要设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 系统概要设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-30 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |

> 本文档定义 Ikaros V2 的系统级总体架构、子系统划分、核心技术边界、数据流、集成方式、安全模型、部署形态与演进原则。
>
> 本文档位于 PRD 与各子系统详细设计之间。PRD 回答“系统要做什么”，本文档回答“整个系统总体如何组织”，各子系统设计进一步回答“某个领域内部如何设计”。
>
> V2 是一次整体重构。V1 的代码、数据库表、API、目录结构和历史实现仅作为经验参考，不构成兼容性约束。

---

## 1. 文档目标与设计范围

Ikaros V2 不再只是一个围绕动画与本地媒体构建的内容管理系统，而是一个面向个人数字内容、知识、创作、效率、协作与自动化场景的自托管平台。

系统概要设计需要解决以下问题：

1. Ikaros V2 在逻辑上由哪些层和子系统组成。
2. 各子系统之间如何保持边界，并进行查询、命令、事件和自动化联动。
3. Resource、Attachment、Blob、Collection、Relation 等平台核心概念如何贯穿不同业务领域。
4. PostgreSQL、对象存储、缓存、搜索索引和分析数据分别承担什么职责。
5. Web、桌面端、移动端和第三方客户端如何访问同一套平台能力。
6. 后台任务、定时任务、插件、自动化与 AI 如何接入核心业务，而不绕过权限与领域规则。
7. 普通业务数据与高敏感 Secure Domain 数据如何使用不同安全边界。
8. 单机自托管部署如何保持简单，同时为后续扩展 Worker、多节点和外部基础设施保留演进空间。
9. 系统如何处理一致性、幂等、失败重试、审计、备份、恢复和可观测性。
10. 后续详细设计应该遵循哪些统一约束。

本文档不提前固定：

- 具体 Java 包名与类名；
- 最终数据库表名；
- 所有 REST API 的精确路径；
- 每个页面的 UI 结构；
- 每个算法的最终实现；
- 每个插件扩展点的最终 Java 接口签名。

这些内容应在领域详细设计、数据库设计、API 设计和交互设计中继续展开。

---

## 2. 总体设计目标

### 2.1 自托管优先

Ikaros V2 的主要运行模式仍然是用户自行部署。

系统必须优先支持：

- 单机 Docker / Docker Compose；
- 家庭服务器；
- NAS；
- 普通 Linux 服务器；
- 外置 PostgreSQL；
- 本地文件系统或对象存储；
- 反向代理后的 HTTPS 访问。

不能为了理论上的大规模分布式能力，使最常见的单机部署依赖大量基础设施组件。

### 2.2 数据自主

用户的 Resource、Attachment、Blob、文档、媒体、个人状态、创作数据与配置应由用户自己的 Ikaros 实例控制。

第三方服务只能作为：

- 元数据来源；
- 外部身份来源；
- 外部存储；
- AI Provider；
- 通知渠道；
- 插件集成；
- 自动化目标；
- 导入导出来源。

第三方平台不能成为 Ikaros 内部资源身份或业务状态的唯一真相源。

### 2.3 统一平台，专业体验

V2 使用统一 Resource 身份和通用平台能力，但不能把所有内容类型压缩成一套没有业务语义的通用 JSON 模型。

统一的是：

- 身份；
- 权限；
- 标签；
- 收藏；
- Collection；
- Relation；
- Attachment；
- 搜索；
- 分享；
- Activity；
- 生命周期；
- 通知；
- 自动化；
- 审计。

专业化的是：

- 动画与影视的作品、季、剧集、字幕、播放；
- 漫画的卷、章节、页和阅读模式；
- 小说与电子书的章节、排版和阅读进度；
- 音乐的艺术家、专辑、曲目和播放队列；
- 图片的相册、EXIF、预览与原图；
- 文档的 Revision、协作和编辑；
- Productivity 的 Task、Goal、Project、OKR；
- Accounting 的 Ledger、Account、Transaction、Budget；
- Password Manager 与 Private Notes 的高敏感安全模型。

### 2.4 HTTP-first / HTTP-native

系统能力优先通过稳定的 HTTP API 暴露。

官方 Web、Flutter 客户端、插件、脚本和第三方客户端原则上都通过同一套公开能力访问系统。

实时协作、Room、播放同步、长时间任务进度等场景可以使用：

- WebSocket；
- SSE；
- WebRTC；
- 其他适合的实时协议。

但不能形成只有官方客户端才能调用的隐藏业务通道。

### 2.5 模块化单体优先

V2 初期采用：

> **模块化单体（Modular Monolith）作为主要服务形态，明确子系统边界，避免提前微服务化。**

逻辑上各子系统必须保持独立边界；部署上可以先运行在一个 Server 进程中。

这样可以同时获得：

- 自托管部署简单；
- 本地开发简单；
- 统一事务仍可在单一领域内部使用；
- 减少服务发现、分布式事务、消息中间件和运维成本；
- 通过公开契约保持未来可拆分能力。

后续若某些能力需要独立扩展，例如：

- 视频转码 Worker；
- OCR / AI Worker；
- 大规模索引任务；
- 归档恢复 Worker；
- 异步导入 Worker；

可以在不改变领域所有权的前提下拆出独立执行进程。

### 2.6 PostgreSQL-first

V2 的核心关系数据只面向 PostgreSQL 设计。

PostgreSQL 作为普通业务状态的主要真相源，允许合理使用：

- 事务；
- 外键；
- 唯一约束；
- JSONB；
- 数组；
- 全文检索；
- advisory lock；
- LISTEN / NOTIFY；
- CTE；
- 窗口函数；
- 部分索引；
- 表达式索引。

但任何高级能力都必须服务于清晰的领域约束，而不能为了技术炫技增加不可维护性。

---

## 3. 系统上下文

Ikaros V2 的外部参与者和系统边界如下：

```mermaid
flowchart LR
    USER[用户]
    ADMIN[管理员]
    WEB[CMS / Web]
    APP[桌面端 / 移动端]
    THIRD[第三方客户端]
    PROVIDER[第三方服务 / Provider]
    STORAGE[本地存储 / NAS / 对象存储]
    AI[AI 模型服务]

    USER --> WEB
    USER --> APP
    ADMIN --> WEB
    THIRD --> API[Ikaros HTTP / Realtime API]
    WEB --> API
    APP --> API

    API --> CORE[Ikaros V2 Server]
    CORE --> STORAGE
    CORE --> PROVIDER
    CORE --> AI
```

### 3.1 系统内部负责

Ikaros Server 负责：

- 业务规则；
- 身份与权限；
- Resource 与 Attachment 管理；
- 存储编排；
- 搜索与索引；
- 插件；
- 自动化；
- 后台任务；
- 协作与 Room；
- 通知；
- 分析；
- 安全；
- 管理与运维。

### 3.2 客户端负责

客户端负责：

- 展示和交互；
- 本地状态与体验优化；
- 媒体播放；
- 漫画、小说、文档阅读；
- 部分离线缓存；
- Secure Domain 中需要客户端持有密钥时的本地解锁与解密；
- 设备能力接入。

客户端不得成为跨设备业务状态的唯一真相源。

---

## 4. 总体逻辑架构

```mermaid
flowchart TB
    subgraph Clients[客户端层]
        CMS[CMS / Web Console]
        FLUTTER[Flutter App]
        EXTCLIENT[第三方客户端]
    end

    subgraph Interface[接口层]
        HTTP[HTTP API]
        RT[WebSocket / SSE / Realtime]
        OPENAPI[OpenAPI / API Contract]
    end

    subgraph Application[应用与平台层]
        SECURITY[身份 / 权限 / 安全]
        RESOURCE[Resource / Collection / Relation]
        CONTENT[媒体 / 阅读 / 音乐 / 图片 / 创作]
        PRODUCTIVITY[效率规划]
        FINANCE[个人财务]
        SECUREAPP[Private Notes / Password Manager]
        COLLAB[分享 / 协作 / Room]
        SEARCH[搜索与发现]
        AIINTEL[AI 智能增强]
        ANALYTICS[统计与分析]
        ADMINOPS[平台管理与运维]
    end

    subgraph Integration[平台联动层]
        CAP[Capability]
        CMD[Command]
        EVT[Event]
        REL[Relation]
        AUTO[Automation]
        ACT[Activity]
        CTX[Context]
    end

    subgraph Common[通用平台能力]
        TASK[Background Task]
        NOTIFY[Notification]
        AUDIT[Audit]
        PLUGIN[Plugin Runtime]
        STORAGECORE[Attachment / Blob / Storage]
        SECURE[Secure Data Foundation]
    end

    subgraph Infra[基础设施层]
        PG[(PostgreSQL)]
        CACHE[(Redis 可选)]
        INDEX[(Search Index)]
        BLOB[(Filesystem / S3 / NAS)]
    end

    Clients --> Interface
    Interface --> Application
    Application --> Integration
    Application --> Common
    Integration --> Common
    Common --> Infra
```

系统逻辑上分为六层：

1. **客户端层**：Web、Flutter、多端与第三方客户端。
2. **接口层**：HTTP、实时协议、OpenAPI 与外部契约。
3. **业务与平台子系统层**：承载领域规则和业务真相。
4. **平台联动层**：负责跨领域稳定协作。
5. **通用平台能力层**：存储、后台任务、通知、插件、安全基础等共享能力。
6. **基础设施层**：PostgreSQL、缓存、索引和物理存储。

---

## 5. 子系统划分

### 5.1 核心内容资源子系统

核心内容资源子系统负责 Resource 的统一身份与通用组织能力。

主要对象包括：

- Resource；
- Resource Type；
- Title / Alias；
- Metadata；
- Metadata Provenance；
- External Identity；
- Collection；
- Tag；
- Relation；
- Lifecycle；
- User State；
- Favorite；
- Rating；
- Progress；
- History。

该子系统回答：

> “这是什么内容，它如何被组织，它和其他对象是什么关系？”

它不直接承担 Blob 字节存储。

### 5.2 媒体子系统

负责：

- 动画；
- 电视剧；
- 电影；
- 通用视频；
- Season / Episode；
- 可播放版本；
- 音轨；
- 字幕；
- 播放历史；
- 播放进度；
- 派生媒体；
- 转码与媒体分析任务。

媒体子系统拥有媒体领域规则，但实际内容仍通过 Attachment / Blob Storage 管理。

### 5.3 阅读子系统

负责：

- 漫画作品；
- 卷；
- 章节；
- 页面；
- 小说；
- 电子书；
- 阅读布局；
- 阅读方向；
- 阅读进度；
- 继续阅读。

### 5.4 音乐子系统

负责：

- Artist；
- Album；
- Track；
- 音频版本；
- 播放队列；
- 播放历史；
- 播放进度；
- 歌词与封面。

### 5.5 图片与相册子系统

负责：

- 图片 Resource；
- Album / Collection；
- EXIF 与媒体信息；
- 缩略图；
- 预览图；
- 原图访问；
- 图片派生版本；
- 时间、地点和标签组织。

### 5.6 内容创作、文章与文档子系统

负责：

- Article；
- Blog Post；
- Column；
- Document；
- Revision；
- Draft；
- Publish State；
- 文档附件；
- 版本历史；
- 协作者；
- 内容编辑。

普通文档与 Private Notes 必须明确分离。

普通 Document 可以使用服务器端正常持久化、搜索和协作能力；Private Notes 属于 Secure Domain，采用更严格的密文边界。

### 5.7 Productivity / Planning 子系统

负责个人效率与计划场景：

- Task；
- Project；
- Goal；
- Milestone；
- Habit；
- OKR；
- Calendar Context；
- Reminder；
- Progress；
- Review。

Productivity Task 是用户业务对象，不得与系统 Background Task 或 Scheduled Job 混为一谈。

### 5.8 Personal Finance / Accounting 子系统

负责：

- Account；
- Ledger；
- Transaction；
- Category；
- Budget；
- Asset；
- Liability；
- Statement；
- Reconciliation；
- Report。

普通账本数据默认仍属于常规业务数据，以支持查询、统计和报表。

银行 Token、Secret 等敏感凭据必须通过受保护的 Secret Reference / Password Manager 管理，而不是作为普通配置明文保存。

### 5.9 Private Notes 子系统

Private Notes 属于 Secure Domain。

其核心目标不是普通文档编辑，而是：

- 服务端无法默认看到内容明文；
- 持久化副本保持密文；
- 支持安全解锁；
- 支持密钥版本；
- 支持安全备份与恢复；
- 禁止普通搜索索引泄露明文。

### 5.10 Password Manager 子系统

Password Manager 属于最高敏感级别之一，主要保存：

- 登录凭据；
- Secret；
- Secure Item；
- API Key；
- Token；
- 受保护字段；
- 安全附件。

该子系统必须通过 Security Subsystem 与 Secure Data Foundation 使用受控密码学能力。

### 5.11 分享、协作与 Room 子系统

负责：

- Share；
- Share Token；
- ACL；
- 协作者；
- 评论与协作上下文；
- Room；
- Room Member；
- Room Event；
- 播放状态同步；
- 临时共享队列。

Room 主要同步状态、成员和事件，不默认充当媒体转发服务器。

### 5.12 搜索与发现子系统

负责：

- Resource 检索；
- 多标题检索；
- Metadata 检索；
- Tag / Collection 检索；
- 文档全文检索；
- 搜索建议；
- 排序与过滤；
- 语义检索扩展；
- 索引重建；
- 权限感知搜索。

搜索索引是派生数据，不是业务真相源。

### 5.13 AI 智能增强子系统

AI 是横向平台能力，不是独立聊天孤岛。

负责：

- 模型 Provider；
- Model Registry；
- Prompt / Tool 管理；
- Embedding；
- RAG；
- 多模态理解；
- 元数据候选生成；
- 摘要；
- 创作辅助；
- 自然语言搜索；
- Agent；
- 智能自动化。

AI 只能调用已有 Capability / Command 完成业务动作，不能绕过业务校验直接写数据库。

### 5.14 Data Analytics / Statistics 子系统

负责统一事实、聚合、指标、趋势和报表。

数据流原则：

```text
业务事实
  ↓
Event / Activity / Snapshot
  ↓
Fact
  ↓
Aggregate
  ↓
Metric
  ↓
Dashboard / Report
```

Analytics 数据属于派生数据，不能反过来成为业务状态真相源。

### 5.15 Platform Administration / Operations 子系统

负责：

- User；
- Role；
- Permission；
- Session；
- Parameter；
- Dictionary；
- Menu；
- Announcement；
- Notification；
- Operation Log；
- Login Log；
- Security Event；
- Scheduled Job；
- Job Run；
- Health；
- Metrics；
- Alert；
- 在线用户。

菜单仅是导航展示，不是权限源。

### 5.16 Plugin 子系统

插件用于扩展平台，而不是获得无边界的内部数据库访问权。

插件可能扩展：

- Metadata Provider；
- 内容导入器；
- Storage Provider；
- Notification Provider；
- AI Provider；
- Automation Trigger / Action；
- Search Enricher；
- External Identity Provider；
- Parser；
- Background Task Handler。

插件执行任何业务修改时仍需进入目标子系统的公开 Command / Capability。

---

## 6. 平台核心模型

### 6.1 Resource

Resource 表达逻辑内容身份。

Resource 的职责是：

> 表达“这是什么”。

Resource 不负责表达物理字节当前放在哪个磁盘或对象存储桶。

### 6.2 Attachment

Attachment 是 Resource 可关联的业务内容对象。

例如：

- 视频；
- 音频；
- 字幕；
- 封面；
- 漫画页；
- 电子书；
- 文档附件；
- 压缩包；
- 导出包。

Attachment 表达业务用途、媒体类型、来源与关系。

### 6.3 Blob

Blob 表达实际内容及其内容身份。

Blob 通过摘要、大小和完整性信息支持：

- 内容寻址；
- 完整性校验；
- 去重；
- 多副本；
- 迁移；
- 归档；
- 恢复。

### 6.4 Blob Placement / Replica

Blob Placement 表示一个 Blob 的具体物理位置。

```text
Resource
  ↓
Attachment
  ↓
Blob
  ├── 本地文件系统副本
  ├── NAS 副本
  ├── S3 热存储副本
  └── 冷归档副本
```

一个 Blob 可以同时拥有多个副本。

### 6.5 Collection

Collection 用于逻辑组织 Resource，可以表达：

- 媒体库；
- 播放列表；
- 阅读列表；
- 收藏夹；
- 专题；
- 用户自定义集合；
- 文件夹式组织。

### 6.6 Relation

Relation 用于表达长期、明确、可查询的对象关系。

例如：

- Episode 属于 Series；
- Chapter 属于 Book；
- Resource 是另一个 Resource 的前传；
- Attachment 是 Resource 的封面；
- Derived Attachment 来源于 Original Attachment；
- Task 与 Resource 相关；
- Room 当前上下文为某 Resource。

Relation 必须有明确类型，不能只保存自由文本。

### 6.7 External Identity

External Identity 用于将内部 Resource 与第三方平台映射。

```text
Resource
  ├── Bangumi / 12345
  ├── TMDB / 67890
  └── Provider-X / abc
```

第三方 ID 永远不是 Ikaros Resource 的内部主键。

### 6.8 Metadata Provenance

重要元数据必须能够追踪来源：

- 用户人工输入；
- 文件扫描；
- 导入任务；
- Metadata Provider；
- 插件；
- AI；
- 系统生成。

用户明确确认或修改的数据默认具有更高优先级，自动同步不得无提示覆盖。

---

## 7. 子系统边界与联动模型

V2 的核心规则是：

> **一个子系统拥有自己的业务状态，其他子系统不能绕过它直接修改其内部数据。**

跨系统联动统一使用以下模型。

| 模型 | 用途 |
|---|---|
| Capability | 同步查询当前必须得到的结果 |
| Command | 请求目标子系统执行状态变更 |
| Event | 广播“某件事已经发生” |
| Relation | 保存长期对象关系 |
| Automation | 根据触发条件编排跨系统动作 |
| Activity | 保存用户或系统行为时间线 |
| Context | 表达对象当前所处的跨系统上下文 |

### 7.1 Capability

用于同步读取或校验。

例如：

- 查询当前用户是否可以读取某 Resource；
- 查询某 Blob 是否存在可用副本；
- 查询当前 Room 成员；
- 查询 Resource 基础摘要。

### 7.2 Command

用于请求目标子系统改变状态。

例如：

```text
Automation
  ↓
CompleteTaskCommand
  ↓
Productivity
  ↓
校验
  ↓
Task Completed
```

调用者不能直接修改 Productivity 的 Task 表。

### 7.3 Event

用于传播已经发生的事实。

例如：

- ResourceCreated；
- AttachmentImported；
- BlobReplicaLost；
- TaskCompleted；
- DocumentPublished；
- RoomCreated；
- UserLoggedIn。

Event 消费者不得假定只有自己一个消费者。

### 7.4 可靠事件

关键 Event 不能依赖“进程内发布成功就算成功”。

对于影响以下场景的事件：

- 搜索索引；
- Analytics；
- Automation；
- Notification；
- Storage 迁移；
- 审计；

应使用数据库事务内记录的 Outbox / Durable Event 机制，确保业务提交成功后事件不会因进程崩溃而静默丢失。

推荐流程：

```text
业务事务
  ├── 更新业务状态
  └── 写入 Outbox Event
        ↓
事件分发器
        ↓
消费者
        ↓
幂等处理
```

### 7.5 最终一致性

跨子系统默认采用最终一致性。

例如：

```text
Resource 修改完成
  ↓
立即返回成功
  ↓
异步更新 Search Index
  ↓
异步更新 Analytics
  ↓
异步触发 Automation
```

不应为了让搜索、统计、通知和自动化在同一个数据库事务中完成，而建立一个跨全系统的大事务。

---

## 8. 数据架构

### 8.1 数据分层

Ikaros V2 的数据按职责分为：

```text
业务真相数据
  → PostgreSQL

内容字节
  → Blob Storage

缓存数据
  → 内存 / Redis / 本地缓存

搜索派生数据
  → Search Index

统计派生数据
  → Analytics Fact / Aggregate

高敏感数据
  → Secure Data Foundation 管理的密文
```

### 8.2 PostgreSQL

PostgreSQL 保存：

- Resource；
- Collection；
- Relation；
- Attachment 元数据；
- Blob 元数据；
- Placement 状态；
- 用户；
- 权限；
- 业务状态；
- Task；
- Job；
- Event Outbox；
- Audit；
- Plugin Registry；
- Automation Rule；
- 其他普通领域数据。

### 8.3 Blob Storage

Blob Storage 可以由不同 Provider 实现：

- Server Local Filesystem；
- NAS；
- S3 Compatible Object Storage；
- 远程对象存储；
- 冷归档 Provider。

业务模块不得把一个物理路径当作 Attachment 的永久身份。

### 8.4 Redis

Redis 不是强制依赖。

可用于：

- 热点缓存；
- 短期 Session 辅助；
- 分布式锁；
- Rate Limit；
- 临时状态；
- 实时协作加速；
- 多实例消息辅助。

任何必须长期保存的数据都不能只存在 Redis。

### 8.5 Search Index

Search Index 保存用于快速检索的派生文档。

索引内容必须可从业务数据重建。

当索引损坏时：

```text
业务数据仍然正确
  ↓
执行 Rebuild Index
  ↓
重新生成索引
```

### 8.6 Analytics 数据

Fact、Aggregate、Metric 结果用于统计与报表。

聚合结果必须具有：

- 数据来源；
- 指标定义；
- 时间语义；
- 版本；
- 可重建路径。

---

## 9. Attachment 与存储总体流程

### 9.1 导入流程

推荐通用导入流程：

```mermaid
flowchart LR
    SOURCE[文件 / NAS / URL / Plugin]
    INGEST[Ingest]
    HASH[摘要 / 完整性识别]
    BLOB[Blob]
    PLACE[Placement]
    ATTACH[Attachment]
    RESOURCE[Resource]
    META[Metadata Enrichment]
    INDEX[Search Index]

    SOURCE --> INGEST
    INGEST --> HASH
    HASH --> BLOB
    BLOB --> PLACE
    BLOB --> ATTACH
    ATTACH --> RESOURCE
    RESOURCE --> META
    META --> INDEX
```

### 9.2 去重

去重基于 Blob 内容身份，而不是文件名或路径。

两个不同 Attachment 可以引用同一个 Blob。

### 9.3 派生内容

转码、缩略图、OCR、封面提取、波形、格式转换等生成的新内容必须创建 Derived Attachment，并保留来源关系。

```text
Original Attachment
  ↓ derives
Derived Attachment
```

派生内容可重建，因此其生命周期策略可以与原始内容不同。

### 9.4 可用性状态

系统需要向客户端提供统一可理解的可用性状态，例如：

- Available；
- Cached；
- Remote；
- Processing；
- Restoring；
- Missing；
- Corrupted。

客户端不能只得到一个模糊的“404”或“播放失败”，而应能解释内容为什么当前不可用。

---

## 10. API 总体设计

### 10.1 API 是产品能力边界

API 不应简单镜像数据库 CRUD。

例如：

错误方向：

```text
PATCH /task/{id}
body: { status: COMPLETED }
```

如果完成任务包含业务规则，更合适的是表达业务动作：

```text
Complete Task Command
```

REST 路径和 HTTP 方法在 API 详细设计中确定，但必须保留业务语义。

### 10.2 API 统一原则

API 应统一：

- 身份认证；
- Permission 检查；
- Resource ACL；
- Pagination；
- Sort；
- Filter；
- Error Model；
- Request ID；
- Trace ID；
- Idempotency Key；
- 版本策略；
- 审计上下文。

### 10.3 幂等

以下类型接口必须考虑幂等：

- 导入；
- 上传完成；
- Webhook；
- Automation Action；
- 外部同步；
- 支付或账务导入；
- 重试型 Background Task；
- Event Consumer。

### 10.4 实时接口

实时协议用于：

- Room；
- 文档协作；
- Task 进度；
- 通知推送；
- 在线状态；
- 系统健康更新。

实时协议只是传输方式，业务权限规则仍由目标子系统负责。

### 10.5 OpenAPI

核心 HTTP API 应生成或维护统一 OpenAPI Contract，供：

- CMS；
- Flutter App；
- SDK；
- Plugin；
- 第三方开发者；
- 自动化测试。

---

## 11. 后台执行体系

V2 中至少存在三种“任务”概念，必须严格区分。

### 11.1 Productivity Task

用户的待办、计划或目标任务。

### 11.2 Background Task

系统一次异步执行实例，例如：

- 导入；
- 转码；
- OCR；
- 索引重建；
- 数据迁移；
- Blob 校验；
- 备份；
- 恢复；
- AI 批处理。

Background Task 应具备：

- 状态；
- 进度；
- 创建者；
- 参数摘要；
- 结果摘要；
- 错误；
- 重试；
- 取消；
- 超时；
- 日志关联；
- 幂等键。

### 11.3 Scheduled Job

定义“什么时候自动启动某个系统动作”。

例如：

- 每天校验 Storage；
- 每晚备份；
- 定期同步元数据；
- 定期清理缓存；
- 定期执行统计聚合。

Scheduled Job 触发后通常产生一个 Background Task / Job Run。

### 11.4 Worker 演进

初期 Background Task 可以在主 Server 中执行。

资源消耗大的任务必须允许后续迁移到独立 Worker：

```text
Ikaros Server
  ↓ 创建任务
PostgreSQL / Queue Contract
  ↓
Worker
  ↓ 执行
更新 Task 状态
```

Worker 不能因此获得绕过业务权限和状态机的数据库写入特权。

---

## 12. 搜索总体设计

### 12.1 搜索对象

统一搜索可以覆盖：

- Resource；
- 标题与别名；
- Description；
- Tag；
- Collection；
- External Identity；
- Document；
- Article；
- Metadata；
- 允许索引的 Attachment 文本；
- AI Embedding。

### 12.2 权限感知

搜索结果不得泄露当前用户无权访问的对象。

原则：

> 搜索可见集合不得大于普通 API 可见集合。

### 12.3 索引一致性

搜索使用异步索引，因此允许短暂最终一致性。

用户刚修改 Resource 后，详情页可以立即看到新值，而搜索结果可能在极短时间后更新。

### 12.4 Secure Domain

Private Notes、Password Manager 等 Secure Domain 数据禁止直接进入普通明文搜索索引。

如果未来提供 Secure Search，必须在对应安全设计中明确：

- 索引在哪一侧构建；
- 索引是否加密；
- 查询时如何解锁；
- 服务端能看到哪些信息。

---

## 13. 身份、权限与安全架构

### 13.1 三层安全判断

必须区分：

```text
Authentication
确认是谁

Authorization
确认是否有权限

Step-up Verification
确认当前认证保证等级是否足够执行高风险操作
```

完整高风险操作判断可以表示为：

```text
已认证
  AND
拥有 Permission / ACL
  AND
当前验证等级满足要求
  AND
安全策略允许
  ↓
执行操作
```

### 13.2 Platform RBAC 与 Resource ACL

两套模型解决不同问题。

Platform RBAC：

- 管理用户；
- 管理角色；
- 修改系统配置；
- 查看审计；
- 操作 Scheduled Job。

Resource ACL：

- 能否查看某个 Resource；
- 能否编辑某个 Document；
- 能否下载某个 Attachment；
- 能否管理某个 Collection；
- 能否加入某个 Room。

### 13.3 Secret 与普通配置分离

普通 Parameter 不得保存：

- Password；
- Access Token；
- Secret Key；
- Storage Credential；
- Plugin Secret；
- AI Provider Secret。

这些值必须进入独立 Secret / Secure Credential 管理路径。

### 13.4 Secure Data Foundation

Secure Data Foundation 只服务于明确声明的 Secure Domain。

```text
普通业务
  ↓
正常 PostgreSQL / Blob Storage

Secure Domain
  ↓
Secure Data Foundation
  ↓
应用层加密
  ↓
密文持久化
```

不能因为系统存在 Private Notes，就把所有普通 Resource、媒体和账本都强制改造成 E2EE。

### 13.5 审计

以下操作必须进入安全审计：

- 登录与异常登录；
- 权限修改；
- Role 修改；
- 高风险配置修改；
- Secret 访问；
- Secure Domain 解锁；
- 密钥轮换；
- 永久删除；
- 大规模批处理；
- 备份恢复；
- 插件安装与权限变化。

---

## 14. 自动化架构

Automation 用于表达：

> “当某个事件或条件发生时，以某个主体的权限执行一个或多个动作。”

例如：

```text
AttachmentImported
  ↓
Automation Rule
  ├── 生成媒体信息
  ├── 请求 Metadata Provider 补全元数据
  ├── 创建缩略图任务
  └── 通知用户
```

Automation 必须具有：

- Owner；
- Trigger；
- Condition；
- Action；
- Permission Context；
- Retry Policy；
- Idempotency；
- Execution Log；
- Enabled State；
- Rate Limit；
- Failure Policy。

Automation 不获得“系统超级管理员”默认权限。

---

## 15. 插件架构

### 15.1 插件定位

插件是受控扩展能力。

核心系统负责稳定领域语义，插件负责连接变化更快的外部生态。

### 15.2 插件禁止事项

插件不得：

- 假定内部数据库 Schema 永远不变；
- 直接修改其他子系统私有表；
- 获取所有用户数据作为默认能力；
- 绕过 Permission；
- 绕过 Secure Data Boundary；
- 静默读取 Secret；
- 将内部实体类作为长期公共契约。

### 15.3 插件权限

插件未来应具备声明式权限模型，例如：

```text
需要读取 Resource Metadata
需要创建 External Identity
需要创建 Background Task
需要访问某个 Storage Provider
需要发出外部 HTTP 请求
需要读取某个 Secret Reference
```

管理员安装或启用插件时应能够看到其所需能力。

---

## 16. AI 总体架构

### 16.1 AI 是增强能力

没有 AI 时，Ikaros 的核心业务仍必须可工作。

### 16.2 权限感知

AI Retrieval 和 Tool 访问必须继承用户权限。

```text
用户
  ↓
Security Context
  ↓
AI Context Builder
  ↓
Retrieval / Tool
  ↓
Model
```

AI 不能直接读取全库内容。

### 16.3 Tool-based Action

AI 需要改变业务状态时：

```text
AI Agent
  ↓
平台 Command
  ↓
目标子系统
  ↓
Permission / Validation / Audit
```

### 16.4 Human-in-the-loop

以下操作默认必须人工确认：

- 永久删除；
- 修改权限；
- 修改角色；
- 发布内容；
- 批量变更；
- 对外发送消息；
- 修改关键参数；
- 删除 Blob Replica；
- 停用 Storage Provider。

### 16.5 Provider-neutral

AI Provider 必须可替换，支持：

- 云端模型；
- 本地模型；
- 自托管模型；
- OpenAI Compatible API；
- 厂商原生 API。

---

## 17. 数据统计与分析架构

Analytics 不直接对大量业务表执行任意跨域 JOIN 作为长期架构。

推荐通过统一事实流构建：

```text
Event / Activity / Snapshot
  ↓
Fact
  ↓
Aggregate
  ↓
Metric Definition
  ↓
Dashboard / Report
```

正式 Metric 必须定义：

- Key；
- 名称；
- 描述；
- 单位；
- 聚合方式；
- 维度；
- 来源；
- 时间语义；
- Null 语义；
- Owner；
- Version。

这样能够避免同一个“完成任务数”“观看时长”“存储占用”等指标在不同页面出现不同口径。

---

## 18. 缓存设计

缓存是性能优化，不是业务状态来源。

缓存层可以包括：

- 服务进程内缓存；
- Redis；
- HTTP Cache；
- CDN；
- 客户端缓存；
- 媒体本地缓存。

通用原则：

1. 缓存丢失不应造成业务数据丢失。
2. 缓存必须有明确 TTL 或失效策略。
3. 权限敏感数据的缓存键必须包含安全上下文。
4. Secure Domain 明文不得进入普通共享缓存。
5. 不能通过缓存绕过最新 ACL / Permission 判断。

---

## 19. 一致性、并发与幂等

### 19.1 领域内部强一致

单一子系统内的关键业务不变量优先通过 PostgreSQL 事务和数据库约束保证。

例如：

- 唯一关系；
- 状态迁移；
- Ledger 平衡规则；
- Resource Lifecycle；
- Blob Replica 引用约束。

### 19.2 跨领域最终一致

搜索、通知、统计、自动化等跨领域流程通过 Event 异步完成。

### 19.3 乐观并发

文档、配置、可编辑 Resource 等场景应考虑 Version / ETag / Revision，避免后写覆盖先写。

### 19.4 幂等消费

Event Consumer 和 Background Task Handler 必须能够安全处理重复投递。

推荐使用：

- event_id；
- idempotency_key；
- unique constraint；
- processed_event 记录；
- 业务自然幂等规则。

---

## 20. 生命周期与删除策略

Resource、Attachment、Blob 必须区分逻辑删除和物理回收。

### 20.1 Resource 生命周期

至少支持：

- Active；
- Archived；
- Trashed；
- Purged。

### 20.2 Blob 回收

Resource 被删除不代表 Blob 可以立即删除。

Blob 可能仍然被以下对象引用：

- 其他 Attachment；
- Revision；
- Share；
- Backup；
- Derived Relationship；
- Archive；
- Secure Snapshot。

物理回收必须经过引用判断和安全策略。

### 20.3 高风险删除

永久删除必须：

- 明确影响范围；
- 权限校验；
- Step-up Verification；
- 审计；
- 必要时延迟清理；
- 允许后台执行；
- 给出不可恢复提示。

---

## 21. 备份与恢复

Ikaros V2 的备份对象不仅是 PostgreSQL。

完整备份体系需要覆盖：

- PostgreSQL；
- Blob 数据；
- Storage Provider 配置；
- Plugin 配置；
- Secret / Key Material 的安全备份；
- Search Index 是否需要备份由成本决定，默认应可重建；
- Analytics Aggregate 默认可重建，但必要时可备份以缩短恢复时间。

### 21.1 备份原则

- 业务数据与 Blob 必须形成一致的恢复点语义；
- Secure Domain 备份保持密文；
- 密钥备份不能和普通明文配置混放；
- 定期执行恢复演练；
- Backup Success 不等于 Restore Verified。

### 21.2 灾难恢复

恢复顺序建议：

```text
基础配置
  ↓
PostgreSQL
  ↓
Blob Storage
  ↓
Secure Key / Secret Material
  ↓
Plugin
  ↓
重建 Search Index
  ↓
重建 Analytics
  ↓
执行完整性校验
```

---

## 22. 可观测性与运维

### 22.1 三类日志分离

```text
Application Log
用于开发与运行诊断

Audit Log
用于回答谁执行了什么高价值操作

Activity
用于表达用户与业务行为时间线
```

三者不能混为一张表或一个概念。

### 22.2 Metrics

系统至少监控：

- HTTP 请求；
- 错误率；
- 响应时间；
- PostgreSQL 连接；
- Background Task；
- Scheduled Job；
- Event Backlog；
- Search Index Lag；
- Storage Provider Health；
- Blob Missing / Corrupted；
- Cache；
- JVM / Runtime；
- Worker 状态。

### 22.3 Health

Health 需要区分：

- Server Runtime；
- PostgreSQL；
- Storage Provider；
- Search；
- Redis；
- Plugin；
- External Provider；
- Worker。

单个非核心外部 Provider 故障不应导致整个 Ikaros 被判定为完全不可用。

### 22.4 Trace / Correlation

跨 HTTP、Command、Event、Automation、Background Task 的关键链路应携带：

- request_id；
- trace_id；
- actor；
- source_event_id；
- task_id；
- automation_run_id。

从而能够追踪“一个用户动作最终触发了哪些系统行为”。

---

## 23. 客户端架构关系

### 23.1 CMS / Web Console

CMS 主要面向：

- 内容管理；
- Resource 编辑；
- Collection 管理；
- 用户、角色与权限；
- Plugin；
- Storage；
- Background Task；
- Scheduled Job；
- 运维；
- 系统配置；
- 数据统计。

CMS 不拥有独立的后台业务规则。

### 23.2 Flutter App

Flutter App 主要面向：

- 内容发现；
- 播放；
- 阅读；
- 音乐；
- 图片；
- 个人状态；
- Room；
- 笔记与 Productivity；
- 通知；
- Secure Domain 客户端解锁。

移动端与桌面端应共享同一业务 API，但可根据设备能力提供不同交互。

### 23.3 第三方客户端

只要具备相同权限，第三方客户端应能够通过公开 API 完成与官方客户端一致的核心业务能力。

---

## 24. 技术实现基线

V2 的业务架构不应依赖具体框架才能成立，但结合当前仓库技术方向，初始实现基线可以保持：

| 层 | 基线方向 |
|---|---|
| Server Runtime | Java 21 |
| Server Framework | Spring Boot 4 / WebFlux |
| Reactive Data | Reactor / R2DBC |
| Database | PostgreSQL |
| Cache | Redis，可选 |
| API Contract | OpenAPI |
| Plugin Runtime | PF4J 方向，V2 重新定义稳定扩展契约 |
| Search | 独立 Search Subsystem，具体引擎可演进 |
| CMS | Vue / TypeScript / Vite 方向 |
| App | Flutter |
| Metrics | Micrometer / Prometheus 方向 |
| Deployment | Docker / Docker Compose 优先 |

此表表示工程实现基线，不代表所有版本永久绑定这些组件。

如果后续更换 Search Engine、Cache、Plugin Runtime 等，不应改变 Resource、Attachment、Capability、Command、Event 等核心业务语义。

---

## 25. 部署架构

### 25.1 最小单机部署

```mermaid
flowchart TB
    CLIENT[Web / App]
    PROXY[Reverse Proxy / HTTPS]
    SERVER[Ikaros Server]
    PG[(PostgreSQL)]
    LOCAL[(Local / NAS Blob Storage)]

    CLIENT --> PROXY
    PROXY --> SERVER
    SERVER --> PG
    SERVER --> LOCAL
```

这是 V2 必须长期保持良好支持的部署形态。

### 25.2 推荐家庭服务器部署

```mermaid
flowchart TB
    CLIENT[Web / App]
    PROXY[Reverse Proxy]
    SERVER[Ikaros Server]
    PG[(PostgreSQL)]
    REDIS[(Redis 可选)]
    NAS[(NAS / Local Storage)]
    S3[(S3 Compatible Storage)]

    CLIENT --> PROXY
    PROXY --> SERVER
    SERVER --> PG
    SERVER --> REDIS
    SERVER --> NAS
    SERVER --> S3
```

### 25.3 扩展部署

资源密集任务增加后：

```mermaid
flowchart TB
    PROXY[Reverse Proxy]
    SERVER[Ikaros Server]
    WORKER1[Media Worker]
    WORKER2[AI / OCR Worker]
    PG[(PostgreSQL)]
    REDIS[(Redis / Coordination)]
    STORAGE[(Object / NAS Storage)]

    PROXY --> SERVER
    SERVER --> PG
    SERVER --> REDIS
    SERVER --> STORAGE
    WORKER1 --> PG
    WORKER1 --> STORAGE
    WORKER2 --> PG
    WORKER2 --> STORAGE
```

即使 Worker 独立运行，领域状态仍由对应子系统契约控制。

---

## 26. 非功能性设计要求

### 26.1 性能

- 普通列表和详情查询不应依赖跨全部子系统的大型 JOIN；
- 大文件上传和下载使用流式处理；
- 媒体内容不得整体加载到 JVM 内存；
- 大规模导入、转码、索引、备份通过后台任务执行；
- Search / Analytics 使用派生数据降低核心业务库压力。

### 26.2 可靠性

- 关键业务状态使用数据库约束；
- 关键 Event 使用 Durable Event / Outbox；
- Background Task 可重试；
- Event Consumer 幂等；
- Storage 有完整性校验；
- Blob Replica 状态可检测；
- Backup 必须可验证恢复。

### 26.3 安全

- 默认 HTTPS；
- 安全 Cookie / Token 策略；
- 密码安全哈希；
- RBAC + ACL；
- Step-up Verification；
- Secret 与配置分离；
- Secure Domain 应用层加密；
- 高风险操作审计；
- Plugin 权限边界；
- AI 权限继承。

### 26.4 可维护性

- 子系统内部实现不向外泄漏；
- 跨系统使用稳定契约；
- 数据迁移可版本化；
- API 有明确版本策略；
- Search 与 Analytics 可重建；
- 插件使用公共扩展契约；
- 重要架构变化通过 ADR 记录。

### 26.5 可扩展性

系统扩展优先级：

```text
先扩展子系统内部实现
  ↓
再扩展 Background Worker
  ↓
再增加外部基础设施
  ↓
只有明确需要时拆分独立服务
```

不以“未来可能很多用户”为理由提前构建复杂分布式架构。

---

## 27. 故障与降级原则

V2 必须允许非核心能力故障时继续提供核心服务。

示例：

| 故障 | 期望行为 |
|---|---|
| Redis 不可用 | 无 Redis 依赖的核心业务继续工作，缓存退化 |
| AI Provider 不可用 | 普通搜索、编辑、播放等继续工作 |
| Metadata Provider 不可用 | 允许人工编辑与稍后重试同步 |
| Search Index 异常 | 详情与业务数据仍可访问，管理员可重建索引 |
| Analytics 异常 | 核心业务不受影响，统计延迟更新 |
| 单个 Storage Replica 异常 | 尝试其他可用副本并标记异常 |
| 冷存储内容未恢复 | 返回 Restoring 状态，而不是假装资源不存在 |
| 单个 Plugin 崩溃 | 隔离插件故障，避免拖垮整个 Server |

---

## 28. V2 代码组织建议

代码组织应按领域边界，而不是按 Controller / Service / Repository 技术层全局堆放。

推荐概念结构：

```text
server
└── subsystem
    ├── resource
    ├── media
    ├── reading
    ├── music
    ├── image
    ├── document
    ├── productivity
    ├── finance
    ├── security
    ├── securedata
    ├── storage
    ├── search
    ├── collaboration
    ├── automation
    ├── analytics
    ├── ai
    ├── plugin
    └── operations
```

每个子系统内部再组织：

```text
<subsystem>
├── api / contract
├── application
├── domain
└── infrastructure
```

这里表达的是模块边界原则，不要求最终目录必须逐字一致。

禁止形成：

```text
controller/
service/
repository/
entity/
```

然后把所有业务域全部混在同一个技术分层目录中的大型结构。

---

## 29. 系统启动与运行阶段

### 29.1 启动阶段

建议启动顺序：

```text
加载基础配置
  ↓
初始化数据库连接
  ↓
执行 Schema Migration
  ↓
初始化 Security / Key Runtime
  ↓
加载核心子系统
  ↓
加载 Plugin Registry
  ↓
注册 Background Task Handler
  ↓
启动 Event Dispatcher
  ↓
启动 Scheduled Job
  ↓
执行 Health Check
  ↓
对外提供服务
```

### 29.2 停机阶段

优雅停机应：

- 停止接收新的高成本任务；
- 停止新的 Scheduled Job 调度；
- 等待可安全完成的请求；
- 保存 Background Task 状态；
- 释放 Plugin 资源；
- 关闭实时连接；
- 关闭数据库与存储连接。

---

## 30. 设计文档关系

本概要设计与现有 V2 文档的关系如下：

| 文档 | 职责 |
|---|---|
| `Product-Requirements-Document.md` | 定义产品目标、核心概念、功能范围和产品约束 |
| `System-Overview-Design.md` | 定义整个 V2 的系统级总体结构与统一架构规则 |
| `Platform-Integration-Automation-Design.md` | 详细定义 Capability、Command、Event、Relation、Automation、Activity、Context |
| `Security-Identity-Authorization-Crypto-Subsystem-Design.md` | 详细定义认证、授权、Step-up、安全会话与密码学能力 |
| `Secure-Data-Foundation-Design.md` | 定义 Secure Domain 的统一密文持久化与安全基础 |
| `Platform-Administration-Operations-Subsystem-Design.md` | 定义用户、角色、权限、配置、日志、通知、任务和运维 |
| `Data-Analytics-Statistics-Subsystem-Design.md` | 定义 Fact、Aggregate、Metric 与统一统计分析体系 |
| `AI-Intelligence-Subsystem-Design.md` | 定义 AI Provider、模型、RAG、Agent 与跨系统 AI 能力 |
| `AI-Persona-System-Design.md` | 定义 AI Persona 相关语义与行为边界 |
| `Productivity-Planning-Subsystem-Design.md` | 定义效率、计划、Goal、Task、OKR 等业务 |
| `Personal-Finance-Accounting-Subsystem-Design.md` | 定义个人财务与记账业务 |
| `Private-Notes-Subsystem-Design.md` | 定义私密笔记 Secure Domain |
| `Password-Manager-Subsystem-Design.md` | 定义密码管理 Secure Domain |

未来新增 Media、Reading、Music、Storage、Search、Collaboration、Plugin 等详细设计时，都应以本文档的系统边界作为上位约束。

---

## 31. 关键架构决策摘要

V2 当前系统级关键决策如下：

1. **Resource-centric**：逻辑内容统一以 Resource 为身份基础。
2. **Attachment 与 Blob 分离**：业务内容对象与实际字节身份分离。
3. **Storage Placement 独立**：Blob 可以有多个物理副本。
4. **PostgreSQL-first**：核心业务数据库只面向 PostgreSQL。
5. **模块化单体优先**：初期不主动微服务化。
6. **HTTP-first**：公开 API 是客户端与外部集成主要边界。
7. **子系统拥有自己的状态**：其他模块禁止直接修改内部数据。
8. **Capability / Command / Event 分离**：同步查询、状态变更和异步传播具有不同语义。
9. **跨系统最终一致性**：Search、Analytics、Automation、Notification 默认异步。
10. **Durable Event**：关键事件使用可靠事件机制，不能只依赖内存发布。
11. **Search / Analytics 是派生数据**：必须能够重建。
12. **Platform RBAC 与 Resource ACL 分离**：平台权限和实例权限不混用。
13. **Secure Domain 显式声明**：高敏感加密边界不扩散到普通业务。
14. **Secret 与 Parameter 分离**：敏感凭据不能作为普通配置保存。
15. **AI 不是真相源**：AI 只能建议或通过受控 Tool 执行业务操作。
16. **Plugin 受权限约束**：插件不拥有默认全库访问权。
17. **Background Task、Scheduled Job、Productivity Task 分离**。
18. **自托管简单性长期保留**：单机部署不是过渡方案，而是正式支持场景。

---

## 32. 后续设计工作

在本概要设计之后，建议继续补充以下 V2 设计文档：

1. Resource / Collection / Relation Core Design；
2. Attachment / Blob / Storage Subsystem Design；
3. Media Subsystem Design；
4. Reading Subsystem Design；
5. Music Subsystem Design；
6. Image / Album Subsystem Design；
7. Document / Content Creation Subsystem Design；
8. Search / Discovery Subsystem Design；
9. Sharing / Collaboration / Room Subsystem Design；
10. Plugin Architecture Design；
11. Background Task / Scheduler Design；
12. Notification Subsystem Design；
13. Backup / Restore / Archive Design；
14. V2 Database Overview Design；
15. V2 API Convention；
16. V2 Deployment & Operations Guide；
17. CMS 与 App 交互设计。

这些文档应继续遵循本文确定的统一模型和系统边界，避免各子系统独立设计后再次出现概念重复、权限重复、存储重复和跨模块直接耦合。

---

## 33. 总结

Ikaros V2 的核心不是简单增加更多功能，而是建立一套能够长期承载多种个人数字业务的统一平台结构。

整体架构可以概括为：

```text
统一 Resource 身份
  +
专业领域子系统
  +
Attachment / Blob / Storage 基础
  +
Identity / Permission / Secure Data
  +
Capability / Command / Event / Automation
  +
Search / Analytics / AI 横向能力
  +
Plugin 扩展体系
  +
HTTP-first 多端访问
  +
模块化单体、自托管优先
```

最终目标是让不同业务能力可以在明确边界下组合，而不是继续形成彼此独立的功能孤岛。

V2 的每一个后续子系统设计都应回答两个问题：

1. **这个子系统真正拥有的业务状态是什么？**
2. **它通过什么公开契约与其他子系统协作？**

只要这两个边界始终清晰，Ikaros 就可以在保持自托管简单性的同时，逐步演进为完整的 Personal Digital Content Platform。
