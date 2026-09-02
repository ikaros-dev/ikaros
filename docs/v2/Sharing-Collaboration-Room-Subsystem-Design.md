# Ikaros V2 Sharing / Collaboration / Room 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Sharing / Collaboration / Room 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Security-Identity-Authorization-Crypto-Subsystem-Design.md`、`Platform-Integration-Automation-Design.md`、`Core-Resource-Library-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 的分享、协作与 Room 实时会话领域。它负责回答“谁可以通过什么方式访问某个对象”“多人如何进入同一个协作上下文”“实时状态如何在断线、重连和多客户端之间保持可解释一致性”。
>
> 本文档不把 WebSocket、SSE 或 WebRTC 当成业务领域本身。协议只是传输方式，真正的业务真相仍由 Share、Membership、Room State、Sequence、Permission 与持久化事件定义。

---

## 1. 设计目标

Sharing / Collaboration / Room 子系统需要解决：

1. Resource、Collection、Document、Playlist 等对象如何安全分享给其他用户或外部访问者。
2. Share Link、邀请、成员资格和 Resource ACL 如何分工，避免出现多套互相冲突的授权体系。
3. Room 如何作为临时或长期协作空间承载一起看、一起听、协作文档、评论和聊天等场景。
4. 多客户端并发操作时，哪些状态由服务端权威决定，哪些只是客户端临时状态。
5. WebSocket 断线重连后如何恢复状态，而不是依赖“重新打开页面就算同步”。
6. Presence、Typing、Cursor 等瞬时状态与持久化业务状态如何分离。
7. Room Event 如何有序、可重放、可去重，并与平台 Integration Event 区分。
8. Share Token、Guest、Plugin、Automation 等非普通登录用户主体如何进入权限模型。
9. 房主离开、成员被移除、资源被删除、权限被撤销时，实时连接和访问能力如何立即收敛。
10. 如何在不引入过早微服务化的前提下，为未来独立 Realtime Gateway / Worker 保留扩展空间。

核心原则：

> **ACL 决定“能不能访问对象”，Share 决定“如何授予访问”，Room 决定“在一个协作上下文里如何共同活动”。三者相关但不能合并成一个万能模型。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Share 定义与生命周期；
- Share Link / Share Token；
- 用户邀请与接受流程；
- Room 与 Room Membership；
- Owner / Moderator / Member / Guest 等 Room 角色语义；
- Presence 与连接状态；
- Room 内服务端权威状态；
- Room Event Sequence、replay、reconnect；
- 评论 / 聊天的通用协作边界；
- 实时权限收敛；
- 跨客户端同步契约；
- 与 Notification、Automation、Audit、Analytics 的事件联动。

### 2.2 本子系统不负责

- Resource 本体、Collection 本体或 Document 内容的领域规则；
- 用户身份认证、Session、RBAC 的底层实现；
- 视频解码、媒体转码、字幕解析；
- 文档 CRDT / OT 算法细节；
- Notification Provider 的投递实现；
- WebRTC SFU / TURN 等媒体基础设施的最终选型；
- 全局社交网络、关注关系或公共社区推荐。

专业领域通过公开 Capability / Command 接入 Room，而不是把自己的内部状态交给 Room 子系统直接修改。

---

## 3. 核心不变量

