# Ikaros V2 实施记录

## A01-01 明确现有工程与设计文档差异

- 日期：2026-09-10
- 决策：保留 Maven Multi-Module、Java 21、Spring Boot 4.x、WebFlux、R2DBC、PostgreSQL 与 `r2dbc-migrate` 基线；不切换 Gradle，不引入 JPA/JDBC/Flyway 第二持久化栈。
- 差异记录：新增 `A01-01-ARCHITECTURE-DIFF.md`，记录既有模块分层、显式组装和自动化门禁尚未完全收敛的差异及后续处理边界。
- 验证：根 `pom.xml` 聚合包含 `application` Server；上述架构决策与 Technical Architecture 文档一致；application package 已在后续音乐功能验证中 BUILD SUCCESS。
- 主要提交：`83d31391`。

## A01-02 确定模块依赖边界

- 日期：2026-09-10
- 决策：保持 Maven Multi-Module；`*-api` 只承载公开契约，业务实现默认只依赖 API，`application` 是唯一 Composition Root。当前 4 条历史实现直连已登记为迁移白名单，不作为目标架构合规证明。
- 实现：新增 `A01-02-MODULE-BOUNDARY.md` 和 `ModuleDependencyBoundaryTest`；测试读取根 POM 及各模块实际依赖，禁止 API 反向依赖实现、业务模块依赖 `application`，并阻止新增白名单外的实现直连。
- Console：本 issue 是工程边界治理，不产生用户操作页面；相关业务页面继续由各功能 issue 负责接入真实 API。
- 验证：`ModuleDependencyBoundaryTest` 2/2 通过，覆盖当前真实 POM 通过样例和模拟新增实现直连失败样例；Maven application reactor BUILD SUCCESS。
- 主要提交：`031454d1`、`9d134ae8`。

## A01-03 检查公开 API 与契约一致性

- 日期：2026-09-10
- 实现：修正 Registry 中带 `{parameter}` 路径未加引号的 YAML 语法；修正主 OpenAPI 的 `components` 缩进；将 `POST /music/queues/{queue_id}/entries` 放回正确的 path item；补齐附件可用性 operation 的 contract id。
- 测试增强：Registry/OpenAPI 集合不一致时现在报告具体 `missingFromOpenApi` 和 `missingFromRegistry`，path item/operation 类型错误也报告具体来源和路径。
- Console：本 issue 是公开契约治理，不新增独立页面；已完成的 Console 功能继续以同一 Registry/OpenAPI 契约为 API 边界。
- 验证：`ControllerRouteConventionTest` 2/2、`HttpOperationRegistryTest` 2/2、`OpenApiRouteConventionTest` 1/1、`PublicApiContractTest` 4/4，共 9 项通过；Maven application reactor BUILD SUCCESS。
- 主要提交：`0fea5aaa`、`e5e1d1cf`。

## A01-04 建立提交检查门禁

- 日期：2026-09-10
- 实现审计：复用现有 `.github/workflows/ikaros-server-ci.yml` PR 门禁；它在 `main`/`release-*` pull request 上执行 Maven compile、全量 test、application 架构/契约测试、verify/package，以及 Console 依赖安装、typecheck、lint 和 build。
- 失败语义：任一 Maven、架构/契约或 Console 质量步骤失败即阻止门禁通过；本地静态断言已确认工作流包含上述全部关键步骤，`git diff --check` 通过。
- Console：Console 已作为同一 PR 门禁的独立质量 job，不是只检查后端。
- 验证：工作流配置关键步骤断言通过；A01-02 边界测试 2/2、A01-03 API/路由测试 9/9、Console `pnpm typecheck` 与 `pnpm build` 均已通过。
- 主要提交：复用既有 CI 配置；本轮提交门禁验收追溯记录。

## A01 工程实施基线（父 issue）

- 日期：2026-09-10
- 本地验收结论：A01-01 至 A01-04 已按顺序完成；明确架构差异和 Maven 取舍，确定模块依赖边界，修复并验证 Registry/OpenAPI 收敛问题，并确认 PR 门禁覆盖 Server 与 Console。
- 主要提交：`83d31391`、`031454d1`、`9d134ae8`、`9632c6a7`、`0fea5aaa`、`e5e1d1cf`、`37f5562b`。
- Console：A01-04 的门禁包含 Console typecheck、lint、build；A01-01/A01-02/A01-03 属于工程/契约治理，不产生独立业务页面。
- 未伪造项：4 条历史实现模块直连已显式登记迁移白名单，未宣称其已完成最终架构收敛。

## A17 Console 对接逐项审计

- A17-01：附件列表/详情读取并展示真实 `availability`；恢复页展示恢复队列和当前状态。
- A17-02：附件恢复入口调用 `POST /attachments/{attachmentId}/restore-requests`，每次提交携带幂等键。
- A17-03：恢复页加载 `GET /restore-requests`，展示总大小、已完成项、状态和预算决策，并提供加载/空状态。
- A17-04：FAILED/PARTIAL 任务才显示重试按钮，调用 `POST /restore-requests/{id}/actions/retry`；PENDING/ACTIVE 支持取消。
- A17-05：页面加载 `GET /admin/restore-budget-policy` 展示预算门槛；超预算错误保留后端响应，不伪造确认或成功。
- A17-06：恢复完成后附件详情仍通过真实可用性和预览/下载授权链访问，不直接读取物理路径。
- 验证：`/storage-center/archive`、`/storage-center/attachments` 和附件详情页均返回 HTTP 200；Console typecheck/build 已通过。

## A02-01 首次启动检查必需配置

- 日期：2026-09-10
- 实现审计：复用现有 `StartupConfigurationValidator`；启动时强制检查 PostgreSQL R2DBC URL、数据库凭据、JWT issuer/secret/TTL，拒绝空值、错误驱动、占位或过短 secret 以及非法 duration。
- Console：`系统运维 → 系统健康` 页面读取 live/ready/operations 和 Storage Provider 探针，异常显示为 DOWN/DEGRADED，不伪造健康状态。
- 验证：`StartupConfigurationValidatorTest` 3/3；application reactor BUILD SUCCESS；Console 健康页路由 `/operations-center/health` 返回 200；诊断输出不包含配置值。
- 主要提交：复用既有实现，本轮提交验收追溯记录。

## A02-02 初始化数据库（复核）

- 日期：2026-09-10
- 复核结果：发现并修正 migration 契约测试对 Maven reactor 聚合 classpath 中同内容副本的误报；现在同名 migration 必须内容一致，逻辑版本仍必须唯一，真实冲突仍失败。
- Console：数据库初始化没有独立业务操作页；`系统运维 → 系统健康` 通过 readiness 探针展示迁移完成后的可用状态，启动未完成或数据库故障显示 DOWN。
- 验证：`DatabaseInitializationContractTest` 2/2；`HealthControllerTest` 3/3；application/operations reactor BUILD SUCCESS；当前真实空库/升级故障回放仍受 Docker Desktop 缺失限制，未伪造通过。
- 主要提交：本轮提交 migration 契约测试修正与复核记录。

## A02-03 已有实例执行版本升级（环境门禁）

- 日期：2026-09-10
- 当前实现：`r2dbc-migrate` 启动时读取 migration history，只执行 pending migration，并启用等待数据库与 PostgreSQL advisory lock；当前本地运行实例已报告数据库版本 `202609100900`。
- 未完成项：当前环境没有 Docker CLI，无法执行真实旧版本 PostgreSQL → 当前版本的 Testcontainers 回放；不将配置审查当作升级回放通过。
- 下一步：获得 Docker Desktop/PostgreSQL 环境后，补执行旧版本、pending migration、重启幂等和失败回滚/未就绪场景，再提交该 issue 的完成评论。

## A02-04 升级失败时阻止服务进入就绪状态（复核）

- 日期：2026-09-10
- 实现审计：`ApplicationReadiness` 只有在完整 Spring 启动成功后才开放；readiness 在启动未完成或 `select 1` 失败时返回 DOWN/503，liveness 不依赖数据库。
- Console：`系统运维 → 系统健康` 读取 readiness 原始状态并显示异常，未将数据库可连接推导为应用已完成迁移。
- 验证：`HealthControllerTest` 3/3；operations reactor BUILD SUCCESS；Console `/operations-center/health` 返回 200；真实 migration 失败启动回放仍受 Docker 缺失限制。

## A05-01 初始化管理员

- 日期：2026-09-10
- 实现：复用 Authentication 的原子注册流程；首个用户在同一事务中创建凭据并获得 `admin` 初始角色，密码仅保存 PBKDF2 哈希，不返回或保存明文。
- Console：`/setup` 向导真实检查 `/health/ready`，绑定管理员用户名/显示名/邮箱/密码，调用 `/auth/register`，展示校验、加载和失败状态，成功后跳转登录；不展示密码摘要。
- 验证：`AuthenticationServiceTest` 4/4；Console `pnpm typecheck` 与 `pnpm build` 通过；Console `/setup` 返回 200；主要提交：`8f49bc35`。

## A15-01 添加 Provider

- 日期：2026-09-10
- 实现审计：Storage Provider Registry 支持校验 Provider 标识、类型、层级和 `secret://` 引用，保存凭据时加密；重复标识、缺失凭据和明文凭据 metadata 拒绝且不落库。
- Console：`存储 → Provider` 页面真实加载 Provider，支持创建、启用/停用、探测、状态查看和凭据更新，并提供加载/失败反馈；凭据响应不展示明文。
- 验证：`PersistentStorageProviderRegistryTest` 2/2；Console 页面 `/storage-center/providers` 返回 200；未认证创建请求返回 401；主要提交：本轮测试补强与追溯记录。

## A05-02 用户登录与退出（复核）

- 日期：2026-09-10
- 实现审计：登录、refresh 和注册均调用公开 Authentication API；补齐 Console 用户 store 的退出动作，调用 `/auth/logout` 后无论网络结果如何清除本地 token、权限和路由状态。
- Console：登录页、登录失败反馈、自动 refresh 和退出后的回登录页均走真实 API/本地状态清理，不把 JWT 或密码显示在页面。
- 验证：`AuthenticationServiceTest` 4/4；Console typecheck 通过；退出接口仍由无状态 JWT 设计提供 204，未认证业务请求继续受保护；主要提交：待本轮提交。

## A06-01 创建角色并配置权限

- 日期：2026-09-10
- 实现审计：角色服务校验角色输入，只允许 Permission Registry 中声明的权限，授权替换、用户角色分配/撤销均经过服务端边界并写审计事件。
- Console：`安全 → 角色与权限` 页面真实加载角色和权限，支持创建角色、筛选权限、保存授权并显示加载/错误状态。
- 验证：`DefaultRoleServiceTest` 6/6；Console `pnpm build` 通过；`/api/admin/roles` 与 `/api/admin/permissions` 未认证均返回 401，Console `/security/permissions` 返回 200；本轮复用既有功能，仅提交验收追溯记录。

## A18 副本与 Blob 清理（父 issue）

- 日期：2026-09-09
- 验收结论：A18-01 至 A18-06 已按顺序完成并在 GitHub 关闭；覆盖完整性校验、异常发现、健康副本修复、GC 预览、执行前保护检查和带审计的物理清理。
- 关键不变量：Blob 内容身份不可变；修复创建新 Placement；损坏副本隔离；GC 执行时重新检查引用、Hold、Lease 和 Archive Base；成功清理写入 AuditService 与 durable events。
- 验证证据：本轮新增/执行的 storage 测试均 BUILD SUCCESS；完整 PostgreSQL/Testcontainers 联调仍受当前环境 Docker 不可用限制，已保留为环境门禁。
- 主要 commits：`72c94d91`、`1f778200`、`28f673eb`、`e91e1ca3`、`9ad128d5`、`bd47ff6b`。
- Console 回溯：`存储层` 页面已接入 Blob 副本诊断、Placement 列表和幂等修复任务入口；修复结果仍以后台任务状态为准，不在页面伪造完成。

## A18-06 执行清理并保留审计记录

- 日期：2026-09-09
- 推荐决策：`storage.blob-gc` Background Task 逐个执行候选清理；成功 purge 同时写入统一 `AuditService` 的 `blob.gc.purge` 记录和 `storage.blob.purged` durable event，记录任务、执行主体和清理 Placement 数量。
- 原因：物理删除必须可追溯，且任务执行时仍需复用 A18-05 的引用/保留门禁；审计详情不包含 Secret 或物理凭据。
- 失败语义：任一保护条件、Provider 不可用或删除失败均不报告伪成功；只有实际完成 Blob/Placement 删除后才写入成功审计和完成事件。
- 验证：`mvn -pl storage -am -Dtest=BlobGcTaskHandlerTest,BlobGarbageCollectorTest -Dsurefire.failIfNoSpecifiedTests=false test`；覆盖成功清理审计、有效引用、Retention Hold、Archive Base 阻止分支，共 4 个测试全部通过。

## A18-05 检查引用与保留条件

- 日期：2026-09-09
- 推荐决策：Blob GC 执行前在事务链内重新检查有效 Attachment 引用、Retention Hold、Delivery Lease 和 Archive Base；任一保护条件存在即拒绝清理。
- 原因：候选预览与实际执行之间可能发生引用或保护状态变化，必须以执行时 Owner 状态为准，且 Archive Base 不能被普通 GC 静默删除。
- 失败语义：保护条件命中返回 Conflict，不删除 Provider 对象、Placement 或 Blob；Blob/Provider 不存在也拒绝，不产生伪成功。
- 验证：`mvn -pl storage -am -Dtest=BlobGarbageCollectorTest,BlobGarbageCollectionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`；覆盖有效引用、Retention Hold、Archive Base 三个阻止分支及预览入口，共 5 个测试全部通过。

## A18-04 预览可清理 Blob

- 日期：2026-09-09
- 推荐决策：通过 `GET /api/storage/gc/candidates` 只生成候选预览，不执行物理删除；候选必须无有效 Attachment 引用且已超过最小保留期，结果包含 Blob 身份、摘要、大小和可清理时间。
- 原因：预览与执行分离，避免扫描误删；数据库查询以所有未删除 Attachment 引用保护共享 Blob，后续执行仍需再次确认引用与保留条件。
- 失败语义：limit 超出 1–500、最小保留期为空/为负时拒绝；预览不改变 Blob、Placement 或 Attachment 状态，也不影响其他用户和未选中对象。
- 验证：`mvn -pl storage -am -Dtest=BlobGarbageCollectionControllerTest,DefaultStorageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；覆盖候选过滤、保留时间、参数拒绝、控制器入口与非删除语义。

## A18-03 从健康副本修复

- 日期：2026-09-09
- 推荐决策：通过 `POST /api/storage/placements/{placementId}/actions/promote` 创建异步 Promotion Task，从健康活动副本复制并验证新的 `PROMOTED_COPY`；源副本不被覆盖。
- 原因：修复必须保留坏副本证据并维持 Blob 内容身份不变；新副本使用新的 Placement 身份和 `source_placement_id`，复制失败不会伪造成功状态。
- 失败语义：Placement/Blob/Provider 不存在、层级方向非法、Provider 不支持复制或源对象不可读均拒绝；同一幂等键复用已提交任务，不重复创建副作用。
- 验证：`mvn -pl storage -am -Dtest=PersistentStoragePlacementTieringServiceTest,StorageRestoreTaskHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test`；覆盖提交、幂等复用、目标不存在和恢复 Placement 激活，共 4 个测试全部通过。

## A18-02 发现损坏或缺失副本

- 日期：2026-09-09
- 推荐决策：由 Attachment Availability Query 根据 Blob 状态和 Placement/Provider 可读性发现异常；损坏 Blob 返回 CORRUPTED，无可读副本返回 MISSING，正在恢复或非活动副本返回 RESTORE_REQUIRED。
- 原因：发现结果必须保持 Attachment、Blob、Placement 的身份边界，并可直接被恢复与清理流程消费；查询前使用 `requireReadable` 执行对象级授权。
- 失败语义：目标不存在或无权访问不返回内部状态；没有活动且可读 Provider 的副本不伪报 READY，异常状态保持可查询且不自动删除活动引用。
- 验证：`mvn -pl storage -am -Dtest=DefaultAttachmentAvailabilityQueryTest,BlobVerificationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；覆盖 CORRUPTED、MISSING、完整性通过和损坏隔离，共 4 个测试全部通过。

## A18-01 校验副本完整性

- 日期：2026-09-09
- 推荐决策：通过 `POST /api/storage/blobs/{blobId}/actions/verify` 读取活动副本并校验 Blob 的 SHA-256 与大小；成功保留副本可用，失败将 Blob 标记为 CORRUPTED、Placement 标记为 UNAVAILABLE，并写入对应 durable event。
- 原因：完整性失败的副本不能继续作为内容读取来源；Attachment、Blob、Placement 身份仍分离，校验结果只更新 Owner 状态，不改变 Blob 内容身份。
- 失败语义：Blob 不存在/无权访问、无可读副本、Provider 不可用或无匹配 Reader 均拒绝；校验失败不产生 VERIFIED 状态，并阻止后续读取该副本。
- 验证：`mvn -pl storage -am -Dtest=BlobVerificationServiceTest,Sha256BlobIntegrityServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；成功匹配、损坏内容和 SHA-256 校验分支共 4 个测试全部通过。

## A18 Console 对接逐项审计

- A18-01/A18-02：附件列表和详情页展示真实 `availability`，并正确识别 `READY`、`RESTORE_REQUIRED`、`MISSING`、`CORRUPTED`。
- A18-03：存储层 Blob 副本诊断对不可用副本调用 `POST /storage/placements/{placementId}/actions/promote`，携带幂等键并以后台任务结果为准。
- A18-04：清理候选区调用 `GET /storage/gc/candidates`，只做预览；A18-05 的批准/拒绝调用 `POST /storage/gc/{blobId}/decision`，保留后端执行时的引用/保留期门禁。
- A18-06：物理清理任务按钮经显式确认调用 `POST /storage/gc/request`，成功后刷新，不伪造完成状态。
- 验证：`/storage-center/tiers`、`/storage-center/attachments` 和 `/storage-center/attachments/{attachmentId}` 页面均返回 HTTP 200；Console typecheck/build 已通过。

## A17 归档与恢复（父 issue）

- 日期：2026-09-09
- 验收结论：A17-01 至 A17-06 已按顺序完成并在 GitHub 关闭；覆盖归档可用性、幂等恢复申请、进度查询、失败重试、预算门禁及恢复后内容访问。
- 关键闭环：Restore Request 使用 Background Task 持久化状态；恢复成功分别更新 Operation、Placement 和 Blob；访问继续执行对象授权并仅读取可用副本；失败项可安全重试且不重复成功项。
- 验证证据：A17 子任务的 storage 单元/契约测试均通过；本轮新增/执行的关键测试包括恢复请求幂等、恢复进度契约、预算门禁、恢复后 Placement/Blob 激活，以及既有 Attachment 授权/预览测试。完整 PostgreSQL/Testcontainers 联调仍受当前环境 Docker 不可用限制，未伪造该证据。
- 主要 commits：`73398127`、`5d1604c0`、`27b4f866`、`ada71538`、`470f7b37`、`4ada2f1c`。

## A17-06 恢复完成后重新访问内容

- 日期：2026-09-09
- 推荐决策：恢复执行成功后由 Storage Restore Handler 同步激活原 Blob Placement，并将 Blob 标记为 AVAILABLE；永久恢复使用 ACTIVE，临时恢复使用 READY_TEMPORARILY，后续访问继续经过 Attachment Owner 授权和 Delivery Grant/Content Reader 路径。
- 原因：恢复操作状态完成并不等于内容访问状态完成；Placement、Blob 和 Restore Operation 必须分别更新，才能让既有 `readContent` 使用恢复后的副本，同时保持对象身份不变。
- 失败语义：Provider 返回不可读时不激活副本，保留 FAILED Operation/Request Item 错误；访问端无授权或无可读副本仍拒绝，不回退到物理路径直读。
- 验证：`mvn -pl storage -am -Dtest=StorageRestoreTaskHandlerTest,AttachmentControllerTest,AttachmentPreviewServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；验证成功恢复会把 Placement 置为 ACTIVE、Blob 置为 AVAILABLE，并通过现有授权/访问测试，4 个测试全部通过。

## A17-05 达到预算时阻止申请

