# Ikaros V2 Game / Digital Asset Archive 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Game / Digital Asset Archive 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md`、`Offline-Cache-Device-Synchronization-Subsystem-Design.md`、`Platform-Integration-Automation-Design.md` |

> 本文档定义 Ikaros V2 游戏与数字资料归档领域中的 Game、Edition / Version、Platform Target、Installer / Patch / MOD / Save Backup / Manual 等 Game Asset、兼容关系、Archive / Restore 与外部 Launcher / Plugin Action 的服务端边界。
>
> Ikaros V2 在该领域的默认定位是**游戏数字资产归档、组织、检索、下载与可追溯管理平台**，不是完整游戏启动器、安装器或 Mod Manager。只有在明确的客户端 / Plugin Capability 存在时，才可以提供受控本地安装或启动动作。

---

## 1. 设计目标

Game 子系统需要解决：

1. Game 逻辑身份与 Installer / ROM / Package 文件如何分离。
2. 一个游戏的不同 Edition、Version、Platform、Region 如何表达。
3. Installer、Patch、MOD、Save Backup、Manual、Screenshot 等数字资产如何分类并关联到正确版本。
4. “服务器存在安装包”为什么不能推导“当前设备已安装”。
5. 外部 Launcher / Plugin 如何提供打开 / 安装动作而不伪装成 Ikaros 原生业务状态。
6. Patch / MOD 的兼容版本如何表达，并避免任意插件绕过验证直接改用户游戏目录。
7. Save Backup 如何记录来源、游戏版本、设备和完整性。
8. Save Restore 为什么属于高影响本地覆盖动作，需要明确目标路径、备份和用户确认。
9. 大安装包 / Archive 如何通过 Storage Restore 与 Offline Download 复用平台基础能力。
10. Checksum / Signature / Provenance 如何帮助用户理解资产完整性，但不虚假声称未知来源一定安全。
11. Search 如何同时返回 Game Resource 与 Game Asset，并保持类型可解释。
12. Metadata Provider、Plugin、Automation 如何扩展而不直接写领域私有表。

核心原则：

> **Game 表达“是什么游戏”；Edition / Version 表达“哪个发行 / 版本”；Game Asset 表达“围绕该版本保存了什么数字资料”。资产存在不等于设备已安装。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Game Resource 专业模型；
- Game Edition / Version；
- Platform Target；
- Game Asset；
- Asset Category / Role；
- Version / Asset Compatibility；
- Installer / Package 元数据；
- Patch / Update；
- MOD 归档信息；
- Save Backup 元数据；
- Manual / Document / Screenshot 等资料关系；
- Checksum Verification 状态；
- Game Asset Availability；
- Archive / Restore 的游戏侧投影；
- Launcher / Plugin Action Capability 边界；
- Download / Offline 集成；
- 搜索、权限、事件、测试与可观测性。

### 2.2 本子系统不负责

- 完整游戏安装器；
- OS Registry / Steam / Epic / Console 的全局安装状态扫描；
- 默认自动修改游戏安装目录；
- 通用 MOD 依赖求解器；
- 反作弊绕过；
- DRM 绕过；
- 破解 / License Circumvention；
- Blob / Replica 的物理存储；
- 外部 Launcher 的内部数据库；
- Save 文件的游戏专属二进制解析（除非 Plugin 明确提供）。

---

## 3. 核心不变量

1. **Game Identity ≠ Package File**：Game 使用 UUIDv7 Resource Identity，不使用安装包路径 / ROM Hash 作为业务主键。
2. **资产存在 ≠ 已安装**：服务器有 Installer、Package 或 ROM 不能推断任何设备安装状态。
3. **Edition / Version 与 Platform 分离且可组合**：同版本可能面向多个 Platform，同 Platform 也存在多个 Version。
4. **Game Asset 使用 Attachment ID**：领域不保存 NAS Path / Object Key 作为永久身份。
5. **Asset Category 是稳定业务语义**：Installer、Patch、MOD、Save、Manual 不应仅靠文件名猜测后永久不可更正。
6. **Compatibility 必须可解释**：MOD / Patch 兼容关系需要目标 Game / Version / Platform 范围，不能只有一个自由文本备注。
7. **Checksum Verified 只表示内容与预期摘要匹配**，不表示文件无恶意代码或来源可信。
8. **Plugin Action 不能伪装 Core State**：插件报告“可启动”不能被写成平台永久 `installed=true`，除非未来有正式 Device Installation 子系统。
9. **Save Restore 默认不由服务器直接执行本地覆盖**：必须通过受控 Client / Plugin Capability，并在客户端展示目标和影响。
10. **Archive Restore ≠ Download**：冷存储恢复先让 Server Asset 可读取，然后才进入客户端 Download。
11. **删除 Asset 不自动删除 Game / Version**，删除 Version 也不能未经检查永久删除共享 Attachment。
12. **Plugin / Automation 不直接修改游戏目录或领域表**：通过明确 Command / Capability。
13. **Offline Download ≠ Cache**：预览缓存不是用户长期保存的 Installer / MOD / Save 副本。

