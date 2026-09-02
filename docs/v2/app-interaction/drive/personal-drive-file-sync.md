# Personal Drive：文件空间、设备备份与同步 — App 交互规格

> 本文档定义 Ikaros V2 官方 Desktop / Mobile App 中 Personal Drive 的信息架构、页面、状态、交互与设备同步体验。
>
> 上位语义以 `Product-Requirements-Document.md`、`System-Overview-Design.md`、`Personal-Drive-File-Synchronization-Subsystem-Design.md` 与 `Personal-Drive-File-Synchronization-P0-Semantics.md` 为准。本文档只把已确定的领域能力映射为客户端体验，不重新定义服务端业务事实。

---

## 1. 产品定位

Personal Drive 在 App 中表达的是：

> **用户自己的远端文件空间，以及把本机文件可靠保存到该空间、在设备之间同步的入口。**

它不是：

- Resource Library 的另一种皮肤；
- Attachment / Blob 运维界面；
- “我的下载”页面；
- 普通客户端 Cache；
- 系统实例 Backup / Restore；
- 企业部门盘或复杂协作网盘。

必须始终保持：

```text
Personal Drive
= 用户可理解的远端文件 / 文件夹 / Revision / Trash

Download
= 某个 Resource 的显式本地离线副本

Cache
= 客户端可淘汰访问加速数据

Device Backup / Sync
= 本机文件与 Personal Drive 的持续关系
```

因此：

> **Drive File ≠ Download ≠ Cache ≠ Local Path。**

---

## 2. App 信息架构

### 2.1 主导航位置

Personal Drive 放入根导航的 **“内容库”** 分组，名称统一为：

- `个人网盘`

建议顺序：

```text
内容库
├── 统一资源库
├── 个人网盘
├── Collection / 标签
├── 视频与影视
├── 漫画与小说
├── 音乐
├── 图片与相册
├── 文章与文档
└── 游戏与数字资料
```

原因：Drive 是用户的远端内容空间，不是单纯的本机设备设置。

### 2.2 “本地与设备”分组

“本地与设备”保留本机语义，并增加：

- `备份与同步`

建议结构：

```text
本地与设备
├── 备份与同步
├── 我的下载
├── 离线内容
└── 缓存与空间
```

`备份与同步` 是 Personal Drive 的设备侧入口，最终仍跳转到 Drive Sync 页面，不创建第二套同步业务模型。

### 2.3 Compact 底部导航

Personal Drive 不默认占用 5 个 Bottom Navigation 固定位置。

高频用户可以通过以下路径快速进入：

- 首页快捷卡；
- Navigation Drawer；
- 系统分享 / 文件选择器“保存到 Ikaros”；
- 最近访问；
- Deep Link。

未来如果数据证明 Drive 成为最高频能力，可通过用户级导航自定义提升入口，但不能在初始 IA 中挤掉首页、资源库、Today、AI、我的。

---

## 3. 路由与页面目录

建议逻辑路由：

```text
/drive
/drive/folder/:nodeId
/drive/file/:nodeId
/drive/recent
/drive/favorites
/drive/shared
/drive/trash
/drive/transfers
/drive/sync
/drive/sync/:bindingId
/drive/conflicts
/drive/camera-backup
/drive/storage
```

路由只表达客户端信息架构，最终 HTTP API 路径由 API 设计确定。

页面目录：

1. Drive Home / Root。
2. Folder Browser。
3. File Detail / Preview。
4. Revision History。
5. Recent / Favorites / Shared。
6. Trash。
7. Transfer Center。
8. Backup & Sync。
9. Sync Binding Detail。
10. Conflict Center。
11. Camera Backup。
12. Drive Storage / Quota。

---

## 4. Drive Home

### 4.1 App Bar

字段与操作：

- 标题 `个人网盘`。
- Search。
- View Mode：List / Grid。
- Sort。
- More。
- 主操作 `上传`。