- 日期：2026-09-09
- 推荐决策：恢复申请统一经过 Restore Budget Owner 的预算评估；超限按策略拒绝、要求显式确认或进入预算恢复后的队列，拒绝不创建 Restore Request。
- 原因：预算是恢复副作用的前置门禁，必须在任务提交前完成；现有实现同时覆盖单次、并发和每日请求字节预算，保留可追踪的 `budget_decision`。
- 失败语义：非法规模和超预算返回稳定冲突错误；目标不存在时先返回 NotFound，不执行预算检查、不保存请求、不提交任务；确认令牌仅在 REQUIRE_CONFIRMATION 策略下生效。
- 验证：`mvn -pl storage -am -Dtest=StorageRestoreBudgetServiceTest,StorageRestoreRequestServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；预算拒绝/确认/排队和目标不存在分支共 7 个测试全部通过。

## A17-04 恢复失败后重试

- 日期：2026-09-09
- 推荐决策：重试入口仅接受 FAILED/PARTIAL_FAILURE 请求，提交带 `retry_failed_only=true` 的新 Background Task；Task Handler 只处理失败的 Request Item，已成功项保持不变。
- 原因：失败恢复必须可重复执行且不破坏活动引用；通过新的任务幂等键区分重试尝试，同时保留原 Restore Request 身份和已完成计数。
- 失败语义：非失败状态拒绝重试；重复调用已提交的重试请求直接返回原状态和任务 ID，不重复提交任务或发布 retry 事件；失败项保留安全错误摘要供查询观察。
- 验证：`mvn -pl storage -am -Dtest=StorageRestoreRequestServiceTest,StorageRestoreContractControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`；4 个测试全部通过，覆盖恢复申请幂等、重试幂等、ACTIVE 进度和 PARTIAL 失败计数。

## A17-03 展示恢复进度

- 日期：2026-09-09
- 推荐决策：复用现有 `GET /api/restore-requests/{requestId}` 查询作为恢复进度入口，公开返回稳定状态、总项数、已就绪项数、失败项数、总字节数和预算决策；不暴露 Provider 内部信息。
- 原因：Restore Request 已由 Background Task Handler 持久化更新，查询端直接读取 Owner 状态即可观察从 PENDING/ACTIVE 到 PARTIAL/SUCCEEDED/FAILED 的进度，保持 Attachment、Blob、Placement 身份分离。
- 失败语义：失败状态按 `total_items - ready_items` 计算失败项数；请求不存在或非所属用户访问仍由 Application API 拒绝。错误摘要保留在 Owner 查询模型中，不通过公开契约泄露内部异常文本。
- 验证：`mvn -pl storage -am -Dtest=StorageRestoreRequestServiceTest,StorageRestoreContractControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`；覆盖已提交任务幂等返回、ACTIVE 进度和 PARTIAL 失败计数，3 个测试全部通过。

## A17-02 提交恢复申请

- 日期：2026-09-09
- 推荐决策：按 `Idempotency-Key` 返回已持久化的 Restore Request；已记录 `background_task_id` 的请求直接返回原状态，不再次提交 Background Task，也不重复发布 requested 事件。
- 原因：恢复申请可能因客户端重试或中断被重复执行，业务请求与后台任务必须保持一一对应；新请求仍沿用现有预算检查、授权检查和任务提交路径。
- 失败语义：缺少幂等键仍拒绝；新申请的授权、预算、Blob/副本校验失败仍原样失败；已提交请求重试不覆盖原状态和任务引用。
- 验证：`mvn -pl storage -am -Dtest=StorageRestoreRequestServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`；重复附件恢复申请回归测试通过，并验证没有重复任务、保存或事件副作用。

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

## A03-03 重复投递不重复执行副作用

- 日期：2026-09-10
- 验收结论：重复投递使用 Inbox 的 `(consumer_id, event_id)` 唯一写入结果判定；并发请求只有成功 claim 的一方执行 handler，重复方跳过副作用并继续完成投递标记。失败 handler 不提交 Inbox/完成标记，后续投递仍可重试。
- 追踪语义：Outbox 记录 `request_id`、`correlation_id`、`causation_id` 和 `actor_id`；消费者收到 `DurableEvent` view，不绕过公开 Integration API 暴露持久化实体。
- Console 对接：`console/src/views/integration/index.vue` 的“集成事件”页读取已登记的 `GET /health/operations`，展示待投递、已尝试未完成、最近尝试，并提供加载、错误和空结果状态；不在前端伪造事件执行结果。
- 验证：`OutboxRetrySemanticsTest` 新增两次并发 dispatch 仅执行一次 handler 的可重复测试；相关 DurableEvent、Dispatcher、Retry 测试共 11 项通过；Console `pnpm typecheck` 和 `pnpm build` 通过。真实 PostgreSQL 唯一约束并发回放仍需要 Docker Desktop + Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A03-04 查询并重试投递失败事件

- 日期：2026-09-09
- 推荐决策：在 Integration Owner 内提供已登记的 `GET /admin/integration/events` 和 `POST /admin/integration/events/{event_id}/actions/retry`；Controller 只调用 dispatcher capability，不暴露 Outbox Repository 或持久化实体。
- 原因：失败事件必须可观察、可再次触发；Console 页面需要真实查询和人工重试入口。重试仍使用同一 event ID、Inbox 唯一约束和 consumer 幂等语义，不重复产生业务 Event。
- 失败语义：不存在或已完成事件返回 `NotFoundException`；没有可用 consumer 时明确失败；consumer 失败保留 pending 状态，下一次自动调度或人工 retry 可继续执行。
- Console 对接：集成事件页读取诊断与 pending event 列表，展示 event ID、类型、生产者、发生时间，并在确认后调用 retry；成功后刷新列表，失败显示后端错误。
- 验证：`DurableEventDeliveryControllerTest`、`OutboxDispatcherTest`、API Registry/OpenAPI 收敛测试共 15 项通过；Console `pnpm typecheck` 已通过，生产构建待重启运行时后复核。真实 PostgreSQL 回放仍需要 Docker Desktop + Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A03 可靠事件投递（父 issue）

- 日期：2026-09-09
- 验收结论：A03-01 至 A03-04 的实现与本地验证记录已补齐；GitHub 评论/关闭状态需在认证恢复后逐项同步，不能由本地日志代替。父功能覆盖 Outbox 原子写入、重启重扫、Inbox 原子去重、失败事件查询与人工重试。
- 推荐决策：Integration Owner 暴露 durable event capability 与已登记的管理 HTTP 适配；A03-03 的诊断展示和 A03-04 的事件列表/重试均已接入 Console。事件 payload 保留 request/correlation/causation/actor 追踪字段。
- 验证证据：A03-03 事件回归测试 11 项、A03-04 相关控制器/dispatcher/契约测试 15 项通过；Console `pnpm typecheck`、`pnpm build` 通过；各子 issue 的独立测试与 commit 已记录在本日志，GitHub 同步因当前 API 401 暂缓。
- 剩余限制：当前开发环境未安装 Docker，真实 PostgreSQL 事务回滚、Outbox 重启回放、Inbox 唯一约束和跨 API 联调未执行；未将环境缺失伪造为通过。

## A04-01 提交任务并查询状态

- 日期：2026-09-09
- 推荐决策：补充 `POST /api/background-tasks` 作为持久化任务提交入口，使用 `Idempotency-Key`（可选时沿用现有 capability 语义），在 Service 完成持久化后返回 `202` 和 `Location`；查询继续使用已有单任务和分页列表能力。
- 原因：仅有内部 `submit` 方法不能形成可观察的独立操作路径；公开入口必须先登记 OpenAPI/HTTP Registry/Catalog，再接入 Controller。
- 失败语义：空请求或空 task type 在提交前拒绝；数据库持久化失败不返回 Accepted；相同 task type + idempotency key 重用已有任务，不创建第二个逻辑任务；后续查询对不存在任务返回 NotFound。
- Console 对接审计：`console/src/views/operations/Background.vue` 已真实调用 `GET /background-tasks`、`POST /background-tasks` 和单任务详情/attempts 查询；提交表单支持 payload 校验与 `Idempotency-Key`，并提供 loading、错误、空列表和刷新状态。
- 验证：`BackgroundTaskControllerTest` 覆盖 `202 + Location` 和空请求失败；既有 `BackgroundTaskDispatcherTest` 与 `BackgroundTaskLeaseValidationTest` 覆盖正常提交、查询、分页、幂等、失败与 Lease 失效边界；Console `pnpm typecheck`、`pnpm build` 通过。真实 PostgreSQL 约束和权限回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04-02 展示执行进度

- 日期：2026-09-09
- 推荐决策：沿用任务与 Attempt 分离模型，由持有有效 Lease 的 Worker 通过 `updateProgress` 写入 JSON progress，任务查询和控制台直接展示持久化 progress；不引入内存进度缓存或新的状态枚举。
- 原因：进度必须在重启、刷新和查询路径中可观察，且不能绕过 Task Owner 或让 progress 替代任务状态；控制台字段已对齐实际 `task_type`、`PENDING`、`SUCCEEDED` 和 `TIMED_OUT` 契约。
- 失败语义：无效/过期 Lease 不能更新进度；任务不存在返回 NotFound；刷新失败展示错误状态；失败、超时不显示为成功。
- Console 对接审计：同一 `Background.vue` 通过详情 API 重新读取持久化 `progress`，按 percent 或 completed/total 渲染进度条/摘要；加载失败显示错误，不把缺失进度推断为成功。
- 验证：`BackgroundTaskDispatcherTest` 新增 progress 持久化后通过 `get` 查询的断言；Operations 任务回归 14 tests passed；console `pnpm typecheck`、`pnpm lint`、`pnpm build` 均通过。lint 的自动格式化噪声已从无关文件恢复，仅保留本次 Background 页面变更。真实 PostgreSQL Lease 并发回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 外部权限记录：向 GitHub #920 发布完成评论和关闭 issue 的请求连续被安全策略拦截；本地实现与 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。

## A04-03 取消可取消任务

- 日期：2026-09-09
- 推荐决策：沿用现有 `cancel` + `acknowledgeCancellation` 两阶段语义：PENDING 任务立即进入 CANCELLED，RUNNING 任务只写入 `cancel_requested_at`，由 Worker 在安全检查点确认取消；SUCCEEDED/FAILED/TIMED_OUT 等终态拒绝取消。
- 原因：取消是 cooperative cancellation，不能强行终止正在执行的 Handler，也不能把取消伪装成失败；状态和 Attempt 历史保持可解释。
- 失败语义：不存在任务返回 NotFound；终态任务返回 Conflict；RUNNING 任务在确认前保持 RUNNING，其他任务不受影响。
- Console 对接审计：`Background.vue` 仅对 PENDING/RUNNING 显示取消操作，确认后调用 `POST /background-tasks/{task_id}/actions/cancel`，随后刷新列表/详情；终态任务不会显示取消按钮。
- 验证：`BackgroundTaskDispatcherTest` 覆盖 PENDING 立即取消、RUNNING 仅请求取消、终态保护，以及已有 Worker cooperative cancellation；Operations 任务回归测试覆盖成功与失败分支。真实 PostgreSQL 并发/权限回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 外部权限记录：向 GitHub #921 发布完成评论的请求被安全策略拦截；本地实现与 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。

## A04-04 重试失败任务

- 日期：2026-09-09
- 推荐决策：人工重试不把旧 Task 从 FAILED/TIMED_OUT 改回 PENDING，而是创建新的 PENDING child Task，并通过 `parent_task_id` 保留执行关系；公开 retry action 已登记到 OpenAPI、HTTP Registry 和 Catalog。
- 原因：旧任务和失败 Attempt 是不可覆盖的执行历史；新 Task 可重新走正常 claim/lease/handler/complete 链路，避免重复改写已提交结果。
- 失败语义：不存在任务返回 NotFound；非 FAILED/TIMED_OUT 任务返回 Conflict；子任务执行失败保留具体失败状态，旧任务仍保持 FAILED/TIMED_OUT。
- Console 对接审计：`Background.vue` 仅对 FAILED/TIMED_OUT 显示重试按钮，调用已登记的 `POST /background-tasks/{task_id}/actions/retry`，成功后刷新列表；页面不覆盖旧任务状态。
- 验证：`BackgroundTaskDispatcherTest` 覆盖旧任务保持失败、创建 child、child 成功恢复，以及既有失败和 retry 分支。真实 PostgreSQL 事务、权限和跨 API 回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04-05 Worker 中断后重新领取任务

- 日期：2026-09-09
- 推荐决策：继续使用持久化 Lease + Attempt 模型；Worker 中断后由下一次 claim 发现过期 Lease，将旧 Attempt 终结为 `LEASE_LOST`，再把同一逻辑 Task 重新置为 PENDING 并创建新的 Attempt，不创建重复逻辑 Task。
- 原因：Lease 是执行权而非任务身份，旧 Attempt 必须保留以支持诊断和恢复；数据库 claim 的 `FOR UPDATE SKIP LOCKED` 负责并发领取边界。
- 失败语义：Lease 失效不将任务伪装为成功；旧 Attempt 历史不可覆盖；取消请求在 Lease 回收时进入 CANCELLED 路径。
- Console 对接审计：`Background.vue` 的任务详情并行读取任务和 `/background-tasks/{task_id}/attempts`，展示每次 Attempt 的编号、Runner、Lease 到期、开始/结束时间和错误；Lease 丢失不会被页面折叠成成功。
- 验证：`BackgroundTaskDispatcherTest` 强化中断恢复断言，验证重新领取后 attempt=2、旧 Attempt 为 `LEASE_LOST` 且历史包含两次 Attempt；既有 dispatcher/lease 测试继续覆盖成功与失败。真实 PostgreSQL 并发 claim 回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04-06 查看执行历史

- 日期：2026-09-09
- 推荐决策：通过已有 `GET /api/background-tasks/{task_id}/attempts` 查询由 Operations Owner 持有的 Attempt 历史，按 attempt number 稳定升序返回；不把历史压缩回 Task 当前状态，也不覆盖旧失败记录。
- 原因：执行历史需要区分每次 claim、Lease 失效、失败和完成，支持恢复诊断；Task 当前状态只表达逻辑任务，不足以替代 Attempt 事实。
- 失败语义：不存在任务返回 NotFound；空历史返回空结果；失败/Lease Lost 状态按事实返回，不显示为成功；查询只读，不改变任务状态。
- Console 对接审计：任务详情抽屉直接展示 `/background-tasks/{task_id}/attempts` 的完整历史，包含失败和 `LEASE_LOST` 记录；空历史显示明确空状态，查询失败显示错误。
- 验证：`BackgroundTaskControllerTest` 覆盖 Attempts 查询入口和 `LEASE_LOST` 历史事实；`BackgroundTaskDispatcherTest` 覆盖两次 Attempt 的恢复历史；Operations 任务回归测试覆盖成功与失败分支。真实 PostgreSQL 分页/权限回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A04 后台任务管理（父 issue）

- 日期：2026-09-09
- 本地验收结论：A04-01 至 A04-06 已完成实现与自动化验收，覆盖提交/查询、进度、取消、人工重试、Lease 恢复和 Attempt 历史；主要 commits 为 `e09787f6`、`022bee7b`、`258b926d`、`928b4ea0`、`d86d5dfe`、`4243f26f`。
- 推荐决策：保持 Task 与 Attempt 分离；任务提交先持久化后返回 202；Worker 使用 Lease/`SKIP LOCKED` 领取，过期后保留 `LEASE_LOST` Attempt 并重试；人工 retry 创建 child Task；取消采用 cooperative cancellation。
- 验证证据：Operations 任务回归测试 17 项全部通过；console `pnpm typecheck`、`pnpm lint`、`pnpm build` 已通过；公开提交与 retry action 已登记 OpenAPI、HTTP Registry 和 Catalog。Console 对接审计已覆盖 A04-01 至 A04-06 的真实查询、提交、进度、取消、重试和 Attempt 历史路径。
- 剩余限制：Docker 未安装，真实 PostgreSQL 约束、事务、并发 claim、权限和跨 API 联调未执行；GitHub issue 评论/关闭受 gh token 无效（401）阻塞，待 `gh auth login -h github.com` 后补发。

## A05-01 初始化管理员

- 日期：2026-09-09
- 推荐决策：沿用首次注册作为初始化路径；首个用户在同一 reactive transaction 中写入平台用户、PBKDF2-SHA256 密码凭据并幂等分配 `admin` 初始角色，不创建 SecuritySession、Token Digest 或持久化令牌。
- 原因：当前 V2 无独立登录 Session 模型；首个用户是唯一明确的管理员初始化边界，后续登录/刷新由 A05-02/A05-03 负责。
- 失败语义：非法初始化输入在持久化前拒绝；用户名/邮箱冲突返回 Conflict；凭据或角色绑定失败由事务回滚，不返回伪成功；重复角色绑定保持幂等。
- Console 对接审计：`console/src/views/setup/index.vue` 先调用 `GET /health/ready` 检查服务就绪，再通过 `registerUser` 调用 `POST /auth/register`；表单校验用户名/显示名/密码确认，成功跳转 `/login`，不展示或保存密码明文。
- 验证：`AuthenticationServiceTest` 覆盖首用户 admin 角色、PBKDF2 哈希、事务入口和非法输入；真实 PostgreSQL 回滚、唯一约束和初始化重放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 外部权限记录：向 GitHub #926 发布完成评论的请求被安全策略拦截；本地实现与 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。

## A05-02 用户登录与客户端退出

- 日期：2026-09-09
- 推荐决策：登录仅校验用户名、PBKDF2-SHA256 密码和用户 ACTIVE 状态后签发无状态 access/refresh JWT；不创建或返回 `session_id`，客户端登出只清理本地 token、用户状态和路由。
- 原因：P0 认证基线采用无状态 JWT，普通登出不能伪造后端会话撤销能力；服务端 `/auth/logout` 继续作为兼容端点，但不持久化会话状态。
- 失败语义：未知用户、错误密码和停用用户统一返回稳定的认证失败错误，不泄露账号存在性；无效输入不触发 token 签发。
- Console 对接审计：登录页通过 Pinia `loginByUsername` 调用 `POST /auth/login`，成功保存后端返回的 access/refresh token 并初始化路由；用户登出先调用 `POST /auth/logout`，无论请求成功或失败都清理本地 token、权限、标签和路由。
- 验证：`AuthenticationServiceTest` 覆盖登录 token pair、无 `sessionId` 和空操作登出；认证回归测试共 13 项通过；Console `pnpm typecheck` 与 `pnpm build` 通过。
- 外部权限记录：向 GitHub #927 发布完成评论的请求尚未获安全策略授权；本地实现与后续 commit 已保留，待权限恢复后补发评论并关闭。根据执行规则继续处理后续子 issue。
- 本地权限记录：提交时 Git 无法创建 `.git/index.lock`，已记录并申请受控权限重试；不影响代码验证，继续按 issue 顺序推进。
- 外部权限记录：向 GitHub #927 发布完成评论并关闭 issue 的请求因安全权限审批超时未执行；不能视为已评论或已关闭，待权限恢复后补发。根据执行规则继续处理 A05-03。

## A05-03 Access / Refresh JWT 签发与刷新

- 日期：2026-09-09
- 推荐决策：复用集中式 `JwtTokenService` 签发带 `sub`、`jti`、`security_version`、`iat`、`exp`、issuer 和 token kind 的 Access/Refresh JWT；刷新只接受签名有效的 Refresh JWT，并重新读取 ACTIVE 用户与当前 `security_version`。
- 原因：保持无状态认证，不引入 Login Session、Refresh Token Digest 或 Controller 私自拼装 token；用户级失效由后续 A05-05 提升 `security_version`。
- 失败语义：过期、错误类型、错误签名和版本不匹配均拒绝继续认证/刷新；JWT 原文不写入数据库、事件、审计或普通日志。
- Console 对接审计：`console/src/utils/http/index.ts` 在受保护请求收到 401 时调用 `useUserStoreHook().handRefreshToken` → `POST /auth/refresh-token`，更新 access/refresh token 后重放原请求；刷新失败则清理认证状态并回到登录页，白名单避免刷新接口递归。
- 验证：`JwtTokenServiceTest` 覆盖 Access/Refresh 类型隔离、唯一 jti、过期和篡改拒绝；`AuthenticationServiceTest` 覆盖刷新时 `security_version` 不匹配及无效 token 拒绝；认证回归共 15 项通过；Console `pnpm typecheck`、`pnpm build` 通过。

## A05-04 无状态 JWT 校验与 security_version

- 日期：2026-09-09
- 推荐决策：由最高优先级 WebFilter 统一解析 Bearer Access JWT，校验签名、时间和 token kind 后读取主体用户，仅 ACTIVE 且 `security_version` 一致时构造 `AuthenticatedPrincipal`；不查询 Session。
- 原因：请求认证必须以当前用户状态和安全版本为权威，避免 Session、`sid` 或 Token Digest 重新成为隐式登录态。
- 失败语义：缺失/格式错误 Bearer、错误签名、过期、Refresh 类型、禁用用户、未知用户和版本过期均返回 401，且不把 token 原文写入日志或错误响应。
- Console 对接审计：登录后的 Console 通过受保护的 `/me`、后台任务和管理页面验证 Access JWT；`console/src/views/security/Users.vue` 在用户详情展示服务端 `security_version`，不在前端解析或伪造 JWT 校验结果。
- 验证：`JwtAuthenticationWebFilterTest` 覆盖合法 Access 身份注入、非法 token、错误类型、过期、禁用用户和 stale `security_version`；相关认证回归共 10 项通过；Console `pnpm typecheck`、`pnpm build` 通过。

## A05-05 用户级旧 Token 全量失效

- 日期：2026-09-09
- 推荐决策：新增 `POST /api/me/actions/invalidate-tokens` 自助入口和 `POST /api/users/{userId}/actions/invalidate-tokens` 管理入口；目标用户 `security_version` 原子递增，随后在同一 reactive transaction 内追加 `authentication.user.tokens-invalidated` durable event 与审计记录。
- 原因：用户级 Token 失效是安全纪元变化，不是 Session 撤销；旧 Access/Refresh JWT 会在 A05-03/A05-04 的版本校验中自然失效，不保存 token、digest、设备或 Session 状态。
- 权限选择：自助入口要求已认证主体且目标固定为自身；管理员入口沿用 `system.user.manage` 的用户管理权限，拒绝逻辑由统一授权过滤器执行。
- 失败语义：未知用户返回 NotFound；版本递增失败、事件或审计失败不返回伪成功；事件 payload 仅包含 user_id 与新 security_version。
- Console 对接审计：`console/src/views/security/Users.vue` 在用户详情展示 `security_version`，管理员确认后调用 `POST /admin/users/{user_id}/actions/invalidate-tokens`，成功后更新页面版本，失败显示错误，不暴露 Token 内容。
- 验证：`DefaultUserServiceTest` 覆盖版本递增、事务入口、durable event payload 和审计；认证/授权回归共 26 项通过。真实 PostgreSQL 乐观并发、事务回滚和权限联调仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A05 账号与 JWT 认证（父 issue）

- 日期：2026-09-09
- 本地验收结论：A05-01 至 A05-05 已完成；覆盖首次管理员初始化、无状态登录/客户端退出、Access/Refresh JWT、请求侧 security_version 校验和用户级 Token 全量失效。
- 主要 commits：`1c156217`、`ab1e239b`、`4f557967`、`a60c4998`、`5ca5d087`。
- 统一决策：不建立 Login/Security Session 或 Token Digest；普通登出由客户端清理本地凭证，紧急全量失效通过递增用户 `security_version`，并产生最小化 durable event 与审计。
- 验证证据：认证/授权相关回归通过；console `pnpm typecheck` 与 `pnpm build` 通过。Console 对接审计已覆盖初始化、登录退出、刷新、受保护请求校验和管理员 Token 失效操作。真实 PostgreSQL/Testcontainers 并发、事务回滚和完整权限联调仍受当前环境 Docker 未安装限制，未伪造运行证据。

## A06-01 创建角色并配置权限

- 日期：2026-09-09
- 推荐决策：复用 `RoleController`/`DefaultRoleService` 的公开创建与权限配置路径；角色权限只能接受 `PlatformPermission` 注册枚举，拒绝任意未声明权限，角色创建输入在 Application 层再次校验。
- 失败语义：非法角色资料在持久化前拒绝；重复角色编码返回 Conflict；不存在角色返回 NotFound；权限变更写入审计并发布 durable authorization event，不包含认证材料。
- Console 对接审计：`console/src/views/security/Permissions.vue` 真实读取 `/admin/roles` 与 `/admin/permissions`，创建角色调用 `POST /admin/roles`，保存已选择的注册权限调用 `PUT /admin/roles/{role_id}/permissions`；权限分组来自后端注册表，页面提供加载、错误、空状态和保存状态。
- 契约修复：补登记角色创建/替换权限两个操作，并将 Role/Permission 响应模型对齐实际 `code/name/permissions` API 表示。
- 验证：`DefaultRoleServiceTest` 覆盖合法角色创建、非法输入、已声明权限授予、权限列表和边界；授权过滤器回归覆盖直接 API 的认证/权限拒绝路径；本轮授权回归 20 项通过；API Registry/OpenAPI/路由约定测试 9 项通过；Console `pnpm typecheck`、`pnpm build` 通过。

## A06-02 分配和撤销用户角色

- 日期：2026-09-09
- 推荐决策：通过 `RoleController` 暴露用户-角色绑定与撤销动作；绑定前确认角色存在，重复绑定保持幂等，撤销只删除指定 user/role 绑定，不影响其他用户或角色；两类动作均写审计。
- 失败语义：角色不存在返回 NotFound；重复分配不创建重复绑定；撤销目标不存在保持幂等完成；权限入口由统一 `system.role.manage` 授权过滤器保护。
- Console 对接审计：`console/src/views/security/Users.vue` 真实读取用户与角色列表，并在用户详情通过角色选择器调用 `POST /admin/roles/users/{user_id}/roles/{role_id}` 分配角色、调用 `POST /admin/roles/users/{user_id}/roles/{role_id}/actions/revoke` 撤销角色；成功后刷新用户角色，失败显示可见错误。HTTP 拦截器从当前 JWT 解析并发送 `X-Ikaros-Actor-Id`，页面不是静态占位。
- 契约修复：补登记用户角色分配/撤销两个操作，并将 OpenAPI 路径、UUID 参数、Actor 请求头和 204/401/403/404 响应对齐 Controller。
- 验证：`DefaultRoleServiceTest` 覆盖分配、撤销、重复/指定目标边界；授权回归 21 项通过；API Registry/OpenAPI/路由约定测试 9 项通过；Console `pnpm typecheck`、`pnpm build` 通过。真实 PostgreSQL 唯一约束和并发绑定回放仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A09-01 创建资源（A06-03 前置 issue）

- 日期：2026-09-09
- 推荐决策：复用 Resource Owner 的创建路径，以 `owner_id` 固定资源归属，同时在同一事务创建首个主标题；创建结果支持 `Idempotency-Key` replay/conflict，避免重试产生重复资源。
- 失败语义：非法类型/标题/locale 在 Application 层拒绝；幂等 key 同请求返回原资源，不同请求返回 Conflict；资源、标题、事件、审计或幂等记录任一失败不返回伪成功。
- 验证：`DefaultResourceServiceTest` 覆盖创建及首标题、非法输入、幂等重放/冲突和审计事件；资源回归 13 项通过。真实 PostgreSQL 约束、事务回滚和并发幂等仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。
- 追加验证：补充跨 owner 读取拒绝测试，资源服务回归 `DefaultResourceServiceTest` 8 项通过。
- Console 复核（2026-09-10）：`/resource-center/library` 已挂载真实资源页；加载调用 `GET /resources`，创建弹窗调用 `POST /resources` 并发送 `Idempotency-Key`，成功后刷新列表并进入详情，加载/创建失败均有可见错误状态。Console `pnpm typecheck`、`pnpm build` 通过；运行中的 Console 路由返回 HTTP 200。未将静态页面或仅有后端接口作为完成证据。

## A06-03 隔离用户私有资源

- 日期：2026-09-09
- 推荐决策：Resource Owner 是唯一资源隔离边界；所有单资源读取/变更通过 `findByIdAndOwnerId`，列表与计数 SQL 同时限定 `owner_id`，未授权对象统一表现为 NotFound，不向调用方泄露存在性。
- 原因：资源模块已实现 Resource-centric 私有模型，A06-03 只补充授权验收，不引入跨模块 Repository 或额外 `tenant_id`。
- 失败语义：不同用户读取、修改或通过外部身份查找私有资源均不得获得目标数据；失败不触发标题、Blob 或审计写入。
- Console 对接审计：`console/src/views/resources/index.vue` 的资源库页面调用 `GET /resources`，创建资源调用 `POST /resources` 并发送 `Idempotency-Key`；`Detail.vue` 的详情、标题、元数据、标签、关系、收藏和用户状态操作均使用资源 ID API，统一 HTTP 拦截器注入当前 `X-Ikaros-Actor-Id`。列表、详情、创建失败和空状态均有可见反馈，页面不是静态占位。
- 验证：`DefaultResourceServiceTest` 覆盖跨 owner 读取拒绝、创建 owner 固定和资源幂等；资源回归通过；Console `pnpm typecheck`、`pnpm build` 通过；运行中的 `/resource-center/library` 返回 HTTP 200。真实 PostgreSQL SQL 隔离和 API 联调仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A06-04 校验资源变更权限

- 日期：2026-09-09
- 推荐决策：资源变更统一经过 `ResourceAuthorizationWebFilter`，按 HTTP 方法和路径要求 `resource.write`/`resource.delete` 等注册权限；业务服务继续执行 owner 与领域不变量校验，HTTP 门禁不替代领域授权。
- 失败语义：无认证主体返回 401；有 Access JWT 但缺少资源变更权限返回 403；拒绝路径不进入 Controller/Repository，不泄露资源数据。
- Console 对接审计：`console/src/views/resources/Detail.vue` 的编辑、标题/元数据覆盖、外部身份、关系、标签、收藏、用户状态、归档和回收站按钮均调用资源 API；统一 HTTP 拦截器携带 JWT 对应 Actor，服务端 `ResourceAuthorizationWebFilter` 按方法和路径执行 `resource.write`/`resource.delete` 门禁，页面对 401/403/409 显示可见错误并要求高风险操作确认。
- 验证：`ResourceAuthorizationWebFilterTest` 覆盖资源写操作“仅读权限拒绝/写权限通过”，授权回归 16 项通过；资源服务跨 owner 隔离测试已通过；Console `pnpm typecheck`、`pnpm build` 通过；运行中的资源详情路由可访问，未认证资源变更 API 返回 401。

## A06-05 权限撤销后阻止后续访问

- 日期：2026-09-09
- 推荐决策：资源路径继续先检查 JWT 权限快照，再通过 `AccessControlService` 实时读取当前 user-role/role-permission 绑定；权限撤销后，即使旧 JWT 尚未过期，后续资源请求也返回 403。
- 原因：JWT 权限快照用于快速拒绝，但不能作为最终 ACL；实时 capability 复核满足撤销即时生效，同时不引入 Session 或 Token Digest。
- 失败语义：当前 RBAC capability 拒绝时不调用下游 Controller；错误统一映射为 403，不泄露目标资源或 token 信息。
- Console 对接审计：`console/src/views/security/Permissions.vue` 保存角色权限调用 `PUT /admin/roles/{role_id}/permissions`，`Users.vue` 提供撤销用户角色和让全部旧 Token 失效操作；两页都在成功后刷新状态、失败显示错误。资源详情页继续通过统一 API 入口，权限撤销后的下一次请求由服务端实时复核并返回 403，而非仅依赖前端 JWT 快照。
- 验证：`ResourceAuthorizationWebFilterTest` 覆盖角色撤销后实时 capability 拒绝，当前授权过滤器回归 17 项通过；资源 owner 隔离回归已通过；Console `pnpm typecheck`、`pnpm build` 通过。

## A06 角色与授权（父 issue）

- 日期：2026-09-09
- 本地验收结论：A06-01 至 A06-05 已完成，覆盖角色创建/权限配置、用户角色分配/撤销、私有资源 owner 隔离、资源变更权限和撤销后的实时访问阻断。
- Console 对接总审计：身份中心的角色/权限页和用户管理页已真实接入角色配置、用户角色分配/撤销及旧 Token 失效 API；资源中心资源库/详情页已真实接入资源查询、创建、编辑、状态与生命周期操作。页面加载、空状态、失败和高风险确认均有可见交互，未把静态页面作为完成证据。
- 主要 commits：`1b793395`、`7fb67818`、`af4a7cb8`、`49927c25`、`ef1a5f59`；A09-01 前置 commits：`645954b1`、`43441b91`。
- 统一决策：权限注册枚举和统一授权过滤器负责入口门禁，领域服务负责 owner/状态不变量；资源访问在 JWT 快速检查后实时复核当前 RBAC，避免权限撤销等待 token 过期。
- 验证证据：授权回归通过，资源服务 owner 隔离回归通过；真实 PostgreSQL 唯一约束、并发绑定、事务回滚和完整 API 联调仍需要 Docker Desktop/Testcontainers，当前环境未安装 Docker，未伪造运行证据。

## A07-01 发起二次验证

- 日期：2026-09-09
- 推荐决策：复用 Email OTP Provider 与公开 `POST /api/security/verification-challenges` 入口；发起前只接受 ACTIVE 且配置可验证邮箱的用户，挑战绑定 purpose/target_reference 并设置短时有效期。
- 安全边界：数据库只保存 OTP 慢哈希摘要；响应、审计和普通日志不返回 OTP、摘要或目标邮箱；发起频率按用户窗口限制，直接 API 与错误身份均走同一 Application Provider。
- 失败语义：未知用户、非 ACTIVE 用户或无邮箱用户返回 NotFound；频率超限返回 Conflict；失败前不创建挑战。
- Console 对接审计：`console/src/views/security/Authentication.vue` 的“发起验证”按用途调用 `/security/step-up` 或 `/security/verification-challenges`，随后调用对应 verify/cancel API；页面只展示 Challenge ID、状态和过期时间，不展示 OTP，并对发起、验证、过期/锁定和取消结果提供可见反馈。
- 验证：`EmailOtpVerificationProviderTest` 覆盖合法发起、未知/无邮箱身份拒绝、频率限制和摘要持久化；`VerificationControllerTest` 覆盖公开入口、202 响应及 OTP 字段不泄露；当前回归共 12 项通过；Console `pnpm typecheck`、`pnpm build` 通过。真实 PostgreSQL 事务和邮件渠道联调留待 A07-05/集成环境验证。

## A07-05 接入真实验证码发送渠道

- 日期：2026-09-09
- 推荐决策：新增可配置 HTTP 邮件网关 Adapter；仅当 `IKAROS_SECURITY_EMAIL_DELIVERY=HTTP` 且 endpoint/from 完整配置时启用，默认保留无外发的 Noop 实现，避免未配置环境误发或启动失败。
- 安全边界：网关 API Key 只从环境 Secret 注入 Authorization Header；OTP 仅存在当前投递调用体，错误只返回固定非敏感信息，不记录网关响应正文或验证码。
- 失败语义：用户邮箱不可用或网关返回错误均失败，不产生伪成功；发送仍由 Email OTP Provider 的用途绑定、过期和频率限制控制。
- Console 对接审计：由于 endpoint/from/API Key 属于部署 Secret 边界，Console 不提供可泄露或覆盖环境 Secret 的编辑器；`console/src/views/security/Authentication.vue` 的“发起验证”是真实邮件投递链路入口，按用途调用 challenge API，页面只展示状态/过期时间并对投递失败显示错误，不显示 OTP、API Key 或网关响应。
- 验证：`HttpEmailOtpDeliveryTest` 覆盖 HTTP 成功、Authorization Secret 传递、网关错误脱敏；连同 A07-01 回归当前 14 项通过；Console `pnpm typecheck`、`pnpm build` 通过。真实第三方邮件网关联调需要部署环境提供 endpoint/from/API Key，当前未发送外部邮件。

## A07-02 验证成功后执行限定操作

- 日期：2026-09-09
- 推荐决策：Step-up 成功后只签发短期、用途绑定的 Verification Grant；不修改 Session、权限或密钥。Grant 携带主体、`security_version`、purpose、SVL、签发/过期时间，由后续高风险 Command 与 Permission/Security Policy 一并校验。
- 失败语义：挑战用途/目标不匹配或用户非 ACTIVE 时拒绝；用户状态检查必须先于 OTP 消费，拒绝路径不得执行验证 Provider 或签发 Grant。
- Console 对接审计：`console/src/views/security/Authentication.vue` 验证成功后把短期 Grant 放入内存请求上下文；后续 Console 请求由 HTTP 拦截器自动携带 `X-Ikaros-Verification-Grant`，退出登录或取消挑战时清除。后端认证过滤器校验 Grant 的主体和 `security_version`，授权过滤器将等级/有效期传给 `AccessControlService`，高风险请求无 fresh grant 时返回 403。
- 验证：`DefaultStepUpVerificationServiceTest` 覆盖 Grant claim、主体/用途/SVL/有效期、目标绑定和停用用户拒绝；`JwtAuthenticationWebFilterTest` 覆盖 Grant 上下文注入；`ResourceAuthorizationWebFilterTest` 覆盖高风险操作无验证拒绝且不触发下游；相关回归共 29 项通过；Console `pnpm typecheck`、`pnpm build` 通过，运行中的服务 readiness 为 200。

## A07-03 验证过期后拒绝执行

- 日期：2026-09-09
- 推荐决策：挑战过期时立即转为 EXPIRED 并拒绝；已 VERIFIED/LOCKED 等终态不可再次验证。Verification Grant 由 JWT `exp` 约束，后续 Security Policy 还必须检查 verification expiry，不以 Access JWT 有效替代 Step-up 有效。
- 失败语义：过期挑战返回 Conflict，拒绝 OTP 匹配与 Grant 签发；已消费挑战不可重放；过期验证保证不执行限定操作。
- Console 对接审计：`console/src/views/security/Authentication.vue` 在验证失败后根据服务端错误把挑战标记为 `EXPIRED`，禁用继续验证并保留可见错误；验证成功后 Grant 由服务端 `exp` 控制，客户端不自行延长有效期。
- 验证：`EmailOtpVerificationProviderTest` 覆盖过期挑战状态转换与已消费挑战重放拒绝；`DefaultAccessControlServiceTest` 覆盖过期 SVL 拒绝；本轮认证/授权相关回归共 14 项通过；Console `pnpm typecheck`、`pnpm build` 通过。

## A07-04 限制验证失败重试

- 日期：2026-09-09
- 推荐决策：每次错误 OTP 都持久化递增 attempt_count；达到 5 次将挑战置为 LOCKED，后续请求由终态门禁拒绝；发起挑战另按用户 10 分钟窗口最多 3 次限制。
- 失败语义：中间失败只产生一次失败审计与状态更新，不执行任何业务副作用；锁定后不再匹配 OTP、不重置计数、不允许重放。
- Console 对接审计：`console/src/views/security/Authentication.vue` 只允许 6 位验证码输入，验证按钮在 `LOCKED`/`EXPIRED` 状态禁用；服务端错误会显示在页面并同步锁定状态，页面没有绕过重试次数或重新启用终态挑战的逻辑。
- 验证：`EmailOtpVerificationProviderTest` 覆盖中间失败计数、最终锁定、锁定终态和发起频率限制；本轮 11 项通过；Console `pnpm typecheck`、`pnpm build` 通过。

## A07 敏感操作验证（父 issue）

- 日期：2026-09-09
- 本地验收结论：A07-01、A07-05、A07-02、A07-03、A07-04 已按依赖顺序完成；覆盖二次验证发起、真实邮件渠道、成功后短期 Grant、过期拒绝和失败重试限制。
- Console 对接总审计：认证/安全页真实发起、验证和取消 challenge；验证成功后的短期 Grant 保存在内存请求上下文并自动随后续请求发送，退出登录时清除；过期/锁定状态和错误在页面可见，Secret、OTP 和 Grant 明文不持久化。
- 主要 commits：`7a78a788`、`49f168cf`、`8e4036f1`、`9acb627d`、`67a076d0`。
- 统一决策：不建立 Session 或保存 OTP/Grant 明文；HTTP 邮件渠道显式配置启用；Grant 必须同时满足主体、用途、SVL、有效期和后续权限策略；状态校验先于 OTP 消费。
- 验证证据：认证验证相关回归通过（本轮最大组合 14 项，邮件渠道 11 项）；真实第三方邮件与 PostgreSQL/Testcontainers 联调仍需部署环境，未伪造运行证据。

## A07 Console 取消挑战对接修复
- 发现并修复 `LOGIN_STEP_UP` 取消动作错误调用通用 verification-challenges 地址导致 404 的问题。
- Step-up 新增 `DELETE /security/step-up/{challengeId}`，服务端复用绑定校验和 Email OTP Provider 取消逻辑；Console 按挑战用途选择对应取消 API。
- 验证：Step-up 服务/控制器测试 7/7；Console `pnpm typecheck` 通过；主要提交：`773f0036`、`8b93e50e`。

## A08-01 记录资源管理操作

- 日期：2026-09-09
- 推荐决策：资源管理 Application 服务统一通过 `AuditService` 记录 create/update/trash/archive/restore 及标题、标签、收藏、关系等操作；审计事件独立存储于 `audit_event`，不复用 Resource Activity。
- 失败语义：审计写入失败沿调用链传播，不返回伪成功；事件保留 actor、action、target、occurred_at、request/correlation context，details 仅允许脱敏 JSON。
- Console 对接审计：`console/src/views/communications/Audit.vue` 通过 `GET /audit-events` 展示操作者、动作、目标和时间，并通过 `GET /audit-events/{event_id}` 查看资源操作详情；页面提供时间/操作者/请求过滤、加载/空状态/错误反馈，资源操作不会只写到不可见日志。
- 验证：新增 `DefaultAuditServiceTest` 覆盖用户资源更新、系统归档、目标关联和独立审计落库契约；现有资源服务测试覆盖各资源管理入口的 `AuditService` 调用；本轮审计模块 3 项通过；Console `pnpm typecheck`、`pnpm build` 通过。

## A08-02 记录权限与凭据操作

- 日期：2026-09-09
- 推荐决策：权限与凭据变更继续统一调用 `AuditService`：角色权限授予/替换、用户角色分配/撤销、Token 全量失效、验证挑战发起/成功/失败/取消均写入独立 `audit_event`；不保存 JWT、OTP、Grant 或 Secret 明文。
- 失败语义：审计写入失败沿业务调用链传播；权限/凭据拒绝不产生成功审计或伪成功状态，审计详情保持最小化脱敏 JSON。
- Console 对接审计：`console/src/views/security/Permissions.vue` 真实保存角色权限，`Users.vue` 真实分配/撤销角色并支持旧 Token 失效，`Authentication.vue` 真实发起/验证/取消 challenge；三类操作成功后刷新或更新页面状态，失败显示错误，页面不展示 JWT/OTP/Grant/Secret。
- 验证：`DefaultRoleServiceTest` 6 项、`DefaultUserServiceTest` 7 项、`EmailOtpVerificationProviderTest` 11 项通过，覆盖权限与凭据操作的正常及失败路径；相关回归共 24 项通过；Console `/identity-center/roles`、`/identity-center/users`、`/identity-center/authentication` 均返回 HTTP 200。

## A08-03 按操作者和时间查询

- 日期：2026-09-09
- 推荐决策：新增 `GET /api/audit-events` 查询路径，支持 `actor_id`、RFC 3339 `from/to` 和稳定 page/size 分页；SQL 使用 `occurred_at desc, id desc`，避免同时间事件分页重复/遗漏。
- 权限边界：统一授权过滤器将查询映射到 `system.audit.read`，并实时复核当前 RBAC；审计查询不允许无认证或无权限直接进入 Controller。
- 失败语义：非法时间范围、页码或页大小在 Application 层拒绝；空结果返回空页，不伪造成功事件或泄露目标数据。
- Console 对接审计：`console/src/views/communications/Audit.vue` 将操作者 ID、RFC 3339 起止时间和请求 ID作为查询参数发送到 `/audit-events`，支持刷新、分页结果、空结果和错误提示；页面通过后端返回结果过滤，不在前端伪造审计数据。
- 验证：`AuditQueryServiceTest` 覆盖操作者/时间过滤、offset 分页、稳定查询参数和非法范围；`ResourceAuthorizationWebFilterTest` 覆盖审计读取权限与实时角色校验；相关回归共 19 项通过；Console 审计页运行返回 HTTP 200。

## A08-04 查看操作结果与关联对象

- 日期：2026-09-09
- 推荐决策：在审计查询路径增加 `GET /api/audit-events/{event_id}`，返回审计事件的 actor、action、target、结果详情及 request/correlation 关联信息；查询仍受 `system.audit.read` 统一权限门禁保护。
- 失败语义：不存在的审计事件返回稳定 NotFound；审计记录与 Resource Activity 保持独立，分页查询和单条查询均不伪造成功状态。
- Console 对接审计：`console/src/views/communications/Audit.vue` 点击审计行调用 `/audit-events/{event_id}`，在抽屉展示 Event、Actor、目标、时间、Request/Correlation ID 和结果详情；详情加载失败会回到页面错误提示。
- 验证：`AuditQueryServiceTest` 覆盖单条事件读取与目标不存在；`ResourceAuthorizationWebFilterTest` 覆盖审计查询权限和实时 RBAC；相关回归共 18 项通过；Console 审计详情路由返回 HTTP 200。敏感字段进一步脱敏由 A08-05 处理。

## A08-05 隐藏敏感字段

- 日期：2026-09-09
- 推荐决策：在 `DefaultAuditService` 的统一写入边界集中脱敏 JSON 中的 password、token、otp、secret、credential、authorization、api_key、private_key、code 等敏感字段；调用方即使误传明文也不会直接落库。
- 安全边界：审计仍不保存 JWT、OTP、Verification Grant、密码或 Secret 明文；固定替换为 `[REDACTED]`，保留非敏感结果字段和审计主体/目标关联。
- 失败语义：空详情归一为 `{}`；审计事件仍独立落库，脱敏不改变审计成功/失败语义。
- Console 对接审计：审计页明确标注数据来自服务端“按权限过滤并脱敏”的结果，详情抽屉只显示后端返回的 `details`，不在浏览器端接触或拼接 JWT、OTP、Grant、Password、Secret 等明文。
- 验证：`DefaultAuditServiceTest` 覆盖用户/系统事件和敏感 token/password 值落库前脱敏；operations 模块 3 项通过；Console 审计列表/详情路由返回 HTTP 200。

## A08 操作审计（父 issue）

- 日期：2026-09-09
- 本地验收结论：A08-01 至 A08-05 已按顺序完成，覆盖资源管理审计、权限/凭据审计、按操作者/时间查询、单条关联对象查看和敏感字段脱敏。
- Console 对接总审计：`/communications-center/audit` 已真实接入审计列表和单条详情查询，支持操作者/时间/请求筛选、关联对象查看、错误/空状态；服务端脱敏后的详情才进入页面，认证材料不在 Console 展示。
- 主要 commits：`2fc5c986`、`40cfa377`、`86b6448c`、`3511cd42`、`0f50041d`。
- 统一决策：审计与 Resource Activity 分离；查询要求 `system.audit.read` 并实时复核 RBAC；事件按稳定时间/id 排序；写入边界集中脱敏且不保存认证材料明文。
- 验证证据：审计与授权相关回归通过；operations 查询/写入测试和授权过滤器测试均通过。真实 PostgreSQL 分页 SQL、约束和完整 API 联调仍需 Docker/Testcontainers，未伪造运行证据。

## A09-02 浏览列表和详情

- 日期：2026-09-09
- 推荐决策：复用 Resource Owner 的列表/详情 API；列表 SQL 固定限定 `owner_id` 与 ACTIVE 生命周期，详情使用 `findByIdAndOwnerId`，分页按稳定 `updated_at` 顺序返回 ResourceView、标题和外部身份。
- 失败语义：跨 owner 或不存在资源统一 NotFound；空结果返回空页；非法分页参数在 Application 层拒绝，不进入 Repository。
- Console 对接审计：`console/src/views/resources/index.vue` 真实调用 `GET /resources`，支持关键词/类型筛选、加载/空状态/错误反馈并打开详情；`Detail.vue` 真实调用 `GET /resources/{resource_id}` 及关联详情 API，显示后端 Resource、标题和生命周期数据。
- 验证：`DefaultResourceServiceTest` 覆盖 owner-scoped 分页列表、空列表、跨 owner 详情拒绝和非法分页；resource 服务回归 19 项通过；Console `pnpm typecheck`、`pnpm build` 通过；运行中的列表/详情路由均返回 HTTP 200。

## A09-03 编辑时检测版本冲突

- 日期：2026-09-09
- 推荐决策：更新必须携带 expected_version/If-Match；Application 层先按 owner 读取并比较版本，持久化继续使用 Resource `@Version` 乐观并发，不允许旧版本静默覆盖新值。
- 失败语义：版本过期返回稳定 Conflict；冲突路径不写 Resource、标题、事件或审计，不改变 Resource 身份或共享 Blob。
- Console 对接审计：`console/src/views/resources/Detail.vue` 编辑资源时从当前版本发送 `If-Match: "<version>"`，收到 409 时显示“资源已被其他请求修改，请刷新后重新编辑”，不静默覆盖；成功后更新当前详情状态。
- 验证：`DefaultResourceServiceTest` 以两次重复更新复现首个版本提交后旧版本拒绝，确认只发生一次保存；resource 回归 19 项通过；Console `pnpm typecheck`、`pnpm build` 通过；资源详情路由返回 HTTP 200。

## A09-04 归档资源

- 日期：2026-09-09
- 推荐决策：仅 ACTIVE Resource 可归档；归档保持 Resource UUID、owner、标题和任何 Attachment/Blob 身份不变，只更新生命周期与版本并发布事件/审计。
- 失败语义：跨 owner/不存在由 owner-scoped 查询统一 NotFound；TRASHED 等非 ACTIVE 状态返回 Conflict；拒绝路径不写 Resource、不触碰 Blob。
- Console 对接审计：`console/src/views/resources/Detail.vue` 的归档按钮要求确认并调用 `/resources/{resource_id}/actions/archive`，携带当前 `If-Match`；成功更新详情，失败显示状态错误，不删除 Attachment/Blob。
- 验证：`DefaultResourceServiceTest` 覆盖 ACTIVE 归档成功、Resource 身份保留、TRASHED 状态拒绝和无写入；resource 回归 19 项通过；Console 资源详情路由返回 HTTP 200。

## A09-05 移入回收站

- 日期：2026-09-09
- 推荐决策：移入回收站是 Resource 的逻辑生命周期变更；状态、事件和审计在同一 reactive transaction 内提交，绝不直接删除 Attachment/Blob。
- 失败语义：跨 owner/不存在统一 NotFound；旧版本按 If-Match 拒绝；重复处理已 TRASHED 资源保持幂等且不重复写入、审计或业务副作用。
- Console 对接审计：资源详情页的“移入回收站”调用 `/resources/{resource_id}/actions/trash`，携带 `If-Match` 并在确认后返回列表；错误和版本冲突在页面显示，附件/Blob 不由页面直接删除。
- 验证：`DefaultResourceServiceTest` 覆盖正常移入、身份/Blob 保留、重复幂等和版本边界；resource 回归 19 项通过；Console 资源详情路由返回 HTTP 200。

## A09-06 恢复资源

- 日期：2026-09-09
- 推荐决策：仅 TRASHED 或 ARCHIVED Resource 可恢复为 ACTIVE；恢复保持 Resource UUID、owner、标题及 Attachment/Blob 身份不变，状态、事件和审计在同一 reactive transaction 内提交。
- 失败语义：跨 owner/不存在统一 NotFound；ACTIVE 或版本不匹配返回 Conflict/Precondition Failed；拒绝路径不写 Resource、不重复产生业务副作用。
- Console 对接审计：`console/src/views/storage/Archive.vue` 先按 Resource ID 调用详情 API，再调用 `/resources/{resource_id}/actions/restore`，携带 `If-Match`；恢复结果刷新页面并对非法状态/版本错误显示提示。
- 验证：`DefaultResourceServiceTest` 覆盖 TRASHED 恢复成功、身份/删除时间清理、ACTIVE 拒绝和版本边界；resource 回归 19 项通过；Console 存储归档路由返回 HTTP 200。

## A09-04 Console 归档列表补齐
- 修复存储中心“已归档资源”仍为占位的问题；资源列表 API 新增按契约参数 `lifecycle_status=ARCHIVED` 查询，保持 owner scope、分页和标题搜索能力。
- `console/src/views/storage/Archive.vue` 真实加载归档资源并展示 Resource ID、标题、类型、状态和更新时间；空/错误状态沿用页面反馈。
- 验证：`DefaultResourceServiceTest` 20/20；API/路由契约测试 9/9；Console `pnpm typecheck`、`pnpm build` 通过；主要提交：`e3e974e6`、`402b9525`。

## A09-07 执行符合保留规则的永久删除

- 日期：2026-09-09
- 推荐决策：当前 Resource Schema 未单独建模 retention deadline，因此以 `TRASHED + deleted_at` 作为当前最小保留条件；永久删除写入 `PURGED` 可审计终态并保留 Resource UUID，不物理删除 Resource、标题或外部身份。
- 安全边界：入口为独立 purge Action，要求 `If-Match` 和显式 `X-Ikaros-Confirmation: PURGE`；状态事件与 Audit 在同一 reactive transaction 内提交；Attachment/Blob 引用释放和物理 GC 留给其 Owner 按引用、备份及保留规则处理。
- 失败语义：ACTIVE/ARCHIVED、缺少 `deleted_at` 或版本过期均拒绝且不写入、不审计；owner-scoped 查询隔离其他用户和对象。
- Console 对接审计：`console/src/views/storage/Archive.vue` 的永久删除按钮要求用户输入 Resource ID，先读取当前版本，再经显式确认发送 `If-Match` 与 `X-Ikaros-Confirmation: PURGE`；成功清空输入并刷新，失败显示错误，不在页面物理删除 Blob。
- 验证：`DefaultResourceServiceTest` 覆盖 TRASHED 成功转为 PURGED、身份/Blob 边界、ACTIVE 禁止分支；resource 回归 19 项通过；Console 存储归档路由返回 HTTP 200。未启动真实 PostgreSQL/Testcontainers（本机 Docker 不可用），未伪造 SQL 联调证据。

## A09 资源生命周期（父 issue）

- 日期：2026-09-09
- 本地验收结论：A09-01 至 A09-07 已按顺序完成，覆盖创建、浏览/详情、版本冲突、归档、回收站、恢复和符合保留条件的永久删除。
- Console 对接总审计：资源中心列表/详情页真实接入创建、浏览、编辑、归档、回收站；存储中心归档页真实接入恢复和永久删除。所有页面均携带 owner 由服务端解析的当前 Actor、If-Match 版本和必要的高风险确认，成功/冲突/错误/空状态均可见。
- 主要 commits：`645954b1`、`8a5bd90e`、`a9dd07dc`、`b998f8c1`、`0b6b9b0f`、`35a5afa5`、`f459a19a`。
- 统一决策：Resource 生命周期由显式 Application Action 管理；owner scope、乐观并发、事务内事件与审计保持一致；逻辑生命周期绝不隐式删除共享 Attachment/Blob。
- 验证证据：Resource 服务单测已覆盖各子行为的成功、状态拒绝、权限边界、幂等和版本冲突分支；真实 PostgreSQL/Testcontainers 仍需 Docker 环境补跑。

## A10-01 添加多语言标题

- 日期：2026-09-09
- 推荐决策：复用既有 ResourceTitle Application/API；同一 Resource 的 locale 唯一，标题保存支持主标题切换，删除最后标题被拒绝，owner scope 由 Resource 查询保证。
- 失败语义：请求校验拒绝空/超长 locale 或 title；不存在或无权 Resource 拒绝；数据库结果通过重新查询保持一致，保存和审计在同一 reactive transaction 内完成。
- 验证：`DefaultResourceTitleServiceTest` 3 项、`ResourceTitleControllerTest` 2 项通过，覆盖新增多语言标题、主标题降级、最后标题禁止和控制器入口。行为已满足，未做无关重写。
- Console 对接审计：资源详情页“多语言标题”页签通过资源详情 API 展示现有标题，并由“添加标题”对话框调用 `PUT /resources/{id}/titles`；成功后重新加载，失败显示错误。

## A10-02 维护别名

- 日期：2026-09-09
- 推荐决策：别名复用 `ResourceTitle` 聚合但使用 `title_kind=ALIAS`；同一 locale 允许多个别名，完全相同的 Resource/locale/kind/value 由数据库唯一约束去重；别名不能成为主标题。
- 实现：移除旧的 `(resource_id, locale)` 限制，新增 `(resource_id, locale, title_kind, title)` 唯一约束；保存和返回路径均按标题类型区分，避免同语言主标题被误返回。
- 失败语义：空/超长输入交由 API 校验；不存在或无权 Resource 拒绝；别名主标题组合返回 Conflict，数据库重复值保持显式冲突且不产生伪成功。
- 验证：`DefaultResourceTitleServiceTest` 4/4、`ResourceTitleControllerTest` 2/2 通过，新增覆盖同语言多别名并存且不替换主标题；真实 PostgreSQL 约束联调仍需 Docker/Testcontainers。
- Console 对接审计：同一详情页使用独立“添加别名”入口，将 `kind=ALIAS` 发送到同一标题 API，列表按 `kind` 展示，不把别名伪装成主标题。

## A10-03 绑定外部平台身份

- 日期：2026-09-09
- 推荐决策：复用 Resource 外部身份 Application/API；绑定使用 `provider + external_type + external_id` 作为全局唯一身份，外部 ID 仅作映射，不取代 Resource UUID。
- 失败语义：owner-scoped Resource 不存在或无权时拒绝；数据库唯一约束冲突转换为稳定 Conflict；绑定/解绑定分别写 Durable Event 与 Audit，失败不产生伪成功。
- 验证：`DefaultResourceServiceTest` 18/18 通过，覆盖绑定冲突和外部身份生命周期事件；ResourceController 已接入 POST/DELETE 公开路径。真实 PostgreSQL 唯一约束联调仍需 Docker/Testcontainers。
- Console 对接审计：资源详情页“外部身份”页签展示 `provider/type/value`，绑定对话框调用 `POST /resources/{id}/external-identities`，成功后刷新列表，错误可见。

## A10-04 处理重复身份绑定

- 日期：2026-09-09
- 推荐决策：重复外部身份由数据库唯一约束作为并发最终裁决；Application 将 DuplicateKey 映射为稳定 Conflict，不采用静默覆盖或先查后写的竞态方案。
- 失败语义：重复请求只允许第一次写入成功；冲突请求在身份保存阶段失败，不发布绑定事件、不写绑定审计，也不影响已有映射。
- 验证：`DefaultResourceServiceTest` 19/19 通过，新增先成功后重复提交的回归测试，并验证保存次数为 2、成功审计仅 1 次；真实 PostgreSQL 并发约束联调仍需 Docker/Testcontainers。
- Console 对接审计：绑定入口沿用真实 API 的 Conflict 响应并显示错误，不做本地先查后写或静默覆盖；重复身份仍由后端唯一约束裁决。

## A10-05 展示字段来源

- 日期：2026-09-09
- 推荐决策：复用 metadata 查询 API；每个字段返回当前值、`source`、`sourceReference`、`manuallyLocked` 与 `applied`，让调用方可区分用户确认值、自动来源和被人工锁定而未应用的自动结果。
- 失败语义：查询严格按 owner-scoped Resource；不存在或无权目标拒绝，空字段列表返回空结果；自动来源遇到人工锁定时保留现值并返回 `applied=false`。
- 验证：`DefaultResourceMetadataServiceTest` 2/2 通过，覆盖字段来源展示及人工锁定不覆盖；ResourceMetadataController 已接入 GET 公开路径。真实 PostgreSQL 联调仍需 Docker/Testcontainers。
- Console 对接审计：详情页加载 `GET /resources/{id}/metadata`，展示字段值、来源、来源引用、手动锁定状态及 `applied` 状态；已修复前端误读 `userOverride` 导致锁定状态始终显示为否的问题。

## A10-06 保存用户手动覆盖值

- 日期：2026-09-09
- 推荐决策：手动覆盖写入 `MetadataSource.USER` 并设置 `manuallyLocked=true`；写入、审计和已有值读取在同一 reactive transaction 内完成。自动来源遇到锁定字段只返回现值并标记 `applied=false`，解除锁定必须走显式 restore Action。
- 失败语义：owner-scoped Resource 不存在或无权时拒绝；API 校验空/超长字段值；写入失败不产生成功结果，用户值不会被自动同步静默覆盖。
- 验证：`DefaultResourceMetadataServiceTest` 3/3 通过，覆盖手动值 USER/locked 持久化、自动更新保护、显式恢复和字段来源读取；真实 PostgreSQL 事务联调仍需 Docker/Testcontainers。
- Console 对接审计： “新增/覆盖字段”调用 `PUT /resources/{id}/metadata/{fieldKey}` 保存用户值；外部候选通过 `POST /ingestion/resources/metadata-candidates/{id}/resolution` 显式应用/拒绝，刷新后保留后端来源与锁定结果。

## A10 资源描述信息（父 issue）

- 日期：2026-09-09
- 本地验收结论：A10-01 至 A10-06 已按顺序完成，覆盖多语言标题、别名、外部身份绑定、重复身份冲突、字段来源展示和用户手动覆盖。
- 主要 commits：`a9b38af5`、`6c383f32`、`0c2ef3d0`、`47c0d322`、`fa092e8f`、`a1f77796`。
- 统一决策：标题与别名保持明确类型和数据库唯一性；外部身份由唯一约束最终裁决；metadata 来源与用户锁定状态显式返回，自动同步不得静默覆盖人工值；所有写路径遵守 owner scope、审计和 reactive transaction。
- 验证证据：标题、Resource 外部身份和 metadata 服务/控制器回归均通过；真实 PostgreSQL 唯一约束、迁移和事务联调仍需 Docker/Testcontainers。

## A11-01 创建和编辑 Collection

- 日期：2026-09-09
- 推荐决策：保留既有事务化 Collection 创建路径，新增 owner-scoped、`If-Match` 保护的 PUT 编辑路径；编辑只修改名称/描述，保留 Collection UUID、父级和资源成员关系。
- 失败语义：API 校验空/超长名称和描述；不存在或无权 Collection 拒绝；版本不匹配返回 Conflict，不写入、不审计，不改变成员 Resource。
- 验证：`DefaultCollectionServiceTest` 2/2 通过，覆盖创建事件/审计、成员关系路径和编辑版本校验；新增 `CollectionView.version` 供 ETag 返回。真实 PostgreSQL 联调仍需 Docker/Testcontainers。
- Console 对接审计：集合页加载 `GET /collections`；“新建集合”调用 `POST /collections`，“编辑所选集合”调用带当前版本 `If-Match` 的 `PUT /collections/{id}`，成功后刷新，版本冲突显示可理解的错误。

## A11-02 添加与移除资源

- 日期：2026-09-09
- 推荐决策：复用 CollectionResource 关系 API；添加前同时校验 Collection 与 Resource 属于当前用户，数据库唯一约束保证幂等边界；移除只删除组织关系，不删除 Resource 或其 Attachment/Blob。
- 失败语义：任一目标不存在/无权返回 NotFound；重复添加返回 Conflict；添加/移除与事件、审计在同一 reactive transaction 内完成。
- 验证：`DefaultCollectionServiceTest` 2/2 通过，覆盖创建后添加、移除及成员事件；真实 PostgreSQL 关系约束联调仍需 Docker/Testcontainers。
- Console 对接审计：选择集合后加载 `GET /collections/{id}/resources`；“加入集合”调用 `POST /collections/{id}/resources/{resourceId}`，“移除”调用对应 `DELETE`，两者成功后重新读取成员列表。

## A11-03 调整资源顺序

- 日期：2026-09-09
- 推荐决策：新增 `PUT /api/collections/{collectionId}/resources/order`，请求必须完整覆盖当前 Collection 成员且不可重复；服务按请求顺序从 0 重新编号，在同一 reactive transaction 内保存并审计。
- 失败语义：Collection 不存在/无权返回 NotFound；空、重复、未知成员或不完整顺序拒绝且不写入；仅修改成员关系位置，不改变 Resource。
- 验证：`DefaultCollectionServiceTest` 3/3 通过，覆盖成员重排和不完整顺序拒绝；真实 PostgreSQL 顺序约束联调仍需 Docker/Testcontainers。
- Console 对接审计：成员编辑区以逗号分隔的 Resource UUID 生成完整顺序请求，调用 `PUT /collections/{id}/resources/order`；后端拒绝不完整/重复顺序时页面显示错误并保留当前列表。

## A11-04 移动 Collection 层级

- 日期：2026-09-09
- 推荐决策：复用既有 `POST /api/collections/{collectionId}/move`；移动前校验新父级属于当前用户，随后沿祖先链拒绝自引用和任意深度循环，状态更新在 reactive transaction 内完成。
- 失败语义：目标 Collection/父级不存在或无权时拒绝；自引用/循环返回 Conflict；失败不保存，不影响 Collection 成员 Resource。
- 验证：Collection 服务回归 3/3 通过，包含层级移动路径所在服务编译与回归验证；真实 PostgreSQL 层级约束联调仍需 Docker/Testcontainers。
- Console 对接审计：集合选择器调用 `POST /collections/{id}/move` 并以 `parentId` 查询参数传递父集合；成功刷新层级，失败显示移动错误。

## A11-05 阻止循环层级

- 日期：2026-09-09
- 推荐决策：移动操作沿父级链递归检查，拒绝自引用和任意深度祖先循环；校验发生在保存前，失败不写入 Collection，也不影响成员 Resource。
- 失败语义：自引用或将 Collection 移入自身后代返回 Conflict；不存在/无权父级返回 NotFound；不采用静默截断或自动改父级。
- 验证：`DefaultCollectionServiceTest` 4/4 通过，新增覆盖自引用和三节点深层循环，确认没有 Collection 保存发生；真实 PostgreSQL 并发层级联调仍需 Docker/Testcontainers。
- Console 对接审计：移动失败的 409 被转换为“会形成循环”的明确提示，未在前端自动改父级或吞掉后端拒绝。

## A11-06 删除 Collection 时保留资源

- 日期：2026-09-09
- 推荐决策：新增 owner-scoped `DELETE /api/collections/{collectionId}`；先删除 CollectionResource 关系，再删除 Collection，明确不调用 Resource 删除能力。
- 失败语义：不存在或无权 Collection 拒绝；成员关系清理与 Collection 删除在同一 reactive transaction 内完成，Resource、Attachment/Blob 保持不变。
- 验证：`DefaultCollectionServiceTest` 5/5 通过，新增验证成员关系被清理、Collection 被删除且 ResourceRepository 无交互；真实 PostgreSQL FK/事务联调仍需 Docker/Testcontainers。
- Console 对接审计：删除按钮要求显式确认并调用 `DELETE /collections/{id}`，确认文案明确说明只删除成员关系、保留 Resource；成功后清空选择和成员列表并刷新。

## A11 Collection 管理（父 issue）

- 日期：2026-09-09
- 本地验收结论：A11-01 至 A11-06 已按顺序完成，覆盖 Collection 创建/编辑、成员添加/移除、顺序调整、层级移动、循环阻止和删除保留 Resource。
- 主要 commits：`5f31417a`、`faf246ba`、`470cf89c`、`f7be0e61`、`4456df9f`、`399e73c8`。
- 统一决策：Collection 与 Resource 关系保持逻辑解耦；owner scope、事务、唯一约束、完整顺序校验和层级循环保护由 Application/Schema 共同保证；删除 Collection 不级联删除 Resource 或 Blob。
- 验证证据：Collection 服务回归覆盖创建编辑、成员关系、排序、循环和删除边界；真实 PostgreSQL FK/事务/排序联调仍需 Docker/Testcontainers。
- Console 对接总审计：`console/src/views/collections/index.vue` 已覆盖 A11 全部六项真实读写入口，未使用假数据或仅修改本地状态；运行页面 `/resource-center/collections` 返回 HTTP 200，Console typecheck/build 已通过。

## A12-01 创建指定类型的关系

- 日期：2026-09-09
- 推荐决策：复用 ResourceRelation API 创建有向关系；关系类型使用受限枚举，来源与目标必须同属当前 owner，Resource UUID 保持为双方稳定身份。
- 失败语义：自关联在持久化前拒绝；任一资源不存在/无权返回 NotFound；数据库重复关系映射为 Conflict，失败不产生审计或关系记录。
- 验证：`DefaultResourceRelationServiceTest` 3/3、`ResourceRelationControllerTest` 1/1 通过，覆盖指定类型创建、双端 owner 校验路径、自关联拒绝和公开 API。真实 PostgreSQL 约束联调仍需 Docker/Testcontainers。
- Console 对接审计：集合页“关系浏览器”提供关系类型、来源 Resource ID 和目标 Resource ID 输入，创建时调用 `POST /resources/{resourceId}/relations`，成功后重新加载关系。

## A12-02 展示关联资源

- 日期：2026-09-09
- 推荐决策：复用 `GET /api/resources/{resourceId}/relations` 查询来源 Resource 的有向关系，按关系类型和位置稳定排序，返回关系 ID、目标 Resource ID、类型和位置。
- 失败语义：先 owner-scoped 校验来源 Resource，再惰性查询关系；未知/无权来源返回 NotFound，空关系返回空流，不暴露跨 owner 关系。
- 实现修复：将关系查询包装为 `Flux.defer`，避免 owner 校验前 eager 访问关系仓储。
- 验证：`DefaultResourceRelationServiceTest` 4/4、`ResourceRelationControllerTest` 1/1 通过，覆盖正常、空结果、未知来源和公开查询入口；真实 PostgreSQL 分页/排序联调仍需 Docker/Testcontainers。
- Console 对接审计：点击“加载关系”调用 `GET /resources/{resourceId}/relations`，展示目标 ID、类型和位置，并提供空状态和加载状态。

## A12-03 移除关系

- 日期：2026-09-09
- 推荐决策：复用 `DELETE /api/resources/{resourceId}/relations/{relationId}`；先校验来源 Resource owner，再惰性读取并确认关系属于该来源，成功后删除关系并审计，不删除任一 Resource。
- 失败语义：来源不存在/无权返回 NotFound；关系不存在或不属于来源返回 NotFound；失败不删除、不审计。
- 实现修复：关系读取与删除审计均改为惰性 Publisher，避免授权/关系存在性校验前产生仓储或审计副作用。
- 验证：`DefaultResourceRelationServiceTest` 5/5、`ResourceRelationControllerTest` 1/1 通过，覆盖正常、空结果、未知来源、未知关系和公开入口；真实 PostgreSQL 联调仍需 Docker/Testcontainers。
- Console 对接审计：关系表的“删除”按钮调用 `DELETE /resources/{resourceId}/relations/{relationId}`，成功后刷新；不会删除任一 Resource。

## A12-04 阻止无效或重复关系

- 日期：2026-09-09
- 推荐决策：关系类型使用枚举与数据库 CHECK 约束，双方 Resource 使用 FK；应用层提前拒绝空目标/类型和负 position，自关联拒绝，数据库唯一约束最终裁决重复关系。
- 失败语义：无效请求、任一资源无权/不存在、自关联和重复关系均显式失败，不保存、不审计、不发布错误成功事件。
- 验证：`DefaultResourceRelationServiceTest` 6/6、`ResourceRelationControllerTest` 1/1 通过，覆盖指定类型、无效请求、自关联、重复冲突、双方 owner、空结果和移除边界；真实 PostgreSQL 约束联调仍需 Docker/Testcontainers。
- Console 对接审计：关系类型使用后端允许的枚举选项，409 显示“不能创建自关联或重复关系”，其他校验/权限错误保留后端错误信息。

## A13-01 收藏与取消收藏

- 日期：2026-09-09
- 推荐决策：Favorite 以 `(owner_id, resource_id)` 隔离用户；添加幂等，取消幂等，查询返回当前用户状态。收藏关系不改变 Resource 本体。
- 实现修复：添加、取消和查询均改为在 Resource owner 校验后惰性访问 Favorite 仓储，避免未知/无权 Resource 产生 eager 查询或副作用。
- 失败语义：未知/无权 Resource 统一 NotFound；重复收藏不重复保存/审计，重复取消不产生删除/审计；成功路径写 Audit。
- 验证：`DefaultFavoriteServiceTest` 4/4、`FavoriteControllerTest` 2/2 通过，覆盖添加、重复添加、查询未收藏、未知资源和公开入口；真实 PostgreSQL 唯一约束联调仍需 Docker/Testcontainers。
- Console 对接审计：资源详情页“收藏/取消收藏”分别调用 `POST /resources/{id}/favorite` 和 `DELETE /resources/{id}/favorite`，成功后重新加载当前用户状态，未修改 Resource 公共数据。

## A13-02 管理个人标签

- 日期：2026-09-09
- 推荐决策：标签关系按 `(owner_id, resource_id, name)` 隔离并唯一；添加幂等，列表按名称返回，删除只解除用户标签关系，不修改 Resource。
- 实现修复：添加、列表、删除均改为先 owner 校验再惰性访问标签仓储；未知/无权 Resource 不访问标签数据，不产生写入或审计副作用。
- 失败语义：重复标签保持现有关系；未知 Resource/标签统一 NotFound；删除成功写审计和事件。
- 验证：`DefaultResourceTagServiceTest` 4/4 通过，覆盖添加、列表、删除、未知 Resource 和审计路径；真实 PostgreSQL 唯一约束联调仍需 Docker/Testcontainers。
- Console 对接审计：资源详情页的个人标签入口调用 `POST /resources/{id}/tags` 和 `DELETE /resources/{id}/tags/{tagId}`，成功后刷新标签；标签目录页也提供同一真实 API 的加载、添加和移除入口。

## A13-03 保存个人评分

- 日期：2026-09-09
- 推荐决策：复用 `PUT /api/resources/{resourceId}/user-state` 保存用户评分，评分量纲固定为 0–10，并通过 User + Resource 主键隔离，不修改 Resource 公共元数据。
- 实现修复：状态读取改为在 Resource owner 校验通过后惰性访问；评分边界使用精确 `BigDecimal` 比较，避免浮点转换造成边界误判。
- 失败语义：评分越界或目标 Resource 不存在/无权访问时失败，不读取或写入用户状态，不发布成功事件；成功更新沿用事务、版本和 `resource.user-state.changed` 事件。
- 验证：`DefaultUserResourceStateServiceTest` 3/3 通过，覆盖评分更新事件、未知 Resource 授权边界和越界输入；真实 PostgreSQL/Testcontainers 联调仍需 Docker。
- Console 对接审计：资源详情页评分控件通过 `PUT /resources/{id}/user-state` 保存，使用 0–10 输入范围并在 409 时提示刷新重试。

## A13-04 保存消费进度

- 日期：2026-09-09
- 推荐决策：复用 `PUT /api/resources/{resourceId}/user-state` 保存状态码、进度值和单位；进度按 User + Resource 隔离，更新时记录最近访问时间，不修改 Resource 公共元数据。
- 实现修复：状态读取改为在 Resource owner 校验通过后惰性访问，确保未知/无权 Resource 不触发状态读写；沿用事务、版本和统一用户状态变更事件。
- 失败语义：进度值必须为非负数，非法输入或目标 Resource 不存在/无权访问时失败且不产生部分写入；成功后通过返回视图和再次查询观察持久化结果。
- 验证：`DefaultUserResourceStateServiceTest` 4/4 通过，覆盖进度保存并返回持久化状态、评分越界、未知 Resource 和变更事件；真实 PostgreSQL/Testcontainers 联调仍需 Docker。
- Console 对接审计：同一用户状态表单提交 `statusCode/progressValue/progressUnit` 到真实 API，非负进度由控件和后端共同约束，保存结果直接替换页面状态。

## A13-05 查看近期活动

- 日期：2026-09-09
- 推荐决策：复用 `GET /api/activity?limit=` 查询当前用户 Activity，默认 50、上限 200，按发生时间倒序返回；Activity 展示记录与不可删除的 Audit 保持分离。
- 实现修复：记录、近期列表和删除均改为惰性 Publisher；记录先完成 Resource owner 校验，删除成功后才写审计，避免越权或失败链产生仓储/审计副作用。
- 失败语义：未知/无权 Resource 不保存 Activity；limit 超出 1–200 失败；空结果返回空列表；Activity 删除只影响当前用户自己的 Activity。
- 验证：`DefaultResourceActivityServiceTest` 6/6、`ResourceActivityControllerTest` 2/2 通过，覆盖正常记录、限量、空结果、未知 Resource 和非法 limit；真实 PostgreSQL/Testcontainers 分页联调仍需 Docker。
- Console 对接审计：资源详情页加载 `GET /activity?limit=200` 并按当前 Resource ID 展示近期活动；活动为空时显示空状态，活动与不可删除 Audit 分开。

## A13 用户资源状态（父 issue）

- 日期：2026-09-09
- 本地验收结论：A13-01 至 A13-05 已按顺序完成，覆盖收藏、个人标签、评分、消费进度和近期活动；各路径均遵守用户隔离、Resource owner 授权、事务和事件/审计分离。
- 主要 commits：`a2903998`、`ed6fdc03`、`0d9771e0`、`6e950762`、`96b3188e`。
- 验证证据：收藏、标签、用户状态和 Activity 服务/控制器测试均通过；真实 PostgreSQL/Testcontainers 联调仍需 Docker。
- Console 对接总审计：资源详情页和集合页均已接入 A13 对应读写 API；已运行页面 `/resource-center/library/{resourceId}` 与 `/resource-center/collections` 返回 HTTP 200，Console typecheck/build 已通过。

## A14-01 创建上传会话

- 日期：2026-09-09
- 推荐决策：复用 `POST /api/resources/{resourceId}/upload-intents` 生成短时效 Provider 上传意图；先校验 Resource owner，再校验可写 Provider，实际二进制传输由 Provider 负责。
- 实现修复：Provider 可写能力查询改为在 Resource 授权成功后惰性执行，错误身份不会触发 Provider、Blob 或物理上传 adapter 查询；已存在相同内容且有可用 Placement 时返回去重 SKIP 意图。
- 失败语义：未配置上传能力、未知/无权 Resource、不可写/不存在 Provider 和同 SHA 大小冲突均失败；不泄露认证材料，不创建 Attachment/Blob/Placement。
- 验证：`DefaultStorageServiceTest` 8/8 通过，覆盖附件边界回归及未知 Resource 不创建上传意图；真实 Provider 预签名和 PostgreSQL/Testcontainers 联调仍需 Docker/外部 Provider。
- Console 对接审计：`console/src/views/attachments/index.vue` 的“上传附件”对话框读取启用 Provider，并调用 `POST /resources/{id}/attachments/upload-intents` 获取短时效上传地址；页面不自行保存 Provider 凭据。

## A14-02 检查上传约束

- 日期：2026-09-09
- 推荐决策：上传请求在 DTO 层使用 Bean Validation，并在 Application API 再校验必要字段、SHA-256 格式和非负大小；约束失败必须发生在 Provider/Blob/adapter 访问之前。
- 实现修复：`beginUpload` 增加直接调用边界校验，负大小、空必要字段和非法 SHA-256 均显式拒绝；Provider 查询保持在 Resource 授权之后惰性执行。
- 失败语义：不满足上传约束时返回参数错误，不创建上传意图、Attachment、Blob 或 Placement，也不访问 Provider 认证/物理 adapter。
- 验证：`DefaultStorageServiceTest` 9/9 通过，覆盖约束失败、未知 Resource、已有附件边界和相关回归；真实 Provider/PostgreSQL/Testcontainers 联调仍需 Docker/外部 Provider。
- Console 对接审计：文件选择、Provider、Object Key 和 Attachment 角色在上传前由页面校验，SHA-256/大小/媒体类型由浏览器计算后交给后端约束，失败在页面可见。

## A14-03 校验并提交上传结果

- 日期：2026-09-09
- 推荐决策：复用 `POST /api/resources/{resourceId}/upload-commit`，先校验 Resource owner 与可写 Provider，再确认远端对象的 SHA-256、大小和 Provider tier，最后在事务内提交 Attachment、Blob 与 Placement。
- 实现修复：提交链改为先授权 Resource，再惰性查询 Provider 和验证远端对象，防止越权请求触发 Provider/对象验证；保留已有 Attachment、Blob、Placement 身份分离及幂等提交路径。
- 失败语义：未知/无权 Resource、Provider 不可写、SHA-256/大小/tier 不匹配或远端对象不可确认均失败，不产生伪成功 Attachment 或错误成功事件。
- 验证：`DefaultStorageServiceTest` 10/10 通过，覆盖越权提交不访问 Provider/对象 adapter 及附件存储回归；真实 Provider 对象校验和 PostgreSQL/Testcontainers 联调仍需 Docker/外部 Provider。
- Console 对接审计：获得意图后按返回的 method/url 上传真实文件，再调用 `POST /resources/{id}/attachments/commit` 提交 Attachment、Blob 和 Placement；成功刷新附件列表，失败保留错误。

## A14-04 相同内容复用 Blob

- 日期：2026-09-09
- 推荐决策：以规范化 SHA-256 作为 Blob 内容身份；相同摘要复用既有 Blob，Attachment 仍按上传资源独立创建，Placement 负责物理对象绑定。
- 实现确认：`findOrCreateBlob` 先按 SHA-256 查找，复用时强制校验大小与哈希算法；已有可用 Placement 的上传意图返回去重结果，不重复创建物理对象。
- 失败语义：同 SHA 大小不一致显式冲突，不创建新 Blob 或 Attachment；所有 Attachment/Blob/Placement 关系保持独立，失败不破坏既有引用。
- 验证：`DefaultStorageServiceTest` 11/11 通过，覆盖既有 Blob 复用、大小冲突及无新写入；真实 PostgreSQL 唯一约束/Testcontainers 并发联调仍需 Docker。
- Console 对接审计：上传流程使用后端返回的 `deduplicated` 分支，去重时跳过物理上传但仍提交独立 Attachment，页面展示刷新后的 Blob/Placement 结果。

## A14-05 恢复中断上传

- 日期：2026-09-09
- 推荐决策：使用稳定 `Idempotency-Key` 作为上传提交重试边界；客户端可重新执行提交，已完成的 Attachment/Blob/Placement 状态被复用，不重复产生业务副作用。
- 实现确认：提交路径在授权与远端对象校验后进入既有幂等 Attachment 分支；同一资源和 key 已有提交时返回原 Attachment 与 Blob，不再次保存。
- 失败语义：恢复重试中的摘要、大小、Provider 或对象校验失败继续明确失败；既有成功状态不回退、不重复创建、不删除有效引用。
- 验证：`DefaultStorageServiceTest` 12/12 通过，覆盖同一 Idempotency-Key 重试不重复创建 Attachment/Blob 及越权、约束和 Blob 复用回归；真实 Multipart Provider 断点续传联调仍需 Docker/外部 Provider。
- Console 对接审计：上传流程为每次会话生成并复用 `Idempotency-Key`，意图返回 `sessionId`；“终止上传”调用 `DELETE /resources/{id}/attachments/upload-intents/{sessionId}` 并明确提示临时对象清理。

## A14-06 终止并清理失效上传会话（契约与 Schema）

- 日期：2026-09-09
- 推荐决策：按 Storage 设计引入持久化 `storage_upload_session`，记录 Owner、Resource、Provider、临时对象键、声明大小/摘要、状态、TTL、幂等键和版本；会话状态限定为 OPEN/RECEIVING/FINALIZING/COMPLETED/ABORTED/EXPIRED。
- 实现进展：已新增公开 `UploadSessionState`/`UploadSessionView`、Storage-owned Entity/Repository，以及带状态/TTL/幂等唯一约束的版本化 Migration；不保存 Provider 凭据或临时认证材料。
- 验证：`mvn -pl storage -am -DskipTests compile` 通过；服务入口、Provider 清理和过期调度将在后续步骤接入。
- 实现进展：已接入 `abortUploadSession` 的 Owner 隔离、终止状态转换和重复终止幂等行为；`DefaultStorageServiceTest` 13/13 通过。物理临时对象删除与过期会话调度仍待接入。
- 实现进展：新增 `StorageObjectProvider.deleteObject` 统一物理删除 seam；终止会话先提交 ABORTED，再清理临时对象，清理失败保留可重试状态。`DefaultStorageServiceTest` 13/13、`StorageObjectProviderRegistryTest` 1/1 通过。会话创建、过期扫描和 HTTP 入口仍待接入。
- 实现进展：上传意图创建已持久化 `storage_upload_session` 并返回 `session_id`；新增当前用户终止会话 HTTP 入口，终止后清理临时 Provider 对象。`mvn -pl storage -am -DskipTests compile` 通过；过期扫描仍待接入。
- 实现进展：新增过期会话定时扫描，OPEN/RECEIVING/FINALIZING 超时会话标记 EXPIRED，随后清理临时对象并发布 `storage.upload-session.expired@1`；单会话清理失败可重试且不影响其他会话。编译通过，待补扫描器测试与最终 A14-06 验收。
- 验证：`UploadSessionExpirySchedulerTest` 2/2 通过，覆盖过期标记、临时对象清理、事件发布及 Provider 不可用时保留重试机会。
- 最终验收：会话创建已返回 `session_id`，终止入口按 Owner 隔离并清理临时对象，过期扫描可重试清理；`DefaultStorageServiceTest` 15/15、`UploadSessionExpirySchedulerTest` 2/2 通过。主要 commits：`a6f60eb0`、`e6909303`、`2f628294`、`a5408adc`、`6748d3e0`、`e87f1de0`、`1af16c2c`。
- 契约追溯：已登记上传意图/终止会话 HTTP operation、请求/响应 Schema 及 `storage.upload-session.expired@1` 事件。
- Console 对接审计：附件页已覆盖 A14-06 的会话终止入口；过期扫描属于服务端定时清理，页面通过上传会话状态/错误反馈呈现结果，不伪造后台完成状态。

## A12 资源关系管理（父 issue）

- 日期：2026-09-09
- 本地验收结论：A12-01 至 A12-04 已按顺序完成，覆盖指定类型关系创建、关联资源展示、关系移除及无效/重复关系阻止。
- 主要 commits：`dfa9687e`、`52e15855`、`0b73425f`、`1b0f113a`。
- 统一决策：关系类型、方向和双方 Resource owner 均由 Application/Schema 校验；查询和删除遵守先授权后访问、惰性副作用；关系操作不删除 Resource。
- 验证证据：关系服务与控制器回归覆盖正常、空结果、未知资源/关系、自关联、重复和非法输入；真实 PostgreSQL FK/唯一约束联调仍需 Docker/Testcontainers。

## A14 附件上传（父 issue）

- 日期：2026-09-09
- 本地验收结论：A14-01 至 A14-06 已按顺序完成，覆盖上传意图创建、约束检查、远端结果校验与提交、Blob 去重、幂等恢复，以及持久化上传会话的终止/过期清理。
- 主要 commits：`a4d6792d`、`7d710ea4`、`0e9a45a2`、`9478c140`、`eb1f8ae9`、`a6f60eb0`、`e6909303`、`2f628294`、`a5408adc`、`6748d3e0`、`e87f1de0`、`1af16c2c`、`c5bf1970`。
- 统一边界：Attachment、Blob、Placement、Upload Session 身份分离；授权先于 Provider/对象访问；失败不产生伪成功；临时对象清理可重试。
- 验证证据：storage 服务与上传会话测试通过；真实 PostgreSQL/Testcontainers、Multipart/外部 Provider 联调仍需 Docker/外部环境。
- Console 对接总审计：附件页 `/storage-center/attachments` 已覆盖 A14 上传、提交、去重、幂等恢复和会话终止操作，运行页面返回 HTTP 200；Console typecheck/build 已通过。真实 Provider 上传仍需外部 Provider 配置。

## A15-01 添加 Provider

- 日期：2026-09-09
- 推荐决策：通过 `POST /api/storage/providers` 注册 Provider；Provider key/type/tier 和 Secret Reference 进入持久化注册表，直接凭据仅加密保存，绝不返回或记录明文。
- 实现修复：持久化注册路径拒绝将 password/secret 等明文凭据写入 Provider metadata；内存 registry 同样执行 metadata 安全边界，重复 Provider key 显式冲突。
- 失败语义：参数不完整、缺少 `secret://` 引用/凭据、非法 metadata 或重复 key 均在持久化前失败；成功注册默认 ENABLED 并发布 Provider created 事件。
- 验证：`InMemoryStorageProviderRegistryTest` 2/2、`PersistentStorageProviderRegistryTest` 1/1 通过；真实 PostgreSQL 唯一约束联调仍需 Docker。
- Console 对接审计：持久化存储层页面的“添加对象存储 Provider”表单调用 `POST /storage/providers`，支持 Secret Reference 或一次性凭据输入，列表只显示“已配置（引用）”，不回显密钥。
## A15-02 检测连接和读写能力
- 日期：2026-09-09
- 推荐决策：增加 Provider 独立探测入口 `POST /api/admin/storage-providers/{provider_id}/probe`；探测不读取或修改 Attachment、Blob、Placement，也不持久化健康结果。
- 实现修复：新增 `StorageProviderProbeResult`/状态契约、Provider probe service 和 HTTP 路径；S3 adapter 使用随机哨兵对象执行 PUT → HEAD → DELETE，删除置于 finally，失败返回脱敏错误码；未支持的 adapter 返回 `UNSUPPORTED`。
- 失败语义：Provider 不存在沿用 NotFound；无 adapter/凭据/网络失败不产生伪成功；哨兵对象创建后即使 HEAD 失败也尝试清理。
- 验证：`StorageProviderProbeServiceTest` 3/3、`StorageObjectProviderRegistryTest` 1/1，`mvn -pl storage -am '-Dtest=StorageProviderProbeServiceTest,StorageObjectProviderRegistryTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` BUILD SUCCESS。
- Console 对接审计：Provider 表格新增“探测”按钮，调用 `POST /storage/providers/{providerId}/probe`，展示 connection/read/write 探测结果及失败状态。
## A15-03 启用与停用 Provider
- 日期：2026-09-09
- 推荐决策：复用已有 `POST /api/storage/providers/{providerId}/enable` 和 `DELETE /api/storage/providers/{providerId}` 路径，不新增同义状态接口。
- 实现修复：现有 Registry 已将状态变更持久化并分别发布 `storage.provider.enabled` / `storage.provider.disabled`；补充重新查询状态和事件转移验收测试。
- 失败语义：停用 Provider 后 `requireWritable` 拒绝写入；不存在的 Provider 沿用 NotFound；重新启用只改变目标 Provider，不触碰 Attachment、Blob、Placement。
- 验证：`InMemoryStorageProviderRegistryTest` 3/3（含停用拒写、停用后重新启用并重新查询、配置更新事件），Maven targeted test BUILD SUCCESS。
- Console 对接审计：启用/停用按钮分别调用 `POST /storage/providers/{providerId}/enable` 与 `DELETE /storage/providers/{providerId}`，成功后重新加载列表，不修改附件数据。
## A15-04 更新凭据并验证
- 日期：2026-09-09
- 推荐决策：保留凭据替换入口并在同一调用链完成 Provider probe，返回脱敏能力摘要；不返回原始凭据，不把凭据写入事件或日志。
- 实现修复：`POST /api/admin/storage-providers/{provider_id}/credentials` 先加密替换凭据，再调用 Provider probe；补齐 OpenAPI、HTTP Operation 和 Command 契约。
- 失败语义：目标 Provider 不存在返回 NotFound；非法请求由 Bean Validation 拒绝；错误凭据/过期凭据只返回 `FAILED` 与稳定错误码，不泄露认证材料或目标对象数据。
- 验证：沿用 `StorageProviderCredentialServiceTest` 验证凭据以当前密钥加密保存；Provider probe 的成功、未支持和目标不存在分支由 `StorageProviderProbeServiceTest` 3/3 覆盖，Maven BUILD SUCCESS。
- Console 对接审计：Provider 行的“凭据”入口调用 `POST /storage/providers/{providerId}/credentials`，输入框为密码控件，页面仅接收脱敏 probe 结果。
## A15-05 查看容量和健康状态
- 日期：2026-09-09
- 推荐决策：增加只读入口 `GET /api/admin/storage-providers/{provider_id}/status`；健康状态实时复用 Provider probe，容量从 Provider 自有 metadata 读取，缺失返回 `null`，不把未知容量伪装为 0。
- 实现修复：新增状态查询服务与 API 视图，支持 Provider 状态、健康摘要、可选容量/已用容量和检查时间；目标不存在沿用 NotFound。
- 失败语义：probe 失败或过期不会显示为健康成功；未配置容量不产生推测值；不触碰 Attachment、Blob、Placement。
- 验证：`StorageProviderStatusServiceTest` 2/2，覆盖正常和容量空结果/目标不存在；与既有 probe/registry 测试合计 Maven BUILD SUCCESS。
- Console 对接审计：新增“健康 / 容量”按钮调用 `GET /storage/providers/{providerId}/status`，分别展示健康状态和容量/已用容量；容量缺失显示“—”，不把未知值显示为 0。
## A15 存储提供者管理
- 日期：2026-09-09
- 子任务汇总：A15-01 #984、A15-02 #991、A15-03 #992、A15-04 #993、A15-05 #994 均已独立验收并关闭。
- 组合交付：Provider 注册/凭据加密、S3 连接读写 probe、启停状态事件、凭据替换后验证、健康与可选容量查询均已接入 storage API；公开路径已同步 OpenAPI 与 HTTP Operation Registry。
- 主要限制：容量字段依赖 Provider-owned metadata，未配置时明确返回 `null`；未提供 probe adapter 的 Provider 返回 `UNSUPPORTED`。所有内容对象边界仍由 Attachment/Blob/Placement 原有服务负责。
- 验证证据：A15 子任务 targeted Maven 测试均通过；关键汇总测试覆盖 provider 注册、probe、启停事件、凭据加密替换与状态查询。
- Console 对接总审计：`console/src/views/storage/Tiers.vue` 已覆盖 A15 五项 API 操作；Provider 页面 `/storage-center/tiers` 返回 HTTP 200，Console typecheck/build 已通过。真实 Provider 网络探测需配置外部 Provider。
## A16-01 配置 Delivery Provider
- 日期：2026-09-09
- 推荐决策：复用现有 `POST /api/admin/delivery-providers` 配置入口；要求 `Idempotency-Key`，credential_ref 只允许 `secret://` URI，创建后触发 probe。
- 实现修复：现有持久化服务已完成配置编码、重复 key/幂等处理、创建事件和自动 probe；补充非法 credential_ref 与目标不存在的自动化验收证据。
- 失败语义：非法凭据引用在持久化前拒绝；目标不存在返回 NotFound；不向 Attachment、Blob、Placement 写入任何状态。
- 验证：`PersistentDeliveryProviderServiceValidationTest` 2/2，Maven targeted test BUILD SUCCESS。
- Console 对接审计：`console/src/views/storage/DeliveryProviders.vue` 的 Delivery Provider 配置向导调用 `POST /admin/delivery-providers`，要求 Secret Reference 使用 `secret://`，创建后显示真实状态。
## A16-02 绑定存储来源
- 日期：2026-09-09
- 推荐决策：复用 `POST /api/storage/providers/{providerId}/delivery-bindings`，由 Storage Provider 作为 owner 校验来源、由 Delivery Provider key 解析目标。
- 实现修复：现有绑定服务已持久化绑定、校验两端 Provider、处理重复约束并发布创建事件；补充正常创建和目标不存在的验收测试。
- 失败语义：Storage Provider 或 Delivery Provider 不存在返回 NotFound；重复绑定返回 Conflict；保存前失败不产生绑定或事件；Attachment、Blob、Placement 身份保持分离。
- 验证：`PersistentMediaDeliveryBindingServiceTest` 2/2，Maven targeted test BUILD SUCCESS。
- Console 对接审计：选择存储 Provider 后，Binding 对话框调用 `POST /storage/providers/{providerId}/delivery-bindings`，编辑使用带 `If-Match` 的 `PUT`，解绑使用 `DELETE`；页面展示真实 Binding 列表。
## A16-03 按优先级选择可用路径
- 日期：2026-09-09
- 推荐决策：复用现有预览/租约选择链，按 Binding priority 升序选择，并跳过禁用、失败或不存在的 Delivery Provider；不新增旁路选择逻辑。
- 实现修复：现有 `AttachmentPreviewService` 已实现优先级选择和可用性过滤；扩展测试为两条不同优先级路径，确认低 priority 的可用绑定被选中。
- 失败语义：没有可用绑定返回 `StorageUnavailableException`；不产生 grant/lease 伪成功；不修改 Attachment、Blob、Placement。
- 验证：`AttachmentPreviewServiceTest` 2/2，Maven targeted test BUILD SUCCESS。
- Console 对接审计：Binding 表单提供 priority、启用状态和 Range 策略，说明数值越小越优先；预览时由附件详情页选择后端返回的可用分发 Provider。
## A16-04 生成预览地址
- 日期：2026-09-09
- 推荐决策：复用 `GET /api/attachments/{attachmentId}/preview-url`，由资源 owner 校验、Blob/Placement 查询、Binding 优先级选择和 Delivery Grant 合同共同生成短期地址。
- 实现修复：现有预览路径已接入授权、可用 Placement、Provider 健康和绑定选择；扩展测试覆盖多优先级候选，确认返回的是选中路径的预览地址。
- 失败语义：附件不存在或无权访问、无可用绑定分别沿用既有错误；失败不创建 grant/lease 伪成功，不暴露原始凭据。
- 验证：`AttachmentPreviewServiceTest` 2/2，Maven targeted test BUILD SUCCESS。
- Console 对接审计：附件详情页“预览”调用 `GET /attachments/{attachmentId}/preview-url`，按真实返回地址渲染图片、视频、音频或文档，并支持切换返回的分发 Provider。
## A16-05 支持 Range 下载
- 日期：2026-09-09
- 推荐决策：复用附件内容读取 API 的单段 `bytes` Range 语义；授权先于物理读取，Local/S3 adapter 采用流式读取，不聚合整个对象。
- 实现修复：现有控制器返回 206、Content-Range、Content-Length 和 Accept-Ranges；reader 对非法、多段、超限 Range 拒绝并保持 Provider/Blob 边界。
- 失败语义：无权/不存在附件和非法 Range 沿用既有错误；不创建或修改业务引用，不暴露 Provider 凭据。
- 验证：`AttachmentControllerTest`、`AttachmentPreviewServiceTest`、`DeliveryGrantContractServiceTest` 共 4/4，Maven BUILD SUCCESS。
- Console 对接审计：附件详情页“下载”调用 `/attachments/{attachmentId}/content`，浏览器媒体预览使用原生 Range 请求；页面不自行拼接或绕过授权 URL。
## A16-06 拒绝越权和过期访问
- 日期：2026-09-09
- 推荐决策：复用 Delivery Grant 授权链；token 仅保存 hash，授权同时校验 attachment、owner、撤销时间、过期时间和请求 Range，授权通过后才读取物理内容。
- 实现修复：现有 Grant/Attachment Controller 已实现拒绝越权、过期、未知 token 和非法 Range；补充服务级自动化测试覆盖合法 owner、其他 owner、过期和未知 token。
- 失败语义：所有拒绝分支统一为 NotFound，避免泄露 token 是否存在或目标对象信息；失败不创建读取结果、不改变 Attachment、Blob、Placement。
- 验证：`PersistentDeliveryGrantServiceTest` 4/4，Maven targeted test BUILD SUCCESS。
- Console 对接审计：预览和内容下载均从授权 API 获取入口，错误直接显示为预览/下载失败；页面没有本地 token 校验或绕过 owner/过期检查的旁路。
## A16 内容分发
- 日期：2026-09-09
- 子任务汇总：A16-01 #996、A16-02 #997、A16-03 #998、A16-04 #999、A16-05 #1000、A16-06 #1001 均已独立验收并关闭。
- 组合交付：Delivery Provider 配置、Storage Binding、优先级选择、预览地址、Range 下载及 Grant 授权/过期控制已接入 API；相关 OpenAPI/HTTP Operation/Command 契约已同步。
- 验证证据：配置、绑定、优先级、预览、Range、Grant 授权测试均通过；失败路径不泄露凭据、不产生伪成功、不破坏 Attachment/Blob/Placement 引用。
- Console 回溯：`Delivery 运维` 页面已接入真实 Delivery Provider 列表、健康/启用状态和带幂等键的连接检测任务；没有后端契约的 Purge 不提供伪造执行入口。
- Console 对接总审计：Delivery 运维页已覆盖 Provider 配置、存储来源 Binding、优先级/RANGE 策略、探测和启停；附件详情页已覆盖预览、Provider 切换和下载。运行页面 `/edge-acceleration/providers` 与 `/storage-center/attachments/{attachmentId}` 返回 HTTP 200，Console typecheck/build 已通过。
## A17-01 展示归档可用状态
- 日期：2026-09-09
- 推荐决策：复用 Attachment 元数据查询返回的 `availability` 字段；由 Attachment Reference 授权后汇总 Blob、Placement、Provider 与临时恢复状态。
- 实现修复：现有 `DefaultAttachmentAvailabilityQuery` 已区分 PROCESSING、RESTORE_REQUIRED、MISSING、CORRUPTED、READY，并过滤禁用/失败 Provider；无需新增旁路状态模型。
- 失败语义：无权或不存在 Attachment 沿用统一拒绝；Provider 暂时不可用不会把状态显示为 READY；不修改任何引用。
- 验证：`DefaultAttachmentReferenceQueryTest` 2/2、`DefaultStorageServiceTest` 相关可用性分支已通过，Maven targeted test BUILD SUCCESS。
- Console 对接审计：附件列表和详情页均展示后端 `availability`；已修复前端只识别 `AVAILABLE` 而忽略契约状态 `READY` 的问题，READY/AVAILABLE 都显示为可用，其余状态保持明确的非成功提示。
## A19-01 关键词搜索与分页
- 日期：2026-09-09
- 推荐决策：补齐公开只读入口 `GET /api/search?q=&cursor=&limit=`；PostgreSQL Search Projection 仅负责关键词候选和稳定 `(projected_at, document_id)` 游标，Resource Ownership Capability 负责逐条最终授权。
- 实现修复：新增 Search Query Contract、持久化候选查询、授权过滤、空关键词短路和稳定游标分页；未授权或损坏投影在结果中被隐藏，不回写业务真相。
- 失败语义：缺少 actor、空关键词或非法游标返回空页；候选授权失败/投影读取失败不泄露目标对象；limit 上限为 100。
- 契约追溯：新增 `search.keyword-search`，同步 P0 Command/Query Catalog、HTTP Operation Registry 和 OpenAPI `searchResources`。
- 验证：`PersistentSearchQueryServiceTest` 2/2，覆盖授权过滤、分页游标和空关键词不读库；`mvn -pl search -am '-Dtest=PersistentSearchQueryServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` BUILD SUCCESS。
## A19-02 按类型和标签筛选
- 日期：2026-09-09
- 推荐决策：在同一 Search Query Contract 增加白名单语义参数 `type` 与 `tag`；候选查询直接使用 JSONB 投影字段过滤，保留实时 Resource 授权和稳定游标。
- 实现修复：`GET /api/search` 支持类型精确匹配和标签数组包含匹配；空筛选保持全量关键词语义，筛选结果仍经过逐条授权。
- 失败语义：不存在的类型/标签返回空页；授权失败、投影失败不泄露结果；不修改资源真相或投影内容。
- 契约追溯：Search OpenAPI 参数与 `search.keyword-search` 查询契约同步。
- 验证：服务查询测试覆盖筛选参数绑定、授权过滤与空关键词短路；Maven targeted test BUILD SUCCESS。
## A19-03 按当前权限过滤
- 日期：2026-09-09
- 推荐决策：保留 Search Projection 候选查询与 Resource Ownership Capability 的双层边界；HTTP 层继续由统一 ResourceAuthorizationWebFilter 校验 JWT、resource.read 和当前角色状态。
- 实现修复：补充 `/api/search` 直接调用的未认证与权限撤销验收证据；搜索服务对每个候选实时调用 `requireOwned`，不依赖旧 ACL 投影做最终授权。
- 失败语义：无凭据返回 401，缺少/已撤销 resource.read 返回 403；单条资源无权或投影损坏时跳过该结果，不暴露资源数据、认证材料或索引内部状态。
- 验证：`ResourceAuthorizationWebFilterTest` 17/17、`PersistentSearchQueryServiceTest` 2/2；相关 Maven targeted tests BUILD SUCCESS。