---

## 4. 领域模型总览

```text
Game Resource
   ├── Game Edition / Version
   │     ├── Platform Target
   │     ├── Installer Asset
   │     ├── Patch Asset
   │     ├── MOD Asset
   │     ├── Manual Asset / Document
   │     └── Related Media
   ├── Save Backup(s)
   └── External Launcher / Plugin Capabilities

Game Asset
   ↓
Attachment
   ↓
Blob / Storage
```

---

## 5. Game Resource

Game 使用 Core Resource Type `game`。

通用能力：

- title / alias；
- summary；
- tag / genre；
- favorite；
- collection；
- external identity；
- lifecycle；
- relation；
- share。

Game 专业元数据可以包括：

- developer references；
- publisher references；
- original release date；
- supported platform summary；
- default / preferred edition；
- metadata provenance。

Developer / Publisher 如果需要可搜索稳定身份，后续可以复用 Person / Organization Resource，而不是仅使用字符串。

---

## 6. Platform Target

Platform Target 使用稳定标识，例如：

- `windows-x64`；
- `linux-x64`；
- `macos-arm64`；
- `switch`；
- `ps5`；
- `android`；
- plugin namespaced platform。

需要区分：

- Platform Family；
- Architecture；
- OS / Console Generation；
- optional compatibility layer。

Platform 标识不能由 UI 显示字符串承担业务语义。

---

## 7. Edition

Edition 表达发行语义，例如：

- Standard；
- Deluxe；
- GOTY；
- Collector；
- Remastered；
- Complete Edition。

至少包含：

- UUIDv7；
- game id；
- name；
- release date；
- region / language；
- edition metadata；
- platform scope；
- provenance。

Edition 不等于 Patch Version。

---

## 8. Game Version

Version 表达软件版本 / Build 语义，例如：

- 1.0.0；
- 1.2.3；
- Build 20260831；
- Provider-specific revision。

字段：

- id；
- game / edition；
- version label；
- normalized comparable version（如果有可靠规则）；
- build id；
- release date；
- platform scope；
- parent / supersedes relation；
- preferred flag；
- provenance。

### 8.1 Version 不强制 SemVer

游戏版本可能不是 SemVer。

系统必须保留：

- raw display version；
- optional normalized ordering；
- provider-specific comparator。

禁止强行解析失败后把版本号错误排序。

---

## 9. Game Asset

Game Asset 是具有游戏领域角色的数字资料引用。

至少包含：

- UUIDv7；
- game id；
- optional edition / version；
- platform scope；
- attachment id；
- category；
- display name；
- asset version；
- source / author；
- size projection；
- checksum status；
- availability；
- provenance；
- created_at / updated_at。

---

## 10. Asset Category

核心分类：

- INSTALLER；
- PACKAGE；
- PATCH_UPDATE；
- MOD；
- SAVE_BACKUP；
- MANUAL_DOCUMENT；
- SCREENSHOT_MEDIA；
- CONFIG_BACKUP；
- ARCHIVE_OTHER。

插件扩展使用命名空间。

Category 影响：

- UI；
- Permission；
- Compatibility；
- Restore / Open With；
- Search。

---

## 11. Installer / Package

Installer / Package 可以保存：

- target platform；
- architecture；
- game version；
- installer type；
- expected checksum；
- source / provider；
- language / region；
- required companion assets；
- notes。

### 11.1 安装状态边界

Ikaros Core 默认只能说明：

```text
Installer Asset Available
```

不能说明：

```text
Game Installed On This Device
```

除非未来正式引入 Device Installation Capability，并由可信客户端 / Plugin 实时报告。

---

## 12. Patch / Update

Patch 至少表达：

- source version range；
- target version；
- platform；
- attachment；
- patch type：incremental / full；
- prerequisites；
- checksum；
- provenance。

系统可以显示 Patch Chain，但 V2 初期不要求自动执行升级。

---

