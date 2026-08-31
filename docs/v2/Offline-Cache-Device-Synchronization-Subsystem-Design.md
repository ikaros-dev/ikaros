# Ikaros V2 Offline Cache / Device Synchronization 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Offline Cache / Device Synchronization 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Security-Identity-Authorization-Crypto-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Content-Creation-Revision-Collaborative-Document-Subsystem-Design.md`、`Media-Video-Anime-Playback-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 多端离线能力中的 Device、Download、Cache、Offline Copy、Pending Mutation、Sync Cursor、Change Feed、Tombstone、Conflict、Device Revocation 与 Secure Offline Data 的系统边界。
>
> Offline / Sync 是跨领域基础能力，但它不拥有 Document、Media、Finance、Private Notes 等领域的最终业务状态。同步层负责可靠传输、去重、游标、删除传播和冲突编排；具体业务冲突如何合并，仍由目标领域决定。

---

## 1. 设计目标

本子系统需要解决：

1. Web、Desktop、Mobile 等客户端断网后哪些数据可以继续读取和修改。
2. Download、Cache、Server Blob Replica 三类“副本”如何严格区分。
3. 一个显式 Download 什么时候才真正达到 `offline ready`。
4. 客户端离线产生的写操作如何可靠排队、重试和去重。
5. 多设备同时修改同一对象时，如何检测冲突而不是全局 Last Write Wins。
6. Sync Cursor 如何设计，避免依赖不可靠的客户端时钟或单纯 `updated_at > lastSync`。
7. 删除如何通过 Tombstone 传播到长时间离线设备。
8. Tombstone 被压缩后，过旧设备如何安全 Full Resync。
9. 权限、Share、Session 或设备信任被撤销后，离线设备重新联网时如何收敛权限。
10. Secure Domain 离线数据如何保持密文、本地解锁和设备撤销边界。
11. Download / Cache 完整性如何校验和修复。
12. Progress、Document、Finance、Private Notes 等不同领域如何复用 Sync Runtime，又不被统一成错误的通用 Merge。
13. 大量 Pending Mutation 如何批量同步、部分失败和恢复。
14. 服务端和客户端如何知道一次同步是增量、需要重试还是必须全量重建。

核心原则：

> **同步层负责“可靠把变化带到正确的地方”；目标领域负责“这个变化在业务上意味着什么”。同步不是第二套业务数据库。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Device Registration / Device Metadata；
- Device Sync Capability；
- Download Manifest 的服务端契约；
- Cache 与 Download 的统一术语；
- Offline Copy 状态；
- Sync Feed / Change Feed；
- Sync Cursor；
- Pending Mutation Envelope；
- Client Operation ID / Mutation ID；
- Tombstone / Deletion Feed；
- Conflict Envelope 与 Resolver Routing；
- Batch Push / Pull；
- Full Resync；
- Device Revocation；
- Secure Offline Data 的平台边界；
- 同步可观测性、限流和错误模型。

### 2.2 本子系统不负责

- Document 三方合并算法；
- Media Progress 的专业合并策略；
- Finance Transaction 的领域冲突规则；
- Private Notes / Password Manager 密文内部内容；
- Storage Server Replica 的物理管理；
- 客户端具体 SQLite / IndexedDB / Realm / Isar 技术选型；
- OS Background Task 的具体调度接口；
- iOS / Android 平台下载 API 的实现；
- Peer-to-peer Device Sync；
- 跨 Ikaros Instance Federation。

---

## 3. 核心不变量