## A19-04 资源变更后更新索引

- 当前验收：`ResourceSearchProjectionConsumer` 消费 Resource/Tag Durable Event，重新读取当前可投影 Resource；资源进入不可投影生命周期时删除对应搜索投影，旧事件不会替换更新版本。
- Console 对接审计：搜索页直接查询当前投影；资源详情页仍以 Resource API 作为 Read-your-write 真相，不等待索引更新伪造业务成功。
- 验证：`ResourceSearchProjectionConsumerTest` 4/4；资源投影全量读取接口编译通过。主要提交：`be99b59a`。

## A19-05 重建索引并切换

- 实现：新增 `POST /api/admin/search/rebuild`，从 Resource Owner 提供的当前可投影数据生成新 rebuild generation；重建结果返回 generation、成功数和失败数，投影版本保持乐观保护。
- Console 对接审计：`console/src/views/workbench/Search.vue` 的“重建并切换索引”按钮调用真实 Admin API，并在完成后刷新待处理失败项；页面不直接写 Search Projection。
- 验证：Search 模块编译通过，重建服务既有回归通过；真实 PostgreSQL 全量重建联调仍需 Docker/Testcontainers。

## A19-06 重试失败的投影更新

- 实现：新增 `GET /api/admin/search/projection-failures` 和 `POST /api/admin/search/projection-failures/{failureId}/retry`；重试先从当前 Resource 投影重新投影，成功后才标记原失败记录 resolved，来源不存在或投影失败不会伪造成功。
- Console 对接审计：搜索页索引运维区展示 Resource、来源版本、失败原因和时间，并提供真实“重试”按钮；成功后重新加载失败列表，错误保持可见。
- 验证：`PersistentSearchReconciliationServiceTest` 1/1、`ResourceSearchProjectionConsumerTest` 4/4；Console `pnpm typecheck` 通过。主要提交：`9bd37d6b`。

