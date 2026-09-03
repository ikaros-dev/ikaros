# Ikaros V2 后台任务与调度器设计（Background Task / Scheduler Design）

> 状态：专项设计定稿  
> 适用范围：Ikaros V2  
> 契约版本：V2  
> 本文角色：定义 Ikaros V2 统一 Background Task / Scheduler 的持久化执行模型、调度语义、状态机、重试与恢复、并发控制、取消、Handler 契约、数据边界和运维接口。  
> 上位约束：`System-Overview-Design.md`

---

## 1. 文档定位

本文是 `System-Overview-Design.md` 在 **Background Task / Scheduler** 专项上的细化设计。

本文不重新定义系统总览已经确定的全局规则，而是在这些规则之下回答以下问题：

- 长耗时操作如何可靠进入后台执行；
- Background Task 如何持久化、认领、续租、恢复和终结；
- 自动重试与人工重试如何区分；
- 取消请求如何进入统一异步状态语义；
- Scheduled Job 如何产生一次次实际执行，而不与 Background Task 混为一体；
- 应用重启、重复调度 Tick、Worker 异常退出时如何避免静默丢任务或重复物化；
- 不同任务类型如何声明幂等、超时、重试、资源类别、并发约束和 Payload 版本；
- 用户权限、Instance 边界、数据敏感等级和 Secret 如何贯穿异步执行；
- Platform Administration / Automation / AI / Analytics 等子系统如何使用统一任务运行时而不越过 Schema Ownership。

### 1.1 设计依据

本文以当前 `main` 中已经合并的 V2 文档为设计依据，重点核对：

- `System-Overview-Design.md`
- `Product-Requirements-Document.md`
- `Platform-Administration-Operations-Subsystem-Design.md`
- `Platform-Integration-Automation-Design.md`
- `AI-Intelligence-Subsystem-Design.md`
- `AI-Persona-System-Design.md`
- `Data-Analytics-Statistics-Subsystem-Design.md`
- `Security-Identity-Authorization-Crypto-Subsystem-Design.md`
- `Secure-Data-Foundation-Design.md`

本文不以 V1 的 Task、Scheduler、线程池、定时器或历史数据库实现作为 V2 设计前提。V1 只能在未来迁移实现阶段作为数据来源被单独评估。

### 1.2 与 System Overview 的关系

以下规则直接继承 System Overview，不在本文另起一套标准：

- 新建 V2 实体 ID 使用统一 UUIDv7 字符串；
- 时间字段使用带时区语义的 ISO 8601 表达；
- 应用默认时区为 UTC+8，允许配置；
- Background Task 与 Scheduled Job 必须遵守 Instance 边界；
- Schema Ownership 决定 DDL、索引、约束和 Migration 的唯一 Owner；
- 数据敏感等级沿用 L0 / L1 / L2 / L3；
- 对外契约版本为 V2，不能与数据库 Schema Version、Task Handler Payload Version 混淆；
- Background Task 的公开异步状态遵循 `pending -> running -> success | failed | cancelled`；
- `cancelled` 是独立终态，不能伪装为 `failed`；
- 失败使用标准错误包络：`code`、`message`、可选 `details`、可选 `retryable`；
- 事件通知不能替代持久化状态，关键异步事实必须以数据库记录为准。

---

## 2. 目标与非目标

### 2.1 目标

V2 Background Task / Scheduler 必须提供：

1. **持久化可靠性**：API 已返回“已受理”的任务不能因为进程重启而消失。
2. **统一状态**：所有长耗时操作使用相同的任务生命周期和错误语义。
3. **可恢复执行**：Runner 崩溃、应用异常退出、Lease 失效后能够按任务策略恢复。
4. **可控重复**：通过 Idempotency Key、Schedule Occurrence Key、Lease Token 和 Handler 幂等约束降低重复副作用。
5. **统一调度**：一次性、Cron、固定间隔调度都通过 Scheduled Job 物化为 Background Task。
6. **可取消**：取消是一项有持久化记录的协作式请求，不是简单删除内存队列元素。
7. **可重试**：区分自动 Attempt Retry、人工 Retry 与下一次 Scheduled Occurrence。
8. **资源隔离**：支持全局并发、Resource Class 并发、Task Type 并发和 Concurrency Key 串行化。
9. **边界清晰**：任务运行时不拥有业务实体，业务子系统不直接修改任务运行时表。
10. **可观测**：任务、Attempt、Scheduler Lag、Retry、Lease Recovery、Misfire 均可被诊断。
11. **安全执行**：异步队列不得成为绕过当前权限或持久化 Secret 明文的通道。
12. **契约可演进**：排队中的旧版本 Payload 在应用升级后仍有明确兼容策略。

### 2.2 非目标

本文不负责：

- Workflow / DAG 编排引擎；
- BPMN；
- 分布式消息队列产品选型；
- 跨 Ikaros Instance 搬运任务；
- 业务领域内部的导入、转码、AI、Automation、Analytics 具体算法；
- 前端任务中心的像素级页面布局；
- Notification / Event Bus 的完整设计；
- 用后台任务替代业务事务；
- 用 Scheduler 替代 Automation Trigger 的业务规则；
- 用 Scheduler 直接执行任意脚本或任意未注册 Handler。

如果未来需要工作流编排，应建立在本文 Background Task 之上，而不是把 Background Task 本身扩展成不可维护的通用工作流 DSL。

---

## 3. 核心术语

### 3.1 Background Task

Background Task 是一个**持久化、可观察、可恢复的逻辑后台工作单元**。

例如：

- 扫描媒体目录；
- 重建索引；
- 执行一次 AI 长任务；
- 安装插件；
- 运行一次 Automation；
- 重算 Analytics；
- 执行导入 / 导出。

Background Task 不是线程，不等于某一次函数调用，也不等于某一个 Runner Process。

### 3.2 Task Attempt

Task Attempt 表示 Background Task 的**一次实际执行尝试**。

一个 Task 可以因为可重试错误产生多个 Attempt：

```text
Task A
├── Attempt 1 -> failed(retryable)
├── Attempt 2 -> failed(retryable)
└── Attempt 3 -> success

Task A final status = success
```

自动重试不会创建新的逻辑 Task。

### 3.3 Scheduled Job

Scheduled Job 是**持久化的调度定义**，回答：

> 什么任务，在什么时间规则下，应被物化为新的 Background Task？

它不是 Background Task，也不直接持有“运行中”线程。

### 3.4 Scheduled Job Run

Scheduled Job Run 是某个 Scheduled Job 的**一次触发 / 物化记录**。

它回答：

- 哪个计划触发了；
- 理论触发时间是什么；
- 是正常触发、补偿触发还是人工立即运行；
- 是否成功创建 Background Task；
- 如果未创建，为什么被 Misfire / Overlap 策略跳过。

Scheduled Job Run 不复制 Background Task 的完整状态机。

### 3.5 Task Handler

Task Handler 是某种 Task Type 的正式执行契约。

它负责：

- 校验 Payload；
- 执行业务逻辑；
- 上报 Progress；
- 响应 Cancellation Token；
- 返回 Result 或标准错误；
- 声明 Retry / Timeout / Idempotency / Resource Class / Concurrency 等运行属性。

### 3.6 Runner

Runner 是当前应用进程内实际执行已认领 Task Attempt 的运行组件。

为避免与 Ikaros 的业务 **Instance** 混淆，本文使用 `runnerId` 表示运行进程 / 执行节点身份，不使用 `instanceId` 表示 Worker。

### 3.7 Lease

Lease 是 Runner 对某个 Task Attempt 的**有时效认领权**。

Lease 的意义是：

- 降低多个执行循环同时运行同一 Task 的概率；
- 允许崩溃后的自动回收；
- 阻止 Lease 已失效的旧 Runner 继续把自己写成最终结果。

Lease 本身不能替代业务幂等设计。

---

## 4. 领域边界与 Schema Ownership

### 4.1 Owner

