# Photos：图片与相册

## 1. 页面目录

- 图片首页 / 时间线。
- Album 列表。
- Album 详情。
- Photo Viewer。
- Photo Info。
- 多选与批量操作。

---

## 2. 图片首页

### 2.1 App Bar

- 标题 `图片`。
- Search。
- Filter。
- Select。

### 2.2 视图切换

- Timeline。
- All Photos。
- Albums。

Timeline 默认按日期分组。

### 2.3 日期 Group

Header：

- 日期，例如 `2026 年 8 月 30 日`。
- 当天图片数量。
- Select Group。

Grid 使用接近正方形 Thumbnail，图片使用 `BoxFit.cover`；进入 Viewer 查看完整比例。

---

## 3. 图片 Grid

### Item Overlay

只在必要时显示：

- Favorite。
- Video / Live Photo 类型（未来支持时）。
- Remote / Restoring 状态。
- Selected Check。

普通 Available 图片不堆叠状态文字。

### 交互

Tap：Viewer。

Long Press：进入 Selection Mode 并选中当前图。

Desktop Hover：右上角 Selection Checkbox 与 Quick Favorite。

---

## 4. Album 列表

Album Card：

- Cover。
- Name。
- Photo Count。
- Date Range。
- Shared 状态。

支持：Create Album、Sort、Search。

---

## 5. Album Detail

Header：

- Cover / Hero。
- Name。
- Description。
- Count。
- Date Range。
- Share。
- Add Photos。
- More。

Content 使用 Photo Grid。

手动 Album 支持 Edit Order；智能 Album 显示 Query 摘要，普通 App 只提供常见筛选编辑，高级表达式可转 CMS / Advanced 页面。

---

## 6. Photo Viewer

### 6.1 Immersive

黑 / 深色背景优先。

Top Bar：Back、Date / Album、Favorite、Share、More。

Bottom Bar：Previous / Next（Desktop 可隐去）、Info、Edit Metadata（有权限）、Download。

### 6.2 手势

- 左右 Swipe：上一张 / 下一张。
- Pinch：Zoom。
- Double Tap：Zoom Toggle。
- Zoom 后单指拖动图片，不触发换图直到回到边界并达到阈值。

Desktop：鼠标滚轮默认不换图；Arrow Key 换图。

### 6.3 预加载

预加载前后 1–2 张适配尺寸的 Preview，不在移动端无上限预拉原图。

---

## 7. Photo Info Sheet

字段分组：

### Basic

- Title / Filename Display。
- Date Taken。
- Added At。
- Dimensions。
- File Size。
- MIME / Format。

### Camera / EXIF

存在时显示：

- Device / Camera。
- Lens。
- ISO。
- Aperture。
- Shutter。
- Focal Length。

### Location

存在且用户有权限时：

- Location Text。
- Map Preview（未来可选）。

### Organization

- Albums / Collections。
- Tags。
- Related Resource。

### Storage

只显示用户语义：Available / Cached / Remote / Restoring、Downloaded；不直接展示底层 Object Key。

---

## 8. AI 能力

图片 More / Selection Mode：

- 生成描述。
- OCR。
- 建议标签。
- 语义搜索。

AI 处理属于 Job 时显示 Job Progress；生成标签先进入 Suggestion Preview，用户确认后写入。

---

## 9. 多选操作

Action Bar：

- Add to Album。
- Favorite。
- Download。
- Share。
- Tag。
- More。

More：Archive / Trash（有权限）。

批量下载显示总数量与预计空间。

---

## 10. Original / Preview

Viewer 默认选择适合屏幕的 Preview / Derived Attachment。

提供 `查看原图`：

- 原图 Remote 时提示网络与大小。
- 原图 Archive 时触发 Restore Flow。
- Preview 清理不等于删除原图。

---

## 11. Offline

- 已下载原图 / Preview 可离线查看。
- Cache 命中时显示内容但仍不标记 Downloaded。
- Offline Selection 可修改 Favorite / Album 等允许同步的元数据，标记 Pending。

---

## 12. 响应式

- Grid 根据最小 Thumbnail 120–180dp 自适应列数。
- Compact Viewer 全屏。
- Expanded Viewer 左侧图像 + 右侧可固定 Info Pane 320–360dp。
- Large Timeline 不把 Thumbnail 放大到失去浏览密度，增加列数但设置最大列宽。