## A19 资源搜索（父 issue）

- 本地验收结论：A19-01 至 A19-06 已按顺序完成，覆盖搜索查询、筛选、实时授权、增量索引、全量重建切换和失败投影重试。
- 统一决策：Search Projection 仅作为可重建候选读取模型，Resource API 保持业务真相；重建采用 generation，失败项可观察且成功重试后才清除。
- 验证限制：真实 PostgreSQL migration、全量重建和索引切换联调仍需 Docker/Testcontainers；当前环境未伪造该证据。

## A19 Console 对接逐项审计

- A19-01：`console/src/views/workbench/Search.vue` 调用真实 `GET /search`，提交关键词后展示结果、空状态和错误状态，并把查询条件同步到 URL。
- A19-02：类型标签和标签输入分别传递 `type`/`tag` 参数，筛选变化会重新请求服务端，不在前端伪造筛选。
- A19-03：结果使用后端授权候选；“下一页”使用 `nextCursor` 继续请求，页面不缓存或暴露未授权结果。
- 验证：搜索页面 `/workbench/search` 返回 HTTP 200；Console typecheck/build 已通过。

## A20 内容导入（父 issue）

- Console 对接审计：`console/src/views/ingestion/index.vue` 已覆盖来源创建/启停、扫描、候选预览、导入计划逐项修改与审批、导入运行结果、失败项重试和取消；所有操作调用 `/ingestion/*` 真实 API，并显示加载、空、错误和后台运行状态。
- 本地验收证据：`DefaultIngestionSourceServiceTest` 2/2、`DefaultImportRunServiceTest` 2/2、`DefaultMetadataCandidateServiceTest` 2/2，合计 6/6；Console 页面已通过既有 typecheck/build 验证。
- 证据限制：扫描/计划组合路径的专门服务测试和真实 Provider/文件系统联调仍需补充；Docker/Testcontainers 不可用期间不伪造该环境证据。

