# 附件与存储 — CMS Console 交互规格

## 1. 附件与 Blob

**路由：** `/console/attachments`

### 页面标题区
- 标题：`附件与 Blob`。
- 副标题明确说明 `Resource → Attachment → Blob → Placement` 的层级关系。
- 主操作：`上传附件`。
- 次操作：`扫描/导入`。

### KPI 卡片
- Attachment 数量。
- 唯一 Blob 数量。
- 去重节省空间。
- 缺失/不可用 Blob 数量。
- 完整性警告数量。

### 筛选

支持按 Attachment 名称、Checksum、Blob ID、关联 Resource 搜索。

Filter Chip：Attachment 角色/类型、MIME 大类、可用状态、原始/派生、完整性状态、存储层级。

高级筛选：大小范围、创建时间、Checksum 算法、引用数量、孤儿状态。

### Data Table

列：
1. 选择 Checkbox；
2. 类型图标/预览；
3. Attachment 显示名称；
4. 角色，例如 `VIDEO`、`AUDIO`、`SUBTITLE`、`COVER`、`PAGE`、`DOCUMENT`；
5. 关联 Resource 数量；
6. Blob 短 ID + Checksum 状态；
7. 逻辑大小；
8. Placement 摘要，例如 `2 个副本 · Hot/Warm`；
9. 来源：`原始` / `派生`；
10. 可用性/完整性 Chip；
11. 更新时间；
12. 操作。

点击行进入 Attachment 详情。

### Attachment 详情

标题区展示显示名称、角色、MIME、可用状态。操作：预览/打开、下载、在允许时安全替换内容、创建派生内容、Overflow。

Tabs：
- `概览`。
- `Resource 关联`。
- `Blob`。
- `派生内容`。
- `活动`。

概览字段：Attachment ID、文件名/显示名称、MIME、大小、角色、创建时间、创建者/来源、原始/派生、元数据、可访问状态。

Resource 关联表格：Resource、关系角色、是否主附件、创建时间。`关联 Resource` 打开 Resource 搜索，并要求选择 Attachment Relation Role。移除关系前必须明确该 Attachment 是否仍被其他 Resource 引用。

Blob Tab：Blob ID、Checksum 算法和值、字节大小、最近校验时间、引用数量、Placement 列表。`校验完整性` 创建后台任务。

派生内容显示父子 Graph，例如原始视频 → 缩略图/转码/字幕提取，并展示生成状态，以及可用时的 Recipe/工具标识。

## 2. 持久化存储层

**路由：** `/console/storage/tiers`

### 总览

Hot、Warm、Cold/Archive 和总持久化数据分别使用状态卡片。每张卡显示已使用/容量、对象数量、健康/降级状态、Provider/Backend。

必须明确：**Cache 不是持久化存储层。**

### 存储 Backend 表格

列：名称、Backend 类型、Tier、Endpoint/位置（安全显示）、容量/已使用、健康状态、读写状态、副本数量、最近检查、操作。

`添加存储 Backend` Side Sheet 字段：
- 名称：必填；
- Backend/Provider 类型；
- Tier；
- 根据类型展示 Endpoint/Bucket/Path；
- Credential Secret 字段；
- 加密/配置 Switch；
- 读写模式；
- 健康检查配置。

`测试连接` 使用当前输入进行校验，但不得把 Secret 写入 UI 日志。保存后 Credential 只返回已配置/遮罩状态。

### Backend 详情

Tabs：`概览`、`Placements`、`策略`、`健康`、`活动`。

概览展示经过安全处理的配置字段、容量图表、延迟和错误统计。

Placements 表格列出当前 Backend 上的 Blob、大小、副本状态和最近校验时间。

策略区域配置 Placement/迁移规则，支持优先级排序和 Dry-run 预览。规则字段：匹配条件、目标 Tier/Backend、最小副本数、Age/Access 条件、动作。`模拟` 在启用前显示预计命中对象数量和字节数。

## 3. 缓存与我的下载

**路由：** `/console/storage/cache`

Tabs：`服务端缓存`、`客户端下载`。

### 服务端缓存

卡片：缓存字节数、对象数、命中率、Eviction 活动。

表格：Blob/Resource、缓存类型、大小、最近访问、过期/可淘汰时间、来源持久化 Placement。

操作：`淘汰所选缓存`、`预热/预取`。淘汰确认必须明确：**不会影响持久化数据。**

### 客户端下载

列：用户/设备、Resource、Attachment、大小、状态、下载时间、最近同步。

管理员只看到管理所需元数据；除非客户端明确上报并且用户有权限，否则不得显示客户端本地私有路径。

如果支持远程删除请求，按钮文案必须使用 `请求客户端删除`，不能暗示服务端能够立即删除客户端数据。

## 4. 归档、恢复与回收站

**路由：** `/console/storage/archive`

Tabs：`已归档资源`、`恢复队列`、`回收站`。

### 已归档资源

列：Resource、类型、归档时间、归档存储摘要、逻辑大小、预计恢复耗时/状态、保留策略、操作。

`恢复` Dialog：
- 目标恢复 Tier/Backend；
- 预计字节数；
- 受影响 Attachment；
- 明确说明这是异步操作；
- 可选 `恢复完成后通知并打开`。

提交后创建后台任务，并立即返回列表，同时出现进度行。

### 恢复队列

列：任务、目标 Resource/Blob 数量、当前阶段、已恢复/总字节数、目标位置、开始时间、可信时显示 ETA、发起人、操作。

只有当前阶段可安全取消时才显示 `取消`，并解释已完成数据和清理语义。

### 回收站

列：Resource/Attachment、类型、删除者、进入回收站时间、计划永久删除时间、依赖/引用摘要、操作。

操作：恢复、永久删除。永久删除使用 Error 色高风险 Dialog，列出引用影响，并要求输入实体名称；批量永久删除可以要求输入 `DELETE`。

## 5. 备份与恢复

**路由：** `/console/storage/backup`

### 摘要卡片
- 最近一次成功备份。
- 下一次计划备份。
- 备份目标健康状态。
- 最近恢复验证状态。

### 备份集表格

列：Backup Set ID/时间、范围、目标、逻辑大小、增量/全量、加密状态、校验状态、保留到期时间、状态、操作。

主操作：`立即备份`、`配置备份`。

### 配置备份

分区：
- 范围：数据库/配置/元数据/Blob 选择策略。
- 目标 Backend。
- 调度计划。
- 保留策略。
- 加密/Key 引用。
- 校验策略。

每个分区下方显示简短后果说明。Credential/Key 只展示引用，不展示值。

### 恢复向导

使用完整页面 Stepper：
1. 选择 Backup Set。
2. 校验兼容性和完整性。
3. 选择恢复范围。
4. 选择冲突/覆盖策略。
5. 审阅影响。
6. 涉及覆盖时重新认证。
7. 启动恢复。

恢复开始后进入可持久访问的后台操作详情页，显示阶段进度、适合 UI 展示的安全日志/事件，以及适用时明确提示是否需要重启服务器。

## 通用交互规则
- 字节数统一使用 Binary Unit，并可在 Tooltip 中显示精确 Bytes。
- 完整性失败必须使用 Error 严重级别，不能降级成普通中性 Chip。
- 迁移、归档、恢复、校验、备份、清理等后台操作离开页面后仍可从后台任务中心继续查看。
- UI 不得把“移除 Attachment 关系”描述成“删除 Blob”；所有确认文案必须准确说明实际修改的是哪一层。
