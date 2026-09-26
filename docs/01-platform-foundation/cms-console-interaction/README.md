# Ikaros V2 CMS Console 交互规格

> 状态：设计基线。
>
> 本目录定义 Ikaros V2 Web Console 的最终交互设计。V2 当前按从零重构处理，文档不承担旧菜单、旧路由或旧页面结构兼容。

## 1. 设计基线

Console 必须同时遵循：

- Resource-centric：Resource 是逻辑内容身份，但 UI 优先使用动画、电影、剧集、漫画、歌曲、文档等业务语言；
- Attachment / Blob 分离：业务内容、内容身份和物理存储位置不得混为一层；
- HTTP-first：Console 是公开能力的一个客户端，不形成只能由官方前端触发的隐藏业务逻辑；
- User-owned Metadata：人工确认数据优先，外部同步不得静默覆盖；
- Task as infrastructure：后台任务是执行机制，不是全局信息架构；
- Attention-first：管理端首先帮助用户发现和解决问题，而不是展示数据库对象数量；
- Route mirrors navigation：菜单层级与路由层级一致，一个页面只拥有一个 canonical route。

全局 IA 以 [`Console-Information-Architecture-and-Product-Journey-Contract.md`](./Console-Information-Architecture-and-Product-Journey-Contract.md) 为准，产品完成门槛以 [`Console-Product-Journey-Acceptance-Contract.md`](./Console-Product-Journey-Acceptance-Contract.md) 为准。

## 2. 全局视觉与组件语言

Console 使用 Material Design 3 作为设计语言。

- 桌面后台体验优先，同时提供平板和移动响应式降级；
- 使用 Navigation Drawer / Rail、Top App Bar、Card、Data Table、Tabs、Chip、Button、Menu、Dialog、Side Sheet、Snackbar、Tooltip、Text Field、Select、Progress Indicator、Banner 和 Empty State；
- 按钮允许增加图标；按钮同时包含文字和图标时，图标优先放在文字左侧；
- 高信息密度页面可采用紧凑密度，但交互目标不得小于 44×44 CSS px；
- 主操作使用 Filled Button；次级操作使用 Filled Tonal / Outlined；低强调操作使用 Text / Icon Button；
- 高风险操作使用 Error 语义并经过确认；
- 状态不能只依赖颜色表达。

### 2.1 组件选型约束

- 优先使用已经过市场验证、维护稳定且符合现有设计语言的组件方案；
- 若需要更换既有组件方案，必须先向产品负责人说明原因、影响和备选方案，并在获得确认后实施；
- 未经确认，不得因个人偏好或局部便利替换全局组件方案。

## 3. 应用壳层

### 3.1 Navigation Drawer

用户可见 Sidebar 采用中文菜单名称，并固定为以下层级：

```text
仪表盘                                    /dashboard

资源                                      /resources/*
  资源库                                  /resources/library
  添加资源                                /resources/add
  活动中心                                /resources/activity

存储                                      /storage/*
  存储概览                                /storage/overview
  存储提供方                              /storage/providers
  存储策略                                /storage/policy
  归档管理                                /storage/archive
  备份管理                                /storage/backup
  存储维护                                /storage/maintenance

应用                                      /apps/*
  应用中心                                /apps/overview
  云盘                                    /apps/drive/**
  文档                                    /apps/documents/**
  媒体                                    /apps/media/**
  计划                                    /apps/planning/**
  财务                                    /apps/finance/**
  私密笔记                                /apps/private-notes/**
  密码库                                  /apps/passwords/**
  AI                                      /apps/ai/**
  分享                                    /apps/sharing/**
  数据分析                                /apps/analytics/**
  自动化                                  /apps/automation/**
  插件应用                                /apps/plugins/<appId>/**

系统                                      /system/*
  访问控制                                /system/access/*
    用户管理                              /system/access/users
    角色与权限                            /system/access/roles-permissions
    身份认证                              /system/access/authentication
  集成                                    /system/integrations/*
    应用管理                              /system/integrations/apps
    外部集成                              /system/integrations/external
    事件投递                              /system/integrations/events
  通知与审计                              /system/communications/*
    通知中心                              /system/communications/notifications
    审计日志                              /system/communications/audit
  平台配置                                /system/settings/*
    系统参数                              /system/settings/parameters
  运维                                    /system/operations/*
    系统健康                              /system/operations/health
    系统诊断                              /system/operations/diagnostics
```

