# Ikaros V2 Personal Drive / File Synchronization P0 关键语义补充

| 项目 | 内容 |
|---|---|
| 文档名称 | Personal Drive / File Synchronization P0 Semantics |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 主设计 | `Personal-Drive-File-Synchronization-Subsystem-Design.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Attachment-Blob-Storage-Subsystem-Design.md`、`Offline-Cache-Device-Synchronization-Subsystem-Design.md`、`Photo-Album-Image-Asset-Subsystem-Design.md` |

> 本文档补充 `Personal-Drive-File-Synchronization-Subsystem-Design.md` 中会直接影响 P0 Server / Desktop / Mobile Sync Engine 实现的一致性语义。
>
> 本文档不是第二套 Drive 领域模型。Drive Space、Drive Node、File Revision、Sync Binding、Attachment / Blob、Photo Projection 等对象仍以主设计为准；本文只把容易在不同客户端实现中产生歧义的状态机、时间、事件归并、配额和备份安全条件固定下来。

---

## 1. 补充目标

本补充设计重点解决以下问题：

1. Drive Change Feed 中究竟什么算一次可同步变化。
2. Trash 后 Restore 如何避免被旧 Tombstone 再次删除。
3. Drive 时间与源文件时间如何分离。
4. Office、IDE、编辑器采用临时文件替换保存时如何避免误判为删除 + 新建。
5. 并发 Upload Session 如何正确占用和释放配额。
6. Camera Backup 在什么条件下才可以告诉用户“已安全备份，可以释放本地空间”。
7. 多个 Sync Binding 的本地或远端 Scope 重叠时如何防止同步回环。
8. File / Folder Copy 的 Revision History 到底如何处理。
9. Windows / macOS / Linux 文件系统差异在 P0 中支持到什么程度。
10. iCloud Photos、Android 云端媒体、Limited Permission 等“发现 Asset 但暂时拿不到 Original”的场景如何建模。
11. Shared Folder 是否允许直接作为 P0 TWO_WAY Sync Root。

核心原则：

> **同步正确性优先于“看起来实时”；任何无法证明为无损的自动收敛，都不得通过路径、mtime 或 watcher 事件猜测后静默覆盖用户内容。**

---

## 2. Drive Change Generation

### 2.1 Change Feed 与领域事件分离

Drive Event 用于跨领域集成；Drive Change Feed 用于设备可靠同步。

两者可以来自同一次领域事务，但不是同一个概念：

```text
Drive Command
     ↓
Drive Domain Transaction
     ├── Outbox Domain Event
     └── Drive Change Record
               ↓
        Device Sync Feed
```

Domain Event 可以按消费者需求演进；Change Feed 必须满足设备增量重放、排序、幂等和 Full Resync 判断。

### 2.2 Change Generation

每个 Drive Space 需要一个服务端权威、单调递增、可比较的 Change Generation / Sequence。

建议语义：

```text
Drive Change
├── drive_space_id
├── sequence: bigint
├── node_id
├── mutation_kind
├── node_version
├── revision_id?
├── previous_parent_id?
├── previous_name?
├── actor / device
├── occurred_at
└── contract_version
```

`sequence`：

- 由服务端产生；
- 在一个 Drive Space 内严格定义顺序；
- 不由客户端时间生成；
- 不使用 UUIDv7 时间部分替代；
- 不要求等同数据库 WAL / transaction id；
- 可以作为 Offline Sync Runtime 中 Drive Projection 的 Cursor 基础。

### 2.3 Mutation Kind

P0 至少区分：

- `NODE_CREATED`；
- `CONTENT_REVISION_CREATED`；
- `NODE_RENAMED`；
- `NODE_MOVED`；
- `NODE_TRASHED`；
- `NODE_RESTORED`；
- `NODE_PURGED`。

未来可以增加：

- ACL / Share Projection Change；
- Pin / Favorite；
- Special Folder Change；
- Metadata Change。

