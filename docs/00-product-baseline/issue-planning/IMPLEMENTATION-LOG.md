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

## A05-01 初始化管理员

- 日期：2026-09-09
- 推荐决策：沿用首次注册作为初始化路径；首个用户在同一 reactive transaction 中写入平台用户、PBKDF2-SHA256 密码凭据并幂等分配 `admin` 初始角色，不创建 SecuritySession、Token Digest 或持久化令牌。
- 原因：当前 V2 无独立登录 Session 模型；首个用户是唯一明确的管理员初始化边界，后续登录/刷新由 A05-02/A05-03 负责。
- 失败语义：非法初始化输入在持久化前拒绝；用户名/邮箱冲突返回 Conflict；凭据或角色绑定失败由事务回滚，不返回伪成功；重复角色绑定保持幂等。
- 验证：`AuthenticationServiceTest` 覆盖首用户 admin 角色、PBKDF2 哈希、事务入口和非法输入；真实 PostgreSQL 回滚、唯一约束和初始化重放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 外部权限记录：向 GitHub #926 发布完成评论的请求被安全策略拦截；本地实现与 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。

## A05-02 用户登录与客户端退出

- 日期：2026-09-09
- 推荐决策：登录仅校验用户名、PBKDF2-SHA256 密码和用户 ACTIVE 状态后签发无状态 access/refresh JWT；不创建或返回 `session_id`，客户端登出只清理本地 token、用户状态和路由。
- 原因：P0 认证基线采用无状态 JWT，普通登出不能伪造后端会话撤销能力；服务端 `/auth/logout` 继续作为兼容端点，但不持久化会话状态。
- 失败语义：未知用户、错误密码和停用用户统一返回稳定的认证失败错误，不泄露账号存在性；无效输入不触发 token 签发。
- 验证：`AuthenticationServiceTest` 覆盖登录 token pair、无 `sessionId` 和空操作登出；认证回归测试共 13 项通过；console `pnpm typecheck` 与 `pnpm build` 通过。
- 外部权限记录：向 GitHub #927 发布完成评论的请求尚未获安全策略授权；本地实现与后续 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。
- 本地权限记录：提交时 Git 无法创建 `.git/index.lock`，已记录并申请受控权限重试；不影响代码验证，继续按 issue 顺序推进。
- 外部权限记录：向 GitHub #927 发布完成评论并关闭 issue 的请求因安全权限审批超时未执行；不能视为已评论或已关闭，待权限恢复后补发。根据执行规则继续处理 A05-03。

## A05-03 Access / Refresh JWT 签发与刷新

- 日期：2026-09-09
- 推荐决策：复用集中式 `JwtTokenService` 签发带 `sub`、`jti`、`security_version`、`iat`、`exp`、issuer 和 token kind 的 Access/Refresh JWT；刷新只接受签名有效的 Refresh JWT，并重新读取 ACTIVE 用户与当前 `security_version`。
- 原因：保持无状态认证，不引入 Login Session、Refresh Token Digest 或 Controller 私自拼装 token；用户级失效由后续 A05-05 提升 `security_version`。
- 失败语义：过期、错误类型、错误签名和版本不匹配均拒绝继续认证/刷新；JWT 原文不写入数据库、事件、审计或普通日志。
- 验证：`JwtTokenServiceTest` 覆盖 Access/Refresh 类型隔离、唯一 jti、过期和篡改拒绝；`AuthenticationServiceTest` 覆盖刷新时 `security_version` 不匹配及无效 token 拒绝；认证回归共 15 项通过。

## A05-04 无状态 JWT 校验与 security_version

- 日期：2026-09-09
- 推荐决策：由最高优先级 WebFilter 统一解析 Bearer Access JWT，校验签名、时间和 token kind 后读取主体用户，仅 ACTIVE 且 `security_version` 一致时构造 `AuthenticatedPrincipal`；不查询 Session。
- 原因：请求认证必须以当前用户状态和安全版本为权威，避免 Session、`sid` 或 Token Digest 重新成为隐式登录态。
- 失败语义：缺失/格式错误 Bearer、错误签名、过期、Refresh 类型、禁用用户、未知用户和版本过期均返回 401，且不把 token 原文写入日志或错误响应。
- 验证：`JwtAuthenticationWebFilterTest` 覆盖合法 Access 身份注入、非法 token、错误类型、过期、禁用用户和 stale `security_version`；相关认证回归共 10 项通过。

