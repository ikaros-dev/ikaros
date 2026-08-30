# Ikaros V2 API Convention

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 API Convention |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 系统级上位约束 | `System-Overview-Design.md` |
| 产品基线 | `Product-Requirements-Document.md` |

> 本文档定义 Ikaros V2 对外 HTTP API、实时接口及其公共契约的统一约定。
>
> `System-Overview-Design.md` 是本文档的系统级上位约束。本文档只负责将其中已经确定的 HTTP-first、UUIDv7、统一时间与时区、Instance 边界、权限、数据敏感等级、契约版本、异步状态、最终一致性等规则细化为可执行的 API 规范，不重新定义这些系统级规则。
>
> 本文档同时参考现有 V2 各专项设计中已经确定的领域边界。若某一业务域对状态机、安全边界、冲突处理、异步执行或数据暴露已有更严格要求，API 必须遵循该领域要求，不得通过“通用 CRUD”绕过。
>
> V2 是重新设计。V1 API、V1 DTO、V1 路径、V1 错误结构和历史客户端行为不构成本文档的兼容性约束。

---

## 1. 文档目标与范围

API Convention 需要解决的是“所有 V2 API 应遵循哪些共同规则”，而不是提前列出所有业务接口。

本文档负责定义：

1. HTTP API 的版本入口、URI、资源与业务动作表达方式。
2. HTTP Method、Status Code、Content Type 与缓存语义。
3. JSON 字段、UUIDv7、时间、时区、金额、枚举、空值等公共数据格式。
4. Authentication、Authorization、Step-up Verification 在接口层的统一行为。
5. Request ID、Trace、Correlation、审计上下文和数据最小化。
6. 列表、分页、排序、筛选与搜索的统一形式。
7. Error Model 与机器可判断的错误 Code。
8. ETag / Revision、并发更新和离线同步冲突约定。
9. Idempotency Key 与安全重试规则。
10. 长耗时业务动作、Background Task 和异步结果的 API 表达。
11. SSE / WebSocket / Streaming 等实时协议的公共边界。
12. Attachment / Blob 上传、下载与 Range Access 的接口原则。
13. Webhook 入口和投递的公共可靠性、安全与幂等约定。
14. API Version、Event Contract Version 等契约的兼容与弃用规则。
15. Capability Discovery 的接口语义。
16. OpenAPI Contract、SDK 生成和 CI 契约检查要求。

本文档不负责定义：

- 每个子系统完整的 endpoint 清单；
- 具体 Java Controller、DTO、Package 或 Handler 类名；
- 具体数据库表、Column、Repository 或 SQL；
- 各业务领域自身的状态机；
- Event Outbox 的物理表设计；
- Authentication Provider 的完整协议实现；
- Web / Flutter 的页面结构；
- Plugin API 的 Java 接口签名；
- 某一 Webhook Provider 的专有字段；
- Secure Domain 的具体密码学算法实现。

这些内容应由对应专项设计继续负责。

---

## 2. 规范用语

本文档使用以下规范强度：

| 用语 | 含义 |
|---|---|
| 必须 / MUST | V2 稳定 API 的强制规则，不满足即视为契约缺陷 |
| 禁止 / MUST NOT | 明确不允许的行为 |
| 应 / SHOULD | 默认必须遵循；偏离时需要有明确领域理由 |
| 可以 / MAY | 可选能力，是否支持由 endpoint 或 Capability 声明 |

同一个领域专项若定义了比本文更严格的限制，应采用更严格规则。

---

## 3. API 设计总原则

### 3.1 API 是产品能力边界，不是数据库 CRUD 镜像

外部 API 必须表达业务语义。

例如，若 Task 完成需要检查依赖、权限、重复任务、Activity、Event 等业务规则，则禁止把完成动作定义成：

```http
PATCH /api/v2/tasks/{task_id}
Content-Type: application/json

{
  "status": "COMPLETED"
}
```

更合适的表达是显式业务动作：

```http
POST /api/v2/tasks/{task_id}/actions/complete
```

调用链逻辑上应保持：

```text
HTTP API
   ↓
Application API
   ↓
Command / Capability
   ↓
Target Subsystem
   ↓
Permission + Validation + Business Rule
```

API 层不得成为绕过领域所有权的“万能写库入口”。

### 3.2 官方客户端与第三方客户端使用同一公开能力

CMS、Flutter App、脚本、插件集成和第三方客户端原则上都应使用公开稳定能力。

禁止长期保留：

- 只有官方 Web 才能调用的关键业务接口；
- 只有 Flutter 才知道的隐藏状态变更入口；
- 绕过正常 Permission / ACL 的内部 HTTP 后门；
- 通过前端菜单可见性代替后端权限校验。

内部模块在模块化单体阶段可以直接调用公开 Command / Capability，不要求通过本机 HTTP 绕一圈。

### 3.3 API 不暴露 Schema Ownership 内部实现

数据库采用单一 PostgreSQL 不意味着 API 可以暴露所有表。

公共契约不得要求调用方理解：

- 某个子系统的表名；
- 内部外键导航；
- ORM Entity；
- 私有 Repository；
- Migration 结构；
- Search / Analytics 的物理表结构。

跨子系统 API 只使用稳定公共身份和公共 DTO。

### 3.4 Instance 是默认 API 边界

一个 API Base URL 默认对应一个 Ikaros Instance。

V2 默认不是 SaaS 多租户系统，因此：

- 不在所有 URI 中机械加入 `{tenant_id}`；
- 不要求普通请求携带 `X-Tenant-ID`；
- 不在公共 DTO 中机械加入 `tenant_id`；
- 多 User 通过 Authentication、RBAC、ACL、Share 等处理；
- Instance 级 Application Timezone、Plugin、Storage 等能力由当前连接的 Instance 提供。

未来若引入真正 Multi-Tenant，必须通过独立 ADR 和新契约设计处理，不能通过偷偷增加一个 Header 改变现有 V2 语义。

---

## 4. API 版本入口与 URI Convention

### 4.1 Major API Version 放入路径

Ikaros V2 稳定 HTTP API 使用：

```text
/api/v2
```

作为 Major API Version 前缀。

示例：

```text
/api/v2/resources
/api/v2/attachments
/api/v2/tasks
/api/v2/analytics/metrics
/api/v2/admin/users
```

Major Version 只在发生无法通过兼容演进解决的公共契约变更时提升。

Server 产品版本、Database Schema Version、Plugin API Version、Event Contract Version、Export Format Version 不得因为 API 路径中出现 `v2` 就被视为同一个版本概念。

### 4.2 URI 命名

统一规则：

- URI Path Segment 使用小写 `kebab-case`；
- 可寻址集合使用复数名词；
- 实体 ID 作为 Path Parameter；
- 不把 Java 类名、数据库表名暴露在 URI；
- 不在 URI 中使用动词表达普通资源读取；
- 业务动作统一放在 `/actions/{action}` 下；
- Query Parameter 使用 `snake_case`。

示例：

```text
GET  /api/v2/resources/{resource_id}
GET  /api/v2/background-tasks/{task_id}
POST /api/v2/resources/{resource_id}/actions/archive
POST /api/v2/background-tasks/{task_id}/actions/cancel
```

### 4.3 Admin API 独立命名空间

平台管理能力使用明确的 Admin Namespace，例如：

```text
/api/v2/admin/...
```

这样做只用于表达产品与权限边界，不表示部署成独立服务。

能够访问普通 Resource API 的 Principal 不自动获得 Admin API 权限。Admin API 每次请求仍必须进行独立 Authentication / Authorization / Step-up 判断。

### 4.4 URI 不包含展示状态