## 13. Compatibility Rule

MOD / Patch / Save 可以声明：

```text
Compatibility
├── game_id
├── edition_scope
├── version_range / explicit versions
├── platform_scope
├── dependency references
├── incompatible references
├── status
└── provenance
```

Version Range 只有在对应版本体系可比较时才使用。

否则保存 explicit compatible version references。

---

## 14. MOD

MOD 元数据：

- name；
- author / source；
- version；
- target Game；
- compatibility；
- dependency hints；
- description；
- attachment；
- checksum；
- installation instruction / document link；
- provenance。

### 14.1 默认不自动安装

Core 行为：

- Archive；
- Search；
- Download；
- Share；
- Compatibility Display。

自动安装必须由 Plugin / Client Capability 提供，并明确显示：

- 执行主体；
- target path；
- planned file operations；
- overwrite impact；
- rollback / backup capability；
- permission context。

---

## 15. Save Backup

Save Backup 是一类高价值用户资产。

至少记录：

- asset id；
- game id；
- game version；
- platform；
- created_at / captured_at；
- imported_at；
- source device；
- save slot / profile hint；
- attachment；
- size；
- checksum；
- notes；
- provenance。

### 15.1 敏感性

Save 可能包含：

- 用户名；
- 游戏账号标识；
- 游戏内聊天 /世界状态；
- 个人游玩数据。

默认至少按 Private / Sensitive 内容处理，不应无授权进入公共 Search / AI。

---

## 16. Save Restore

恢复本地 Save 是高影响 Device Action。

服务端只提供：

- asset descriptor；
- checksum；
- compatibility；
- authorized download；
- optional restore plan metadata。

Client / Plugin 执行前必须：

1. 显示目标路径；
2. 检查当前本地 Save；
3. 提供备份当前 Save 的选项；
4. 显示覆盖内容；
5. 用户明确确认；
6. 执行后验证结果；
7. 记录最小操作结果。

Core Server 不应通过一个普通 HTTP 请求远程任意写用户文件系统路径。

---

## 17. Manual / Document

Manual 可以是：

- PDF / README Attachment；
- 普通 Ikaros Document Resource；
- External Link（受安全策略）。

Game Asset 通过稳定 ID 引用。

Document 本体继续由 Content Creation / Resource 领域拥有。

---

## 18. Screenshot / Media

Screenshot、Trailer、Artwork 可以关联：

- Photo Resource；
- Media Resource；
- Attachment。

应优先引用已有专业 Resource，而不是将所有图像 /视频重新塞入 Game Asset JSON。

---

## 19. Checksum

Checksum / Digest 至少保存：

- algorithm；
- value；
- source；
- expected / computed；
- verified_at；
- verifier version。

状态：

- UNKNOWN；
- MATCHED；
- MISMATCH；
- NOT_APPLICABLE。

### 19.1 Checksum 不是 Malware Scan

`MATCHED` 只说明：

> 当前内容与指定摘要一致。

不表示：

- 文件可信；
- 文件安全；
- 没有恶意代码；
- 具有合法 License。

UI 不应将 Checksum Verified 显示成“安全认证”。

---

## 20. Signature / Trust Extension

未来可以支持：

- Publisher Signature；
- Package Signature；
- trusted source policy；
- malware scan result（如果明确集成）。

这些是独立 Evidence，不与 Checksum 混为一个布尔值。

---

## 21. Availability

Game Asset Availability 可以显示：

- AVAILABLE；
- REMOTE；
- ARCHIVED；
- RESTORING；
- MISSING；
- CORRUPTED；
- PROCESSING。

客户端 Download State 是另一维：

- NOT_DOWNLOADED；
- DOWNLOADING；
- DOWNLOADED；
- NEEDS_REPAIR。

不能把 `RESTORING` 显示为“下载 0%”。

---

## 22. Archive Restore

```text
User requests Download archived Installer
       ↓
Game Asset availability = ARCHIVED
       ↓
Storage Request Restore
       ↓
Background Task
       ↓
Game Asset = RESTORING
       ↓
Storage restored
       ↓
Game Asset = AVAILABLE
       ↓
Client starts Download
```

Restore 与 Download 是两个不同阶段。

---

## 23. Offline Download

Installer / MOD / Save / Manual 使用统一 Download Manifest。

大型文件支持：

- Range Resume；
- pause / resume；
- integrity verification；
- repair；
- network policy；
- local free space checks。

Cache 只用于 Preview / temporary open，不标记 Downloaded。

---

