# Personal Drive — CMS Console 交互规格

> 本文档定义 Ikaros V2 CMS Console 中 Personal Drive 的信息架构、文件空间治理、同步诊断、冲突处理、回收站、Revision、Quota 与策略交互。
>
> 上位语义以 `Product-Requirements-Document.md`、`System-Overview-Design.md`、`Personal-Drive-File-Synchronization-Subsystem-Design.md` 与 `Personal-Drive-File-Synchronization-P0-Semantics.md` 为准。Console 不得通过 UI 重新定义 Drive、Attachment / Blob 或 Device Sync 的领域边界。

---

## 1. Console 定位

Personal Drive 在 CMS Console 中是独立业务域，不并入“附件与存储”。

必须区分：

```text
Personal Drive
= Drive Space / Node / File Revision / Trash / Sync Binding / Conflict

Attachment / Blob / Storage
= 内容对象、内容身份、Placement、Provider、完整性与物理生命周期
```

因此：

> **管理 Drive 文件 ≠ 管理 Blob；管理同步关系 ≠ 管理 Storage Provider。**

Console 主要承担：

- 用户有权访问的 Drive Space 文件管理；
- Revision / Trash 管理；
- Sync Binding 配置与诊断；
- Transfer / Failed Item / Conflict 处理；
- Camera Backup 状态诊断；
- Drive Quota 与逻辑用量查看；
- 管理员级 Drive Policy / Capability 配置；
- Audit / Background Task / Storage 深链接。

Console 不应默认承担：

- 任意管理员无授权读取所有用户私有文件内容；
- 直接编辑 Blob Placement；
- 通过数据库表修改 Drive 状态；
- 把远端 Drive Path 当作稳定身份；
- 直接操作客户端本地文件系统。

---

## 2. 信息架构

### 2.1 侧栏分组

新增独立分组：

```text
个人网盘
├── 文件空间
├── 传输与同步
├── 冲突处理
├── 回收站与版本
└── 配额与策略
```

建议位于：

```text
内容与创作
个人网盘
附件与存储
效率与计划
...
```

原因：

- Drive 是面向用户的业务域；
- Attachment / Storage 是平台内容与物理存储底座；
- 两者需要频繁深链接，但不应在信息架构上混成一个菜单。

### 2.2 路由建议

```text
/console/drive
/console/drive/spaces
/console/drive/spaces/:spaceId
/console/drive/nodes/:nodeId
/console/drive/transfers
/console/drive/sync
/console/drive/sync/:bindingId
/console/drive/conflicts
/console/drive/trash
/console/drive/revisions
/console/drive/quota
/console/drive/policies
```

具体 HTTP API 路径不由本文档确定。

---

## 3. 权限与隐私边界

### 3.1 平台管理员不自动等于文件读取者

必须明确：

> **Platform ADMIN ≠ Drive File READ。**

管理员可以拥有：

- 查看 Drive 系统健康；
- 查看 Sync Binding 运行状态；
- 查看 Quota / Transfer 统计；
- 管理 Drive Policy；
- 处理系统级故障；

但是否能够查看某个用户的文件名、目录结构、预览或下载内容，必须经过独立 Permission / ACL 判断。

### 3.2 管理视图中的最小化信息

如果管理员只有运维权限、没有目标 Drive 内容读取权限，列表只允许展示必要诊断信息，例如：

- Space ID；
- Owner / User ID 或允许显示的账号摘要；
- Logical Usage；
- Node Count；
- Sync Binding Count；
- Conflict Count；
- Transfer Failure Count；
- Last Activity At；
- Health State。

不得展示：

- 私有文件名；
- Folder Path；
- Thumbnail；
- File Content；
- 本地设备路径。

### 3.3 Step-up

以下操作按安全策略可要求 Step-up Verification：

- Permanent Delete；
- 大规模 Trash Purge；
- 修改危险删除传播策略；
- 强制解除高风险 Sync Binding；
- 管理员修改他人 Drive Quota；
- 批量冲突丢弃某一端数据；
- 恢复到会产生大范围覆盖影响的 Revision 操作。

---

## 4. 文件空间首页

