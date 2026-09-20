# ADR-006：App Client Authorization Grant 与 Token Binding

| 项目 | 内容 |
|---|---|
| 状态 | Accepted |
| 日期 | 2026-09-19 |
| 关联决策 | ADR-003、ADR-004、ADR-005 |
| 影响范围 | Authentication、Authorization、App Runtime、JWT Claim、Client Registration、Device、Audit、P0 Schema |

## 背景

ADR-003 确立了 Ikaros V2 的无状态 JWT 基线：

- 不持久化 Login Session / Security Session；
- 不持久化 Access / Refresh Token 原文或 Digest；
- 不维护基于 `jti` 的 Token 黑名单；
- 用户级紧急失效通过 `security_version` 完成。

ADR-005 又引入了独立 Client App：

```text
User
  ↓
Client App on Device
  ↓
Server App
```

每个 Client App 必须拥有独立 `client_id`、独立 Scope 和可独立撤销的授权。若仍只依赖用户级 `security_version`，撤销一个 Anime Client 会同时使 Photos、Drive、Ikaros 管理客户端等所有 Token 失效；这不满足 Client App 隔离要求。

本 ADR 引入 **AppAuthorizationGrant** 作为用户对 Client App 的授权事实，但不重新引入登录 Session 或 Token Persistence。

## 决策

### 1. Authorization 拥有 AppAuthorizationGrant

`authorization` 是 AppAuthorizationGrant 的唯一业务 Owner。

逻辑模型：

```text
AppAuthorizationGrant
├── grant_id
├── subject_id
├── client_id
├── app_id / audience
├── device_id (optional)
├── granted_scopes
├── grant_version
├── refresh_generation
├── refresh_last_used_at (optional)
├── status
├── granted_at
├── updated_at
└── revoked_at (optional)
```

其中：

- `subject_id`：被授权用户；
- `client_id`：Client App identity；
- `app_id`：目标 Server App；
- `device_id`：需要设备级隔离时绑定的 Device identity；
- `granted_scopes`：用户实际同意且当前仍有效的 App Scope；
- `grant_version`：授权版本，用于使旧 Access / Refresh Token 立即失效；
- `refresh_generation`：Public Client Refresh Token Rotation 的单调代际，不是 Token ID；
- `refresh_last_used_at`：最近一次成功 Rotation 时间，可用于 inactivity policy；
- `status`：至少区分 ACTIVE / REVOKED；
- Grant 是“授权关系”，不是“登录会话”。

不得向 Grant 表写入：

- Access Token 原文；
- Refresh Token 原文；
- Token Digest；
- `jti` 列表；
- 浏览器 / App 的临时登录状态；
- 每个 HTTP 请求的在线状态。

### 2. App Runtime 拥有 Client Registration 与 Scope Definition

`app-runtime` 拥有：

```text
AppClientRegistration
AppScopeDefinition / App Scope Registry
```

Authorization 通过公开 App Runtime Capability 获取：

- client 是否存在且启用；
- client 对应的目标 app；
- client type；
- redirect URI / platform metadata；
- Server App 声明的合法 scope。

Authorization 不复制 App Registry 作为自己的业务真相。

### 3. Authentication 负责协议与 Token 签发

Authentication 负责：

- Authorization Code；
- PKCE validation；
- Access / Refresh Token 签发；
- Token signature / standard claims；
- 调用 Authorization 取得 Grant Snapshot。

Authentication 不直接读写 Authorization Repository。

Authorization 应通过 `authorization-api` 暴露公开 Capability，例如：

```text
AppAuthorizationGrantQuery.activeGrant(...)
AppAuthorizationGrantCommand.grant(...)
AppAuthorizationGrantCommand.revoke(...)
```

具体 Java 接口名可在实现阶段冻结，但 Owner Boundary 不得改变。

### 4. App-scoped Token 必须绑定 Grant

面向 Server App 的 Access / Refresh Token 至少需要表达：

```text
iss
sub = subject_id
jti
token_type
security_version
client_id
aud = target app_id
scope
authorization_grant_id
authorization_grant_version
refresh_generation (refresh token only)
device_id (when device-bound)
iat
exp
```

其中：

- `client_id` 不能由请求方在业务调用时任意覆盖；
- `aud` 必须与目标 Server App 匹配；
- Token 中的 `scope` 必须是 Grant 当前授权 Scope 的子集；
- `authorization_grant_version` 必须与当前 ACTIVE Grant 一致。

### 5. Client / Device 撤销不使用 Token 黑名单

撤销一个 Client App 或某个 Device 的授权：

```text
revoke AppAuthorizationGrant
        ↓
status = REVOKED
grant_version++
        ↓
old Access / Refresh Token grant binding invalid
```

请求认证 / 授权链必须校验：

```text
user.status allows authentication
AND token.security_version == user.security_version
AND grant.status == ACTIVE
AND token.authorization_grant_version == grant.grant_version
AND token.client_id == grant.client_id
AND token.aud == grant.app_id
AND token.scope ⊆ grant.granted_scopes
AND device binding matches when required
```

因此无需：

- Token blacklist；
- 持久化 `jti`；
- 恢复 `security_session`；
- 为每个 Token 建立数据库行。