## A23 通知中心
- 日期：2026-09-10
- 子任务汇总：通知持久化、筛选分页、标记已读、关联目标跳转和任务通知偏好已分别实现并提交；Console 通知中心与账户偏好页均使用服务端 API。
- 组合交付：通知由 durable background-task terminal event 生成并按 recipient 去重；支持状态/来源/优先级筛选、已读操作、任务/Resource 关联跳转和成功/失败通知偏好。
- 失败语义：未认证/越权请求由统一安全层拒绝；通知偏好关闭时不生成对应任务通知；没有 durable actor 的系统事件不伪造接收人。
- 验证：`NotificationServiceTest` 5/5；Console `pnpm typecheck`、`pnpm build` 通过；运行时迁移至 `202609070700`，通知 API 已出现在 `/openapi.json`。
## A23 Console 对接审计

- 通知列表调用 `GET /communications/notifications`，真实传递阅读状态、来源、优先级和分页参数；标记已读调用 `POST /communications/notifications/{id}/actions/read`。
- 任务/Resource 关联按钮分别跳转后台任务或资源详情；账户偏好页读取并保存 `GET/PUT /communications/notification-preferences`。
- 移除没有后端契约支持的“我的/全部授权范围”伪筛选，避免页面显示与实际查询语义不一致；批量操作和投递日志继续明确显示为未接入。
- 验证：通知页与偏好页路由返回 HTTP 200，Console typecheck/build 已通过。

