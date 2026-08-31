# Ikaros V2 Content Creation / Revision / Collaborative Document 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Content Creation / Revision / Collaborative Document 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Sharing-Collaboration-Room-Subsystem-Design.md`、`AI-Intelligence-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 普通内容创作领域中 Document、Article、普通 Note、Working Copy、Revision、Publication、Comment / Annotation 与协同编辑之间的服务端边界。
>
> 本文档中的普通文档不是 Secure Domain。高度敏感笔记继续进入 `Private-Notes-Subsystem-Design.md`；不能通过给普通 Document 增加一个 `is_private` 字段来替代端到端加密安全模型。

---

## 1. 设计目标

本子系统需要回答：

1. Article、Document、普通 Note 如何复用 Resource 身份，同时保留创作领域的专业语义。
2. 编辑中的 Working Copy 与不可变 Revision 如何分离。
3. 自动保存如何避免“每敲一个字就产生一个 Revision”。
4. Revision 如何支持历史查看、Diff、恢复，并保证历史不可被静默改写。
5. 多设备和多用户同时编辑时如何检测、合并和呈现冲突。
6. 实时协作与 Room / Presence 的边界在哪里。
7. 发布 Article 时如何固定发布内容，而不是让公开页面随未完成草稿实时变化。
8. 定时发布、撤回、重新发布如何形成明确状态机。
9. 文档中的 Attachment、Resource Embed、Link 和 Comment Anchor 如何稳定引用。
10. AI Writing Assistant 如何提供建议而不直接成为正文事实源。
11. Search、Analytics、Automation、Notification 如何消费创作事件而不直接修改文档内部表。
12. 普通文档离线编辑如何与未来 Device Sync 设计兼容。

核心原则：

> **Working Copy 表达“正在编辑什么”；Revision 表达“曾经正式保存过什么”；Publication 表达“对外发布的是哪个确定版本”。三者不得合并成一个可随意覆盖的正文记录。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Article / Document / 普通 Note 的创作领域模型；
- Working Copy / Draft；
- Revision 与版本历史；
- Revision Restore；
- Publication / Scheduled Publication；
- 文档结构内容与 Schema Version；
- Attachment / Resource Embed 引用；
- Comment / Annotation 的文档锚点；
- 编辑并发控制；
- 实时协作的文档侧契约；
- 冲突检测与 Merge 结果持久化；
- AI Writing Suggestion 的应用边界；
- Search / Analytics / Automation 的创作事件。

### 2.2 本子系统不负责

- Resource、Collection、Tag 的通用生命周期；
- Attachment / Blob 的字节存储、复制与 GC；
- Share Token、Room Membership、Presence 的通用实现；
- Private Notes 的 E2EE；
- 最终 CRDT / OT 第三方库选型；
- 具体富文本编辑器框架；
- 外部博客平台的 API 适配；
- Notification Provider；
- Search Engine 的物理索引实现。

---

## 3. 核心不变量

1. **创作对象拥有稳定 Resource ID**：Document / Article / Note 的内部身份不随 Revision 改变。
2. **Revision 一旦提交即不可变**：历史 Revision 只能新增、保留或按明确 Retention Policy 清理，不能原地修改正文。
3. **恢复旧版本必须产生新 Revision**：Restore 不得删除后续 Revision，也不得把旧 Revision 改成当前记录。
4. **Working Copy 不等于 Revision**：频繁 autosave 可以更新工作副本，但不应无控制地产生成百上千正式版本。
5. **发布内容必须绑定确定 Revision**：Public / Shared Publication 不能指向一个仍持续变化的 Working Copy。
6. **客户端不能以 Last Write Wins 静默覆盖并发编辑**：版本不匹配必须 Merge、Reject 或形成 Conflict。
7. **Attachment 引用只保存稳定 Attachment ID**：禁止把临时本地路径、对象存储 Key 写进文档正文作为永久身份。
8. **Resource Embed 默认保存引用而不是复制完整元数据**：动态显示由 Resource Capability 读取；需要静态快照时必须显式声明。
9. **评论锚点允许失效但不能错误指向其他文本**：内容变化无法安全 re-anchor 时应标记 detached / outdated。
10. **AI 默认只生成 Suggestion**：AI 结果必须由用户或明确自动化规则应用后才进入正文 Revision。
11. **普通管理员权限不自动绕过文档 ACL**：管理入口与具体文档读取权限必须分离。
12. **创作领域事件可靠传播**：`document.revision.created`、`article.published` 等关键事实应通过平台可靠 Event 机制传播。