## A05-05 用户级旧 Token 全量失效

- 日期：2026-09-09
- 推荐决策：新增 `POST /api/me/actions/invalidate-tokens` 自助入口和 `POST /api/users/{userId}/actions/invalidate-tokens` 管理入口；目标用户 `security_version` 原子递增，随后在同一 reactive transaction 内追加 `authentication.user.tokens-invalidated` durable event 与审计记录。
- 原因：用户级 Token 失效是安全纪元变化，不是 Session 撤销；旧 Access/Refresh JWT 会在 A05-03/A05-04 的版本校验中自然失效，不保存 token、digest、设备或 Session 状态。
- 权限选择：自助入口要求已认证主体且目标固定为自身；管理员入口沿用 `system.user.manage` 的用户管理权限，拒绝逻辑由统一授权过滤器执行。
- 失败语义：未知用户返回 NotFound；版本递增失败、事件或审计失败不返回伪成功；事件 payload 仅包含 user_id 与新 security_version。
- 验证：`DefaultUserServiceTest` 覆盖版本递增、事务入口、durable event payload 和审计；认证/授权回归共 26 项通过。真实 PostgreSQL 乐观并发、事务回滚和权限联调仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A05 账号与 JWT 认证（父 issue）

- 日期：2026-09-09
- 本地验收结论：A05-01 至 A05-05 已完成；覆盖首次管理员初始化、无状态登录/客户端退出、Access/Refresh JWT、请求侧 security_version 校验和用户级 Token 全量失效。
- 主要 commits：`1c156217`、`ab1e239b`、`4f557967`、`a60c4998`、`5ca5d087`。
- 统一决策：不建立 Login/Security Session 或 Token Digest；普通登出由客户端清理本地凭证，紧急全量失效通过递增用户 `security_version`，并产生最小化 durable event 与审计。
- 验证证据：认证/授权相关回归通过；console `pnpm typecheck` 与 `pnpm build` 通过。真实 PostgreSQL/Testcontainers 并发、事务回滚和完整权限联调仍受当前环境 Docker 未安装限制，未伪造运行证据。

## A06-01 创建角色并配置权限

- 日期：2026-09-09
- 推荐决策：复用 `RoleController`/`DefaultRoleService` 的公开创建与权限配置路径；角色权限只能接受 `PlatformPermission` 注册枚举，拒绝任意未声明权限，角色创建输入在 Application 层再次校验。
- 失败语义：非法角色资料在持久化前拒绝；重复角色编码返回 Conflict；不存在角色返回 NotFound；权限变更写入审计并发布 durable authorization event，不包含认证材料。
- 验证：`DefaultRoleServiceTest` 覆盖合法角色创建、非法输入、已声明权限授予、权限列表和边界；授权过滤器回归覆盖直接 API 的认证/权限拒绝路径；本轮授权回归 20 项通过。

## A06-02 分配和撤销用户角色

- 日期：2026-09-09
- 推荐决策：通过 `RoleController` 暴露用户-角色绑定与撤销动作；绑定前确认角色存在，重复绑定保持幂等，撤销只删除指定 user/role 绑定，不影响其他用户或角色；两类动作均写审计。
- 失败语义：角色不存在返回 NotFound；重复分配不创建重复绑定；撤销目标不存在保持幂等完成；权限入口由统一 `system.role.manage` 授权过滤器保护。
- 验证：`DefaultRoleServiceTest` 覆盖分配、撤销、重复/指定目标边界；授权回归 21 项通过。真实 PostgreSQL 唯一约束和并发绑定回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A09-01 创建资源（A06-03 前置 issue）