1. **访问授权最终在服务端判定**：客户端隐藏按钮不能代替 ACL / Share 校验。
2. **Share 不是第二套 Resource ACL**：Share 只表达一个受控授权入口，最终有效权限应解析为明确 Principal + Capability。
3. **Token 只保存摘要**：可作为凭据的 Share Token 不应以可直接使用的明文长期存储。
4. **Room 必须有明确所有者或治理策略**：不能存在无人负责、权限无法收敛的永久 Room。
5. **持久化状态与 Presence 分离**：成员资格、播放位置、评论属于业务状态；在线、光标、正在输入属于瞬时状态。
6. **服务端权威状态必须版本化 / 有序化**：实时客户端不得通过本地最后写入者直接覆盖共享状态。
7. **断线重连必须可恢复**：连接断开不等于用户退出 Room，重连后应通过 Snapshot + Replay 或等价机制恢复。
8. **权限撤销必须影响现有连接**：用户被移出 Room、Share 被撤销或 Resource 权限被移除后，已有实时连接必须在可接受延迟内失效。
9. **Room 不绕过目标领域**：例如同步播放只能调用 Media 的播放状态契约，不能直接改媒体内部表。
10. **外部 Guest 权限最小化**：匿名或外部访问默认只获得 Share 明确授予的最小能力。
11. **Room Event 不是全局 Integration Event**：高频实时事件不应全部进入平台持久 Outbox；只有需要跨域传播的业务事实才升级为 Integration Event。
12. **重试不能产生重复副作用**：Invite 接受、加入 Room、发表评论、同步 Command 等必须有清晰幂等键或去重规则。

---

## 4. 核心领域概念

### 4.1 Share

Share 表示“对象所有者或有授权的主体，向另一个 Principal 或外部访问入口授予受限访问”。

Share 至少表达：

- `id`：UUIDv7；
- target type / target id；
- issuer / owner；
- grantee type；
- granted capabilities；
- access mode；
- expiration；
- status；
- created_at / updated_at；
- 可选访问次数 / 风险限制。

Target 可以是：

- Resource；
- Collection；
- Document；
- Playlist；
- Room；
- 其他明确支持分享的业务对象。

Share 目标通过稳定公开 ID 表达，不直接引用其他子系统私有表结构。

### 4.2 Share Grantee

建议支持：

- USER；
- GROUP / ROLE（仅在明确需要时）；
- EMAIL_INVITEE；
- LINK_TOKEN；
- GUEST_SESSION；
- PLUGIN / SERVICE PRINCIPAL（少量受控场景）。

“知道 URL 的所有人”本质上也是 Token Principal，不应在授权模型中被当成无身份请求。

### 4.3 Share Capability

Share 只授予目标对象支持的能力，例如：

- `read`
- `comment`
- `download`
- `edit`
- `manage_members`
- `join_room`
- `control_playback`

最终命名应与 Security / Permission Registry 保持一致。

Share 不能授予目标领域根本不存在的能力。

### 4.4 Invite

Invite 是“期望某个主体加入协作关系”的待确认记录。

典型状态：

- PENDING；
- ACCEPTED；
- DECLINED；
- EXPIRED；
- REVOKED。

Invite 被接受后，产生正式 Membership / Share Grant；Invite 本身不应永久充当授权事实。

### 4.5 Room

Room 表示一个多人协作上下文。

可用于：

- 一起看视频；
- 一起听音乐；
- 共同阅读 / 演示；
- 文档协作；
- 讨论某个 Resource / Collection；
- 临时协作会话。

Room 至少具有：

- UUIDv7；
- kind；
- owner；
- target context；
- visibility；
- lifecycle status；
- state version / sequence；
- created_at / updated_at；
- 可选 expires_at。

Room Kind 应稳定命名，例如：

- `watch_party`
- `listen_together`
- `document_collaboration`
- `discussion`
- `generic`

插件扩展 Room Kind 必须使用命名空间。

### 4.6 Room Membership

Membership 表达一个 Principal 与 Room 的持久业务关系。

角色可包括：

- OWNER；
- MODERATOR；
- MEMBER；
- GUEST。

Role 只定义 Room 内能力，不替代平台 RBAC。

一个用户可以在平台上是普通 User，但在某个 Room 中是 Owner；反之平台管理员也不自动成为任意 Room 成员。

### 4.7 Presence

Presence 是瞬时连接状态，不是 Membership。

Presence 可表达：

- online / away；
- active connection count；
- device / client 摘要；
- last heartbeat；
- typing；
- cursor / viewport；
- transient playback readiness。

Presence 默认可丢失、可重建，不应成为必须恢复的数据库真相。

### 4.8 Comment / Chat Message