Background Task / Scheduler 属于 Platform Administration / Operations 的共享运行时基础能力。

其 Schema Owner 负责：

- Background Task 表；
- Task Attempt 表；
- Scheduled Job 表；
- Scheduled Job Run 表；
- 这些表自身的索引、唯一约束、DDL 和 Migration；
- Task Runtime / Scheduler Runtime 的核心服务接口。

### 4.2 Reader / Writer 边界

| 对象 | Schema Owner | 允许读取者 | 允许写入方式 |
|---|---|---|---|
| Background Task | Platform Administration / Operations | Admin API、任务中心、业务调用方、Telemetry | 仅 Task Runtime Service |
| Task Attempt | Platform Administration / Operations | Admin API、诊断 / Telemetry | 仅 Runner / Recovery Service |
| Scheduled Job | Platform Administration / Operations | Admin API、Automation、需要调度的业务域 | 仅 Scheduler Service |
| Scheduled Job Run | Platform Administration / Operations | Admin API、业务域、Telemetry | 仅 Scheduler Materializer |
| AutomationRun | Integration / Automation | Automation 自身、Admin | Automation Service |
| AI Run / Output | AI Intelligence | AI 自身、Admin | AI Service |
| Analytics Snapshot / Report | Analytics | Analytics 自身、Admin | Analytics Service |

业务子系统必须通过服务契约创建 / 查询 / 取消任务，不能直接写 Background Task 表。

### 4.3 业务对象引用

Background Task 可以记录：

```text
subjectType / targetType
subjectId / targetId
originType
originId
```

但这些只是跨域语义引用。

除非两个表属于同一个 Schema Owner，否则 Background Task Migration 不应对 Automation、AI、Media、Analytics 等业务表创建跨域 FK，也不能替业务域创建索引或列。

### 4.4 与 Automation 的边界

推荐：

```text
AutomationDefinition
      │
      ├── 需要计划触发
      ↓
Scheduler Service
      ↓
ScheduledJob
      ↓ due
ScheduledJobRun
      ↓
BackgroundTask(type = automation.run, payload = { automationId, triggerContextRef })
      ↓
AutomationRun / Action execution
```

AutomationRun 是业务运行记录；BackgroundTask 是通用执行记录。两者不能合并成一张“万能 run 表”。

### 4.5 与 AI / Analytics / Plugin 的边界

同样采用“业务记录 + Background Task 引用”模式：

- AI 域拥有 Prompt / Session / Run / Output；
- Analytics 域拥有 Metric / Snapshot / Report；
- Plugin / Automation 域拥有自身定义与业务结果；
- Background Task 只保存通用执行状态与轻量 Result Reference。

---

## 5. 总体架构

```text
Domain API / Admin API / Automation / Internal Service
                       │
                       ↓
                Task Runtime Service
                       │
             durable transaction
                       ↓
                BackgroundTask DB
                       │
                       ↓
                  Dispatcher
                       │
            claim + lease + limits
                       ↓
                    Runner
                       │
                 Task Handler
                       │
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
   Progress        Domain Data     Result/Error
        │              │              │
        └──────────────┴──────┬───────┘
                              ↓
                       durable finalize
```

Scheduler 独立负责“制造任务”：

```text
ScheduledJob DB
      │
      ↓ due scan
Scheduler Evaluator
      │
      ↓ claim / dedupe
ScheduledJobRun
      │
      ↓ materialize
BackgroundTask
      │
      ↓
Task Runtime
```

### 5.1 核心组件

建议运行时组件至少包括：

- `TaskService`
- `TaskRepository`
- `TaskHandlerRegistry`
- `TaskDispatcher`
- `TaskRunner`
- `TaskLeaseManager`
- `TaskRecoveryService`
- `TaskProgressReporter`
- `TaskCancellationService`
- `SchedulerService`
- `SchedulerEvaluator`
- `ScheduledJobMaterializer`
- `SchedulerRecoveryService`

组件名称可以在实现阶段调整，但职责不能重新混为一个巨大 Scheduler Service。

---

## 6. Background Task 逻辑模型

### 6.1 Task 主体

建议逻辑字段：

```text
BackgroundTask
├── id: UUIDv7
├── instanceId: UUIDv7
├── type: string
├── handlerContractVersion: string
├── status: pending | running | success | failed | cancelled
├── priority: integer
├── resourceClass: string
├── concurrencyKey?: string
├── payload: json
├── payloadSensitivity: L0 | L1 | L2 | L3
├── progress?: json
├── result?: json
├── error?: StandardError
├── idempotencyKey?: string
├── parentTaskId?: UUIDv7
├── rootTaskId?: UUIDv7
├── originType?: string
├── originId?: string
├── scheduledJobRunId?: UUIDv7
├── initiatedByPrincipalId?: UUIDv7
├── executionPrincipalKind: USER | SYSTEM
├── authorizationContext?: json
├── traceId?: string
├── requestId?: string
├── scheduledAt: zoned instant
├── nextAttemptAt?: zoned instant
├── attempt: integer
├── maxAttempts: integer
├── timeoutSeconds?: integer
├── cancelRequestedAt?: zoned instant
├── startedAt?: zoned instant
├── finishedAt?: zoned instant
├── leaseOwner?: string
├── leaseToken?: string
├── leaseExpiresAt?: zoned instant
├── heartbeatAt?: zoned instant
├── revision: integer
├── createdAt: zoned instant
└── updatedAt: zoned instant
```

`payloadSensitivity = L3` 在数据模型上允许表达“这个输入属于最高敏感级别”的事实，但 **不表示允许把 L3 Secret 明文持久化到 Payload**。详见安全章节。

### 6.2 `type`

`type` 必须是稳定、可注册、可版本化的逻辑类型，不应使用 TypeScript 类名或模块文件路径作为持久化值。

推荐命名方式：

```text
media.scan
search.rebuild
plugin.install
automation.run
analytics.rebuild
ai.run
```

以上仅是命名示例，不在本文枚举完整业务 Task Type。

### 6.3 `handlerContractVersion`

Task Handler Payload / Result Schema 必须有独立版本。

必须区分：

```text
API Contract Version = V2
Database Schema Version = migration version
Task Handler Contract Version = per task type payload/result version
```

应用升级时，如果数据库中仍有旧版本 Pending / Running-recoverable Task，则新版本 Handler 必须满足以下之一：

1. 仍兼容该 Handler Contract Version；
2. 提供确定性的 Payload Upcaster / Migrator；
3. 明确把任务终结为 `failed`，错误码为“Handler 版本不支持”，并提供运维修复信息。

禁止静默使用新 Schema 猜测旧 Payload。

### 6.4 `priority`

`priority` 只影响**可执行任务之间的选择顺序**，不能绕过：

- `scheduledAt / nextAttemptAt`；
- Resource Class 配额；
- Concurrency Key；
- Instance 边界；
- 权限；
- Handler 是否存在。

相同优先级下默认按可执行时间、创建时间和 UUIDv7 稳定排序。

实现应加入公平性 / Aging 机制或等价策略，防止持续高优先级任务永久饿死普通任务。

### 6.5 `progress`

Progress 推荐结构：

```json
{
  "phase": "scan",
  "current": 120,
  "total": 500,
  "percent": 24,
  "message": "Scanning library",
  "updatedAt": "..."
}
```

规则：

- `total` 未知时允许缺失；
- 不知道总量时前端显示 Indeterminate，不伪造百分比；
- Progress 是“最新快照”，不是业务审计日志；
- 高频进度必须节流 / 合并后持久化，避免数据库写放大；
- 任务终态必须持久化，即使最后一个中间 Progress 因节流未写入也不能影响最终状态正确性。

### 6.6 `result`

Task Result 应保持轻量。

大量输出应存入业务 Owner 的表、Attachment / Blob 或其他领域存储，然后 Task 仅返回：

```json
{
  "resourceType": "analytics.report",
  "resourceId": "..."
}
```

禁止把大型导出文件、AI 完整上下文、大规模扫描结果直接塞入 Task Row。