- 日期：2026-09-09
- 推荐决策：复用 Resource Owner 的创建路径，以 `owner_id` 固定资源归属，同时在同一事务创建首个主标题；创建结果支持 `Idempotency-Key` replay/conflict，避免重试产生重复资源。
- 失败语义：非法类型/标题/locale 在 Application 层拒绝；幂等 key 同请求返回原资源，不同请求返回 Conflict；资源、标题、事件、审计或幂等记录任一失败不返回伪成功。
- 验证：`DefaultResourceServiceTest` 覆盖创建及首标题、非法输入、幂等重放/冲突和审计事件；资源回归 13 项通过。真实 PostgreSQL 约束、事务回滚和并发幂等仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 追加验证：补充跨 owner 读取拒绝测试，资源服务回归 `DefaultResourceServiceTest` 8 项通过。

## A06-03 隔离用户私有资源

- 日期：2026-09-09
- 推荐决策：Resource Owner 是唯一资源隔离边界；所有单资源读取/变更通过 `findByIdAndOwnerId`，列表与计数 SQL 同时限定 `owner_id`，未授权对象统一表现为 NotFound，不向调用方泄露存在性。
- 原因：资源模块已实现 Resource-centric 私有模型，A06-03 只补充授权验收，不引入跨模块 Repository 或额外 `tenant_id`。
- 失败语义：不同用户读取、修改或通过外部身份查找私有资源均不得获得目标数据；失败不触发标题、Blob 或审计写入。
- 验证：`DefaultResourceServiceTest` 覆盖跨 owner 读取拒绝、创建 owner 固定和资源幂等；资源回归通过。真实 PostgreSQL SQL 隔离和 API 联调仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A06-04 校验资源变更权限

- 日期：2026-09-09
- 推荐决策：资源变更统一经过 `ResourceAuthorizationWebFilter`，按 HTTP 方法和路径要求 `resource.write`/`resource.delete` 等注册权限；业务服务继续执行 owner 与领域不变量校验，HTTP 门禁不替代领域授权。
- 失败语义：无认证主体返回 401；有 Access JWT 但缺少资源变更权限返回 403；拒绝路径不进入 Controller/Repository，不泄露资源数据。
- 验证：`ResourceAuthorizationWebFilterTest` 新增资源写操作“仅读权限拒绝/写权限通过”覆盖，授权回归 16 项通过；资源服务跨 owner 隔离测试已通过。

## A06-05 权限撤销后阻止后续访问

- 日期：2026-09-09
- 推荐决策：资源路径继续先检查 JWT 权限快照，再通过 `AccessControlService` 实时读取当前 user-role/role-permission 绑定；权限撤销后，即使旧 JWT 尚未过期，后续资源请求也返回 403。
- 原因：JWT 权限快照用于快速拒绝，但不能作为最终 ACL；实时 capability 复核满足撤销即时生效，同时不引入 Session 或 Token Digest。
- 失败语义：当前 RBAC capability 拒绝时不调用下游 Controller；错误统一映射为 403，不泄露目标资源或 token 信息。
- 验证：`ResourceAuthorizationWebFilterTest` 覆盖角色撤销后实时 capability 拒绝，授权过滤器回归 14 项通过；资源 owner 隔离回归已通过。

## A06 角色与授权（父 issue）

- 日期：2026-09-09
- 本地验收结论：A06-01 至 A06-05 已完成，覆盖角色创建/权限配置、用户角色分配/撤销、私有资源 owner 隔离、资源变更权限和撤销后的实时访问阻断。
- 主要 commits：`1b793395`、`7fb67818`、`af4a7cb8`、`49927c25`、`ef1a5f59`；A09-01 前置 commits：`645954b1`、`43441b91`。
- 统一决策：权限注册枚举和统一授权过滤器负责入口门禁，领域服务负责 owner/状态不变量；资源访问在 JWT 快速检查后实时复核当前 RBAC，避免权限撤销等待 token 过期。
- 验证证据：授权回归通过，资源服务 owner 隔离回归通过；真实 PostgreSQL 唯一约束、并发绑定、事务回滚和完整 API 联调仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A07-01 发起二次验证