**路由：** `/console/drive`

### 4.1 页面标题区

- 标题：`个人网盘`。
- 副标题：`管理 Drive Space、文件树、版本、同步与回收站；底层字节仍由 Attachment / Blob Storage 管理。`
- 主操作：`上传文件`（有当前 Drive 写权限时）。
- 次操作：`新建文件夹`。
- More：传输中心、同步、回收站、配额。

### 4.2 KPI 卡片

当前用户 / 当前有权管理的 Drive Context 下显示：

- Logical Used；
- File / Folder Count；
- Active Sync Bindings；
- Pending Transfers；
- Conflict Count；
- Trash Logical Size。

管理员全局视图不得把多个用户的逻辑用量聚合后暗示为实际 Blob 磁盘占用。

### 4.3 Drive Space Selector

如果当前账号可访问多个 Drive Space：

- 使用 Select / Searchable Popover；
- 展示 Space Name、Owner、Permission Summary；
- 最近使用置顶；
- 切换后更新 Breadcrumb、列表、Quota Context。

没有他人 Drive READ 权限时，不出现在选择器中。

---

## 5. Folder Browser

### 5.1 Desktop 布局

推荐三栏：

```text
Folder Tree | File / Folder Table | Detail Side Pane
```

- Folder Tree：260–320 px；
- 主表格：自适应；
- Detail Pane：360–440 px，可关闭。

### 5.2 Breadcrumb

展示当前 Path Projection，但所有页面导航使用 Node ID。

Move / Rename 后：

- 当前 URL 保持 Node ID；
- Breadcrumb 重新获取；
- 不跳转到“新文件”详情。

### 5.3 Data Table

列：

1. Selection；
2. Type Icon / Preview；
3. Name；
4. Node Type：File / Folder；
5. Size（Folder 为逻辑摘要时需标注）；
6. Current Revision；
7. Modified At；
8. Owner / Permission Summary（适用时）；
9. Share State；
10. Sync / Conflict State；
11. Domain Projection；
12. Actions。

### 5.4 Filter

Filter Chip：

- File / Folder；
- Shared；
- Conflict；
- Sync State；
- Domain Projection；
- Recently Modified。

高级筛选：

- Size Range；
- Created / Modified Range；
- Owner；
- MIME / Extension；
- Revision Count；
- Special Folder Type。

---

## 6. 文件 / 文件夹操作

### 6.1 通用操作

Folder：

- Open；
- Rename；
- Move；
- Copy；
- Share；
- Favorite；
- Select as Sync Target；
- Move to Trash。

File：

- Preview / Open；
- Download；
- Upload New Revision；
- Rename；
- Move；
- Copy；
- Share；
- Revision History；
- Move to Trash。

### 6.2 Rename / Move

Dialog / Side Sheet 必须说明：

- `该操作不会改变 Drive Node 身份`；
- `不会重新上传文件内容`。

如果存在活跃 Sync Binding，可显示：

- `该变化将在同步协议允许时传播到绑定设备。`

### 6.3 Copy

Copy 创建新的 Drive Node 业务对象。

即使底层可复用同一个 Blob，也不得在 UI 中描述为“同一个文件 ID 的第二个路径”。

---

## 7. File Detail

**路由：** `/console/drive/nodes/:nodeId`

### 7.1 Header

展示：

- Name；
- File Type；
- Current Revision；
- Logical Size；
- Modified At；
- Owner；
- ACL / Share Summary；
- Current State。

Actions：

- Preview；
- Download；
- Upload New Revision；
- Share；
- Move；
- Rename；
- More。

### 7.2 Tabs

- Overview；
- Revisions；
- Attachment / Blob；
- Share / ACL；
- Sync；
- Activity / Audit。

### 7.3 Attachment / Blob Tab

这里只展示引用关系摘要：

```text
Drive File
→ Current File Revision
→ Attachment
→ Blob
```

显示：

- Attachment ID；
- Blob ID；
- Integrity State；
- Availability；
- Placement Count；
- `在附件与存储中打开`。

Drive 页面不得提供：

- 直接删除 Placement；
- 修改 Bucket / Object Key；
- 直接触发 Blob GC。

