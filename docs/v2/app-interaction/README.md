# Ikaros V2 App 页面布局与交互设计

> 本目录定义 Ikaros V2 官方客户端 App 的页面信息架构、界面布局、字段、组件、状态与交互规则。
>
> **文件名与目录名使用英文，文档内容使用中文。**
>
> 本文档不是对 V1 Flutter App 的翻版，也不是把 CMS 页面缩小后搬到移动端。设计基线来自 `docs/v2/` 下的 V2 PRD 与各子系统设计文档；旧客户端仅可作为历史交互经验参考。

---

## 1. 设计目标

App 面向 Desktop 与 Mobile，暂定 Flutter 技术栈，并统一采用 Material Design 3。

客户端重点承担：

- 统一资源浏览、搜索、收藏与消费。
- 视频、音乐、漫画、小说、图片等内容体验。
- 普通笔记、文档、文章的查看与轻量创作。
- Productivity / Planning 高频使用场景。
- Personal Finance 的日常记录与查看。
- Private Notes 与 Password Manager 的安全本地体验。
- AI Assistant、Persona、用户级 AI 隐私与 Memory 管理。
- 个人 Analytics / Insights。
- 分享、Room、评论、通知等用户协作能力。
- Automation、Import / Sync 的用户级配置与状态查看。
- Download、Cache、Offline-first 与本地设备能力。
- 账号、安全、外观、通知、本地存储与客户端设置。

管理员专属的以下能力不进入普通 App 主菜单：

- 用户 / 角色 / 平台权限管理。
- 平台参数、字典、后台菜单配置。
- 系统级审计日志。
- Scheduled Job 管理。
- 服务健康、运行时指标、系统告警详情。
- Storage Provider 管理、Blob GC 策略等系统运维页面。

这些能力属于 CMS Console。若某项状态与用户当前操作直接相关，App 只展示必要的只读状态与操作反馈，例如“归档恢复中”“同步失败”“后台任务进度”。

---

## 2. V2 文档映射

| V2 设计来源 | App 设计落点 |
|---|---|
| `Product-Requirements-Document.md` | 全局信息架构、Library、Media、Reading、Music、Photo、Document、Game、Search、Activity、Share、Room、Download、Offline |
| `Productivity-Planning-Subsystem-Design.md` | Today、Inbox、Task、Project、Calendar、Goal、OKR、Habit、Focus、Review |
| `Personal-Finance-Accounting-Subsystem-Design.md` | 财务首页、账户、交易、预算、周期账、对账、导入 |
| `Private-Notes-Subsystem-Design.md` | 私密保险库、Notebook、笔记、编辑、版本、冲突、加密导出与恢复 |
| `Password-Manager-Subsystem-Design.md` | 密码保险库、条目、TOTP、生成器、健康检查、共享与设备解锁 |
| `AI-Intelligence-Subsystem-Design.md` | AI Assistant、上下文、工具确认、Memory、AI Job、结果来源与反馈 |
| `AI-Persona-System-Design.md` | Persona 选择、用户覆盖、会话人格切换、场景化表达设置 |
| `Data-Analytics-Statistics-Subsystem-Design.md` | 个人内容、消费、效率、创作、财务与存储使用洞察 |
| `Platform-Integration-Automation-Design.md` | 用户 Automation、执行记录、Import / Sync、Activity、跨对象跳转 |
| `Security-Identity-Authorization-Crypto-Subsystem-Design.md` | 登录、Step-up Verification、会话、安全设置、恢复流程 |
| `Secure-Data-Foundation-Design.md` | Secure Domain 锁定、解锁、密文缓存、安全预览、导出边界 |
| `Platform-Administration-Operations-Subsystem-Design.md` | 仅引用通知、用户自己的活跃会话等用户级能力；管理员页面仍留在 CMS |

---

## 3. 全局导航模型

### 3.1 主导航原则

完整菜单按子系统分组，**所有分组初始默认收起**。

用户展开某一组后，同一时间允许多个分组保持展开；提供“全部收起”动作。通过 Deep Link 直接进入子页面时，允许自动展开当前页面所属分组，以便用户理解当前位置。

### 3.2 菜单分组

#### 首页与发现

- 首页
- 全局搜索
- 我的活动
- 通知中心

#### 内容库

- 统一资源库
- Collection / 标签
- 视频与影视
- 漫画与小说
- 音乐
- 图片与相册
- 文章与文档
- 游戏与数字资料

#### 计划与生活

- Today / Inbox
- Task / Project
- Calendar
- Goal / OKR
- Habit / Focus / Review
- 个人财务

#### 私密与安全

- 私密笔记
- 密码管理

#### AI 与洞察

- AI Assistant
- Persona
- AI Memory / Privacy
- 数据洞察

#### 协作与自动化

- 分享
- Room
- Automation
- Import / Sync

#### 本地与设备

- 我的下载
- 离线内容
- 缓存与空间

#### 我的

- 个人资料
- 安全与会话
- 通知偏好
- 外观与可访问性
- 客户端设置
- 关于