## A24 运行诊断
- 日期：2026-09-10
- 子任务汇总：服务就绪、数据库/存储健康、Request ID 审计筛选、任务/事件投递积压和可操作异常提示均已接入对应 Console 健康/审计入口。
- 组合交付：新增只读 `GET /api/health/operations`，由 Operations 读取任务计数、由 Integration capability 读取 Outbox 投递状态；Health 页面显示真实探针结果，并将异常跳转到后台任务、通知投递或存储 Provider 页面。
- 失败语义：探针请求失败显示 DOWN；积压显示 DEGRADED；未知状态不显示为健康；诊断响应不包含凭据或物理路径。
- 验证：后端 `mvn -s .mvn-local-settings.xml -pl application -am -DskipTests package` BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` 通过；运行时 `/openapi.json` 确认 `/api/health/operations`，未认证访问返回 401。
- 主要提交：`bfb85fa2`（任务/投递诊断）、`df0ffbb2`（可操作异常提示）、`6b227ebc`（HTTP 契约登记）。
## A24 Console 对接审计

- 健康页调用 `/health/live`、`/health/ready`、`/health/operations` 和 Provider status API，展示服务就绪、任务队列、Durable Event 投递和存储探针状态。
- Request ID/审计筛选继续通过审计页真实 API；异常卡片跳转后台任务、通知投递或存储 Provider 页面。
- 修复自动刷新下拉框仅改本地状态的问题：选择 30 秒、1 分钟或 5 分钟后会定时重新请求健康 API，离开页面时清理定时器。
- 未提供真实指标/告警 API 的 CPU、Memory、Incident 区域继续显示空状态，不伪造监控数据。
- 验证：健康页 `/operations-center/health` 返回 HTTP 200，Console typecheck/build 已通过。

## B01 视频与剧集管理
- 日期：2026-09-10
- 子任务汇总：B01-01 至 B01-05 已按依赖顺序完成；每个功能均有后端 API、Console 管理入口和针对性验证。
- 组合交付：视频条目创建、剧集顺序维护、播放附件关联、字幕/封面关联以及附件可用状态均已接入 `视频条目` 页面；页面通过真实 API 加载和提交，不使用静态演示数据。
- 失败语义：视频 Resource 创建失败不落媒体主体；剧集顺序校验主体/Season 所属关系并在事务中避免唯一键冲突；附件关联必须通过可读/活动引用校验；可用性查询失败显示 `UNKNOWN`，不伪造为可用。
- 契约追溯：媒体 subjects、episodes reorder、releases、subtitles 和 attachment availability 已同步 HTTP Operation Registry 与 OpenAPI。
- 验证：媒体目录、Release、字幕服务测试累计 8/8；Console `pnpm typecheck` 通过。B01-05 主要提交：`d9c6b903`（Console 展示附件可用状态与契约登记）。
## B01 Console 对接逐项审计

- B01-01：视频条目页调用 `POST /media/subjects` 创建条目，并通过 `/media/subjects` 与资源 API 加载真实列表。
- B01-02：Season/Episode 顺序抽屉调用真实 Season/Episode 查询和 `POST /media/subjects/{subjectId}/seasons/{seasonId}/actions/reorder-episodes` 保存顺序。
- B01-03：播放附件区调用 `/media/resources/{resourceId}/releases`，不把附件 ID 当作静态演示数据。
- B01-04：字幕/封面区真实加载附件与字幕 API，字幕关联调用 `POST /media/releases/{releaseId}/subtitles`，封面按 Attachment 角色展示。
- B01-05：Release 和封面通过 `/attachments/{attachmentId}/availability` 刷新真实可用状态，失败显示 UNKNOWN，不伪造 READY。
- 验证：视频条目页面 `/media/videos` 返回 HTTP 200，Console typecheck/build 已通过；后端媒体相关测试累计 8/8 通过。

## B02-01 选择附件开始播放
- 日期：2026-09-10
- 实现：视频条目页选择可用 Release 后，先调用授权播放源解析，再创建播放 Session，并通过预览授权地址打开真实视频播放器；不可用 Release 在页面上不可播放。
- 边界：播放源只引用 Attachment，播放 Session 只保存 Resource/Release/用户和位置，不把物理存储路径写入媒体业务状态。
- 验证：`PersistentMediaPlaybackServiceTest` 2/2，覆盖自有可用 Release 创建 ACTIVE Session 和归档 Release 拒绝且不保存；Console `pnpm typecheck` 通过；播放源与 Session API 已登记 OpenAPI/HTTP Registry。
- 主要提交：`bc401c1e`。
## B02-02 保存播放进度
- 日期：2026-09-10
- 实现：播放器在播放过程中按位置变化保存，暂停和关闭时强制保存；请求携带 Session 版本，关闭播放器后结束会话，避免遗留 ACTIVE 会话。
- 持久化：媒体服务更新 Playback Session，并通过 `ResourceProgressService` 写入 `VIDEO_SECONDS` 进度；超过已知总时长的输入在持久化前拒绝。
- 验证：`PersistentMediaPlaybackServiceTest` 4/4，覆盖创建会话、不可播放 Release、进度持久化和超时长拒绝；Console `pnpm typecheck` 通过；PATCH API 已登记 OpenAPI/HTTP Registry。
- 主要提交：`1c7aa0bd`。
## B02-03 从上次位置续播
- 日期：2026-09-10
- 实现：开始播放前读取当前用户对 Resource 的 `VIDEO_SECONDS` 进度；未完成记录作为 Session 起始位置，并在播放器 `loadedmetadata` 后定位；已完成记录从 0 秒重新开始。
- 失败语义：首次播放或进度不存在按 0 秒处理；进度读取失败不伪造已完成状态，仍通过正常播放授权链校验 Resource/Release/Attachment。
- 验证：Console `pnpm typecheck` 通过；进度查询 API 已登记 OpenAPI/HTTP Registry。主要提交：`56569128`。
## B02-04 切换字幕
- 日期：2026-09-10
- 实现：播放器加载当前 Release 的字幕列表，为字幕 Attachment 获取授权地址并生成原生字幕轨道；Console 下拉框可启用或关闭指定语言/标题字幕。
- 失败语义：字幕列表或单个字幕授权失败时保留字幕记录但不生成无效轨道，不影响视频主播放；字幕仍受 Release 所属 Resource 和 Attachment 访问校验。
- 验证：Console `pnpm typecheck` 通过；字幕 API 和 Attachment 预览授权链已在 B01-04/B02-01 验证。主要提交：`0a3f7659`。
## B02-05 切换音轨
- 日期：2026-09-10
- 实现：播放器读取浏览器从实际媒体文件解析出的内嵌 AudioTrack 列表，在 Console 中显示轨道名称并切换 `enabled` 状态；未暴露音轨时明确提示，不伪造可选项。
- 边界：不把音轨元数据或物理路径复制到播放会话；切换只作用于当前已授权播放元素。
- 验证：Console `pnpm typecheck` 通过。主要提交：`18e634f8`。
## B02-06 归档内容恢复后继续播放
- 日期：2026-09-10
- 实现：Release/Attachment 不可用时禁止开始播放并提供异步恢复请求；恢复完成后通过真实可用性 API 刷新，只有状态回到 `READY` 才重新开放播放入口。
- 失败语义：恢复请求使用幂等键；恢复中的状态不伪装成可播放，恢复失败由恢复队列显示并可按既有恢复流程重试；原有播放进度不被清除。
- 验证：Console `pnpm typecheck` 通过；恢复请求 API 已登记 OpenAPI/HTTP Registry。主要提交：`ee3029f2`。
## B02 视频播放
- 日期：2026-09-10
- 子任务汇总：B02-01 至 B02-06 已按清单顺序完成本地实现，并全部接入视频条目 Console 播放入口。
- 组合交付：选择 Release 开始播放、保存进度、断点续播、字幕切换、内嵌音轨切换和归档恢复后的继续播放形成一条真实 API 联调路径；Session、Resource Progress、Subtitle、Attachment Restore 各自保持边界。
- 验证：`PersistentMediaPlaybackServiceTest` 4/4；Console `pnpm typecheck` 通过；相关播放/进度/恢复接口已同步 OpenAPI 与 HTTP Operation Registry。
## B02 Console 对接逐项审计

- B02-01：选择可用 Release 后调用播放源、播放 Session 和附件预览授权 API，真实 `<video>` 播放器使用返回地址。
- B02-02：播放器暂停/时间更新/关闭时调用 Session PATCH 保存位置，并使用 `If-Match` 版本控制。
- B02-03：开始播放前读取 `/media/playback/resources/{resourceId}/progress`，在 `loadedmetadata` 后恢复未完成位置。
- B02-04：字幕来自 Release API，并为每个字幕附件获取预览地址后生成原生字幕轨道。
- B02-05：读取浏览器实际暴露的 AudioTrack 并切换 `enabled`，未暴露时明确提示。
- B02-06：不可用附件禁止播放，提供带幂等键的恢复请求；恢复后刷新 availability，只有 READY 才开放播放。
- 验证：视频播放页 `/media/videos` 返回 HTTP 200，Console typecheck/build 已通过；播放服务相关测试覆盖创建、进度和恢复边界。

## B03-01 导入受支持的漫画包
- 日期：2026-09-10
- 实现：阅读库 Console 提供漫画包导入入口；服务端校验当前用户可读附件的 CBZ、CBR、ZIP 扩展名，创建 Comic Work、Edition 和持久化导入记录，并支持按幂等键重复提交。
- 失败语义：附件不存在、无权访问或格式不支持时拒绝创建；创建前不会写入导入记录；导入状态明确为 `ACCEPTED`，章节和页序解析由 B03-02 处理。
- 契约追溯：新增 `reading.create-comic-import`、`reading.list-comic-imports`、`reading.get-comic-import`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentComicImportServiceTest` 2/2；`mvn -s .mvn-local-settings.xml -pl reading -am '-Dtest=PersistentComicImportServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` BUILD SUCCESS；Console `pnpm typecheck` BUILD SUCCESS；运行时 migration 已应用至 `202609100100`，`/openapi.json` 已确认漫画导入路径；主要提交：`ce25edb5`、`6d2cf31b`。
## B03-02 识别章节和页序
- 日期：2026-09-10
- 实现：新增受授权 Attachment 的流式读取能力；解析 CBZ/ZIP 中的图片条目，按顶层目录识别章节，并按自然数字顺序生成页序条目；解析结果持久化到 `reading_comic_import_entry`，Reading Console 可触发解析并显示章节/页数汇总。
- 失败语义：CBR 在未配置 RAR 解析器时返回明确失败原因；源附件不可读、容器损坏或无图片条目不会伪造成功结果，并清理本次候选条目。
- 契约追溯：新增 `reading.parse-comic-import`、`reading.list-comic-import-entries`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentComicImportParseServiceTest`、`PersistentComicImportServiceTest` 共 3/3；Console `pnpm typecheck` 通过；主要提交：`71d09866`。
## B03-03 人工修正页序
- 日期：2026-09-10
- 实现：Reading Console 对已解析导入按章节加载条目，提供上移/下移和保存操作；后端要求提交该章节全部且不重复的条目，在安全偏移区间内重排并递增版本。
- 失败语义：章节不存在、条目缺失、重复或跨导入提交均拒绝；校验失败不写入页序。
- 契约追溯：新增 `reading.reorder-comic-pages`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentComicImportParseServiceTest` 覆盖完整重排和部分列表拒绝；Console `pnpm typecheck` 通过；主要提交：`e5aaf7a5`。
## B03-04 展示解析失败原因
- 日期：2026-09-10
- 实现：导入记录持久化 `error_code`/`error_message`；解析链遇到不可读附件、损坏容器、无支持格式或未配置 CBR 解析器时进入 `FAILED`，Reading Console 在导入表格展示状态和错误原因。
- 失败语义：失败记录不会显示为成功，解析前后候选条目会被清理；错误信息截断到持久化上限，不暴露存储路径或内部堆栈。
- 验证：B03-02 解析失败路径已由服务错误恢复链覆盖；Console `pnpm typecheck` 通过；主要提交：`71d09866`、`e5aaf7a5`。
## B03-05 重试失败解析
- 日期：2026-09-10
- 实现：新增独立 `retry-parse` API；Reading Console 对 `FAILED` 导入显示“重试解析”，其他状态继续使用正常解析/查看结果操作。后端只允许失败记录重试，重试重新读取源附件并更新候选条目。
- 失败语义：成功或进行中的导入不能通过重试接口重复执行；重试失败继续保留 `FAILED` 和明确错误原因，不伪造成功。
- 契约追溯：新增 `reading.retry-comic-import-parse`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentComicImportParseServiceTest`、`PersistentComicImportServiceTest` 共 6/6；Console `pnpm typecheck` 通过；主要提交：`b71d7a4a`。
## B03 Console 对接逐项审计

- B03-01：阅读库漫画导入表单调用 `POST /reading/comic-imports` 并携带幂等键，导入记录通过 `GET /reading/comic-imports` 加载。
- B03-02：导入记录的解析按钮调用 `POST /reading/comic-imports/{id}/actions/parse`，随后加载 entries 展示章节和页数。
- B03-03：按章节加载 entries，使用上移/下移调整本地顺序后调用 `POST /reading/comic-imports/{id}/actions/reorder-pages` 保存完整条目列表。
- B03-04：导入表格直接展示后端 status/errorMessage，失败不会显示为成功。
- B03-05：仅 FAILED 记录显示“重试解析”，调用 `retry-parse`；其他状态不显示伪造的重试入口。
- 验证：阅读库页面 `/reading` 返回 HTTP 200，Console typecheck/build 已通过；B03 相关 Reading 测试累计 6/6 通过。

## B04-01 按页阅读
- 日期：2026-09-10
- 实现：Reading 新增按章节读取有序漫画页列表和按页读取 Attachment 内容的 API；服务端先校验章节所有权，再读取页面对应附件，避免通过页面 ID 越权读取。
- Console：阅读库新增“按页阅读”入口，输入章节 ID 后通过真实 API 加载页面并渲染图片，同时展示加载失败和空结果状态。
- 契约追溯：新增 `reading.list-comic-chapter-pages`、`reading.get-comic-page-content`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：Reading 模块 compile BUILD SUCCESS；Console `pnpm typecheck` BUILD SUCCESS；主要提交：`30bc58d0`。
## B04-02 切换阅读方向和布局
- 日期：2026-09-10
- 实现：阅读器支持从左到右/从右到左和单页/双页布局切换；设置按作品级 `WORK + COMIC` 写入 Reading Preference，重新打开章节时从 API 恢复。
- Console：切换控件和保存按钮已接入阅读库页面，渲染布局与方向即时生效，保存失败会显示错误。
- 契约追溯：补登记 `reading.update-preference`、`reading.get-preference`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`17a3c718`。
## B04-03 保存阅读位置
- 日期：2026-09-10
- 实现：Console 打开章节时创建 Reading Session；翻页以 `COMIC_PAGE` 逻辑 locator 通过 PATCH 保存当前页面、完成状态和版本，使用 If-Match 避免并发覆盖。
- Console：阅读器提供上一页/下一页，显示当前页和保存中状态；加载/保存失败均进入可见错误状态。
- 契约追溯：登记 `reading.start-session`、`reading.update-progress`、`reading.get-progress`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：Console `pnpm typecheck` 通过；主要提交：`96be9163`。
## B04-04 从上次位置续读
- 日期：2026-09-10
- 实现：打开章节时先查询 Work/Edition 的 Reading Progress，按保存的 `COMIC_PAGE` locator 恢复页索引；首次阅读或 locator 已失效时从第一页开始。
- Console：恢复路径与按页阅读入口合并，打开章节后直接显示恢复页；无历史进度不阻断首次阅读。
- 验证：Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`b858bbd3`。
## B04-05 切换章节
- 日期：2026-09-10
- 实现：Console 按 Edition 调用 Reading 章节目录接口，提供章节选择器；切换后复用按页读取、Work/Edition 进度恢复和 Session 保存链路。
- 失败语义：章节目录加载失败、章节不存在或无权访问时保留当前页面并显示错误，不伪造切换成功。
- 验证：Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`ec07abb6`。
## B04 漫画阅读
- 日期：2026-09-10
- 子任务汇总：B04-01 至 B04-05 已按顺序完成本地实现，均接入 Reading Console 阅读器。
- 组合交付：章节目录选择、按页内容读取、阅读方向/布局设置、逻辑阅读位置保存、上次位置恢复和章节切换形成真实 API 联调路径。
## B04 Console 对接逐项审计
- B04-01：`console/src/views/reading/index.vue` 调用 `GET /reading/chapters/{chapterId}/pages` 获取有序页面，并逐页调用 `GET /reading/pages/{pageId}/content` 渲染图片。
- B04-02：方向/布局切换真实读写 `GET/PUT /reading/preferences?scope=WORK&kind=COMIC&workId=...`，不是仅改本地状态。
- B04-03：打开章节创建 `POST /reading/works/{workId}/sessions`，翻页通过 `PATCH /reading/sessions/{sessionId}` 携带 `If-Match` 保存位置。
- B04-04：打开章节先调用 `GET /reading/works/{workId}/progress?editionId=...`，按 `locatorValue` 恢复页面；无进度时回到第一页。
- B04-05：章节选择器调用 `GET /reading/chapters/{editionId}`，选择后复用真实页面、进度和 Session 链路。
- 验证：阅读库页面 `/reading` 返回 HTTP 200，Console typecheck/build 已通过。

## B05-01 导入受支持的电子书
- 日期：2026-09-10
- 实现：新增 EPUB 导入记录、幂等创建 API 和 `reading_ebook_import` 迁移；导入时校验当前用户可读 Attachment，仅接受 `.epub`，创建 EBOOK Work/Edition 并保留明确的 ACCEPTED 状态。
- Console：新增“电子书导入”后台菜单页，支持提交 Attachment ID、书名/语言、导入记录刷新和失败状态展示，全部调用真实 Reading API。
- 契约追溯：新增 `reading.create-ebook-import`、`reading.list-ebook-imports`、`reading.get-ebook-import`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentEbookImportServiceTest` 2/2；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`fa41fcf3`、`8b73ce26`。
## B05-02 解析目录
- 日期：2026-09-10
- 实现：流式读取 EPUB Attachment，安全解析 `META-INF/container.xml`、OPF manifest/spine，按 spine 稳定顺序创建 Reading Chapter，并以导入关联表持久化 href/title/order。
- 失败语义：缺少 container/rootfile、无可阅读 spine、附件不可读或损坏时导入进入 `FAILED`，清理本次章节关联，不伪造成功目录。
- Console：电子书导入页新增“解析目录/重试解析目录”和“查看目录”，通过真实 API 展示章节顺序、内容路径、加载/空结果/错误状态。
- 契约追溯：新增 `reading.parse-ebook-toc`、`reading.list-ebook-chapters`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentEbookTocParseServiceTest`、`PersistentEbookImportServiceTest` 共 3/3；Reading compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`e02de228`、`ccc0db0f`。
## B05-03 展示基础书籍信息
- 日期：2026-09-10
- 实现：新增按导入记录读取基础书籍信息的 API，基于 Resource、Reading Edition 和已持久化 Chapter 返回书名、语言、出版社、来源、章节数及导入状态；查询沿用 owner 边界，不暴露其他用户或不存在对象。
- Console：电子书导入页新增“书籍信息”入口，使用真实 API 展示加载、空字段、章节数和失败状态；查询失败显示可见错误，不伪造成功信息。
- 契约追溯：新增 `reading.get-ebook-book-info`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentEbookBookInfoServiceTest`、`PersistentEbookTocParseServiceTest`、`PersistentEbookImportServiceTest` 共 4/4；Reading compile BUILD SUCCESS；Console `pnpm typecheck` 通过；主要提交：`6acb7de4`。
## B05-04 处理不支持或损坏的文件
- 日期：2026-09-10
- 实现：导入阶段拒绝非 EPUB；目录解析遇到损坏容器、缺失 rootfile 或无可阅读 spine 时清理本次章节关联，保存 `FAILED`、稳定错误码和安全错误摘要，不产生伪成功目录。
- Console：电子书导入页展示失败状态/错误摘要，失败记录保留“重试解析目录”入口；解析成功、失败和空目录均通过真实 API 结果呈现。
- 验证：`PersistentEbookTocParseServiceTest`、`PersistentEbookImportServiceTest` 共 4/4；Console `pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；主要提交：`6acb7de4`、`e29313d1`。
## B05 电子书导入
- 日期：2026-09-10
- 子任务汇总：B05-01 至 B05-04 已按顺序完成；电子书导入、目录解析、基础信息展示和不支持/损坏文件处理均已形成真实 API 与 Console 管理页面闭环。
- 组合交付：`电子书导入` 页面支持提交 EPUB、查看书籍信息、解析/重试目录、查看稳定章节顺序，并在失败时展示错误摘要；Resource/Edition/Chapter 和导入状态分别保持各自边界。
- 验证：B05 相关 Reading 测试 6/6；Console `pnpm typecheck`、`pnpm build` 通过；application package BUILD SUCCESS；运行时 migration `202609100400` 已应用，主要提交：`fa41fcf3`、`8b73ce26`、`e02de228`、`ccc0db0f`、`6acb7de4`、`e29313d1`。
## B05 Console 对接逐项审计
- B05-01：`console/src/views/reading/EbookImport.vue` 通过 `POST/GET /reading/ebook-imports` 提交并刷新导入记录，携带 `Idempotency-Key`。
- B05-02：解析/重试目录调用 `POST /reading/ebook-imports/{id}/actions/parse-toc`，目录通过 `GET /reading/ebook-imports/{id}/chapters` 加载并显示稳定顺序与内容路径。
- B05-03：书籍信息按钮调用 `GET /reading/ebook-imports/{id}/info`，展示后端返回的基础字段、章节数和导入状态。
- B05-04：失败状态与 `errorMessage` 直接展示；失败记录才提供重试解析目录入口，未用本地成功状态掩盖后端失败。
- 验证：电子书导入页返回 HTTP 200，Console typecheck/build 已通过。

## B06-01 按目录进入章节
- 日期：2026-09-10
- 实现：新增 owner-scoped EPUB 章节内容查询，按已持久化章节 href 从源 EPUB 读取 XHTML 正文并提取可阅读文本；导入、章节和附件不属于当前用户或内容不存在时返回 NotFound，不产生伪内容。
- Console：电子书目录增加“打开章节”，通过真实内容 API 打开正文阅读对话框，覆盖加载、空正文和请求失败状态。
- 契约追溯：新增 `reading.get-ebook-chapter-content`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentEbookTocParseServiceTest` 3/3；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时 OpenAPI 包含新路由且未认证请求返回 401；主要提交：`e5a4d622`。
## B06-02 调整阅读设置
- 日期：2026-09-10
- 实现：复用 Reading Preference 的 owner + WORK 作用域，为 EBOOK 保存书籍级字号、行高、内容宽度、主题和滚动/分页设置；修正响应式授权/仓库调用的延迟求值，其他用户的 Work 返回 NotFound。
- Console：电子书导入页新增“阅读设置”，打开时读取已保存配置，未配置使用明确默认值；保存后立即重新读取确认，校验非法字号/行高/宽度并展示加载、错误和成功状态。
- 验证：`PersistentReadingPreferenceServiceTest` 2/2；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`c19f1aff`。
## B06-03 添加和移除书签
- 日期：2026-09-10
- 实现：新增 `reading_bookmark` 持久化表及 owner-scoped 新增、按 Work 列表、删除 API；书签固定绑定 Work/Edition/Chapter 与稳定 locator，跨用户、跨 Edition/Chapter 引用拒绝，删除他人书签返回 NotFound。
- Console：电子书章节阅读对话框支持添加当前章节书签，导入记录支持查看/移除书签；所有操作调用真实 API，并展示加载、空结果、成功和错误状态。
- 契约追溯：新增 `reading.create-bookmark`、`reading.list-bookmarks`、`reading.delete-bookmark`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentReadingBookmarkServiceTest` 2/2；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时 migration `202609100500` 已应用，新 API 未认证请求返回 401；主要提交：`0a784be9`。
## B06-04 保存阅读位置
- 日期：2026-09-10
- 实现：电子书章节打开时建立 Reading Session，使用 `EPUB_LOCATION` 逻辑定位；保存按钮通过 `If-Match` 更新 Session，并在同一进度链路持久化 Work/Edition 的 Reading Progress。
- Console：章节阅读对话框新增“保存阅读位置”，显示保存中、成功和失败状态；保存失败不伪造成功结果。
- 验证：`PersistentReadingProgressServiceTest` 1/1；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`e78dd8f1`。
## B06-05 跨会话恢复阅读状态
- 日期：2026-09-10
- 实现：复用 owner-scoped Reading Progress 查询，以 `chapter_id + EPUB_LOCATION` 恢复上次阅读章节；当前目录缺少已保存章节或无历史进度时保留明确错误，不伪造恢复结果。
- Console：电子书导入记录新增“恢复阅读”，读取 Progress 后匹配当前 EPUB 目录并打开真实章节，同时展示恢复中和失败状态。
- 验证：`PersistentReadingProgressServiceTest` 2/2；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`3919c205`。
## B06 电子书阅读
- 日期：2026-09-10
- 子任务汇总：B06-01 至 B06-05 已按顺序完成，并全部接入电子书 Console 页面。
- 组合交付：目录进入章节、EBOOK 阅读设置、书签增删、逻辑阅读位置保存和跨会话恢复形成真实 API 联调路径；权限、空态、错误态和持久化复查均有覆盖。
- 验证：B06 相关 Reading 测试均通过；Console `pnpm typecheck`、`pnpm build` 通过；application package BUILD SUCCESS；运行时 migration `202609100500` 已应用；主要提交：`e5a4d622`、`c19f1aff`、`0a784be9`、`e78dd8f1`、`3919c205`。
## B06 Console 对接逐项审计
- B06-01：目录“打开章节”调用 `GET /reading/ebook-imports/{importId}/chapters/{chapterId}/content`，正文加载、空正文和错误均有可见状态。
- B06-02：阅读设置通过 `GET/PUT /reading/preferences?scope=WORK&kind=EBOOK&workId=...` 真实读写，并在保存后重新 GET 校验。
- B06-03：书签列表/新增/删除分别调用 `GET/POST /reading/works/{workId}/bookmarks`、`POST /reading/works/{workId}/editions/{editionId}/chapters/{chapterId}/bookmarks`、`DELETE /reading/bookmarks/{id}`。
- B06-04：章节阅读创建 Session，保存位置调用 `PATCH /reading/sessions/{sessionId}` 并携带 `If-Match`。
- B06-05：恢复阅读调用 `GET /reading/works/{workId}/progress?editionId=...`，匹配当前目录章节后再打开真实正文。
- 验证：电子书导入/阅读页返回 HTTP 200，Console typecheck/build 已通过。

## B07-01 导入音乐附件
- 日期：2026-09-10
- 实现：新增带 `Idempotency-Key` 的 Music 导入命令；校验 owner 可读的音频 Attachment，使用 Attachment 的 Resource identity 创建 Track，再绑定 Audio Source；重复 Resource 和非法音频被拒绝，操作在响应式事务中完成，不留伪成功记录。
- Console：`/music` 改为真实音乐入库页，支持 Attachment ID、标题和技术元数据，调用 `/music/imports`，展示 loading、empty、success、error 和持久化导入记录。
- 契约追溯：新增 `music.create-import`、`music.list-imports`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicImportServiceTest` 1/1；Music compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package/runtime migration `202609100600`。
- 主要提交：`d5ba529b`。
## B07-02 识别歌曲信息
- 日期：2026-09-10
- 实现：新增 owner-scoped 音乐元数据识别 API，读取授权 Audio Source 的 Attachment 内容，解析 MP3 ID3 与 FLAC Vorbis 标签；结果持久化为 Metadata Candidate，保留 Track/Attachment 身份边界，不静默覆盖人工元数据。
- Console：音乐库导入记录新增“识别信息”，展示识别中的 loading、候选结果、无标签空态和错误状态。
- 契约追溯：新增 `music.recognize-metadata`、`music.list-metadata-candidates`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`MusicTagParserTest`、`PersistentMusicImportServiceTest`、`PersistentMusicMetadataRecognitionServiceTest` 共 5/5；Music compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`7aadd7dd`、`e1729a4d`。
## B07-03 关联专辑与艺术家
- 日期：2026-09-10
- 实现：新增 Track Association、Track Artist 关联及迁移；确认 Metadata Candidate 后创建独立 Artist/Album Resource，并持久化 Edition、Disc、Track Membership 与艺术家关系，重复确认同一候选可安全复用关联结果。
- Console：音乐库识别候选弹窗新增“关联”操作，展示关联中、已关联、成功和失败状态，并通过真实 API 重新读取关联结果。
- 契约追溯：新增 `music.create-track-association`、`music.list-track-associations`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicTrackAssociationServiceTest` 1/1；Music compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时迁移 `202609100800`、`202609100801` 已应用，关联 API 未认证返回 401；主要提交：`efa29eef`、`e83ca548`。
## B07-04 处理重复歌曲
- 日期：2026-09-10
- 实现：新增基于 Blob SHA-256 与大小的 owner-scoped 重复查询；重复附件只返回已有 Track 结果，不创建/覆盖歌曲。重复导入仍由 Resource identity 与幂等唯一约束拒绝，重复请求不会产生第二条 Track。
- Console：音乐导入表单新增“检查重复”，展示已有歌曲、Track 和附件；无重复、加载、失败以及导入冲突均使用真实 API 结果反馈。
- 契约追溯：新增 `music.find-duplicates`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicDuplicateServiceTest`、`PersistentMusicImportServiceTest` 共 3/3；Music compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；主要提交：`60ef8501`。
## B07-05 修正识别错误
- 日期：2026-09-10
- 实现：新增 owner-scoped Metadata Candidate 人工修正 API；使用可选 `If-Match` 版本校验拒绝并发覆盖，保存时将来源明确标记为 `MANUAL`，保留候选身份与创建时间，不静默改写 Track、Artist 或 Album。
- Console：音乐库候选弹窗新增“修正”表单，支持编辑标题、艺术家、专辑、ISRC、流派、曲目/碟号和发行年份；保存期间展示 loading，成功重新读取候选，冲突和错误均反馈到页面。
- 契约追溯：新增 `music.update-metadata-candidate`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicDuplicateServiceTest`、`PersistentMusicImportServiceTest`、`PersistentMusicMetadataCorrectionServiceTest`、`PersistentMusicMetadataRecognitionServiceTest` 共 7/7；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时迁移版本 `202609100801`，人工修正 API 未认证返回 401，Console `/music` 返回 200；主要提交：`8b216f07`、`2027415b`。
## B07 音乐入库
- 日期：2026-09-10
- 汇总：B07-01 至 B07-05 均已完成后端能力、契约追溯、自动化验证和 Console `/music` 真实 API 对接；导入、识别候选、人工修正、艺术家/专辑关联、重复检测均有明确 loading、空态、成功、冲突或错误反馈。
- 主要提交：`d5ba529b`、`7aadd7dd`、`efa29eef`、`60ef8501`、`8b216f07`。
## B07 Console 对接逐项审计
- B07-01/B07-04：`console/src/views/media/Catalog.vue` 通过 `POST /music/imports`（Idempotency-Key）和 `GET /music/duplicates` 完成导入与重复检查。
- B07-02/B07-03：识别按钮调用 `POST /music/tracks/{trackId}/metadata/recognize`，候选与关联分别通过 GET/POST API 读取和提交。
- B07-05：修正表单通过带 `If-Match` 的 `PATCH /music/metadata-candidates/{id}` 保存，成功后重新 GET 候选。
- 验证：音乐库页 `/music` 返回 HTTP 200，Console typecheck/build 已通过。

## B08-01 播放指定歌曲
- 日期：2026-09-10
- 实现：补齐音乐 Audio Source 列表和播放会话启动的公开契约；播放前校验 Track 所有权、Audio Source 所有权及 `AVAILABLE` 状态，成功后持久化 ACTIVE Playback Session，非法位置、目标不存在和越权来源均拒绝且不创建会话。
- Console：`/music` 导入记录点击歌曲即可查询可用 Audio Source、创建播放会话并获取授权预览 URL，通过 HTML5 Audio 播放；页面展示准备中、授权/播放失败、当前会话和关闭状态。
- 契约追溯：新增 `music.list-audio-sources`、`music.start-playback`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicPlaybackServiceTest` 3/3；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时数据库版本 `202609100801`，播放会话和 Audio Source API 未认证均返回 401，Console `/music` 返回 200；主要提交：`5a7de3f1`、`2d35bb0d`。
## B08-02 添加与移除队列项
- 日期：2026-09-10
- 实现：新增 owner-scoped Queue Entry 添加 API；新项按当前最大 `active_position + 1` 追加，Track 与 Queue 均校验所有权；保留已有删除入口并覆盖不存在/越权目标，不修改歌曲原件。
- Console：`/music` 新增播放队列管理区，支持创建队列、加载队列、添加 Track 和移除 Entry，展示加载、空结果、成功和错误状态。
- 契约追溯：新增 `music.create-queue`、`music.list-queue-entries`、`music.add-queue-entry`、`music.remove-queue-entry`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicQueueServiceTest` 2/2；Music compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时数据库版本 `202609100801`，队列新增/列表 API 未认证均返回 401，Console `/music` 返回 200；主要提交：`4ef7bd1e`、`fb2976c0`。
## B08-03 调整队列顺序
- 日期：2026-09-10
- 实现：新增带 `If-Match` 的队列重排命令；要求提交的 Entry ID 集合与当前队列完全一致，按请求顺序更新 `active_position`，并在同一响应式事务中更新队列版本；缺项、重复项、越权队列和过期版本均拒绝。
- Console：`/music` 队列区支持填写 Entry 顺序和 Queue 版本并保存，保存后重新读取顺序，展示加载、成功和错误结果。
- 契约追溯：新增 `music.reorder-queue`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicQueueServiceTest` 3/3；Music compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时数据库版本 `202609100801`，队列重排 API 未认证返回 401，Console `/music` 返回 200；主要提交：`8b3c5f0e`、`df0dacbb`。
## B08-04 切换播放模式
- 日期：2026-09-10
- 实现：队列播放策略支持 `OFF`、`QUEUE`、`ONE` 三种 Repeat 模式和 Shuffle 开关；更新操作使用 `If-Match` 校验并在响应式事务内持久化，拒绝过期版本。
- Console：`/music` 队列区新增播放模式控件和保存操作，使用 Queue 版本提交并反馈保存结果或冲突错误。
- 契约追溯：新增 `music.update-queue-policy`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicQueueServiceTest` 4/4；Music compile BUILD SUCCESS；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时数据库版本 `202609100801`，播放策略 API 未认证返回 401，Console `/music` 返回 200；主要提交：`41443de6`、`0bf67708`。
## B08-05 展示可用歌词
- 日期：2026-09-10
- 实现：新增独立 `music_lyrics` Artifact 与 Track 归属校验，提供最多 100 条歌词版本查询，返回语言、类型、内容、时间轴、来源、溯源和置信度；目标不存在或越权时拒绝，不把歌词混入 Track 或音频文件身份。
- Console：`/music` 新增“可用歌词”区域，输入 Track ID 后调用真实 API，展示加载、歌词版本、空结果和错误状态。
- 契约追溯：新增 `music.list-lyrics`，同步 HTTP Operation Registry 与 OpenAPI；新增歌词迁移 `V202609100900`。
- 验证：`PersistentMusicLyricsServiceTest` 3/3；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时迁移版本 `202609100900`，歌词 API 未认证返回 401，运行时 OpenAPI 已出现 `/api/music/tracks/{trackId}/lyrics`，Console `/music` 返回 200；主要提交：`07290fce`、`d18030d5`。
## B08-06 恢复播放会话
- 日期：2026-09-10
- 实现：新增 owner-scoped 活跃播放会话查询，按开始时间稳定返回最多 100 条，保留原 Track、Audio Source、队列和已保存位置，不重复创建会话。
- Console：`/music` 启动时加载未完成会话，支持刷新并通过真实 Audio Source/预览地址恢复播放；来源不可用时展示具体失败原因。
- 契约追溯：新增 `music.list-active-playback-sessions`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicPlaybackServiceTest` 4/4；Console `pnpm typecheck`、`pnpm build` BUILD SUCCESS；application package BUILD SUCCESS；运行时迁移版本 `202609100900`，活跃会话 API 未认证返回 401，运行时 OpenAPI 已出现 `/api/music/playback/sessions`，Console `/music` 返回 200；主要提交：`b8bbe5d6`。
## B08 Console 对接逐项审计
- B08-01：点击歌曲真实读取 `/music/tracks/{trackId}/audio-sources`，创建 `/music/playback/tracks/{trackId}/sessions`，再获取附件预览 URL 播放。
- B08-02/B08-03/B08-04：队列创建、增删、重排和播放策略分别调用 `/music/queues` 及其 Entry/Policy API，重排与策略提交携带 `If-Match`。
- B08-05：歌词区调用 `GET /music/tracks/{trackId}/lyrics`，展示加载、空结果、版本和错误。
- B08-06：页面启动调用 `GET /music/playback/sessions`，恢复时重新读取 Audio Source 和预览 URL，不复用失效地址。
- 验证：音乐库页 `/music` 返回 HTTP 200，Console typecheck/build 已通过。

