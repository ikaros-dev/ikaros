# Documents：文章、普通笔记、文档与轻量创作

## 1. 页面目录

- 文档首页。
- 文档 / 文章列表。
- 普通 Note 列表。
- 文档详情。
- 编辑器。
- Revision 历史。
- 评论 / 批注。
- 发布设置。
- 协作状态。

本子系统仅描述普通 Document / Note / Article。高度敏感内容进入 Private Notes，不使用一个普通 `is_private` 开关替代安全域。

---

## 2. 文档首页

### 2.1 App Bar

- 标题：`文档与创作`。
- Search。
- `新建` Filled Button / FAB。
- More：导入、模板、排序。

### 2.2 顶部分段

使用 Tabs / Segmented：

- 最近。
- 我的文档。
- Note。
- 文章。
- 草稿。
- 已发布。
- 与我共享。

### 2.3 最近文档卡片

字段：

- 类型图标：Document / Note / Article。
- Title。
- 摘要，最多 2 行。
- Updated At。
- Status：Draft / Published / Archived。
- Collaborator Avatar Stack（存在协作时）。
- Attachment Count。
- Offline / Pending Sync 状态。

Tap 打开详情或编辑器；More：重命名、移动到 Collection、分享、下载导出、归档、移入回收站。

---

## 3. 新建入口

点击 `新建` 打开 Bottom Sheet / Menu：

- 普通笔记。
- 文档。
- 文章 / 博客文章。
- 从模板创建。
- 从文件导入。
- `私密笔记` 单独入口跳转 Private Notes，并标注“端到端加密”，不在普通 Document 中创建。

选择后进入编辑器，未输入内容前返回可直接取消；已有内容时提示保存草稿 / 丢弃。

---

## 4. 文档详情页

适用于只读查看状态。

### 4.1 Header

- 类型 Icon。
- Title。
- Status Chip。
- Owner。
- Last Updated。
- `编辑` Filled Button（有权限）。
- `分享`。
- More。

### 4.2 Metadata Bar

横向 Wrap：

- Tags。
- Collection。
- Language。
- Revision Number。
- Comment Count。
- Attachment Count。

### 4.3 正文

按照文档内容结构渲染：Heading、Paragraph、List、Checklist、Table、Code Block、Callout、Image、Attachment、Resource Embed、Link。

外链点击前按平台安全策略显示目标域名；Resource Embed 点击进入 Resource Detail。

### 4.4 右侧 Context Pane（Desktop）

Tabs：

- Outline。
- Comments。
- Details。
- Activity。

Compact 使用 Bottom Sheet / 独立页。

---

## 5. 编辑器整体布局

### 5.1 Compact

```text
┌───────────────────────┐
│ Back  Draft  Save More│
├───────────────────────┤
│ Title                 │
│ Metadata Chips        │
├───────────────────────┤
│                       │
│ Editor Body           │
│                       │
├───────────────────────┤
│ Context Toolbar       │
└───────────────────────┘
```

### 5.2 Expanded

三栏可选：

- 左 240–300dp：Outline / Document Tree。
- 中 640–900dp：正文编辑。
- 右 300–360dp：Properties / Comments / AI。

左右栏可收起，编辑正文始终保持主要宽度。

---

## 6. 编辑器顶部字段

1. `Title *`：大号文本框；Enter 不提交页面。
2. Status：Draft / Published / Archived；普通编辑时默认不允许直接点 Published，发布走 Publish Flow。
3. Tag Chips：添加 / 删除。
4. Collection：可选多选。
5. Language / Locale。
6. 可选 Cover / Hero Attachment。

字段离焦后本地保存 Draft；不要每个按键都立即远端提交。

---

## 7. 编辑器 Toolbar

根据编辑模式提供：

- Undo / Redo。
- Paragraph / Heading。
- Bold / Italic / Strike。
- Link。
- Bullet / Numbered List。
- Checklist。
- Quote。
- Code / Code Block。
- Table。
- Callout。
- Image。
- Attachment。
- Embed Resource。
- Mention / Collaborator（支持时）。
- AI Assistant（用户开启时）。

