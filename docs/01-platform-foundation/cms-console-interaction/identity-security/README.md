# 身份与安全 — CMS Console 交互规格

## 1. 用户与角色

**路由：** `/console/security/users`

### 页面标题区
- 标题：`用户与角色`。
- 主操作根据部署策略显示 `邀请用户` 或 `创建用户`。
- 筛选：状态、角色、MFA 状态、最近活动时间、账户类型。

### 用户表格

列：
- 头像/显示名称；
- 用户名/登录身份；
- 有权限时显示 Email/联系方式；
- 账户状态：`正常`、`已禁用`、`已锁定`、`待激活`；
- Role 摘要；
- MFA 状态；
- 最近活动；
- 创建时间；
- 操作。

点击行打开用户详情。

### 用户详情

标题区：头像、显示名称、用户名、状态 Chip。操作：编辑、禁用/启用、重置/要求凭据操作、Overflow。

Tabs：
- `概览`。
- `角色与权限`。
- `认证与设备安全`。
- `安全事件`。
- `活动/审计引用`。

概览字段：User ID、账户类型、创建时间、最近登录、Locale/Timezone、账户状态，以及 Policy 允许展示的 Profile Field。

角色 Tab 展示 Direct Role 和 Inherited/Effective Permission。`分配角色` 使用可搜索 Multi-select，并展示角色描述和风险级别。

`认证与设备安全` 只展示可持久化的账号安全事实，例如 MFA 配置、Trusted Device（未来启用时）、最近高风险验证摘要和当前 `security_version`。不得构造 Login Session / Security Session 列表，也不得显示 Session ID、单设备登录状态或“撤销某个 Session”。

Backend Policy 必须阻止把当前唯一管理员账户的最后一个管理员等价角色移除；前端在操作区域解释为什么不可执行。

禁用账户 Dialog 明确说明：
- 账号将不能继续通过认证；
- 按 Security Policy 提升 `security_version` 后，此前签发的 JWT 会失效；
- API Credential / Automation 是否受影响由对应独立凭据与策略决定；
- **禁用账户不会删除数据。**

### Role 列表 / 编辑器

Role 表格：名称、描述、用户数、Permission 数、System/Custom、更新时间。

创建/编辑字段：名称（必填）、描述、Permission Set、可选继承关系。System Role 可以只读。

删除 Custom Role 前必须预览受影响用户，并要求完成重新分配或明确移除。

## 2. 权限矩阵

**路由：** `/console/security/permissions`

### 布局

顶部选择器：按 `Role` / `User` 查看。提供 Capability/Resource 搜索框。

主矩阵按子系统分组。列可以是 `查看`、`创建`、`编辑`、`删除`、`管理`，也可以根据最终 Authorization Model 使用明确 Capability Key。

每组可折叠。高风险 Capability 显示 Warning 图标，并通过 Tooltip 说明作用范围。

交互：
- Checkbox 修改先进入 Staged 状态，不立即保存。
- Group Header Checkbox 选择当前组所有可编辑权限；部分选中显示 Indeterminate。
- 继承得到的只读 Permission 使用 Lock/Inheritance 图标，并显示来源 Role。
- 存在修改后底部显示 Sticky Action Bar：`放弃修改`、`审阅修改`。

Review Dialog 按风险分组展示新增/删除权限。提升到安全管理、密钥管理、插件管理、私密数据管理等高风险权限时可以要求重新认证。

Effective Permission Inspector 解释权限来源：Direct、Role、Inherited、Owner-specific，或被 Policy Deny。

## 3. 认证、Token 安全、密钥与恢复

**路由：** `/console/security/authentication`

Tabs：`认证策略`、`Token 安全`、`密钥与 Secret`、`恢复`、启用时的 `OAuth/API 访问`。

> 平台登录认证使用无状态 JWT。服务端不建立或持久化 Login Session / Security Session，所以 CMS 不提供 `/console/security/sessions`、活跃 Session 表、Session Detail 或单 Session 撤销操作。

### 3.1 认证策略

Card/Form 分组：
- Password/Login Policy；
- MFA 要求；
- JWT Access / Refresh Token TTL；
- Login Rate Limit/Lockout；
- Trusted Device Policy（未来启用时，作为独立安全对象）；
- External Identity Provider。

每个 Policy Field 显示当前 Effective Value 和简短影响说明。`保存策略` 先显示 Diff Review，并可要求重新认证。

Token TTL 只控制签名 Token 的有效期，不等同于 Session Lifetime / Idle Timeout。

### 3.2 Token 安全

页面展示的是用户级 Token 安全状态，而不是 Session 列表。

管理员可查看：
- 用户当前 `security_version`；
- 账号状态；
- 最近 Token Invalidation 安全事件；
- JWT Signing Key 当前版本 / 状态摘要（只读链接到密钥管理）；
- Security Policy 是否会在密码修改、账号禁用等动作后自动提升 `security_version`。

高风险操作：
- `使该用户所有旧 Token 失效`。
- 具备平台级权限时可对明确用户集合批量执行 Token Invalidation，但必须显示影响数量并要求 Step-up Verification。

操作语义：

```text
identity.invalidate-user-tokens
        ↓
increment user.security_version
        ↓
all JWTs carrying the previous security_version are rejected
```

不提供：
- 单 Token / 单 Session 撤销；
- 当前 Session Chip；
- 活跃用户数从 Session 表推导；
- Session IP / User-Agent / Last Seen 列表。

### 3.3 密钥与 Secret

按用途分表：Signing/Encryption Key、API Credential、Integration Secret Reference。Secret Material 绝不能与普通设置混在一起展示。

Key 行字段：Key Name/ID、用途、Algorithm/Type、状态（`Active`、`Retiring`、`Revoked`）、创建时间、过期/轮换时间、最近使用、操作。

`轮换密钥` Wizard：
1. 选择 Key/用途。
2. 展示受影响 Service/Data。
3. 支持时配置 Overlap/Grace Period。
4. 生成/导入新 Key。
5. 校验就绪状态。
6. 激活。
7. 需要时跟踪 Re-encryption/Re-signing 后台任务。

Private Key/Secret 生成后默认永不再次显示，除非产品明确设计了一次性导出流程。

### 3.4 恢复

展示管理员/系统和各 Protected Vault Domain 的 Recovery Readiness，但不展示 Recovery Material 本身。

操作：生成/轮换 Recovery Code/Material、验证恢复能力、使旧 Recovery Set 失效。一次性 Code 展示页面要求用户明确确认已安全保存。

### 3.5 OAuth/API 访问

Client/Token 表格：Client Name、Owner、Scope、Created、Last Used、Expiry、Status。

这里的 OAuth/API Token 是独立 Credential，不等同于用户登录 Session。

创建 Token 时先审阅 Scope，创建结果中的 Secret **只展示一次**。撤销立即生效且不可撤销。

## 通用安全行为
- 所有安全敏感修改都产生 Audit Event。
- Re-authentication / Step-up Dialog 必须说明为什么需要重新认证，并在成功后恢复用户刚才待执行的操作。
- Step-up 成功后使用短期、Purpose-bound Verification Grant，不创建 Security Session。
- Permission Denied 页面不能泄露隐藏 Secret Field。
- ID 可以通过显式操作复制，但敏感标识在 Table 中默认缩短显示。
- Security Event Severity 必须同时使用文字、图标和颜色表达。