---

## 7. Task Attempt 模型

### 7.1 为什么需要独立 Attempt

仅在 Task Row 上记录 `attempt = 3` 无法回答：

- 第一次为什么失败；
- 第二次运行多久；
- 哪个 Runner 丢失 Lease；
- 每次退避多久；
- 自动重试是否在预期发生。

因此 V2 推荐保留轻量 Attempt 历史。

```text
TaskAttempt
├── id: UUIDv7
├── taskId: UUIDv7
├── attemptNo: integer
├── runnerId: string
├── leaseToken: string
├── startedAt
├── heartbeatAt?
├── finishedAt?
├── outcome: success | failed | cancelled | lease_lost
├── error?: StandardError
├── retryScheduledAt?: zoned instant
├── durationMs?: integer
└── createdAt
```

Attempt 的 `outcome` 是内部执行事实，不替代 Background Task 的公开状态机。

### 7.2 自动重试

自动重试使用同一 Task：

```text
pending
  ↓ claim
running / attempt 1
  ↓ retryable failure
pending + nextAttemptAt
  ↓ claim
running / attempt 2
  ↓ success
success
```

因此公开状态在非终态期间允许发生：

```text
pending <-> running
```

但终态仍严格只有：

```text
success | failed | cancelled
```

Task 只有在：

- 错误不可重试；或
- 已达到 `maxAttempts`；或
- Retry Policy 明确禁止继续

时进入最终 `failed`。

### 7.3 人工重试

人工重试**不得把旧 Task 从 `failed` 改回 `pending`**。

人工重试创建新的 Task：

```text
Old Task: failed (immutable terminal history)
     │
     └── manual retry
             ↓
New Task: pending
parentTaskId = oldTask.id
rootTaskId   = oldTask.rootTaskId ?? oldTask.id
```

这样可以：

- 保留旧错误证据；
- 独立记录新的权限与 Payload Version；
- 避免终态回滚；
- 让审计和统计区分自动 Attempt 与人工 Retry。

Scheduled Job 的下一次触发也不是对上一次 Task 的 Retry。

---

## 8. 状态机

### 8.1 Background Task 公开状态

```text
                ┌──────────── retryable attempt failure ───────────┐
                │                                                   │
                ↓                                                   │
pending ─────> running ─────> success                              │
   │            │                                                   │
   │            ├──────────> failed                                 │
   │            │                                                   │
   │            └──────────> cancelled                              │
   │                                                                │
   └──────────────────────> cancelled                               │
                running ─────────────> pending ─────────────────────┘
                         automatic retry backoff
```

### 8.2 终态不可逆

一旦 Task 进入：

```text
success
failed
cancelled
```

该 Task 不允许再回到 `pending` 或 `running`。

任何“再执行一次”的需求都创建新 Task。

### 8.3 `cancelRequestedAt` 不是状态

运行中的任务收到取消请求时：

```text
status = running
cancelRequestedAt != null
```

它表示“取消已请求”，不保证业务副作用一定可以撤销。

只有 Runner 完成协作式取消并成功提交终态后：

```text
status = cancelled
```

### 8.4 Terminal Write 的并发规则

终态更新必须使用条件更新 / Compare-And-Set：

- 必须匹配当前 Task Revision；
- Running Attempt 必须匹配当前 `leaseToken`；
- 已进入终态的记录不可被覆盖；
- Lease 已过期并被回收的旧 Runner 不得提交 Success / Failed。

这条规则用于阻止“僵尸 Runner”在恢复任务已经重新执行后覆盖新状态。

---

## 9. Enqueue 语义

### 9.1 Durable Before Acknowledge

任何 API / Domain Service 对外返回“任务已经创建”之前，必须先完成 Task 的持久化提交。

错误：

```text
HTTP 202
  ↓
setTimeout / in-memory queue
  ↓ process crash
Task lost
```

正确：

```text
validate + authorize
  ↓
DB transaction insert BackgroundTask
  ↓ commit
return Task ID / 202 Accepted
  ↓
Dispatcher observes persisted task
```

如果持久化失败，调用方得到同步错误，不能返回虚假的 Accepted。

### 9.2 Idempotency Key

调用方可以提供 `idempotencyKey`。

建议唯一范围：

```text
(instanceId, type, idempotencyKey)
```

在记录仍处于保留周期内时，同一个 Key 重复 Enqueue 应返回已有 Task，而不是创建第二个逻辑 Task。

调用方如果明确要求“再执行一次”，必须使用新的 Idempotency Key 或人工 Retry / Run Now 语义。

### 9.3 Transaction Boundary

若业务域需要在“创建业务记录”和“创建 Background Task”之间达到可靠一致性，不应依赖内存 Event Bus。

优先顺序：

1. 同一 Owner / 同一事务边界可直接原子提交时，使用事务；
2. 跨 Schema Owner 时，通过正式 Service + Outbox / Durable Intent 等可靠机制衔接；
3. Event Bus 只做提交后的通知和刷新，不做唯一可靠来源。

---

## 10. Handler Registry 与执行契约

### 10.1 Registry

V2 不使用巨大 `switch(task.type)`。

所有 Task Handler 必须通过 Registry 注册：

```text
TaskHandlerRegistry
  ├── media.scan@1
  ├── search.rebuild@2
  ├── automation.run@1
  ├── analytics.rebuild@1
  └── ...
```

Registry 至少按：

```text
(type, handlerContractVersion)
```

解析 Handler。

### 10.2 Handler Definition

推荐 Handler 声明：

```text
TaskHandlerDefinition
├── type
├── supportedContractVersions
├── payloadSchema
├── resultSchema
├── defaultPriority
├── resourceClass
├── defaultTimeout
├── retryPolicy
├── recoveryPolicy
├── idempotencyRequirement
├── cancellationMode
├── concurrencyPolicy
└── sensitivityPolicy
```

### 10.3 Handler Context

执行上下文至少提供：

```text
TaskExecutionContext
├── taskId
├── attemptId
├── instanceId
├── principal / system execution context
├── traceId / requestId
├── cancellationToken
├── progressReporter
├── currentAttempt
├── maxAttempts
├── leaseGuard
└── clock / applicationTimeZone accessor
```

Handler 不应拿到 Task Repository 并自行修改状态。

### 10.4 Handler 返回

Handler 只返回：

- 成功 Result；
- 标准化可分类错误；
- 协作式 Cancelled 信号。

Task Runtime 负责最终写入 Task / Attempt 状态。

### 10.5 Handler 不得执行的行为

禁止：

- 自己把 Task Row 改成 Success；
- 自己创建无限递归 Retry；
- 把 Access Token / Password / Private Key 写入 Progress；
- 启动无法追踪的 Detached Promise / Timer 后立即返回 Success；
- 在失去 Lease 后继续提交任务终态；
- 绕过 Instance / Authorization 直接访问其他域数据。

---

## 11. Claim、Lease 与 Heartbeat

### 11.1 Claim 条件

Dispatcher 只能认领满足以下条件的 Task：

```text
status = pending
AND scheduledAt <= now
AND (nextAttemptAt IS NULL OR nextAttemptAt <= now)
AND instanceId = current execution instance
AND handler exists
AND resource / concurrency permits
```

### 11.2 原子认领

认领必须依赖数据库原子条件更新，而不能仅依赖：

```text
SELECT pending rows
→ application checks
→ UPDATE later
```

因为多个执行循环可能同时看到同一行。

认领成功后至少写入：

- `status = running`
- `leaseOwner = runnerId`
- 新 `leaseToken`
- `leaseExpiresAt`
- `heartbeatAt`
- `attempt += 1`
- `startedAt`（仅首次为空时）
- `revision += 1`

并创建 Task Attempt。

### 11.3 Lease Token

每次 Attempt 使用新的不可预测 Lease Token。

Runner Finalize 时必须同时匹配：

```text
taskId
status = running
leaseToken = expected token
```

如果匹配失败，Runner 必须停止最终写入，并把结果视为“执行权已经丢失”。