1. **Download ≠ Cache**：Download 是用户明确要求保留的离线副本；Cache 是可淘汰、可重建的访问加速数据。
2. **Client Offline Copy ≠ Storage Blob Replica**：客户端本地副本默认不参与 Server Storage 的 Replica 数量、Durability 或 GC 判断。
3. **Cache 命中不代表 Download Complete**：只有 Download Manifest 所需对象完整下载并通过完整性验证后才能标记 Offline Ready。
4. **删除客户端 Download 不删除 Server Resource**：本地副本生命周期与服务端业务对象生命周期分离。
5. **Sync Cursor 不使用客户端墙钟作为唯一依据**：增量同步必须基于服务端可重放、可比较的稳定游标。
6. **每个离线 Mutation 必须有稳定 Mutation ID**：网络重试不得重复创建业务副作用。
7. **同步层不做万能 Merge**：冲突由拥有目标状态的领域处理。
8. **Tombstone 必须有保留窗口**：否则长期离线设备会把已经删除的数据重新上传为“新数据”。
9. **过旧 Cursor 必须显式失效**：无法保证增量完整性时返回 Full Resync Required，而不是静默漏数据。
10. **重新联网必须重新授权**：离线时缓存的权限快照不能永久授予服务端写权限。
11. **权限撤销影响未来同步**：被撤销用户 / 设备 / Share 不能因为曾经离线就继续向服务端提交受保护写入。
12. **Secure Domain 本地数据保持加密**：未解锁时普通同步基础设施不需要读取明文。
13. **设备撤销不能虚假承诺远程擦除已经泄露的明文**：系统可以停止未来密钥 / Token / Sync 能力并要求客户端清理，但无法数学保证已经被恶意复制到外部介质的数据消失。
14. **同步失败不应破坏已确认服务端事实**：每项 Mutation 独立返回结果，客户端只移除已被服务端确认的 Pending 操作。
15. **Change Feed 是传播机制，不是业务真相源**：源领域数据仍是最终事实。

---

## 4. 概念分层

```text
Server Business Truth
Resource / Document / Media / Finance / Secure Ciphertext
            │
            ├── Domain Event / Change Projection
            ↓
        Sync Change Feed
            │
            ↓ pull(cursor)
      Device Local Store
            │
       offline mutation
            ↓
       Pending Mutation
            │
            ↓ push(batch)
       Target Domain Command
            │
            ↓
      Server Business Truth
```

内容字节另走：

```text
Server Attachment / Blob
          ↓ authorized download
Download Manifest
          ↓
Device Offline Copy

Cache
= device-local acceleration only
```

---

## 5. Device

Device 表示一个被 Ikaros 识别的客户端设备 / 安装实例上下文。

建议字段：

- `id`：UUIDv7；
- owner user id；
- installation id / public device identifier；
- display name；
- platform；
- app version；
- capability summary；
- registered_at；
- last_seen_at；
- trust state；
- revoked_at；
- optional push capability reference。

### 5.1 Device 不是用户身份

Device 不能代替 User / Session。

一次请求仍需要：

```text
Authenticated User / Principal
+
Session
+
Device Context
```

Device ID 只能用于：

- 同步诊断；
- 本地副本管理；
- Device Trust；
- 撤销；
- Conflict 来源；
- Notification / Push 目标。

### 5.2 Installation Identity

App 重装可能形成新 Device Registration。

不能假设：

- OS Device Name 唯一；
- MAC 地址可用；
- 硬件序列号可访问或适合持久跟踪。

应使用应用生成的随机 Installation Identity，再通过认证绑定 User。

---

## 6. Device Trust 与 Revocation

Device 可具有：

- ACTIVE；
- LIMITED；
- REVOKED；
- EXPIRED（可选）。

Revocation 后：

- 新 Session / Refresh 受策略拒绝；
- Sync Push / Pull 被拒绝；
- 新 Download Token 不签发；
- Secure Key Material 不再下发；
- Realtime 连接应收敛；
- Push Registration 可注销。

客户端重新联网后应：

- 清除失效 Session；
- 按策略清理受保护 Cache；
- Secure Domain 进入 Locked / Revoked 状态；
- 不再尝试无限重试 Pending Mutation。

---

## 7. Download、Cache 与 Offline Copy

### 7.1 Download

Download 是用户明确请求：

