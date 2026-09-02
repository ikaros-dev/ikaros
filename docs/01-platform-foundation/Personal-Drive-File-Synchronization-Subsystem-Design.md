# Ikaros V2 Personal Drive / File Synchronization 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Personal Drive / File Synchronization 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Attachment-Blob-Storage-Subsystem-Design.md`、`Offline-Cache-Device-Synchronization-Subsystem-Design.md`、`Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md`、`Photo-Album-Image-Asset-Subsystem-Design.md`、`Sharing-Collaboration-Room-Subsystem-Design.md`、`Background-Task-Scheduler-Design.md` |

> 本文档定义 Ikaros V2 的个人网盘、文件树、文件版本、上传下载、回收站、目录同步、移动端照片 / 视频自动备份、跨设备文件同步、冲突处理与分享集成。
>
> 本子系统提供的是“用户可理解的文件空间与同步语义”。实际字节仍由 Attachment / Blob / Storage 保存；设备级可靠传输、游标与 Pending Mutation 继续复用 Offline / Device Synchronization；图片的 EXIF、Timeline、Album 等专业语义仍由 Photo 子系统拥有。

---

## 1. 设计目标

Personal Drive 子系统需要同时覆盖传统个人网盘与现代设备同步场景：

1. 提供类似个人网盘的文件 / 文件夹树、上传、下载、移动、复制、重命名、搜索、收藏与回收站。
2. 支持桌面端 / 移动端将一个或多个本地目录持续同步到 Ikaros。
3. 支持类似 Immich 的手机照片 / 视频自动备份，包含后台增量上传、断点续传、去重和已备份状态。
4. 允许“上传到网盘”与“进入专业内容库”分离，同时为照片、视频、文档等内容提供可选的领域投影。
5. 支持文件不可变 Revision，使覆盖写、历史版本、恢复和同步冲突均可解释。
6. 支持单向备份、双向同步、仅下载等不同同步模式，而不是把所有场景强制为双向同步。
7. 支持大文件、弱网络、移动网络、后台受限环境下的分片 / 可恢复上传。
8. 支持 rename / move 检测，尽量避免把路径变化错误识别成“删除 + 重新上传”。
9. 支持同步冲突显式保留，禁止静默丢失任一端用户数据。
10. 支持多用户、ACL 与 Share，但文件树的拥有关系、共享访问与物理存储隔离必须分离。
11. 保持自托管优先：单机 + PostgreSQL + 本地文件系统即可落地，S3 / NAS / 多副本是可选扩展。
12. 任何删除、版本清理和物理 GC 都必须与 Blob Storage 的保守删除原则一致。

核心原则：

> **Drive Path 是用户组织语义，不是内容身份；File Revision 是文件内容历史，不是 Blob 的可变写入；同步负责保持副本收敛，但不能通过静默覆盖来伪造“无冲突”。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Personal Drive Space；
- File / Folder Tree；
- Drive Node；
- 文件名、父目录、路径投影与排序；
- File Revision / Revision History；
- 文件上传、续传、替换与版本恢复；
- 文件 / 文件夹移动、复制、重命名；
- Trash / Restore / Permanent Delete 请求；
- Drive Quota / Logical Usage Projection；
- Sync Binding；
- Sync Mode / Direction / Filter；
- Local File Identity / Remote Node Mapping；
- 增量扫描与变化检测；
- 手机相册 / 相机胶卷自动备份；
- Sync Conflict 与 Conflict Copy；
- 同步状态、错误、重试、Pause / Resume；
- Drive 与 Share / ACL 的交接；
- Drive 与 Photo / Media / Document 等专业领域的投影交接；
- Drive Event / Command / Query 契约；
- Drive 自有 PostgreSQL Schema 的概念边界。

### 2.2 本子系统不负责

- Blob Placement、Storage Provider、多副本和 Archive 的物理实现；
- 通用 Device Registration、Sync Cursor Runtime 与 Pending Mutation Envelope；
- Photo 的 EXIF、GPS、Timeline、Album、Thumbnail / Preview 专业模型；
- Media 的播放版本、转码与字幕模型；
- Document Revision / Collaborative Editing；
- OS 文件监听 API 的唯一技术选型；
- iOS / Android 后台任务机制的具体实现；
- WebDAV / SMB / SFTP 的最终协议实现；
- 跨 Ikaros Instance Federation；
- 企业级多人协作盘、部门盘与复杂租户模型。

---

## 3. 与现有 V2 子系统的边界

### 3.1 Attachment / Blob / Storage

Drive 不保存物理路径、Bucket、Object Key 或 Provider URL 作为文件身份。

```text
Drive File Node
      ↓ 当前 Revision
File Revision
      ↓
Attachment
      ↓
Blob
      ↓
Placement / Storage Provider
```

同一个 Blob 可以被多个 Drive File Revision 或其他业务 Attachment 引用；物理去重由 Storage 负责。

### 3.2 Offline / Device Synchronization

Offline / Device Sync 负责：

- Device；
- Sync Cursor；
- Change Feed；
- Pending Mutation；
- Tombstone 传播；
- Client Operation ID；
- 通用可靠传输和重试。

Drive 负责：

- 文件树领域事实；
- 文件 Revision；
- 路径变更语义；
- Sync Binding；
- 文件级冲突判定；
- 本地文件与 Remote Node 映射；
- 同步策略。

因此：

> **Offline Sync Runtime ≠ Drive File Synchronizer。前者提供可靠同步基础设施，后者定义文件同步的业务语义。**