## 24. External Launcher / Plugin Capability

Plugin 可以声明：

- Detect Local Installation；
- Launch Game；
- Open Game Folder；
- Install MOD；
- Restore Save；
- Sync Save；
- Resolve Store Link。

每项 Capability 必须：

- 明确 Provider / Plugin 名称；
- 说明是否本地执行；
- 权限；
- 参数 Schema；
- side effect level；
- confirmation requirement。

UI 应显示：

> “由 xxx 插件执行”

而不是伪装为 Ikaros 核心内置能力。

---

## 25. Device Installation Projection（未来）

如果未来要展示“已安装”：

必须建立正式模型，例如：

```text
DeviceInstallation
├── device_id
├── game_id
├── edition / version
├── detected_by capability
├── install_path_safe_reference
├── detected_at
├── confidence
└── status
```

它是 Device Projection，有 TTL / Refresh，不是 Game 永久公共元数据。

V2 初期不需要实现。

---

## 26. Search

Game Search 可以索引：

- Game title / alias；
- platform；
- edition；
- version；
- asset display name；
- asset category；
- tag；
- external identity；
- MOD name / author（按权限）。

Result 必须区分：

- Game Resource；
- Game Asset；
- Document / Media / Photo related resource。

不能把 Installer Attachment 当成 Game Resource 返回而不标类型。

---

## 27. Metadata Provenance

来源可以包括：

- USER；
- FILE_SCAN；
- PROVIDER；
- PLUGIN；
- PACKAGE_METADATA；
- EMBEDDED_MANIFEST；
- SYSTEM；
- AI_SUGGESTION。

用户手工修正 Version / Platform / Asset Category 后，重新扫描不应无条件覆盖。

---

## 28. Command 契约

典型：

- CreateGame
- CreateGameEdition
- CreateGameVersion
- AddPlatformTarget
- AddGameAsset
- ChangeGameAssetCategory
- LinkAssetToVersion
- AddCompatibilityRule
- AddModMetadata
- AddSaveBackup
- RequestAssetRestore
- VerifyGameAssetChecksum
- SetPreferredGameVersion
- CreateGameDownloadManifest
- InvokeGamePluginAction（通过 Plugin Runtime）

---

## 29. Query / Capability

建议：

- GetGame
- ListGameEditions
- ListGameVersions
- ListGameAssets
- GetGameAsset
- ResolveGameAssetAvailability
- GetCompatibility
- ListMods
- ListSaveBackups
- ResolveDownloadAsset
- ListAvailableLauncherCapabilities
- GetDeviceInstallationProjection（未来）

---

## 30. Integration Event

建议：

- `game.created`
- `game.version.created`
- `game.asset.added`
- `game.asset.updated`
- `game.asset.checksum.verified`
- `game.asset.availability.changed`
- `game.save-backup.added`
- `game.mod.added`
- `game.plugin-action.completed`（仅重要 Side Effect）

Event Payload 不包含完整本地路径 / Credential。

---

## 31. 数据库关键约束

1. Game / Edition / Version / Asset 使用 UUIDv7。
2. 时间点用 `timestamptz`。
3. Asset 必须引用有效 Attachment。
4. Version / Platform Compatibility 结构化保存。
5. Checksum Algorithm + Value 语义明确。
6. Save Backup Source Device 使用稳定 Device ID（可空）。
7. Plugin Capability 不直接成为 Core Schema 任意动态列。
8. Game 不跨域修改 Blob Placement。
9. Device Installation 若未来实现，有过期 / last detected 语义。

---

## 32. 权限与安全

至少区分：

- game metadata read；
- asset list read；
- asset download；
- save backup read；
- save backup manage；
- manage version；
- manage MOD；
- request restore；
- invoke local plugin action；
- share。

规则：

- Save Backup 可以比普通 Manual 更敏感；
- Plugin Local Action 使用额外权限 /确认；
- Download 不暴露 Storage Secret；
- 本地目标路径不能由远程不可信输入任意指定；
- 防 Path Traversal / Archive Extraction Traversal；
- Plugin 执行受 Plugin Permission / Sandbox Policy 约束。

---

## 33. Archive Extraction 安全

如果 Client / Plugin 未来支持解压 Installer / MOD：

必须防止：

- `../` Path Traversal；
- absolute path overwrite；
- symlink escape；
- special device files；
- zip bomb / decompression bomb；
- unexpected executable auto-run。

默认 Download / Archive 不等于自动执行。

---

## 34. 与 Ingestion 的关系