评论与聊天都属于协作消息，但生命周期不同：

- Comment 通常绑定目标对象或位置，长期存在，可编辑 / 删除 / 审计；
- Chat Message 通常绑定 Room Timeline，强调时间顺序，可以按策略保留或过期。

是否合用底层 Message 基础设施由实现决定，但领域语义应保持可区分。

---

## 5. Share 生命周期

建议状态：

```text
ACTIVE
  ├──> EXPIRED
  ├──> REVOKED
  └──> CONSUMED   （仅一次性 Share）
```

规则：

1. 创建 Share 前必须验证 issuer 对目标对象具有再授权能力。
2. 被分享对象权限变化后，Share 有效性必须重新评估；Share 不能“冻结”高于当前 Owner 权限的能力。
3. Share 到期或撤销后，新的访问立即拒绝。
4. 已通过 Share 建立的实时连接必须触发权限收敛。
5. 一次性 Token 成功兑换后进入 CONSUMED，重复兑换不得再次产生成员关系。
6. 修改 Share Capability 应创建可审计的权限变化，而不是静默覆盖。

---

## 6. Share Token 安全

公开链接的 Token 必须使用高熵随机值。

持久化建议保存：

- token id；
- token hash / digest；
- prefix / fingerprint（用于后台识别）；
- expires_at；
- last_used_at；
- use_count；
- risk metadata。

禁止：

- 数据库保存可直接复制使用的完整 Token；
- 日志输出完整 Token；
- Analytics、Trace、Error Message 携带完整 Token；
- 把 Token 放入可被第三方 Referer 泄露的页面资源 URL。

高风险 Share 可以支持：

- 密码 / 二次验证码；
- 最大访问次数；
- IP / 网络限制（可选）；
- 强制登录；
- 禁止下载；
- Step-up Verification 后创建。

---

## 7. Room 生命周期

建议状态：

- OPEN；
- LOCKED；
- ENDED；
- EXPIRED；
- DELETED。

典型流程：

```text
Create Room
    ↓
OPEN
 ├── Join / Leave
 ├── Lock / Unlock
 └── End
      ↓
    ENDED
```

规则：

1. ENDED Room 不接受新的共享状态修改，但可以按权限读取历史。
2. 临时 Room 可设置 TTL，到期后进入 EXPIRED。
3. Owner 离开连接不等于 Room 自动结束；Membership 与 Presence 必须分离。
4. Owner 永久退出前应明确：转移 Owner、结束 Room 或按策略自动选举。
5. 删除 Room 不能自动删除其关联 Resource / Document / Attachment。

---

## 8. Room 权限模型

有效权限建议由以下信息共同计算：

```text
Platform Identity
      +
Target Object ACL
      +
Share Grant
      +
Room Membership Role
      +
Room Current Policy
      =
Effective Room Capability
```

例如一起看 Room：

- OWNER：播放 / 暂停 / seek / 邀请 / 移除成员；
- MODERATOR：播放控制 / 管理普通成员；
- MEMBER：观看 / 聊天；
- GUEST：按 Share 授权观看 / 聊天；
- Spectator（若未来需要）：只读状态。

Room Role 不得自动赋予目标 Resource 的永久编辑权。

---

## 9. 实时传输职责边界

### 9.1 HTTP

HTTP 负责：

- 创建 / 查询 / 更新 Share；
- 邀请；
- Room 创建 / 结束；
- 成员管理；
- 读取 Snapshot；
- 历史消息分页；
- 权限管理；
- 需要强一致确认的 Command。

### 9.2 WebSocket

WebSocket 优先用于：

- Room 双向实时 Command；
- Presence；
- 播放同步；
- Typing / Cursor；
- 高频 Room Event。

### 9.3 SSE

SSE 可用于只有服务端推送需求的场景：

- Room 状态只读订阅；
- Notification；
- 长任务状态。

如果同一页面已经持有 WebSocket，不应为了形式统一再建立重复 SSE 连接。

### 9.4 WebRTC