禁止将易变化的状态直接编码为资源身份，例如：

```text
/api/v2/completed-tasks/{id}
```

如果 Completed 只是 Task 的查询视图，应使用：

```text
/api/v2/tasks?status=COMPLETED
```

或对应领域定义的 Smart View / Saved Query。

---

## 5. HTTP Method Convention

| Method | 主要语义 | 约束 |
|---|---|---|
| GET | 读取资源、列表、查询 | Safe，不产生业务状态变更 |
| POST | 创建资源或执行显式业务动作 | 可非幂等；需要时配合 `Idempotency-Key` |
| PUT | 完整替换一个已知资源 | 仅在确有完整替换语义时使用，默认不作为普通更新手段 |
| PATCH | 部分修改可直接编辑的属性 | 不能代替有业务含义的 Command |
| DELETE | 删除当前 URI 所代表资源 | 只在删除语义清晰时使用；高风险永久清理优先显式 Action |
| HEAD | 获取元信息 | 可用于 Blob / Attachment、缓存或可用性检查 |
| OPTIONS | 协议能力 / CORS | 不承担业务 Capability Discovery |

### 5.1 创建资源

创建成功：

```http
HTTP/1.1 201 Created
Location: /api/v2/resources/019...
```

响应可以返回创建后的完整 Representation。

### 5.2 Partial Update

默认 Partial Update 使用：

```http
Content-Type: application/merge-patch+json
```

并明确区分：

```text
字段未出现
→ 不修改

字段显式为 null
→ 仅当该字段契约允许清空时表示清空
```

如果某字段的 `null` 本身是独立业务值，或者更新过程需要复杂操作顺序，应使用专用 Command / Action 或更适合的 Patch Contract，不能制造歧义。

### 5.3 业务状态迁移必须显式表达

以下动作通常不应通过任意 PATCH 修改状态完成：

- Complete / Cancel Task；
- Publish Article；
- Archive / Restore Resource；
- Restore Blob；
- Permanent Purge；
- Reconcile Account；
- Enable / Disable 高风险 Provider；
- Reset Credential / Key；
- Plaintext Secure Export。

这些动作应进入目标子系统 Command，并拥有独立权限、校验、审计和必要的 Step-up Verification。

---

## 6. Content Type 与 Representation

### 6.1 JSON

普通结构化 API 使用：

```http
Content-Type: application/json; charset=utf-8
Accept: application/json
```

错误统一使用：

```text
application/problem+json
```

SSE 使用：

```text
text/event-stream
```

二进制上传下载使用对应实际 Media Type，不把大型二进制 Base64 塞进普通 JSON。

### 6.2 JSON 字段命名

公共 JSON 字段统一使用 `snake_case`：

```json
{
  "resource_id": "019c...",
  "created_at": "2026-08-30T17:07:27Z",
  "updated_at": "2026-08-31T00:10:00+08:00"
}
```

### 6.3 公共字段后缀

推荐统一：

| 语义 | 命名 |
|---|---|
| 实体引用 | `*_id` |
| 时间点 | `*_at` |
| 纯日期 | `*_date` |
| 数量 | `*_count` |
| 字节 | `*_bytes` |
| 毫秒 | `*_ms` |
| 秒 | `*_seconds` |
| Version / Revision | 明确命名 `version` / `revision`，不得混用 |

不得使用没有单位的模糊字段，例如 `duration: 10`，除非该 Schema 明确定义其单位且不会产生歧义。

---

## 7. UUIDv7 与实体身份

### 7.1 Wire Format

Ikaros V2 内部持久化实体的 UUIDv7 在 HTTP / JSON 中以标准 UUID 字符串传输：

```json
{
  "id": "019c5f1a-7a2b-7abc-8def-0123456789ab"
}
```

规则：

- 不转成数字；
- 不使用第三方 Provider ID 代替；
- 不为 API 额外生成自增数字 ID；
- OpenAPI Schema 使用 `type: string` + `format: uuid`；
- Path Parameter 同样使用 UUID 字符串；
- 服务端必须验证 UUID 格式以及目标实体类型。

### 7.2 UUIDv7 是 Opaque Identity

尽管 UUIDv7 具有时间有序特性，API 调用方必须把它视为不透明身份。

禁止：

- 从 UUIDv7 推导正式 `created_at`；
- 使用 ID 大小作为业务时间比较；
- 使用 `ORDER BY id` 代替正式排序字段；
- 把 UUIDv7 内嵌时间作为 Analytics 时间事实。

列表排序需要创建时间时，必须显式使用 `created_at`，并使用 `id` 作为稳定 Tie-breaker，而不是反过来。

### 7.3 外部身份与内容身份分离

公共契约必须区分：

```text
Ikaros Entity ID
→ UUIDv7

External Provider Identity
→ provider + external_id

Blob Content Identity
→ cryptographic hash
```

三者不得在同一个 `id` 字段中混用。

---

## 8. 时间、时区与日期格式

### 8.1 时间点统一使用 RFC 3339

所有表示真实发生时刻、状态变化时刻、计划执行时刻或可比较时间点的字段，必须显式携带 Offset 或 `Z`：

```text
2026-08-31T00:30:00Z
2026-08-31T08:30:00+08:00
```

禁止：

```text
2026-08-31 08:30:00
2026-08-31T08:30:00
```

这类无时区的 naive datetime。

### 8.2 Server 可规范化绝对时间

对“实际时间点”字段，Server 可以在响应中统一规范化为 UTC `Z`，也可以保留具有等价绝对时间语义的 Offset；客户端不得依赖具体 Offset 做展示逻辑。

展示时必须根据 Instance 的 Application Timezone 转换。

默认 Application Timezone 为 **UTC+8**，与 `System-Overview-Design.md` 保持一致。

### 8.3 Wall-clock Rule 必须带时区上下文

Reminder、Scheduled Job、Calendar Rule、Scheduled Transaction、统计自然日等需要表达“当地墙上时间”的对象，不得只保存一个绝对时间点然后丢失规则时区。

API Schema 应明确提供：

- Local Date / Local Time 或 Recurrence Rule；
- Timezone Context；
- 下一次实际执行时刻（若已计算）；
- 未显式指定时使用当前 Instance Application Timezone。

### 8.4 Date-only

纯业务日期使用：

```text
YYYY-MM-DD
```

例如账期日、生日、统计自然日标签等。

纯日期不得偷偷附加 `00:00:00Z` 后假装成时间点。

### 8.5 时间窗口

Analytics、Audit、Log、History 等范围查询应采用明确的半开或闭区间定义，并在 OpenAPI 中写明。

推荐范围语义：

```text
[from, to)
```

避免相邻窗口重复计数。

涉及日 / 周 / 月 / 季度边界的统计，默认按 Application Timezone 切分，而不是客户端设备时区或数据库 Session 时区。

---

## 9. 数值、金额与枚举

### 9.1 Decimal 与 Money

涉及货币、汇率或不能容忍 IEEE-754 二进制浮点误差的精确小数，API 不应依赖 JSON Float 表达精确值。

推荐金额结构：

```json
{
  "amount": "1234.56",
  "currency": "CNY"
}
```

其中 `amount` 为十进制定点字符串，具体 Scale / Precision 由领域契约声明。

### 9.2 枚举

公共枚举值使用稳定字符串 Code，例如：

```text
PENDING
RUNNING
SUCCEEDED
FAILED
CANCELLED
TIMED_OUT
```

禁止使用数据库 ordinal 或语言内部枚举序号。

读取方必须能安全处理未来新增的未知枚举值；不得因为服务端新增一个兼容枚举值就直接反序列化崩溃。

写入方提交服务端不支持的枚举值时，服务端应返回明确 Validation Error，不得静默映射为默认值。

