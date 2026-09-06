# ADR-004：Authentication 通过 Authorization 获取权限快照

| 项目 | 内容 |
|---|---|
| 状态 | Accepted |
| 日期 | 2026-09-07 |
| 关联 Issue | [#905](https://github.com/ikaros-dev/ikaros/issues/905) |
| 影响范围 | Authentication、Authorization、JWT Claim、Maven 模块依赖、权限变更语义 |

## 背景

Authentication 需要在登录、注册和刷新 Token 时把用户权限写入 Access JWT，但角色、权限注册表和用户角色绑定属于 Authorization Owner。若 Authentication 直接读取 Authorization 的 Repository，就会违反模块边界；若 JWT 不携带权限，则会改变当前请求授权链路和 Token 契约。

## 决策

1. `authentication` 实现模块允许依赖 `authorization-api`，不得依赖 `authorization` 实现模块。
2. `authentication-api` 对外提供 `AuthenticatedPrincipal` 与 `SecurityVerificationLevel`；它不依赖 Authorization。
3. `authorization-api` 暴露只包含公开值类型的权限快照 Capability：

   ```text
   PermissionSnapshotQuery.permissionsFor(UUID subjectId)
     -> Mono<PermissionSnapshot>
   ```

   同时暴露用户创建流程所需的初始角色分配 Capability：

   ```text
   InitialRoleAssigner.assignInitialRole(UUID subjectId, String roleCode)
     -> Mono<Void>
   ```

   Authentication 用户视图需要展示角色编码时，通过角色成员查询 Capability 获取，不直接读取角色绑定：

   ```text
   RoleMembershipQuery.roleCodesFor(UUID subjectId)
     -> Mono<List<String>>
   ```

4. `PermissionSnapshot` 至少包含 `subject_id` 和去重、稳定排序后的 `permission_keys`；不得暴露 Role、Permission、Binding Entity、Repository 或 Persistence 类型。
5. Authentication 在注册、登录和刷新时调用该 Capability；调用失败或返回不可用结果时，认证签发失败，不签发权限不完整的 JWT。
6. Access JWT 携带本次签发时取得的 `permissions` 快照；Refresh JWT 不承担请求授权，可不携带权限快照。刷新成功后重新读取最新权限并签发新的 Access JWT。
7. 已签发 Access JWT 的权限快照在 Token 有效期内保持不变。角色、权限或角色绑定变更不自动提升用户 `security_version`，因此不主动使既有 JWT 失效。
8. 需要立即使用户既有 JWT 失效时，使用既有的用户级 Token 失效 Command，提升该用户的 `security_version`。高风险权限变更是否同时触发该 Command，由对应 Command Contract 单独声明，不由 Capability 隐式完成。
9. `authorization` 不依赖 `authentication` 实现；对象级授权、Security Policy 和最终 Allow / Deny 仍由 Authorization 或目标业务 Owner 权威判断，JWT 权限快照不能替代 Resource ACL、Share 或 Membership 判断。
10. `InitialRoleAssigner` 只由用户创建流程调用，用于保持首个用户等系统初始化角色分配的同步语义；它不返回 Role Entity，也不允许调用方直接写入角色绑定。
11. `RoleMembershipQuery` 只返回稳定排序的角色编码；Authentication 可以用它组装 `UserView`，但不得因此获得角色、绑定或权限 Persistence 的访问权。

## Maven 依赖方向

```text
authentication  -> authorization-api
authorization   -> authentication-api
authorization   -> common-api
application     -> authentication
application     -> authorization
```

禁止：

```text
authentication  -> authorization
authentication  -> authorization.persistence
authorization   -> authentication
```

## 后果

- 保留当前 Access JWT 的 `permissions` 契约和请求链路；
- Authorization 独占角色、权限和绑定数据；
- 权限快照存在 Token 生命周期内的已知延迟，必须通过刷新或用户级 Token 失效获得最新授权状态；
- Authentication 签发 Token 依赖 Authorization Capability 的可用性，但不会因为 Capability 异常而签发权限不完整的 Token；
- 后续可在不暴露 Persistence 的前提下替换权限计算、缓存或 Projection 实现。

## 验证

- Architecture Test 阻止 Authentication 依赖 Authorization 实现包或 Repository；
- Authentication 单元测试验证注册、登录和刷新均调用 `PermissionSnapshotQuery`；
- Capability 契约测试验证权限键去重、稳定排序和失败关闭；
- JWT 测试验证 Access Token 携带快照，Refresh 后获得新的快照；
- 权限变更测试验证既有 JWT 按本决策保持原快照，用户级 `security_version` 失效后旧 JWT 被拒绝。