但客户端不得把所有变化统一解释成“文件 modified”。

### 2.4 Rename / Move 不是 Delete + Create

服务端已知 Node Identity 时：

```text
NODE_RENAMED(node_id=123)
```

必须保持 `node_id = 123`。

同步客户端收到该 Change 后应尽量执行本地 Rename，而不是：

```text
Delete old path
Download same bytes to new path
```

如果本地文件系统无法原子 rename，再由客户端降级实现，但映射层仍保持同一 Remote Node Identity。

### 2.5 Cursor 推进规则

客户端只有在某个 Change 已经：

1. 成功应用到本地；或
2. 被确定为无需应用但已持久记录原因；或
3. 被持久化为可恢复 Conflict；

之后，才能推进该 Change 对应的 Cursor。

不能“先 Ack Cursor，再异步写文件”。

---

## 3. Node Version、Lifecycle Generation 与 Restore

### 3.1 问题

设备 A 删除文件：

```text
ACTIVE
  ↓ sequence 20
TRASHED
```

设备 B 长期离线。

用户随后从 Web 恢复：

```text
TRASHED
  ↓ sequence 21
ACTIVE
```

如果 B 只保存一个“此 Node 曾被删除”的 Tombstone，而不理解后续 Restore Generation，则 B 重连后可能再次删除已经恢复的文件。

### 3.2 Node Version

Drive Node 每次会影响同步可见状态的领域变更都增加 `node_version`。

包括：

- rename；
- move；
- trash；
- restore；
- purge request / purge；
- current revision 切换。

Node Version 是乐观并发与快照版本，不替代 Change Sequence。

### 3.3 Tombstone 不是永久真相

Tombstone 表示：

> 在某个服务端 sequence 上，这个 Node 进入了删除传播状态。

它至少需要：

```text
Tombstone
├── node_id
├── tombstone_sequence
├── node_version
├── lifecycle = TRASHED | PURGED
├── deleted_at
└── retention_deadline
```

### 3.4 Restore 语义

Trash Restore：

- 保持同一个 `node_id`；
- 增加 `node_version`；
- 产生新的 `NODE_RESTORED` Change Sequence；
- 旧 Tombstone 不能覆盖比自己更新的 Node Version / Sequence。

因此：

```text
node 123
v5 ACTIVE
   ↓ seq 20
v6 TRASHED
   ↓ seq 21
v7 ACTIVE
```

设备收到 seq 20 和 seq 21 后，最终必须收敛到 v7 ACTIVE。

### 3.5 Purge 后不得复活同一 Node

`PURGED` 与 `TRASHED` 不同。

P0 规则：

- Trash 可以 Restore 同一 Node Identity；
- Permanent Purge 完成后，该 Node Identity 不再恢复为 ACTIVE；
- 如果用户重新上传相同内容，应创建新的 Drive Node ID；
- 即使底层 Blob 因其他引用尚未 GC，也不能据此复活已 Purged Drive Node。

---

## 4. 文件时间语义

### 4.1 必须区分的时间

Drive 至少区分：

```text
Drive Metadata Time
├── created_at
└── updated_at

Content Time
└── content_modified_at?

Source Time Hints
├── source_created_at?
└── source_modified_at?
```

含义：

- `created_at`：Drive Node 在 Ikaros 中创建的真实时间点；
- `updated_at`：Drive Node 领域状态最后变化时间；
- `content_modified_at`：当前文件内容在源语义中的最后修改时间；
- `source_created_at`：来源文件系统或 Provider 提供的创建时间 Hint；
- `source_modified_at`：来源文件系统提供的修改时间 Hint。

### 4.2 上传时间不覆盖源修改时间

例如：

```text
source_modified_at = 2022-03-01T10:00:00+08:00
Drive upload at     = 2026-08-31T10:00:00+08:00
```

上传后：

- Drive `created_at` 是 2026；
- `content_modified_at` 可以保留可信的 2022 源修改语义；
- UI 可以同时显示“修改于 2022 / 添加于 2026”。