---

## 4. 领域模型总览

```text
Resource (document / article / note)
        │
        └── Creation Document Aggregate
              ├── Working Copy
              ├── Revision 1
              ├── Revision 2
              ├── Revision 3
              ├── Publication
              ├── Comment / Annotation
              └── Collaboration Metadata
```

Resource 负责：

- 稳定内部身份；
- 标题 / Alias 的平台级能力；
- Collection / Tag；
- 生命周期；
- Search 的通用入口。

Creation 子系统负责：

- 正文结构；
- Revision；
- 草稿编辑；
- 发布；
- 文档级协作语义。

---

## 5. Document Kind

V2 初期至少支持：

- `note`：普通轻量笔记；
- `document`：通用结构化文档；
- `article`：具有发布语义的文章 / 博客内容。

Document Kind 是专业领域语义，不应仅通过 Tag 判断。

共同能力可以复用，但不同 Kind 可具有不同规则：

| 能力 | Note | Document | Article |
|---|---:|---:|---:|
| Revision | ✅ | ✅ | ✅ |
| Collaboration | 可选 | ✅ | ✅ |
| Publish | 通常无 | 可选 | ✅ |
| Scheduled Publish | 无 | 可选 | ✅ |
| Comment / Annotation | 可选 | ✅ | ✅ |
| Search | ✅ | ✅ | ✅ |

Private Note 不属于这里的 `note`。

---

## 6. Document Aggregate

Document Aggregate 至少具有：

- `id`：UUIDv7，或与对应 Resource 使用稳定的一对一引用；
- `resource_id`；
- `kind`；
- `owner_id`；
- `content_schema_version`；
- `current_revision_id`；
- `working_copy_version`；
- `publication_state`；
- `created_at` / `updated_at`；
- 乐观并发版本。

是否让 Creation Document ID 与 Resource ID 完全相同可由实现决定，但公共契约必须清楚表达两者关系，不应为前端制造两套无法解释的身份。

---

## 7. 文档内容模型

### 7.1 结构化内容优先

正文应有明确的 Content Schema，而不是后端完全不理解的任意 JSON。

可以支持：

- Paragraph；
- Heading；
- List；
- Checklist；
- Quote；
- Code Block；
- Table；
- Callout；
- Image；
- Attachment；
- Resource Embed；
- Link；
- Mention；
- Plugin-defined Block（受版本约束）。

实现可选 Markdown、富文本 AST、Block Tree 或其他模型，但需要满足：

1. 可版本化；
2. 可验证；
3. 可稳定序列化；
4. 可从 Revision 重建；
5. 可识别 Attachment / Resource 引用；
6. 不依赖某一 Web 编辑器私有 DOM 才能解释。

### 7.2 Content Schema Version

每个 Revision 应记录 `content_schema_version`。

编辑器格式升级时需要：

- 向后读取旧版本；
- 或通过显式 Migration 转换；
- 禁止因为前端升级导致旧 Revision 无法渲染。

Content Schema Version 与 Database Schema Version、API Version、Revision Number 不同。

---

## 8. Stable Block Identity

对需要评论锚点、协作 Cursor、Diff 或引用的结构化 Block，建议具有稳定 Block ID。

Block ID：

- 在同一文档的正常编辑中尽量保持稳定；
- 不作为平台全局 Resource ID；
- 复制 Block 时生成新 ID；
- Merge 时必须避免两个并发新 Block 使用同一身份。

稳定 Block Identity 能降低评论 / Annotation 在文本变更后的漂移问题。

---

## 9. Working Copy

Working Copy 表达当前尚未形成正式 Revision 的编辑状态。

至少包含：

- document id；
- content；
- base revision id；
- working copy version；
- last editor；
- last saved at；
- optional change summary；
- optional collaboration sequence checkpoint。

### 9.1 Autosave

Autosave 默认更新 Working Copy。

推荐策略：

- 用户停止输入一小段时间后 debounce；
- 页面失焦 / 切换时尝试保存；
- Ctrl/Cmd+S 强制持久保存；
- 客户端退出前尽量刷新；
- Offline 时先进入本地 Pending Change。