Desktop 可额外提供 `新建文件夹` Filled Tonal Button；Mobile 收入 FAB / Create Sheet。

### 4.2 顶部上下文区域

默认显示 Breadcrumb：

```text
个人网盘 / 当前文件夹 / 子文件夹
```

Root 时不显示冗余 `/`。

可选状态信息：

- Drive 已使用 / Quota；
- 正在上传数量；
- 同步异常 Badge；
- Trash 待清理提示。

正常状态不常驻展示“同步成功”绿色提示，避免噪音。

### 4.3 快捷入口

Root 可显示最多 4 个轻量入口：

- 最近文件；
- 收藏；
- 分享给我的；
- 回收站。

如果 Camera Backup 已启用，可出现 `相机备份` 状态卡，但不替代 Photo 页面。

### 4.4 文件区

支持：

- List View；
- Grid View；
- Folder-first / Mixed Sort，由用户偏好决定；
- 多选；
- Drag & Drop（Desktop）；
- Pull to Refresh（Mobile）。

默认排序建议：Folder First + Name Asc；用户切换排序后记住设备级偏好。

---

## 5. Drive Item

### 5.1 Folder Item

显示：

- Folder Icon / 可选预览；
- 名称；
- 修改时间；
- 可选子项摘要；
- Share / Sync / Offline 状态（只有非默认状态才显示）；
- More。

### 5.2 File Item

显示：

- 类型图标或安全缩略图；
- 文件名；
- 大小；
- 修改时间；
- 当前 Revision 状态；
- 可选领域投影 Chip，例如 `照片`、`视频`、`文档`；
- 非正常状态 Chip；
- More。

### 5.3 状态 Chip

可见状态包括：

- `正在上传`；
- `等待上传`；
- `校验中`；
- `同步中`；
- `冲突`；
- `失败`；
- `仅远端`；
- `离线可用`；
- `分享中`；
- `已移入回收站`（仅 Trash 内）。

不得显示内部 Blob Placement 状态替代用户语义。

---

## 6. 创建、上传与保存到 Drive

### 6.1 Create Sheet

入口：Root / Folder 的 FAB、`+`、Desktop 主操作。

选项：

- 新建文件夹；
- 上传文件；
- 上传文件夹（平台支持时）；
- 扫描文档 / 拍照上传（Mobile 可选）；
- 从系统分享内容保存到 Drive。

### 6.2 上传目标

上传前必须明确目标 Folder。

如果从某个 Folder 内触发，默认使用当前 Folder；允许用户修改目标位置。

### 6.3 同名冲突

发现同名 Active Node 时，不允许客户端自行覆盖。

交互必须根据服务端可用策略展示：

- 创建新 Revision；
- 保留两者 / 重命名；
- 取消。

`创建新 Revision` 必须明确说明：

> 文件身份保持不变，旧版本仍保留在版本历史中。

### 6.4 上传完成语义

必须区分：

```text
Bytes Uploaded
        ↓
Server Integrity Verified
        ↓
File Revision Committed
        ↓
Drive File Available
        ↓
Optional Domain Projection
```

UI 只有在 `File Revision Committed` 后才显示为 Drive 文件保存完成。

Photo / Media / Document 投影失败时显示次级状态，例如：

- `文件已保存 · 照片处理中`
- `文件已保存 · 媒体识别失败，可重试`

不得把领域投影失败显示为“上传失败”。

---

## 7. Transfer Center

### 7.1 页面结构

Tabs：

- 上传；
- 下载；
- 后台备份；
- 失败。

注意：Drive 的“下载”表示临时保存/导出或创建本地文件，不自动等于 Resource 的 `我的下载` 业务对象。

### 7.2 Transfer Item

显示：

- 文件名；
- 来源 / 目标摘要；
- 已传输 / 总大小；
- 当前速度；
- Progress；
- Status；
- Retry Count；
- Pause / Resume（支持时）；
- Cancel。