### 3.3 Content Ingestion

Content Ingestion 解决“外部 Source 中的内容如何被发现并导入 Ikaros 的 Resource 体系”。

Drive Sync 解决“某个用户明确绑定的本地目录 / 相册如何持续和远端文件空间收敛”。

两者可以共享 Fingerprint、Probe、Background Task 等能力，但不应复用成一个模糊的万能 Scanner。

### 3.4 Photo 子系统

手机照片备份必须同时满足两种用户视角：

```text
文件视角：Drive 中存在原始文件
          +
照片视角：Photos 中存在 Photo Resource / Timeline / EXIF
```

推荐流程：

```text
Device Camera Roll
      ↓ backup
Drive File Revision / Attachment
      ↓ domain projection
Photo Resource + Original Asset
```

Drive 拥有“文件已备份到哪个目录、同步状态是什么”；Photo 拥有“这是一张什么照片、拍摄时间、EXIF、Album、Preview”。

不能为了照片体验绕过 Drive / Storage 直接保存一套独立原图字节。

### 3.5 Sharing

Share 不改变 Drive Node 的拥有者，也不创建第二份文件。

Share 只是对 File / Folder Scope 的授权入口，最终访问仍必须经过 ACL / Permission。

---

## 4. 核心不变量

1. **Drive Node ID ≠ Path**：File / Folder 使用 UUIDv7，路径只是由父子关系和名称推导的用户视图。
2. **File Revision 不可变**：已提交 Revision 的 Attachment / Blob 不得原地改写。
3. **覆盖写创建新 Revision**：用户“替换文件”不能修改旧 Blob。
4. **Folder 不持有 Blob**：目录只是组织节点。
5. **移动 / 重命名不改变 File Identity**：只修改组织关系，不创建新文件内容。
6. **同名限制由父目录作用域决定**：同一父目录下默认禁止两个 ACTIVE Node 使用相同规范化名称。
7. **删除进入 Trash 不等于 Blob GC**：Trash、Revision Retention 与物理删除完全分离。
8. **本地路径不是服务端身份**：不同设备上的 `D:\Photos` 与 `/Users/a/Pictures` 可以映射到同一个 Remote Folder。
9. **同步不能静默丢数据**：无法安全合并的双端修改必须产生显式 Conflict。
10. **单向 Backup 不传播远端删除到本地**：除非用户明确启用对应危险策略。
11. **照片已上传 ≠ 已完成 Photo 入库**：Drive Backup Success 与 Photo Projection Success 分别可观测。
12. **客户端成功 Hash ≠ 服务端最终完整性确认**：服务端仍按 Storage 规则验证 Blob。
13. **物理去重 ≠ 业务合并**：相同 Blob 的两个 Drive File 可以保持不同名称、目录和生命周期。
14. **Sync Cursor 不能依赖本地时间**：使用服务端稳定游标 / Revision Token。
15. **Conflict Copy 不能覆盖原节点**：冲突副本必须保留可追踪来源。
16. **Root / Special Folder 是稳定身份**：不能依赖显示名称识别 Camera Uploads、Trash 等系统语义。

---

## 5. 领域模型总览

```text
User
 └── Drive Space
      ├── Root Folder
      │    ├── Folder Node
      │    │    ├── File Node
      │    │    │    └── File Revisions
      │    │    └── Folder Node
      │    └── File Node
      ├── Trash Scope
      └── Special Folders

Device
 └── Sync Binding
      ├── Local Root / Camera Roll
      ├── Remote Folder
      ├── Mode / Policy
      ├── Sync Cursor
      └── Item Mappings

Local Item ↔ Drive Node
          ↓
   Sync Item State
          ↓
 Conflict / Retry / Tombstone
```

---

## 6. Drive Space

Drive Space 表示一个用户可管理的个人文件空间。

V2 P0 推荐：

- 每个 User 至少拥有一个 Personal Drive Space；
- 一个用户未来可以扩展多个 Space，例如 Personal / Archive；
- 不把 Drive Space 等同于 Storage Provider 或 Bucket；
- Storage Policy 决定其字节落到哪里，但用户不需要知道 Provider Object Key。

建议字段：

```text
DriveSpace
├── id: uuid / UUIDv7
├── owner_user_id: uuid
├── display_name: text
├── root_node_id: uuid
├── default_storage_policy_id: uuid?
├── quota_policy_id: uuid?
├── state: ACTIVE | READ_ONLY | DISABLED
├── created_at: timestamptz
├── updated_at: timestamptz
└── version: bigint
```

### 6.1 Personal Space 与共享文件

共享给用户的文件默认不复制进其 Personal Space。

客户端可以提供虚拟入口：

- My Drive；
- Shared With Me；
- Recent；
- Favorites；
- Photos；
- Trash。

其中 `Shared With Me`、`Recent`、`Favorites` 多数是 Query / Projection，不是物理目录。

---

## 7. Drive Node

Drive Node 是文件树的稳定组织实体。

类型：

- `FILE`；
- `FOLDER`。

建议公共字段：

```text
DriveNode
├── id: uuid / UUIDv7
├── drive_space_id: uuid
├── parent_id: uuid?
├── node_type: FILE | FOLDER
├── name: text
├── normalized_name: text
├── lifecycle: ACTIVE | TRASHED | PURGED
├── created_by: uuid
├── created_at: timestamptz
├── updated_at: timestamptz
├── trashed_at: timestamptz?
└── version: bigint
```

