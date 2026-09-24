# ADR-007：审计读取权限键迁移至 audit.read

| 项目 | 内容 |
|---|---|
| 状态 | Accepted |
| 日期 | 2026-09-18 |
| 关联 Issue | [#1417](https://github.com/ikaros-dev/ikaros/issues/1417) |
| 影响范围 | Authorization、HTTP 审计查询、Console 路由、内置角色权限 |

## 背景

历史审计查询使用 `system.audit.read`，而当前 Console canonical route
`/system/communications/audit`、路由权限矩阵和权限注册表已使用 `audit.read`。两个键并存会让菜单、JWT 权限快照和后端过滤器的授权结果不一致。

## 决策

1. `audit.read` 是审计日志读取的唯一 canonical permission key。
2. `system.audit.read` 仅用于过渡兼容：既有角色和旧 JWT 可在迁移窗口内继续读取审计；新建或更新角色不得再分配该键。
3. 审计查询 HTTP 门禁在迁移完成后使用 `audit.read`；Console 菜单、OpenAPI、HTTP Operation Registry 和测试使用同一键。
4. 过渡结束后以追加 Migration 移除旧键的内置角色绑定和注册表项；不修改已发布 Migration。

## 后果

- `audit.read` 不授予任何目标业务对象的读取权限；Audit Deep Link 必须在目标路由重新执行目标授权。
- #1422 负责实现 HTTP 门禁、内置角色迁移、公开 API 契约和兼容测试；#1418 只冻结此决策，不提前改变查询端点行为。

## 验证

- 授予 `audit.read` 的主体能通过审计查询门禁；无此权限的主体得到稳定 403。
- 迁移窗口内，仅带 `system.audit.read` 的既有 JWT 按明确兼容策略处理；迁移结束后不再作为授权依据。