### 11.4 Heartbeat

长任务定期续租：

```text
heartbeatAt = now
leaseExpiresAt = now + leaseDuration
```

Heartbeat 周期和 Lease Duration 是运行时配置，不写死在业务 Handler。

### 11.5 Lease 不等于 Exactly Once

Lease 只能保护运行时状态认领，无法保证外部副作用 Exactly Once。

例如 Runner 在：

```text
调用外部 API 成功
↓
还没写 Task success
↓
进程崩溃
```

恢复后仍可能再次调用外部 API。

因此涉及副作用的 Handler 必须结合：

- 业务幂等键；
- Upsert / Unique Constraint；
- 外部 API Idempotency Key；
- Checkpoint；
- Domain-side dedupe；
- 事务性结果提交。

V2 对任务执行语义按 **at-least-once capable** 设计，不宣称通用 Exactly Once。

---

## 12. Retry Policy

### 12.1 Policy 来源

Retry Policy 默认由 Handler Definition 声明，可在明确受控的 Scheduled Job / Enqueue 参数中做有限覆盖。

至少包括：

```text
maxAttempts
backoffStrategy
baseDelay
maxDelay
jitter
retryableErrorCodes / classifier
```

### 12.2 推荐退避

对网络、限流、外部服务不可用等瞬态错误，默认使用指数退避并加入 Jitter。

禁止大量任务在固定时间间隔整齐重试，形成 Thundering Herd。

### 12.3 不可重试错误

以下类型通常不可自动重试：

- Payload Schema 不兼容；
- 当前用户已无权限；
- 目标资源已被永久删除且业务不允许忽略；
- Handler Contract Version 不支持；
- 明确的业务校验失败；
- Secret / Credential 配置缺失且需要人工修复；
- Handler 声明为非幂等且无法确认副作用状态。

实际 `retryable` 由 Handler / Runtime 的标准错误分类决定。

### 12.4 Retry 与权限

自动 Retry 不意味着沿用一个永久有效的权限快照。

每次 Attempt 在执行需要受保护资源的动作前，仍按本文授权规则获取有效执行权限。

如果权限已经被撤销，Task 应以非重试型授权错误终结，而不是无限重试。

---

## 13. Cancellation 与 Timeout

### 13.1 Pending Task 取消

对尚未运行的 Pending Task，可原子执行：

```text
pending -> cancelled
cancelRequestedAt = now
finishedAt = now
```

Dispatcher 之后不能再认领该 Task。

### 13.2 Running Task 取消

流程：

```text
Cancel API
  ↓ authorize
set cancelRequestedAt
  ↓
Runner observes token
  ↓
Handler reaches cancellable checkpoint
  ↓
stop new side effects / cleanup
  ↓
Task -> cancelled
```

### 13.3 Cancellation Mode

Handler 应声明：

```text
COOPERATIVE
CHECKPOINT_ONLY
NON_CANCELLABLE_AFTER_COMMIT
```

对于已经跨过不可逆 Commit Point 的任务，取消请求可能无法改变最终 `success`。

因此 API 必须区分：

- “取消请求已接受”；
- “任务已经取消”；
- “任务已经进入不可取消阶段 / 已终结”。

不能在收到请求的瞬间对 Running Task 向用户谎报 `cancelled`。

### 13.4 Timeout

Timeout 是运行时对 Attempt 的限制，不等同于用户取消。

Timeout 时：

1. Runtime 触发 Cancellation Token；
2. 记录标准错误 `TASK_TIMEOUT` 或 Handler 映射后的错误；
3. 根据 Retry Policy 决定回到 Pending 还是最终 Failed；
4. 如果 Handler 无法及时退出，Lease 仍负责隔离旧 Runner 的最终写入。

Timeout 不能通过粗暴 `kill` 破坏同一 Ikaros 进程中的其他请求。

---

## 14. Recovery 与应用生命周期

### 14.1 启动顺序

推荐：

```text
load config / application timezone
  ↓
initialize DB + migrations
  ↓
register Task Handlers
  ↓
recover expired task leases
  ↓
recalculate / recover scheduler state
  ↓
start Dispatcher
  ↓
start Scheduler tick
```

这样可以避免 Scheduler 创建了当前版本无法执行的任务后才发现 Handler 未注册。

### 14.2 Pending Recovery

Pending Task 本身已经持久化，应用重启后只需重新被 Dispatcher 发现。

不需要“把数据库重新灌进某个唯一内存队列”才能恢复正确性。

内存结构只用于加速，不是 Source of Truth。

### 14.3 Expired Running Recovery

Recovery Service 发现：

```text
status = running
AND leaseExpiresAt < now
```

时，根据 Handler Recovery Policy 处理。

建议策略：

```text
RETRY_SAFE
MANUAL_REVIEW
FAIL
```

- `RETRY_SAFE`：把本次 Attempt 记为 `lease_lost`，Task 回到 Pending，并按 Retry Policy 进入下一 Attempt；
- `MANUAL_REVIEW`：Task 最终 Failed，提示人工确认外部副作用后再 Retry；
- `FAIL`：不自动恢复，直接 Failed。

V2 不提供通用“从任意代码行 Resume”。需要 Resume 的业务必须自行设计 Checkpoint，Handler 读取 Checkpoint 后从安全位置继续。

### 14.4 Graceful Shutdown

应用优雅关闭时：

1. 停止新 Scheduler Materialization；
2. 停止 Dispatcher 认领新 Task；
3. 允许已运行 Handler 在 Grace Period 内完成；
4. 继续为仍受控的 Task 续租；
5. 到期仍未完成的 Attempt 不伪装为 Cancelled；
6. 进程退出后由 Lease Expiry + Recovery Policy 接管。

系统重启不是“用户取消”。

---

## 15. Resource Class、并发与公平性

### 15.1 多层限流

Dispatcher 应支持至少四层约束：

```text
Global concurrency
  ↓
Resource Class concurrency
  ↓
Task Type concurrency
  ↓
Concurrency Key
```

### 15.2 Resource Class

`resourceClass` 是 Handler 声明的稳定逻辑类别，例如：

```text
default
cpu
io
network
external
```

业务可注册更专门的类别，但不应让每个 Task 动态创造高基数 Resource Class。

Resource Class 的目的是避免：

- 大量转码吃满 CPU 后 Admin 操作完全无法执行；
- 大量外部 API 调用同时触发限流；
- 大量 I/O 扫描压垮存储。

### 15.3 Concurrency Key

对于同一目标不能并发执行的任务，使用稳定 `concurrencyKey`：

```text
search-index:main
media-library:{libraryId}
automation:{automationId}
```

相同 Key 同时只允许一个 Running Task。

它是运行时互斥约束，不替代业务数据库唯一约束和幂等设计。

### 15.4 公平调度

Dispatcher 不应简单永久执行：

```text
ORDER BY priority DESC LIMIT N
```

否则持续高优先级流量可能造成饥饿。

实现应提供 Aging、Weighted Fair Queue 或等价机制，并把具体参数作为 Platform Operations 配置，而不是业务 Task Payload。

---

## 16. Scheduled Job 模型

### 16.1 逻辑结构

```text
ScheduledJob
├── id: UUIDv7
├── instanceId: UUIDv7
├── key?: string
├── name: string
├── enabled: boolean
├── taskType: string
├── handlerContractVersion: string
├── taskPayloadTemplate: json
├── payloadSensitivity: L0 | L1 | L2 | L3
├── priority?: integer
├── scheduleKind: ONCE | CRON | FIXED_INTERVAL
├── scheduleExpression: json/string
├── timeZone?: IANA zone
├── misfirePolicy
├── maxCatchUpRuns?
├── overlapPolicy
├── concurrencyKey?: string
├── nextRunAt?: zoned instant
├── lastMaterializedAt?: zoned instant
├── createdByPrincipalId?: UUIDv7
├── updatedByPrincipalId?: UUIDv7
├── revision: integer
├── createdAt
└── updatedAt
```