---

## 8. Revision History

### 8.1 Revision Table

列：

- Revision；
- Created At；
- Source；
- Logical Size；
- Attachment；
- Integrity；
- Creator / Device；
- Current；
- Actions。

Source 示例：

- Manual Upload；
- Device Backup；
- Two-way Sync；
- Conflict Resolution；
- Restore。

### 8.2 Restore

`恢复此版本` Dialog 必须明确：

> **恢复不会删除后续历史，而是基于选中版本创建一个新的 Current Revision。**

影响摘要：

- File；
- Selected Revision；
- Current Revision；
- New Revision 将被创建；
- Active Sync Bindings 可能收到新内容版本。

### 8.3 Revision 清理

如果服务端允许按策略清理旧 Revision：

- 只能从受控 Retention 操作进入；
- 必须显示被 Share / Backup / Retention Pin 等引用的不可删除原因；
- 清理 Revision 不直接等于删除 Blob。

---

## 9. Trash

**路由：** `/console/drive/trash`

### 9.1 Table

列：

- Original Name；
- Type；
- Original Parent / Path（有权读取时）；
- Logical Size；
- Deleted At；
- Deleted By；
- Retention / Purge At；
- Share / Revision Reference Summary；
- Actions。

### 9.2 Restore

原位置无冲突时：

- `恢复`。

存在同名或父目录不存在时：

- 打开 Restore Side Sheet；
- 选择新 Parent；
- 修改 Name；
- 显示冲突预览。

### 9.3 Permanent Delete

危险 Dialog：

- 明确 Node / File / Revision 范围；
- 显示仍存在的业务引用；
- 显示 `Blob 是否物理删除由 Storage GC 决定`；
- 要求输入实体名或 `DELETE`（按风险等级）；
- 需要时执行 Step-up。

不得使用：

- `立即删除底层文件`
- `释放全部物理空间`

等无法保证的文案。

---

## 10. Transfer Center

**路由：** `/console/drive/transfers`

### 10.1 KPI

- Running；
- Queued；
- Failed；
- Throughput；
- Bytes Today；
- Verification Failure。

### 10.2 Table

列：

- Transfer ID；
- Direction；
- File / Safe Summary；
- Device / Actor；
- Target Folder；
- Stage；
- Progress；
- Bytes；
- Speed；
- Retry Count；
- Started At；
- Error；
- Actions。

Stage：

```text
Queued
Preparing
Transferring
Verifying
Committing Revision
Projecting Domain
Succeeded
Failed
Cancelled
```

`Projecting Domain` 失败时必须与 `Committing Revision` 失败分开统计。

### 10.3 运维权限视图

只有 Transfer 运维权限、没有 File READ 权限时：

- File Name 显示安全摘要或 Node ID；
- Local Path 不显示；
- 允许查看 Error Category、Provider、Bytes、Stage；
- 不允许 Preview / Download。

---

## 11. Sync Bindings

**路由：** `/console/drive/sync`

### 11.1 Table

列：

- Binding Name；
- Owner；
- Device；
- Remote Folder；
- Mode；
- Enabled / Paused；
- Last Successful Sync；
- Current State；
- Pending；
- Conflict；
- Failed Item；
- Actions。

Mode：

- Backup / Upload-only；
- Two-way；
- Download-only（能力可用时）。

### 11.2 Filters

- Mode；
- Device；
- State；
- Conflict；
- Failed；
- Owner；
- Last Sync Range。

### 11.3 创建 Binding

Console 支持 Desktop / Web 能够明确选择来源的场景。

如果浏览器无法安全访问本地路径：

- 不伪造 Local Folder Picker；
- 创建流程转为 `在 Desktop App 完成绑定`；
- Console 可以创建 Remote-side Preparation / Policy，但真正本地授权由 Device 完成。

### 11.4 Binding Detail

Tabs：

- Overview；
- Rules；
- Activity；
- Failed Items；
- Conflicts；
- Diagnostics。

显示：