Status：

```text
Queued
Preparing
Transferring
Verifying
Committing
Succeeded
Failed
Paused
Cancelled
```

### 7.3 ETA

只有传输速率稳定且服务端 / 客户端能够给出可靠估算时显示 ETA。

不可靠时显示：

- `估算中`
- 当前速度

禁止显示不断跳动的假精确秒数。

---

## 8. File Detail

### 8.1 Header

展示：

- 文件名；
- 文件类型；
- 当前 Revision；
- 大小；
- 修改时间；
- Owner；
- 可见 / Share 摘要；
- 当前状态。

操作：

- 打开 / 预览；
- 下载 / 保存到设备；
- 分享；
- 收藏；
- 重命名；
- 移动；
- 查看版本；
- More。

### 8.2 Tabs / Sections

- 概览；
- 版本；
- 分享；
- 活动。

如存在专业领域投影，可显示：

- `在照片中打开`
- `在媒体库中打开`
- `在文档中打开`

但不复制展示另一套专业编辑器。

---

## 9. Revision History

列表显示：

- Revision Number / 短 ID；
- 创建时间；
- 大小；
- 创建来源：User Upload / Device Backup / Sync / Restore；
- 创建设备（允许展示时）；
- Current Chip；
- Integrity State；
- More。

操作：

- Preview；
- Download；
- Restore as New Revision；
- Compare Metadata（适用时）。

恢复旧 Revision 必须表达为：

> **创建一个新的 Current Revision，历史不被重写。**

不提供“让数据库指针直接回退并删除后续版本”的 UI 文案。

---

## 10. Folder 操作

支持：

- 新建；
- Rename；
- Move；
- Copy；
- Share；
- Favorite；
- 选择为 Sync Target；
- Move to Trash。

Move / Rename 成功后保持 Node Identity；客户端需要更新 Breadcrumb 和本地映射，不把操作表现为删除后重新上传。

批量操作：

- Move；
- Copy；
- Download / Export；
- Share（能力允许时）；
- Move to Trash。

永久删除不得出现在普通 Active Folder 的快捷菜单中。

---

## 11. Trash

### 11.1 页面

显示：

- 原名称；
- 原路径；
- 类型；
- 大小；
- 删除时间；
- 删除来源；
- 保留 / 计划清理信息（如果服务端提供）；
- Actions。

### 11.2 Actions

- Restore；
- Restore to…（原路径冲突时）；
- Delete Permanently。

### 11.3 永久删除

必须使用 Error 语义，并显示：

- 将永久删除哪些 Drive Node / Revision；
- 是否仍存在 Share；
- 物理 Blob 是否会立即回收：不得承诺；
- Blob GC 由服务端引用与保留规则决定。

重要提示：

> `永久删除 Drive 文件` 不等于 `立即删除底层 Blob`。

批量永久删除可以要求输入 `DELETE`；高风险策略需要 Step-up Verification 时进入统一安全流程。

---

## 12. Backup & Sync 首页

入口：

- `本地与设备 / 备份与同步`；
- Drive App Bar More；
- 当前 Device 设置；
- 同步异常 Banner。

页面分区：

1. 当前设备；
2. Sync Bindings；
3. Camera Backup；
4. 最近同步；
5. 冲突与失败；
6. 网络 / 电池策略。

当前设备卡：

- Device Name；
- Last Seen；
- App Version；
- 当前 Sync State；
- Pending Transfer；
- Conflict Count。

---

## 13. 创建 Sync Binding

使用 Stepper / 多步骤 Sheet。

### Step 1：选择本地来源

Desktop：

- Local Folder Picker。

Mobile：

- 平台允许访问的 Folder；
- Camera Roll / Album 进入 Camera Backup 专用流程。

本地路径只显示在当前设备，不应作为跨设备业务 ID。