### 4.3 时间不是内容身份

任何情况下：

```text
mtime + size
```

只能用于快速变化候选筛选，不能证明内容相同。

### 4.4 时间可信度

Source Time 可能：

- 被复制工具保留；
- 被用户手动修改；
- 因文件系统精度丢失；
- 因时区处理错误；
- 根本不存在。

因此同步冲突判定仍使用 Revision / Base Version / Fingerprint，而不是“谁的时间更新”。

---

## 5. Atomic Save / Change Coalescing

### 5.1 问题

很多编辑器保存：

```text
Create .tmp
Write new bytes
fsync
Delete / replace original
Rename .tmp -> report.docx
```

Watcher 可能观察到：

```text
CREATE .tmp
MODIFY .tmp
DELETE report.docx
RENAME .tmp report.docx
```

如果同步引擎逐条立即上传，会错误产生：

- Trash；
- New File；
- Conflict Copy；
- 多个无意义 Revision。

### 5.2 Event 只是 Hint

OS File Watcher / Journal Event 只能用于“需要重新检查哪些路径”。

P0 不允许：

> 一个 watcher event = 一个远端 Drive Mutation。

正确模型：

```text
Watcher / Journal Events
       ↓
Dirty Scope / Candidate Set
       ↓ debounce / settle
Re-stat / map / fingerprint
       ↓
Logical Local Delta
       ↓
Drive Mutation
```

### 5.3 Settle Window

客户端可以对活跃文件设置短暂 Settle / Debounce Window，等待连续写入稳定。

窗口：

- 属于客户端实现优化；
- 不应写死为跨平台唯一毫秒值；
- 大文件写入时可以结合 size stable / handle state / journal signal；
- 不得无限等待导致文件永不备份。

### 5.4 Atomic Replace Detection

若出现：

- 原路径消失；
- 同目录出现新 local item；
- 新对象最终占用相同相对路径；
- 内容发生变化；
- 时间窗口合理；

客户端应优先将其解释为：

> 原 File Node 的新 Content Revision 候选

而不是直接产生 Delete + Create。

但若证据不足，宁可进入保守 Reconcile，也不能错误合并两个不同用户文件。

### 5.5 Temporary File Filter

P0 默认建议忽略常见临时文件模式，但规则必须可配置，例如：

- `*.tmp`；
- `~$*`；
- `.DS_Store`；
- editor swap file；
- application lock file。

不能仅凭扩展名永久禁止用户显式上传某个真实文件。

---

## 6. Quota Reservation

### 6.1 为什么需要 Reservation

假设用户剩余 1 GiB 配额，同时创建 10 个 1 GiB Upload Session。

如果只在 `Begin Upload` 查询当前已用空间而不占位，10 个 Session 都可能被接受。

### 6.2 Reservation 生命周期

P0 语义：

```text
Begin Upload
     ↓
Quota Check + Reserve
     ↓
Upload Session ACTIVE
     ↓
Finalize
     ↓
Commit Actual Logical Usage
     ↓
Release Reservation
```

失败路径：

```text
Abort / Expire / Reject Finalize
     ↓
Release Reservation
```

### 6.3 Reservation 字段

建议概念：

```text
DriveQuotaReservation
├── id
├── drive_space_id
├── upload_session_id
├── reserved_bytes
├── state: ACTIVE | COMMITTED | RELEASED | EXPIRED
├── expires_at
├── created_at
└── updated_at
```

### 6.4 Replace File 的配额

Replace File 创建新 Revision 时，配额增长取决于 Revision Retention Policy。

P0 推荐在 Finalize 前按“新 Revision 将产生的最坏逻辑增量”预留，并在提交后根据实际 Retention 结果结算。

### 6.5 Dedup 不降低用户逻辑 Reservation

即使 Storage 最终发现 Blob 已存在，P0 用户逻辑配额仍按 Drive 的逻辑引用大小结算。

