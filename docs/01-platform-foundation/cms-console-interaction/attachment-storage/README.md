# Storage Workspace — CMS Console 交互规格

> Attachment / Blob / Placement 仍是核心存储模型，但默认 Console 以“数据是否安全、在哪里、如何恢复”为主语。Blob 级操作进入 Advanced Maintenance。

## 1. Storage Overview

**Route：** `/storage`

### 1.1 目标

回答：

- 持久化数据分布在哪些层级；
- 当前容量和增长；
- 哪些 Provider 异常；
- 有多少内容正在恢复；
- 是否存在 Integrity / Replica 风险。

### 1.2 主要区域

- Tier 分布：HOT / WARM / COLD / ARCHIVE / DEEP；
- Provider Health 摘要；
- Archive / Restore 摘要；
- Maintenance Attention；
- Backup 摘要。

Unknown、未探测和加载失败不得显示为 Healthy。

Cache 必须与持久化 Storage 区分。

## 2. Providers

**Route：** `/storage/providers`

Provider 页面只负责真实存储后端配置和健康。

列表字段：

- 名称；
- Provider 类型；
- Endpoint / Bucket / Region 等安全摘要；
- Capacity / Used；
- Health；
- Credential configured state；
- Enabled state；
- Last probe。

操作：Add Provider、Probe、Update Credentials、Enable/Disable。

**Provider Detail：** `/storage/providers/:providerId`

Tabs：Overview、Capacity、Health、Activity、Advanced。

不得在 Providers 首页混入 Blob GC、Replica Repair 或 Placement 查询。

## 3. Policy

**Route：** `/storage/policy`

负责内容生命周期和 Placement 目标策略，而不是 Provider 连接信息。

规则可以包含：

- Resource / Attachment 类型条件；
- Age / Last Access；
- Source / Collection 等业务条件；
- 目标 Tier；
- 目标副本数；
- 保留期；
- 迁移 / 归档动作。

策略启用前提供 Dry-run / Impact Preview，展示预计对象数量和字节数。大规模策略变更按安全策略要求确认或 Step-up。

## 4. Archive & Restore

**Route：** `/storage/archive`

Tabs：Archived Content、Restore Queue、Archive Activity。

### Archived Content

以 Resource / 业务内容为主：标题、类型、归档时间、所在层级、逻辑大小、Availability、操作。

Restore Dialog 展示目标层级、预计字节数、受影响内容和异步语义。

### Restore Queue

队列状态必须同时进入全局 `/activity`。此页面是 Storage 业务视图，不创建另一套任务状态模型。

## 5. Maintenance

**Route：** `/storage/maintenance`

这是高级维护区，可以直接使用内部存储术语。

包含：

- Blob Integrity；
- GC Candidate / Physical Cleanup；
- Placement / Replica Diagnostics；
- Repair / Promote；
- Delivery / CDN 深度诊断；
- Orphan / Reference 检查。

### 5.1 Blob / Attachment Inspector

允许按业务对象、Attachment、Blob ID、Checksum 等查询。但“输入 Blob ID”只属于高级排障，不是 Storage 首页主任务。

### 5.2 Integrity

Integrity Verify 创建长期后台工作并进入 `/activity`。失败使用 Error 级别，并可以回到相关 Resource / Attachment 查看业务影响。

### 5.3 GC

GC 必须明确引用、保留期和物理删除后果。普通 Resource 删除或 Attachment unlink 不等于立即物理删除 Blob。

### 5.4 Replica / Placement

显示 Placement、Tier、Provider、状态、最近校验和修复动作。Repair/Promote 进入 `/activity`。

## 6. Backup

**Route：** `/storage/backup`

展示最近成功备份、下一计划、目标健康和最近恢复验证。

备份配置包括：范围、目标、调度、保留、加密/Key Reference、校验策略。

Restore 使用完整向导：选择 Backup → 校验 → 范围 → 冲突策略 → 影响确认 → 必要时 Step-up → 启动。

Backup/Restore 的执行状态统一进入 `/activity`。

## 7. Cache 与客户端下载

Cache 是加速层，不是持久化 Storage Tier。

服务端 Cache 可以作为 Storage Overview 的二级页面或 Maintenance 子视图；客户端下载属于 Offline / Media / Drive 等业务 App 的用户体验，不创建新的一级 Storage 导航。

淘汰 Cache 必须明确不会删除持久化原始数据。

## 8. Resource 与 Storage 的跨域规则

- Resource Detail 默认展示 Availability 和简单 Storage 摘要；
- 需要技术排障时进入 `/storage/maintenance`；
- Storage → Resource deep link 重新检查 `resource.read`；
- Storage 管理权限不自动授予 Drive 私有文件内容读取；
- Resource 编辑权限不自动授予 Placement / Provider / GC 管理。

## 9. 后台工作

Migration、Archive、Restore、Verify、Repair、GC、Backup 等长期操作全部进入全局 `/activity`。

Storage 页面可以显示当前业务上下文的任务摘要，但不得定义独立“后台任务中心”。

## 10. 用户语言

默认页面使用：数据层级、Provider、恢复、备份、完整性风险等可理解术语。

Attachment ID、Blob ID、Checksum、Placement、Replica、Object Key 等只在 Advanced / Maintenance 中展示。

## 11. 验收

- `/storage` 不再同时承担 Provider 配置和 Blob GC；
- Provider / Policy / Archive & Restore / Maintenance / Backup 职责独立；
- Unknown 不显示为 Healthy；
- Resource 问题可以从业务对象发起 Restore/Repair；
- 所有长期 Storage 工作统一进入 `/activity`；
- 不再使用历史 `/console/storage/*` 或 `storage-center` 路由作为设计基线。
