# Ikaros V2 App Discovery / Client Authorization Protocol Design

| 项目 | 内容 |
|---|---|
| 适用版本 | Ikaros V2 |
| 状态 | Draft / Protocol Freeze Candidate |
| 上位决策 | ADR-005、ADR-006 |
| 关联设计 | Security / Identity / Authorization / Crypto、API Convention、App Runtime / Identity / Client Architecture |

> 本文档定义独立 Client App 如何发现 Ikaros Instance、发现目标 Server App、注册/识别 Client、通过 Authorization Code + PKCE 获得 App-scoped Token，以及 Ikaros 管理客户端如何作为认证 Broker 改善体验。
>
> Server 始终是 Token Issuer。Ikaros 管理客户端不得把自己的 Token 复制给业务 Client。

---

## 1. 目标

协议支持：

- 多个自托管 Ikaros Instance；
- 独立官方和第三方 Client；
- Native / Browser Public Client；
- Server Confidential Client；
- App Discovery / API compatibility；
- Scope consent；
- Client / Device 独立撤销；
- Ikaros 管理 App 快速认证 Broker；
- 无 Broker 时系统浏览器授权；
- Token 不共享；
- User-wide 与 Client-specific revoke 分离。

## 2. 身份

一次业务调用至少区分：

```text
instance_id
subject_id
app_id
client_id
device_id (optional)
authorization_grant_id
```

`device_id` 不是 `client_id`；同一个 Client 可运行在多台设备。

## 3. Client 类型

### PUBLIC_NATIVE

iOS / Android / Desktop：

- 不持有可信静态 client secret；
- PKCE S256 REQUIRED；
- Redirect URI exact match；
- Universal/App Link 优先，自定义 scheme 作为受控 fallback。

### PUBLIC_BROWSER

Browser / PWA：

- 无可信 client secret；
- PKCE S256 REQUIRED；
- Redirect URI exact match；
- CORS / SameSite 另行配置。

### CONFIDENTIAL_SERVER

服务器侧 Client 可持有 Secret / private-key credential，不适用于 APK / IPA / Desktop binary。

## 4. Instance Discovery

目标入口：

```http
GET /.well-known/ikaros
```

建议响应：

```json
{
  "instance_id": "019...",
  "issuer": "https://ikaros.example.com",
  "platform_api_version": "2",
  "authorization_endpoint": "https://ikaros.example.com/api/auth/authorize",
  "token_endpoint": "https://ikaros.example.com/api/auth/token",
  "revocation_endpoint": "https://ikaros.example.com/api/auth/revoke",
  "app_registry_endpoint": "https://ikaros.example.com/api/app-registry/apps",
  "pkce_methods_supported": ["S256"]
}
```

最终路径必须同步进入 OpenAPI 与 HTTP Operation Registry 后才成为实现契约；本文冻结字段与语义。

Discovery 无需登录，只返回连接必要公开元数据。默认安全假设是客户端信任的 HTTPS Origin。

## 5. App Discovery

目标操作：

```http
GET /api/app-registry/apps/{app_id}
```

响应至少表达：

```json
{
  "app_id": "run.ikaros.anime",
  "name": "Ikaros Anime",
  "state": "ENABLED",
  "package_version": "1.4.0",
  "api_majors": [1],
  "scopes": [
    "anime.library.read",
    "anime.playback",
    "anime.progress.write"
  ],
  "capabilities": [
    "library",
    "playback"
  ]
}
```

Client 必须能区分：

```text
app.not-installed
app.disabled
app.incompatible
app.api-version-unsupported
```

Discovery 不等于 Authorization。

## 6. Client Registration Policy

官方 Client 可通过 deterministic seed / trusted package metadata 预注册：

```text
client_id
app_id
client_type
publisher
redirect_uris
official=true
```

官方身份不自动获得 Scope。

第三方首版采用：

```text
administrator-approved registration
```

不开放匿名 Dynamic Client Registration。管理员审核 client_id、publisher/name、target app_id、client type、redirect URI。

## 7. Redirect URI

默认规则：

- exact match；
- 禁止 wildcard host；
- 禁止 prefix match；
- 禁止运行时未注册 URI；
- 禁止 fragment；
- HTTPS Universal/App Link 优先；
- native custom scheme 必须具有 Client 唯一 namespace。

