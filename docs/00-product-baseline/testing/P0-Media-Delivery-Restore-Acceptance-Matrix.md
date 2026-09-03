# Ikaros V2 P0 Media Delivery / Restore Acceptance Matrix

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Media Delivery / Restore Acceptance Matrix |
| 适用版本 | Ikaros V2 |
| 状态 | Draft / Acceptance Contract |
| 基线 | `P0-Acceptance-Invariant-Test-Matrix.md` |
| Schema | `../database/P0-Media-Delivery-Restore-Schema-Addendum.md` |
| Contract | `../contracts/P0-Media-Delivery-Restore-Contract-Addendum.md` |
| OpenAPI | `../contracts/openapi-v2-p0-media-delivery-restore.yaml` |

> 本矩阵是 P0 Acceptance 基线的规范性扩展。所有新增 Delivery / Restore P0 能力必须有自动化证据；不能因为功能依赖外部 CDN / Object Storage 就只保留手工测试。

---

## 1. Test Levels

使用：

- `UNIT`：纯领域 / Policy；
- `DB`：PostgreSQL Constraint / Transaction；
- `CONTRACT`：Provider Contract Test；
- `INTEGRATION`：Application + PostgreSQL + Fake Provider；
- `E2E`：HTTP / Auth / Playback flow；
- `CHAOS`：Provider failure / retry / race；
- `SECURITY`：授权、Token、Secret、日志泄漏。

P0 最少需要 Fake Storage Provider + Fake Delivery Provider，以便 CI 不依赖真实云厂商账户。

---

## 2. Schema / Persistence

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-DB-001 | DB | 创建 Delivery Provider | provider_key 唯一；Secret 只能以 Reference 输入 |
| MDR-DB-002 | DB | 同 Storage Provider 重复绑定同 Delivery Provider | 唯一约束拒绝重复 Binding |
| MDR-DB-003 | DB | Delivery Provider `provider_type` 非法 | CHECK 拒绝 |
| MDR-DB-004 | DB | Restore Request 相同 actor + idempotency key | 唯一约束 / Application 返回同一 Request |
| MDR-DB-005 | DB | Restore Operation 相同 `operation_key` 并发创建 | 最终只有一个 Operation |
| MDR-DB-006 | DB | 两个 Request Item 指向同 Operation | 合法；支持 Request 合并 |
| MDR-DB-007 | DB | Lease 无有限 `expires_at` | 拒绝 |
| MDR-DB-008 | DB | Restore Budget 负数字节数 | CHECK 拒绝 |
| MDR-DB-009 | DB | INSTANCE Budget Policy 存在两个实例级记录 | partial unique constraint 拒绝 |
| MDR-DB-010 | DB | 删除仍被 Request Item 引用的 Restore Operation | RESTRICT |

---

## 3. Restore Idempotency / Dedup

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-RST-001 | INTEGRATION | 同一个 HTTP `Idempotency-Key` 连续请求恢复单集 | 返回同一 Restore Request ID |
| MDR-RST-002 | CHAOS | 两客户端同时恢复同一归档 Attachment | 可有两个 Request，但只有一个 Active Restore Operation |
| MDR-RST-003 | CHAOS | 一个客户端恢复本集，同时另一个恢复本季 | 重叠 Placement 共享 Operation |
| MDR-RST-004 | INTEGRATION | Placement 已 `READY_TEMPORARILY` | 不提交新 Provider Restore，Item 直接 Ready |
| MDR-RST-005 | INTEGRATION | Placement 已立即可读 | Restore Item 直接 `READY` |
| MDR-RST-006 | CHAOS | Worker 在 Provider 接收 Restore 后崩溃 | Retry 复用 operation_key / provider request ref，不重复收费型提交 |
| MDR-RST-007 | INTEGRATION | Background Task Retry | 不创建新 Restore Request |
| MDR-RST-008 | INTEGRATION | 取消一个共享 Operation 的 Request | 不影响仍依赖该 Operation 的其他 Request |
| MDR-RST-009 | CONTRACT | Provider 不支持取消 | Request 可取消等待；Operation 自然完成，不标 Failed |
| MDR-RST-010 | INTEGRATION | Season Restore 部分失败 | Request -> PARTIAL，失败 Item 可重试 |