### 16.2 Schedule Kind

#### ONCE

一次性计划，使用绝对 `runAt`。

完成物化后不再产生未来触发。

#### CRON

使用 Cron Expression + Time Zone 表达墙上时间规则。

例如“每天上午 09:00”属于 CRON / Calendar Semantics，不应先把“09:00”永久换算成某个固定 UTC Offset。

#### FIXED_INTERVAL

从 Anchor Instant 按固定 Duration 推进。

适合“每 30 分钟一次”，不应混入 Cron 的日历语义。

### 16.3 应用默认时区

当 Scheduled Job 没有显式 `timeZone` 时：

```text
Effective Time Zone = Application Default Time Zone
```

V2 默认应用时区为 **UTC+8**，但该值可配置。

为了保证历史可解释性，Scheduler 在创建 / 更新 CRON Job 时应记录其最终使用的 Time Zone 或明确记录“follow application default”的策略。

推荐区分：

```text
timeZoneMode = FIXED | APPLICATION_DEFAULT
resolvedTimeZone
```

如果使用 `APPLICATION_DEFAULT`，应用默认时区变化后必须重新计算未来 `nextRunAt`，但不得篡改历史 Scheduled Job Run 的 `scheduledFor`。

### 16.4 DST

对于存在 DST 的 IANA Time Zone，Cron Evaluator 必须使用 Zoned Calendar 语义。

V2 建议确定以下规则：

- 春季跳时导致某个本地时间不存在：不凭空补出一个错误 Offset；按 Misfire / 下一合法匹配点规则处理；
- 秋季回拨导致本地时间重复：同一 Cron Wall-clock Occurrence 默认只物化一次；
- `scheduledFor` 始终保存最终解析后的带时区 Instant；
- DST 行为必须有自动化测试，不能依赖宿主机 Local Time Zone 的隐式行为。

---

## 17. Scheduled Job Run 与物化幂等

### 17.1 逻辑模型

```text
ScheduledJobRun
├── id: UUIDv7
├── scheduledJobId: UUIDv7
├── instanceId: UUIDv7
├── triggerKind: SCHEDULED | CATCH_UP | MANUAL
├── occurrenceKey: string
├── scheduledFor?: zoned instant
├── scheduleRevision: integer
├── materializedAt?: zoned instant
├── materializationOutcome:
│     ENQUEUED
│   | SKIPPED_MISFIRE
│   | SKIPPED_OVERLAP
│   | FAILED_TO_ENQUEUE
├── backgroundTaskId?: UUIDv7
├── reason?: StandardError / structured reason
└── createdAt
```

### 17.2 Occurrence Key

对正常计划触发，必须生成确定性的 Occurrence Key。

核心唯一语义：

```text
(scheduledJobId, scheduledFor)
```

同一个 Job 的同一个理论触发时间只能成功物化一次。

即使：

- Scheduler Tick 重复；
- 应用在创建 Task 后崩溃；
- 两个调度循环同时扫描到同一 Due Job；

也不能产生第二个逻辑 Scheduled Occurrence。

人工 `Run Now` 使用独立 UUIDv7 Occurrence Key，不与原计划时间冲突。

### 17.3 物化事务

推荐单次物化在同一 Owner 事务中完成：

```text
claim due ScheduledJob
  ↓
insert ScheduledJobRun with unique occurrence
  ↓
insert BackgroundTask
  ↓
link run.backgroundTaskId
  ↓
advance ScheduledJob.nextRunAt
  ↓
commit
```

如果同一物理数据库事务无法一次完成，也必须使用等价 Durable Intent / Outbox 保证恢复后可完成，而不能出现“nextRunAt 已前移但 Task 永久没创建”的静默丢失。

---

## 18. Misfire Policy

Misfire 指理论触发时间已经过去，但系统当时没有成功物化执行，例如：

- Ikaros 长时间离线；
- Scheduler 被暂停；
- 系统升级；
- 资源故障。

V2 支持至少：

### 18.1 `SKIP`

跳过所有已经错过的 occurrence，直接计算下一个未来触发点。

适合高频且只关心最新状态的工作。

### 18.2 `FIRE_ONCE`

无论错过多少次，只补一次执行，并记录它代表的最近 / 聚合 Missed Window。

这是普通维护类任务的推荐默认策略。

### 18.3 `CATCH_UP`

按时间顺序补历史 occurrence，但必须设置 `maxCatchUpRuns` 或平台上限。

禁止系统离线一个月后无上限创建几十万条任务。

超过上限的 occurrence 必须在 Scheduled Job Run / Operations 中留下结构化 skipped / truncated 诊断信息。

---

## 19. Overlap Policy

Scheduled Job 的“重复触发”与 Background Task 的“并发执行”需要显式策略。

V2 建议支持：

### 19.1 `ALLOW`

每个 occurrence 都物化 Task，允许它们并发运行，仍受全局 Resource Class 限制。

### 19.2 `SERIALIZE`

每个 occurrence 都物化 Task，但使用同一 Concurrency Key 排队串行执行。

适合不能并发、但不能丢 occurrence 的任务。

### 19.3 `SKIP_IF_RUNNING`

如果前一次 Job 产生的 Task 仍是 Pending / Running，则当前 occurrence 只创建 Scheduled Job Run 并标记 `SKIPPED_OVERLAP`，不创建 Background Task。

V2 不把“自动取消前一个任务并替换”为通用默认策略，因为取消是协作式的，无法保证瞬间停止副作用。需要 Replace 语义的业务必须由专门业务设计明确承担风险。

---

## 20. Scheduler Tick 与 Due Job Claim

### 20.1 Scheduler 不是 `setInterval` 真相源

可以使用 Timer 唤醒 Scheduler，但 Timer 只负责“提醒去数据库检查”。

正确语义：

```text
Timer wake-up
  ↓
query persisted due jobs
  ↓
claim / materialize transactionally
```

Timer 丢失、事件循环延迟或进程重启都不能让调度事实永久消失。

### 20.2 Claim

Due Job 也需要短 Lease / Claim 或等价数据库互斥，避免两个调度循环同时更新 `nextRunAt`。

即使当前 System Overview 以单进程运行时为主，仍应防御：

- 重入 Tick；
- 上一次 Tick 尚未完成；
- 开发 / 运维误启动重复 Runner；
- 未来执行模型演进。

### 20.3 Scheduler Lag

记录：

```text
schedulerLag = materializedAt - scheduledFor
```

用于区分：

- Task 执行慢；
- Scheduler 自己没及时物化；
- 系统离线造成 Misfire。

---

## 21. Instance 边界

### 21.1 Task 必须属于明确 Instance

V2 Background Task 的 `instanceId` 是必填运行边界。

规则：

- Dispatcher 只执行自己当前服务上下文允许的 Instance Task；
- Task Payload 引用的业务对象必须属于同一 Instance，除非该业务的 System Overview 级契约明确支持跨 Instance；
- Scheduled Job 与其生成的 Background Task 必须保持相同 `instanceId`；
- Scheduled Job Run 必须记录同一 `instanceId`；
- Idempotency Key / Concurrency Key 的唯一与限流语义至少包含 Instance 范围；
- Admin 查询跨 Instance 时必须显式拥有跨 Instance 管理权限。

### 21.2 不使用 `NULL instanceId` 逃逸边界

普通业务任务不得通过 `instanceId = NULL` 声称“全局”从而访问所有 Instance。

真正的平台进程级维护行为如果未来需要公开成 Background Task，应由 System Overview / Platform Operations 明确定义 Global Scope 契约后再扩展。

在该契约出现前，不能私自把 `NULL` 当万能 Global Instance。

### 21.3 跨 Instance 工作

需要跨 Instance 导入 / 导出 / 同步时：

- 每个 Instance 内的 Task 仍独立；
- 通过显式 Export / Import / Sync Contract 交换数据；
- 不把一个 Task Row 从 A Instance“搬到”B Instance；
- 不允许 A Instance 的 Handler 直接写 B Instance 业务表。