### 7.1 Desktop Loopback Exception

依据 RFC 8252，Desktop Native App 可以使用 loopback interface redirect：

```text
http://127.0.0.1:{ephemeral-port}/oauth/callback
http://[::1]:{ephemeral-port}/oauth/callback
```

这是 exact-match 的唯一端口例外：

- 注册时固定 scheme、loopback IP literal 与 path；
- Authorization Request 可以使用 OS 动态分配的端口；
- Server 校验时只忽略 port 差异；
- host、path、scheme 必须精确匹配；
- 不接受 `localhost` 作为首选注册值；
- Client 只监听 loopback interface，并在收到 response 后关闭 listener；
- 该例外仅适用于 `PUBLIC_NATIVE` Desktop Client。

除 loopback IP port 外，其余 Redirect URI 仍必须完整 exact match。

## 8. PKCE

Public Client：

```text
code_verifier = high entropy random
code_challenge = BASE64URL(SHA256(code_verifier))
code_challenge_method = S256
```

禁止 plain PKCE、复用 verifier、把 verifier 发给 Broker、把 verifier 放进 QR/deep link。

Verifier 只保存在发起 Client 本地直到换 Token。

## 9. Authorization Request

逻辑参数：

```text
response_type=code
client_id
redirect_uri
code_challenge
code_challenge_method=S256
state
scope
audience=app_id
device_id?
```

Server 校验 Client ACTIVE、redirect exact match、目标 App availability、requested scope 属于 Scope Registry，以及当前 consent/security policy。

## 10. Authorization Code

Code 必须：

- 单次使用；
- 短 TTL；
- 绑定 client_id；
- 绑定 redirect_uri；
- 绑定 PKCE challenge；
- 绑定 subject；
- 绑定 target app；
- 绑定 approved scopes；
- 绑定 authorization transaction。

推荐默认 TTL：120 秒。

Code 不是 Access Token。

## 11. Consent

第一次 Grant 或 Scope 扩大时展示：

```text
Client
Publisher
Target Server App
Requested Scopes
Device (when relevant)
Risk explanation
```

用户批准后创建或更新 Authorization-owned `AppAuthorizationGrant`。

Scope 缩减可直接收紧；Scope 扩大必须再次 consent。

## 12. Token Exchange

逻辑请求：

```text
grant_type=authorization_code
code
client_id
redirect_uri
code_verifier
```

Public Client 不发送静态 Client Secret。

Server 校验 Code、Client、Redirect、PKCE、User、App availability、Grant、Scope、Device policy。

## 13. App-scoped Token Claims

至少：

```text
iss
sub
jti
token_type
security_version
client_id
aud = target app_id
scope
authorization_grant_id
authorization_grant_version
device_id?
iat
exp
```

Server App API 接受 Token 前验证：

```text
signature
AND expiry
AND issuer
AND audience == own app_id
AND client registration valid
AND user status valid
AND token.security_version == user.security_version
AND grant ACTIVE
AND token.grant_version == current grant_version
AND token.scope subset-of grant.scopes
AND device binding matches when required
```

## 14. Refresh Token

Refresh Token 可以继续是签名 JWT，但不是 Login Session Row。

Refresh 时重新校验 User status、security_version、Client registration、App availability、Grant status/version、current scopes、device binding。

Refresh 不能扩大 Scope。Grant revoke 后 Refresh 必须失败。

## 15. Revocation

Client / Device revoke：

```text
AppAuthorizationGrant.status = REVOKED
grant_version++
```

只影响：

```text
subject + client + app + optional device
```

User-wide revoke：

```text
platform_user.security_version++
```

两者都不需要 jti blacklist、Token Digest 或 Security Session。

## 16. Ikaros Admin App Broker

Broker 只优化 UX，Server 仍是 Authorization Server：

```text
Anime Client
  │ create state + nonce + PKCE
  ├─ open authorization
  ▼
Ikaros Admin App / System Browser
  │ authenticate + consent
  ▼
Ikaros Server
  │ create/update Grant
  │ issue one-time code
  ▼
Anime Client
  │ code + local verifier
  ▼
Ikaros Server token endpoint
  └─ Anime-specific token
```

Broker 不得到 PKCE verifier、业务 Client Access Token 或 Refresh Token。

## 17. Broker Handoff

主 Ikaros App 可以传：