- Stable Binding ID；
- Mode；
- Device；
- Remote Folder Node；
- Current Generation；
- Cursor Summary；
- Last Scan；
- Last Reconcile；
- Pending Count；
- Conflict Count；
- Network / Battery Policy（设备上报允许时）。

Local Root：

- Owner 本人或有明确设备诊断权限时可以看到安全处理后的路径；
- 普通管理员默认不显示完整用户本地路径。

---

## 12. Sync Binding 危险策略

双向同步的删除传播策略必须独立显示。

Policy Card：

- Local Delete → Remote Trash；
- Remote Delete → Local Delete；
- Conflict Strategy；
- Rename / Move Propagation；
- Full Rescan Behavior。

`Remote Delete → Local Delete` 属于高风险策略：

- 默认关闭；
- 启用前解释影响；
- 明确只对 Two-way Binding 生效；
- 需要时 Step-up；
- 修改写入 Audit。

Backup Mode 必须固定显示：

> **远端删除不会自动删除设备本地原始文件。**

不能允许普通策略表单悄悄改变这一安全语义。

---

## 13. Conflict Center

**路由：** `/console/drive/conflicts`

### 13.1 Summary

- Unresolved；
- Auto-preserved Conflict Copy；
- Needs User Decision；
- Resolved Today；
- Oldest Conflict Age。

### 13.2 Table

列：

- Conflict ID；
- File / Safe Summary；
- Device；
- Remote Folder；
- Conflict Type；
- Local Version Summary；
- Remote Version Summary；
- Detected At；
- Status；
- Resolver；
- Actions。

### 13.3 Resolver

左右对比：

```text
Local Side | Remote Side
```

展示：

- Size；
- Modified At；
- Content Hash（有权限并适合显示时）；
- Base Revision；
- Device；
- Preview（有内容读取权限时）。

允许动作：

- Keep Remote + Preserve Local Conflict Copy；
- Promote Local as New Revision；
- Keep Both；
- Explicitly Discard One Side。

最后一种属于高影响动作，必须再次确认。

核心原则：

> **Console 不提供“静默 Last Write Wins”作为默认批量解决方案。**

批量处理只允许对语义完全明确且不会丢数据的情况执行，例如 `Keep Both`。

---

## 14. Camera Backup

Camera Backup 不单独创建顶级侧栏页面，作为 Sync 的专门 Tab / Filter。

### 14.1 Table

列：

- User；
- Device；
- Source Scope；
- Target Drive Folder；
- Pending；
- Backed Up；
- Failed；
- Photo Projection Pending；
- Photo Projection Failed；
- Last Run；
- State。

### 14.2 两阶段状态

必须分别展示：

```text
Camera Asset
    ↓
Drive File Backup
    ↓
Photo Domain Projection
```

因此 KPI 不得只显示一个 `成功率`。

至少区分：

- File Backup Success Rate；
- Photo Projection Success Rate。

Photo Projection 故障时，提供：

- `在 Drive 中查看已备份文件`；
- `重试照片入库`；
- `打开 Photo 子系统诊断`。

不得建议重新上传已经成功保存的原始文件。

---

## 15. Quota 与逻辑用量

**路由：** `/console/drive/quota`

### 15.1 用户视图

显示：

- Logical Used；
- Quota；
- Active File Size；
- Trash Logical Size；
- Revision Logical Size；
- File Count；
- Largest Files；
- Growth Trend。

### 15.2 管理员视图

Table：

- User / Drive Space；
- Quota；
- Logical Used；
- Usage %；
- Active Files；
- Trash；
- Revisions；
- Active Sync Binding；
- Last Activity；
- Actions。

### 15.3 Quota 修改

管理员修改 Quota Side Sheet：

- Current Quota；
- New Quota；
- Current Used；
- Over-quota 结果预览；
- Reason；
- Save。

降低到当前用量以下时必须明确：

- 不自动删除数据；
- 新上传 / 新 Revision 可能被拒绝；
- 现有文件保持可读（除非上位产品规则另有定义）。

### 15.4 逻辑与物理空间

必须显示解释：

> Drive Quota 使用逻辑文件空间语义；Blob 去重、多引用、Replica 数量与物理 Storage Used 是另一套统计。