- 日期：2026-09-09
- 推荐决策：复用 Email OTP Provider 与公开 `POST /api/security/verification-challenges` 入口；发起前只接受 ACTIVE 且配置可验证邮箱的用户，挑战绑定 purpose/target_reference 并设置短时有效期。
- 安全边界：数据库只保存 OTP 慢哈希摘要；响应、审计和普通日志不返回 OTP、摘要或目标邮箱；发起频率按用户窗口限制，直接 API 与错误身份均走同一 Application Provider。
- 失败语义：未知用户、非 ACTIVE 用户或无邮箱用户返回 NotFound；频率超限返回 Conflict；失败前不创建挑战。
- 验证：`EmailOtpVerificationProviderTest` 覆盖合法发起、未知/无邮箱身份拒绝、频率限制和摘要持久化；`VerificationControllerTest` 覆盖公开入口、202 响应及 OTP 字段不泄露；本轮共 9 项通过。真实 PostgreSQL 事务和邮件渠道联调留待 A07-05/集成环境验证。

## A07-05 接入真实验证码发送渠道

- 日期：2026-09-09
- 推荐决策：新增可配置 HTTP 邮件网关 Adapter；仅当 `IKAROS_SECURITY_EMAIL_DELIVERY=HTTP` 且 endpoint/from 完整配置时启用，默认保留无外发的 Noop 实现，避免未配置环境误发或启动失败。
- 安全边界：网关 API Key 只从环境 Secret 注入 Authorization Header；OTP 仅存在当前投递调用体，错误只返回固定非敏感信息，不记录网关响应正文或验证码。
- 失败语义：用户邮箱不可用或网关返回错误均失败，不产生伪成功；发送仍由 Email OTP Provider 的用途绑定、过期和频率限制控制。
- 验证：`HttpEmailOtpDeliveryTest` 覆盖 HTTP 成功、Authorization Secret 传递、网关错误脱敏；连同 A07-01 回归共 11 项通过。真实第三方邮件网关联调需要部署环境提供 endpoint/from/API Key，当前未发送外部邮件。

## A07-02 验证成功后执行限定操作

- 日期：2026-09-09
- 推荐决策：Step-up 成功后只签发短期、用途绑定的 Verification Grant；不修改 Session、权限或密钥。Grant 携带主体、`security_version`、purpose、SVL、签发/过期时间，由后续高风险 Command 与 Permission/Security Policy 一并校验。
- 失败语义：挑战用途/目标不匹配或用户非 ACTIVE 时拒绝；用户状态检查必须先于 OTP 消费，拒绝路径不得执行验证 Provider 或签发 Grant。
- 验证：`DefaultStepUpVerificationServiceTest` 覆盖 Grant claim、主体/用途/SVL/有效期、目标绑定和停用用户拒绝；4 项通过。

## A07-03 验证过期后拒绝执行

- 日期：2026-09-09
- 推荐决策：挑战过期时立即转为 EXPIRED 并拒绝；已 VERIFIED/LOCKED 等终态不可再次验证。Verification Grant 由 JWT `exp` 约束，后续 Security Policy 还必须检查 verification expiry，不以 Access JWT 有效替代 Step-up 有效。
- 失败语义：过期挑战返回 Conflict，拒绝 OTP 匹配与 Grant 签发；已消费挑战不可重放；过期验证保证不执行限定操作。
- 验证：`EmailOtpVerificationProviderTest` 新增过期挑战状态转换与已消费挑战重放拒绝；`DefaultAccessControlServiceTest` 已覆盖过期 SVL 拒绝；本轮认证/授权相关回归共 14 项通过。

## A07-04 限制验证失败重试

- 日期：2026-09-09
- 推荐决策：每次错误 OTP 都持久化递增 attempt_count；达到 5 次将挑战置为 LOCKED，后续请求由终态门禁拒绝；发起挑战另按用户 10 分钟窗口最多 3 次限制。
- 失败语义：中间失败只产生一次失败审计与状态更新，不执行任何业务副作用；锁定后不再匹配 OTP、不重置计数、不允许重放。
- 验证：`EmailOtpVerificationProviderTest` 覆盖中间失败计数、最终锁定、锁定终态和发起频率限制；本轮 11 项通过。

