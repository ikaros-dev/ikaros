# 资源库、资源与创作 — CMS Console 交互规格

> Resource 是统一逻辑内容身份。Console 中 Resource 管理的用户入口固定为“资源 / 资源库”；Documents、Media、Sharing 等专业体验属于“应用”，但必须引用同一 Resource 身份。

## 1. 资源库（Library）

**Route：** `/resources/library`

### 1.1 页面目标

统一浏览和管理动画、电影、剧集、视频、漫画、章节、小说、音乐、图片、文章、文档、游戏等 Resource。

不再设计“资源中心”“内容”“内容与媒体中心”等平级入口。“资源”是一级目录，资源库是其二级页面。

### 1.2 标题区

- H1：`资源库`；
- 主操作：`添加资源`，进入 `/resources/add`；
- 次操作：`创建资源`，只在确实需要手工创建且有权限时显示。

### 1.3 筛选

支持：

- 资源类型；
- Availability；
- Lifecycle；
- Collection；
- Tag；
- Source；
- 收藏 / 用户状态；
- 更新时间；
- 元数据冲突状态。

筛选状态同步 URL Query，私密字段除外。

### 1.4 列表 / Grid

默认展示业务标题、封面/类型图标、资源类型、Availability、Lifecycle、Collection/Tag 摘要、进度和更新时间。

内部 Resource ID 不作为默认主列。复制内部 ID 等操作进入 Overflow / Advanced。

点击条目进入 `/resources/library/:resourceId`。

## 2. Canonical Resource Detail

**Route：** `/resources/library/:resourceId`

Resource Detail 是资源调查和资源级操作的 canonical 起点。

### 2.1 Header

展示：

- 标题与别名；
- 类型；
- Lifecycle；
- Availability；
- 封面；
- 收藏 / 用户状态；
- 播放、阅读、刷新元数据、恢复、归档等适用动作。

### 2.2 Tabs

按适用性显示：

- Overview；
- Episodes / Chapters / Tracks / Pages 等类型专属业务结构；
- Metadata；
- Files；
- Relations；
- Collections & Tags；
- Activity；
- Advanced。

这些 Tab 默认属于同一 Resource Detail 页面；只有需要独立 deep link、权限或生命周期时才建立子路由。

### 2.3 资源结构与 Availability

剧集、章节、曲目等每项直接展示业务可用状态：Available、Cached、Remote、Processing、Restoring、Missing、Corrupted。

点击异常状态时解释原因和下一步，例如“当前只有归档副本，可发起恢复”。Restore / Repair 进入 `/resources/activity`。

### 2.4 Metadata

按字段展示当前值、来源、人工确认状态和外部候选。

发生冲突时提供：保留人工值、采用候选值、手工编辑。外部同步不得静默覆盖人工确认值。

### 2.5 Files

日常视图使用“视频文件、字幕、封面、电子书文件”等业务角色，不直接要求用户理解 Blob Placement。

Advanced 中才展示 Attachment ID、Blob ID、Checksum、Replica、Placement 等技术信息。

### 2.6 Relations / Collections / Tags

保持明确关系类型和 Collection/Tag 语义。禁止通过自由文本替代有类型的关系。

## 3. Collections

**Route：** `/resources/library/collections`

支持手动 Collection 和动态 Collection。

列表展示名称、类型、Resource 数量、所有者/可见性和更新时间。动态规则使用字段/操作符/值组合，并提供预览命中结果。

Collection 详情中的 Resource 点击回到 `/resources/library/:resourceId`。

## 4. Search

**Route：** `/resources/library/search`

搜索只返回调用者有权读取的 Resource 和 Collection。支持关键词、类型、Availability、Lifecycle、Tag、Collection、Source 等条件。

搜索结果使用业务标题和类型，不使用内部 ID 作为主要识别方式。

## 5. 文档应用（Documents）

**Base Route：** `/apps/documents`

文档是专业创作体验，不再作为资源库的平级一级菜单。

能力包括：文档/文章列表、Working Copy 编辑、Revision / Diff、Publish / Unpublish、Attachment 插入、冲突恢复。

文档对应的逻辑 Resource 仍可从资源库的 Resource Detail 进入专业编辑器。

## 6. 媒体应用（Media）

**Base Route：** `/apps/media`

媒体承载专业消费体验，例如继续播放、播放历史、队列和播放器设置。媒体应用不创建另一套 Resource 身份；点击资源必须能够回到同一 `/resources/library/:resourceId`。

用户消费历史不占用 `/resources/activity`。活动中心只表示后台长期工作。

## 7. 分享应用（Sharing）

**Base Route：** `/apps/sharing`

承载 Share Link、Room、Watch/Listen 等协作体验。

Share / Room 不修改底层 Resource 身份。需要长期异步处理的操作仍进入 `/resources/activity`。

## 8. 生命周期与危险操作

Resource 必须区分 Active、Archived、Trash 和永久清理。

- Archive/Trash 是业务生命周期动作；
- 永久清理必须使用高风险确认；
- 移除 Resource 与 Attachment 的关系不等于物理删除 Blob；
- 物理 GC 属于存储维护。

## 9. Advanced 规则

以下内容默认只在 Advanced / Diagnostics：Resource ID、Attachment ID / Blob ID、Checksum、Placement / Replica、内部 Metadata Candidate ID、Task ID / Attempt。

## 10. 验收

- 所有 Resource 浏览和管理从“资源 / 资源库”（`/resources/library`）进入；
- Resource Detail 使用 `/resources/library/:resourceId`；
- 文档 / 媒体 / 分享只作为“应用”下的专业体验；
- 添加资源入口统一到 `/resources/add`；
- 长任务统一进入“资源 / 活动中心”（`/resources/activity`）；
- `/resources` 仅作为一级目录根，不渲染资源库页面；
- 不再使用“内容”作为 Console 一级业务域名称；
- 不再使用历史 `/library`、`/add`、`/activity`、`/console/resources`、`resource-center` 或 `content-center` 作为 canonical 设计路由。