服务器必须限制：

- autosave 请求频率；
- 单次正文大小；
- 超大 Patch；
- 过期版本写入。

### 9.2 Working Copy 崩溃恢复

Working Copy 应具有独立持久化语义，使应用或浏览器异常退出后可以恢复最近编辑。

但 Working Copy 不是审计级历史；其旧快照可以按短期策略清理。

---

## 10. Revision

Revision 是一个不可变的文档版本。

Revision 至少具有：

- UUIDv7；
- document id；
- revision number / sequence；
- parent revision id；
- content snapshot 或可可靠重建的内容引用；
- content schema version；
- author / actor；
- created_at；
- change summary；
- provenance；
- optional merge parents；
- optional source working copy version。

### 10.1 Revision Number

Revision Number 只在单个 Document 内有序，例如：

```text
Document A: 1, 2, 3, 4
Document B: 1, 2
```

它不是全局实体 ID。

数据库应保证一个 Document 内 Revision Sequence 唯一。

### 10.2 Snapshot 与 Delta

实现可以：

- 每个 Revision 保存完整 Snapshot；
- 保存 Delta + 周期性 Snapshot；
- 或使用适合的内容版本存储。

无论物理方案如何，必须满足：

> 任意被保留 Revision 都可以在不依赖当前 Working Copy 的情况下可靠重建。

不能为了节省空间让历史版本依赖一个可能已被删除的客户端缓存。

---

## 11. Revision Commit 策略

正式 Revision 可在以下时机创建：

- 用户显式保存版本；
- 发布前自动创建；
- 离开一段持续编辑会话后按策略创建；
- 合并冲突成功后创建；
- 恢复旧版本时创建；
- 重要自动化修改后创建。

不建议每次 autosave 都创建 Revision。

系统可以配置：

- minimum revision interval；
- significant change threshold；
- explicit checkpoint；
- publish always commits。

---

## 12. Restore Revision

恢复旧版本流程：

```text
Current Revision = 12
User selects Revision 5
        ↓
Preview / Diff
        ↓
RestoreRevision(document, revision=5)
        ↓
Create Revision 13
content = Revision 5 content
provenance = RESTORE
        ↓
Current Revision = 13
```

规则：

- Revision 6–12 继续保留；
- Revision 5 不发生修改；
- Restore 操作写入 Audit / Activity；
- 如果当前存在未提交 Working Copy，必须先明确保留、丢弃或另存。

---

## 13. 并发编辑模型

### 13.1 单用户多设备

基础场景使用：

- `base_revision_id`；
- `working_copy_version`；
- ETag / If-Match；
- client operation id。

客户端提交时若基线落后，服务端不得静默覆盖新版本。

可能结果：

- ACCEPT；
- AUTO_MERGED；
- CONFLICT；
- STALE_BASE；
- PERMISSION_CHANGED。

### 13.2 三方合并

非实时编辑可使用：

```text
Base
Mine
Remote
```

进行三方 Merge。

结构化内容应尽量以 Block / Node 语义合并，不应只对整个 JSON 字符串做文本覆盖。

### 13.3 冲突持久化

无法自动合并时，可以创建 Conflict Record，至少记录：

- document id；
- base revision；
- local candidate；
- remote current；
- detected at；
- actor / device；
- resolution status。

冲突解决后创建新的 Revision。

---

## 14. 实时协作文档

实时协作必须同时遵守：

- 本文档的 Document / Revision 规则；
- `Sharing-Collaboration-Room-Subsystem-Design.md` 的 Membership、Presence、Sequence、Reconnect 与权限规则。

Room 负责：

- 谁在协作；
- Presence；
- Connection；
- Reconnect / Replay；
- 实时权限收敛。

Document 子系统负责：

- 正文内容；
- Change 合法性；
- Document Operation Sequence；
- Revision Commit；
- Merge / Conflict；
- Comment Anchor。

### 14.1 CRDT / OT 边界

V2 设计阶段不强制锁定 CRDT 或 OT。

无论选择哪种算法，都必须提供稳定的领域契约：

```text
Client Change
    ↓
Document Collaboration Handler
    ↓ permission + base/sequence validation
Authoritative Operation / Merge
    ↓
Realtime Broadcast
    ↓
Periodic Durable Working Copy
    ↓
Revision Checkpoint
```