- instance discovery URL；
- instance_id；
- authorization URL/transaction reference；
- non-secret display metadata。

不得传 Admin App Token、password、refresh token、signing secret。

## 18. No-Broker Fallback

没有安装主 Ikaros App：

```text
Business Client
→ system browser
→ Instance authorization page
→ login / passkey / MFA
→ consent
→ redirect back
```

因此官方/第三方业务 Client 都不依赖 Broker 存在。

## 19. QR Pairing

QR 允许：

```text
discovery URL
instance_id
optional one-time pairing challenge
expires_at
```

禁止 password、长期 Access/Refresh Token、Client Secret、PKCE verifier。

## 20. Multiple Instances

Client 本地 Token/Grant context 按：

```text
(instance_id, client_id, user profile)
```

隔离。同一个 Client 在不同 Instance 的授权完全独立。

## 21. App API Version Selection

Client：

1. Instance Discovery；
2. App Discovery；
3. 选择最高共同 App API Major；
4. Authorization；
5. 调用 `/api/apps/{app_id}/v{major}/...`。

不得根据 Ikaros Server 产品版本推测业务 API。

## 22. Stable Errors

至少：

```text
app.not-installed
app.disabled
app.incompatible
app.api-version-unsupported
app.client-invalid
app.authorization-required
app.scope-insufficient
app.grant-revoked

auth.authorization-code-invalid
auth.pkce-invalid
auth.redirect-uri-invalid
auth.consent-required
auth.token-invalid
```

UI 本地化 message，程序依赖 stable code。

## 23. Public Client Secret Rule

Native / Browser：

```text
client_secret = not a security boundary
```

即使某 SDK 要求提供静态字符串，Server 也不能把它当 Client authentication proof。

## 24. Step-up

高风险 Scope/动作可要求 Step-up。

Step-up 不创造 Scope 或 User Permission，只满足当前 Security Policy 的 Verification Level / freshness。

## 25. Audit

至少审计：

- Client registration；
- authorization started（不记录 verifier/code）；
- Grant created；
- Scope change；
- Grant revoke；
- redirect mismatch；
- PKCE failure；
- admin-driven revoke。

禁止审计 Token 原文、Authorization Code 原文、PKCE verifier。

## 26. HTTP Contract Freeze 顺序

```text
Protocol Design
→ API Convention update if needed
→ HTTP Operation Registry
→ OpenAPI
→ Controller / DTO
→ SDK / Client
→ E2E
```

不得 Controller-first。

## 27. 第一实现切片

### Slice A — Discovery
Instance Discovery + App Discovery。

### Slice B — Client Registration
official seed + admin-approved third party + redirect exact match。

### Slice C — Authorization Code + PKCE
transient authorization transaction + code issue/redeem + AppAuthorizationGrant persistence。

### Slice D — Token Binding / Revocation
app-scoped claims + grant version + refresh + revoke。

### Slice E — Broker
主 Ikaros App handoff，不共享 Token。

## 28. P0 Protocol Profile：OAuth 2.0，不隐式引入 OIDC

P0 Client Authorization Profile 使用 OAuth 2.0 Authorization Code + PKCE，不宣称实现 OpenID Connect。

因此 P0：

- 不要求 `openid` Scope；
- 不签发 ID Token；
- 不提供 UserInfo 作为 Client identity source；
- Authorization Request 不要求 OIDC `nonce`；
- Client 用户身份以 Access Token 的 `sub` 与业务 API 返回的用户视图为准。

如果未来需要 OIDC Federation / ID Token：

```text
separate protocol decision
→ issuer/discovery extension
→ ID Token signing/validation
→ nonce
→ claims contract
```

不得只因为当前 JWT 有 `sub` / `iss` 就声称兼容 OIDC。

---

## 29. Authorization Server Issuer / Mix-up Defense

Ikaros Client 天然可能同时连接多个 Instance，因此必须防 Authorization Server mix-up。

Instance Discovery 返回稳定：

```text
issuer
```

Authorization transaction 在发起时绑定：

```text
expected_instance_id
expected_issuer
authorization_endpoint
token_endpoint
client_id
state
```

Authorization Response 必须包含 RFC 9207 `iss`：

```text
code
state
iss
```

Client 校验：

```text
response.state == pending.state
AND response.iss == pending.expected_issuer
AND token endpoint belongs to expected issuer metadata
```

