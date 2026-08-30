# 沟通与审计 — CMS Console 交互规格

## 1. 公告

**路由：** `/console/communications/announcements`

### 页面标题区
- 标题：`公告`。
- 主操作：`新建公告`。
- 筛选：状态、受众、优先级、计划/发布时间、作者。

### 表格

列：
- 标题；
- 状态：`草稿`、`已计划`、`已发布`、`已过期`、`已归档`；
- 优先级；
- 受众摘要；
- 发布/开始时间；
- 过期/结束时间；
- 作者；
- 更新时间；
- 操作。

### 公告编辑器

字段：
- 标题：必填；
- 简短摘要；
- 正文编辑器；
- 优先级：`普通`、`重要`、策略允许时的 `紧急`；
- 受众选择器：全部用户、Role、特定用户/分组；
- 开始/发布时间；
- 过期时间：可选但建议配置；
- 是否允许关闭；
- 紧急公告可配置是否要求确认已读；
- 可选 Action Button Label + 安全的内部/外部 Destination。

Preview Mode 同时展示桌面/移动端公告效果和受众摘要。

交互：
- `保存草稿` 永远不会发布公告。
- `立即发布` 先打开 Review Dialog，展示预计受众数量、可见时间范围和确认已读行为。
- `定时发布` 校验开始时间必须早于过期时间。
- 编辑已经发布的公告时形成更新版本；有实质修改时向接收者显示 `已更新` 状态。
- 取消发布/归档前说明已经产生的阅读/确认历史是否保留。

## 2. 通知中心与投递

**路由：** `/console/communications/notifications`

Tabs：`通知中心`、`投递规则`、`投递日志`。

### 通知中心

管理端可以同时支持当前用户通知和有权限查看的系统级通知，但必须明确显示 Scope Selector：`我的` / `全部授权范围`。

筛选：未读/已读、来源子系统、优先级、日期、投递状态。

通知行字段：
- 来源图标/子系统；
- 标题；
- 根据敏感策略处理后的正文预览；
- 优先级；
- 创建时间；
- 已读状态；
- 投递渠道图标；
- 操作。

操作：标记已读/未读、归档、打开目标。支持批量已读/归档。

通知详情 Side Sheet 显示安全处理后的完整正文、目标链接、Source Event ID、创建/投递/阅读时间，以及有权限时的投递尝试记录。

Private Notes、Password Manager，以及敏感 Finance/Security 通知在受保护业务域之外必须使用通用 Redacted 文案。

### 投递规则

规则表格：Event/Category、Audience/User Scope、In-app 是否启用、已配置的 Email/Webhook/其他 Channel、Quiet Hours 行为、Priority Override、更新时间。

Rule Editor：
- Notification Category/Source；
- Delivery Channel；
- Severity Threshold；
- Digest / Immediate；
- Quiet Hours/Timezone；
- Destination/Account Selector；
- Sensitive Content Policy。

`发送测试通知` 必须明确标记为测试，并且不得包含真实 Secret/Private Content。

### 投递日志

列：Notification ID、Recipient、Channel、Destination Safe Label、Status、Attempt Count、Last Attempt、Provider Response Category、操作。

详情展示 Attempt Timeline。只有 Retry-safe 的投递才显示 `重试`。重试只重新发送既有 Notification，不重新触发底层业务 Event。

## 3. 审计与安全事件

**路由：** `/console/communications/audit`

Tabs：`审计日志`、`安全事件`。

### 审计日志

筛选：
- Actor；
- Subsystem；
- Action/Category；
- Entity Type；
- Entity ID；
- Result；
- 日期/时间范围；
- Correlation/Request ID。

表格列：
1. 时间；
2. Actor：用户/Service/System；
3. Action；
4. Subsystem；
5. Target Safe Label/Type；
6. Result；
7. Policy 允许时显示 Source/Device/Network 摘要；
8. Correlation ID；
9. 查看操作。

Audit Detail Side Sheet：
- 不可修改的 Audit ID；
- Timestamp/Timezone；
- Actor Identity 与 Authentication Context；
- Action/Capability；
- Target Reference；
- Policy 允许时显示 Before/After Summary 或 Changed Field Name；
- Request/Correlation ID；
- Source Metadata；
- Result/Error Category；
- 关联 Execution/Job/Security Event。

敏感字段值必须 Redact。审计记录可以记录“某个受保护字段发生修改”，但不得记录 Secret 本身。

导出审计日志要求明确日期范围和筛选，并显示预计 Row Count；根据权限策略可以要求重新认证。导出元数据包括筛选条件和生成时间。

### 安全事件

Security Event Card/Table 按 Severity 优先排列。

列：
- Severity；
- Event Type；
- 受影响用户/Service；
- 摘要；
- 检测时间；
- 状态：`待处理`、`调查中`、`已解决`、`已忽略/误报`；
- 操作。

详情页区域：
- 事件摘要；
- Timeline；
- 根据 Policy 展示关联 Session/Device/IP/Network Metadata；
- 关联 Audit Event；
- 推荐响应操作；
- Resolution Note。

响应操作可以包括撤销 Session、禁用账户、轮换 Credential、检查 Plugin/Connector。具体操作应跳转到所属子系统的标准流程，而不是在审计页面重复实现安全修改逻辑。

修改 Security Event State 时，根据 Severity 要求可选/必填 Resolution Note。关闭 Critical Event 前要求确认已经审阅 Remediation。

## 通用规则
- Audit Record 在普通 CMS UI 中不可修改。
- Notification 的 Read/Archive State 可以修改，但不等于删除 Source Event。
- Security Severity 统一使用 `Info`、`Low`、`Medium`、`High`、`Critical` 或 Backend 最终确定的规范集合，并同时使用文字、图标、颜色。
- External Delivery Destination 使用安全/遮罩形式展示。
- Audit/Security Event 中跳转目标实体时重新校验权限；目标已删除或不可访问时显示 `已不可用`，不得通过旧记录泄露正文。