> “让这个内容在当前设备上长期离线可用。”

Download 具有：

- 用户意图；
- 明确 scope；
- 质量 / 版本选择；
- 完整性要求；
- 可管理生命周期；
- Offline Ready 状态。

### 7.2 Cache

Cache 是：

- 可淘汰；
- 可重建；
- 不承诺完整消费；
- 由客户端空间策略自动管理。

例如：

- 视频已缓冲片段；
- 图片 Preview；
- 封面；
- Search 缓存；
- 文档只读 Snapshot。

### 7.3 Offline Copy

Offline Copy 是当前设备实际保存的一份本地内容副本。

它可以由：

- Download 产生；
- Secure Domain 同步产生；
- Pin Document 产生。

默认不进入 Server Storage Replica 真相。

只有未来明确设计 Trusted Edge Storage 时，才允许某类 Device Replica 被 Storage 子系统视为正式 Replica。

---

## 8. Download Manifest

Download Manifest 描述“为了让目标 Scope 真正离线可用，需要哪些内容”。

例如 Episode：

```text
Download Manifest
├── selected Media Release / Variant
├── video/audio content
├── required subtitle(s)
├── optional poster
└── integrity metadata
```

Comic Volume：

```text
Volume
├── Chapter 1 pages
├── Chapter 2 pages
└── metadata / cover
```

建议字段：

- manifest id；
- target resource / scope；
- selected version / quality；
- required attachment list；
- optional attachment list；
- expected hash / size；
- manifest version；
- generated_at；
- expires / refresh policy。

### 8.1 Manifest Snapshot

Manifest 是一次下载意图的快照。

后续服务端新增字幕 / 新版本，不应悄悄让一个已完成 Download 突然变成“不完整”。

用户主动 Repair / Refresh Download 时可以生成新 Manifest Version。

---

## 9. Download State

建议客户端语义：

- QUEUED；
- DOWNLOADING；
- PAUSED；
- VERIFYING；
- COMPLETED；
- FAILED；
- NEEDS_REPAIR；
- CANCELLED / REMOVED。

`COMPLETED` 必须满足：

1. Manifest required items 均存在；
2. 内容大小合理；
3. Hash / Integrity 验证通过；
4. 必要的本地索引 / 解密元数据已准备；
5. 当前客户端确实能打开对应 Offline Scope。

不能仅凭 HTTP 200 就标记完成。

---

## 10. 下载授权与 URL

下载内容仍必须走服务端权限判断。

临时下载 URL / Token：

- 短时有效；
- 绑定目标 Attachment / Blob read capability；
- 不暴露 Storage Credential；
- 不应长期持久化到日志；
- 过期后可在有权限时刷新。

Range Resume 可以刷新授权后继续，不要求使用同一个永久 URL。

---

## 11. Download Integrity 与 Repair

下载完成后至少校验：

- expected size；
- cryptographic hash（可用时）；
- manifest completeness。

Repair 流程：

```text
Local verification failed
        ↓
mark NEEDS_REPAIR
        ↓
compare manifest
        ↓
redownload only missing/corrupted items
        ↓
verify
        ↓
COMPLETED
```

Repair 不创建新的 Server Resource。

---

## 12. Local Pending Mutation

离线写操作进入本地 Durable Queue。

Mutation Envelope 至少包括：

- mutation id；
- target subsystem；
- command type；
- target id；
- payload / change reference；
- base version / base revision；
- occurred_at；
- device id；
- local sequence；
- sensitivity；
- retry count；
- local dependency ids；
- schema version。

### 12.1 Mutation ID

Mutation ID 必须在客户端首次创建操作时生成，并在所有重试中保持不变。

服务端按：

```text
principal + mutation_id
```

或明确业务作用域进行幂等去重。

网络超时后客户端不知道服务端是否成功时，可以安全重试同一个 Mutation。

---

## 13. Local Sequence