---

## 4. Restore Budget / Cost Guard

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-BUD-001 | UNIT | 单 Request 超 `max_bytes_per_request` + REJECT | 服务端拒绝，不创建 Provider Operation |
| MDR-BUD-002 | E2E | REQUIRE_CONFIRMATION 且无确认 token | 返回明确 Problem，不静默执行 |
| MDR-BUD-003 | E2E | 合法确认后重试 | Request budget_decision=`CONFIRMED` |
| MDR-BUD-004 | INTEGRATION | PARTIAL_ACCEPT | 只接受预算内 Item，剩余 Item 标 REJECTED/未创建 Operation |
| MDR-BUD-005 | INTEGRATION | QUEUE_AFTER_BUDGET_RESET | Request 可进入 QUEUED，不提前调用 Provider |
| MDR-BUD-006 | INTEGRATION | 自动预热超过 trigger bytes | 自动停止预热，不要求用户交互确认 |
| MDR-BUD-007 | CHAOS | 两 Request 共享一个 Restore Operation | provider restore bytes 预算只计一次 |
| MDR-BUD-008 | INTEGRATION | 达到 max concurrent operations | 新 Operation 排队或拒绝，不能越限提交 |
| MDR-BUD-009 | INTEGRATION | 达到 max concurrent bytes | 同上 |
| MDR-BUD-010 | UNIT | Provider / Policy override | 按明确优先级计算 effective budget |

---

## 5. Cache Identity / CDN Authorization

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-CDN-001 | CONTRACT | 同 Blob 两个不同 token URL | Authorization 均验证；Normalized Cache Identity 相同 |
| MDR-CDN-002 | CONTRACT | expires/signature 改变 | 默认不改变 Cache Identity |
| MDR-CDN-003 | SECURITY | 无 token 请求一个已缓存对象 | 仍返回 401/403，不能因 Cache Hit 绕过授权 |
| MDR-CDN-004 | SECURITY | 非法 token + Cache Hit | 401/403 |
| MDR-CDN-005 | CONTRACT | Representation 不同 | Cache Identity 不同 |
| MDR-CDN-006 | CONTRACT | Content-Encoding 变体不同 | 根据 policy 正确 Vary，不能错发字节 |
| MDR-CDN-007 | CONTRACT | Provider 无法排除 auth query | Capability 标记 auth-coupled，并产生配置风险 |
| MDR-CDN-008 | INTEGRATION | Grant 续签后继续 Range 播放 | 新 URL 可续播，Origin/Cache Identity 稳定 |
| MDR-CDN-009 | SECURITY | Cache Key normalized | 不包含 user ID / session ID / trace ID / raw token |
| MDR-CDN-010 | CONTRACT | Range cache miss | Origin 请求遵守 Range Policy，不无界请求整个 Archive Object |

---

## 6. Delivery Grant Security / Revocation

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-GRT-001 | SECURITY | Issue Grant | 每次重新检查当前 Attachment Permission |
| MDR-GRT-002 | SECURITY | Permission 已撤销后请求新 Grant | 403 |
| MDR-GRT-003 | SECURITY | Provider revocation mode=IMMEDIATE | 撤销后旧 Grant 立即失败 |
| MDR-GRT-004 | SECURITY | mode=KEY_VERSION_BOUND，Emergency Rotate | 旧 key version Grant 失效 |
| MDR-GRT-005 | SECURITY | mode=TTL_BOUNDED | 旧 Grant 最长只存活到声明 TTL；新 Grant 拒绝 |
| MDR-GRT-006 | SECURITY | 高敏感 Classification 不接受 Provider revocation level | Delivery Resolution 拒绝该 Provider |
| MDR-GRT-007 | SECURITY | App / Server 普通日志 | 不出现完整 Signed URL / token / signature |
| MDR-GRT-008 | SECURITY | Durable Event payload | 不出现完整 Signed URL / Secret |
| MDR-GRT-009 | SECURITY | Analytics | 只记录 provider/grant id/bytes/result，不记录 raw URL |
| MDR-GRT-010 | CONTRACT | Grant method | 只允许最小必要 Method，默认 GET |

