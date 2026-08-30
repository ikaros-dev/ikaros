# Library：统一资源库、Resource 详情、Collection、标签与关系

## 1. 页面目录

- 统一资源库。
- Resource 通用详情。
- Resource 元数据详情。
- 外部身份与来源。
- Collection 列表。
- Collection 详情。
- 标签浏览。
- Relation 关系浏览。
- 生命周期操作 Sheet。

Resource 的专业消费界面由对应子系统继续细化：Video、Reading、Music、Photos、Documents、Games。

---

## 2. 统一资源库页

### 2.1 App Bar

- 标题：`资源库`。
- Search。
- View Toggle：Grid / List（Desktop 可常驻，Mobile 放 More）。
- Sort。
- Filter。

### 2.2 顶部快捷筛选

横向 `FilterChip`：

- 全部
- 收藏
- 最近添加
- 最近访问
- 已下载
- 离线可用

第二行类型 Chip 允许横向滚动：视频、动画、电影、漫画、小说、音乐、图片、文档、游戏、其他。

### 2.3 Grid Card

通用字段：

- 封面。
- Resource Type 小图标 / Chip。
- 标题。
- 可选第二标题。
- 关键元信息，例如年份、作者、艺术家。
- Progress。
- Availability Chip，仅状态非普通 Available 时显示。
- Favorite 图标。

#### 封面比例

- 海报类：2:3。
- 专辑 / 图片：1:1。
- 文档：卡片图标 + 4:3 Preview。
- 通用视频：16:9。

网格必须允许不同类型按类型模板渲染，但卡片底部信息区高度保持稳定。

### 2.4 List Row

- 72dp Thumbnail。
- Title。
- Type + Meta。
- Collection / Tags 摘要。
- Availability。
- Updated At。
- Progress（适用时）。
- More。

### 2.5 分页 / 无限加载

Mobile 默认 Infinite Scroll；Desktop 可以 Infinite Scroll 或分页，但同一客户端内保持一致。

Loading More 只在列表底部显示，不重新 Skeleton 已加载项目。

---

## 3. Library Filter

### 3.1 Filter Sheet 字段

- Resource Type，多选。
- Lifecycle：Active / Archived / Trash。
- Availability。
- Favorite。
- Has Download。
- Collection。
- Tag。
- Added Date。
- Updated Date。
- External Provider。
- Metadata Source。

### 3.2 交互

选中的过滤条件在列表顶部以可删除 Chip 展示。

`清空` 只清理过滤条件，不清搜索关键词。

复杂筛选可“保存为 Smart View”（当该能力落地时）。

---

## 4. Resource 通用详情页

专业类型详情页共享一个通用 Header。

### 4.1 Header

Compact：

1. 大封面 / Hero Image。
2. 标题。
3. Original / Alternative Title 一行，可展开。
4. 类型 + 年份等核心 Chip。
5. Availability 状态。
6. 主操作组。

Desktop：封面在左 240–320dp，右侧信息区。

### 4.2 主操作组

根据类型动态出现：

- `播放 / 继续观看`。
- `阅读 / 继续阅读`。
- `播放音乐`。
- `打开文档`。
- `查看图片`。

通用次操作：

- 收藏。
- Collection。
- 下载。
- 分享。
- More。

主操作只有一个 Filled Button；其他使用 Tonal / Outlined / Icon。

### 4.3 状态说明卡

当 Resource 非立即可用时显示。

#### Remote

`内容位于远端存储，打开时将读取远端内容。`

#### Restoring

显示：

- `正在从归档层恢复`。
- Progress。
- Background Task ID 的用户友好入口。
- `完成后通知我` Switch（若通知系统支持）。

#### Missing

显示缺失说明，不展示“重试播放”作为唯一动作。

#### Corrupted

使用 Error Container，提供`查看可用版本`、`报告问题`，普通用户不直接看到 Blob 管理动作。

---

## 5. Resource 详情 Tabs

根据类型组合：

- 概览。
- 内容 / 剧集 / 章节 / 曲目。
- 信息。
- 关系。
- 附件。
- Activity。

### 5.1 概览

- Summary / Description。
- 关键人员 / 作者。
- Tag。
- Collection。
- Continue Progress。
- Related Resource Preview。

### 5.2 信息

使用 Definition List，不使用一大段 JSON：

- Resource ID（放高级信息）。
- Type。
- Created At。
- Updated At。
- Language。
- Titles。
- Lifecycle。
- Metadata Provenance 摘要。

