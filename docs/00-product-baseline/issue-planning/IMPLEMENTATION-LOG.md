# Ikaros V2 实施记录

## A02-01 首次启动检查必需配置

- 日期：2026-09-09
- 推荐决策：启动时只强制校验 PostgreSQL R2DBC 连接信息与 JWT 核心配置；Storage 加密密钥在存储能力启用时由 Storage Owner 校验。
- 原因：这些配置直接决定核心服务能否安全启动；Storage 属于可选能力，不能把可选依赖误判为全局启动前置条件。
- 失败语义：缺失、空值、非 PostgreSQL R2DBC URL、非法 TTL 或占位/过短 JWT secret 直接阻止 Spring 容器完成启动；错误只包含配置 key 和通用原因，不包含配置值。
- 验证：`mvn -pl application -am -DskipTests compile`；`mvn -pl application -Dtest=StartupConfigurationValidatorTest test`。

## A02-02 初始化数据库

- 日期：2026-09-09
- 推荐决策：继续使用应用启动阶段的 `r2dbc-migrate` 作为唯一初始化入口，以 `classpath*:/db/migration/*.sql` 聚合各 Owner 的 versioned migration；不新增旁路初始化 API，也不允许 Repository 或启动逻辑手写 DDL。
- 原因：空 PostgreSQL 初始化与已有实例升级必须共享同一套可重放 migration 历史，保持 PostgreSQL 为业务真相源，并让 migration 失败阻止应用完成启动。
- 失败语义：migration 资源缺失、命名不符合 `V<monotonic-version>__<description>.sql` 或版本重复均视为构建/验收失败；数据库连接或 SQL 执行失败由 `r2dbc-migrate` 阻止应用启动，不报告为健康。
- 验证：`DatabaseInitializationContractTest` 验证应用 classpath 可发现全部 Owner migration、文件命名和版本唯一性；完整空库执行需要 Docker Desktop + PostgreSQL Testcontainers，当前环境未安装 Docker，因此保留为环境门禁。