### 9.3 null、unknown、not_applicable、empty

以下语义不得混用：

```text
null
≠ unknown
≠ not_applicable
≠ empty
```

当 Unknown / Not Applicable 是正式业务状态时，必须使用显式状态或枚举表达。

空 List 用 `[]`，不能因为“没有成员”就返回 `null`，除非该字段契约明确把 `null` 定义为不同语义。

---

## 10. Request Context、Trace 与 Correlation

### 10.1 Request ID

每个 HTTP 请求必须具有 Request ID。

客户端可以发送：

```http
X-Request-ID: <opaque-id>
```

若未发送，由 Server 生成。

Server 应在响应中回传实际 Request ID。

Request ID 用于定位一次 HTTP Request，不是业务实体 ID，也不是安全凭据。

### 10.2 Trace Context

分布式追踪优先兼容 W3C Trace Context：

```http
traceparent: ...
tracestate: ...
```

错误响应可以额外返回机器可查询的 `trace_id`，但不得把 Trace 当作权限凭据。

### 10.3 Correlation ID 与 Causation ID

跨 Command、Event、Background Task、Automation Run 等长期业务链路，需要保留业务 Correlation / Causation 语义。

```text
request_id
= 一次 HTTP 请求

trace_id
= 一次技术追踪链路

correlation_id
= 一条业务因果链的总关联标识

causation_id
= 直接导致当前动作/事件的上一跳标识
```

它们不能因为“都是 ID”而合并成同一个字段。

### 10.4 审计上下文

状态变更请求必须能够解析出明确 Principal，例如：

- User；
- API Token；
- Share Token；
- Automation；
- Scheduled Job；
- Plugin；
- Webhook Principal；
- System Worker。

不存在“后台请求天然是管理员”的规则。

---

## 11. Authentication、Authorization 与 Step-up

### 11.1 三层判断

受保护 API 需要保持：

```text
Authentication
   AND
Authorization
   AND
Required Step-up Verification
   AND
Security Policy
```

高等级验证不会创造业务 Permission。

### 11.2 HTTP Status

基础语义：

- `401 Unauthorized`：未建立有效身份或认证凭据失效；
- `403 Forbidden`：身份已确认，但当前 Principal 不满足业务授权或安全策略；
- `404 Not Found`：对象不存在，或安全策略要求隐藏其存在性；
- `403` + `security.verification_required`：调用者本来具备目标权限，但当前 Security Verification Level / Freshness 不足，且返回该信息不会造成枚举泄露。

### 11.3 Step-up Error

需要 Step-up 时，使用统一 Problem Detail，例如：

```json
{
  "type": "about:blank",
  "title": "Additional verification required",
  "status": 403,
  "code": "security.verification_required",
  "request_id": "...",
  "trace_id": "...",
  "required_svl": "SVL_1",
  "fresh_verification_required": true,
  "allowed_methods": ["EMAIL_OTP"]
}
```

这些字段只能在调用者已经被允许知道目标能力存在时返回。

### 11.4 Anti-enumeration

认证、Recovery、Secure Domain、Share、Secret 等接口不得通过不同状态码、不同错误文案、明显不同响应结构泄露：

- 某 Email 是否注册；
- 某用户是否拥有 Vault；
- 某 Secret 是否存在；
- 某 Private Note 是否存在；
- 某不可见 Resource 是否存在。

必要时允许对“无权限”和“不存在”统一返回安全的 `404` 或统一 Challenge Result。

### 11.5 Capability Discovery 不替代权限

`capability.available = true` 只表示当前 Instance 具备能力。

它绝不表示当前 User 可以调用该能力。

真实业务请求必须重新进行完整权限检查。

---

## 12. 数据敏感等级与 API 暴露

API 必须遵循系统级数据敏感等级：

```text
Public
Shared
Private
Sensitive
Secure Domain
```

该等级约束数据传播方式，不替代 Permission / ACL。

### 12.1 Public / Shared / Private

即使数据允许读取，修改仍必须独立授权。

Shared / Private Representation 必须避免：

- 被共享代理缓存；
- 出现在未授权搜索建议；
- 通过错误详情泄露；
- 通过关联对象反向泄露不可见对象存在性。

### 12.2 Sensitive

Sensitive 字段应采用最小化返回。

例如银行账户只需要尾号时，返回：

```json
{
  "masked_identifier": "**** 1234"
}
```

而不是同时把完整值放在另一个“前端不会显示”的字段中。

### 12.3 Secure Domain

对于 USER_LOCKED_E2EE 等 Secure Domain：

- Server API 默认只传输 Ciphertext 与最小必要 Metadata；
- 不得把明文写入 Error、Trace、Log、Event、Notification、Analytics；
- 普通管理员权限不等价于解密权限；
- Secure Search 默认不通过普通服务端 Search API 返回明文；
- API 调试模式也不得关闭上述约束。

Private Notes、Password Manager 的同步接口应以 Ciphertext / Revision 为核心，而不是要求 Server 理解内容。

### 12.4 Secret Use 与 Secret Reveal 分离

Secret Reference 应优先支持：

```text
Use Secret
```

而不是：

```text
Reveal Secret
```

调用 Storage、Plugin、HTTP Connector 等场景时，能由安全子系统直接注入 Credential 的，不应把 Secret 明文返回给调用方。

Reveal API 必须有独立权限和必要的 Unlock / Step-up / Audit。

---

## 13. 统一 Success Representation

V2 不采用全局强制：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

这种二次包装。

原因：

- HTTP Status 已经表达协议层结果；
- Streaming / Download / `204` 不适合统一 Wrapper；
- 额外 Wrapper 会让 OpenAPI Schema 复杂化；
- 客户端不应同时判断 HTTP Status 和自定义 `code=0` 才知道成功。

### 13.1 单资源

直接返回资源 Representation：

```json
{
  "id": "019c...",
  "title": "...",
  "version": 7,
  "created_at": "2026-08-30T17:07:27Z",
  "updated_at": "2026-08-31T00:10:00Z"
}
```

### 13.2 无响应体

无需返回 Representation 时使用：

```http
204 No Content
```

### 13.3 创建

同步创建成功使用 `201 Created`，并返回 `Location`。

### 13.4 异步接受

请求已经通过校验并创建异步执行时使用：

```http
202 Accepted
Location: /api/v2/background-tasks/{id}
```

`202` 不表示业务已经执行成功。

---

## 14. Error Model

### 14.1 Problem Details

统一错误响应采用 `application/problem+json`，结构基于 Problem Details，并增加 Ikaros 扩展字段。

推荐结构：

```json
{
  "type": "about:blank",
  "title": "Request validation failed",
  "status": 422,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v2/tasks",
  "code": "validation.failed",
  "request_id": "req_...",
  "trace_id": "...",
  "retryable": false,
  "errors": [
    {
      "field": "deadline_at",
      "code": "time.invalid_range",
      "message": "deadline_at must be after scheduled_start_at"
    }
  ]
}
```

### 14.2 `code` 是稳定机器契约

客户端可以依赖：

```text
validation.failed
security.verification_required
resource.not_found
concurrency.precondition_failed
idempotency.key_reused
rate_limit.exceeded
```

这类稳定 Code。

客户端禁止通过匹配 `title` / `detail` 文案判断业务逻辑。

人类可读文案可以本地化或调整。

### 14.3 常用 Status Mapping