### Step 2：选择 Remote Folder

使用 Drive Folder Picker。

必须使用 Node ID 作为内部选择结果，不将展示 Path 当作稳定远端身份。

### Step 3：选择模式

至少：

- `备份到 Ikaros`（Backup / Upload-only）；
- `双向同步`（能力可用时）；
- `仅下载到本机`（能力可用时）。

默认推荐 `备份到 Ikaros`，因为它对本地原始数据更保守。

### Step 4：过滤规则

可配置：

- Include / Exclude Folder；
- 文件类型；
- 最大文件大小；
- Hidden File；
- 临时文件规则。

不得在客户端偷偷增加服务端详细设计未定义的过滤语义；具体能力根据 Capability Discovery 显示。

### Step 5：危险策略

双向同步涉及删除传播时必须单独确认。

至少明确：

- 远端删除是否删除本地；
- 本地删除是否进入远端 Trash；
- 冲突默认策略；
- 是否允许蜂窝网络。

Backup 模式默认：

> **远端删除不得删除本地原始文件。**

### Step 6：Review

显示：

```text
Local Root
Remote Folder
Mode
Filters
Delete Policy
Network Policy
Initial Scan Scope
```

用户确认后创建 Binding，并启动初始扫描 / Background Task。

---

## 14. Sync Binding Detail

### 14.1 Header

显示：

- Binding Name；
- Mode；
- Enabled / Paused；
- Local Root（仅当前设备）；
- Remote Folder；
- Last Successful Sync；
- Current Cursor / Generation 的用户友好摘要；
- Pending Count；
- Conflict Count。

用户界面默认不展示内部 Cursor Token 原值，只显示诊断摘要；技术详情可在 Debug / Support 展开。

### 14.2 Sections

- Overview；
- Rules；
- Activity；
- Failed Items；
- Conflicts。

### 14.3 Actions

- Pause / Resume；
- Sync Now；
- Edit Rules；
- Full Rescan / Reconcile（有能力时）；
- Disconnect Binding。

`Disconnect` 必须明确：

- 是否只解除同步关系；
- Local Files 不删除；
- Remote Drive Files 默认不删除；
- Pending Mutation 如何处理。

不使用含糊的 `删除同步` 文案。

---

## 15. Conflict Center

Drive 文件冲突进入 Drive Resolver，不交给 Offline Runtime 自己决定。

列表显示：

- File Name；
- Remote Folder；
- Local Device；
- 冲突类型；
- Local Modified At；
- Remote Modified At；
- Local / Remote Size；
- Status。

Resolver 提供：

- 保留远端作为 Current，并保留本地 Conflict Copy；
- 上传本地为新 Revision，并保留远端历史；
- 保留两份并重命名；
- 用户明确放弃某一副本（高影响确认）。

原则：

> **默认 Resolver 不能通过“最后写入获胜”静默丢弃任一端内容。**

当服务端已经自动创建 Conflict Copy 时，UI 应明确显示两份文件的来源和后续处理选项。

---

## 16. Camera Backup

### 16.1 入口

- Backup & Sync；
- Photos 页面设置；
- 首次授予照片权限后的可选引导。

不得在首次启动 App 时强制要求全部照片权限。

### 16.2 Setup

配置：

- Source：Camera Roll / 指定 Album；
- Target Drive Folder；
- Photos / Videos；
- Wi‑Fi Only；
- Allow Cellular；
- Background Upload；
- Charging Only（平台支持时）；
- 可选领域投影到 Photos。

### 16.3 Status

必须分别显示：

- `待备份`；
- `上传中`；
- `文件已备份`；
- `照片处理中`；
- `照片已入库`；
- `备份失败`；
- `照片投影失败`。

核心规则：

> **Camera Backup Success ≠ Photo Projection Success。**

当照片投影失败时，用户仍应能在 Drive 中找到已安全保存的原始文件。

### 16.4 权限变化