### 6. User-wide Revocation 与 Client Revocation 分层

两种撤销语义并存：

#### 用户级紧急失效

```text
increment platform_user.security_version
```

影响该用户全部旧 Token。

适用于：

- 账号疑似被盗；
- 密码 / Credential 高风险变更；
- 管理员禁用用户；
- 用户主动“退出所有设备”。

#### Client / Device 级撤销

```text
revoke AppAuthorizationGrant
```

只影响对应：

```text
subject + client + app + optional device
```

其他 App / Device 的 Grant 不受影响。

### 7. Refresh Token 仍不建立 Token Session

Refresh Token 可以继续使用签名 JWT，但必须绑定同一 Authorization Grant。

依据 RFC 9700，Public Client 若签发 Refresh Token，必须具备 replay detection。V2 P0 采用 **Refresh Token Rotation**，而不是创建 Token Row / Digest / `jti` blacklist。

Refresh JWT 额外携带：

```text
authorization_grant_id
authorization_grant_version
refresh_generation
```

初次授权时：

```text
grant.refresh_generation = 0
```

每次 Refresh 必须重新校验：

- User status；
- `security_version`；
- Client registration；
- Server App availability；
- Grant status / version；
- 当前 granted scopes；
- Token `refresh_generation == grant.refresh_generation`。

成功 Refresh 使用 compare-and-swap 原子执行：

```text
grant.refresh_generation++
grant.refresh_last_used_at = now()
```

然后签发新 Access Token 与新一代 Refresh Token。

如果已经使用过的旧 Refresh Token 再次出现：

```text
token.refresh_generation < grant.refresh_generation
```

视为 Refresh Token replay。Server 无法可靠区分攻击者与合法 Client，因此撤销对应 Grant、提升 `grant_version`，要求重新完成 Authorization。

刷新成功后签发的 Access Token 不得扩大到 Grant 未授权 Scope。

Grant 已撤销时 Refresh 必须失败。

`refresh_generation` 是 Grant-owned replay state，不是 per-token Session/Persistence；仍然不保存 Refresh Token 原文、Digest 或 `jti`。

### 8. Grant 不是 OAuth Login Session

以下状态不进入 AppAuthorizationGrant：

- Authorization Code；
- PKCE verifier / challenge 的长期状态；
- 登录页面临时状态；
- CSRF / state；
- OTP challenge；
- Step-up challenge。

这类短生命周期协议状态由 Authentication / Verification 对应契约管理，并具有独立 TTL。

AppAuthorizationGrant 只表达“用户当前允许某 Client 以哪些 Scope 访问哪个 Server App”。

### 9. Official Client 不特殊授权

`official=true`、可信 Publisher、预注册 Redirect URI 等只能影响：

- 注册信任；
- UI 标识；
- 安装 / 注册流程；
- 风险策略。

不得跳过：

- Grant；
- Scope；
- Audience；
- Object Policy；
- User Permission。

### 10. Audit

以下操作必须写 Platform Audit：

- Grant 创建；
- Scope 增加 / 缩减；
- Grant revoke；
- Device-bound Grant revoke；
- 管理员代用户撤销（如允许）。

Audit 记录 Grant identity 与 actor / subject / client / app / device reference，不记录 Token 原文。

## 与 ADR-003 的关系

ADR-003 继续有效，以下原则不变：

- 无 Login Session / Security Session；
- 无 Token Digest Persistence；
- 无 `jti` blacklist；
- JWT 不使用 `sid` 作为登录状态主键；
- User-wide revocation 仍使用 `security_version`。

本 ADR 补充：

> AppAuthorizationGrant 是 Authorization Domain 的授权事实，不是 Authentication Session。App-scoped Token 在验证时除了用户级 `security_version`，还必须校验 Grant status / version。

## 与 ADR-004 的关系

ADR-004 的 User Permission Snapshot 继续表达平台用户权限。

App Scope 不是 Platform Permission。

面向 Server App 的有效授权至少由以下部分共同决定：

```text
User Permission
∩ AppAuthorizationGrant Scope
∩ Client / App availability
∩ Server App Policy
∩ Target Object Policy
```

Authentication 可以在签发 App-scoped Token 时分别获取：

- User Permission Snapshot；
- App Authorization Grant Snapshot。

不得把二者合并成一个无来源区分的 permission list。

## 验证

后续实现至少验证：

1. 撤销 Anime iOS Grant 后，Anime iOS 旧 Access / Refresh Token 被拒绝。
2. 同一用户 Photos Client 的 Grant 不受影响。
3. 同一 Client 不同 Device 可在启用 device-bound grant 时分别撤销。
4. Grant revoke 不写 Token blacklist。
5. `jti` 不持久化。
6. User `security_version` 增加后，该用户所有 Grant 对应的旧 Token 均失效。
7. Token `aud` 与目标 Server App 不匹配时拒绝。
8. Token Scope 超出当前 Grant Scope 时拒绝。
9. Official Client 与 Third-party Client 使用相同 Grant / Scope enforcement。
10. Grant 已撤销时 Refresh 失败且不能换取新 Access Token。