每个 Device 可以维护单调增加的 Local Sequence，用于：

- 本地 Pending 排序；
- 诊断缺口；
- 保证同设备依赖操作顺序。

Local Sequence：

- 不是全局版本；
- 不能用于跨设备比较“谁更新”；
- 不能替代 Server Revision。

---

## 14. Push Mutation Batch

客户端恢复联网后可以批量 Push。

请求逻辑：

```text
Device
  ↓
Batch [M1, M2, M3...]
  ↓
Sync Gateway
  ↓
Auth / Device / Schema validation
  ↓
Route to target Domain Command
  ↓
Per-item result
```

每项返回：

- APPLIED；
- ALREADY_APPLIED；
- CONFLICT；
- REJECTED_PERMISSION；
- INVALID；
- RETRYABLE_ERROR；
- DEPENDENCY_FAILED；
- REQUIRES_UPGRADE。

批量 API 不应因为一项 Conflict 就隐藏其他项的结果。

---

## 15. Mutation Dependency

离线创建对象会产生依赖，例如：

```text
Create Document(local temp ref A)
      ↓
Add Attachment to A
      ↓
Create Comment on A
```

建议：

- 客户端尽可能预生成最终 UUIDv7；
- 或 Sync Protocol 提供 explicit local reference mapping。

优先使用客户端安全生成的 UUIDv7 可以减少“临时负 ID”映射复杂度，但服务端仍必须验证 ID 格式、权限和重复创建。

依赖失败时后续项返回 `DEPENDENCY_FAILED`，不能创建半关联垃圾数据。

---

## 16. Change Feed

服务端为可同步领域生成增量 Change Feed。

Change Feed Entry 至少包含：

- server change id / cursor position；
- domain；
- object type；
- object id；
- change type；
- version / revision；
- changed_at；
- minimal payload or fetch hint；
- tombstone flag；
- authorization scope metadata（实现级）。

### 16.1 Change Feed 不等于 Integration Event

Integration Event 面向跨域业务联动。

Sync Change Feed 面向：

> 客户端需要知道“哪些可同步对象发生了可观察变化”。

二者可以由同一 Domain Event 投影产生，但生命周期和 Payload 不必相同。

---

## 17. Sync Cursor

Cursor 必须满足：

- 服务端生成；
- 单调 / 可比较；
- opaque to client；
- 能准确表示“已经消费到哪里”；
- 可识别过期 / compacted。

可以使用：

- durable sequence；
- log position；
- per-feed monotonically increasing id；
- 其他可靠游标。

禁止只使用：

```text
updated_at > client_last_sync_time
```

作为唯一同步机制，因为：

- 客户端时钟可能错误；
- 同毫秒多条更新；
- 删除行不存在；
- transaction commit 顺序与业务时间不同；
- 时间边界容易漏 / 重。

---

## 18. Feed Scope

不建议一开始建立一个包含全部平台数据的超级全局 Feed。

可以按：

- User；
- Domain；
- Vault；
- Collection / Sync Subscription；

拆分。

客户端保存：

```text
feed_id -> cursor
```

从而允许：

- Documents Feed；
- Media User State Feed；
- Planning Feed；
- Secure Vault Ciphertext Feed；

分别演进和限制权限。

---

## 19. Pull Sync

典型：

```text
GET changes since cursor C
        ↓
Server auth
        ↓
return [change...], next_cursor
```

响应需要：

- `next_cursor`；
- `has_more`；
- change list；
- optional snapshot / resync hint。

客户端只有在成功持久化这一批 Change 后才推进本地 Cursor。

如果 App 在应用第 10/100 条时崩溃，下一次仍可从旧 Cursor 重放；应用 Change 必须幂等。

---

## 20. Push / Pull 顺序

不同领域可以选择：

- Push pending first, then Pull；
- Pull latest first, merge, then Push；
- 双阶段协商。

不能由 Sync Runtime 全局规定一个所有领域都正确的顺序。