### 7.1 Path 是投影

路径：

```text
/Documents/Project/spec.pdf
```

来自：

```text
Root -> Documents -> Project -> spec.pdf
```

Path 可以缓存用于查询优化，但不得成为实体主键或跨领域引用。

### 7.2 名称规范化

需要显式定义：

- Unicode normalization；
- 大小写敏感策略；
- 禁止名称；
- 尾随空格 / 点；
- Windows / macOS / Linux 跨平台兼容；
- 最大名称长度。

服务端应拥有统一规范化规则，否则 Windows 与 Linux 客户端可能产生不可收敛的名称冲突。

V2 推荐以“跨平台最小公分母”作为 P0 规则，并允许客户端在上传前预检。

### 7.3 同目录唯一性

推荐约束：

```text
UNIQUE(drive_space_id, parent_id, normalized_name)
WHERE lifecycle = 'ACTIVE'
```

Trash 中历史同名节点不阻止用户创建新的 Active Node。

---

## 8. File Node 与 File Revision

File Node 表示“用户认为是同一个文件槽位”的稳定身份。

File Revision 表示这个文件在某一时刻的不可变内容版本。

```text
File Node
├── current_revision_id
└── Revision History
     ├── rev 1 -> Attachment A -> Blob A
     ├── rev 2 -> Attachment B -> Blob B
     └── rev 3 -> Attachment C -> Blob C
```

建议字段：

```text
DriveFileRevision
├── id: uuid / UUIDv7
├── file_node_id: uuid
├── revision_no: bigint
├── attachment_id: uuid
├── content_hash: text?  # 可作为冗余查询字段，不替代 Blob 真相
├── size: bigint
├── media_type: text?
├── source_kind: UPLOAD | SYNC | COPY | RESTORE | IMPORT
├── source_device_id: uuid?
├── base_revision_id: uuid?
├── created_by: uuid
├── created_at: timestamptz
└── metadata: jsonb?
```

### 8.1 Revision Number

`revision_no` 只在单个 File Node 内单调增长。

它不是全局 ID，也不替代 UUIDv7。

### 8.2 乐观并发

更新现有 File 时，客户端必须携带它认为的 `base_revision_id` 或 ETag。

若服务端当前 Revision 已改变，则不能直接覆盖，应：

- 返回 Conflict；或
- 根据 Sync Policy 生成 Conflict Copy。

---

## 9. 文件操作语义

### 9.1 Create Folder

创建 Folder 只产生 Drive Node，不创建 Attachment / Blob。

### 9.2 Upload New File

```text
Create Upload Session
      ↓
Upload bytes / chunks
      ↓
Finalize Blob / Attachment
      ↓
Create File Node
      ↓
Create Revision 1
```

### 9.3 Replace File

```text
Existing File Node
      ↓ upload new bytes
New Attachment / Blob
      ↓
New File Revision
      ↓
CAS current_revision_id
```

旧 Revision 保留到 Retention Policy 决定是否清理。

### 9.4 Rename / Move

Rename / Move 只修改：

- `name`；
- `parent_id`。

不得创建新 Blob，也不应创建新 File Revision。

### 9.5 Copy

Copy 默认创建新的 Drive Node，但可以让初始 Revision 引用同一个 Attachment / Blob。

因此逻辑复制可以做到 O(1) 元数据复制，Storage 无需复制字节。

后续任一副本被替换时生成自己的新 Revision，不影响另一副本。

### 9.6 Folder Copy

大型目录复制必须作为 Background Task，并支持：

- 进度；
- 部分失败；
- Cancel；
- 幂等；
- 冲突策略。

---

## 10. 上传协议

### 10.1 小文件

小文件可以使用单请求 Upload，但最终仍应经过统一 Ingest / Finalize 语义。

### 10.2 大文件 / 弱网络

必须支持 resumable upload：

```text
Upload Session
├── expected size
├── optional expected hash
├── chunk policy
├── received ranges / parts
├── expires_at
├── actor / device
└── target intent
```

推荐允许实现：

- multipart；
- chunked upload；
- content-range；
- S3 multipart delegation。

具体传输协议可以后续在 API 设计中确定，但业务语义统一为 Upload Session。

### 10.3 Finalize

Finalize 前需要：

- 校验上传完整性；
- 计算 / 验证内容摘要；
- 创建或复用 Blob；
- 创建 Attachment；
- 原子提交 Drive Revision。

若字节上传成功但 Drive Revision 提交失败，必须允许清理孤立临时对象或通过后台 Reconcile 回收。

### 10.4 秒传 / 去重

客户端可以先提交 Hash Hint 询问服务端是否已有可复用 Blob，但：

- 必须经过当前用户的授权语义；
- 服务端不能通过“是否命中 Hash”泄露其他用户私有文件存在性；
- 即使命中 Blob，也仍需创建新的业务 Attachment / Revision。

---

## 11. 下载与预览

Drive 下载复用 Storage 的授权下载、Range、Restore、Cache 能力。

客户端只处理用户语义：

- Available；
- Remote；
- Restoring；
- Temporarily Unavailable；
- Missing / Corrupted。

不直接展示 Object Key。

对于可预览内容：

- 图片使用 Photo Preview / Thumbnail；
- 视频可使用 Media Playback Variant；
- 文档可使用 Document / Preview Artifact；
- 未知文件至少支持原始下载。

Drive 本身不重复实现所有格式预览器。

---

## 12. Trash / Restore / Permanent Delete