## A07 敏感操作验证（父 issue）

- 日期：2026-09-09
- 本地验收结论：A07-01、A07-05、A07-02、A07-03、A07-04 已按依赖顺序完成；覆盖二次验证发起、真实邮件渠道、成功后短期 Grant、过期拒绝和失败重试限制。
- 主要 commits：`7a78a788`、`49f168cf`、`8e4036f1`、`9acb627d`、`67a076d0`。
- 统一决策：不建立 Session 或保存 OTP/Grant 明文；HTTP 邮件渠道显式配置启用；Grant 必须同时满足主体、用途、SVL、有效期和后续权限策略；状态校验先于 OTP 消费。
- 验证证据：认证验证相关回归通过（本轮最大组合 14 项，邮件渠道 11 项）；真实第三方邮件与 PostgreSQL/Testcontainers 联调仍需部署环境，未伪造运行证据。

## A08-01 记录资源管理操作

- 日期：2026-09-09
- 推荐决策：资源管理 Application 服务统一通过 `AuditService` 记录 create/update/trash/archive/restore 及标题、标签、收藏、关系等操作；审计事件独立存储于 `audit_event`，不复用 Resource Activity。
- 失败语义：审计写入失败沿调用链传播，不返回伪成功；事件保留 actor、action、target、occurred_at、request/correlation context，details 仅允许脱敏 JSON。
- 验证：新增 `DefaultAuditServiceTest` 覆盖用户资源更新、系统归档、目标关联和独立审计落库契约；现有资源服务测试覆盖各资源管理入口的 `AuditService` 调用；本轮审计模块 2 项通过。

## A08-02 记录权限与凭据操作

- 日期：2026-09-09
- 推荐决策：权限与凭据变更继续统一调用 `AuditService`：角色权限授予/替换、用户角色分配/撤销、Token 全量失效、验证挑战发起/成功/失败/取消均写入独立 `audit_event`；不保存 JWT、OTP、Grant 或 Secret 明文。
- 失败语义：审计写入失败沿业务调用链传播；权限/凭据拒绝不产生成功审计或伪成功状态，审计详情保持最小化脱敏 JSON。
- 验证：`DefaultRoleServiceTest` 6 项、`DefaultUserServiceTest` 7 项、`EmailOtpVerificationProviderTest` 11 项通过，覆盖权限与凭据操作的正常及失败路径；相关回归共 24 项通过。

## A08-03 按操作者和时间查询

- 日期：2026-09-09
- 推荐决策：新增 `GET /api/audit-events` 查询路径，支持 `actor_id`、RFC 3339 `from/to` 和稳定 page/size 分页；SQL 使用 `occurred_at desc, id desc`，避免同时间事件分页重复/遗漏。
- 权限边界：统一授权过滤器将查询映射到 `system.audit.read`，并实时复核当前 RBAC；审计查询不允许无认证或无权限直接进入 Controller。
- 失败语义：非法时间范围、页码或页大小在 Application 层拒绝；空结果返回空页，不伪造成功事件或泄露目标数据。
- 验证：`AuditQueryServiceTest` 覆盖操作者/时间过滤、offset 分页、稳定查询参数和非法范围；`ResourceAuthorizationWebFilterTest` 覆盖审计读取权限与实时角色校验；相关回归共 19 项通过。

## A08-04 查看操作结果与关联对象

- 日期：2026-09-09
- 推荐决策：在审计查询路径增加 `GET /api/audit-events/{event_id}`，返回审计事件的 actor、action、target、结果详情及 request/correlation 关联信息；查询仍受 `system.audit.read` 统一权限门禁保护。
- 失败语义：不存在的审计事件返回稳定 NotFound；审计记录与 Resource Activity 保持独立，分页查询和单条查询均不伪造成功状态。
- 验证：`AuditQueryServiceTest` 覆盖单条事件读取与目标不存在；`ResourceAuthorizationWebFilterTest` 覆盖审计查询权限和实时 RBAC；相关回归共 18 项通过。敏感字段进一步脱敏由 A08-05 处理。

## A08-05 隐藏敏感字段

