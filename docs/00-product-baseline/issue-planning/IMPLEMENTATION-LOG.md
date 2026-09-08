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

## A02-03 已有实例执行版本升级

- 日期：2026-09-09
- 推荐决策：让 `r2dbc-migrate` 在每次启动时读取已应用版本并只执行 pending migration；显式启用等待数据库和 PostgreSQL advisory lock，多个实例启动时由数据库锁串行升级。
- 原因：升级必须复用 A02-02 的 versioned migration，不引入第二套升级 API、手工 SQL 或 ORM schema 自动更新；migration history 由 PostgreSQL 持久化，重启后可继续判断 pending 版本。
- 失败语义：任一 pending migration 执行失败则启动失败/保持未就绪；不得写入伪成功版本，也不得跳过失败版本继续提供业务服务。凭据仅通过 R2DBC 配置传入，不进入诊断输出。
- 验证：`DatabaseInitializationContractTest` 验证聚合 migration 的版本唯一性，`application.yaml` 显式锁定启用、等待数据库和 PostgreSQL 专用锁；真实旧版本数据库升级回放需要 Docker Desktop + PostgreSQL Testcontainers，当前环境未安装 Docker，未伪造该运行证据。

## A02-04 升级失败时阻止服务进入就绪状态

- 日期：2026-09-09
- 推荐决策：在 Operations health 层增加 `ApplicationReadiness` 门闩，只有 Spring 发布 `ApplicationReadyEvent` 后才允许 `/api/health/ready` 继续检查数据库并返回 UP。
- 原因：r2dbc-migrate 在应用启动阶段执行；migration 失败时不会发布 `ApplicationReadyEvent`。即使未来 Web 层在启动失败期间仍可响应探针，也必须明确返回 DOWN，不能把“数据库可连接”误报为“应用已完成升级”。
- 失败语义：启动流程尚未完成或数据库探测失败均返回 HTTP 503 与 `status=DOWN`；成功完成启动且数据库探测通过才返回 HTTP 200 与 `status=UP`。响应不包含凭据或内部异常。
- 验证：`HealthControllerTest` 覆盖启动未完成和数据库故障分支；现有 liveness 保持不依赖数据库。真实 migration 失败启动回放仍需要 Docker Desktop + PostgreSQL Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A02 安装与升级（父 issue）

- 日期：2026-09-09
- 验收结论：A02-01 至 A02-04 已按顺序完成并在 GitHub 关闭；实现覆盖首次配置校验、空库 migration 聚合、已有实例 pending migration 升级，以及 migration/启动未完成时的 readiness 保护。
- 推荐决策：保留当前最小组合路径，不新增独立初始化/升级 HTTP API；通过 PostgreSQL R2DBC + r2dbc-migrate 的启动生命周期完成数据库准备，Operations health 只报告已完成启动且数据库可用的服务为 ready。
- 验证证据：`mvn -pl application -am -DskipTests compile`；`mvn -pl application -am -Dtest=DatabaseInitializationContractTest,StartupConfigurationValidatorTest -Dsurefire.failIfNoSpecifiedTests=false test`；`mvn -pl operations -am -Dtest=HealthControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`；全部 BUILD SUCCESS。
- 剩余限制：当前开发环境未安装 Docker，真实 PostgreSQL 空库初始化、旧版本升级回放和故障启动回放未执行；相关测试门禁和操作路径已保留，未将环境缺失伪造为通过。

## A03-01 业务修改与事件原子提交

- 日期：2026-09-09
- 推荐决策：`DurableEventPublisher.append()` 在 Integration Owner 内通过 `TransactionalOperator` 写入 Outbox；业务 Owner 的 command 仍可将业务状态写入与 append 放在同一外层 reactive transaction 中，由事务上下文参与同一提交。
- 原因：不能要求每个调用方记住隐藏的事务前置条件，也不能让 Outbox 写入脱离 PostgreSQL 事务；外层 command 事务仍是业务状态与 Outbox 的原子边界。
- 失败语义：业务写入或 Outbox 写入任一失败均回滚；非法事件类型、版本、Payload 或敏感字段在持久化前拒绝；不产生伪成功事件。
- 验证：`DurableEventServiceTest` 验证 append 经由 `TransactionalOperator` 执行并写入 Outbox；现有 Resource command 测试继续验证状态操作使用事务和事件能力。真实 PostgreSQL 回滚原子性测试需要 Docker Desktop + Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A03-02 重启后继续投递未完成事件

- 日期：2026-09-09
- 推荐决策：增加公开 `DurableEventConsumer` 契约和 Integration Owner 的 `OutboxDispatcher` 定时重扫；dispatcher 每次应用启动后从持久化 Outbox 查询 `dispatched_at IS NULL` 的事件，按稳定 consumer ID 投递。
- 原因：进程重启不能依赖内存队列恢复；Outbox 是唯一事件事实源，Inbox 去重和 `dispatched_at` 更新继续沿用现有至少一次投递语义。
- 失败语义：消费者失败时保留未完成 Outbox 事实和投递状态，下一次调度/重启继续扫描；dispatcher 记录脱敏错误并继续保留可重试状态，不把失败事件标记为已投递。
- 验证：`OutboxDispatcherTest` 验证重启后的重扫入口遍历稳定 consumer；`DurableEventServiceTest` 验证事件通过公开 consumer view 投递。真实 PostgreSQL 重启回放需要 Docker Desktop + Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A03-04 查询并重试投递失败事件