这样：

- 不依赖物理去重时机；
- 不泄露其他用户内容存在性；
- Reservation 规则稳定。

---

## 7. Backup Verification 与 Free Up Space

### 7.1 Upload Complete 不等于 Backup Verified

移动端不能仅因为 HTTP Upload 成功就建议删除本地照片。

至少区分：

```text
DISCOVERED
  ↓
QUEUED
  ↓
UPLOADING
  ↓
UPLOAD_COMPLETE
  ↓
BLOB_VERIFIED
  ↓
DRIVE_COMMITTED
  ↓
BACKUP_VERIFIED
```

Photo Projection 可以并行或随后执行：

```text
BACKUP_VERIFIED
  ↓
PHOTO_PROJECTION_PENDING
  ↓
PHOTO_PROJECTED | PHOTO_PROJECTION_FAILED
```

### 7.2 BACKUP_VERIFIED 条件

P0 中，一个 Camera Asset 只有同时满足以下条件，才能进入 `BACKUP_VERIFIED`：

1. Upload Session Finalize 已完成；
2. 服务端已经计算 / 验证完整内容 Hash；
3. Blob 已具备 Storage Policy 要求的最低可用 Placement；
4. Attachment 已持久化；
5. Drive File Revision 已成功提交；
6. Camera Sync Mapping 已持久化到该 Remote Node / Revision；
7. 所有上述状态均能通过稳定 ID 重新查询确认。

### 7.3 Photo Projection 不阻塞备份安全

Photo EXIF 解析、Thumbnail、AI、Timeline Projection 失败：

- 不应回滚已安全保存的 Original；
- 不应把 `BACKUP_VERIFIED` 降级为“文件未备份”；
- UI 可以显示“已备份，照片处理中 / 索引失败”。

因为 Photo Projection 可重建，而 Original Blob 丢失不可接受。

### 7.4 Free Up Space

客户端只有对 `BACKUP_VERIFIED` 的本地 Asset 才可以提供：

- “释放本地空间”；
- “删除设备原件，保留 Ikaros 备份”；

之类操作。

执行前客户端仍必须再次确认当前 Mapping 指向的 Revision 没有进入 Missing / Corrupted 等失败状态。

### 7.5 用户删除本地后的状态

本地释放后：

```text
local_state = REMOVED_AFTER_VERIFIED_BACKUP
remote_state = BACKUP_VERIFIED
```

不能将其重新排入“待备份”，也不能显示成 Backup Failure。

---

## 8. Camera Source Availability

### 8.1 Discovered ≠ Original Readable

移动平台可能列出一个 Asset，但 Original 当前并不在本地：

- iCloud Photos 开启优化存储；
- Android Gallery 展示云端占位内容；
- Provider 需要联网下载原件；
- 用户授予 Limited Photos Permission；
- 原件在可移除存储设备上；
- Source Provider 临时离线。

因此：

> **发现 Asset 不代表当前 App 一定能读取 Original Bytes。**

### 8.2 Source Item State

Camera Source Item P0 至少允许：

- `READY`；
- `WAITING_FOR_LOCAL_ORIGINAL`；
- `PERMISSION_REQUIRED`；
- `SOURCE_UNAVAILABLE`；
- `QUEUED`；
- `BACKUP_VERIFIED`；
- `IGNORED`；
- `ERROR`。

`WAITING_FOR_LOCAL_ORIGINAL` 不应按普通网络 Upload Error 无限指数重试。

### 8.3 Limited Permission

当 iOS / Android 权限只允许访问部分媒体时：

- Sync Binding 仍可保持 ACTIVE / DEGRADED；
- 只备份当前可访问 Asset；
- 权限扩大后增量发现新增可访问 Asset；
- 权限缩小不等同于 Remote Delete；
- 不删除之前已经完成的 Remote Backup。

### 8.4 App 重装

App 重装可能丢失：