例如 Document 可能需要先看到 Remote Revision 再 Merge；简单 Favorite Toggle 可以安全命令式 Push。

Sync Profile 应由领域声明。

---

## 21. Conflict

Sync Runtime 负责检测：

- base version mismatch；
- stale revision；
- deleted target；
- permission changed；
- domain-specific conflict response。

但具体 Resolver 属于目标领域。

示例：

### Document

进入三方 Merge / Conflict Resolver。

### Media Progress

使用 Progress Intent、Session、Version、Occurred At 规则。

### Finance

按 Accounting 子系统的不可变 Transaction / Reconciliation 规则处理。

### Private Notes

服务端看不到明文时，保留 Conflict Ciphertext Revision 供客户端解锁合并。

Global Sync UI 只汇总冲突，不替目标域做“Keep Latest”万能按钮。

---

## 22. Tombstone

删除需要可同步事实。

Tombstone 至少包括：

- object id；
- object type；
- deleted / purged version；
- deleted_at；
- domain；
- optional replacement / merge reference；
- retention_until。

客户端收到 Tombstone 后：

- 删除 / 隐藏本地业务副本；
- 清理普通 Cache；
- 处理本地未同步修改；
- 不把旧副本重新当新对象上传。

### 22.1 Trash vs Tombstone

Resource `TRASHED` 是业务生命周期状态。

Tombstone 是同步层表示“这个同步对象已删除 / 不应继续作为活跃对象存在”的传播记录。

二者不能混用。

---

## 23. Tombstone Retention 与 Cursor Expiry

Tombstone 保留时间必须考虑最长支持离线窗口。

如果 Tombstone / Change Log 已经 compact，而设备 Cursor 仍更旧：

```text
CURSOR_EXPIRED
FULL_RESYNC_REQUIRED
```

禁止：

- 返回空列表假装同步完成；
- 让设备继续使用不完整视图；
- 悄悄丢失删除传播。

---

## 24. Full Resync

Full Resync 不是“把本地库先全删再说”的唯一实现。

安全流程：

1. 暂停普通增量 apply；
2. 保存未提交 Pending Mutation；
3. 获取服务端当前 Snapshot / listing；
4. 对比本地对象；
5. 应用 Tombstone / 权限变化；
6. 重新建立 Cursor；
7. 按目标领域规则 rebase Pending Mutation；
8. 恢复增量同步。

Secure Domain 需要遵守自己的密文 / key policy。

---

## 25. 权限变化

离线设备可能在断网期间失去：

- Resource ACL；
- Share；
- Room Membership；
- User Role；
- Session；
- Device Trust。

重新联网时：

- 所有 Push 重新执行当前权限判断；
- Pull 只能返回当前仍可见内容；
- 撤销信息通过 Permission / Tombstone / Scope invalidation 传播；
- 无权限对象应从受保护本地 Cache 清理；
- 显式 Download 的处理遵守数据分类策略。

离线时曾经合法下载的数据不能被假装“从物理世界远程收回”，但受控客户端应在重新联网并收到撤销后执行本地清理策略。

---

## 26. Data Classification 与本地存储

### 普通 Public / Shared Data

可按客户端安全策略明文或应用沙箱保存。

### Private / Sensitive

应考虑：

- app sandbox；
- local database encryption；
- OS keystore / keychain；
- backup exclusion；
- screen / recent app policy（必要时）。

### Secure Domain

必须：

- ciphertext persistence；
- key material 受 Secure Storage 保护；
- Lock 后移除解密上下文；
- 普通 Sync Runtime 不读取正文；
- 不进入普通 Search / Cache。

---

## 27. Secure Offline Sync

Private Notes / Password Manager 可使用：

```text
Encrypted Local Store
       ↓
Ciphertext Mutation
       ↓
Sync Runtime
       ↓
Ciphertext Server Store
```

Sync Envelope 可以看到：