算法实现不得改变以下事实：

- Revision 是持久版本；
- Presence 不是 Revision；
- Room 断开不删除文档；
- 权限撤销后不能继续提交 Change。

### 14.2 实时操作日志

高频 Operation 不一定全部进入平台 Integration Event Outbox。

可以保留短期 Document Operation Log 用于：

- reconnect replay；
- sequence recovery；
- crash recovery。

达到 Revision checkpoint 后，旧 Operation 可按策略压缩 / 清理。

---

## 15. Comment / Annotation

Comment 可以绑定：

- 整篇 Document；
- Block；
- Text Range；
- Revision Snapshot。

Comment 至少包含：

- id；
- document id；
- author；
- anchor；
- body；
- status：OPEN / RESOLVED / DELETED；
- created_at / updated_at；
- parent comment id（回复）；
- anchor revision id。

### 15.1 Anchor

推荐 Anchor 保存：

- block id；
- optional start / end offset；
- selected text fingerprint；
- anchor revision id。

内容变化后：

1. Block 仍存在且范围可安全映射 → re-anchor；
2. 无法确定 → 标记 `DETACHED`；
3. 禁止悄悄指向另一个相似文本。

Resolved Comment 不代表删除历史。

---

## 16. Attachment 与 Embed

### 16.1 Attachment Node

文档节点仅引用：

- Attachment ID；
- display metadata；
- optional caption / alt text。

上传过程：

```text
Client Upload
   ↓
Attachment / Blob subsystem
   ↓
Attachment available
   ↓
Insert Attachment Reference
   ↓
Document Working Copy
```

临时上传失败不能留下一个假装可用的永久 Attachment ID。

### 16.2 Resource Embed

默认保存：

- Resource ID；
- display mode；
- optional explicit snapshot metadata。

如果是动态 Embed，渲染时读取当前有权限的 Resource。

如果是 Publication 的静态快照，需要显式记录 Snapshot，不能混淆两种语义。

### 16.3 引用与删除

文档 Revision 对 Attachment 的有效引用会影响 Blob GC。

因此：

- 删除当前正文中的图片不意味着 Blob 可立即删除；
- 历史 Revision 仍可能引用 Attachment；
- Revision Retention 清理后才可能释放最后引用；
- Attachment GC 由 Storage 子系统最终判断。

---

## 17. Publication

Publication 表示 Article / Document 的发布状态与已发布版本。

至少包含：

- id；
- document id；
- published revision id；
- status；
- visibility / ACL reference；
- slug / external path（如果支持）；
- published_at；
- scheduled_at；
- publisher；
- optional external publication references。

### 17.1 状态

建议：

```text
DRAFT
  ↓ publish
PUBLISHED
  ├── update publication
  ├── unpublish -> UNPUBLISHED
  └── archive -> ARCHIVED
```

Document 的 Resource Lifecycle 与 Publication State 分离。

例如：

- Resource 仍 ACTIVE；
- Article Publication 可以 UNPUBLISHED。

### 17.2 Publish

发布流程：

```text
Validate permission
    ↓
Validate document
    ↓
Commit current Working Copy as Revision (if needed)
    ↓
Create / Update Publication → revision N
    ↓
transaction commit + durable event
    ↓
article.published
    ↓
Search / Notification / Automation / Analytics
```

发布后继续编辑 Working Copy，不应自动改变已经发布的 Revision。

用户再次发布时显式把 Publication 指向新 Revision。

### 17.3 Scheduled Publish

定时发布保存：

- target document；
- target revision 或“执行时先生成 revision”的明确策略；
- scheduled time + timezone context；
- actor / permission context；
- visibility snapshot。

推荐在预约时固定 `target_revision_id`，防止定时任务执行时意外发布后来尚未确认的草稿。

Scheduled Job 到期后调用 `PublishDocument` Command，而不是直接改状态字段。

### 17.4 Slug

Slug 是路由 / 展示标识，不是 Document 内部身份。

如果支持公开 URL：

- 同一作用域内唯一；
- 可修改；
- 修改后可选择保留 redirect；
- API 仍以 UUIDv7 为稳定身份。

---

## 18. Template

Template 可以定义：

- kind；
- initial content；
- metadata defaults；
- required blocks；
- optional variables。

从 Template 创建文档后：