- local scope id；
- local media mapping DB；
- pending queue。

重新登录后必须允许通过：

- Source Provider stable identifier（若仍可用）；
- strong content hash；
- size；
- capture metadata；
- Remote Mapping / Backup Manifest；

重建映射，避免把整个 Camera Roll 再次上传为新文件。

---

## 9. Sync Binding 重叠与回环

### 9.1 本地 Scope 重叠

例如：

```text
Binding A: D:\Photos
Binding B: D:\Photos\RAW
```

同一文件可能同时被两个 Binding 观察和上传。

P0 默认：

- 同一 Device 上，不允许两个启用中的 TWO_WAY / UPLOAD 类 Binding 存在本地 Scope 祖先 / 后代重叠；
- 若平台无法可靠判断 Scope 是否重叠，则要求用户显式确认，并将 Binding 标记为风险配置；
- CAMERA_ROLL 与普通 Directory Binding 如果底层实际覆盖同一资产集合，也应尽可能提示重复备份风险。

### 9.2 Remote Scope 重叠

以下配置同样危险：

```text
Binding A local A -> /Documents
Binding B local B -> /Documents/Projects
```

尤其两个 Binding 都是 TWO_WAY 时，可能产生重复应用和冲突回环。

P0 默认禁止同一 User / Device 上的可写 Binding Remote Root 祖先 / 后代重叠。

### 9.3 Sync 输出不得再次作为另一个 Binding 输入

客户端必须检测典型回环：

```text
Download Binding output
       ↓
另一个 Upload Binding root
       ↓
Remote
       ↓
再次 Download
```

P0 可以通过 Scope 规则直接拒绝；不要求实现复杂的循环图自动求解。

---

## 10. Copy 与 Revision History

### 10.1 File Copy

P0 普通 Copy 语义：

```text
Source File
  current revision = rev 8
       ↓ Copy
New File Node
  revision 1 -> same Attachment / Blob
```

规则：

- 创建新的 Drive Node ID；
- 新文件从 `revision_no = 1` 开始；
- 初始 Revision 可以复用 Source 当前 Revision 的 Attachment / Blob；
- 不复制 Source 的完整 Revision History；
- `source_kind = COPY`；
- 可以保存 `copied_from_node_id / copied_from_revision_id` 作为审计来源。

### 10.2 为什么不复制全部历史

Revision History 属于原 File Identity 的历史。

如果普通 Copy 自动复制所有版本：

- 用户难以理解为何新文件创建时就有多年历史；
- Retention / Quota 语义复杂；
- 后续两个文件历史分叉难解释；
- 大目录复制会放大数据库元数据。

需要完整历史克隆的场景未来应使用独立 `Clone With History` 能力，而不是普通 Copy。

### 10.3 Folder Copy

Folder Copy 对每个 File Node 应遵守同样规则：

- 新 Node；
- revision 1；
- 复用当前 Attachment / Blob；
- 不复制历史 Revision。

---

## 11. 跨平台文件系统 P0 语义

### 11.1 目标

P0 Drive Sync 目标是：

> 在主流 Windows / macOS / Linux / Mobile 文件系统之间可靠同步“用户文件内容 + 基础组织元数据”。

它不是完整 POSIX filesystem clone。

### 11.2 名称规范化

服务端应定义统一 canonical / normalized name 规则，并至少处理：

- Unicode normalization；
- case collision；
- trailing space；
- trailing dot；
- `/`、`\` 等路径分隔符；
- Windows reserved names，例如 `CON`、`PRN`、`AUX`、`NUL`、`COM1` 等；
- 最大文件名长度；
- 服务端最大 Path Depth / Logical Path Length。

### 11.3 Case-only Rename

例如：

```text
readme.md -> README.md
```

在大小写不敏感文件系统上可能无法直接原子完成。

客户端可以使用临时中间名：

```text
readme.md
  ↓
.__ikaros_tmp_xxx
  ↓