### 12.1 Trash

删除 File / Folder 默认进入 Trash。

Folder 进入 Trash 时，其后代节点保持树关系，以便整体 Restore。

需要记录：

- original parent；
- trashed_at；
- actor；
- optional deletion reason。

### 12.2 Restore

恢复时若原父目录仍存在且名称不冲突，则恢复原位置。

若冲突：

- 返回冲突供用户选择；或
- 根据策略自动重命名，例如 `file (restored).txt`。

### 12.3 Permanent Delete

Permanent Delete 是高风险动作。

它只删除 Drive 业务引用 / Revision 保留资格，不能直接承诺立即删除 Blob 字节。

Blob 是否可 GC 仍由 Storage 判断：

- 是否还有其他 Attachment 引用；
- 是否有 Retention Hold；
- 是否有 Revision Retention；
- 是否有 Share / Export / Backup 相关保留规则。

---

## 13. Revision Retention

Drive 可提供版本历史策略：

- 保留最近 N 个 Revision；
- 保留最近 N 天；
- 手动 Pin Revision；
- 永久保留（可选）；
- 配额压力下自动清理未 Pin 的旧 Revision。

清理旧 Revision 时只释放业务引用，不直接删除 Blob Placement。

版本恢复采用：

> **从历史 Revision 创建一个新的当前 Revision。**

不得通过把 `current_revision_id` 简单指回旧记录来伪造时间历史，否则后续审计和同步顺序会变得不清晰。

---

## 14. Sync Binding

Sync Binding 表示：

> “某个设备上的一个本地 Scope，按某种策略，与某个 Drive Folder 持续同步。”

建议字段：

```text
SyncBinding
├── id: uuid / UUIDv7
├── user_id: uuid
├── device_id: uuid
├── drive_space_id: uuid
├── remote_root_node_id: uuid
├── local_scope_id: stable client-generated id
├── local_display_path: protected text?
├── source_kind: DIRECTORY | CAMERA_ROLL | MEDIA_COLLECTION
├── mode: BACKUP | TWO_WAY | UPLOAD_ONLY | DOWNLOAD_ONLY
├── delete_policy
├── conflict_policy
├── network_policy
├── include_rules
├── exclude_rules
├── enabled: boolean
├── state
├── created_at
└── updated_at
```

### 14.1 Local Scope Identity

服务端不能把绝对本地路径当作 Binding 身份。

客户端应生成稳定 `local_scope_id`。

原因：

- 用户可能重命名本地目录；
- Windows Drive Letter 变化；
- Android SAF URI 变化；
- macOS Sandbox Bookmark；
- 路径可能包含隐私信息。

`local_display_path` 只用于用户诊断，按 Private / Sensitive 数据处理。

---

## 15. Sync Mode

### 15.1 BACKUP

适用于手机照片、工作资料备份等场景。

语义：

- 本地新增 / 修改上传到服务端；
- 服务端不会主动删除本地文件；
- 服务端重命名 / 移动默认不强制回写本地；
- 本地删除默认不删除云端已备份内容；
- 可选“本地删除后保留云端”是默认安全行为。

### 15.2 UPLOAD_ONLY

持续上传本地变更，但比 BACKUP 更接近目录镜像，可配置是否传播本地删除。

### 15.3 DOWNLOAD_ONLY

服务端为权威端，本地保持一个镜像副本。

### 15.4 TWO_WAY

双向同步：

- 本地增删改 → Remote；
- Remote 增删改 → Local；
- 必须有 Conflict Detection；
- 必须有 Tombstone；
- 不能仅靠文件修改时间解决冲突。

---

## 16. Sync Item Mapping

为了识别 rename / move，客户端与服务端需要维护稳定映射，而不能每次只比较路径。

建议概念：

```text
SyncItemMapping
├── binding_id
├── local_item_id
├── remote_node_id
├── local_parent_id?
├── last_synced_revision_id?
├── last_synced_fingerprint?
├── last_seen_local_metadata
├── last_seen_remote_version
├── state
└── updated_at
```

### 16.1 Local Item ID

不同平台实现可以不同：

- 文件系统 inode / file id + volume identity（可用时）；
- Android MediaStore ID；
- iOS Photos local identifier；
- 客户端持久化 sidecar mapping；
- 内容 + 元数据组合弱标识。

任何单一平台 ID 都不能成为 Drive Node 的全局主键。

### 16.2 Rename / Move Detection

优先信号：

1. stable local item id；
2. previous mapping；
3. strong content hash；
4. size + weak fingerprint；
5. path heuristic。

只有足够可信时才自动视为 rename / move。

否则宁可产生新文件并保留旧 Tombstone，也不要错误地把两个不同文件合并。

---

## 17. 增量同步流程

典型双向同步：

```text
Local Scan / OS Change Journal
        ↓
Local Delta
        ↓
Compare with Mapping + Last Synced Revision
        ↓
Push Mutations
        ↓
Drive Commands
        ↓
Server Change Feed / Cursor
        ↓
Pull Remote Delta
        ↓
Apply to Local
        ↓
Persist Mapping / Checkpoint
```

关键要求：

- 每项 Mutation 有稳定 Operation ID；
- Batch 部分失败可单项重试；
- Cursor 只在已安全应用到本地后推进；
- 客户端崩溃后可从 Checkpoint 恢复；
- 重复 Push 不产生重复 Revision；
- 重复 Pull 不重复创建本地文件。

---

## 18. 文件变化检测

变化检测分层：

