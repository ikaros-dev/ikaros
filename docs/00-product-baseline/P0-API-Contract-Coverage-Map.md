# Ikaros V2 P0 API / 契约覆盖映射

| 项目 | 内容 |
|---|---|
| 基线 | `v2-p0-foundation-0.2` |
| 基础 OpenAPI | `contracts/openapi-v2-p0.yaml` |
| 契约收敛补充 | `contracts/openapi-v2-p0-contract-convergence.yaml` |
| 机器可读注册表 | `contracts/P0-HTTP-Operation-Registry.yaml` |
| 事件 Schema | `contracts/schema/p0-event-v1.schema.json` |

> 存在应用契约 ≠ 必须存在公开 HTTP 端点。禁止先写 Controller、后补契约。

## 契约文件

- `P0-Implementation-Baseline.md` — Phase 0 工程实现准入与冻结边界。
- `P0-Requirement-Traceability-Matrix.md` — 需求 → 契约 → 数据库 → API → 事件 → 测试的可追溯关系。
- `contracts/P0-Command-Query-Event-Catalog.md` — 应用层 Command / Query / Event 的权威目录。
- `contracts/P0-Event-Payload-Schema-Registry.md` — 面向人的事件 Payload 兼容性契约。
- `contracts/schema/p0-event-v1.schema.json` — 机器可读的事件 Envelope / Type / Version 基线。
- `contracts/P0-HTTP-Operation-Registry.yaml` — 完整的 P0 公开 HTTP 操作映射。
- `contracts/openapi-v2-p0.yaml` — 原始 P0 OpenAPI 基线。
- `contracts/openapi-v2-p0-contract-convergence.yaml` — 对原始规范中缺失、但已由 Catalog 声明的 HTTP 操作进行增量补充。
- `testing/P0-Acceptance-Invariant-Test-Matrix.md` — **必须满足**的工程验收门禁。

## P0 公开 API 规则

```text
子系统能力
  -> Command 或 Query ID
  -> 权限 / 授权策略
  -> HTTP operationId
  -> HTTP Operation Registry
  -> OpenAPI 请求 / 响应 Schema
  -> Event type/version（产生持久事实时）
  -> 验收 / 不变量测试
```

CI 应校验：`operationId` 唯一性、method/path 唯一性、Registry → OpenAPI 的存在性、Registry 中的 contract ID → Catalog 的存在性，以及 OpenAPI 引用有效性。

## 契约收敛后的覆盖情况

原始 OpenAPI 基线包含 **16** 个公开操作；契约收敛补充新增 **12** 个缺失映射，因此当前共有 **28** 个 P0 公开操作。

本轮新增覆盖的契约：

- `resource.find-by-external-identity`
- `resource.trash-resource`
- `resource.get-user-state`
- `resource.list-tags`
- `resource.list-collections`
- `operations.list-background-tasks`
- `operations.list-task-attempts`
- `storage.list-blob-placements`
- `storage.get-provider`
- `identity.get-current-user`
- `identity.invalidate-user-tokens`
- `identity.get-user`

其中原 `identity.list-sessions` 已由 `identity.invalidate-user-tokens` 替换。JWT 登录不提供服务端 Session 列表；用户级紧急失效通过提升 `security_version` 完成。该替换保持 P0 公开操作总数不变。

精确的机器可读列表见 `contracts/P0-HTTP-Operation-Registry.yaml`。

## 当前明确不公开的能力

以下应用契约继续保持内部使用或 `contract-deferred` 状态，直到路由、请求/响应模型、授权/二次验证、幂等与并发语义完成冻结：

- External Identity、Tag、Collection、User State 相关的 Resource 变更契约；
- Storage Attachment 生命周期、Blob 校验/GC、Provider 更新/启用/禁用/排空；
- `operations.retry-background-task`；
- 基础 OpenAPI 尚未公开的 Identity 用户/角色/Token 安全变更命令。

禁止为这些能力自行臆造 Controller 路由。

## 事件机器契约

`contracts/schema/p0-event-v1.schema.json` 冻结 P0 事件 Envelope、UUIDv7 Event ID、Schema Version、Producer Namespace，以及当前 **42 个 P0 v1 Event Type**。本次无状态 JWT 收敛删除 `identity.session.revoked`、`identity.user.sessions-revoked`，新增 `identity.user.tokens-invalidated`。每种事件的 Payload 字段级约束仍由 `P0-Event-Payload-Schema-Registry.md` 管理，并应在 Phase 0 实现阶段扩展为机器可执行的兼容性检查（`P0-EVT-004/013/014`）。

## 变更检查清单

- [ ] Catalog 中存在对应 Command / Query。
- [ ] 权限 / 对象级授权规则明确。
- [ ] OpenAPI 中存在对应 `operationId`。
- [ ] HTTP Operation Registry 中存在对应条目。
- [ ] 新增/变更操作携带 `x-ikaros-contract-id`。
- [ ] 幂等 / 并发语义明确。
- [ ] 适用时已同步 Event Schema / Payload Registry。
- [ ] 存在对应 Acceptance Test ID。
- [ ] 已同步 Traceability Matrix。
