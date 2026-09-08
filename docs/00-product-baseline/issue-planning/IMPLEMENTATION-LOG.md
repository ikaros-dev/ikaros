# Ikaros V2 实施记录

## A02-01 首次启动检查必需配置

- 日期：2026-09-09
- 推荐决策：启动时只强制校验 PostgreSQL R2DBC 连接信息与 JWT 核心配置；Storage 加密密钥在存储能力启用时由 Storage Owner 校验。
- 原因：这些配置直接决定核心服务能否安全启动；Storage 属于可选能力，不能把可选依赖误判为全局启动前置条件。
- 失败语义：缺失、空值、非 PostgreSQL R2DBC URL、非法 TTL 或占位/过短 JWT secret 直接阻止 Spring 容器完成启动；错误只包含配置 key 和通用原因，不包含配置值。
- 验证：`mvn -pl application -am -DskipTests compile`；`mvn -pl application -Dtest=StartupConfigurationValidatorTest test`。