WebRTC 只用于需要端到端实时媒体传输的场景，例如未来语音 / 视频通话、屏幕共享。

播放同步不等于媒体流必须走 WebRTC。对于一起看，媒体内容仍可各客户端从 Storage / CDN / Server 独立读取，只同步控制状态。

---

## 10. 服务端权威状态

Room 中需要区分三类状态。

### 10.1 Authority State

必须由服务端权威维护，例如：

- 当前播放资源；
- play / pause；
- authoritative position anchor；
- playback rate；
- current presenter；
- Room lock；
- active document revision / collaboration session；
- 成员角色。

### 10.2 Durable Collaborative State

需要持久化，例如：

- Membership；
- Comment；
- Chat（按保留策略）；
- Room lifecycle；
- 邀请；
- 权限变化；
- 重要控制历史。

### 10.3 Ephemeral State

可以丢弃 / TTL：

- Presence；
- Typing；
- Cursor；
- 当前缓冲百分比；
- 客户端鼠标位置；
- 临时网络质量。

---

## 11. Sequence、Snapshot 与 Replay

每个需要有序恢复的 Room 应维护单调递增逻辑 Sequence。

Room Event 至少包含：

- room_id；
- sequence；
- event_id；
- event_type；
- actor；
- occurred_at；
- payload；
- state_version（必要时）。

连接建立时：

```text
Client last_sequence = N
        ↓
Server 判断 replay window
        ├── 可重放 → events N+1 ... current
        └── 超出窗口 → full snapshot + current sequence
```

要求：

1. 同一 Room 的 Authority State 变化必须存在稳定顺序。
2. Sequence 是 Room 内排序语义，不使用 UUIDv7 排序代替。
3. 客户端收到旧 sequence 不得覆盖新状态。
4. 客户端检测到 sequence gap 时应请求 replay / snapshot，而不是猜测状态。
5. Replay Buffer 可以是持久化或有限窗口缓存，具体取决于 Room 类型和恢复要求。

---

## 12. 一起看 / 一起听同步模型

服务端不需要每秒广播播放位置。

建议用锚点模型：

```text
state = PLAYING
position = 125.400s
server_time = T0
rate = 1.0
```

客户端在本地估算当前目标位置：

```text
target_position = position + (now - T0) * rate
```

客户端根据误差选择：

- 小误差：轻微速率校正；
- 中误差：seek；
- 大误差 / 资源变化：重新同步。

关键 Command：

- Play；
- Pause；
- Seek；
- ChangePlaybackRate；
- ChangeResource / Episode；
- RequestControl / TransferControl（可选）。

服务端必须验证 actor 是否拥有控制权限。

---

## 13. 并发与冲突

Room Authority Command 应携带：

- command id；
- expected state version（适用时）；
- actor；
- client sequence / request timestamp（仅辅助）；
- payload。

当两个 Moderator 同时 seek：

- 服务端按 Room 的串行化规则确定顺序；
- 后提交但基于旧版本的 Command 可以返回 Conflict；
- 不允许两个客户端各自认为自己的状态是最终结果。

文档协作等需要 CRDT / OT 的专业场景，应由 Document 子系统定义操作合并语义，Room 负责连接、身份、成员、replay 和会话上下文。

---

## 14. Reconnect 与连接生命周期

客户端连接必须区分：

- authenticated session；
- room membership；
- realtime connection。

断线流程：

1. Connection 消失；
2. Presence 延迟进入 offline；
3. Membership 保留；
4. 客户端重连并重新认证；
5. 服务端重新检查 Room / Resource 权限；
6. 使用 last_sequence 恢复；
7. 更新 Presence。

禁止仅凭旧 connection token 在权限已撤销后重新加入。

---

## 15. Command 契约

典型 Command：