Mobile 使用横向可滚动 Bottom Toolbar；高频按钮固定，低频进入 `+` 菜单。

---

## 8. Attachment 插入

点击 Attachment：

- 从设备选择。
- 从 Ikaros Attachment / Resource 选择。
- 拍照 / 扫描（Mobile 支持时）。

上传项显示：

- Filename。
- Size。
- Upload Progress。
- Cancel / Retry。

完成后插入 Document Node，而不是把临时本地路径作为永久内容。

图片支持 Alt Text / Caption。

---

## 9. Resource Embed

选择 Resource 后插入卡片：

- Cover / Icon。
- Title。
- Type。
- Short Metadata。
- Availability（必要时）。

编辑器只保存 Resource 引用，不复制 Resource 元数据成为不可追踪快照，除非文档格式明确需要静态快照。

---

## 10. 保存与同步状态

App Bar 标题附近显示状态：

- `已保存`。
- `正在保存…`。
- `离线 · 已保存到本机`。
- `待同步`。
- `同步冲突`。
- `保存失败`。

离线编辑写本地队列；联网后同步。

离开页面前如果本地保存成功，不以“远端暂时失败”阻止用户退出。

---

## 11. Revision 历史页

### 11.1 列表

每个 Revision：

- Revision Number。
- Timestamp。
- Author。
- Change Summary（可用时）。
- AI Assisted / Human 标识（有 Provenance 时）。
- `当前版本` Chip。

### 11.2 详情

Desktop：左右 Diff；Mobile：切换 Before / After / Diff。

操作：

- 查看此版本。
- 与当前比较。
- 恢复到此版本。

恢复并不是删除后续历史；创建新的 Revision 表达恢复动作。

---

## 12. 评论 / 批注

Comment 可绑定：

- 整篇文档。
- 选中文字 / Block。

Comment Card：Avatar、Author、Time、Content、Resolved State、Reply Count。

操作：回复、Resolve / Reopen、编辑自己的评论、删除自己的评论（按权限）。

点击正文批注 Marker 高亮对应文本。

---

## 13. 协作状态

实时协作可用时：

- App Bar 显示在线协作者 Avatar Stack。
- 点击打开 Collaborator Panel。
- 正文 Cursor / Selection 使用明确身份颜色，但不只靠颜色，Hover / Tap 显示姓名。
- 网络断开时显示“已切换本地编辑”，不静默丢内容。

冲突无法自动合并时进入 Conflict Resolver，而不是 Last Write Wins。

---

## 14. Publish Flow

Article / Blog 点击 `发布`：

Bottom Sheet / Dialog 字段：

- Publish Status。
- Publish Time：立即 / 定时（能力支持时）。
- Visibility / Permission。
- Cover。
- Summary / Excerpt。
- Share / External Publish Provider（如插件可用）。

底部先 `预览`，再 `发布`。

若 AI 大幅参与内容，可显示 Provenance 选项，但最终内容仍归用户管理。

---

## 15. AI Writing Assistant

入口：选中文字浮动菜单或侧栏。

动作：

- 改写。
- 扩写。
- 缩写。
- 翻译。
- 调整语气。
- 语法检查。
- 生成大纲。
- 基于已授权 Resource 辅助写作。

AI 结果以 Suggestion Card 显示：

- 原文。
- 建议文本。
- 使用的 Model / Persona（详情中）。
- 来源引用（存在时）。
- `替换`、`插入下方`、`复制`、`丢弃`。

不得自动覆盖用户正文。

---

## 16. Offline

- 已同步文档可离线读取。
- 用户明确 Pin / Download 的文档及附件离线可用。
- 离线可新建、编辑、评论草稿（协作评论是否允许离线由 API 能力决定）。
- Pending Change 必须可查看。

普通 Document 本地缓存可被清理；用户主动下载 / Pin 的离线副本单独管理。

---

## 17. 响应式

- Compact：单栏编辑器，Toolbar 底部。
- Medium：正文 + 可切换 Side Sheet。
- Expanded：Outline / Body / Context 三栏。
- Large：正文仍限制可读宽度，右侧空余用于 Comments / AI，而不是拉宽段落。