### 18.1 快速元数据阶段

使用：

- local item id；
- size；
- mtime；
- directory entry generation；
- OS change journal / MediaStore change token。

这一步只用于发现候选变化。

### 18.2 Fingerprint 阶段

对疑似变化计算：

- fast fingerprint；
- partial hash；
- full cryptographic hash（必要时）。

`mtime + size` 不能作为最终内容身份。

### 18.3 大文件优化

对于超大文件：

- 先比较 stable item id / size / metadata；
- 必要时使用分块 hash；
- 最终上传完整性仍由 Blob Hash 保证。

---

## 19. Sync Conflict

### 19.1 冲突定义

如果双方都基于同一个 `last_synced_revision` 独立产生了新内容，则是内容冲突。

例如：

```text
Remote rev 10
  ↙       ↘
Local A   Remote B
```

不能靠“谁的 mtime 新”自动覆盖。

### 19.2 默认策略

P0 推荐：

- 保留 Remote 当前文件；
- 将 Local 上传为 Conflict Copy；
- Conflict Copy 与原 File 建立关联；
- 文件名包含设备 / 时间等可读后缀；
- UI 提示用户人工合并。

例如：

```text
report.docx
report (conflict from Laptop 2026-08-31).docx
```

命名只是展示策略，真正关联通过 Conflict Record。

### 19.3 Conflict Record

建议字段：

- id；
- binding_id；
- original_node_id；
- conflicting_node_id / revision_id；
- base_revision_id；
- source_device_id；
- detected_at；
- state: OPEN | RESOLVED | DISMISSED；
- resolution；
- resolved_at；
- resolved_by。

### 19.4 Metadata Conflict

重命名 / 移动也可能冲突，例如：

- Device A rename `a.txt -> b.txt`；
- Device B move `a.txt` 到另一个目录。

P0 可以使用保守规则：

- 内容变化优先保留；
- 无损组织操作尽量自动重放；
- 无法确定顺序时产生 Metadata Conflict。

---

## 20. 删除同步与 Tombstone

删除传播必须有稳定 Tombstone。

Drive Tombstone 至少包含：

- node id；
- deletion generation / sequence；
- deleted_at；
- actor / device；
- previous parent；
- previous name；
- retention deadline。

### 20.1 BACKUP 默认删除策略

手机照片 Backup：

> 本地删除默认不删除 Ikaros 中已备份文件。

这是备份与同步最重要的区别之一。

可以未来提供：

- “仅备份新增内容”；
- “本地释放空间但保留云端”；
- “同步删除”高级危险开关。

### 20.2 TWO_WAY 删除策略

双向同步中删除可以传播，但必须：

- 进入 Trash，而不是直接 Purge；
- 保留 Tombstone；
- 对长期离线设备保证删除不会复活；
- Cursor 太旧时要求 Full Resync。

---

## 21. 手机照片 / 视频自动备份

这是 Drive Sync 的专用策略，不是独立第二套上传系统。

### 21.1 Source

支持：

- iOS Photos Library；
- Android MediaStore；
- 用户选择的 Camera / Screenshots / WhatsApp 等媒体集合；
- 未来其他本地媒体集合。

### 21.2 Backup Binding

Photo Backup Binding 默认：

```text
source_kind = CAMERA_ROLL
mode = BACKUP
delete_policy = KEEP_REMOTE
conflict_policy = PRESERVE_BOTH
```

默认 Remote 目标可以是系统 Special Folder，例如：

```text
/Photos/Camera Uploads/{device-name}/
```

但服务端用 Special Folder ID 识别语义，不依赖这个显示路径。

### 21.3 增量发现

客户端应优先使用平台增量能力：

- Photos change token；
- MediaStore generation / query；
- persisted local media identity。

首次启用允许：

- 仅从现在开始；
- 备份最近 N 天；
- 备份全部历史。

### 21.4 已备份判定

不能只以文件名判断。

至少组合：

- local media id；
- source library identity；
- content fingerprint；
- server mapping；
- optional asset resource identifier。

### 21.5 HEIC / RAW / Live Photo

原则：

- 原始字节优先完整备份；
- 是否生成 JPEG Preview 属于 Photo Derived Asset；
- Live Photo 未来可将 image + video 作为 Companion Asset；
- RAW + JPEG 可投影为同一个 Photo 的多个 Original Role，但 Drive 中仍可保留明确文件节点。

### 21.6 Metadata

上传阶段可以提交：

- capture time hint；
- local asset id；
- media type；
- dimensions；
- location hint（若用户授权）；
- album / source collection hint。

这些只是候选来源，最终 EXIF / GPS / Capture Time 由 Photo 子系统解析和裁决。

### 21.7 后台限制

移动系统可能限制后台执行，因此客户端必须支持：

- foreground acceleration；
- background best effort；
- pending queue；
- Wi-Fi only；
- charging only（可选）；
- mobile data size threshold；
- low battery pause；
- retry with backoff。

服务端不能假设手机客户端持续在线。

### 21.8 用户删除本地照片

备份完成后用户可以选择本地释放空间。

Ikaros 必须能明确区分：

- local deleted；
- remote backup retained；
- remote photo available；
- local original cached / downloaded。

不能把“本地不存在”显示成“备份丢失”。

---

## 22. 通用目录同步

目录同步支持用户选择：

- Desktop folder；
- NAS mounted folder（客户端可访问时）；
- Android SAF folder；
- 其他客户端可持久访问的目录。