目录节点只负责组织和展开，可以 redirect 到第一个可访问子页面，但不渲染独立业务页面。不得把同一个页面同时绑定到 `/storage` 与 `/storage/overview` 之类的两个 canonical route。

不得把后端子系统、Task 类型、Provider 类型或单个插件模块直接提升为新的一级入口。

### 3.2 Top App Bar

包含：

- 当前工作区 / 页面 Breadcrumb；
- 全局搜索入口；
- 活动中心状态入口，可显示 Running / Failed Badge；
- 通知入口；
- 当前用户头像菜单。

头像菜单固定包含个人资料、偏好、当前账号安全、主题/语言和退出。账号页面不进入 Sidebar。

### 3.3 Canonical routes

菜单层级与 canonical route tree 必须一致。一级目录使用稳定英文前缀；二级菜单页面使用二层 route；三级菜单页面使用三层 route。

目录根可以做 redirect，但不是页面。例如 `/resources` 默认重定向到 `/resources/library`，`/storage` 默认重定向到 `/storage/overview`，`/apps` 默认重定向到 `/apps/overview`。

全局 canonical route tree 见 IA 契约和 [`route-permission-matrix.md`](./route-permission-matrix.md)。当前设计不要求旧路由 redirect 或 alias。

## 4. 核心菜单职责

### 仪表盘（Dashboard）

展示 Attention、异常和正在进行的工作。Unknown 不得渲染成 Healthy。

### 资源 / 资源库（Library）

Canonical route：`/resources/library`。统一 Resource 浏览、搜索、类型视图、Collection、Tag、Lifecycle、Availability 和 canonical Resource Detail。

### 资源 / 添加资源（Add Content）

Canonical route：`/resources/add`。用“来源 → 预览 → 确认 → 执行”表达导入，不暴露 Source / Scan / Plan / Run 作为普通用户步骤。

### 资源 / 活动中心（Activity）

Canonical route：`/resources/activity`。统一 Import、Restore、Sync、Backup、Download、AI、Automation、Index rebuild 等长期后台工作。Attempt / Worker / Lease 只在 Advanced 中显示。

### 存储（Storage）

分为存储概览、存储提供方、存储策略、归档管理、备份管理、存储维护；对应 `/storage/<page>` 二层路由。Provider 配置、Tier Policy、GC 和 Placement Repair 不得混在一屏。

### 应用（Apps）

应用中心位于 `/apps/overview`；云盘、文档、媒体、计划、财务、私密笔记、密码库、AI、分享、数据分析、自动化以及插件应用等可选业务产品位于 `/apps/**`。

### 系统（System）

承载访问控制、集成、通知与审计、平台配置和运维。目录层级必须反映为 `/system/<group>/<page>`。

## 5. 标准页面结构

默认页面按以下顺序组织：

1. Breadcrumb；
2. 页面标题、职责说明、状态和主/次操作；
3. 必要时的 Attention / Context 卡片；
4. 搜索和筛选；
5. 主内容区域；
6. 分页或加载更多；
7. Context Drawer / Dialog / Side Sheet。

KPI 卡片只有在数据真实、有用户决策价值且存在明确目标页面时才使用。

## 6. 搜索与筛选

- Enter 执行搜索；实时搜索采用合理 Debounce；
- 可分享的筛选状态同步到 URL Query，私密解密内容除外；
- 高频条件使用 Filter Chip，低频条件进入更多筛选；
- 清空筛选恢复未筛选结果，不整页刷新；
- 资源库全局搜索只返回当前用户有权读取的实体；
- 完整资源搜索页面为 `/resources/library/search`。

## 7. Data Table

- 表头可 Sticky；
- 可排序列使用统一排序循环；
- 只有存在批量操作时显示行选择；
- 行点击可以进入详情，但始终保留显式 Overflow Menu；
- 宽表格支持水平滚动，不得静默隐藏关键列；
- 行操作根据实体状态和权限动态展示；
- 内部 ID 不作为默认首列，除非页面属于 Advanced Diagnostics。

## 8. 表单与并发

