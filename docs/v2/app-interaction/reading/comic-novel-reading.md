# Reading：漫画、小说与阅读器

## 1. 页面目录

- 阅读首页。
- 漫画作品详情。
- 漫画章节列表。
- 漫画阅读器。
- 小说作品详情。
- 小说卷 / 章节列表。
- 小说阅读器。
- 阅读设置。

---

## 2. 阅读首页

内容顺序：

1. Continue Reading。
2. 漫画。
3. 小说 / Ebook。
4. 待读 Collection。
5. 最近添加。

Continue Card 显示：Cover、Title、Chapter、Progress、Last Read、Offline State。

---

## 3. 漫画作品详情

Header：

- Cover。
- Title / Alternate Title。
- Author。
- Status / Year。
- Read Progress。
- `继续阅读`。
- `从头阅读` 放 More。
- Download。
- Favorite。

Tabs：章节、简介、信息、关联。

---

## 4. 漫画章节页

### 4.1 Group

按 Volume / Arc 分组，Group Header：Name、完成数 / 总数。

### 4.2 Chapter Row

- Chapter Number。
- Title。
- Page Count。
- Read State。
- Last Read。
- Download State。
- Availability。
- More。

`已读` 使用 Check，不只用文本颜色。

### 4.3 批量下载

Selection Mode：选择 Volume / Chapter → Download。

显示预计大小（可知时）和空间提示。

---

## 5. 漫画阅读器

### 5.1 模式

必须支持：

- 单页。
- 双页。
- 连续纵向滚动。
- 条漫。
- Left-to-Right。
- Right-to-Left。

### 5.2 Immersive UI

默认内容全屏，Chrome 隐藏。

Tap 中间区域显示：

Top Bar：Back、Chapter Title、More。

Bottom Bar：Page `12 / 48`、Slider / Scrubber、Previous Chapter、Next Chapter、Settings。

### 5.3 单页

页面居中 Fit Contain。

- Tap 左 / 右区域翻页，方向受阅读方向设置影响。
- Pinch Zoom。
- Zoom 后拖动图片，禁止误触翻页。

### 5.4 双页

Desktop / Tablet 默认更适合。

- 奇偶页配对规则可设置封面是否单页。
- 窄屏自动回退单页并提示一次。

### 5.5 连续滚动 / 条漫

- 使用虚拟化 / 懒加载。
- 当前阅读位置按可见页和偏移保存。
- Page Gap 可配置。

### 5.6 Page Scrubber

拖动显示小预览缩略图与页码。

如果缩略图未生成，显示页码而不是阻断操作。

### 5.7 章节切换

到末页显示轻量 Chapter End Sheet：

- 已完成章节。
- 下一章标题。
- `下一章`。
- `返回作品`。

可设置自动进入下一章。

---

## 6. 漫画阅读设置

Bottom Sheet：

- Reading Mode。
- Direction。
- Fit Width / Fit Height / Contain。
- Page Gap。
- Background：Black / Dark / Light / System。
- Keep Screen On。
- Auto Next Chapter。

设置分为：`本书` 与 `全局默认`，避免用户改一本书导致全部书意外改变。

---

## 7. 小说作品详情

Header：Cover、Title、Author、Progress、Continue、Download、Favorite。

Tabs：目录、简介、信息、关联。

目录允许 Volume 折叠，默认展开当前阅读位置所在卷。

---

## 8. 小说章节 Row

- Chapter Number / Index。
- Title。
- Estimated Read Time（可计算时）。
- Read / Unread。
- Downloaded。
- More。

Tap 打开阅读器并从上次位置继续；More 提供“从头打开”。

---

## 9. 小说阅读器

### 9.1 内容区

正文容器默认最大宽度：

- Mobile：屏宽减左右 Padding。
- Tablet：600–720dp。
- Desktop：720–860dp。

不在超宽屏把一行文字拉到 1500dp。

### 9.2 Top / Bottom Chrome

Tap 中间显示：

Top：Back、Book Title、Chapter Title、More。

Bottom：Chapter Progress、Previous、TOC、Next、Settings。

### 9.3 阅读方式

支持：

- Continuous Scroll。
- Paginated（Flutter 实现可后续评估，但文档保留产品语义）。

### 9.4 进度

保存：Chapter + 字符 / 段落 / CFI 类稳定定位语义，最终由格式适配层决定。

只保存屏幕像素 Offset 不足以支持跨设备恢复。

---

## 10. 小说阅读设置

分组：

### Typography

- Font Family。
- Font Size。
- Font Weight（允许时）。
- Line Height。
- Paragraph Spacing。
- Letter Spacing。

### Layout

- Content Width。
- Page Margin。
- Text Alignment。

### Theme

- System。
- Light。
- Sepia。
- Dark。
- OLED Black。

### Behavior

- Keep Screen On。
- Volume Key Page Turn（Android 可选）。
- Auto Next Chapter。

实时预览当前正文，点击“重置”恢复书籍 / 全局默认。

---

## 11. Annotation / Link

普通 Document / Resource Link 如果存在：

- 选中文字可创建普通 Note / Quote（未来能力）。
- Internal Link Tap 打开目标 Resource。

私密引用必须遵循 Secure Domain，不把 Private Note 的存在反向暴露。

---

## 12. AI 阅读辅助

AI 可用时 More → `阅读助手`：

- 本章摘要。
- 人物 / 实体回顾。
- 翻译选中内容。
- 解释选中内容。

AI 功能必须标注生成内容；用户关闭 AI 或当前内容不允许发送外部 Provider 时隐藏 / 限制。

漫画 OCR / 翻译属于 AI Job 时显示处理进度，不覆盖原始页面。

---

## 13. Offline

- 已下载章节完全离线可读。
- Cache 可用于当前阅读，但不标记为 Downloaded。
- Offline 阅读进度本地记录。
- 章节下载失败支持单章重试。

---

## 14. 响应式

- Compact：沉浸单页 / 单列正文。
- Medium：漫画可默认单页或双页由方向与宽度决定；目录可 Side Sheet。
- Expanded：漫画双页；小说左 TOC 280dp + 正文，TOC 可收起。
- Large：正文保持最大阅读宽度，左右空白可作为控制 / Annotation Pane，不拉宽文字。
