# ADR-001：采用 Maven Multi-Module 表达模块边界

| 项目 | 内容 |
|---|---|
| 状态 | Accepted |
| 日期 | 2026-09-06 |
| 关联 Issue | [#905](https://github.com/ikaros-dev/ikaros/issues/905) |
| 影响范围 | 构建拓扑、Java Package Ownership、模块 API、Spring Composition Root、Architecture Test |

## 背景

Ikaros V2 采用 Modular Monolith，但当前仓库使用根 `pom.xml` 的单模块 Maven。单模块可以表达 Java Package 边界，却无法在编译期阻止业务模块依赖其他模块的实现、Repository、Entity 或 Persistence Package。

现有代码已经出现跨领域 Repository/Entity 直接引用以及 `media` 与 `storage` 的双向依赖。继续以单模块承载所有实现会使这些依赖容易被重新引入，也无法为公开契约与内部实现提供足够清晰的构建边界。

## 决策

1. V2 继续采用单进程 Modular Monolith，但构建拓扑改为 Maven Multi-Module。
2. 不迁移到 Gradle；Maven 仍是项目唯一构建系统。
3. 每个业务或平台能力拆分为两个 Maven 模块：
   - `<business-name>-api`：公开 Capability、Command、Query、Event Contract、Public DTO、Permission Key 和 Public Error；
   - `<business-name>`：该业务能力的 Domain、Application、Adapter、Persistence 和配置实现。
4. `-api` 模块不得依赖同一业务实现模块，也不得暴露 Entity、Repository、SQL DTO 或技术实现类型。
5. 实现模块只能依赖自己的 `-api`、其他模块的 `-api` 以及允许的平台公开 API。
6. `server` 是唯一 Spring Composition Root；它可以依赖各实现模块，但任何业务模块不得反向依赖 `server`。
7. `test-support` 是测试基础设施模块，不作为业务 Owner，不建立业务模块对它的生产依赖。
8. 所有模块立即执行严格边界。跨模块直接依赖 Entity、Repository、Persistence、私有 SQL、内部 Service 或内部 Spring Bean 均视为架构错误。
9. 每个业务 Owner 的生产 Migration 由对应实现模块持有，路径为 `<owner-module>/src/main/resources/db/migration/V<monotonic-version>__<description>.sql`。`server` 只聚合各实现模块的运行时 classpath 并统一执行 `r2dbc-migrate`，不拥有业务 Migration。
10. 所有实现模块共享全局单调 Migration 版本序列；模块发现顺序不得决定 Schema 结构，文件名和描述必须可识别 Owner。
11. `PrincipalContext` 是 `foundation-api` 的公开值契约；`PrincipalContexts` 是 `foundation` 的 Reactor Context 运行时访问工具。Authentication 负责写入上下文，Integration 与 Operations 只能依赖 Foundation，不依赖 Authentication 实现。
12. Integration 对外只通过 `integration-api` 暴露 `DurableEventPublisher.append(EventAppendRequest)`，返回 `EventReference`。Outbox Entity、Repository、Inbox 和 Dispatcher 均属于 Integration 实现模块，不得进入业务模块依赖。
13. `event_outbox` 由 Integration Owner 追加 Migration 增加 `producer_subsystem`、`subject_type` 与 `subject_id` 正式字段；现有 `aggregate_type / aggregate_id` 数据先通过 Expand / Migrate 回填，待所有实现和契约切换完成后再单独评估 Contract，不在本步骤删除旧字段。

## 模块命名与归属

公开契约使用 `-api` 后缀，业务实现使用不带后缀的业务语义名称：

```text
resource-api          -> resource
storage-api           -> storage
authentication-api    -> authentication
authorization-api     -> authorization
integration-api       -> integration
task-api              -> task
operations-api        -> operations
```

当前包归属决议：

- `collection`、`metadata`、`relation`、`progress`、`activity` 归属 `resource`；
- `identity`、`security`、`verification` 归属 `platform-security`，按职责拆为 `authentication` 与 `authorization`；
- `audit` 归属 `platform-operations`；
- `offline` 归属 `sync`；
- `planning` 归属 `productivity`；
- `notes` 归属 `private-notes`；
- `password` 归属 `password-manager`。

认证与授权的职责划分如下：

- Authentication：用户身份、登录、注册、JWT、Principal、Token 签发与校验、OTP 和 Step-up Verification；
- Authorization：Role、Permission、Access Control、Security Policy 和 Resource Authorization。

## 目标拓扑

```text
ikaros
├── pom.xml
├── server
├── test-support
├── platform
│   ├── foundation-api
│   ├── foundation
│   ├── integration-api
│   ├── integration
│   ├── authentication-api
│   ├── authentication
│   ├── authorization-api
│   ├── authorization
│   ├── task-api
│   ├── task
│   ├── operations-api
│   ├── operations
│   ├── plugin-api
│   └── plugin
└── modules
    ├── resource-api
    ├── resource
    ├── storage-api
    ├── storage
    ├── ingestion-api
    ├── ingestion
    ├── drive-api
    ├── drive
    ├── sync-api
    ├── sync
    ├── sharing-api
    ├── sharing
    ├── search-api
    ├── search
    ├── backup-api
    ├── backup
    ├── media-api
    ├── media
    ├── reading-api
    ├── reading
    ├── music-api
    ├── music
    ├── photo-api
    ├── photo
    ├── document-api
    ├── document
    ├── game-api
    ├── game
    ├── productivity-api
    ├── productivity
    ├── finance-api
    ├── finance
    ├── private-notes-api
    ├── private-notes
    ├── password-manager-api
    └── password-manager
```

## 依赖规则

允许：

```text
media       -> media-api
media       -> storage-api
server      -> media
server      -> resource
```

禁止：

```text
media       -> storage
media       -> storage.persistence
server      -> resource.persistence
planning    -> identity.repository
plugin      -> core.internal
```

跨模块双向依赖必须通过更小的 Capability Contract、Command、Durable Event 或重新划分所有权解决，不得通过 `common` 隐藏循环依赖。

Migration 依赖方向如下：

```text
owner implementation module
  -> owns its migration resources
server
  -> aggregates implementation classpath
  -> executes all pending migrations
```

## 后果

积极后果：

- Maven 编译依赖能够直接反映模块所有权；
- API 与实现的依赖方向可被构建系统和 Architecture Test 同时验证；
- 未来拆分 Worker 或独立服务时可以复用公开契约；
- 非 Owner 无法通过普通模块依赖访问持久化实现。

代价与风险：

- 需要迁移现有单模块目录、测试和资源；
- 现有跨模块 Repository/Entity 访问必须重构为公开 API、Capability 或 Event；
- 现有循环依赖可能需要重新划分职责；
- Maven 子模块数量增加，构建与本地开发配置更复杂。

## 实施约束

编码前必须先完成模块所有权文档和依赖矩阵。实施顺序为：

```text
ADR / Ownership
  -> Maven parent / module POM
  -> API contract extraction
  -> implementation migration
  -> cross-module dependency refactor
  -> Architecture Test
  -> compile / test / verify evidence
```

本 ADR 不改变 Domain、Schema、Command/Query、Event、Permission 或 HTTP 契约本身；发现这些契约需要变化时，必须更新对应事实来源文档和测试契约。
