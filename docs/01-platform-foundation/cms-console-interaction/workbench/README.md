# Dashboard 与全局工作台 — CMS Console 交互规格

> 本文只定义 Dashboard 和全局搜索。后台长期工作统一由 `/activity` 承载；收藏、消费进度和 Resource 业务活动回到 Library / Resource 语境，不再占用全局 Activity 路由。

## 1. Dashboard

**Route：** `/dashboard`

Dashboard 是 Console 默认入口，目标是回答“现在有什么需要我处理”和“Ikaros 正在做什么”。

### 1.1 页面标题

- H1：`Dashboard` / `仪表盘`；
- 副标题：当前环境和最近成功刷新时间；
- 操作：刷新、必要时的显示偏好；
- 不提供与核心任务无关的“为了填满页面”的 KPI。

### 1.2 Attention Queue

优先展示：

- 导入失败或待确认；
- Resource / 剧集 / 章节等 Missing、Corrupted、Restore 失败；
- Storage Provider unhealthy；
- Metadata 冲突；
- Backup / Sync / Index 等关键后台失败；
- 权限或配置导致的关键能力不可用。

每条包含：严重级别、业务标题、简短原因、发生时间、主动作。

跳转规则：优先进入业务对象详情。例如“某集不可用”进入 Resource Detail 对应内容项；只有需要执行诊断时再进入 Activity 或 System Diagnostics。

### 1.3 In Progress

展示正在进行的高价值长期工作，例如：

- 导入内容；
- Archive Restore；
- Backup；
- Sync；
- Index Rebuild。

每行使用业务动作、关联对象、进度、状态和更新时间。`查看全部` 进入 `/activity`。

### 1.4 Library 摘要

可展示 Resource 数量、近期新增、不可用内容数量等，但必须服务于下一步动作。

点击摘要进入 `/library` 并带对应筛选。

### 1.5 Storage 摘要

展示：

- HOT / WARM / COLD / ARCHIVE / DEEP 等层级分布；
- 已使用容量；
- Provider 异常数量；
- Restoring 数量。

点击进入 `/storage`。

### 1.6 健康语义

- `Unknown`、未探测、请求失败不能显示成正常；
- 真实 Healthy 必须有来源和有效时间；
- 单个区块加载失败时其他区块继续工作，但失败区块明确显示 Unknown / 加载失败。

### 1.7 空状态

首次使用时展示 setup 引导：

1. 配置 Storage Provider；
2. Add Content；
3. 从 Library 查看结果。

不展示空的“今天计划”“财务”“AI”等未启用 App KPI。

## 2. 全局搜索

全局搜索可以通过 Top App Bar 或 `Ctrl/Cmd + K` 打开，也可以使用 `/library/search` 完整页面。

### 2.1 默认搜索范围

优先搜索 Resource、Collection 和用户有权访问的核心业务对象。

搜索结果必须显示业务类型和安全识别字段，不直接用内部 ID 作为标题。

### 2.2 Library 搜索

Resource 搜索进入 `/library/search?q=...`。

支持：

- Resource 类型；
- Lifecycle；
- Availability；
- Collection；
- Tag；
- Source；
- 更新时间；
- 收藏/用户状态。

### 2.3 App / System 搜索

启用 App 可以注册搜索结果，但必须声明来源 App。System 管理对象只有具备相应管理权限时才返回。

### 2.4 私密域

Private Notes、Passwords、Finance、Drive 等继续遵守各自数据敏感性和解锁规则。解密后的搜索词不得写入普通 URL、Telemetry 或非安全日志。

## 3. 用户活动与收藏

不再设计全局“我的活动与收藏”页面占用 `/activity`。

- 收藏：在 Library 中通过筛选或用户视图处理；
- 视频/阅读进度：在对应 Resource Detail 或 Media/Reading App 中处理；
- Resource 业务活动：在 Resource Detail 的 Activity Tab 中处理；
- 系统后台工作：统一进入 `/activity`。

这样用户不需要在“活动历史”和“后台任务”之间猜测同名入口。

## 4. 验收

- Dashboard 无虚假 Healthy；
- Attention 能进入业务对象；
- In Progress 与 `/activity` 状态一致；
- Storage / Library 摘要可以直接应用目标筛选；
- 全局搜索不越权；
- 不再存在历史 `/console/dashboard` 或把 `/activity` 定义为收藏历史的设计要求。