### 22.1 Include / Exclude

至少支持：

- glob；
- file extension；
- hidden files；
- max size；
- temporary file pattern；
- `.git` / build output 等默认建议忽略项。

过滤规则必须版本化并可解释。

### 22.2 Symlink

默认不跟随 symlink 逃逸 Sync Root。

可选策略必须明确：

- ignore link；
- sync link metadata（未来）；
- follow target only if target remains inside root。

### 22.3 Sparse / Placeholder

桌面端未来可以支持按需文件：

- 本地仅保留 Placeholder；
- 打开时下载；
- 用户可 Pin Offline。

但这属于客户端文件系统集成增强能力，P0 不要求虚拟文件系统驱动。

---

## 23. Sync State

Binding 状态建议：

- IDLE；
- SCANNING；
- SYNCING；
- PAUSED；
- DEGRADED；
- ERROR；
- AUTH_REQUIRED；
- FULL_RESYNC_REQUIRED；
- DISABLED。

单项状态建议：

- IN_SYNC；
- LOCAL_ONLY；
- REMOTE_ONLY；
- UPLOADING；
- DOWNLOADING；
- PENDING_DELETE；
- CONFLICT；
- ERROR；
- IGNORED。

`SYNCING` 不是 Background Task 的全局任务状态替代。大型 Full Scan / Initial Backup 可以关联 Background Task。

---

## 24. Full Resync

以下情况必须允许 Full Resync：

- Tombstone Retention 已过期；
- Cursor 无效；
- 客户端 Mapping DB 损坏；
- 用户更改关键 Sync Policy；
- 服务端检测映射不一致；
- 客户端重装后用户选择重新关联原 Binding。

Full Resync 必须是“重建映射”，不是“重新上传所有字节”。

应优先利用：

- Remote Node ID；
- local stable id；
- hash；
- file size；
- relative path；
- revision metadata。

来避免重复上传。

---

## 25. 配额与空间统计

Drive 对用户展示的是逻辑空间，不直接等同于 Storage Physical Size。

至少区分：

- Logical Current File Size；
- Revision Retained Size；
- Trash Retained Size；
- Physical Unique Blob Size（管理员统计）；
- Replica Physical Size（管理员统计）；
- Cache Size（不计入 Drive 逻辑配额，除非另有策略）。

### 25.1 去重与配额

P0 推荐用户配额按逻辑引用大小计费，而不是按物理去重后的 Blob 大小计费。

原因：

- 规则稳定；
- 不泄露其他用户是否已有同一 Blob；
- 用户行为可预测；
- 多用户共享 Blob 不产生复杂公平分摊。

实例管理员仍可在 Analytics 查看物理去重节省量。

---

## 26. 权限与安全

### 26.1 默认权限

Personal Drive 默认仅 Owner 可见。

用户显式 Share 后授予目标用户 / Token 对特定 Node Scope 的权限。

### 26.2 Folder ACL

Folder Share 可以产生继承 Scope，但最终 ACL 语义由 Security / Sharing 设计决定。

Drive 不自行实现第二套权限系统。

### 26.3 本地路径隐私

本地绝对路径可能包含：

- 用户真实姓名；
- 公司项目名；
- 隐私目录名。

因此：

- 普通 Event / Analytics 不记录完整绝对路径；
- 服务端最多保存受保护 Display Path，用于用户自己诊断；
- 日志默认只记录 Binding ID / Relative Path Hash 或必要片段。

### 26.4 Hash 隐私

Hash Dedup API 不得成为跨用户内容存在性探测接口。

### 26.5 恶意文件

Drive 允许保存任意用户文件，不应因为无法识别格式就拒绝。

但系统应为未来扩展保留：

- malware scanning artifact；
- suspicious file warning；
- executable download warning；
- archive bomb protection；
- preview sandbox。

这些安全检查不能无提示破坏原文件。

---

## 27. 专业领域投影

Drive File 可以触发可选领域投影：

```text
Drive File Revision
      ↓
Attachment
      ↓
Domain Classifier / User Intent
      ├── Photo Resource
      ├── Media Resource
      ├── Document Resource
      ├── Music Resource
      └── Generic File only
```

### 27.1 Generic File First

未知文件先作为 Generic Drive File 存在，不要求所有文件必须转成 Core Resource。

### 27.2 Auto Import Policy

用户或 Instance 可配置：

- 图片自动进入 Photos；
- 视频仅 Camera Roll 自动进入 Photos / Media；
- PDF / Office 不自动创建 Document；
- 特定 Folder 作为 Ingestion Source；
- 完全关闭自动领域投影。

### 27.3 删除边界

如果 Drive File 已被专业 Resource 引用，删除 Drive Node 不应绕过目标领域规则直接删除 Resource。

需要明确 Attachment 引用生命周期和“从 Drive 移除”与“删除内容资源”的不同语义。

---

## 28. Search / Recent / Favorites

Drive Search 至少支持：

- filename；
- extension / media type；
- folder scope；
- created / modified；
- size；
- owner / shared；
- content-derived metadata（若对应领域提供）。

`Recent` 是 Activity / Access Projection，不是特殊目录。

`Favorites` 可复用 Core User State 或 Drive 专用 Pin 语义，但需要保持跨端同步。

全文内容搜索由 Search / Document / OCR 等能力提供，不在 Drive 内自己实现第二套索引。

---

## 29. API / Command 语义

最终路径由 API Convention 决定，本文仅定义能力。

### 29.1 Query