## B09-01 创建和编辑列表
- 日期：2026-09-10
- 实现：播放列表支持创建和 owner-scoped 编辑；编辑使用 `If-Match`/version 拒绝过期更新，保留 Playlist 与 Track 的身份分离。
- Console：`/music` 新增播放列表创建、编辑、版本显示和新建切换，全部调用真实 Music API，并展示加载、空结果和错误状态。
- 契约追溯：新增 `music.create-playlist`、`music.update-playlist`，同步 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicPlaylistServiceTest` 3/3；Console `pnpm typecheck` 通过；application package BUILD SUCCESS；运行时迁移版本 `202609100900`，播放列表创建/编辑 API 未认证均返回 401，运行时 OpenAPI 已出现创建与编辑路径，Console `/music` 返回 200；主要提交：`55830c41`、`7f483b5e`。
## B09-02 添加与移除歌曲
- 日期：2026-09-10
- 实现：复用并验收 Playlist Entry 的 owner-scoped 添加、顺序追加和删除，非法 Track、越权列表或越权 Entry 均拒绝，不修改歌曲原件。
- Console：`/music` 列表歌曲区支持选择列表、加载 Entry、添加 Track 和移除 Entry，展示加载、空结果、成功和错误状态。
- 契约追溯：补齐 `music.list-playlist-entries`、`music.add-playlist-entry`、`music.remove-playlist-entry` 的 HTTP Operation Registry 与 OpenAPI。
- 验证：`PersistentMusicPlaylistServiceTest` 5/5；Console `pnpm typecheck` 与 `pnpm build` 通过；application package BUILD SUCCESS；运行时迁移版本 `202609100900`，列表 Entry 查询/添加/删除 API 未认证均返回 401，运行时 OpenAPI 已出现对应路径，Console `/music` 返回 200；主要提交：`232b064a`、`7d4973cd`。
## B09-03 调整歌曲顺序
- 日期：2026-09-10
- 实现：新增带 `If-Match` 的 Playlist Entry 顺序调整 API，校验列表归属、版本和完整 Entry 集合，成功后递增列表版本并持久化位置。
- Console：`/music` 列表歌曲区支持上移/下移并保存到 API，重新加载结果，展示加载、空结果、成功和冲突/错误状态。
- 契约追溯：新增 `music.reorder-playlist` 的 HTTP Operation Registry 与 OpenAPI 定义。
- 验证：`PersistentMusicPlaylistServiceTest` 7/7；Console `pnpm typecheck` 通过；application package BUILD SUCCESS；运行时迁移版本 `202609100900`，顺序调整 API 未认证返回 401，运行时 OpenAPI 已出现顺序路径，Console `http://127.0.0.1:8849/music` 返回 200；主要提交：`e26a9d21`、`136a0dae`。
## B09-04 从列表指定位置开始播放
- 日期：2026-09-10
- 实现：Console 读取播放列表 Entry，从选定位置开始创建仅包含后续歌曲的 Queue，再复用 Audio Source、播放会话和 HTML5 Audio 播放链路；播放会话记录 Queue 身份。
- Console：列表歌曲区新增“从此处播放”，覆盖队列创建、播放加载、成功提示和失败提示。
- 契约追溯：复用已登记的 `music.list-playlist-entries`、`music.create-queue`、`music.start-playback` 和 Audio Source/preview API，无新增未登记路由。
- 验证：Console `pnpm typecheck` 与 `pnpm build` 通过；application package BUILD SUCCESS；运行时 Console `http://127.0.0.1:8849/music` 返回 200，播放 API 继续受认证保护；主要提交：`bc7a08f2`。
## B09 播放列表
- 日期：2026-09-10
- 整体验收：B09-01 至 B09-04 已逐项完成；播放列表创建/编辑、歌曲增删、顺序调整和指定位置播放均有 Console `/music` 入口并调用公开 API，列表修改不触碰歌曲原件。
- 验证证据：四个子任务的自动化测试、Console typecheck/build、application package 和运行时认证/路由检查均已记录；运行时 Console `/music` 返回 200，相关 API 未认证请求按预期返回 401。
- 主要提交：`55830c41`、`232b064a`、`e26a9d21`、`bc7a08f2`。
## B09 Console 对接逐项审计
- B09-01：播放列表表单通过 `POST/PATCH /music/playlists` 创建和编辑，编辑携带版本/`If-Match`，保存后重新加载列表。
- B09-02：列表歌曲区通过 `GET/POST /music/playlists/{id}/entries` 和 `DELETE /music/playlists/entries/{id}` 完成加载、添加和移除。
- B09-03：上移/下移最终调用 `PATCH /music/playlists/{id}/entries/order`，提交完整 Entry 顺序及 `If-Match`。
- B09-04：“从此处播放”先创建后续歌曲 Queue，再复用 Audio Source、预览地址和播放 Session API。
- 验证：音乐库页 `/music` 返回 HTTP 200，Console typecheck/build 已通过。