- 新 Document 拥有自己的身份；
- 不与模板正文保持隐式实时绑定；
- 如果未来支持 Template Update Propagation，必须作为独立显式能力设计。

---

## 19. AI Writing Assistant

AI 可提供：

- rewrite；
- expand；
- shorten；
- translate；
- grammar check；
- outline；
- summary；
- citation-aware draft；
- authorized Resource retrieval。

AI 输出默认形成 Suggestion：

```text
AI Suggestion
    ↓ user accepts
Document Change Command
    ↓
Working Copy
    ↓ optional Revision
```

必须记录必要 Provenance：

- model / provider（按隐私策略）；
- generated_at；
- source context reference；
- actor；
- suggestion id。

禁止 AI：

- 静默替换整个正文；
- 绕过 Document Permission；
- 自动发布文章，除非用户配置的 Automation 明确允许且风险策略通过；
- 读取用户无权访问的 Resource。

---

## 20. Search 与索引

Search Projection 可包含：

- title；
- current published / current readable content；
- summary；
- tag；
- author；
- language；
- publication status。

必须明确：

- Draft 是否进入用户私有搜索；
- Published content 是否进入共享搜索；
- 历史 Revision 默认不作为普通全文搜索结果；
- 未授权用户不能通过搜索推断 Draft 标题或内容。

索引失败不回滚已提交 Revision / Publication，使用 Event 重试。

---

## 21. Offline 与未来 Device Sync

当前文档只定义兼容要求，不替代未来 Offline Sync 专项设计。

客户端离线至少需要维护：

- Document ID；
- base Revision；
- local Working Copy；
- pending operation id；
- local saved at；
- attachment upload pending reference。

Reconnect：

```text
local base == server base
→ apply pending

server advanced but mergeable
→ merge

conflict
→ Conflict Resolver
```

删除传播、设备撤销、本地加密、全局 sync cursor 由 Offline / Device Sync 设计继续定义。

---

## 22. Command 契约

典型 Command：

- CreateDocument
- UpdateWorkingCopy
- CommitRevision
- RestoreRevision
- ResolveDocumentConflict
- ChangeDocumentKind（仅允许明确迁移）
- AddDocumentAttachment
- RemoveDocumentAttachment
- CreateComment
- ResolveComment
- ReopenComment
- PublishDocument
- SchedulePublication
- CancelScheduledPublication
- UnpublishDocument
- ArchivePublication
- ApplyAISuggestion

Command 必须：

- 权限校验；
- Version / ETag 校验；
- Idempotency；
- Content Schema 校验；
- Attachment / Resource 引用权限校验；
- 重要状态迁移产生 Event。

---

## 23. Query / Capability

公开读取能力可包括：

- GetDocument
- GetWorkingCopy
- GetCurrentRevision
- ListRevisions
- CompareRevisions
- GetPublication
- ListComments
- ResolveDocumentReferences
- CanEditDocument
- CanPublishDocument

跨子系统读取返回稳定 DTO，不暴露内部 Persistence Entity。

---

## 24. Integration Event

建议事件：

- `document.created`
- `document.working-copy.updated`（仅在确有跨域价值时，避免高频事件风暴）
- `document.revision.created`
- `document.revision.restored`
- `document.conflict.detected`
- `document.comment.created`
- `document.comment.resolved`
- `article.published`
- `article.publication.updated`
- `article.unpublished`

高频实时字符 / operation 不进入平台通用 Integration Event Bus。

---

## 25. 数据库关键约束

具体表名由实现确定，但至少满足：

1. 实体 ID 使用 UUIDv7 / PostgreSQL `uuid`。
2. 时间点使用 `timestamptz`。
3. 一个 Document 内 Revision Sequence 唯一。
4. Revision 内容提交后禁止业务层 UPDATE 修改。
5. `current_revision_id` 必须属于当前 Document。
6. Publication 指向的 Revision 必须属于当前 Document。
7. Comment Anchor 的 Document / Revision 关系必须可验证。
8. Working Copy Version 用于并发控制。
9. Idempotency Key 对受重试 Command 建立可靠唯一约束。
10. 不通过跨域 FK 强绑定 Storage / Share 私有表；使用稳定公共 ID 和 Capability 验证。

---

## 26. 权限与安全

建议能力至少区分：

