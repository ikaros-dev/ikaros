# Games：游戏与数字资料

## 1. 页面目录

- 游戏库。
- 游戏详情。
- 版本 / 平台列表。
- 数字资料与附件。
- MOD / Patch / Save 分类页。
- 下载与归档状态。

Ikaros V2 的定位是游戏数字资产归档与检索平台，不强制承担完整游戏启动器职责。

---

## 2. 游戏库

### 2.1 App Bar

- 标题 `游戏`。
- Search。
- Filter。
- Sort。
- View Toggle。

### 2.2 Game Card

- Cover / Key Art。
- Title。
- Platform Chips，例如 Windows / Switch / PS。
- Version 摘要。
- Favorite。
- Availability。
- Attachment Count。
- Archive 状态。

Tap 进入详情。

---

## 3. Filter

- Platform。
- Genre / Tag。
- Collection。
- Installed Package Available（只表示附件存在，不表示本机已安装）。
- Has Save Backup。
- Has MOD。
- Availability。
- Lifecycle。

“已安装”仅当未来明确存在本地集成并能可靠判断时显示，当前不从安装包存在推断。

---

## 4. 游戏详情 Header

Compact 纵向，Desktop 左图右信息。

字段：

- Cover。
- Display Title。
- Original / Alternative Title。
- Developer / Publisher。
- Release Date。
- Platform。
- Version / Edition。
- Tags。
- Favorite。
- `查看资料` 主按钮。
- Download。
- Share。
- More。

若配置了外部 Launcher / Plugin，可在 More 显示 `使用 xxx 打开`，不能把插件动作伪装为 Ikaros 原生安装状态。

---

## 5. 详情 Tabs

- 概览。
- 资料。
- 版本。
- 关联。
- 信息。

### 概览

- Description。
- Current Version Summary。
- Recent Assets。
- Related Resource。
- Notes / Documents Preview。

---

## 6. 数字资料分类

资料页顶部 Category Chips：

- 安装包。
- Patch / Update。
- MOD。
- Save Backup。
- Manual / Document。
- Screenshot / Media。
- Archive / Other。

每个 Attachment Row：

- 类型图标。
- Display Name。
- Version。
- Platform。
- Size。
- Added At。
- Availability。
- Download State。
- Checksum Verified 状态（如果服务端提供用户语义）。
- More。

---

## 7. Attachment 操作

More：

- Download。
- Share（权限允许）。
- Open With（本机支持）。
- 查看信息。
- 关联到 Version。
- Archive / Restore（有权限）。

如果 Attachment 为 Archive / Cold：点击 Download 先进入 Restore Sheet：

- 当前状态。
- `开始恢复`。
- 恢复完成通知。

恢复完成后再下载，不把 Restoring 显示为“下载 0%”。

---

## 8. Version / Edition

版本卡片字段：

- Name / Version。
- Platform。
- Release Date。
- Region / Language（适用时）。
- Related Attachments Count。
- Preferred 标识。

Tap 进入版本详情，列出 Installer、Patch、MOD Compatibility、Docs。

---

## 9. MOD 页面

MOD Card：

- Name。
- Version。
- Compatible Game Version。
- Author / Source。
- Description。
- Attachment Size。
- Availability。
- Download。

Ikaros 默认只管理资料，不自动修改游戏目录。若插件提供安装能力，必须明确标识“由插件执行”，并在操作前预览目标路径 / 影响。

---

## 10. Save Backup

每个 Save Backup：

- Name。
- Game Version。
- Created / Imported At。
- Device / Source（可用时）。
- Size。
- Notes。
- Download。

恢复到本机属于高风险本地覆盖动作时：

- 先展示目标路径。
- 是否覆盖已有存档。
- 可选先创建本地备份。
- 用户确认后执行。

---

## 11. Manual / Document

PDF / README / Guide 使用 Document Viewer。

若关联普通 Ikaros Document，点击打开 Document 页面；若是 Attachment，使用合适 Viewer / 系统打开。

---

## 12. Search

游戏内搜索范围：

- Title / Alias。
- Platform。
- Version。
- Asset Name。
- Tag。
- External Identity。

Search Result 必须区分 Game Resource 与 Attachment。

---

## 13. Offline

- 已下载 Installer / MOD / Save 明确进入“我的下载”。
- Cache 只用于预览与临时打开，不标记为 Downloaded。
- 游戏资料的大对象下载支持暂停、恢复、失败重试。
- Offline 时仍可浏览本地索引与已下载资料。

---

## 14. 响应式

- Compact：Game Grid 2 列或自适应；详情单列。
- Medium：详情 Header 两列。
- Expanded：左侧 Game List / Filter，右侧详情；资料页可使用 Data Table。
- Large：附件表格保持列宽上限，长名称优先 Title 列伸缩。
