# Home：首页、全局搜索与我的活动

## 1. 页面目录

- Home Dashboard。
- 全局搜索。
- 搜索筛选页 / Sheet。
- 我的 Activity Timeline。
- Continue Center。

---

## 2. Home Dashboard

首页不是系统管理 Dashboard，而是个人内容与行动入口。

### 2.1 App Bar

从左到右：

- Menu / Logo。
- `早上好 / 下午好 / 晚上好` + 用户 Display Name，Desktop 可简化为 `首页`。
- Search。
- Notification Bell + 未读 Badge。
- 可选头像。

### 2.2 Compact 布局顺序

1. 全局 Search Bar。
2. Today Summary Card。
3. Continue 区域。
4. Quick Actions。
5. 最近添加。
6. 我的收藏 / 最近 Collection。
7. AI Brief（用户开启时）。
8. 最近 Activity。

### 2.3 Today Summary Card

字段：

- 日期、星期。
- Today Task：完成 / 总数。
- 下一 Calendar Event / Time Block。
- Overdue 数。
- Habit 待打卡数。
- 可选 Upcoming Bill。

底部操作：`打开 Today`。

若用户未启用 Productivity，则整卡隐藏，不留空占位。

### 2.4 Continue 区域

横向卡片，可混合但明确类型：

- Continue Watching。
- Continue Reading。
- Continue Listening。
- Continue Editing。

卡片字段：封面、类型、标题、当前进度、最后活动时间。

Tap 直接进入对应消费 / 编辑页面，而不是先回 Resource Detail；长按可“查看详情”。

### 2.5 Quick Actions

Compact 使用 2×N Grid；Desktop 使用横向按钮组。

默认：

- 新建 Task。
- 记一笔。
- 新建笔记。
- 扫码 / 导入（支持时）。
- AI Assistant。

允许用户在设置中排序和隐藏，不允许添加无权限动作。

### 2.6 最近添加

显示 6–12 个 Resource Card。

Header：`最近添加` + `查看全部`。

### 2.7 AI Brief

仅用户开启 AI Daily Brief 时展示。

内容最多 3–5 条：

- 今日重要 Task。
- 冲突。
- 新增喜欢内容。
- 需要关注的通知。

卡底部：`和 Ikaros 继续聊`。

AI Brief 必须明显标注 AI 生成，不把推断当业务事实。

### 2.8 Desktop

>=1200dp 首页使用 12 列 Grid：

- 左 8 列：Continue、最近添加、Activity。
- 右 4 列：Today、Quick Actions、AI Brief。

所有卡片顶部对齐，避免瀑布式随机高度导致阅读跳跃。

---

## 3. 全局搜索页

### 3.1 顶部

- 返回。
- Search Bar 自动聚焦。
- Clear。
- Voice Search（未来能力，不作为 P0 必需）。
- Filter。

Desktop 可在 Search Bar 右侧显示快捷键提示 `Ctrl K`。

### 3.2 Search Suggestion

输入前：

- 最近搜索。
- 最近访问。
- 常用标签。
- 可选保存的搜索。

输入后：

- 即时建议标题 / 别名。
- 仅在 debounce 后请求远端。
- 支持键盘上下选择。

### 3.3 搜索结果分区

顶部 Segmented / Tabs：

- 全部
- Resource
- 文档
- Task
- Collection
- 其他

结果卡必须显示对象类型，避免同名对象不可辨识。

Resource 结果字段：封面、标题、多语言匹配说明、类型、Collection、可用状态、消费进度。

Task：状态 Checkbox、标题、Project、Scheduled / Deadline。

Document：图标、标题、摘要、更新时间、类型。

### 3.4 私密结果

未解锁 Private Notes：

```text
[锁] 私密笔记
发现可能的本地匹配结果
[解锁查看]
```

不得显示标题、标签、正文片段。

Password Manager 默认不参与普通内容搜索。只有用户在 Password Manager 内部搜索。

### 3.5 筛选

Filter Sheet / Side Sheet 字段：

- Resource Type，多选。
- 时间范围。
- 标签，多选。
- Collection。
- 收藏状态。
- Lifecycle：Active / Archived / Trash（有权限时）。
- Availability：Available / Cached / Remote / Restoring 等。
- Source：Local / Provider / Plugin（适用时）。
- Sort：Relevance / Recently Updated / Recently Added / Title。

底部：`重置`、`应用`。

Desktop 筛选可以固定在右侧 320dp Pane。

---

## 4. 搜索零结果

显示：

- `没有找到“xxx”`。
- 建议检查拼写、清除筛选。
- `在外部 Provider 搜索`（安装对应 Search Provider 时）。
- `让 AI 帮我找`（AI 可用时），但必须说明这会使用不同的语义检索方式。

---

## 5. 我的 Activity Timeline

### 5.1 页面目标

聚合用户业务行为，不与管理员 Audit 混淆。

### 5.2 顶部筛选

- Today / 7 Days / 30 Days / Custom。
- 类型：播放、阅读、编辑、Task、Goal、Focus、Finance（仅适合的非敏感摘要）、其他。

### 5.3 Timeline Item

字段：

- 时间。
- 类型图标。
- 动作文本，例如“看完第 8 集”。
- 目标对象标题。
- 可选 Duration / Progress。
- 来源客户端。
- More。

Private Notes 默认只显示“更新了私密笔记”，不显示标题；用户可以完全关闭该类 Activity。

### 5.4 删除 Activity

用户可删除允许删除的个人历史：

- 单条 More → 删除。
- 筛选后批量清理。

Dialog 明确：这不会删除 Resource / Task，只删除活动历史。

---

## 6. Continue Center

当 Continue 项目较多时从首页“查看全部”进入。

Tabs：

- 观看
- 阅读
- 收听
- 编辑

每项显示：

- Resource。
- Progress。
- Last Activity。
- 本地可用状态。
- `继续` 主按钮。
- More：从继续列表移除、查看详情、下载。

离线时优先把已下载 / 已缓存且可完整读取内容排在前面，并显示 `离线可用` Chip。

---

## 7. 状态与响应式

- 首页每个模块独立加载，某一模块失败不阻断整个首页。
- Desktop 支持用户调整首页 Card 顺序；Mobile 只允许在“首页布局设置”中重排，避免误拖。
- Home 不显示系统级运维图表；管理员异常通过普通 Notification / Alert 摘要进入用户允许的入口。