- object id；
- vault id；
- revision；
- crypto version；
- ciphertext；
- tombstone；
- minimal timestamps。

不需要看到正文、Secret 或解密 Tag。

### 27.1 Device Revocation 与 Secure Key

撤销设备后：

- 不再下发新的 Vault Key Envelope；
- Refresh Token 失效；
- Secure Sync 失效；
- 受控客户端收到撤销后清理 Key Material。

如果攻击者已经导出了解锁后的明文，Server 无法远程保证其消失，因此 UI / 文档不得做虚假安全承诺。

---

## 28. Offline Authorization Snapshot

客户端为了 UI 可以缓存：

- 最近一次权限结果；
- Offline Allowed Capability；
- Scope expiration。

它只用于：

- 判断离线时是否允许本地操作；
- 提示用户某操作将在联网后重新验证。

它不是服务端最终授权。

高风险操作例如：

- 永久删除；
- 权限修改；
- Secret Recovery；
- Share 创建；
- Key Rotation；

默认不应完全离线执行。

---

## 29. Offline Domain Profile

每个领域需要声明自己的 Offline Profile：

```text
DomainSyncProfile
├── supports_offline_read
├── supports_offline_create
├── supports_offline_update
├── supports_offline_delete
├── conflict_strategy
├── tombstone_policy
├── local_encryption_requirement
├── max_offline_window
└── schema_version
```

示例：

### Media Progress

- read: yes；
- update: yes；
- merge: media intent-based。

### Document

- read: yes；
- edit: yes；
- merge: base revision + three-way / collaboration model。

### Platform Security Setting

- write offline: no。

---

## 30. Schema Version 与 Client Upgrade

Pending Mutation 与 Change Feed Payload 都必须有 Schema Version。

Server 遇到旧客户端：

- 能兼容则转换 / 接受；
- 无法安全转换则返回 `CLIENT_UPGRADE_REQUIRED` / `MUTATION_SCHEMA_UNSUPPORTED`；
- 不应把未知字段静默丢弃后制造错误业务结果。

客户端升级前的 Pending Mutation 需要迁移策略。

---

## 31. Sync Error Model

至少区分：

- NETWORK_UNAVAILABLE；
- AUTH_EXPIRED；
- DEVICE_REVOKED；
- PERMISSION_DENIED；
- CURSOR_EXPIRED；
- FULL_RESYNC_REQUIRED；
- CONFLICT；
- MUTATION_INVALID；
- MUTATION_SCHEMA_UNSUPPORTED；
- RATE_LIMITED；
- SERVER_RETRYABLE；
- LOCAL_STORAGE_FULL；
- LOCAL_INTEGRITY_ERROR；
- SECURE_VAULT_LOCKED。

不要把所有失败都显示“同步失败”。

---

## 32. Retry / Backoff

客户端重试：

- 网络错误指数退避；
- Respect Retry-After；
- 前台用户手动 Retry 可以立即触发；
- Permission / Invalid / Revoked 不无限重试；
- Conflict 等待用户 / Domain Resolver；
- Battery / Background policy 由设备配置影响。

Mutation ID 在重试时保持不变。

---

## 33. Sync Wakeup

为了低延迟可以使用：

- Push Notification；
- SSE；
- WebSocket；
- OS background trigger。

这些只用于提示：

> “有新变化，可以同步。”

真正增量完整性仍由 Durable Cursor / Change Feed 保证。

Push 消息丢失不能导致永久漏同步。

---

## 34. Download Queue 与 Device Policy

Download Policy 可以包含：

- Wi-Fi Only；
- Cellular allowed；
- max concurrent；
- battery saver；
- background allowed；
- free space reserve；
- size confirmation threshold。

这些主要是 Device Preference，不应成为跨设备业务真相。

Automation 可以创建 Download Intent / Policy，但不能绕过：

- 当前权限；
- 设备空间；
- 网络策略。