---

## 22. Authorization 与执行身份

### 22.1 Enqueue 时授权

用户发起长任务时，Domain API 必须先验证：

- 用户是否可以发起该操作；
- 目标资源是否属于当前 Instance；
- Task Payload 是否只包含允许持久化的引用 / 参数。

通过后才创建 Task。

### 22.2 执行时重新授权

后台排队可能持续较长时间。

因此用户发起的任务不能把 Enqueue 时的权限快照当成永久授权票据。

推荐：

```text
Task.authorizationContext
= initiator identity + requested action + audit metadata
```

它用于：

- 审计；
- 解释任务来源；
- 重新构造授权检查上下文。

真正读取 / 修改当前受保护资源前，Handler / Domain Service 应按当前权限再次授权。

如果权限已撤销：

```text
Task -> failed
error.code = AUTHORIZATION_REVOKED / domain equivalent
retryable = false
```

### 22.3 System Principal

Scheduled maintenance、系统恢复等非用户直接触发任务使用明确 `SYSTEM` Execution Principal。

SYSTEM 不是“绕过所有 ACL 的字符串”。它必须由 Platform Security 定义其 Capability，并由 Handler 调用正式 Domain Service。

### 22.4 禁止保存 Credential

Task 中不得保存：

- Session Token；
- Access Token；
- Refresh Token；
- Password；
- Client Secret；
- Private Key；
- Recovery Code；
- Secure Session 解锁密钥。

需要 Credential 时保存 Stable Reference，在 Attempt 执行时通过授权后的 Secret / Credential Service 解析。

---

## 23. 数据敏感等级与 Secure Data Boundary

### 23.1 L0 / L1 / L2 / L3

Task Payload / Result / Error 必须继承 System Overview 的数据敏感分级。

- **L0**：公开 / 低敏感运行参数；
- **L1**：普通内部业务参数；
- **L2**：用户私有元数据、路径、配置、受保护对象引用等；
- **L3**：Secret、Credential、密钥材料等最高敏感数据。

Task 的 `payloadSensitivity` 记录该 Payload 的最高等级，用于：

- API 字段过滤；
- Operations 权限；
- 日志脱敏；
- Retention；
- 导出诊断包。

### 23.2 L3 的特殊规则

**L3 明文禁止持久化进入 Background Task Payload / Result / Error / Progress。**

如果业务参数需要使用 L3：

```text
Task Payload
  ↓ contains only
secretRef / credentialRef / secureObjectRef
  ↓ runtime authorized resolution
Secret Service / Secure Data Foundation
```

### 23.3 Secure Domain

对于 `USER_LOCKED_E2EE` 等 Secure Domain：

- Background Task 不得把解密后的私密正文放入 Payload；
- 服务端无解密权限时，服务端 Background Task 只能处理密文或非敏感元数据；
- 需要客户端明文执行的工作应留在受信客户端 Secure Runtime；
- Event / Trace / Notification 同样只传播安全引用。

### 23.4 Error Redaction

错误 `details` 在持久化前必须经过 Redaction。

禁止把原始异常对象无筛选序列化到 Task Error，因为底层 SDK 异常可能包含：

- Authorization Header；
- URL Credential；
- 文件内容；
- Prompt；
- SQL 参数；
- Secret。

---

## 24. Error Contract

### 24.1 标准结构

```json
{
  "code": "DEPENDENCY_UNAVAILABLE",
  "message": "External provider is unavailable",
  "details": {
    "providerId": "..."
  },
  "retryable": true
}
```

### 24.2 错误类别

运行时至少应能区分：

- Payload / Contract Validation；
- Authorization；
- Resource Missing / Conflict；
- Rate Limit；
- Dependency Unavailable；
- Timeout；
- Transient I/O；
- Handler Missing / Version Unsupported；
- Lease Lost；
- Internal Runtime Error。

具体业务错误码由业务域定义，但必须转换到统一 Error Envelope。

### 24.3 同步 API Error 与异步 Task Error

两者不能混淆：

- Enqueue API 自身失败：按 V2 同步 API 错误契约返回；
- Enqueue 成功后业务执行失败：HTTP 已经完成，错误必须写入 Background Task 的 `error` 并进入 `failed`。

禁止后台失败后假装原始 HTTP 请求仍可返回 500。

---

## 25. Scheduled Job 管理语义

### 25.1 Create / Update

创建 / 修改 Job 时必须：

1. 校验 Task Type + Handler Contract Version 存在；
2. 校验 Payload Template；
3. 校验 Schedule Expression；
4. 解析 Time Zone；
5. 校验当前 Principal 有权创建该类任务；
6. 计算 `nextRunAt`；
7. 持久化 Job Revision。

### 25.2 Disable

`enabled = false`：

- 停止未来新的 Scheduled Occurrence；
- 不删除历史 Scheduled Job Run；
- 不自动取消已经物化的 Background Task。

如果管理员希望同时取消已有 Task，必须执行独立 Cancel Action，并接受取消不是瞬时保证的语义。

### 25.3 Delete / Archive

推荐 Scheduled Job 使用可审计 Archive / Soft Delete 或保留必要历史元数据。

删除计划不能级联删除已产生 Task 的历史事实。

### 25.4 Run Now

`Run Now`：

- 创建 `triggerKind = MANUAL` 的 Scheduled Job Run；
- 创建新的 Background Task；
- 默认不改变原计划 `nextRunAt`；
- 受同样权限、Instance、Payload、Retry、Concurrency 约束。

### 25.5 修改计划时的历史

修改 Cron、Time Zone、Payload Template 后：

- `revision += 1`；
- 重算未来 `nextRunAt`；
- 已经产生的 Scheduled Job Run 继续保留其 `scheduleRevision`；
- 旧 Background Task 不被改写。

---

## 26. API 资源建议

具体 URI 命名最终应与 V2 API Convention 保持统一；在当前上位文档约束下，本文定义资源语义而不绑定未来公共索引文件。

### 26.1 Background Task Admin / Read API

建议能力：

```text
GET  /api/background-tasks
GET  /api/background-tasks/{taskId}
POST /api/background-tasks/{taskId}/cancel
POST /api/background-tasks/{taskId}/retry
```

筛选至少支持：

- `instanceId`
- `type`
- `status`
- `resourceClass`
- `originType / originId`
- 时间范围

默认不向无权用户暴露 L2 Payload / Result。

### 26.2 Scheduled Job Admin API

建议能力：

```text
GET    /api/scheduled-jobs
POST   /api/scheduled-jobs
GET    /api/scheduled-jobs/{jobId}
PATCH  /api/scheduled-jobs/{jobId}
POST   /api/scheduled-jobs/{jobId}/enable
POST   /api/scheduled-jobs/{jobId}/disable
POST   /api/scheduled-jobs/{jobId}/run-now
GET    /api/scheduled-jobs/{jobId}/runs
```

是否使用 Action Subresource、RPC-style Action 或其他统一风格，后续由 API Convention 最终规范；行为语义以本文为准。

### 26.3 不开放任意 Task 执行

普通客户端不应获得：

```text
POST /background-tasks { type: arbitraryString, payload: arbitraryJson }
```

来直接调用所有内部 Handler。

业务 API 应调用 TaskService 创建经过授权的已注册 Task。

只有明确的 Admin / Internal Capability 可以使用受控 Generic Enqueue，并且仍必须通过 Registry Schema + Permission 校验。

### 26.4 202 Accepted

长耗时业务 API 推荐返回：

- `202 Accepted`；
- Task ID；
- Task Resource Link / Reference。

前提仍是 Task 已经 durable commit。

---

## 27. Event 与实时更新

### 27.1 Source of Truth

Background Task DB 是任务状态真相源。

Event / WebSocket / SSE 可以用于：

- 通知 UI 刷新；
- 推送 Progress；
- 触发非关键统计。

但订阅者丢事件后必须能够重新 GET Task 恢复当前状态。