- CreateShare
- UpdateShareCapabilities
- RevokeShare
- RedeemShareToken
- CreateInvite
- AcceptInvite
- DeclineInvite
- RevokeInvite
- CreateRoom
- EndRoom
- LockRoom / UnlockRoom
- JoinRoom
- LeaveRoom
- AddRoomMember
- RemoveRoomMember
- ChangeRoomRole
- TransferRoomOwnership
- PublishComment
- EditComment
- DeleteComment
- SendRoomMessage
- ExecuteRoomControlCommand

要求：

- 创建 / 接受 / 兑换等操作支持幂等；
- 删除成员和撤销 Share 应触发实时权限收敛；
- 高风险 Share 创建可要求 Step-up Verification；
- 所有修改都通过目标子系统公开规则，不直接更新目标 Resource / Document 私有状态。

---

## 16. Integration Event

跨子系统有价值的低频事实可以发布 Integration Event：

- `share.created`
- `share.revoked`
- `invite.created`
- `invite.accepted`
- `room.created`
- `room.ended`
- `room.member.joined`
- `room.member.removed`
- `room.role.changed`
- `comment.created`

以下高频事件通常只属于 Room Realtime Event，不进入全局持久事件总线：

- cursor moved；
- typing；
- heartbeat；
- playback drift report；
- buffer update。

如果某个高频事件需要 Analytics，应由聚合器采样 / 汇总后再产生低频事实。

---

## 17. Notification 与 Automation

Notification 的核心管理能力已经由 `Platform-Administration-Operations-Subsystem-Design.md` 定义。

本子系统只产生业务触发事实，例如：

- 被邀请加入 Room；
- Share 即将过期；
- 评论回复；
- 成员角色变化；
- Owner 转移。

Notification Rule 决定是否通过 In-App / Email / Push 等 Channel 送达。

Automation 可以订阅 Room / Share Integration Event，但必须使用公开 Command 执行动作，且保留原始 actor / automation principal / causation 信息。

---

## 18. 数据库约束

至少应有以下约束：

1. Share / Invite / Room / Membership 等独立实体使用 UUIDv7。
2. 时间点使用 `timestamptz`。
3. 同一个 Room 对同一个 Principal 只能有一个有效 Membership。
4. Room 必须有且只有一个有效 Owner（若采用单 Owner 模型）。
5. Share Token Digest 建唯一约束。
6. Invite 的业务幂等键应防止同一邀请重复创建风暴。
7. Room Sequence 必须按 Room 单调递增，不能依赖客户端生成。
8. 删除 Resource 不通过数据库 Cascade 直接删除审计所需 Share / Room 历史；应由生命周期流程决定失效和保留。
9. Presence 不要求作为强关系数据永久持久化。

---

## 19. 安全与隐私

### 19.1 最小授权

Share 默认只授予用户明确选择的能力，不默认继承 Owner 的全部权限。

### 19.2 信息泄露

未授权用户不能通过：

- Share 查询；
- Room ID 枚举；
- Invite 错误；
- Presence；
- Member List；
- WebSocket close reason；

推断私有 Resource 或用户敏感信息。

### 19.3 Audit

建议审计：

- 创建 / 撤销公开 Share；
- 高权限 Share；
- 成员移除；
- Owner 转移；
- Room 权限变更；
- 管理员 Break-glass 访问。

不应把聊天正文、文档正文等无必要内容直接复制进 Audit Log。

### 19.4 Abuse Protection

对外 Share / Guest Room 至少考虑：

- Token 暴力枚举防护；
- Join / Message Rate Limit；
- Spam；
- 大量 Presence 连接；
- Invite 轰炸；
- WebSocket 连接数量上限；
- 单 Room 成员上限；
- 消息大小上限。

---

## 20. API 与错误语义

HTTP API 遵守 `API-Convention-Design.md`。

常见业务错误应可区分：

- Share expired；
- Share revoked；
- Share capability insufficient；
- Invite expired；
- Invite already consumed；
- Room ended；
- Room locked；
- Membership required；
- Room capability denied；
- State version conflict；
- Replay window unavailable。

对未授权匿名访问，应优先避免通过不同错误泄露目标是否真实存在。

---