---

## 4. 响应式布局分级

Flutter 使用 `LayoutBuilder` / `MediaQuery` 基于可用宽度决定布局，而不是判断具体设备型号。

| Size Class | 宽度 | 主要导航 | 内容布局 |
|---|---:|---|---|
| Compact | `< 600dp` | Bottom Navigation + Modal Navigation Drawer | 单列；详情页覆盖式进入 |
| Medium | `600–839dp` | Navigation Rail + Drawer | 1–2 列；列表/详情可选择分栏 |
| Expanded | `840–1199dp` | Permanent Side Navigation | 主内容 + 可选右侧详情栏 |
| Large | `>= 1200dp` | Permanent Side Navigation | 主内容最大宽度 + 右侧上下文栏 / 多栏 |

### 4.1 Compact 底部导航

底部只放 5 个最高频入口，不尝试承载全部系统菜单：

1. 首页
2. 资源库
3. Today
4. AI
5. 我的

完整子系统入口通过左上角菜单按钮打开 `NavigationDrawer`。Drawer 内严格按上文子系统分组，默认收起。

### 4.2 Medium

左侧显示窄 `NavigationRail`：首页、资源库、Today、AI、我的 + 菜单按钮。点击菜单按钮打开分组 Drawer。

### 4.3 Expanded / Large

左侧固定 260–300dp 侧边栏：

- 顶部 Ikaros 标识、当前服务端名称、连接状态。
- 中部可滚动分组菜单。
- 底部用户头像、设备离线状态、设置入口。

分组标题行包含：图标、组名、可选未读/异常徽标、展开箭头。

---

## 5. 全局 App Bar

普通一级页面顶部 App Bar 从左到右：

- Compact：菜单按钮 / 返回按钮。
- 页面标题。
- 可选上下文副标题，例如当前 Ledger、Vault、Collection。
- 搜索图标（支持该页面时）。
- 同步状态 / 离线状态图标（仅状态异常时常驻）。
- 页面主操作，例如“新增”“更多”。

Desktop Expanded 模式下，App Bar 与内容区对齐，不重复显示侧栏已有的全局菜单按钮。

---

## 6. 全局搜索入口

支持：

- App Bar 搜索按钮。
- Desktop `Ctrl/Cmd + K`。
- Mobile 首页搜索框。

搜索结果分区：Resource、Collection、Document、Task、Goal、Finance 可搜索对象、AI Conversation、已解锁 Secure Domain 本地结果。

Private Notes / Password Manager 未解锁时不得泄露实际标题，只显示安全占位，例如“私密笔记中存在匹配结果，解锁后查看”。

---

## 7. 全局状态语义

每个页面必须处理以下状态，禁止只实现“成功 + 无限转圈”。

### 7.1 Loading

- 首屏使用 Skeleton，尽量保持最终布局尺寸。
- 仅局部刷新时只对局部区域显示进度，不清空整页。
- 用户提交写操作时主按钮进入 Loading，避免重复提交。

### 7.2 Empty

空状态必须包含：

- 图标或轻量插图。
- 明确原因。
- 一个主操作。
- 必要时一个次操作。

例如“还没有下载内容” + “去资源库”。

### 7.3 Error

错误卡片至少包含：

- 用户可理解的错误标题。
- 简短原因。
- 重试按钮。
- 可选“查看详情”展开技术信息。

### 7.4 Offline

顶部使用非阻断式 Offline Banner：

- 显示“离线”。
- 说明哪些能力仍可用。
- 可进入“待同步变更”页。

页面中已缓存 / 已下载数据继续可用。

### 7.5 Permission Denied

不显示空白页。展示：

- 无权限图标。
- 缺少的能力描述。
- 可选请求访问 / 返回按钮。

### 7.6 Locked

Private Notes / Password Manager 统一使用 Locked Surface：

- Vault 名称（如果名称本身允许显示）。
- 锁图标。
- 解锁方式。
- 本地缓存状态。
- 不显示任何解密后的列表内容。

### 7.7 Processing / Restoring

Resource / Attachment 状态可能为：Available、Cached、Remote、Processing、Restoring、Missing、Corrupted。

UI 必须使用状态 Chip + 解释文本。例如：

- `Restoring`：显示恢复进度、预计完成信息（如果服务端提供）、取消按钮（如果可取消）。
- `Missing`：显示“内容元数据存在，但当前没有可用副本”。
- `Corrupted`：使用高风险视觉提示，禁止伪装成普通播放失败。

---

## 8. 通用 Resource Card

资源卡片用于 Library、搜索、Collection、推荐等页面。

### 8.1 海报型

字段顺序：

1. 16:9 / 2:3 / 1:1 按资源类型选择的封面。
2. 左上角可选类型 Chip。
3. 右上角收藏按钮。
4. 封面底部可选消费进度条。
5. 主标题，最多 2 行。
6. 副标题：年份 / 创作者 / 资源类型的关键字段。
7. 可用状态：Cached / Remote / Restoring 等，仅非普通 Available 时显示。