### 27.2 Revision

Task 每次持久化状态变化递增 `revision`。

实时客户端可用：

```text
(taskId, revision)
```

丢弃乱序旧事件。

### 27.3 At-least-once

Task State Changed Event 按 at-least-once 消费思路设计，Consumer 应根据 `taskId + revision` 幂等。

需要跨进程 / 外部系统的关键可靠通知时使用 Outbox / Integration 契约，不能只依赖内存 Event Bus。

---

## 28. 可观测性

### 28.1 Metrics

至少建议采集：

```text
background_task_pending_count
background_task_running_count
background_task_oldest_pending_age
background_task_dispatch_latency
background_task_run_duration
background_task_success_total
background_task_failed_total
background_task_cancelled_total
background_task_retry_total
background_task_lease_expired_total
background_task_timeout_total
scheduler_due_count
scheduler_lag
scheduler_misfire_total
scheduler_materialization_failed_total
```

标签必须控制基数。

推荐低基数维度：

- Task Type；
- Resource Class；
- Status；
- Error Category。

不要直接把：

- Task ID；
- User ID；
- 文件路径；
- URL；
- Prompt；
- Secret Ref；

作为 Metrics Label。

### 28.2 Log Correlation

日志上下文推荐包含：

```text
requestId
traceId
taskId
attemptId
runnerId
instanceId
pluginId? / domain correlation id?
```

但日志中不得打印 L3 Secret，L2 字段遵守 Redaction Policy。

### 28.3 Operations 诊断

任务详情页 / 诊断 API 应能区分：

- 排队等待；
- 因 Backoff 等待；
- 因 Resource Class 饱和等待；
- 因 Concurrency Key 等待；
- Running；
- Cancel Requested；
- Lease Lost Recovery；
- Scheduler Misfire；
- Handler Missing；
- Final Failure。

不能把所有情况都只显示成一个模糊“等待中”。

---

## 29. Retention 与清理

Background Task / Attempt / Scheduled Job Run 会持续增长，需要 Platform Operations 统一 Retention。

规则：

- 非终态 Task 永远不能被 Retention 删除；
- Scheduled Job 当前定义不能因为历史清理丢失；
- 终态 Task 元数据、Attempt、Payload、Result 可以有不同保留周期；
- L2 Payload / Result 可配置比普通元数据更短的保留周期；
- L3 明文本来就不允许存在；
- 已被 Audit / Incident / Compliance Pin 的记录不能被普通清理任务删除；
- 清理本身也应作为受控 Background Task 运行；
- 删除大批历史记录必须批处理，避免长事务阻塞运行时。

不在本文硬编码具体保留天数，由 Operations Policy 决定。

---

## 30. 数据库约束与索引建议

具体物理 Schema 名称由 Platform Administration / Operations Owner 定义。建议至少存在以下逻辑表：

```text
background_task
background_task_attempt
scheduled_job
scheduled_job_run
```

### 30.1 Background Task

建议索引：

```text
(instance_id, status, next_attempt_at, scheduled_at, priority)
(instance_id, type, status)
(instance_id, resource_class, status)
(instance_id, concurrency_key, status)
(origin_type, origin_id)
(created_at)
```

Idempotency 建议唯一约束：

```text
(instance_id, type, idempotency_key)
WHERE idempotency_key IS NOT NULL
```

如底层数据库对 Partial Unique Index 支持方式不同，由 Schema Owner 采用等价实现。

### 30.2 Attempt

唯一约束：

```text
(task_id, attempt_no)
```

索引：

```text
(task_id, started_at)
```

### 30.3 Scheduled Job

建议索引：

```text
(instance_id, enabled, next_run_at)
(instance_id, key)
```

如果 `key` 用作稳定业务绑定，建议在 Instance 内唯一。

### 30.4 Scheduled Job Run

关键唯一约束：

```text
(scheduled_job_id, occurrence_key)
```

正常 Schedule Occurrence 的 `occurrenceKey` 从 `scheduledFor` 确定性生成。

建议索引：

```text
(scheduled_job_id, scheduled_for)
(instance_id, created_at)
(background_task_id)
```

### 30.5 同 Owner 内 FK

上述四张通用运行时表由同一个 Schema Owner 管理，因此它们之间可以由 Owner 决定使用 FK。

但不能由该 Migration 对 Automation / AI / Analytics 等外域表创建 FK。

---

## 31. 关键一致性场景

### 31.1 Enqueue 后进程立即崩溃

预期：

- Task 已在 DB；
- 重启后 Dispatcher 继续执行；
- 不需要原请求客户端重新提交才能发现任务。

### 31.2 Handler 副作用成功、Finalize 前崩溃

预期：

- Lease 到期；
- Recovery 按 Handler Policy 处理；
- Retry-safe Handler 使用相同业务幂等键避免副作用重复；
- 平台不宣称通用 Exactly Once。

### 31.3 Scheduler 创建 Run 后、创建 Task 前崩溃

预期：

- 单事务时整体回滚；或
- Durable Materialization Intent 在恢复后继续；
- 不能让 Job `nextRunAt` 永久跨过去而没有 Task / Skip 记录。

### 31.4 Task 创建后、Scheduler 更新 nextRunAt 前崩溃

预期：

- 下次 Tick 尝试相同 `occurrenceKey`；
- Unique Constraint 命中已有 occurrence；
- Scheduler 恢复并推进 nextRunAt；
- 不创建重复 Task。

### 31.5 Cancel 与 Success 竞争

预期：

- Pending Cancel 原子终结；
- Running Cancel 先记录 Request；
- Handler 在安全点检查 Token；
- Terminal CAS 不覆盖既有终态；
- 若业务已过不可逆 Commit Point，最终可能 Success，但 UI 必须能够解释“取消请求未能生效”，不能篡改为 Failed。

### 31.6 Lease 过期后旧 Runner 返回

预期：

- 旧 Lease Token Finalize 失败；
- 旧 Runner 不覆盖新 Attempt 状态；
- Telemetry 记录 stale / lease-lost completion；
- Handler 仍需业务幂等防护。

---

## 32. 与典型子系统的集成模式

### 32.1 Automation

```text
Automation Trigger
  ↓ durable AutomationRun intent
BackgroundTask(type=automation.run)
  ↓
Automation Handler
  ↓
Action execution
```

Automation 自身的动作历史、输入 / 输出属于 Automation Schema Owner。

如果 Automation 使用 Schedule Trigger，优先复用 Scheduled Job，而不是再实现一套持久化 Cron Engine。

### 32.2 AI

```text
AI Run business record
  ↓
BackgroundTask(type=ai.run, payload={aiRunId, profileRef})
  ↓
resolve provider credential at runtime
  ↓
AI service
  ↓
AI domain persists output
  ↓
Task result={aiRunId/outputRef}
```

Provider Key 不能进入 Task Payload。

### 32.3 Analytics

大规模重算 / Backfill / Repair：

- 用 Background Task 执行；
- Analytics 自己维护 Checkpoint / Watermark；
- Task Result 只返回 Snapshot / Report 引用；
- Retry Handler 必须保证同一 Window 重算幂等。

### 32.4 Plugin

Plugin Install / Update / Repair 等长操作：

- 通过 Background Task 运行；
- Plugin Host 不充当持久化 Scheduler；
- Plugin Credential 使用引用；
- Task Handler Contract 与 Plugin API Contract 分开版本化；
- Plugin 被禁用 / 权限撤销后，相关 Pending Task 再执行时必须重新检查。

### 32.5 Secure Domain

Secure Data 相关 Server Task：

- 只处理 Ciphertext / Safe Metadata / Secure Reference；
- USER_LOCKED_E2EE 明文不进入服务端队列；
- 需要解密的动作必须在有明确解密权限的 Secure Runtime 中完成。

---

## 33. 配置模型

Platform Operations 应提供运行时配置，至少覆盖：