至少需要：

- Get Drive Space；
- List Folder Children；
- Get Node Metadata；
- Resolve Breadcrumb / Ancestors；
- List Revisions；
- Search Drive；
- List Trash；
- List Sync Bindings；
- Get Sync Status；
- List Conflicts；
- Get Upload Session；
- Get Quota Usage。

### 29.2 Command

至少需要：

- Create Folder；
- Begin Upload；
- Finalize Upload；
- Replace File；
- Rename Node；
- Move Node；
- Copy Node；
- Trash Node；
- Restore Node；
- Request Permanent Delete；
- Restore Revision；
- Create Sync Binding；
- Update Sync Policy；
- Pause / Resume Sync Binding；
- Submit Sync Mutation Batch；
- Resolve Conflict；
- Request Full Resync；
- Start Initial Backup / Scan。

### 29.3 Idempotency

以下操作必须支持稳定 Idempotency Key / Operation ID：

- Finalize Upload；
- Create Revision；
- Sync Mutation；
- Folder Copy；
- Initial Backup batch；
- Conflict Copy creation。

---

## 30. Event

建议公开领域 Event：

- `drive.node.created`；
- `drive.node.renamed`；
- `drive.node.moved`；
- `drive.file.revision.created`；
- `drive.node.trashed`；
- `drive.node.restored`；
- `drive.node.purged`；
- `drive.sync.binding.created`；
- `drive.sync.binding.state_changed`；
- `drive.sync.conflict.detected`；
- `drive.sync.conflict.resolved`；
- `drive.photo_backup.completed`；
- `drive.photo_projection.failed`；
- `drive.quota.threshold_reached`。

Event 只携带必要业务事实，不包含：

- Storage Object Key；
- Secret；
- 本地绝对路径；
- 未授权 GPS；
- 大块文件内容。

所有 Event 使用 Event Contract Version。

---

## 31. 数据库概念模型

Drive 子系统建议拥有：

```text
drive_space

drive_node
  ├── file / folder common fields
  └── parent-child hierarchy

drive_file_revision

drive_special_folder

drive_trash_metadata  # 可合并进 node lifecycle metadata

drive_sync_binding

drive_sync_item_mapping

drive_sync_conflict

drive_sync_tombstone / change projection

drive_quota_projection
```

### 31.1 Schema Ownership

Drive 表由 Drive Migration 拥有。

Storage、Photo、Offline Sync 等子系统不得直接更新 Drive 私有表。

### 31.2 外键策略

领域内部强一致引用可以使用外键，例如：

- revision -> file node；
- node -> drive space；
- node -> parent node。

跨领域引用例如 Attachment ID、Device ID、User ID 是否使用物理 FK，应遵守 Database Overview 的边界和部署策略，不通过跨领域 JOIN 修改对方状态。

### 31.3 层级查询

Folder Tree 可以使用：

- adjacency list（`parent_id`）作为事实模型；
- recursive CTE 查询祖先 / 后代；
- 可选 materialized path / closure projection 作为性能优化。

不能把缓存 Path Projection 变成唯一真相。

---

## 32. 一致性与事务边界

### 32.1 创建 Revision

需要保证：

- Attachment 已成功创建；
- Revision 唯一；
- File current revision CAS 成功；
- Outbox Event 与领域提交同事务。

若 CAS 失败，产生 Conflict，不覆盖别人刚提交的 Revision。

### 32.2 Move / Rename

同一 Drive Schema 内可以 ACID 保证：

- parent 存在；
- 不形成目录循环；
- 目标名称不冲突；
- node version 更新。

### 32.3 跨领域 Photo Projection

Drive Upload 成功与 Photo Resource 创建采用最终一致性：

```text
Drive Revision committed
      ↓ outbox
Photo Projection Consumer
      ↓
Photo Resource / Asset
```

Photo Projection 失败不回滚原始备份。

---

## 33. 目录循环与树约束

Move Folder 时必须防止：

```text
A
└── B
    └── C

Move A into C  # invalid
```

实现可以使用 recursive CTE / ancestry projection 检查。

并发 Move 必须结合 Node Version / Lock 防止竞态形成循环。

---

## 34. Background Task

以下操作建议进入 Background Task：

- 大目录 Copy；
- 大目录 Trash / Restore；
- Initial Sync Scan；
- Full Resync；
- 历史照片首次备份；
- 大批量 Revision Retention Cleanup；
- Drive Integrity Reconcile；
- Domain Projection Rebuild。

任务状态继续使用系统级：

`Pending / Running / Succeeded / Failed / Cancelled / TimedOut`。

Drive 可以有业务阶段，例如 `Scanning / Uploading / Applying Remote Changes`，但不能替代系统 Task State。

---

## 35. 可观测性

需要至少统计：

- active drive users；
- node count；
- current logical bytes；
- revision retained bytes；
- trash bytes；
- upload throughput；
- dedup hit ratio（管理员）；
- sync bindings by mode；
- backup pending count；
- conflict count；
- failed item count；
- full resync count；
- mobile backup success rate；
- average sync lag；
- Photo projection lag / failure。

日志 / Trace 使用：

- request id；
- task id；
- binding id；
- operation id；
- node id；
- attachment id；

而不是记录完整本地路径和 Secret。

---

## 36. 故障恢复

### 36.1 Upload Interrupted

Upload Session 保留已完成 Part，客户端恢复后续传。

过期 Session 后由 Reaper 清理临时对象。

### 36.2 Finalize 重复请求