README.md
```

但 Remote Node Identity 不变，只产生一次 Logical Rename。

### 11.4 P0 不保证完整同步的元数据

P0 不承诺跨平台完整保留：

- POSIX uid / gid；
- ACL 全语义；
- executable bit；
- Windows ADS；
- macOS resource fork；
- arbitrary xattr；
- device file；
- socket；
- FIFO。

普通文件内容、目录结构、文件名、基础时间 Hint 是 P0 核心。

### 11.5 Hard Link

P0 不把 Hard Link 关系提升为 Drive 领域语义。

两个本地 hard link 可以：

- 因内容相同而复用同一 Blob；
- 但在 Drive 中仍是两个 File Node。

### 11.6 Symlink

继续遵守主设计：默认不跟随 Root 外部 Target。

P0 可以直接忽略 Symlink，或将其记录为 Unsupported / Ignored；不得误将目标内容静默复制到另一个路径后声称“Symlink 已同步”。

---

## 12. Shared Folder 与 TWO_WAY Sync

### 12.1 P0 默认限制

P0 推荐：

> **只有当前用户拥有写控制权的 Personal Drive Folder 才能作为 TWO_WAY Sync Root。**

`Shared With Me` 虚拟入口不能直接作为 Sync Root。

### 12.2 原因

共享目录会引入：

- Owner 随时撤销权限；
- Read / Write Permission 动态变化；
- Share Scope 被移动或删除；
- 目标用户并不拥有 Trash / Restore 最终控制权；
- Folder ACL 继承变化；
- Owner 与 Guest 同时编辑时的额外冲突来源。

这会把 P0 文件同步状态机和授权状态机强耦合。

### 12.3 P0 可支持只读下载

如果产品需要，可以允许：

```text
Shared Folder
   ↓
DOWNLOAD_ONLY Binding
```

但每次 Pull / Download 必须重新经过当前 ACL 检查。

权限撤销后：

- 停止未来同步；
- Binding 进入 `AUTH_REQUIRED / PERMISSION_REVOKED`；
- 已下载本地文件如何处置遵守 Offline / Security Policy；
- 不尝试把本地修改 Push 回已失去权限的 Shared Folder。

### 12.4 P1 再开放 Shared TWO_WAY

等权限撤销、Shared Tombstone、Owner / Guest Conflict、Offline Permission Snapshot 等行为有完整测试后，再考虑 Shared Folder TWO_WAY。

---

## 13. API / Command 补充

主设计已有 Drive Query / Command。本补充要求以下契约显式表达版本和状态。

### 13.1 Mutation Preconditions

修改现有 Node 的 Command 应至少支持：

- expected node version；
- expected current revision id（内容写入时）；
- operation / idempotency id；
- actor / device context。

例如逻辑语义：

```text
ReplaceFile(
  node_id,
  expected_node_version,
  base_revision_id,
  upload_session_id,
  operation_id
)
```

### 13.2 Pull Drive Changes

至少需要表达：

```text
PullChanges(
  drive_space_id,
  cursor,
  limit
)
```

返回：

- ordered Change；
- next cursor；
- has more；
- cursor expired / full resync required；
- contract version。

### 13.3 Backup Status Query

客户端需要能通过稳定 ID 查询某个 Camera Asset 对应的 Remote Backup 状态：

- upload state；
- verification state；
- Drive Node / Revision；
- Blob availability；
- Photo Projection state。

客户端不得只能依赖本地“我记得上传过”的 Boolean。

### 13.4 Quota

Begin Upload 返回：

- reserved bytes；
- reservation id；
- reservation expires_at；
- effective logical quota snapshot。

Finalize / Abort 应显式结算 Reservation。

---

## 14. 数据库补充

主设计中的 Drive Schema 可以增加或吸收以下概念：

```text
drive_change_log
  - drive_space_id
  - sequence
  - node_id
  - mutation_kind
  - node_version
  - revision_id