需要查看物理占用时跳转 `附件与存储 / Storage Analytics`，不能在 Drive Quota 页面混用口径。

---

## 16. Drive Policies

**路由：** `/console/drive/policies`

仅有对应平台管理权限时显示。

可管理：

- Default User Quota；
- Trash Retention；
- Revision Retention Default；
- Max File Size；
- Upload Concurrency / Limits；
- Camera Backup Capability；
- Two-way Sync Capability；
- Dangerous Delete Propagation 是否允许由用户启用；
- Conflict Retention；
- Transfer Retry Policy 的产品级可配置项。

不得在此管理：

- S3 Credential；
- Storage Bucket；
- Placement Policy；
- Blob GC 内部策略；

这些仍属于 Attachment / Blob / Storage。

Policy 修改必须显示：

- 当前值；
- 新值；
- 影响范围；
- 是否只影响新 Binding；
- 是否要求后台迁移 / Reconcile；
- 是否需要 Restart；
- Audit Reason。

---

## 17. Search 与筛选

### 17.1 Drive Search

只在当前用户有文件读取权限的 Drive Scope 内搜索 File / Folder。

结果字段：

- Name；
- Type；
- Path Projection；
- Size；
- Modified At；
- Owner；
- Sync / Conflict / Share State。

### 17.2 运维搜索

管理员诊断可以按：

- Node ID；
- Revision ID；
- Binding ID；
- Device ID；
- Transfer ID；
- Conflict ID；
- Attachment / Blob ID；

进行精确查询。

如果无目标内容 READ 权限，只返回安全诊断摘要。

---

## 18. 与 Attachment / Storage 的深链接

Drive File Detail 提供：

- `打开 Attachment`；
- `打开 Blob`；
- `查看 Storage Availability`。

Attachment / Storage 页面可以反向显示：

- `Referenced by Drive File Revisions: N`；
- 有权限时跳转对应 Drive File。

不得提供跨域直接修改：

```text
Drive 页面修改 Placement
Storage 页面修改 Drive Current Revision
```

所有状态变更进入目标子系统公开 Command。

---

## 19. 与 Offline / Device Sync Runtime 的边界

Console 中可以展示 Device Sync Runtime 的诊断信息：

- Device；
- Cursor；
- Change Feed Lag；
- Pending Mutation；
- Full Resync / Reconcile State。

但用户最终看到的 Drive 业务状态由 Drive 子系统解释：

- File Revision；
- Conflict；
- Remote Node Mapping；
- Delete / Restore；
- Sync Mode。

因此：

> **Sync Runtime 故障诊断可以共用平台页，Drive Conflict 仍必须回到 Personal Drive Resolver。**

---

## 20. 与 Photo / Media / Document 的边界

Drive File Detail 如存在领域投影，显示 Projection Card：

- Target Domain；
- Target Resource；
- Projection State；
- Last Error；
- Retry；
- Open Target。

Projection Failure：

- 不回滚已提交 File Revision；
- 不建议重新上传原始文件；
- 通过 Background Task / Domain Command 重试。

Camera Backup 特别遵守：

> File Backup Success 与 Photo Projection Success 分开观测。

---

## 21. Audit 与 Activity

以下操作应可进入 Audit：

- Permanent Delete；
- Revision Restore；
- Quota Change；
- Sync Binding Dangerous Policy Change；
- Force Reconcile；
- Explicit Conflict Discard；
- 管理员访问受保护 Drive 内容（如果安全策略要求）；
- Share / ACL 高风险变更。

普通文件浏览、打开等用户行为属于 Activity / Access Log 的语义，不应全部塞入高价值 Security Audit。

详情页 Activity Tab 可以聚合：

- Created；
- Uploaded Revision；
- Moved；
- Renamed；
- Shared；
- Trashed；
- Restored；
- Conflict Detected / Resolved。

---

## 22. Background Task

以下操作转后台任务：

- Folder 大规模上传；
- Initial Sync Scan；
- Reconcile / Full Rescan；
- Camera Backup 批量传输；
- 批量 Revision Retention Cleanup；
- 批量 Trash Purge；
- Conflict Repair；
- 大范围 Move / Copy（实现需要时）。