Ingestion 可以发现：

- filename / folder；
- package metadata；
- version hint；
- platform hint；
- checksum；
- archive manifest；
- Save / MOD candidate。

Import Plan 决定 Game / Version / Asset Match，然后调用领域 Command。

Scanner 不直接设置 `installed=true`。

---

## 35. 与 Storage 的关系

Storage 拥有：

- Blob；
- Replica；
- Archive；
- Restore；
- Integrity；
- GC。

Game 拥有：

- Asset Category；
- Version / Compatibility；
- user-facing availability projection；
- Checksum expectation / verification semantics。

---

## 36. 与 Offline 的关系

Client Download：

- 保存 Manifest；
- Range Resume；
- Local Integrity；
- Downloaded State。

不计入 Server Blob Replica。

Save Restore 的 Pending / Result 可以进入 Device Operation / Audit，但不是普通数据 Sync Mutation 的无提示自动执行项。

---

## 37. 与 Automation / Plugin 的关系

Automation 可以：

- 新 Version 出现时通知；
- 新 MOD / Patch 导入后创建 Task；
- Archive Asset 恢复完成后通知；
- 定期备份 Save（需要明确 Client / Plugin Capability）。

Automation 不获得：

- 任意本地文件写权限；
- 无限制执行程序权限；
- 默认管理员权限。

---

## 38. Analytics

可统计：

- Game count；
- Asset category count；
- archived bytes；
- restore count；
- download count；
- checksum mismatch；
- MOD / Save Backup coverage；
- Plugin action success rate。

不默认采集：

- 用户完整游戏运行时长；
- 本机安装列表；

除非未来明确设计相关功能与隐私政策。

---

## 39. 可观测性

至少：

- missing / corrupted Game Asset；
- restore backlog / failure；
- checksum mismatch；
- unsupported version parse；
- compatibility conflict；
- Plugin Action failure；
- download integrity failure；
- orphaned Asset references；
- Save Backup count / storage usage（受权限）。

---

## 40. 测试与验收基线

实现至少覆盖：

1. Game 不以 Installer 路径为身份。
2. 有 Installer Asset 不会自动显示“已安装”。
3. Edition 与 software Version 可独立表达。
4. 非 SemVer Version 不会因为解析失败错误排序。
5. Asset Category 可由用户修正且 Scanner 不静默覆盖。
6. MOD Compatibility 对 Version / Platform 可解释。
7. Checksum Match 不被 UI 宣称为 Malware Safe。
8. Archive Asset 恢复与 Client Download 状态分离。
9. Restoring 不显示为 Download 0%。
10. Save Backup 具有更严格隐私权限。
11. Save Restore 在本地覆盖前显示目标与备份选项。
12. Plugin Install / Launch 明确标识由 Plugin 执行。
13. Plugin 不能通过远程路径参数执行 Path Traversal。
14. Download Cache 不显示为显式 Download。
15. 删除 Asset 不删除 Game。
16. 删除 Version 不错误 GC 仍被其他对象引用的 Blob。
17. Search Result 区分 Game 与 Asset。
18. Scanner 不直接修改私有 Game Schema。
19. 无权限用户不能通过已知 Attachment ID 下载 Save / Installer。
20. 大文件 Download 支持 Resume + Integrity Repair。

---

## 41. P0 / P1 / P2

### P0

- Game Resource；
- Platform；
- Edition / Version；
- Game Asset；
- Installer / Patch / MOD / Save / Manual 分类；
- Compatibility basic；
- Checksum；
- Availability / Archive Restore；
- Download integration；
- Search。

### P1

- richer MOD dependency metadata；
- Save Backup automation；
- Plugin Launch / Open With capability；
- Device Installation Projection；
- package signature / malware evidence integration。

### P2

- trusted local installation management；
- advanced MOD manager；
- save diff / cloud conflict tooling；
- launcher federation；
- device library scan。

---

## 42. 核心结论

Game 领域应保持：

```text
Game Resource
     ↓
Edition / Version / Platform
     ↓
Game Asset
Installer / Patch / MOD / Save / Manual
     ↓
Attachment / Blob
     ↓
Archive / Download / Plugin Action
```

并长期坚持：

- 有安装包不等于已安装；
- Plugin Action 不等于 Core State；
- Checksum 一致不等于文件安全；
- Save Restore 是高影响本地动作；
- Archive Restore 与 Download 分离；
- Ikaros 默认做数字资产归档与检索，不擅自扩张成高权限游戏安装器。