```text
Task Runtime
├── global concurrency
├── per resource class concurrency
├── per task type concurrency overrides
├── lease duration
├── heartbeat interval
├── dispatch batch size
├── polling / wake-up interval
├── progress persistence throttle
├── graceful shutdown period
├── retry global caps
└── retention policy

Scheduler
├── tick interval
├── due scan batch size
├── materialization concurrency
├── default misfire policy
├── catch-up global cap
└── scheduler lease duration
```

配置变化不得改变已经终态的历史记录。

影响未来语义的设置，例如应用默认时区变化，应触发明确的 Scheduled Job Recalculation，而不是静默继续使用旧 `nextRunAt`。

---

## 34. 安全与运维动作审计

以下动作建议进入 Operation / Audit Log：

- 创建 / 修改 / 禁用 / 启用 Scheduled Job；
- Run Now；
- 人工 Retry；
- Cancel；
- 修改全局任务并发 / Retention / Scheduler 策略；
- 强制解除异常 Lease / 人工 Recovery；
- 查看受限制的 L2 Payload / Result。

Audit 记录应包含：

```text
actor
instanceId
action
taskId / scheduledJobId
result
traceId
occurredAt
```

禁止把完整敏感 Payload 复制进审计日志。

---

## 35. 实现阶段推荐模块边界

在 System Overview 已确定的代码分层下，推荐把共享契约与运行时拆分为：

```text
Core
├── task types / status / error contract
├── handler contract
├── scheduler types
└── policy types

Platform Operations Runtime
├── repositories
├── dispatcher
├── runner
├── lease / recovery
├── scheduler evaluator
├── materializer
└── telemetry

API
├── task query / actions
└── scheduled job management

Business Domains
└── handlers + domain-specific enqueue facade
```

精确目录名由代码库最终模块约定决定；关键要求是 Core 契约、Operations Runtime 与业务 Handler 的依赖方向清晰。

业务 Handler 依赖 Task Execution Contract；Task Runtime 不应反向 import 所有业务模块并通过硬编码判断执行逻辑。

---

## 36. 测试与验收矩阵

### 36.1 Task 状态

必须覆盖：

- Pending -> Running -> Success；
- Pending -> Cancelled；
- Running -> Failed；
- Running -> Cancelled；
- Running retryable -> Pending -> Running -> Success；
- 达到 maxAttempts -> Failed；
- 终态不可重新打开。

### 36.2 Idempotency

必须覆盖：

- 相同 Instance + Type + Idempotency Key 重复请求只产生一个 Task；
- 不同 Instance 同 Key 互不影响；
- Scheduled Job 同 `scheduledFor` 重复 Tick 只物化一次；
- 人工 Retry 产生新 Task，并正确建立 parent / root 链。

### 36.3 Lease / Recovery

必须覆盖：

- Runner Crash；
- Heartbeat Stop；
- Lease Expiry；
- 旧 Runner Late Finalize；
- Recovery RETRY_SAFE；
- Recovery MANUAL_REVIEW；
- Graceful Shutdown。

### 36.4 Scheduler

必须覆盖：

- ONCE；
- CRON；
- FIXED_INTERVAL；
- Application Default Time Zone = UTC+8；
- 自定义 IANA Time Zone；
- 默认时区修改后的 nextRunAt 重算；
- DST Gap；
- DST Overlap；
- Misfire SKIP；
- Misfire FIRE_ONCE；
- Misfire CATCH_UP + Cap；
- Overlap ALLOW；
- SERIALIZE；
- SKIP_IF_RUNNING；
- Run Now 不改变原 nextRunAt。

### 36.5 Security

必须覆盖：

- Enqueue 时无权限；
- Enqueue 后权限被撤销；
- SYSTEM Principal 权限边界；
- L2 Payload 的 API 可见性；
- L3 Secret Payload 被拒绝 / 转为 Secret Ref；
- Error Redaction；
- Secure Domain 明文不进入 Task / Event / Log。

### 36.6 Contract Version

必须覆盖：

- 旧 Handler Contract Version Pending Task 在升级后继续执行；
- Upcaster 正确迁移；
- 不支持版本明确失败；
- API V2 Contract 与 Handler Payload Version 不互相误用。

### 36.7 Schema Ownership

Migration Review 必须确认：

- Background Task / Scheduler Owner 只修改自身表；
- 没有替 Automation / AI / Analytics / Media 等表加列；
- 外域引用不被偷偷建成越权 FK；
- 同 Owner 的 Task / Attempt / Job / JobRun 约束由统一 Migration 管理。

---

## 37. 设计决策摘要

V2 Background Task / Scheduler 的关键决策如下：

1. **数据库中的 Task 是 Source of Truth，内存队列只是执行加速层。**
2. **Scheduled Job、Scheduled Job Run、Background Task、Task Attempt 四种概念严格分离。**
3. **公开异步状态保持 `pending / running / success / failed / cancelled`；取消请求单独记录。**
4. **自动 Retry 留在同一 Task 下形成多个 Attempt；人工 Retry 创建新 Task。**
5. **终态不可逆。**
6. **Task 使用 Claim + Lease + Heartbeat + Lease Token CAS 防止失效 Runner 覆盖状态。**
7. **平台只提供 at-least-once capable 执行基础，业务 Exactly Once 必须依赖幂等 / 唯一约束 / 外部 Idempotency。**
8. **Schedule Occurrence 通过唯一 Occurrence Key 防重复物化。**
9. **Scheduler 支持 ONCE / CRON / FIXED_INTERVAL，Cron 使用显式或应用默认 Time Zone。**
10. **应用默认时区继承 System Overview：UTC+8，可配置；所有持久化时间保持带时区语义。**
11. **Misfire 和 Overlap 必须显式配置，不能靠偶然运行时行为。**
12. **Task / Job / Run 全部服从 Instance 边界，不使用 NULL Instance 作为越权 Global Shortcut。**
13. **用户任务在执行时重新授权；权限快照只能作为审计上下文，不能成为永久凭证。**
14. **L3 Secret 明文永远不能进入 Task Payload / Result / Progress / Error / Log。**
15. **Task Handler 使用 Registry + 独立 Handler Contract Version，不能使用巨大 Switch 或代码类名作为持久契约。**
16. **Platform Administration / Operations 是 Task / Scheduler Schema Owner；业务域只通过服务契约使用它。**
17. **Event 是通知，不是任务真相源；关键可靠衔接使用持久化事务 / Outbox。**
18. **任务中心和运维诊断必须能解释 Backoff、Concurrency、Cancel Requested、Lease Recovery、Misfire 等真实等待原因。**

---

## 38. 验收标准

本专项设计完成后，V2 实现应达到：

- 任意已经返回 Accepted 的 Background Task 在应用重启后仍可查询和恢复；
- 同一 Scheduled Occurrence 在重复 Tick / Crash Recovery 下不会被静默重复物化；
- 自动 Retry、人工 Retry、Scheduled Next Run 在数据上可以明确区分；
- 取消、失败、成功不会互相伪装；
- 旧 Runner 丢失 Lease 后无法覆盖新 Attempt 的最终状态；
- Handler 能声明并执行一致的 Retry、Timeout、Cancellation、Idempotency、Resource Class 和 Concurrency Policy；
- Scheduled Job 的 UTC+8 默认时区、自定义时区和 DST 语义均确定且可测试；
- Instance A 的任务不会因为调度 / Retry / Admin API 自动获得 Instance B 的访问权；
- L3 Secret 不出现在 Task Payload、Result、Progress、Error、Log、Event 中；
- Secure Domain 明文不会因为“后台执行”而突破 Secure Data Boundary；
- Background Task / Scheduler Migration 不修改其他 Schema Owner 的业务表；
- Admin / Automation / AI / Analytics 能共享统一运行时，而不各自重新实现持久化任务队列；
- 系统可以通过 Task / Attempt / Scheduled Job Run / Metrics 清晰解释一次后台操作从“被受理”到“终结”的完整过程。

至此，Background Task / Scheduler 作为 Ikaros V2 的共享异步运行时基础能力完成专项边界定义。