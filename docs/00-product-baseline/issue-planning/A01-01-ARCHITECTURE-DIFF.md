# A01-01 工程与设计差异记录

日期：2026-09-10

## 已确认的一致项

- 根 `pom.xml` 已采用 Maven Multi-Module，并以 `application` 作为 Server Composition Root。
- 工程基线已使用 Java 21、Spring Boot 4.x、Spring WebFlux、Project Reactor、R2DBC 和 PostgreSQL。
- 迁移由 `r2dbc-migrate` 执行，业务运行时未引入 JPA/JDBC 或 Flyway 作为第二持久化栈。
- 公开 HTTP 能力通过 HTTP Operation Registry 与 OpenAPI 追溯；业务模块可独立提供 API、实现和迁移资源。

## 当前差异与处理决策

| 设计要求 | 当前实现差异 | 决策 |
|---|---|---|
| 模块内部按 `api / application / domain / adapter / persistence / config` 分层 | 部分既有模块仍将少量 Controller、Service、Entity 和 Repository 放在同一 package | 保留现有可运行代码；新增或修改行为遵守分层，迁移既有代码时按 issue 独立处理 |
| 通过显式 Module Configuration 组装 Spring Bean | Server 已承担聚合组装，但部分模块仍依赖组件扫描 | 不改变当前构建拓扑；后续模块改造以显式配置和 Architecture Test 逐步收敛 |
| 完整 Architecture Boundary / OpenAPI / Event / Permission 自动化门禁 | 现有门禁覆盖不完整，不能把代码可编译视为契约完成 | 按对应前置 issue 补齐测试和门禁；任何新公开路由先登记再实现 |
| P0 设计的全部领域能力 | 多个模块仍处于基线或增量实现阶段 | 按实施路线图和依赖图推进，不用占位代码宣称完成 |

## 构建与迁移取舍

继续使用根 `pom.xml` 的 Maven Multi-Module；不切换 Gradle，不引入第二数据库访问栈。数据库迁移继续使用版本化 R2DBC migration，并由 `application` 聚合运行时 classpath 执行。该取舍与 `Technical-Architecture-Design.md` 的架构决策摘要一致。

## 可重复验证

```text
mvn -s .mvn-local-settings.xml -pl application -am -DskipTests package
```

上述命令成功即可证明当前 Maven 聚合拓扑和 Server 打包链路可用；具体领域行为仍必须由各自 issue 的测试、契约和运行验证证明。