- 日期：2026-09-09
- 推荐决策：在 `DefaultAuditService` 的统一写入边界集中脱敏 JSON 中的 password、token、otp、secret、credential、authorization、api_key、private_key、code 等敏感字段；调用方即使误传明文也不会直接落库。
- 安全边界：审计仍不保存 JWT、OTP、Verification Grant、密码或 Secret 明文；固定替换为 `[REDACTED]`，保留非敏感结果字段和审计主体/目标关联。
- 失败语义：空详情归一为 `{}`；审计事件仍独立落库，脱敏不改变审计成功/失败语义。
- 验证：`DefaultAuditServiceTest` 覆盖用户/系统事件和敏感 token/password 值落库前脱敏；operations 模块 3 项通过。

## A08 操作审计（父 issue）

- 日期：2026-09-09
- 本地验收结论：A08-01 至 A08-05 已按顺序完成，覆盖资源管理审计、权限/凭据审计、按操作者/时间查询、单条关联对象查看和敏感字段脱敏。
- 主要 commits：`2fc5c986`、`40cfa377`、`86b6448c`、`3511cd42`、`0f50041d`。
- 统一决策：审计与 Resource Activity 分离；查询要求 `system.audit.read` 并实时复核 RBAC；事件按稳定时间/id 排序；写入边界集中脱敏且不保存认证材料明文。
- 验证证据：审计与授权相关回归通过；operations 查询/写入测试和授权过滤器测试均通过。真实 PostgreSQL 分页 SQL、约束和完整 API 联调仍需 Docker/Testcontainers，未伪造运行证据。

## A09-02 浏览列表和详情

- 日期：2026-09-09
- 推荐决策：复用 Resource Owner 的列表/详情 API；列表 SQL 固定限定 `owner_id` 与 ACTIVE 生命周期，详情使用 `findByIdAndOwnerId`，分页按稳定 `updated_at` 顺序返回 ResourceView、标题和外部身份。
- 失败语义：跨 owner 或不存在资源统一 NotFound；空结果返回空页；非法分页参数在 Application 层拒绝，不进入 Repository。
- 验证：`DefaultResourceServiceTest` 覆盖 owner-scoped 分页列表、空列表、跨 owner 详情拒绝和非法分页；resource 服务回归 10 项通过。

## A09-03 编辑时检测版本冲突

- 日期：2026-09-09
- 推荐决策：更新必须携带 expected_version/If-Match；Application 层先按 owner 读取并比较版本，持久化继续使用 Resource `@Version` 乐观并发，不允许旧版本静默覆盖新值。
- 失败语义：版本过期返回稳定 Conflict；冲突路径不写 Resource、标题、事件或审计，不改变 Resource 身份或共享 Blob。
- 验证：`DefaultResourceServiceTest` 以两次重复更新复现首个版本提交后旧版本拒绝，确认只发生一次保存；resource 回归 11 项通过。

## A09-04 归档资源

- 日期：2026-09-09
- 推荐决策：仅 ACTIVE Resource 可归档；归档保持 Resource UUID、owner、标题和任何 Attachment/Blob 身份不变，只更新生命周期与版本并发布事件/审计。
- 失败语义：跨 owner/不存在由 owner-scoped 查询统一 NotFound；TRASHED 等非 ACTIVE 状态返回 Conflict；拒绝路径不写 Resource、不触碰 Blob。
- 验证：`DefaultResourceServiceTest` 覆盖 ACTIVE 归档成功、Resource 身份保留、TRASHED 状态拒绝和无写入；resource 回归 13 项通过。

## A09-05 移入回收站

- 日期：2026-09-09
- 推荐决策：移入回收站是 Resource 的逻辑生命周期变更；状态、事件和审计在同一 reactive transaction 内提交，绝不直接删除 Attachment/Blob。
- 失败语义：跨 owner/不存在统一 NotFound；旧版本按 If-Match 拒绝；重复处理已 TRASHED 资源保持幂等且不重复写入、审计或业务副作用。
- 验证：`DefaultResourceServiceTest` 覆盖正常移入、身份/Blob 保留、重复幂等和版本边界；resource 回归 14 项通过。