空间不足时默认暂停并通知，不能悄悄删除用户显式 Download。

---

## 35. Local Space Management

客户端空间应至少区分：

- Explicit Downloads；
- Cache；
- Secure Encrypted Data；
- Pending Upload / Mutation；
- App Internal DB。

自动清理优先：

1. 可重建 Cache；
2. 过期临时文件；
3. 可配置的旧派生缓存。

显式 Download 和 Secure Data 不能为了空间静默删除，除非用户设置明确 Retention Policy。

---

## 36. Offline Ready

“Offline Ready” 是高于 “some local bytes exist” 的产品状态。

需要：

- Manifest complete；
- integrity verified；
- necessary metadata local；
- content decoder / reader supported；
- Secure Domain key requirement satisfied（必要时仅表示 encrypted-ready，解锁仍需用户认证）；
- no known corruption。

普通 Cache 不自动进入 Offline Ready 列表。

---

## 37. Domain 示例：Document

```text
Offline open Revision 12
      ↓
edit local Working Copy
      ↓
Pending Mutation(base_revision=12)
      ↓ reconnect
Server current revision=13
      ↓
Document Conflict / Merge
      ↓
new Revision 14
```

Sync Runtime 不选择 Mine / Remote。

---

## 38. Domain 示例：Media Progress

```text
Device A offline at 05:00
Device B online reaches 20:00
Device A reconnects
      ↓
UpdateProgress(intent=OFFLINE_REPLAY,
               base_version=old,
               occurred_at=...)
      ↓
Media domain merge rule
```

不能全局使用 position max，也不能 Last Write Wins，因为显式 Reset / Rewatch 需要合法回退。

---

## 39. Domain 示例：Favorite / Tag

简单集合类状态可以使用幂等命令：

- FavoriteResource；
- UnfavoriteResource；
- AddTag；
- RemoveTag。

仍需要 operation ID 和当前权限判断。

对于同一用户同一对象的并发相反操作，可以使用领域 version / intent timestamp / explicit state command 解决，而不是 Sync Runtime 猜测。

---

## 40. API 契约

遵守 `API-Convention-Design.md`。

可以提供逻辑能力：

- RegisterDevice；
- RevokeDevice；
- ListDevices；
- GetSyncCapabilities；
- PullChanges(feed, cursor)；
- PushMutations(batch)；
- GetSyncConflictSummary；
- AcknowledgeFullResync；
- CreateDownloadManifest；
- RefreshDownloadManifest；
- ResolveDownloadItem；
- ReportDeviceDownloadState（仅需要跨端可见时）。

API 不应暴露客户端本地数据库结构。

---

## 41. Integration Event

建议平台业务事件：

- `device.registered`
- `device.revoked`
- `sync.conflict.detected`
- `sync.full-resync.required`（仅确有平台通知价值时）
- `download.intent.created`（如果 Download Intent 作为服务端用户偏好存在）

高频每批 Pull / Push 不需要全部成为平台 Integration Event。

---

## 42. 数据库边界

服务端可以持久化：

- Device Registry；
- Device Trust / Revocation；
- Change Feed / Projection；
- Tombstone；
- Mutation Dedup Record；
- Conflict Summary；
- optional Download Manifest / Intent；
- Sync Subscription / Cursor metadata（必要时）。

不持久化为服务端真相：

- 客户端 Cache 条目全集；
- 每个本地文件路径；
- OS-specific download handle；
- 客户端临时 UI 状态。

---

## 43. 数据库关键约束

1. Device / Sync 重要实体使用 UUIDv7。
2. Mutation Dedup 对 Principal + Mutation ID 建唯一约束。
3. Feed Cursor / Change Sequence 不重复。
4. Tombstone 与对象身份关联可唯一识别最新删除事实。
5. 时间使用 `timestamptz`。
6. Secure Payload 按敏感等级保存，不把明文放入普通 Sync Log。
7. Change Feed 可分区 / Retention，但压缩必须与 Cursor Expiry 协调。
8. Sync Runtime 不跨域 UPDATE 业务表。