通过 Idempotency Key 返回同一结果，不重复创建 Revision。

### 36.3 Client Crash During Apply

客户端必须先落本地事务 / Journal，再推进 Cursor。

### 36.4 Server Crash During Sync Batch

每项 Operation 独立幂等；已提交项不会因整个 Batch 重试而重复执行。

### 36.5 Storage Temporary Failure

Drive Node / Revision 元数据不能因为短暂 Provider 故障消失。

文件显示为 Remote / Unavailable / Restoring 等专业可用性状态。

---

## 37. P0 / P1 / Future

### 37.1 P0

- Personal Drive Space；
- File / Folder Tree；
- Upload / Download / Range；
- resumable upload；
- Rename / Move / Copy；
- Trash / Restore；
- immutable File Revision；
- Revision Restore；
- Desktop / Mobile directory BACKUP；
- TWO_WAY directory sync；
- Sync Binding / Mapping / Cursor；
- Conflict Copy；
- Camera Roll automatic backup；
- Wi-Fi / mobile data policy；
- Photo projection；
- Share integration；
- logical quota；
- Background Task / audit / events。

### 37.2 P1

- selective sync；
- placeholder / online-only file；
- pinned offline folder；
- richer revision retention UI；
- duplicate management UI；
- remote folder rule / automatic organization；
- richer mobile album selection；
- storage optimization after verified backup；
- integrity reconcile dashboard；
- malware scanning plugin integration。

### 37.3 Future

- virtual filesystem driver；
- WebDAV endpoint；
- LAN accelerated transfer；
- peer-assisted transfer；
- Trusted Edge Storage；
- collaborative shared drive；
- end-to-end encrypted drive scope；
- filesystem snapshot integration；
- block-level delta upload for very large mutable files；
- content-defined chunking；
- cross-instance replication / federation。

---

## 38. 不采用的设计

### 38.1 不把 Storage Provider 当作网盘目录

错误：

```text
Drive Folder = /mnt/data/user1
```

问题：

- 无法迁移 Provider；
- 无法多副本；
- 路径泄露基础设施；
- 无法稳定分享和版本化。

### 38.2 不把 Blob 当作文件

Blob 只有不可变字节身份，没有：

- filename；
- parent folder；
- trash；
- user revision history；
- sync mapping。

### 38.3 不使用 Last Modified Time 做双向冲突决策

客户端时间可能：

- 不同步；
- 被用户修改；
- 文件拷贝时保留旧时间；
- 不同文件系统精度不同。

必须使用 Revision / Base Version。

### 38.4 不让 Camera Backup 直接等于 Photo Resource 上传 API

否则会失去：

- 通用文件备份；
- 目录组织；
- 原始字节统一 Revision；
- Drive 与 Photo 的清晰边界。

### 38.5 不默认传播本地删除

尤其 Camera Backup 场景，备份的意义就是本地可以释放空间而不丢远端内容。

---

## 39. 关键用户流程

### 39.1 Web 上传文件

```text
用户进入 My Drive
  ↓
选择文件
  ↓
Begin Upload Session
  ↓
上传 / 续传
  ↓
Finalize
  ↓
创建 File Node + Revision
  ↓
可选专业领域投影
  ↓
列表出现文件
```

### 39.2 手机首次备份照片

```text
用户授权 Photos / MediaStore
  ↓
创建 Camera Roll Backup Binding
  ↓
选择：全部历史 / 最近 N 天 / 从现在开始
  ↓
增量枚举本地资产
  ↓
过滤已备份 Mapping
  ↓
后台 resumable upload
  ↓
Drive Revision committed
  ↓
Photo Projection
  ↓
Photos Timeline 可见
```

### 39.3 两台电脑同步同一目录

```text
PC-A edit report.docx from rev 5
PC-B edit report.docx from rev 5

PC-A uploads -> rev 6
PC-B later uploads with base rev 5
          ↓
Conflict detected
          ↓
keep report.docx = rev 6
create report (conflict from PC-B).docx
          ↓
user manually resolves
```

### 39.4 本地删除已备份照片

```text
Phone local photo deleted
  ↓
Binding mode = BACKUP / KEEP_REMOTE
  ↓
mapping records local missing
  ↓
Remote Drive File remains
  ↓
Photo Timeline remains available
```

---

## 40. 设计结论

Ikaros V2 的网盘子系统应被定位为：

> **建立在 Attachment / Blob / Storage 之上的用户文件空间，并通过 Device Sync Runtime 提供可靠的目录与手机媒体同步，再通过领域投影把文件接入 Photos、Media、Document 等专业内容体验。**

最终核心分层为：

```text
User-visible File Space
Drive Space / Folder / File
          ↓
Immutable File Revision
          ↓
Attachment / Blob
          ↓
Storage Provider / Replica
```

同步分层为：

```text
Local Directory / Camera Roll
          ↓
Sync Binding + Mapping
          ↓
Drive Command / Revision Conflict Rules
          ↓
Offline Sync Runtime / Cursor / Mutation
          ↓
Remote Drive Tree
```

照片备份分层为：

```text
Camera Roll
   ↓ Backup
Drive File + Original Attachment
   ↓ Projection
Photo Resource / Timeline / Album / EXIF
```

这样既能实现传统“个人网盘”的通用文件体验，也能实现类似 Immich 的手机照片自动备份，并为桌面目录双向同步、跨设备一致性、版本历史、回收站、分享和未来虚拟文件系统留下稳定演进空间。