## 21. 与 Resource 生命周期联动

当目标 Resource：

### ARCHIVED

Share 默认仍可按权限访问，但客户端可以提示已归档。

### TRASHED

已有 Share / Room 不再继续正常消费内容，返回明确“目标暂不可用”的受控状态。

### PERMANENTLY DELETED

- Share 失效；
- Room Target 进入 unavailable / detached；
- 搜索和公开入口撤销；
- 不因此自动删除 Room Chat / Audit 等其他领域依法 / 按策略保留的历史；
- Attachment / Blob GC 仍由 Storage 决定。

---

## 22. 可观测性

至少监控：

- Active Room 数量；
- Realtime Connection 数量；
- 每 Room 成员数；
- WebSocket reconnect rate；
- replay hit / snapshot fallback；
- sequence gap；
- Room Command conflict；
- Share 创建 / 撤销；
- 失效 Token 使用尝试；
- Message rate limit；
- Presence heartbeat delay；
- Permission revocation propagation latency。

日志携带 room_id、connection_id、principal_id、correlation_id，但禁止输出完整 Share Token。

---

## 23. 典型流程

### 23.1 通过链接加入一起看 Room

```text
Owner 创建 Room
    ↓
创建 join_room + read Share
    ↓
Server 返回一次性展示的 Share Link
    ↓
Guest 打开链接
    ↓
Redeem Token
    ↓
建立受限 Guest Principal / Membership
    ↓
HTTP 获取 Room Snapshot
    ↓
WebSocket 建连并重新鉴权
    ↓
从 current sequence 开始接收实时事件
```

### 23.2 成员在线时被移除

```text
Moderator RemoveRoomMember
        ↓
Membership 失效
        ↓
发布 room.member.removed
        ↓
Realtime Gateway 收敛授权
        ↓
终止该成员相关 Room Connection
        ↓
客户端再次访问时重新走权限校验
```

### 23.3 WebSocket 断线重连

```text
Connection lost at sequence 105
        ↓
客户端保留 last_sequence=105
        ↓
Reconnect + Auth
        ↓
重新校验 Membership / Share / ACL
        ↓
Server 当前 sequence=111
        ↓
Replay 106...111
        ↓
客户端恢复到一致状态
```

---

## 24. 测试与验收基线

至少覆盖：

1. Share Token 数据库泄露后不能直接得到可使用的完整凭据。
2. Share 撤销后新请求立即失败，已有 Room 连接也能收敛失效。
3. 同一 Invite 重复接受不会创建重复 Membership。
4. 同一 Principal 在 Room 中不会产生多个有效 Membership。
5. Owner 转移保持唯一 Owner 不变量。
6. Presence 消失不会误删 Membership。
7. 客户端断线重连后能通过 replay 恢复。
8. 超出 replay window 时能够 fallback 到 snapshot。
9. 乱序 Room Event 不会覆盖更新状态。
10. 未授权用户不能通过 Room / Share API 枚举私有 Resource。
11. 普通 Member 不能执行仅 Moderator / Owner 允许的控制操作。
12. Resource 进入 TRASHED / DELETED 后 Share 和 Room Target 行为符合生命周期定义。
13. 高并发 Join / Leave 不产生重复成员或负数计数。
14. WebSocket 连接建立时重新校验 Session 与 Room 权限。
15. Notification / Automation 消费重复 Integration Event 时保持幂等。
16. 日志、Trace、Audit 不泄露完整 Share Token 和敏感消息正文。

---

## 25. 后续专项设计边界

以下能力若复杂度继续增长，可独立拆分，但不得重新定义本文档的权限和 Room 基线：

- Document CRDT / OT 与实时协同编辑协议；
- WebRTC Voice / Video / Screen Share；
- 大规模 Room 的分布式 Realtime Gateway；
- Message Retention / Moderation；
- Federation / Multi-instance Collaboration。

V2 初期应优先保证：授权清晰、状态有序、断线可恢复、权限可收敛，再考虑复杂分布式实时架构。