| Status | 语义 |
|---:|---|
| 400 | HTTP / JSON / Query 结构错误、非法 Cursor 等 |
| 401 | 未认证或认证失效 |
| 403 | 已认证但无权限 / Step-up 不足 |
| 404 | 不存在，或按安全策略隐藏存在性 |
| 409 | 业务状态冲突、幂等 Key 与不同 Payload 冲突、无法自动合并的 Domain Conflict |
| 410 | 已明确永久失效且允许调用者知道，例如已过期的公开临时资源 |
| 412 | `If-Match` / ETag 前置条件失败 |
| 415 | 不支持的 Content Type |
| 422 | 请求结构正确，但业务字段校验不通过 |
| 428 | 该更新必须携带 `If-Match` 等前置条件，但调用方未提供 |
| 429 | Rate Limit |
| 500 | 未分类的 Server 内部错误 |
| 502 | 上游 Provider 返回错误或无效响应 |
| 503 | 当前能力临时不可用 / 过载 / 依赖不可用 |
| 504 | 上游 Provider / Worker 请求超时 |

### 14.4 Error Data Minimization

错误响应、Validation Detail 和 Exception Mapping 禁止回显：

- Password；
- OTP；
- Token；
- Cookie；
- Authorization Header；
- Secure Domain 明文；
- 完整银行卡号；
- Private Key；
- 完整敏感请求 Payload。

即使开发环境也应默认遵循该规则。

---

## 15. List、Pagination、Filter 与 Sort

### 15.1 Cursor Pagination 为普通实体列表默认方案

对于持续变化、数据量可能增长的业务集合，默认采用 Cursor Pagination：

```http
GET /api/v2/resources?limit=50&cursor=opaque_cursor
```

响应：

```json
{
  "items": [
    { "id": "019c..." }
  ],
  "page": {
    "limit": 50,
    "next_cursor": "opaque_cursor_or_null",
    "has_more": true
  }
}
```

统一要求：

- Cursor 必须是不透明值；
- 客户端不得解析 Cursor 内部结构；
- Cursor 应绑定排序与关键查询上下文，避免误用于不同 Filter；
- Cursor 不得把未授权字段明文编码给客户端；
- 默认 `limit` 建议为 50；
- 通用最大值建议为 200，专项 API 可以定义更严格上限；
- 非法或失效 Cursor 返回 `400 pagination.cursor_invalid`。

### 15.2 Total Count 默认不是强制字段

大集合分页默认不要求每次返回精确 `total`。

若业务 UI 确实需要总数，可以由 endpoint 明确支持：

```text
include_total=true
```

或提供独立 Count / Metric 能力。

服务端不得为了一个普通列表页在每次请求中强制执行昂贵精确 Count。

### 15.3 Offset / Page Pagination

以下场景可以使用 Page / Offset：

- 小型稳定配置集合；
- 明确需要页码跳转的管理查询；
- 报表结果；
- 某些离线快照。

但同一个 endpoint 不应同时提供两套含义重叠的分页方式。

### 15.4 排序

统一 Query：

```text
sort=-updated_at,title
```

规则：

- `-` 表示 Descending；
- 无前缀表示 Ascending；
- 服务端只接受 allow-list 中的排序字段；
- 不允许客户端传任意 SQL / ORM 表达式；
- 排序必须有稳定 Tie-breaker；
- 使用 UUIDv7 `id` 只能作为稳定 Tie-breaker，不能代替正式时间排序语义。

### 15.5 Filter

领域 Filter 使用明确命名的 Query Parameter，不设计一个能直接映射 SQL 的“万能表达式”。

例如：

```text
status=ACTIVE
resource_type=ANIME
created_from=...
created_to=...
q=ikaros
```

多值参数应通过 OpenAPI 明确数组编码方式，默认采用 `style=form, explode=true`。

### 15.6 Search 与 Filter 分离

`q` 表示面向人的文本检索。

结构化字段筛选使用显式 Filter。

Smart View / Saved Query 可以拥有领域自己的表达式语言，但该语言必须是稳定业务契约，不能成为 SQL Injection 风格的数据库透传层。

### 15.7 Analytics 查询

Analytics API 通过统一 Semantic Layer 查询：

- Metric Query；
- Time Series Query；
- Breakdown Query；
- Comparison Query；
- Dashboard Query；
- Report Export。

Dashboard 不得传入物理 Fact / Aggregate 表名直接查询。

Metric Key、Metric Version、Time Grain、Dimension、Application Timezone 等属于统计契约的一部分。

---

## 16. Optimistic Concurrency、ETag 与 Revision

### 16.1 ETag

对于可能被多端并发编辑的实体，GET / Create / Update Response 应提供 `ETag`。

ETag 是不透明并发 Token，可以来源于实体 Version / Revision，但客户端不能依赖其内部编码。

示例：

```http
ETag: "opaque-version-token"
```

### 16.2 If-Match

需要防止 Lost Update 的写请求应要求：

```http
If-Match: "opaque-version-token"
```

处理规则：

```text
缺少 If-Match，但 endpoint 要求并发前置条件
→ 428 Precondition Required

If-Match 与当前版本不匹配
→ 412 Precondition Failed
```

客户端收到 `412` 后应重新获取最新状态并决定重新提交、人工合并或取消。

### 16.3 ETag 不等于业务 Revision

`ETag` 是 HTTP 层的并发 / 缓存 Token。

`revision` 是业务明确的版本序列时才作为公共领域字段存在。

两者可以相关，但不能强制等价。

### 16.4 Secure Offline Sync

Password Manager 和 Private Notes 等 USER_LOCKED_E2EE 同步必须遵循其专项设计：

```text
Ciphertext
+ Revision
+ ETag / Concurrency Token
+ Tombstone
+ Crypto Metadata
```

Server 在不能解密正文的情况下不得做语义 Last Write Wins。

并发分叉需要形成明确 Conflict Revision / Conflict Result，由解锁后的客户端执行 Keep Mine / Keep Remote / Keep Both / Manual Merge 等策略。

此类业务冲突应返回 `409` 或形成可查询 Conflict Resource，具体由对应同步 API 定义。

---

## 17. Idempotency Convention

### 17.1 使用场景

以下接口默认必须考虑 Idempotency：

- Import；
- Upload Complete / Commit；
- Webhook Ingress；
- Automation Action；
- External Sync；
- Accounting Import；
- 创建 Background Task；
- 可被重试的 Command；
- 客户端网络恢复后可能重复提交的创建动作。

### 17.2 Idempotency-Key

客户端通过：

```http
Idempotency-Key: <opaque-key>
```

标识一次逻辑写操作。

Key 应为高熵、不含业务敏感信息的不透明字符串。

服务端去重范围至少应包含：

```text
Instance
+ Principal / Credential Scope
+ API Operation
+ Idempotency-Key
```

避免不同用户或不同 endpoint 之间互相碰撞。

### 17.3 同 Key 同请求

同一个 Idempotency Key 对同一逻辑请求重复提交时，应返回同一逻辑结果，例如同一个：

- Resource ID；
- Import Batch ID；
- Background Task ID；
- Upload Commit Result。

不能重复创建第二份业务事实。

### 17.4 同 Key 不同请求

同一个 Key 被用于不同 Payload 时返回：

```text
409 Conflict
code = idempotency.key_reused
```

不得把第二份 Payload 当作重试继续执行。

### 17.5 Retention

每个支持 `Idempotency-Key` 的 endpoint 必须在 OpenAPI / 文档中说明去重窗口或保证级别。

服务端保存时间至少覆盖该接口公开承诺的正常网络与任务重试窗口。

### 17.6 Idempotency 与 Concurrent Update 分离

```text
Idempotency-Key
→ 防止同一逻辑命令重复执行

ETag / If-Match
→ 防止基于旧版本覆盖新版本
```

两套机制解决不同问题，不能互相替代。

---

## 18. Long-running Action 与异步 API