照片权限被系统撤销或变为 Limited 时：

- 不把旧备份标记为失败；
- 显示当前访问范围；
- 提供 `管理系统权限`；
- 已备份远端文件不受影响。

---

## 17. Drive Search

Drive 内搜索与全局 Search 的展示目标不同：

- Drive Search 以 File / Folder 为第一对象；
- Global Search 可以同时返回 Resource、Document、Drive File 等结果。

Drive 搜索结果显示：

- 名称；
- 类型；
- Parent Folder / Path Projection；
- 修改时间；
- Size；
- Share / Conflict / Sync 状态。

如果索引暂时落后，文件详情和文件树仍以 Drive 领域状态为真相。

---

## 18. Share

Drive File / Folder 的 Share 入口复用统一 Sharing 子系统。

App 只提供：

- 创建 Share；
- 查看当前 Share；
- Copy Link；
- 修改有效期 / 下载权限（能力允许时）；
- Revoke。

必须明确：

> Share 不复制文件，不改变 Owner，也不产生第二份 Blob。

如果用户没有 Share Capability，不显示 Share 快捷操作。

---

## 19. Drive Storage / Quota

页面显示用户能理解的逻辑空间：

- Logical Used；
- Quota；
- File Count；
- Trash Logical Size；
- Revision Logical Size（服务端提供时）；
- Largest Files；
- Backup Sources。

不得把物理 Blob 去重后的实际磁盘占用直接等同于用户逻辑 Quota。

如服务端提供去重节省信息，可作为只读解释：

- `物理存储可能因去重低于逻辑文件大小总和`。

App 不暴露 Storage Provider、Bucket、Placement、GC Policy 等管理员概念。

---

## 20. Offline 与弱网络

### 20.1 Drive 文件列表

可缓存最近浏览的 Folder Listing，用于弱网占位和快速打开。

必须显示：

- `可能不是最新`；
- Last Synced At。

### 20.2 离线写操作

只有服务端与 Device Sync Contract 明确支持的操作才能进入 Pending Mutation。

例如未来可支持：

- Rename；
- Move；
- Favorite。

高风险操作，如永久删除、Share 权限修改、双向同步删除策略变更，在离线状态下默认 Disabled。

### 20.3 本地文件映射

已绑定同步目录中的 Local Item Mapping 属于当前设备状态。

用户清除 App Data / 重新安装后，不得只凭本地路径猜测远端身份；必须通过受控 Reconcile / Full Rescan 重新建立映射。

---

## 21. 安全与隐私

- File / Folder 是否可见最终由 Server ACL / Permission 决定。
- 客户端缓存文件名、缩略图和最近路径时应遵守设备安全策略。
- Share Token 不进入普通日志。
- 本地路径不得发送到 Analytics 作为普通维度。
- Camera Roll 原始本地路径、平台媒体 ID 等仅上传实现所需最小信息。
- 当前用户被撤销 Drive 权限后，本地页面必须尽快停止展示远端新内容，并按 Offline / Revocation 规则处理缓存。
- CMS / 管理员是否可以读取用户 Drive 内容必须由显式授权决定，不能因为其拥有平台管理权限就由 App 假设其具有文件内容读取权。

---

## 22. 错误与恢复

典型错误：

### Quota Exceeded

显示：

- Required；
- Available；
- Target Folder；
- `管理 Drive 空间`；
- Cancel。

不自动清理 Trash / Revision 后继续上传。

### Remote Changed

Move / Rename / Replace 提交时发现 Version Conflict：

- 刷新当前状态；
- 展示变化摘要；
- 用户重新确认；
- 不静默覆盖。

### Storage Temporarily Unavailable

如果服务端 Drive 元数据可读但 Blob Provider 暂时不可用：

- 文件仍显示在树中；
- 状态为 `暂不可读取` / `Remote unavailable`；
- 保留 Retry；
- 不显示为文件消失。

