# Ikaros V2 P0 Media Delivery / Restore Reliability Acceptance

| 项目 | 内容 |
|---|---|
| 文档名称 | P0 Media Delivery / Restore Reliability Acceptance |
| 适用版本 | Ikaros V2 |
| 状态 | Draft / P0 Acceptance Extension |
| 依赖设计 | `../Media-Delivery-Restore-Reliability-Addendum.md` |

> 本矩阵补充 Delivery Binding 粘性、Circuit Breaker / Hysteresis、Selection Epoch 与 Restore Reconciliation 的 P0 自动化验收要求。

---

## 1. Delivery Binding 粘性

| ID | 场景 | 必须结果 |
|---|---|---|
| DREL-001 | 同一 Playback Session 连续 100 次 Range / Grant Renewal，Provider 健康 | `binding_id` 与 `selection_epoch` 保持稳定 |
| DREL-002 | 单次 CDN 502 后下一次请求成功 | 不发生 Binding 切换 |
| DREL-003 | 在 failure window 内达到阈值 | Circuit OPEN，创建新 Selection Epoch 并按 fallback policy 选择下一 Binding |
| DREL-004 | Circuit OPEN 期间 | 不把普通媒体请求继续发送到已 Open Provider |
| DREL-005 | cooldown 到期进入 HALF_OPEN | 只有受控 Probe，不能全量恢复流量 |
| DREL-006 | recovery success threshold 满足 | Circuit CLOSED，允许后续新 Session 选择；现有健康 Session 不要求强制迁回 |
| DREL-007 | ACL denied | 不触发 DIRECT / SERVER_PROXY fallback 绕过授权 |
| DREL-008 | low-bandwidth profile 下 CDN + DIRECT 均失败 | 明确失败，`server_proxy_bytes == 0` |

---

## 2. Selection Epoch

| ID | 场景 | 必须结果 |
|---|---|---|
| DREL-010 | 当前 Binding 不变，仅 Grant 到期续签 | Epoch 不变 |
| DREL-011 | Binding 从 CDN-A 切到 CDN-B | 新 Epoch，Grant 绑定新 Binding |
| DREL-012 | 客户端带旧 Epoch 请求新 Grant | 服务端返回当前 Epoch / 新 Grant，不继续为旧 Binding 续签 |
| DREL-013 | 新旧 Provider 的 Representation identity 一致 | 可以从当前播放 offset Range 续播 |
| DREL-014 | 新 Provider 返回不同 content identity / length | 禁止静默续播，重新做 Playback Source Resolution |

---

## 3. Restore Crash Recovery

| ID | 崩溃窗口 | 必须结果 |
|---|---|---|
| RREC-001 | Provider 已接受 Restore，但本地尚未标记 accepted 时进程崩溃 | 重启 Reconcile 先查 Provider，不无条件重复提交 Restore |
| RREC-002 | Provider 已完成 Restore，本地尚未 commit success 时崩溃 | Reconcile 将 Placement / Operation 收敛为成功 |
| RREC-003 | Background Attempt 本地失败，但 Provider 后续异步完成 | 保留失败 Attempt 历史；Placement 根据 Provider Truth 修正为可读 |
| RREC-004 | Provider Query 临时 timeout | 不把 RESTORING 直接改成 FAILED |
| RREC-005 | `RESTORING` 超过预计时间范围 | 自动进入 Reconcile 路径并产生 stuck metric |

---

## 4. Restore 外部状态变化

| ID | 场景 | 必须结果 |
|---|---|---|
| RREC-010 | 管理员在云控制台直接 Restore | Reconcile 能发现可读状态；不伪造用户 Restore Request |
| RREC-011 | 临时 Restore Copy 到期时间已过，但 Provider 仍可读 | 不仅凭时间戳降级；刷新物理事实 |
| RREC-012 | 临时 Restore Copy 已不可读 | Availability 回到 `RESTORE_REQUIRED` |
| RREC-013 | 归档对象已被外部 Promotion 到即时读取 Tier | Reconcile 更新 Placement 可用性，不再次 Restore |

---

## 5. Reconcile 幂等 / 并发

| ID | 场景 | 必须结果 |
|---|---|---|
| RREC-020 | 同一 Operation 连续执行两次 Reconcile | 第二次无额外 Provider Restore 副作用 |
| RREC-021 | 两个 Worker 同时尝试 Reconcile | 只有一个成功拥有本次写入 / CAS；最终状态一致 |
| RREC-022 | Reconcile 修正状态并写 Event | 状态修正与 Outbox Event 同事务 |
| RREC-023 | Reconcile 后重放 Consumer | Inbox / Event 消费保持幂等 |

---

## 6. Required E2E

### E2E-R1 — Playback Failover

1. 创建 Playback Session；
2. 首选 CDN-A；
3. 连续 Range 正常并确认 Sticky Binding；
4. 注入足够 CDN-A 5xx 达到 threshold；
5. Circuit OPEN；
6. 切换 CDN-B / DIRECT；
7. Selection Epoch 改变；
8. 从当前 offset 继续播放；
9. 期间权限持续校验；
10. low-bandwidth 配置下无 Server Proxy bytes。

### E2E-R2 — Restore Crash Window

1. 对归档 Placement 发起 Restore；
2. Mock Provider 接受请求；
3. 在本地状态完成前强制终止 Worker；
4. 重启；
5. Reconcile Query Provider；
6. 不重复发出第二次 Provider Restore；
7. Provider 完成后 Placement 收敛可读；
8. 原 Restore Request 正确完成。

### E2E-R3 — External Restore

1. Ikaros 记录对象为 `RESTORE_REQUIRED`；
2. Provider fixture 在 Ikaros 外部改变为 restored；
3. 周期 Reconcile；
4. Availability 变为可读；
5. 产生 externally-observed audit/event；
6. 不生成虚假 actor request。