---

## 7. Delivery Lease

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-LSE-001 | E2E | 播放开始签发 Grant | 创建或续租 PLAYBACK Lease |
| MDR-LSE-002 | INTEGRATION | Active Lease + 自动 Demotion | Demotion 被阻止或延后 |
| MDR-LSE-003 | INTEGRATION | Active Lease + 临时 Restore Copy 清理 | 唯一可读临时副本不被清理 |
| MDR-LSE-004 | INTEGRATION | Active Lease + Provider Drain | 等待 Lease 或先建立替代可读路径 |
| MDR-LSE-005 | INTEGRATION | Active Lease + GC | GC eligibility=false |
| MDR-LSE-006 | E2E | 客户端定期续签 Grant/Lease | expires_at 滑动前进 |
| MDR-LSE-007 | CHAOS | 客户端崩溃停止心跳 | Lease 自动过期，无永久 Hold |
| MDR-LSE-008 | E2E | 正常结束播放 | Lease 可以主动 release |
| MDR-LSE-009 | INTEGRATION | Lease Renew | 不产生每次 Durable Event |
| MDR-LSE-010 | SECURITY | A 用户尝试续租 B 用户 Lease | 403 |

---

## 8. Delivery Fallback

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-FBK-001 | E2E | CDN healthy | 使用 CDN，不经过 Server Proxy |
| MDR-FBK-002 | E2E | CDN unhealthy + DIRECT available | fallback 到 DIRECT |
| MDR-FBK-003 | E2E | CDN unhealthy + DIRECT unavailable + low-bandwidth profile | FAIL / 503，不自动走 Server Proxy |
| MDR-FBK-004 | E2E | 管理员显式配置 SERVER_PROXY fallback | 只有此时允许走 Server Proxy |
| MDR-FBK-005 | SECURITY | CDN 返回 PermissionDenied | 不 fallback 到 DIRECT 绕过权限 |
| MDR-FBK-006 | SECURITY | ClassificationDenied | 不 fallback |
| MDR-FBK-007 | INTEGRATION | Blob Corrupt | 不 fallback 隐藏完整性故障 |
| MDR-FBK-008 | CHAOS | CDN ↔ DIRECT 配置形成环 | Resolver 用 attempted-provider set 终止循环 |
| MDR-FBK-009 | INTEGRATION | Provider capability 不支持 Range | 尝试明确允许且支持 Range 的下一 Provider，否则失败 |
| MDR-FBK-010 | OBSERVABILITY | low-bandwidth profile 正常使用 | `Server Proxy Bytes` 接近 0；异常升高可告警 |

---

## 9. Availability / UX Contract

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-AVL-001 | E2E | 只有 Archive Placement | `RESTORE_REQUIRED`，不是 404/500 |
| MDR-AVL-002 | E2E | Operation Running | `RESTORING` + Restore Request ref |
| MDR-AVL-003 | E2E | 临时恢复可读 | `READY_TEMPORARILY` + expiry |
| MDR-AVL-004 | E2E | 有长期可读 Placement | `READY` |
| MDR-AVL-005 | E2E | Blob Missing | `MISSING` |
| MDR-AVL-006 | E2E | Blob Corrupt | `CORRUPTED` |
| MDR-AVL-007 | E2E | Provider 给 12–24h range | API / UI 保留范围，不伪造精确 ETA |
| MDR-AVL-008 | E2E | Restore 完成 | Attachment ID 不变化即可播放 |

---