## C01-01/C01-02/C01-03/C01-05 Console 对接审计
- `/sharing` 已改为只调用真实 `GET /shares`、`POST /shares` 和 `POST /shares/{id}/actions/revoke`；创建表单字段与 `CreateShareRequest` 对齐：`targetType`、`targetId`、`granteeType`、`granteeId`、`capabilities`、`expiresAt`。
- 创建链接令牌时展示后端本次返回的 token，并提示仅在创建结果中保存；列表展示真实 Share Grant 字段和 ACTIVE/REVOKED 状态。
- 移除了不存在的 `/rooms` 请求，Room/实时协作页明确标记为后续 C02/C03/C04，避免分享列表因无关接口失败。
- 验证：Console `pnpm typecheck`、`pnpm build` 通过；`/sharing` 返回 HTTP 200；主要提交：`60b76efc`。
- C01-04/C01-06：新增 `/collaboration-center/redeem` 验证分享页，调用公开 `POST /shares/redeem?token=...`；成功展示授权目标，失效、过期和撤销令牌直接展示服务端失败原因。
- 验证：Console typecheck/build 通过；主要提交：`8800aae8`。

## C02-01 至 C02-06 Console 对接审计
- 新增 `/collaboration-center/rooms` Room 管理页，真实调用 `GET/POST /rooms`，展示 Room 状态、版本和空/加载/错误状态。
- 成员流程调用 `GET /rooms/{id}/members`、`POST /rooms/{id}/actions/join`、`POST /rooms/{id}/actions/leave`、`POST /rooms/{id}/members/{principal}/actions/remove`。
- 角色与房主变更调用 `POST .../actions/role?role=...`、`POST .../actions/transfer-owner`；Room 生命周期调用 lock/unlock/end API。
- 邀请调用 `POST /rooms/{roomId}/invites`，携带 `Idempotency-Key`；页面不再把 Room 当作未实现占位。
- 验证：Console typecheck/build 通过；主要提交：`6feb874d`。

## C03-01 至 C03-05 Console 对接审计
- 新增 `/collaboration-center/watch`“一起看”页面，播放/暂停/跳转调用 `POST /rooms/{roomId}/control`，携带 `expectedStateVersion`，控制权限由后端校验。
- 页面通过 `GET /rooms/{roomId}/events?afterSequence=...` 增量回放事件，展示 sequence、state version、操作者和 payload，并每 5 秒同步一次以覆盖重连恢复。
- 验证：Console typecheck/build 通过；主要提交：`71d65d44`。

## C04-01 至 C04-05 Console 对接审计
- 新增 `/collaboration-center/listen`“一起听”页面，使用 `POST /rooms/{roomId}/control` 提交 `QUEUE_UPDATE`、`TRACK_CHANGE` 和 `PLAY_STATE`，由后端校验成员控制权限与状态版本。
- 页面使用 `GET /rooms/{roomId}/events?afterSequence=...` 增量同步队列/歌曲/播放状态，并每 5 秒轮询，覆盖断线后按 sequence 恢复。
- 验证：Console typecheck 通过；主要提交：`d42ae610`。

## 媒体消费 Console API 对齐修复
- `/media` 原页面把不存在于 `PlaybackHistoryView` 的标题、进度和删除能力渲染成可用操作；已改为仅展示 `/media/playback/history` 实际返回的 `resourceId`、`sessionId`、`startedAt`、`endedAt` 和 `watchedSeconds`。
- 移除无后端契约支撑的“继续”“从历史中移除”和通用队列“保存顺序”伪入口；音乐播放/队列继续使用 `/music` 的真实 API 页面。
- 验证：`/media` 返回 HTTP 200，Console `pnpm typecheck`、`pnpm build` 通过；主要提交：`efe18ed3`。

## C05-01 Console 对接审计
- 新增 `/documents/editor` 协作编辑页，真实调用 `GET /documents`、`GET /documents/{id}/working-copy` 和 `PUT /documents/{id}/working-copy`。
- 保存携带 `expectedVersion`；后端返回 409 时页面明确提示工作副本已被其他编辑者修改，避免静默覆盖。
- 验证：Console typecheck/build 通过；主要提交：`6232c405`。

## C05-02 Console 对接审计
- `document` 新增 owner-scoped Presence 心跳、查询和离开 API：`PUT/GET/DELETE /documents/{documentId}/presence`，Presence 使用 30 秒短 TTL，不写入文档版本真相。
- `/documents/editor` 每 10 秒发送心跳并刷新在线人数，组件卸载时 best-effort 离开；Presence 请求失败显示可见错误。
- 验证：`mvn -s .mvn-local-settings.xml -pl document -am -DskipTests compile` BUILD SUCCESS；主要提交：`1771997c`、`ee2a054`。

## C05-03 Console 对接审计
- 新增 `POST /documents/{documentId}/working-copy/actions/merge`，以基线内容、离线本地内容和服务端当前工作副本执行保守三方合并。
- 服务端未发生变更时直接采用本地内容；双方均变更时返回 `<<<<<<< LOCAL` / `=======` / `>>>>>>> SERVER` 冲突标记，不静默覆盖，也不自动保存合并结果。
- `/documents/editor` 在工作副本保存返回 409 时自动请求合并结果，将结果放回编辑器供用户人工处理后再保存。
- 验证：`DocumentMergeServiceTest` 2/2；Console `pnpm typecheck`、`pnpm build` 通过；后端 compile BUILD SUCCESS；主要提交：`d03d7d8c`、`e2c551d5`。

## A21-01 元数据同步来源配置
- 日期：2026-09-10
- 实现：新增独立的 `metadata_sync_source` Owner 边界，支持 owner-scoped Provider、`secret://` 凭据引用、MANUAL/HOURLY/DAILY 刷新策略及启停审计；响应只暴露凭据是否配置，不暴露引用内容。
- Console：`/ingestion` 的“元数据同步”页支持真实 API 创建、列表、启用和停用同步来源，并展示凭据配置状态。
- 验证：`DefaultMetadataSyncSourceServiceTest` 2/2；Console `pnpm typecheck` 通过；主要提交：`46edfab6`、`59da474f`。

## A21-02 检测外部更新
- 日期：2026-09-10
- 实现：新增显式刷新检测 API；校验同步来源启用状态与 Resource owner，比较当前 metadata 值，变化时创建 `PENDING` Provider candidate，未变化返回 `UNCHANGED` 且不写入。
- Console：`/ingestion` 的“检测外部更新”表单调用真实 `POST /api/metadata/sync-sources/{sourceId}/refresh`，展示候选创建或值未变化结果。
- 验证：`DefaultMetadataSyncServiceTest` 2/2；Console `pnpm typecheck` 通过；主要提交：`dde659f5`。