- 日期：2026-09-09
- 推荐决策：在 Integration dispatcher 提供 pending event 查询和按 event ID 的人工 retry capability；不新增尚未登记到 HTTP Operation Registry 的公开路由，未来由已登记的 Operations API 适配该 capability。
- 原因：失败事件必须可观察、可再次触发，但查询/重试不应绕过 Integration Owner、直接暴露 Outbox Repository 或重复产生业务 Event。重试仍使用同一 event ID、Inbox 唯一约束和 consumer 幂等语义。
- 失败语义：不存在或已完成事件返回 `NotFoundException`；没有可用 consumer 时明确失败；consumer 失败保留 pending 状态，下一次自动调度或人工 retry 可继续执行。
- 验证：`OutboxDispatcherTest` 覆盖 pending 查询和稳定 event ID retry；`DurableEventServiceTest` 覆盖不存在目标；与既有失败保留、重复跳过和 dispatcher 测试共同验证 10 项事件测试。真实 PostgreSQL 回放仍需要 Docker Desktop + Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A03 可靠事件投递（父 issue）

- 日期：2026-09-09
- 验收结论：A03-01 至 A03-04 已按顺序独立完成并在 GitHub 关闭；父功能覆盖 Outbox 原子写入、重启重扫、Inbox 原子去重、失败事件查询与人工重试。
- 推荐决策：继续由 Integration Owner 暴露 durable event capability，由已登记的上层 Operations API 适配查询/重试；当前不增加未登记的 HTTP 路由或 UI。事件 payload 保留 request/correlation/causation/actor 追踪字段。
- 验证证据：A03-04 事件回归测试 10 项全部通过；`mvn -pl application -am -DskipTests compile` BUILD SUCCESS；各子 issue 的独立测试与 commit 已记录在本日志及 GitHub 完成评论中。
- 剩余限制：当前开发环境未安装 Docker，真实 PostgreSQL 事务回滚、Outbox 重启回放、Inbox 唯一约束和跨 API 联调未执行；未将环境缺失伪造为通过。

## A04-01 提交任务并查询状态

- 日期：2026-09-09
- 推荐决策：补充 `POST /api/background-tasks` 作为持久化任务提交入口，使用 `Idempotency-Key`（可选时沿用现有 capability 语义），在 Service 完成持久化后返回 `202` 和 `Location`；查询继续使用已有单任务和分页列表能力。
- 原因：仅有内部 `submit` 方法不能形成可观察的独立操作路径；公开入口必须先登记 OpenAPI/HTTP Registry/Catalog，再接入 Controller。
- 失败语义：空请求或空 task type 在提交前拒绝；数据库持久化失败不返回 Accepted；相同 task type + idempotency key 重用已有任务，不创建第二个逻辑任务；后续查询对不存在任务返回 NotFound。
- 验证：`BackgroundTaskControllerTest` 覆盖 `202 + Location` 和空请求失败；既有 `BackgroundTaskDispatcherTest` 与 `BackgroundTaskLeaseValidationTest` 覆盖正常提交、查询、分页、幂等、失败与 Lease 失效边界。真实 PostgreSQL 约束和权限回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04-02 展示执行进度

- 日期：2026-09-09
- 推荐决策：沿用任务与 Attempt 分离模型，由持有有效 Lease 的 Worker 通过 `updateProgress` 写入 JSON progress，任务查询和控制台直接展示持久化 progress；不引入内存进度缓存或新的状态枚举。
- 原因：进度必须在重启、刷新和查询路径中可观察，且不能绕过 Task Owner 或让 progress 替代任务状态；控制台字段已对齐实际 `task_type`、`PENDING`、`SUCCEEDED` 和 `TIMED_OUT` 契约。
- 失败语义：无效/过期 Lease 不能更新进度；任务不存在返回 NotFound；刷新失败展示错误状态；失败、超时不显示为成功。
- 验证：`BackgroundTaskDispatcherTest` 新增 progress 持久化后通过 `get` 查询的断言；Operations 任务回归 14 tests passed；console `pnpm typecheck`、`pnpm lint`、`pnpm build` 均通过。lint 的自动格式化噪声已从无关文件恢复，仅保留本次 Background 页面变更。真实 PostgreSQL Lease 并发回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 外部权限记录：向 GitHub #920 发布完成评论和关闭 issue 的请求连续被安全策略拦截；本地实现与 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。

## A04-03 取消可取消任务

