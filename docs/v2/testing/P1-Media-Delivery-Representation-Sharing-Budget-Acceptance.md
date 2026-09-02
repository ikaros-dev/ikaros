# Ikaros V2 P1 Media Delivery Representation / Sharing / Budget Acceptance

| 项目 | 内容 |
|---|---|
| 适用版本 | Ikaros V2 |
| 状态 | Draft / P1 Acceptance |
| 依赖设计 | `../Media-Delivery-Representation-Sharing-Budget-P1-Design.md` |

---

## 1. Representation 生命周期

| ID | 场景 | 必须结果 |
|---|---|---|
| REP-001 | Source MKV 为 ARCHIVE，Derived 1080p 为 WARM | Derived 可以独立播放 |
| REP-002 | Derived Representation 被删除 | Source Attachment / Blob 不受影响 |
| REP-003 | Packaging Profile 升级产生 v2 | v1 / v2 Cache Identity 不冲突 |
| REP-004 | v1 仍有 Active Delivery Lease | v1 不得进入破坏性 GC |
| REP-005 | Manifest v2 | 不能引用 v1 Segment |
| REP-006 | Derived 丢失且 Source 已归档 | 系统返回可解释 `RESTORING_SOURCE / PREPARING_VARIANT` 流程 |

---

## 2. Share / Guest Delivery

| ID | 场景 | 必须结果 |
|---|---|---|
| SHR-001 | Share 只有 stream/read capability | 可以播放，不可签发 download grant |
| SHR-002 | Share 同时有 download | 可以按策略签发下载 Grant |
| SHR-003 | Share expires in 5 min，默认 Grant TTL 15 min | Grant expiry <= 5 min |
| SHR-004 | Share revoked | 新 Grant 立即拒绝 |
| SHR-005 | Active Grant 无 immediate revoke capability | 按 Provider Revocation Contract 在剩余 TTL 内收敛 |
| SHR-006 | Guest 知道 CDN cache path 但没有有效 Share Principal | 401/403，不因 Cache Hit 放行 |
| SHR-007 | Watch Party member 无目标媒体访问 capability | 可以保留 Room Membership，但不能读取媒体字节 |

---

## 3. Delivery Traffic Budget

| ID | 场景 | 必须结果 |
|---|---|---|
| BUD-001 | CDN monthly soft budget 达阈值 | 产生告警，活跃播放默认不中断 |
| BUD-002 | Origin bytes 异常升高 | 产生 origin traffic alert |
| BUD-003 | Server Proxy hard byte limit 达到 | 新的隐式媒体 Proxy 请求被拒绝 |
| BUD-004 | Server Proxy hard concurrent streams limit 达到 | 新隐式 Proxy stream 被拒绝；既有流按策略处理 |
| BUD-005 | Soft Budget 超限且有 auto-prefetch | 可以暂停 / 降低非必要 Prefetch |
| BUD-006 | Metrics 汇总 | CDN / DIRECT / Origin / SERVER_PROXY bytes 不重复归类 |

---

## 4. Immutable Representation / Purge

| ID | 场景 | 必须结果 |
|---|---|---|
| IMM-001 | 发布新转码 | 创建新 Representation Version，不覆盖旧字节 |
| IMM-002 | 普通新版本发布 | 不要求 CDN Purge 才保证一致性 |
| IMM-003 | Emergency Purge | 需要显式运维动作并产生 Audit |
| IMM-004 | Purge Log / Event | 不含完整 Signed URL / Raw Token |
| IMM-005 | CDN 仍缓存 v1，客户端请求 v2 | v2 使用独立 Cache Identity，不命中 v1 内容 |

---

## 5. Required E2E

### E2E-P1-REP — Archived Source / Hot Derived

1. 导入 Source；
2. 生成 Derived Representation；
3. 将 Source Demote 到 Archive；
4. 保持 Derived WARM；
5. 播放 Derived；
6. 验证没有触发 Source Restore；
7. 删除 Derived；
8. 再次播放时进入 Restore Source / Prepare Variant 或选择其他 Variant。

### E2E-P1-SHARE — Guest Streaming

1. 创建 stream-only Share；
2. Guest Token exchange；
3. 建立 Guest Principal；
4. 获取播放 Grant；
5. 尝试 download Grant，必须拒绝；
6. 撤销 Share；
7. 新播放 Grant 拒绝；
8. 验证 Cache Hit 仍受授权。

### E2E-P1-BUDGET — Low-bandwidth Guard

1. 配置 LOW_BANDWIDTH profile；
2. 配置很低的 Server Proxy hard limit；
3. 使 CDN 和 DIRECT 不可用；
4. 尝试播放；
5. 系统拒绝 Server Proxy fallback；
6. Dashboard / Audit 能解释 Hard Guard 原因；
7. Server Proxy bytes 未越过策略上限。