### Sync Binding Needs Reconcile

显示明确 Banner：

- 为什么需要 Reconcile；
- 当前暂停哪些危险传播；
- `开始重新扫描`；
- `查看诊断`。

---

## 23. 响应式设计

### Compact

- Folder Browser 使用单列 List / Grid。
- Breadcrumb 横向滚动或折叠为当前 Folder + 上级按钮。
- 多选后使用 Bottom Action Bar。
- File Detail 全页进入。
- Transfer / Sync 状态使用 Card。

### Medium

- 左 Folder Tree / 右 File List 可选双栏。
- File Detail 使用右侧 Sheet。

### Expanded

推荐三栏：

```text
Folder Tree | File List / Grid | Detail Pane
```

Folder Tree 240–300dp；Detail Pane 320–420dp。

### Large

- Folder Tree 常驻；
- Data Table 支持更多列；
- Transfer Center / Conflict Resolver 可左右对比；
- Drag & Drop、Keyboard Shortcut、Context Menu 完整启用。

---

## 24. Desktop 快捷键

建议：

- `Ctrl/Cmd + U`：上传文件；
- `Ctrl/Cmd + Shift + N`：新建文件夹；
- `F2`：Rename；
- `Delete`：Move to Trash；
- `Shift + Delete`：只打开永久删除确认，不直接执行；
- `Ctrl/Cmd + F`：Drive Search；
- `Space`：Quick Preview（支持时）。

快捷键必须遵循当前 Selection、Focus 和 Permission，不能绕过 Dialog / Step-up。

---

## 25. 与其他 App 页面边界

### Resource Library

Drive 文件可以投影为 Resource，但：

- Drive Browser 关注文件树和文件历史；
- Resource Library 关注“这是什么内容”和专业消费体验。

### Photos

Camera Backup 由 Drive 保存原始文件；Photos 负责 Timeline / EXIF / Album。

Photos 中应可以跳转 `查看原始 Drive 文件`，但不复制一套上传状态模型。

### Offline / Downloads

- Drive Sync 保存的是“设备文件副本与远端 Drive 的关系”；
- 我的下载保存的是“用户主动下载的 Resource 离线副本”；
- Cache 是客户端实现数据。

三者统计和清理入口必须分开。

### Automation

用户可以从 Sync Binding 跳转关联 Automation，但 Automation 不拥有 Drive Tree / Revision 状态。

---

## 26. Capability Discovery

App 根据实例实际能力显示入口，例如：

```text
drive.enabled
drive.device_backup.available
drive.two_way_sync.available
```

规则：

- `drive.enabled = false`：隐藏 Personal Drive 主入口；
- 未提供 Two-way Sync：仍可使用普通 Drive 与 Backup；
- 未提供 Camera Backup：不显示对应入口；
- Capability 可用不代表当前用户有权限，最终仍以 Permission / ACL 为准。

---

## 27. 验收重点

实现验收至少覆盖：

1. File / Folder Path 变化不会在 UI 中被解释为新文件身份。
2. Replace File 明确创建 Revision，而不是“覆盖旧 Blob”。
3. Trash 与 Permanent Delete 清晰分离。
4. Permanent Delete 不承诺底层 Blob 立即物理删除。
5. Backup 模式不会通过远端删除默认删除本地原始文件。
6. 双向同步冲突不会静默丢弃任何一端数据。
7. Camera Backup 能区分 File Backup 与 Photo Projection 两阶段状态。
8. Drive / Download / Cache 的页面与空间统计不会混为一谈。
9. Share 不暗示文件复制或 Owner 变化。
10. 弱网、Quota、Conflict、Storage Unavailable、Permission Revoked 均有明确状态。
11. Desktop / Mobile 共享同一业务 IA，只改变布局和设备能力入口。
12. App 不暴露 Blob Placement / Storage Provider / GC Policy 等管理员实现概念。