### 18.1 何时返回 202

以下操作如果无法在一个短 HTTP Request 内稳定完成，应创建 Background Task / Job Run / 对应 Execution，而不是让请求无限等待：

- 大批量 Import；
- Transcode；
- OCR；
- Index Rebuild；
- Analytics Backfill；
- Backup / Restore；
- Blob Migration / Verify；
- 大型 AI Job；
- 大规模 Export；
- 长时间 External Sync。

请求完成同步校验并成功创建执行实例后返回：

```http
202 Accepted
Location: /api/v2/background-tasks/{task_id}
```

### 18.2 Async Response

推荐返回最小可追踪引用：

```json
{
  "id": "019c...",
  "type": "BACKGROUND_TASK",
  "status": "PENDING",
  "created_at": "2026-08-31T00:00:00Z",
  "links": {
    "self": "/api/v2/background-tasks/019c..."
  }
}
```

### 18.3 状态语义

公共执行状态至少保持系统级语义：

```text
PENDING
RUNNING
SUCCEEDED
FAILED
CANCELLED
TIMED_OUT
```

专项可扩展：

```text
QUEUED
CANCELLING
RETRYING
THROTTLED
```

但必须保持：

- `FAILED` = 本次执行因错误终止；
- `CANCELLED` = 明确取消终止，不等于失败；
- `TIMED_OUT` = 超过允许执行时间，不等于用户取消；
- `SUCCEEDED` = 达到业务成功条件，不只是线程没有抛异常；
- 等待资源 / Rate Limit 应使用 Pending / Queued / Throttled 等等待语义，不伪装成 Failed。

### 18.4 Error Classification

失败结果应能够表达：

```text
retryable = true / false
```

以及安全的 Error Summary。

不能把完整外部响应、Secret、Secure Payload 直接保存或返回。

### 18.5 Retry Attempt

每次 Retry 是同一逻辑执行下的新 Attempt。

API 应允许查看：

- attempt_number；
- started_at；
- ended_at；
- status；
- error_summary；
- next_retry_at（若有）。

Retry 必须继承原始：

- actor / principal context；
- permission context；
- idempotency context；
- correlation_id；
- trace linkage。

同时每个 Attempt 有自己的执行身份。

### 18.6 Cancel

取消使用显式 Action：

```http
POST /api/v2/background-tasks/{task_id}/actions/cancel
```

如果底层不能立即终止，可以先进入：

```text
CANCELLING
```

只有实际结束后才进入 `CANCELLED`。

### 18.7 Progress

只有可计算时才返回数值进度，例如：

```json
{
  "progress": {
    "current": 420,
    "total": 1000,
    "unit": "ITEMS",
    "percent": 42
  }
}
```

无法可靠计算时只返回 Phase / Status，不制造虚假百分比。

### 18.8 组合执行

Automation 等一个 Execution 可能包含多个 Action。

API 可以返回每个 Action Result，但总执行状态必须由该 Execution 预先声明的成功策略判定。

不得随意新增一个与系统级状态语义冲突的“差不多成功”状态。

---

## 19. Streaming、SSE 与 WebSocket

### 19.1 传输方式选择

推荐：

```text
普通请求响应
→ HTTP JSON

Server → Client 单向实时流
→ SSE / HTTP Streaming

双向房间、协作、状态同步
→ WebSocket

媒体实时传输
→ 对应 Media Protocol / HTTP Range / WebRTC 等
```

### 19.2 SSE

适合：

- AI 文本 Streaming；
- Agent 执行阶段；
- Background Task Progress；
- Notification Push；
- Health Update。

SSE 事件类型必须是稳定字符串，不依赖客户端解析自由文本。

例如 AI Agent 可以发送机器事件：

```text
agent.search.started
agent.tool.started
agent.tool.completed
message.delta
message.completed
```

UI 展示“Searching...”等文案由客户端或可本地化字段处理，不能把展示文字当作协议状态。

### 19.3 SSE Resume

只有声明为 Durable / Replayable 的 Stream 才保证 `Last-Event-ID` 恢复。

临时 Token Stream、瞬时 Health Stream 等不得让客户端误以为所有历史事件都可以重放。

### 19.4 WebSocket Message Envelope

双向实时消息应有稳定 Envelope，例如：

```json
{
  "message_id": "019c...",
  "type": "room.playback.updated",
  "contract_version": 1,
  "occurred_at": "2026-08-31T00:00:00Z",
  "correlation_id": "019c...",
  "payload": {}
}
```

### 19.5 实时连接不绕过权限

建立 WebSocket / SSE 连接只代表连接本身通过认证。

订阅 Room、Document、Task、Secure Object 等对象时，Server 仍需检查目标对象 Permission / ACL。

连接期间权限被撤销、Session 被撤销、Share Token 过期时，应停止对应订阅或关闭连接。

### 19.6 实时接口不是隐藏业务通道

任何通过 WebSocket 发起的状态修改都必须映射到明确的业务 Command，并执行与 HTTP API 等价的权限和业务校验。

不得因为“这是 Room Socket 消息”就直接修改目标领域表。

---

## 20. Attachment、Blob、Upload 与 Download

### 20.1 Metadata 与 Binary 分离

Attachment / Blob 元数据使用 JSON API。

大型二进制内容使用独立 Binary Transfer。

禁止：

```json
{
  "file": "<huge base64>"
}
```

作为常规大文件上传方案。

### 20.2 Upload 推荐流程

大文件上传可以使用：

```text
Create Upload Session
        ↓
Upload Binary / Parts
        ↓
Checksum / Integrity Validation
        ↓
Commit Upload
        ↓
Attachment / Blob Domain Command
```

Commit / Complete 操作必须支持幂等，网络重试不能创建重复 Attachment。

### 20.3 Hash

客户端提交 Hash 可以帮助完整性检查，但 Server / Storage 流程应根据可信边界决定是否重新验证。

Blob Content Hash 是内容身份，不是 Attachment ID。

### 20.4 Range Request

视频、音频、大型文档等需要随机访问时，应支持标准 HTTP Range / `206 Partial Content`，具体是否启用由 Attachment 类型和 Storage Provider 能力决定。

### 20.5 Direct / Signed URL

当底层 Storage Provider 支持时，Server 可以签发短期 Direct Download / Upload URL。

该 URL：

- 必须短期有效；
- Scope 只覆盖目标内容和目标动作；
- 不得成为 Attachment 的永久身份；
- 不得把长期 Storage Credential 暴露给客户端；
- 仍需先经过 Ikaros Permission / ACL 判断。

### 20.6 Sensitive Download

Sensitive / Secure Domain 明文导出或临时解密下载应默认：

```http
Cache-Control: no-store
```

并遵循专项 Unlock / Step-up / Audit 规则。

Private Notes / Password Manager 的 Secure Blob 默认仍传输密文；显式明文导出属于更高风险的独立动作。

---

## 21. HTTP Cache 与 Conditional Request

### 21.1 ETag Conditional GET

可缓存资源可以支持：

```http
If-None-Match: "etag"
```

未变化返回：

```http
304 Not Modified
```

### 21.2 Cache-Control 按敏感等级决定

不能给所有 API 统一一个 Cache Policy。

推荐原则：

- Public、稳定元数据：可以声明合理 Cache；
- User-specific Shared / Private：禁止 Shared Cache，按 endpoint 设置 `private`；
- Sensitive：优先短缓存或 `no-store`；
- Secure Domain 明文 / Reveal：必须 `no-store`；
- Ciphertext Sync 可以根据 Revision / ETag 安全缓存，但仍受授权影响。

### 21.3 缓存不替代授权