### 5.3 附件

普通用户只看到业务相关附件：

- 封面。
- 视频版本。
- 字幕。
- 电子书。
- 图片。
- 文档附件。

每项：名称、类型、大小、Availability、Download 状态。

不展示 Blob Placement 内部管理细节。

---

## 6. 多标题与语言

在 Resource Detail 标题区域提供 `全部标题` 展开项。

Side Sheet / Bottom Sheet 字段：

- 标题文本。
- Locale / Language。
- Title Type：Original / Display / Alias / Romanized 等。
- Source：Manual / Provider / Import。

搜索命中别名时，Search Result 可提示：`匹配别名：xxx`。

---

## 7. Metadata Provenance

### 7.1 入口

详情 More → `元数据来源`。

### 7.2 页面

按字段显示：

| 字段 | 当前值 | 来源 | 状态 |
|---|---|---|---|
| 标题 | ... | 用户手动 | 已锁定 |
| 简介 | ... | Provider | 自动管理 |
| 年份 | ... | 文件扫描 | 自动管理 |

### 7.3 冲突

字段有外部新值但用户已人工修改时：

- 当前值卡。
- 外部建议值卡。
- 来源 Provider。
- `保留我的值`。
- `采用外部值`。
- `恢复自动同步`。

批量采用前必须预览变化。

---

## 8. External Identity

详情 More → `外部身份`。

每项：

- Provider 图标与名称。
- Namespace。
- External ID。
- 可选外部链接。
- 最近同步。

普通 App 可以查看和轻量修正自己有权限的映射；复杂 Provider 管理在 CMS。

---

## 9. Collection 列表

### 9.1 页面布局

顶部：Title + Create。

分段：

- Pin / Favorite Collections。
- 最近使用。
- 全部 Collection。

Collection Card：

- Cover Mosaic，最多 4 张缩略图。
- Name。
- Resource Count。
- 手动 / 智能类型 Chip。
- Updated At。
- More。

### 9.2 Create Collection

Bottom Sheet / Dialog：

- Name *。
- Description。
- Visibility。
- Cover Strategy：自动 / 自定义。
- `创建`。

智能 Collection 的 Query Builder 放 Advanced，Mobile 不在首屏暴露复杂表达式。

---

## 10. Collection 详情

### 10.1 Header

- Cover / Mosaic。
- Name。
- Description。
- Resource Count。
- Owner / Shared 状态。
- `添加资源`。
- Share。
- More。

### 10.2 Content

使用同 Library Card。支持：

- 排序。
- 筛选。
- 多选。
- 用户自定义顺序（手动集合）。

Desktop 拖拽排序；Mobile 使用“编辑顺序”页上下拖动 Handle，避免普通浏览误拖。

---

## 11. 标签浏览

标签页：

- Search Tag。
- 常用标签 Chips。
- 全部标签 List。

每行：Tag Name、Resource Count、可选颜色 / Icon。

Tap 进入 Tag Result Page，使用 Library Grid。

系统标签与用户标签在视觉上使用不同 Icon，并有说明。

---

## 12. Relation

Resource Detail 的“关系”使用按 Relation Type 分组的卡片：

例如：

- Episode Of。
- Sequel / Prequel。
- Adaptation。
- Related To。
- Derived From。

每组默认只显示前 6 项，`查看全部`。

关系是双向导航，但 UI 必须显示关系方向，例如：

```text
本资源 --EPISODE_OF--> 某作品
```

不能只写“相关”。

---

## 13. 生命周期操作

More → `管理`。

普通用户有权限时提供：

- Archive。
- Restore from Archive。
- Move to Trash。
- Restore from Trash。

`Permanently Delete` 只有明确权限时显示，并使用高风险 Dialog；说明永久删除 Resource 与底层 Blob GC 不等价，最终内容回收由服务端规则决定。

---

## 14. 批量选择

Library 长按 / Desktop Checkbox 进入 Selection Mode：

顶部 Action Bar：

- 已选数量。
- 收藏。
- 加入 Collection。
- 标签。
- 下载。
- 更多。

删除不放在第一排，进入 More 后二次确认。

---

## 15. 响应式

- Compact：2–3 列海报网格，视卡片最小宽度动态决定，不硬编码固定 3 列。
- Medium：3–5 列。
- Expanded：4–7 列，最大卡宽约 220dp。
- Large：内容区域限制最大宽度，避免无限增加列数。
- Detail >=840dp 可使用 Header + 右侧 Context Panel；Compact 全部纵向。
