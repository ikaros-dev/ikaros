# Personal Drive App — CMS Console 交互规格

> Personal Drive 是 Apps 下的独立业务产品。它不并入 Storage，也不成为全局一级 Sidebar Entry。
>
> 管理 Drive 文件不等于管理 Blob；管理 Sync Binding 不等于管理 Storage Provider。

## 1. App 入口与路由

**App Entry：** `/apps/drive`

Canonical routes：

```text
/apps/drive
/apps/drive/nodes/:nodeId
/apps/drive/transfers
/apps/drive/sync
/apps/drive/sync/:bindingId
/apps/drive/conflicts
/apps/drive/trash
/apps/drive/settings
```

当前设计不要求兼容历史 `/console/drive/*` 路由。

## 2. 权限与隐私硬边界

**Platform ADMIN 不自动等于 Drive File READ。**

Drive 治理能力与文件内容读取必须分别判断。

只有运维权限、没有目标文件读取权限时，可以展示：

- Space / Owner 安全摘要；
- Logical Usage；
- Node Count；
- Sync / Conflict / Transfer 数量；
- Error Category；
- Health。

不得展示：

- 私有文件名；
- Folder Path；
- Thumbnail / Preview；
- File Content；
- 客户端 Local Path。

Deep Link 不能扩权。从 Activity、Storage、Audit、Media 等进入 Drive 后重新执行 Drive Capability + ACL / Scope 判定。

## 3. Drive Home

**Route：** `/apps/drive`

展示当前有权访问的 Drive Space 和文件浏览器。

主要区域：

- Space Selector；
- Folder Tree / Breadcrumb；
- File / Folder Table；
- Detail Side Pane；
- Logical Usage / Quota；
- Sync / Conflict Attention。

主操作：上传文件、新建文件夹。更多入口：Transfer、Sync、Trash、Settings。

## 4. File / Folder Browser

URL 导航使用 Node ID，Path 仅作为显示投影。

列表字段：名称、类型、逻辑大小、当前 Revision、更新时间、权限/分享摘要、Sync/Conflict 状态和 Domain Projection。

Folder 支持 Open、Rename、Move、Copy、Share、Favorite、Sync Target、Trash。

File 支持 Preview、Download、Upload New Revision、Rename、Move、Copy、Share、Revision History、Trash。

Rename / Move 不改变 Drive Node 身份，也不重新上传内容。

## 5. File Detail

**Route：** `/apps/drive/nodes/:nodeId`

Tabs：Overview、Revisions、Storage Summary、Share/ACL、Sync、Activity。

Storage Summary 默认只展示 Attachment / Blob Availability、Integrity 和副本摘要。Placement、Provider、GC 等技术维护通过 `/storage/maintenance` 查看。

Drive 页面不得直接删除 Placement、修改 Object Key 或触发 Blob GC。

## 6. Revision

Revision History 作为 File Detail Tab 展示。

Restore 语义：选择历史 Revision 后创建新的 Current Revision，不删除后续历史。

清理 Revision 受 Retention、Share、Backup、Pin 等引用约束；清理 Revision 不等于物理删除 Blob。

## 7. Trash

**Route：** `/apps/drive/trash`

展示原名称、原位置（有权读取时）、逻辑大小、删除时间、Retention 和引用摘要。

Restore 处理原路径冲突和父目录不存在场景。

Permanent Delete 必须明确：Drive Node / Revision 将被永久清理，但 Blob 是否最终物理删除仍由 Storage GC 决定。

## 8. Transfers

**Route：** `/apps/drive/transfers`

展示上传、下载、Device Backup、Sync 等传输状态。

长期传输同时注册到全局 `/activity`。Drive Transfers 页面是 Drive 业务视图，不建立另一套后台任务中心。

没有 `drive.file.read` 时，运维视图只显示安全摘要和技术状态，不显示文件名、路径、预览。

## 9. Sync

**Route：** `/apps/drive/sync`

展示 Binding Name、Owner、Device、Remote Folder、Mode、State、Last Sync、Pending、Conflict、Failed Item。

浏览器无法安全选择本地目录时，不伪造 Local Folder Picker；需要 Desktop Device 授权的步骤明确转移到客户端完成。

Binding Detail 使用 `/apps/drive/sync/:bindingId`。

## 10. Conflicts

**Route：** `/apps/drive/conflicts`

冲突必须保留两侧上下文，提供 Keep Local、Keep Remote、Keep Both 等明确策略。涉及丢弃一侧内容时显示高影响确认。

Preview 仍要求目标文件内容读取能力。

## 11. Settings

**Route：** `/apps/drive/settings`

包含：Quota、Drive Policy、Backup Binding / Camera Backup 等与 Drive 产品相关的配置。

Quota 使用逻辑空间，不得与 Storage 去重后的物理 Used 混为一项。

Storage Provider、Placement、Blob GC 等配置不进入 Drive Settings。

## 12. 与 Library / Storage / Activity 的关系

- Drive File 可以投影成 Document、Photo、Media 等专业 Resource，但投影不产生新的文件身份；
- Library/Resource Deep Link 到 Drive 时重新检查 Drive 权限；
- Drive File 可查看 Storage Summary，需要技术排障时进入 `/storage/maintenance`；
- 上传、同步、备份、恢复等长期工作统一进入 `/activity`；
- Drive 用户业务 Activity 可以保留在 File/Binding Detail，不占用全局 Activity 的含义。

## 13. 验收

- Drive 只作为 `/apps/drive` App 出现；
- Platform ADMIN 与 Drive File READ 严格分离；
- Path 不是稳定身份；
- Revision Restore 创建新 Revision；
- Trash Permanent Delete 不承诺立即释放全部物理空间；
- Transfer/Sync 长期工作与全局 Activity 状态一致；
- Drive 与 Storage 的配置边界清楚；
- 不再以历史独立一级“个人网盘”分组或 `/console/drive/*` 作为设计基线。