即使 CDN / Reverse Proxy 命中了缓存，受保护 API 也不能向错误 Principal 泄露数据。

---

## 22. Batch 与 Bulk Operation

### 22.1 批量接口必须声明原子性

Bulk API 必须明确属于：

```text
ATOMIC
```

或：

```text
BEST_EFFORT / PER_ITEM_RESULT
```

不得让调用方猜测“第三条失败后前两条到底有没有提交”。

### 22.2 跨子系统不创建超级事务

一个 Batch 不得因为方便前端就把多个领域状态绑进跨系统大事务。

跨域批量动作应通过 Command / Event / Automation 组合，并接受系统级最终一致性原则。

### 22.3 大批量操作异步化

当 Item 数量大、执行时间不可控或需要外部 Provider 时，Bulk API 应创建 Background Task，并通过 `202` 返回执行引用。

### 22.4 Import Preview / Confirm

Accounting 等需要预览与确认的导入不得压缩成一个不可解释的 `bulkCreate`。

应保持领域流程：

```text
Parse
→ Normalize
→ Deduplicate
→ Preview
→ Confirm
→ Commit Business Facts
```

Preview Result 必须有明确有效期或 Version，Confirm 时需要防止基于过期预览提交。

---

## 23. Webhook Convention

Webhook 是可选能力，是否启用由 Capability Discovery 暴露；一旦实现，必须遵循以下公共规则。

### 23.1 Inbound Webhook

入口必须考虑：

- Provider Authentication / Signature；
- Timestamp / Replay Protection；
- Delivery ID；
- Idempotency / Deduplication；
- Payload Size Limit；
- Content Type Validation；
- Rate Limit；
- Principal / Permission Context；
- Audit / Trace。

收到重复 Delivery 不得重复创建业务事实。

### 23.2 快速确认与异步处理

Webhook Handler 不应为了等待完整业务流程而长时间占用第三方连接。

完成签名、基本格式和去重校验后，可以：

```text
Persist Delivery / Command
→ 202 Accepted / Provider-compatible 2xx
→ Async Process
```

具体响应方式需兼容 Provider 协议。

### 23.3 Outbound Webhook

投递必须具有：

- Delivery ID；
- Event / Action Type；
- Event Contract Version；
- Occurred At；
- Correlation ID；
- Attempt Number；
- Signature；
- 安全过滤后的 Payload。

默认按至少一次投递设计，消费者必须幂等。

### 23.4 Retry

可重试投递必须支持：

- Backoff；
- Maximum Attempts；
- Next Retry At；
- Manual Retry；
- Dead Letter / Failed Delivery。

不得无限重试。

### 23.5 敏感数据

Webhook Payload 不得因为“目标是用户自己的服务器”就绕过数据敏感等级。

Secure Domain 明文、Secret、OTP、完整 Credential 默认永久禁止进入普通 Webhook Payload。

---

## 24. Rate Limit、Timeout 与 Retry

### 24.1 Rate Limit

API Gateway / Server 可以按：

- Principal；
- Token；
- IP；
- Endpoint Risk；
- Provider Cost；
- Instance Resource Budget。

执行 Rate Limit。

认证、OTP、Recovery、Login、Secret Reveal 等安全接口应采用更严格策略。

命中限制返回：

```http
429 Too Many Requests
Retry-After: ...
```

### 24.2 Timeout

对外部 Provider、AI、Storage、Notification、Health Check 等依赖必须有 Timeout。

普通 HTTP 请求不应因为第三方永久不返回而无限悬挂。

### 24.3 Client Retry

客户端可以安全自动重试：

- 幂等 GET / HEAD；
- 明确幂等的 PUT；
- 携带有效 `Idempotency-Key` 的可重试 POST；
- 文档明确允许重试的操作。

客户端不得对任意状态变更 POST 盲目自动重试。

### 24.4 Retry-After

`429`、部分 `503` 和 Polling 场景可以返回 `Retry-After`，客户端应尊重该提示并配合指数 Backoff / Jitter。

---

## 25. Capability Discovery

### 25.1 目标

不同 Ikaros Instance 可能启用不同：

- Plugin；
- AI Provider；
- Worker；
- Storage Provider；
- Realtime 能力；
- Semantic Search；
- Archive；
- Secure Domain 功能。

客户端不能通过不断调用失败来猜功能。

### 25.2 Capability Key

使用稳定的 dot-separated Key，例如：

```text
ai.enabled
room.enabled
secure_notes.enabled
media.transcode.available
storage.archive.available
search.semantic.available
plugin.<key>.enabled
```

Capability Key 不绑定 Java Module 名或部署拓扑。

### 25.3 推荐 Response

```json
{
  "api": {
    "major": 2,
    "contract_version": "2.0"
  },
  "application_timezone": "<effective-instance-timezone>",
  "capabilities": {
    "ai.enabled": {
      "available": true
    },
    "media.transcode.available": {
      "available": false,
      "reason": "WORKER_UNAVAILABLE"
    }
  }
}
```

具体 endpoint 名称由实现时确定，但整个 Instance 应只有一个统一 Discovery 语义，不应由每个模块发明自己的 Feature Probe。

### 25.4 安全限制

Discovery 可以告诉客户端“能力是否可用”，但不应暴露：

- Secret；
- Provider Credential；
- 内部主机名；
- Storage 私有路径；
- 当前用户没有权限知道的敏感安装详情。

---

## 26. API Version、兼容性与弃用

### 26.1 版本概念必须分离

Ikaros V2 同时存在：

```text
Database Schema Version
API Version
Plugin API Version
Event Contract Version
Export Format Version
```

API Convention 只直接管理 HTTP API Version。

### 26.2 同一 Major Version 内的兼容演进

通常视为兼容：

- 新增 endpoint；
- 新增可选请求字段；
- 新增响应字段；
- 新增可安全忽略的 Event / Realtime 字段；
- 新增枚举值，前提是读取方遵守 Unknown-safe 规则；
- 扩展 Capability Key。

通常视为破坏性修改：

- 删除字段；
- 改变字段类型；
- 改变字段原有含义；
- 把 Optional 改为 Required；
- 改变默认值并导致既有调用行为变化；
- 改变状态机原有语义；
- 重用旧枚举值表达新含义；
- 把同步成功改成无法兼容处理的另一套结果结构；
- 静默缩小公开权限语义之外的契约能力。

破坏性修改原则上进入新的 API Major Version。

### 26.3 Unknown Field Tolerance

客户端必须忽略自己不认识的响应 JSON 字段。

服务端是否允许请求中出现未知字段，由具体写入 Schema 决定；默认稳定写接口应严格校验，避免用户以为某字段已经生效但实际被静默忽略。

### 26.4 Deprecation

弃用必须：

- 在 OpenAPI 中标记 `deprecated`；
- 文档给出替代能力；
- 提供明确迁移窗口；
- 在运行时可行时返回标准 `Deprecation` / `Sunset` 信息；
- 不在普通 Patch Release 中无信号删除公开稳定接口。

### 26.5 安全紧急例外

若接口本身存在高风险安全缺陷，可能需要缩短弃用窗口或紧急关闭。

此类变更必须明确记录安全原因和受影响范围，不能借“安全”名义常规破坏兼容性。

---

## 27. Event Contract 与 HTTP Contract 的关系

HTTP API Version 与 Event Contract Version 独立。

例如：

```text
POST /api/v2/tasks/{id}/actions/complete
```

成功后可能产生：

```text
task.completed
contract_version = 1
```

API 仍是 V2，并不意味着 Event Contract 必须叫 V2。

长期可被 Automation、Analytics、Plugin、Worker 消费的 Event 必须带独立 Schema Version。