- 必填字段使用文字或符号明确标识；
- Blur 执行字段级校验，提交执行完整校验；
- 服务端字段错误回填到字段并显示错误摘要；
- Secret 保存后默认只展示已配置/遮罩状态；
- 未保存修改离开前确认；
- 修改实体携带版本/ETag 等并发信息；
- 409/412 不得只显示普通 Snackbar，应进入明确冲突流程。

冲突流程至少提供：查看差异、重新加载、基于最新版本继续编辑；只有后端允许且权限满足时才出现覆盖动作。

## 9. 加载、空状态与错误

每个主页面都必须实现：

- 首次加载 Skeleton；
- 后续刷新保留现有内容；
- 真正无数据 Empty State；
- 筛选后无结果状态；
- 可恢复错误的 Retry；
- 401 进入认证恢复；
- 403 解释权限不足；
- 404 用于目标不存在或策略要求隐藏存在性；
- Unknown 与 Healthy 分离。

单个仪表盘 Widget 失败不得导致整个页面失败，但该区域必须显示失败或未知。

## 10. 后台工作

所有非瞬时操作应进入统一活动中心 `/resources/activity`，例如：导入、归档、恢复、Integrity Verify、Repair、Backup、Export、同步、AI 作业和自动化执行。

业务页面发起操作后：

- 立即显示提交结果；
- 提供“查看活动中心”；
- 允许用户离开当前页面；
- 活动完成后可以返回原业务对象；
- 失败时提供业务错误摘要和安全重试入口。

不得为每个子系统创建自己的后台任务中心。

## 11. 危险操作

确认 Dialog 必须包含：

- 实体名称或数量；
- 实际后果；
- 是否可恢复；
- 依赖影响；
- 直接描述动作的危险按钮。

永久清理、覆盖恢复、密钥操作、权限撤销等可以要求 Step-up Verification 或输入实体名。

## 12. Resource / Attachment / Blob 用户语言

主界面优先展示业务资源和 Availability。

Resource ID、Attachment ID、Blob ID、Checksum、Placement、Replica、Provider Key 等进入 Advanced / Diagnostics。需要排障时可以完整展示，但不得成为日常完成任务的必经路径。

在主导航和页面标题中，`Resource` 统一称为“资源”，`Library` 统一称为“资源库”，`Add Content` 统一称为“添加资源”，`Activity` 统一称为“活动中心”。“内容”只在确实描述媒体内容、文档正文或数据载荷时使用，不再作为一级业务域名称。

## 13. 跨工作区跳转

- 仪表盘 Attention 优先进入业务对象详情；
- 资源库中的 Resource 可以进入存储状态摘要，但不直接修改物理 Placement；
- 存储维护可以链接 Resource，但重新执行目标权限；
- 应用发起的长期工作统一进入 `/resources/activity`；
- 审计日志可以链接业务对象，但不得越权展示字段；
- 云盘、私密笔记、密码库等敏感域继续执行各自的内容读取和解锁边界。

## 14. Apps 与 System 文档归属

本目录中的历史子目录继续作为领域交互文档容器，但不代表一级 Sidebar 分组：

- `content-creation/` → 资源库与部分应用；
- `attachment-storage/` → 存储；
- `personal-drive/`、`productivity-planning/`、`personal-finance/`、`private-notes/`、`password-manager/`、`ai-intelligence/`、`data-analytics/` → 应用；
- `identity-security/`、`platform-configuration/`、`communications-audit/`、`system-operations/` → 系统；
- `integration-automation/` → 应用 / 自动化 + 系统 / 集成；
- `authentication-entry/` → 登录前全局入口；
- `user-preferences/` → 头像菜单；
- `workbench/` → 仪表盘和全局搜索规则。

目录名称不等于产品导航名称。

## 15. 响应式与可访问性

- ≥1280 px：展开 Drawer，多栏布局；
- 960–1279 px：Rail 或紧凑 Drawer；
- 600–959 px：Modal Drawer；
- <600 px：单栏布局，复杂操作收入 Overflow；
- Tab 顺序遵循视觉顺序；
- Icon-only 操作必须有 Tooltip 和 Accessible Name；
- Dialog 管理焦点范围；
- 关键图表提供文字摘要或数据表替代。

## 16. 设计完成要求

任何 Console 功能都必须同时满足页面级交互规则、权限/安全边界和适用 Golden Path。不能用“接口已存在”“按钮能调用”“页面已创建”替代产品完成。