drive_quota_reservation
  - upload_session_id
  - reserved_bytes
  - state
  - expires_at

drive_camera_backup_mapping
  - binding_id
  - source_item_id
  - remote_node_id
  - remote_revision_id
  - verification_state
  - source_state
```

这些可以根据最终实现合并进现有表或 Projection，不要求机械一概建成三张独立表；但相应语义必须有持久化真相，不能仅存在客户端内存中。

### 14.1 Change Log Retention

Drive Change Log 可以压缩 / 清理，但必须与 Sync Cursor Retention 协调。

如果设备 Cursor 早于最老可重放 Sequence：

```text
FULL_RESYNC_REQUIRED
```

不能返回“空变化”误导客户端认为已同步。

### 14.2 Quota Reservation Reaper

过期 Reservation 由后台任务安全释放。

Reaper 必须幂等，且不能释放已经成功 Finalize / Commit 的 Reservation。

---

## 15. P0 验收不变量

进入 P0 实现与测试前，至少应将以下行为写成 Integration / E2E 场景：

1. Rename / Move 后 Node ID 不变，客户端不重新上传相同字节。
2. Trash seq 20、Restore seq 21 后，长期离线客户端最终为 ACTIVE，不被旧 Tombstone 再删。
3. Purged Node 不因旧设备重新上线而复活。
4. 两设备基于同一 Revision 同时写入时产生 Conflict Copy，不丢任何一方字节。
5. Office atomic replace 保存只产生一个有效新 Revision，不产生无意义 Trash + Create。
6. Upload Session 并发时 Quota Reservation 阻止超额承诺。
7. Upload Abort / Expire 后 Reservation 最终释放。
8. Camera Upload HTTP 成功但 Blob 验证失败时，不允许 Free Up Space。
9. Photo Projection 失败但 Original `BACKUP_VERIFIED` 时，仍可视为文件备份成功。
10. 本地删除 `BACKUP_VERIFIED` Camera Asset 不删除 Remote Backup。
11. iCloud / cloud-only Asset 无本地 Original 时进入 Waiting 状态，不无限报 Upload Error。
12. App 重装后 Full Resync 能通过 Hash / Mapping 重建，避免全量重复上传。
13. 重叠的 TWO_WAY Binding 被拒绝或明确标记风险，不产生同步回环。
14. File Copy 只复制 Current Revision 为新文件 rev 1，不复制完整历史。
15. `readme.md -> README.md` 在大小写不敏感平台上仍表现为同一个 Remote Node Rename。
16. Shared Folder P0 不允许 TWO_WAY Push；权限撤销后 Download-only Binding 停止同步。
17. Cursor 落后 Change Log Retention 时明确返回 Full Resync Required。
18. 重试同一 Operation ID 不重复创建 Revision、Conflict Copy 或 Reservation。

---

## 16. 结论

P0 Drive Sync 的最终一致性骨架应为：

```text
Local Watcher / Source Provider
          ↓ hint only
Change Coalescing / Reconcile
          ↓
Logical Local Delta
          ↓
Drive Command + Preconditions
          ↓
Drive Transaction
   ├── Node / Revision
   ├── Change Sequence
   └── Outbox Event
          ↓
Sync Change Feed
          ↓
Other Devices
```

Camera Backup 的安全骨架应为：

```text
Source Asset Discovered
      ↓
Original Readable?
   ├── no -> WAITING / PERMISSION / SOURCE_UNAVAILABLE
   └── yes
        ↓
     Upload
        ↓
   Blob Verified
        ↓
   Drive Committed
        ↓
 Mapping Persisted
        ↓
 BACKUP_VERIFIED
        ├── Photo Projection
        └── Free Up Space Eligible
```

因此 P0 不追求“所有 watcher event 都立即同步”，而追求：

> **每一次最终提交都可解释、可重放、可验证；删除与恢复不会因离线设备乱序而误删；备份只有在服务端真正持久化并验证后才允许用户释放唯一的本地原件。**