### 8.2 列表型

左侧缩略图，中间标题与元信息，右侧状态 / 进度 / 更多菜单。

### 8.3 交互

- 单击 / Tap：进入 Resource Detail。
- Desktop 右键：快速菜单。
- Mobile 长按：Bottom Sheet 快速菜单。
- 快速菜单：收藏、加入 Collection、下载、分享、相关 Task、更多。
- 不允许长按默认直接执行破坏性操作。

---

## 9. 通用表单规则

- 必填字段以 `*` 与辅助说明表达，不只靠颜色。
- 保存按钮在内容未修改时 Disabled。
- 离线可编辑页面保存为本地 Pending Change，并明确显示“待同步”。
- 高风险动作需要二次确认；极高风险动作还需 Step-up Verification。
- 删除与“移入回收站”必须区分。
- 可恢复操作优先提供 Snackbar Undo。

---

## 10. Flutter 组件映射建议

| 交互语义 | Flutter / Material 3 建议 |
|---|---|
| App Shell | `Scaffold` / 自适应 Shell |
| 底部导航 | `NavigationBar` |
| 中宽导航 | `NavigationRail` |
| 完整菜单 | `NavigationDrawer` + 自定义可折叠 Group |
| 顶栏 | `AppBar` / `SliverAppBar` |
| 卡片 | `Card` / `Card.filled` / `Card.outlined` |
| 筛选 | `FilterChip` / `ChoiceChip` / `SegmentedButton` |
| 菜单 | `MenuAnchor` / `PopupMenuButton` |
| 移动操作面板 | `showModalBottomSheet` |
| 确认 | `AlertDialog` |
| 提示 | `SnackBar` |
| 进度 | `LinearProgressIndicator` / `CircularProgressIndicator` |
| Tab | `TabBar` |
| 搜索 | `SearchAnchor` / `SearchBar` |
| 日期范围 | `DatePicker` / 自定义 Calendar Sheet |

具体实现允许封装自有组件，但语义应保持 Material 3 一致。

---

## 11. 页面目录

### Foundation

- [App Shell、导航与响应式](./foundation/app-shell-navigation-responsive.md)

### Access

- [登录、服务端连接、安全与会话](./access/authentication-security.md)

### Home

- [首页、全局搜索、活动](./home/home-search-activity.md)

### Library

- [统一资源库、Resource 详情、Collection、标签与关系](./library/resource-library.md)

### Video

- [动画、影视、视频与播放器](./video/video-media.md)

### Reading

- [漫画、小说与阅读器](./reading/comic-novel-reading.md)

### Music

- [音乐库与播放器](./music/music-library-player.md)

### Photos

- [图片与相册](./photos/photo-album.md)

### Documents

- [文章、普通笔记、文档与轻量创作](./documents/document-creation.md)

### Games

- [游戏与数字资料](./games/game-digital-assets.md)

### Productivity

- [效率与计划](./productivity/productivity-planning.md)

### Finance

- [个人财务](./finance/personal-finance.md)

### Private Notes

- [私密笔记](./private-notes/private-notes.md)

### Password Manager

- [密码管理](./password-manager/password-manager.md)

### AI

- [AI Assistant、Persona、Memory 与隐私](./ai/ai-assistant-persona.md)

### Analytics

- [个人数据洞察](./analytics/analytics-insights.md)

### Collaboration

- [分享、Room 与评论](./collaboration/sharing-room-comments.md)

### Automation

- [Automation、Import / Sync 与集成](./automation/automation-sync-integrations.md)

### Offline

- [下载、缓存与离线](./offline/downloads-cache-offline.md)

### Notifications

- [通知中心](./notifications/notification-center.md)

### Account

- [个人资料与客户端设置](./account/profile-settings.md)

---

## 12. App 与 CMS 的职责边界

当同一业务对象同时存在 App 与 CMS 页面时：

- App 优先“消费、个人操作、轻量编辑、用户自己的安全与数据”。
- CMS 优先“平台管理、批量治理、系统配置、全局运维、管理员审计”。
- 两端使用同一业务 API 语义，不因为官方 App 而创造私有业务逻辑。

例如：

- App 可查看自己的 Automation Rule；CMS 可管理系统级 Rule。
- App 可查看自己的 Session 并撤销；CMS 可查看有权限的系统会话治理页。
- App 可查看自己的下载与缓存；CMS 管理 Storage Provider、Placement、GC。
- App 可选择 Persona；CMS 管理 Persona 定义与发布版本。

---

## 13. 文档维护规则

新增 App 页面时必须：

1. 在本目录根页面目录登记。
2. 放入对应子系统目录。
3. 写明页面目的、入口、布局、组件、字段、状态、交互与响应式变化。
4. 写明安全边界与离线语义（如果适用）。
5. 如果页面依赖 V2 设计新增业务能力，应先更新对应 V2 产品 / 子系统文档，不在 UI 文档中偷偷创造新的业务事实。