## 10. OpenAPI / Permission Contract

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-API-001 | CONTRACT | OpenAPI parse | `openapi-v2-p0-media-delivery-restore.yaml` 是有效 OpenAPI 3.1 |
| MDR-API-002 | CONTRACT | operationId registry | operationId 与 Contract Addendum 一致且唯一 |
| MDR-API-003 | CONTRACT | Idempotency-Key | Restore / create provider / probe 等需要的操作标明 Required |
| MDR-API-004 | SECURITY | 普通用户访问 admin delivery API | 403 |
| MDR-API-005 | SECURITY | 用户恢复无权读取的 Season | 403 且不创建 Request |
| MDR-API-006 | SECURITY | storage.restore.manage | 可管理任务但不自动获得内容播放权限 |
| MDR-API-007 | DB | Permission seed 重放 | deterministic、无重复、结果一致 |
| MDR-API-008 | CONTRACT | Problem Codes | stable error code 与 Catalog 一致 |

---

## 11. Observability / Redaction

| ID | Level | Scenario | Expected |
|---|---|---|---|
| MDR-OBS-001 | INTEGRATION | CDN delivery | 记录 delivered bytes / origin bytes，不记录 raw token |
| MDR-OBS-002 | INTEGRATION | Shared Restore Operation | Provider Restore Bytes 只计一次 |
| MDR-OBS-003 | INTEGRATION | Restore Request | logical requested bytes 可按 Request 统计 |
| MDR-OBS-004 | SECURITY | Exception 含 signed URL | Logger redaction 移除 signature/token |
| MDR-OBS-005 | SECURITY | Provider SDK error body 含 secret | 安全摘要，不写原始 secret body |
| MDR-OBS-006 | INTEGRATION | Delivery Provider degraded | Health 与 Storage Provider Health 分离 |

---

## 12. Required P0 E2E Scenarios

### E2E-A — Low-bandwidth CDN Playback

```text
Private Object Storage
+ CDN Delivery Provider
+ Server Media Proxy OFF
+ Server Media Cache OFF

Client -> Ikaros issue grant
Client -> CDN Range GET
CDN -> Origin on miss
```

验收：

- 能播放 / Seek；
- Ikaros Server 不传输媒体正文；
- Server Proxy Bytes = 0；
- URL 续签后可继续播放。

### E2E-B — Restore Same Episode Race

两个客户端同时恢复同一 Episode：

- 两个业务 Request 可以存在；
- 一个 Placement 只产生一个 Provider Restore Operation；
- 两个 Request 最终都收到 Ready 状态；
- Provider Restore Bytes 只计一次。

### E2E-C — Restore Season with Budget

Season 总量超过 `max_bytes_per_request`：

- Policy=REQUIRE_CONFIRMATION 时先拒绝并提示确认；
- 确认后允许；
- 自动预热不弹确认，而是停止在预算内；
- 部分失败可重试失败 Item。

### E2E-D — Permission Revoked During Playback

- 播放期间撤销 Resource 权限；
- 新 Grant 立即拒绝；
- 旧 Grant 按 Provider `revocation_mode` 执行；
- mode=TTL_BOUNDED 时最大暴露窗口等于剩余 TTL；
- 日志不包含旧 Grant URL。

### E2E-E — CDN Failure on Low-bandwidth Profile

- CDN 故障；
- DIRECT 可用 -> 使用 DIRECT；
- DIRECT 也不可用 -> 503；
- 禁止自动 fallback Server Proxy。

### E2E-F — Active Playback vs Tiering

- 正在播放临时恢复媒体；
- 自动 Demotion / temporary cleanup 同时触发；
- Active Lease 阻止失去唯一可读来源；
- Lease 过期后下一次 Reconcile 才允许清理。

---

## 13. P0 Exit Gate

Media Delivery / Restore P0 只有在以下条件全部满足后才算完成：

- Schema Migration + Constraints 有自动化测试；
- Command / Query / Event / Permission seed 已实现；
- OpenAPI 与 Controller Contract 一致；
- Fake Storage + Fake Delivery Provider Contract Suite 通过；
- Restore race dedup 通过；
- Budget hard guard 通过；
- Cache Identity / Auth separation 通过；
- Permission / Grant Revocation 测试通过；
- Lease protection 测试通过；
- low-bandwidth fallback 测试通过；
- Secret / Signed URL redaction 测试通过；
- Required E2E A–F 全部通过。