- 日期：2026-09-09
- 推荐决策：沿用现有 `cancel` + `acknowledgeCancellation` 两阶段语义：PENDING 任务立即进入 CANCELLED，RUNNING 任务只写入 `cancel_requested_at`，由 Worker 在安全检查点确认取消；SUCCEEDED/FAILED/TIMED_OUT 等终态拒绝取消。
- 原因：取消是 cooperative cancellation，不能强行终止正在执行的 Handler，也不能把取消伪装成失败；状态和 Attempt 历史保持可解释。
- 失败语义：不存在任务返回 NotFound；终态任务返回 Conflict；RUNNING 任务在确认前保持 RUNNING，其他任务不受影响。
- 验证：`BackgroundTaskDispatcherTest` 覆盖 PENDING 立即取消、RUNNING 仅请求取消、终态保护，以及已有 Worker cooperative cancellation；Operations 任务回归测试覆盖成功与失败分支。真实 PostgreSQL 并发/权限回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 外部权限记录：向 GitHub #921 发布完成评论的请求被安全策略拦截；本地实现与 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。

## A04-04 重试失败任务

- 日期：2026-09-09
- 推荐决策：人工重试不把旧 Task 从 FAILED/TIMED_OUT 改回 PENDING，而是创建新的 PENDING child Task，并通过 `parent_task_id` 保留执行关系；公开 retry action 已登记到 OpenAPI、HTTP Registry 和 Catalog。
- 原因：旧任务和失败 Attempt 是不可覆盖的执行历史；新 Task 可重新走正常 claim/lease/handler/complete 链路，避免重复改写已提交结果。
- 失败语义：不存在任务返回 NotFound；非 FAILED/TIMED_OUT 任务返回 Conflict；子任务执行失败保留具体失败状态，旧任务仍保持 FAILED/TIMED_OUT。
- 验证：`BackgroundTaskDispatcherTest` 覆盖旧任务保持失败、创建 child、child 成功恢复，以及既有失败和 retry 分支。真实 PostgreSQL 事务、权限和跨 API 回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04-05 Worker 中断后重新领取任务

- 日期：2026-09-09
- 推荐决策：继续使用持久化 Lease + Attempt 模型；Worker 中断后由下一次 claim 发现过期 Lease，将旧 Attempt 终结为 `LEASE_LOST`，再把同一逻辑 Task 重新置为 PENDING 并创建新的 Attempt，不创建重复逻辑 Task。
- 原因：Lease 是执行权而非任务身份，旧 Attempt 必须保留以支持诊断和恢复；数据库 claim 的 `FOR UPDATE SKIP LOCKED` 负责并发领取边界。
- 失败语义：Lease 失效不将任务伪装为成功；旧 Attempt 历史不可覆盖；取消请求在 Lease 回收时进入 CANCELLED 路径。
- 验证：`BackgroundTaskDispatcherTest` 强化中断恢复断言，验证重新领取后 attempt=2、旧 Attempt 为 `LEASE_LOST` 且历史包含两次 Attempt；既有 dispatcher/lease 测试继续覆盖成功与失败。真实 PostgreSQL 并发 claim 回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04-06 查看执行历史

- 日期：2026-09-09
- 推荐决策：通过已有 `GET /api/background-tasks/{task_id}/attempts` 查询由 Operations Owner 持有的 Attempt 历史，按 attempt number 稳定升序返回；不把历史压缩回 Task 当前状态，也不覆盖旧失败记录。
- 原因：执行历史需要区分每次 claim、Lease 失效、失败和完成，支持恢复诊断；Task 当前状态只表达逻辑任务，不足以替代 Attempt 事实。
- 失败语义：不存在任务返回 NotFound；空历史返回空结果；失败/Lease Lost 状态按事实返回，不显示为成功；查询只读，不改变任务状态。
- 验证：`BackgroundTaskControllerTest` 覆盖 Attempts 查询入口和 `LEASE_LOST` 历史事实；`BackgroundTaskDispatcherTest` 覆盖两次 Attempt 的恢复历史；Operations 任务回归测试覆盖成功与失败分支。真实 PostgreSQL 分页/权限回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04 后台任务管理（父 issue）

- 日期：2026-09-09
- 本地验收结论：A04-01 至 A04-06 已完成实现与自动化验收，覆盖提交/查询、进度、取消、人工重试、Lease 恢复和 Attempt 历史；主要 commits 为 `e09787f6`、`022bee7b`、`258b926d`、`928b4ea0`、`d86d5dfe`、`4243f26f`。
- 推荐决策：保持 Task 与 Attempt 分离；任务提交先持久化后返回 202；Worker 使用 Lease/`SKIP LOCKED` 领取，过期后保留 `LEASE_LOST` Attempt 并重试；人工 retry 创建 child Task；取消采用 cooperative cancellation。
- 验证证据：Operations 任务回归测试 17 项全部通过；console `pnpm typecheck`、`pnpm lint`、`pnpm build` 已通过；公开提交与 retry action 已登记 OpenAPI、HTTP Registry 和 Catalog。
- 剩余限制：Docker 未安装，真实 PostgreSQL 约束、事务、并发 claim、权限和跨 API 联调未执行；GitHub issue 评论/关闭受外部安全权限策略拦截，已在各子 issue 实施记录中登记，待权限恢复后补发。