任务中心必须能够回跳：

```text
Task
→ Drive Space / Folder / Binding / Conflict
```

任务 Failed 不自动等于业务对象失败；页面根据任务类型解释可重试范围。

---

## 23. 错误与故障降级

### Storage Provider Unavailable

- Drive Tree 仍可读取时继续展示文件元数据；
- File Availability 显示异常；
- Preview / Download Disabled 或 Retry；
- 不把文件显示为“不存在”。

### Search Index Unavailable

- Folder Browser / Node Detail 继续工作；
- Drive Search 降级并显示状态；
- 不禁止 Upload / Rename / Move。

### Device Offline

- Binding 显示 `Device Offline`；
- 不把它标为永久失败；
- 禁止假装能够立即执行本地删除。

### Binding Needs Reconcile

- 显示 Banner；
- 暂停可能破坏数据的传播；
- 提供 `Start Reconcile`；
- 展示原因与最后安全 Cursor / Generation 摘要。

### Quota Exceeded

- 现有文件仍显示；
- 新上传 / Revision 操作给出准确 Required / Available；
- 不自动清理 Trash / Revision 后重试。

---

## 24. 响应式

### ≥ 1280 px

- Folder Tree + Table + Detail 三栏；
- Conflict Resolver 双栏；
- Sync Detail 支持主内容 + Diagnostics 右栏。

### 960–1279 px

- Folder Tree 可折叠；
- Detail 使用 Side Sheet。

### 600–959 px

- Modal Folder Tree；
- Table 水平滚动；
- Conflict Resolver 改上下对比。

### < 600 px

Console 仍可完成基础管理：

- 文件列表；
- File Detail；
- Sync 状态；
- Trash Restore；

复杂批量治理、冲突对比、策略管理优先提示使用更宽屏幕，但不能因为移动宽度直接绕过权限或危险确认。

---

## 25. Empty / Loading / Error

### Drive Empty

- 图标；
- `个人网盘还是空的`；
- 有写权限：`上传文件` / `新建文件夹`；
- 无写权限：只显示说明。

### No Sync Binding

- `还没有设备备份或同步关系`；
- `在 Desktop / Mobile App 中设置`；
- 如果当前 Web 环境支持绑定，再显示 `新建同步`。

### No Conflict

- `没有待处理冲突`；
- 显示最近一次冲突解决时间（有数据时）。

### Loading

Folder Tree 与 Table 分别 Skeleton，避免一个区域刷新导致整个页面闪白。

---

## 26. Capability Discovery

页面根据实例能力显示：

```text
drive.enabled
drive.device_backup.available
drive.two_way_sync.available
```

规则：

- Drive 未启用：整个侧栏分组隐藏；
- Device Backup 不可用：隐藏 Sync Binding 创建入口，但普通 Drive 仍可使用；
- Two-way Sync 不可用：只展示 Backup / 允许的模式；
- Capability 只表示实例可用性，不替代当前用户 Permission。

---

## 27. 验收重点

实现 Review 至少检查：

1. Personal Drive 在 IA 上与 Attachment / Storage 独立。
2. Platform Admin 不被前端默认赋予任意用户 Drive File READ。
3. Folder Path 只用于展示，页面身份与导航使用 Node ID。
4. Move / Rename 不表现为删除 + 新建文件。
5. Revision Restore 创建新 Revision，不改写历史。
6. Trash / Permanent Delete / Blob GC 三层语义清晰。
7. Transfer 的 Upload Success、Integrity Verify、Revision Commit、Domain Projection 状态可区分。
8. Backup Mode 不允许远端删除默认破坏本地原始文件。
9. Drive Conflict Resolver 不默认提供静默 Last Write Wins。
10. Camera Backup 能区分文件备份与 Photo Projection。
11. Quota 使用逻辑空间，不与 Blob 去重后的物理 Storage Used 混用。
12. Storage / Search / Device 故障时 Drive 元数据仍能按能力降级展示。
13. 所有永久删除、危险同步策略和显式丢弃冲突副本都有准确影响说明与必要安全确认。