- read；
- comment；
- edit；
- manage_metadata；
- manage_collaborators；
- publish；
- archive；
- delete；
- restore_revision。

规则：

- 可以 edit 不自动代表可以 publish；
- 可以 comment 不自动代表可以 edit；
- Share Grant 不能超过目标对象当前可授权能力；
- Plugin / AI / Automation 都以明确 Principal 调用 Command；
- 外链和嵌入内容渲染需要 XSS / Sanitization / URL 安全策略；
- 用户输入的 HTML 若被支持必须进行严格 Sanitization。

---

## 27. 生命周期与删除

Document 的 Resource Lifecycle 沿用 Core Resource：

- ACTIVE；
- ARCHIVED；
- TRASHED；
- permanent delete。

Creation 领域必须额外处理：

- Working Copy；
- Revision；
- Publication；
- Comments；
- Attachment references。

进入 TRASHED：

- 默认停止 Publication 访问或按产品策略显式处理；
- 不立即删除 Revision；
- 不立即释放历史 Attachment 引用。

Permanent Delete：

1. Step-up / high-risk confirmation（按策略）；
2. revoke publication / share；
3. close collaboration session；
4. remove search projections；
5. 清理 / tombstone Revision metadata；
6. 释放 Attachment 引用；
7. 由 Storage 判断 Blob GC；
8. 保留最小 Audit。

---

## 28. 可观测性

至少监控：

- autosave success / failure；
- revision commit latency；
- conflict count；
- merge failure；
- active collaborative documents；
- realtime operation backlog；
- publication success / failure；
- scheduled publication lag；
- detached comment anchor count；
- search projection lag；
- document size / revision growth distribution。

日志可记录 Document ID、Revision ID、Correlation ID，但不得无必要记录完整正文。

---

## 29. 测试与验收基线

实现至少覆盖：

1. autosave 多次不会无控制创建正式 Revision。
2. Revision 提交后不可被原地修改。
3. Restore Revision 5 会创建新的 Revision，而 Revision 6+ 仍存在。
4. 两个设备基于同一旧版本编辑时不会静默 Last Write Wins。
5. 可自动合并修改产生一个可追踪的新版本。
6. 不可自动合并修改产生 Conflict Resolver 所需数据。
7. 发布时固定 Revision；发布后继续编辑不会改变线上版本。
8. Scheduled Publish 不会意外发布预约后产生的未确认草稿。
9. 文档删除图片后，只要旧 Revision 仍引用 Attachment，Blob 不被错误 GC。
10. 评论 Anchor 无法重定位时标记 detached，而不是错误绑定。
11. 用户失去编辑权限后，已有实时连接不能继续提交 Change。
12. AI Suggestion 未被接受前不修改正文。
13. 未授权用户无法通过 Search / Revision / Comment API 获取 Draft 信息。
14. 重复 Publish / Commit 请求按 Idempotency 规则处理。
15. Content Schema 旧版本 Revision 在升级后仍可读取或迁移。

---

## 30. P0 / P1 / P2 建议

### P0

- Document / Article / Note；
- Working Copy；
- immutable Revision；
- Revision History / Restore；
- Attachment / Resource Embed；
- optimistic concurrency；
- Publish / Unpublish；
- Search projection；
- Comment basic model。

### P1

- Scheduled Publish；
- realtime collaborative editing；
- Annotation anchor re-mapping；
- advanced Merge；
- Template；
- AI Writing Suggestion；
- external publish provider。

### P2

- large-scale CRDT optimization；
- branch / proposal workflow；
- editorial review workflow；
- publication channels / multi-site；
- advanced citation graph。

---

## 31. 核心结论

Ikaros V2 的普通创作能力不应实现成“Resource 表里放一段可覆盖正文”。

稳定模型应为：

```text
Resource Identity
      ↓
Creation Document
      ├── Working Copy      ← 高频编辑 / autosave
      ├── Immutable Revision ← 正式历史
      ├── Publication        ← 对外发布的确定版本
      ├── Comment / Annotation
      └── Collaboration Contract
```

其中：

- Working Copy 可以变化；
- Revision 只能新增；
- Restore 产生新 Revision；
- Publication 绑定确定 Revision；
- realtime collaboration 不能绕过 Revision 与权限边界；
- Attachment、Share、AI、Search、Automation 都通过已有平台能力组合，而不复制第二套基础设施。
