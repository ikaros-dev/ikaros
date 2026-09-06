# ADR-002：Authentication 与 Authorization 事件命名空间

- 状态：Accepted
- 日期：2026-09-06
- 关联：#905 模块依赖边界

## 背景

平台安全原有实现将认证、Token 失效、角色和权限事件统一放在 `identity.*` 命名空间。模块边界确定后，Authentication 与 Authorization 已成为两个不同的 Owner，继续使用统一的事件命名空间会让 Durable Event 的 Producer 语义与模块所有权不一致。

## 决策

从 P0 当前版本开始，Durable Event 的事件类型命名空间按业务 Owner 表达：

| 原事件类型 | 新事件类型 | Producer |
|---|---|---|
| `identity.user.created` | `authentication.user.created` | Authentication |
| `identity.user.disabled` | `authentication.user.disabled` | Authentication |
| `identity.user.enabled` | `authentication.user.enabled` | Authentication |
| `identity.user.tokens-invalidated` | `authentication.user.tokens-invalidated` | Authentication |
| `identity.role.created` | `authorization.role.created` | Authorization |
| `identity.role.permissions-replaced` | `authorization.role.permissions-replaced` | Authorization |
| `identity.user.role-assigned` | `authorization.user.role-assigned` | Authorization |
| `identity.user.role-removed` | `authorization.user.role-removed` | Authorization |

其他当前事件的 Producer 归属如下：

- `resource.*`：Resource；
- `storage.*` 与 `attachment.purged`：Storage；
- `operations.*`：Platform Operations；
- `ingestion.*` 与 `source.item.unavailable`：Ingestion。

本次是 P0 契约收敛，不提供旧事件类型的兼容别名或双写。公开事件契约、JSON Schema、Payload Registry、实现代码和测试必须同步使用新名称。

## 影响

- Event Envelope 的 `producer_subsystem` 枚举包含 `authentication` 与 `authorization`，不再包含 `identity`；
- 认证事件与授权事件可以由不同实现模块独立发布和消费；
- Authentication 不发布登录 Session 创建、查询或撤销事件；普通 Logout 由客户端删除本地 Token，用户级提前失效通过 `security_version` 变化表达；
- Command ID、Permission、数据库 Schema 和 Java 包名不因本 ADR 自动改名，除非对应 Owner 拆分子任务另行决定；
- 已存在的 `identity.*` 事件数据不做兼容迁移；当前阶段只约束新版本实现与契约。

## 验证

- P0 Event JSON Schema 只允许新的 Authentication / Authorization 事件类型；
- Event Payload Schema Registry、Event Catalog 与验收矩阵使用新事件类型；
- 业务实现通过 `integration-api` 的 `DurableEventPublisher` 发布，并显式填写 Producer。