事件公共 Envelope 至少保持：

- event_id；
- event_type；
- occurred_at；
- producer_subsystem；
- actor / principal（若存在）；
- subject / aggregate_id；
- correlation_id；
- causation_id；
- contract_version；
- payload。

Payload 必须遵守数据敏感等级，Secure Domain 跨系统只传播安全引用和最小事实。

---

## 28. OpenAPI Contract

### 28.1 核心 HTTP API 必须有 OpenAPI

稳定核心 HTTP API 必须进入统一 OpenAPI Contract，用于：

- CMS Client；
- Flutter Client；
- SDK；
- Plugin / Integration；
- 第三方开发者；
- Mock；
- Contract Test；
- 兼容性检查。

### 28.2 OpenAPI 是公开契约，不是内部代码转储

OpenAPI Schema 不得直接暴露 ORM Entity。

公共 DTO 必须只包含公开语义。

### 28.3 Operation ID

每个公开 Operation 必须有稳定、唯一、可用于生成 SDK 的 `operationId`。

推荐按领域命名，例如：

```text
resources.listResources
resources.getResource
resources.archiveResource
tasks.completeTask
```

一旦发布，非必要不重命名，否则会造成生成 SDK 的破坏性变化。

### 28.4 Tag

OpenAPI Tag 按稳定业务子系统 / 公共能力组织，而不是按 Java Controller 类组织。

### 28.5 Schema 要求

每个 Schema 必须明确：

- Required / Optional；
- Nullable；
- Format；
- Enum；
- 时间与时区语义；
- Decimal / Money 表达；
- Data Sensitivity 注意事项；
- 是否可写 / 只读；
- Example。

Example 禁止使用真实 Secret / Token / 用户敏感数据。

### 28.6 公共 Component

以下内容应定义成统一 Reusable Components：

- Problem Detail；
- Pagination；
- UUID；
- RFC3339 Instant；
- Money；
- Async Operation Reference；
- ETag / If-Match Header；
- Idempotency-Key Header；
- Request ID / Trace Context；
- Security Scheme。

### 28.7 Contract CI

CI 至少应检查：

- OpenAPI 语法；
- Operation ID 唯一；
- 未文档化 Breaking Change；
- 新 endpoint 是否有 Security / Error / Status Code 定义；
- 时间字段是否满足统一格式；
- 公共 ID 是否使用 UUID Contract；
- 枚举是否为字符串；
- 敏感接口是否错误暴露示例明文；
- 生成 SDK 是否仍可生成。

---

## 29. SDK 与客户端兼容规则

### 29.1 Generated Client 是消费者，不是真相源

SDK 可以由 OpenAPI 生成，但真正契约仍由 OpenAPI + 本文规范 + 对应领域设计共同确定。

禁止为了让某个生成器更方便而扭曲业务语义。

### 29.2 Unknown Enum

SDK 必须提供 Unknown / Raw Value 保底能力。

当服务端新增枚举值时，旧客户端应能够：

- 显示 Unsupported / Unknown；
- 保留原始字符串；
- 禁止在不理解该状态时执行危险写操作；
- 不直接崩溃。

### 29.3 Unknown Response Field

旧客户端忽略未知响应字段。

### 29.4 Capability First

客户端判断可选功能优先通过 Capability Discovery，而不是：

```text
if serverVersion >= x.y.z
```

版本号可以用于诊断，但不应成为所有 Feature Detection 的唯一方法。

---

## 30. Log、Audit 与 Observability

### 30.1 Access Log

HTTP Access Log 可以记录：

- request_id；
- trace_id；
- method；
- route template；
- status；
- duration；
- principal 类型 / 安全标识；
- response size；
- rate-limit / dependency outcome。

禁止默认记录完整 Authorization、Cookie、Query Secret、Body。

### 30.2 Route Template 优先

Metric / Log 应记录：

```text
/api/v2/resources/{resource_id}
```

而不是把每个实际 UUID 当作独立 Metric Label，避免高基数。

### 30.3 Audit Log

具有治理价值的写操作应产生业务 Audit，而不是依赖 Access Log 充当审计。

Audit 至少要能回答：

- 谁；
- 什么时候；
- 对什么；
- 做了什么；
- 结果；
- Request / Trace / Correlation；
- 是否经过 Step-up。

Audit 不记录 Secure Payload 明文。

### 30.4 Error / Trace Redaction

Trace Span、Exception、Debug Context 必须执行与 API Response 一致的数据最小化。

不得出现“HTTP Response 已脱敏，但 Trace 里保存完整 Secret”的旁路泄露。

---

## 31. Health、Operations 与 Admin API

### 31.1 Public Health 最小化

若提供匿名 Liveness Endpoint，只应返回判断实例是否能够接受基本连接所需的最小信息。

不得匿名暴露：

- 数据库地址；
- Storage Bucket；
- Plugin 私有配置；
- Secret；
- 完整版本拓扑；
- 内部错误堆栈。

### 31.2 Detailed Health 受权

Subsystem Health、Provider Health、Queue Lag、Task Failure 等详细信息属于 Operations / Admin API，并要求相应平台 Permission。

### 31.3 Admin API 不影响业务主链路

昂贵 Log / Audit / Metrics 查询应具有：

- 时间范围；
- Pagination；
- Query Limit；
- Timeout；
- 合理索引 / Analytics 路径。

管理查询失败或外部 Provider Health Check 超时，不应拖垮普通 Resource、Media、Reading 等业务 API。

---

## 32. Browser、CORS 与 CSRF

### 32.1 CORS

Self-hosted 场景下 CORS 必须由 Instance 明确配置，不默认允许任意 Origin 携带认证信息。

### 32.2 Cookie Session

如果 Web 客户端使用 Cookie Session，则所有状态变更请求必须具备适当 CSRF 防护，并配置安全 Cookie 属性。

### 32.3 Bearer Token

使用 `Authorization: Bearer ...` 的 API Token / OAuth Access Token 不应放入 Query String。

下载或直链若因协议必须使用临时 Token，应使用：

- 极短 TTL；
- 单资源 Scope；
- 最小权限；
- 必要时单次使用；
- 防止被长期日志保留。

---

## 33. Domain-specific API 约束汇总

本节只记录各现有 V2 专项设计已经对 API Convention 形成的横向约束，不重新定义其领域模型。

### 33.1 Platform Integration / Automation

- HTTP Mutation 最终进入目标子系统 Command；
- Capability 用于同步查询；
- Event 是已经发生的事实；
- Event 至少一次投递，消费者幂等；
- Automation / Webhook / Plugin / Scheduled Job 具有明确 Principal；
- Retry / Dead Letter / Correlation 可追踪。

### 33.2 Security

- Authentication、Authorization、Step-up 分离；
- 高风险操作按 Security Policy 要求 Verification；
- OTP / Recovery 等接口 Anti-enumeration；
- 认证保证等级不创造 Permission。

### 33.3 Platform Administration / Operations

- Menu 不是权限源；
- Admin API 独立授权；
- 用户、日志、任务等列表需要统一查询 / 筛选 / 分页；
- Health / Log 等管理能力不得拖垮业务主链路。

### 33.4 AI

- AI Context 不超过当前 Principal 可访问集合；
- AI Tool Action 进入已有 Command；
- 高风险动作需要 Human-in-the-loop；
- 长文本支持 Streaming；
- 长耗时 AI Job 进入 Background Task。

### 33.5 Analytics

- API 查询 Metric / Semantic Layer，不暴露物理表；
- Metric 有版本、时间语义、Dimension、Privacy；
- Aggregate Count 不自动授予底层对象读取权限；
- 时间窗口遵守 Application Timezone。