---

## 44. 隐私与日志

日志可以记录：

- user id / device id；
- feed；
- cursor range；
- mutation id；
- target safe id；
- error code；
- batch size。

禁止无必要记录：

- 文档正文；
- Password / Secret；
- Private Note 明文标题；
- signed download URL；
- encryption key；
-完整 Finance Payload。

Pending Sync UI 对 Secure Domain 只展示安全摘要。

---

## 45. 可观测性

至少监控：

- active device count；
- revoked device count；
- sync pull / push latency；
- mutation apply success rate；
- duplicate mutation rate；
- conflict count by domain；
- cursor expired count；
- full resync count / duration；
- change feed backlog / retention；
- pending mutation age（客户端可上报摘要时）；
- download integrity failure；
- download repair rate；
- secure sync failure（不含敏感正文）。

---

## 46. 测试与验收基线

实现至少覆盖：

1. Cache 清理不会删除显式 Download。
2. 删除 Device Download 不影响 Server Resource / Blob。
3. Download 只有 Required Manifest Item 全部校验后才进入 COMPLETED。
4. 下载中断后刷新授权可以 Range Resume。
5. 同一 Mutation 网络超时重复发送只产生一次业务副作用。
6. 同设备 Local Sequence 缺口可诊断，但不会被误当全局版本。
7. Sync Cursor 不依赖客户端时间正确性。
8. App 在 Pull Batch 中途崩溃后可以重放且不重复副作用。
9. Tombstone 能阻止长期离线设备复活已删除对象。
10. Cursor 超过 Retention 后明确要求 Full Resync。
11. Full Resync 保留未提交 Pending Mutation 并按领域重新 Rebase。
12. 权限撤销后离线 Mutation 重新联网被拒绝。
13. Device Revocation 后新 Sync / Download Token 无法获得。
14. Secure Domain Sync 不要求服务端读取明文。
15. Document Conflict 跳转 Document Resolver，而不是全局 LWW。
16. Media Progress 使用 Media Intent 规则，而不是通用 max / latest。
17. 批量 Push 一项失败不隐藏其他项结果。
18. Schema 不兼容时不会静默丢字段。
19. Push Notification 丢失后下一次主动 Pull 仍能补齐全部变化。
20. Server Change Feed 损坏不会成为业务真相源；可以从 Domain State / Event 重建必要 Projection。

---

## 47. P0 / P1 / P2 建议

### P0

- Device Registry；
- Pending Mutation Envelope；
- Mutation ID 幂等；
- per-domain Change Feed / Cursor；
- Tombstone；
- Full Resync；
- Document / Progress 基础冲突路由；
- Download vs Cache 术语；
- Download Manifest / Integrity；
- Secure ciphertext sync compatibility。

### P1

- Device Trust UI / remote revoke；
- advanced background sync；
- Download Policy / Automation；
- cross-device Download Intent；
- Push wakeup；
- repair optimization；
- richer Conflict Center。

### P2

- trusted edge storage；
- peer-to-peer sync；
- LAN transfer acceleration；
- partial replication policy；
- large-scale feed compaction optimization；
- multi-node Sync Gateway。

---

## 48. 核心结论

Ikaros V2 的离线能力不能只实现成“客户端多一个缓存目录”。

正确分层应为：

```text
Download
= 用户明确管理的离线副本

Cache
= 可淘汰、可重建的加速数据

Pending Mutation
= 离线业务操作

Change Feed + Cursor
= 服务端变化传播

Tombstone
= 删除传播

Domain Resolver
= 冲突业务语义

Device Revocation
= 未来访问与密钥能力收敛
```

同步基础设施只负责可靠传输、游标、幂等、删除和安全边界；Document、Media、Finance、Private Notes 等子系统继续拥有自己的版本、冲突、权限和业务真相。
