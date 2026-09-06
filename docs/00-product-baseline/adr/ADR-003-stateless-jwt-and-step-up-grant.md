# ADR-003：无状态 JWT 与 Step-up Verification Grant

| 项目 | 内容 |
|---|---|
| 状态 | Accepted |
| 日期 | 2026-09-07 |
| 关联 Issue | [#905](https://github.com/ikaros-dev/ikaros/issues/905) |
| 影响范围 | Authentication、Authorization、Foundation PrincipalContext、HTTP 契约、数据库 Schema、验收测试 |

## 背景

当前实现仍包含 `SecuritySessionService`、`security_session` 表、JWT `sid` Claim 和基于服务端会话的 Step-up 状态。但 P0 认证基线采用无状态 JWT，服务端不建立或持久化登录 Session。

## 决策

1. Authentication 使用无状态 Access JWT 与 Refresh JWT。
2. 服务端删除登录 Session / Security Session 的实体、Repository、Application Contract、HTTP 查询与撤销接口，以及对应数据库表和 Migration。
3. JWT 不使用 `sid` / `session_id` 关联服务端登录状态。
4. 每枚 Access JWT、Refresh JWT 和 Step-up Verification Grant 必须携带唯一 `jti`。
5. `jti` 只用于 Token / Grant 追踪、审计关联和问题排查；不持久化 `jti`，不建立 Token 黑名单，不通过 `jti` 实现单 Token 撤销。
6. 用户级提前失效通过提升 `security_version` 完成。请求认证必须校验 Token 中的版本与当前用户版本一致，并校验用户状态。
7. Step-up 成功后签发短期、Purpose-bound 的增强 JWT / Verification Grant，不创建或更新登录 Session。
8. 普通 Logout 由客户端删除本地 Token 与 Credential Cache 完成；服务端不产生登录 Session 变更。

## Token Claim 最小要求

普通 Access / Refresh JWT 至少包含：

```text
iss
sub = user_id
jti = token_id
token_type
security_version
iat
exp
permissions 或 permission reference
```

Step-up Verification Grant 至少包含：

```text
iss
sub = user_id
jti = grant_id
purpose
target_reference（适用时）
achieved_svl
verified_at
iat
exp
```

`jti` 不属于业务身份，不得被解释为登录 Session ID。

## 影响

- `PrincipalContext` 不再承载 `sessionId`；如需追踪 Token，使用与 JWT 无关的 Token Identifier 语义。
- Authorization 通过 Authentication 公开 Principal 能力读取用户身份、权限和验证等级，不读取 Security Session Persistence。
- `ResourceAuthorizationWebFilter` 归属 Authorization 实现模块。
- Authentication 与 Authorization 的 Migration 必须分别拥有用户/凭据/验证挑战和角色/权限/绑定状态。
- 媒体、阅读、上传、Planning 等业务领域自己的 `Session` 实体不受本 ADR 影响。

## 不解决的问题

纯无状态 JWT 不能在服务端只撤销单枚 Token，也不能通过服务端 Logout 立即删除客户端已经持有的 Token。需要用户级紧急失效时，必须提升 `security_version`；正常失效依赖 `exp`。

## 验证

- 不存在 `security_session` 登录状态表；
- 不存在 `sid` / `session_id` 登录状态 Claim；
- Access / Refresh JWT 与 Step-up Grant 均产生唯一 `jti`；
- `security_version` 变化后旧 Access / Refresh JWT 均无法继续使用；
- Step-up Grant 校验 `purpose`、目标、SVL 与有效期；
- 普通 Logout 不修改认证持久化状态。