### 33.6 Password Manager / Private Notes

- 服务端同步 Ciphertext；
- Revision / ETag 用于并发；
- 不采用静默 Last Write Wins；
- Conflict Version 可被客户端处理；
- Reveal 与 Use 分离；
- Secure 明文不进入普通 Search / AI / Event / Log / Analytics。

### 33.7 Accounting

- 普通账本数据不是默认 Secure Domain；
- 真正 Credential 使用 Secret Reference；
- 金额使用精确 Decimal Contract；
- Import 保留 Preview / Dedup / Confirm 业务语义；
- Automation 不直接改账务表。

### 33.8 Productivity

- Task 状态、Deadline、Scheduled Time 等保持领域语义分离；
- Complete / Cancel 等状态迁移优先显式 Command；
- Smart View 是保存的业务 Query，不是 SQL 透传；
- Reminder / Recurrence 遵守统一时区规则。

---

## 34. API Review Checklist

新增或修改一个稳定 API 前，Review 至少检查：

### 34.1 领域边界

- [ ] 该 endpoint 是否属于正确子系统？
- [ ] 是否绕过目标子系统 Command / Capability？
- [ ] 是否暴露了数据库 Schema 或内部 Entity？
- [ ] 是否把业务状态迁移错误建模成裸 PATCH？

### 34.2 Identity / Time

- [ ] 内部实体 ID 是否为 UUIDv7 公共身份？
- [ ] 是否错误使用 External ID 作为内部 ID？
- [ ] 时间点是否带 Offset / `Z`？
- [ ] 是否错误从 UUIDv7 推导正式时间？
- [ ] Wall-clock Rule 是否保留 Timezone Context？
- [ ] 默认 Application Timezone 是否遵守 UTC+8 系统规则？

### 34.3 Instance / Permission

- [ ] 是否误加 Tenant 语义？
- [ ] Authentication / Authorization 是否都执行？
- [ ] Resource ACL 是否执行？
- [ ] 高风险操作是否需要 Step-up？
- [ ] Admin API 是否与普通 Resource 权限分离？
- [ ] Capability Discovery 是否被错误当作 Permission？

### 34.4 Data Sensitivity

- [ ] 字段属于 Public / Shared / Private / Sensitive / Secure Domain 哪一类？
- [ ] Response 是否只返回最小必要数据？
- [ ] Error / Log / Trace 是否会泄露敏感 Payload？
- [ ] Secret 是否应改为 Reference / Use Without Reveal？
- [ ] Secure Domain 是否只返回 Ciphertext / Safe Metadata？
- [ ] 是否存在通过 403 / 404 差异造成枚举泄露？

### 34.5 HTTP Contract

- [ ] Method 与业务语义是否匹配？
- [ ] Success Status 是否正确？
- [ ] Error 是否使用统一 Problem Detail？
- [ ] Error Code 是否稳定、机器可判断？
- [ ] Create 是否返回 `201` + `Location`？
- [ ] Long-running 操作是否应该返回 `202`？
- [ ] 是否需要 `ETag` / `If-Match`？
- [ ] 是否需要 `Idempotency-Key`？
- [ ] 是否正确声明 Cache-Control？

### 34.6 Query

- [ ] 大集合是否使用 Cursor Pagination？
- [ ] Sort 是否 allow-list？
- [ ] 是否有稳定 Tie-breaker？
- [ ] Filter 是否是业务字段而非 SQL 透传？
- [ ] Analytics 是否通过 Metric Semantic Layer？

### 34.7 Compatibility

- [ ] OpenAPI 是否更新？
- [ ] Operation ID 是否稳定？
- [ ] 新枚举值旧客户端是否可安全处理？
- [ ] 是否删除 / 改义现有字段？
- [ ] 若弃用，是否有替代项与迁移窗口？
- [ ] API Version、Event Contract Version 是否被正确区分？

### 34.8 Async / Realtime

- [ ] Async State 是否保持 Pending / Running / Succeeded / Failed / Cancelled / TimedOut 基础语义？
- [ ] Retry 是否保留 Attempt History？
- [ ] Cancel Request 与 Cancelled 是否区分？
- [ ] SSE / WebSocket 是否重新执行对象权限检查？
- [ ] Realtime Message 是否只是传输层，而非隐藏写库通道？

---

## 35. 推荐公共示例

### 35.1 List

```http
GET /api/v2/resources?limit=50&sort=-updated_at
Authorization: Bearer ...
X-Request-ID: req_example
```

```json
{
  "items": [
    {
      "id": "019c5f1a-7a2b-7abc-8def-0123456789ab",
      "type": "ANIME",
      "title": "Example",
      "created_at": "2026-08-30T17:07:27Z",
      "updated_at": "2026-08-31T00:10:00Z"
    }
  ],
  "page": {
    "limit": 50,
    "next_cursor": null,
    "has_more": false
  }
}
```

### 35.2 Conditional Update

```http
PATCH /api/v2/resources/019c5f1a-7a2b-7abc-8def-0123456789ab
Content-Type: application/merge-patch+json
If-Match: "resource-version-7"

{
  "title": "New Title"
}
```

若版本已经变化：

```http
HTTP/1.1 412 Precondition Failed
Content-Type: application/problem+json
```

### 35.3 Business Action

```http
POST /api/v2/tasks/019c5f1a-7a2b-7abc-8def-0123456789ab/actions/complete
Idempotency-Key: d3b0c5a0-...
```

同步完成可以：

```http
HTTP/1.1 200 OK
```

若需要异步执行：

```http
HTTP/1.1 202 Accepted
Location: /api/v2/background-tasks/019c...
```

### 35.4 Validation Error

```json
{
  "type": "about:blank",
  "title": "Request validation failed",
  "status": 422,
  "code": "validation.failed",
  "request_id": "req_example",
  "trace_id": "trace_example",
  "retryable": false,
  "errors": [
    {
      "field": "deadline_at",
      "code": "time.invalid_range",
      "message": "The deadline is outside the allowed range."
    }
  ]
}
```

### 35.5 Background Task

```json
{
  "id": "019c5f1a-7a2b-7abc-8def-0123456789ab",
  "type": "IMPORT",
  "status": "RUNNING",
  "progress": {
    "current": 42,
    "total": 100,
    "unit": "ITEMS",
    "percent": 42
  },
  "attempt": 1,
  "retryable": null,
  "created_at": "2026-08-31T00:00:00Z",
  "started_at": "2026-08-31T00:00:01Z",
  "ended_at": null
}
```

---

## 36. 核心结论

Ikaros V2 API 的核心不是“统一一套 Controller 写法”，而是建立稳定的产品契约边界。

最终应保持：

```text
Client / Plugin / Integration
            ↓
      Stable HTTP API
            ↓
Authentication / Authorization / Step-up
            ↓
    Command / Capability
            ↓
       Domain Owner
            ↓
Business State + Durable Event
```

并同时满足：

```text
Internal Entity Identity
→ UUIDv7

Actual Time Point
→ RFC 3339 with timezone

Default Application Timezone
→ UTC+8

Deployment / Isolation Boundary
→ Ikaros Instance

Cross-domain Write
→ Public Command, not direct Schema access

Sensitive Data
→ Classification-aware minimization

Secure Domain
→ Ciphertext boundary

Long-running Operation
→ 202 + queryable execution state

Retry
→ Idempotency + Attempt History

Concurrency
→ ETag / Revision / explicit Conflict

Contract Evolution
→ Versioned + backward-compatible + explicit deprecation

Optional Feature Detection
→ Capability Discovery
```

这套约定应成为后续各业务子系统 HTTP API、OpenAPI、SDK 和客户端实现的共同基础。