不匹配立即终止，Authorization Code 不发送到任何 Token Endpoint。

Server Discovery 应声明：

```json
{
  "authorization_response_iss_parameter_supported": true
}
```

同一 Client 配置中不允许两个 Instance 使用相同 issuer identity。

---

## 30. Refresh Token Replay Protection

仅有 `grant_version` 可以处理显式 revoke，但不能识别被盗 Refresh Token 的并发重放。

依据 OAuth 2.0 Security BCP（RFC 9700），Public Client 若签发 Refresh Token，必须使用 sender-constrained refresh token 或 Refresh Token Rotation。

V2 P0 选择：

```text
Refresh Token Rotation
```

暂不要求 DPoP / mTLS。

### 30.1 Grant-owned Refresh Generation

不建立 Token Row、Token Digest 或 `jti` blacklist。

在 Authorization-owned `AppAuthorizationGrant` 增加最小 replay state：

```text
refresh_generation: bigint
refresh_last_used_at: timestamptz?
```

Refresh JWT 增加：

```text
authorization_grant_id
authorization_grant_version
refresh_generation
```

初次授权：

```text
grant.refresh_generation = 0
issue refresh(refresh_generation=0)
```

刷新时执行原子 compare-and-swap：

```text
WHERE grant_id = ?
  AND status = ACTIVE
  AND grant_version = token.grant_version
  AND refresh_generation = token.refresh_generation

SET refresh_generation = refresh_generation + 1
    refresh_last_used_at = now()
```

成功后：

```text
issue new Access Token
issue new Refresh Token(refresh_generation = old + 1)
```

### 30.2 Replay

如果 Refresh Token 的 `refresh_generation` 小于当前 Grant generation，说明旧 Refresh Token 被再次使用。

Server 无法可靠判断攻击者和合法 Client 谁在重放，因此：

```text
revoke grant
grant_version++
```

并要求重新完成 Authorization。

该机制保存的是 Grant replay generation，不是每枚 Token 的服务端 Session，也不违反“不持久化 Token / Digest / jti blacklist”的基线。

### 30.3 Confidential Client

Confidential Client 仍必须通过其 Client Authentication 绑定 Refresh Token；可以后续使用 sender-constrained token，但不得弱于 Public Client 的 replay protection policy。

---

## 31. Token Lifetime

沿用现有 Ikaros JWT 默认：

```text
Access Token TTL  = PT15M
Refresh Token TTL = P30D
Authorization Code TTL = 120 seconds
```

规则：

- Access/Refresh TTL 可由 Instance Security Policy 调整；
- Refresh Rotation 不能把已过期 token 恢复为有效；
- 每次 rotation 的新 Refresh Token 使用当前 policy TTL；
- `refresh_last_used_at` 为后续 inactivity policy 提供依据；
- Grant revoke / `security_version` 失效优先于 Token `exp`；
- 高风险 App 可以按 policy 不签发 Refresh Token。

---

## 32. Acceptance Invariants

1. Native Client 无需可信静态 Secret。
2. PKCE 只允许 S256。
3. Redirect URI exact match。
4. Code 单次使用且短 TTL。
5. Broker 不获得业务 Client Token。
6. Anime Token 的 aud 不能调用 Photos App。
7. Grant Scope 缩减后旧扩大 Scope Token 被拒绝。
8. Revoke Anime Client 不影响 Photos Client。
9. User security_version 提升后全部旧 App Token 失效。
10. 未安装/禁用/不兼容 App 在授权前可识别。
11. Third-party 与 Official Client 使用相同 Grant/Scope enforcement。
12. QR 不携带长期 Credential。
13. Desktop loopback redirect 只允许端口动态变化，scheme/IP/path 必须匹配注册值。
14. P0 不返回 ID Token，也不把 OAuth-only flow 描述成 OIDC。
15. 多 Instance Client 必须校验 Authorization Response 的 `iss`，issuer mismatch 时不得兑换 Code。
16. Public Client Refresh Token 每次成功使用后 generation 单调递增。
17. 已使用 Refresh Token 再次出现时触发 replay response，并撤销对应 Grant。
18. Refresh Rotation 不创建 Token Row、Digest 或 `jti` blacklist。
19. Access / Refresh 默认 TTL 与现有 Ikaros Security 配置一致。
