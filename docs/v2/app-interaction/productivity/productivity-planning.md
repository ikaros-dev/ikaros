# Productivity：效率与计划

## 1. 页面目录

- Today。
- Inbox。
- Upcoming。
- Task 列表。
- Task 详情 / 编辑。
- Project 列表 / 详情。
- Kanban。
- Eisenhower Matrix。
- Calendar。
- Time Block 编辑。
- Goal 列表 / 详情。
- OKR Cycle / Objective / Key Result。
- Milestone。
- Habit。
- Focus。
- Daily / Weekly / Monthly / Quarterly Review。

本子系统严格区分：Task、Calendar Event、Scheduled Time、Deadline、Time Block、Goal、Habit、Focus Session。

---

## 2. Today 页面

### 2.1 App Bar

- 标题 `今天` + 日期。
- Calendar Icon。
- `+` Quick Add。
- More：Daily Planning、筛选、排序。

### 2.2 顶部 Summary

Compact 使用横向卡片：

- Task：`3 / 7 已完成`。
- Focus：`1h 20m`。
- Habit：`2 / 4`。
- Overdue：`2`。

这些是事实摘要，不显示“效率分数”。

### 2.3 时间线区域

按时间顺序混合但视觉可区分：

- Calendar Event。
- Scheduled Task。
- Time Block。
- Habit Schedule。

每种使用不同 Icon + Type Label。

### 2.4 Unscheduled Today

没有 Scheduled Time、但 Pin 到 Today / Deadline Today 的 Task 放单独区块。

### 2.5 Overdue

置于 Today 顶部或显眼 Warning Section，但不把 `Overdue` 当 Task Lifecycle Status。

每项快速动作：完成、重新安排、打开详情。

---

## 3. Quick Add Task

### 3.1 Compact Bottom Sheet

第一屏只保留高频字段：

- `标题 *`。
- Today / Tomorrow / Date 快捷 Scheduled Time。
- Deadline 图标。
- Project。
- Priority。
- `创建`。

点击 `更多` 展开完整字段。

### 3.2 自然语言 AI

AI 可用时提供 `用自然语言创建`：

例如输入“下周三前完成数据库设计，预计4小时，拆三步”。

AI 输出先生成 Draft Preview：Title、Scheduled、Deadline、Estimate、Subtasks；用户确认后创建。

---

## 4. Inbox

### 4.1 目标

快速 Capture，不要求先决定 Project / 时间 / Goal。

### 4.2 Row

- Checkbox / Status Icon。
- Title。
- Created At。
- Optional Note Indicator。
- More。

### 4.3 Clarify Flow

Tap Item 打开 Bottom Sheet：

- 加入 Project。
- Scheduled Time。
- Deadline。
- Priority。
- Estimate。
- Related Resource。
- `保留在 Inbox` / `计划`。

---

## 5. Task 列表页

### 5.1 顶部 View

Tabs / Saved Views：

- All。
- Planned。
- In Progress。
- Completed。
- Blocked。
- Saved Smart Views。

### 5.2 Task Row

从左到右：

- Completion Checkbox。
- Title。
- Status Chip（仅特殊状态，如 Blocked）。
- Project / Section。
- Scheduled Time。
- Deadline。
- Priority Flag。
- Estimate。
- Assignee Avatar（共享项目）。
- Related Resource Icon。
- More。

Compact 只首行显示 Title，第二行组合 Project + Time；其余进入详情。

### 5.3 Swipe / Mouse

Mobile：

- 右滑：Complete，必须 Snackbar Undo。
- 左滑：Schedule / Snooze，可逆。

Desktop：Hover 显示 Quick Actions；右键 Context Menu。

---

## 6. Task 详情 / 编辑

### 6.1 Header

- Completion Checkbox。
- Title Editable。
- Status。
- More。

### 6.2 字段顺序

1. Description。
2. Project。
3. Section。
4. Status。
5. Priority。
6. Important Switch。
7. Urgent Switch。
8. Scheduled Start。
9. Scheduled End。
10. Deadline。
11. Estimated Duration。
12. Actual Duration（只读汇总 + 手动补录入口）。
13. Recurrence。
14. Reminder，多条。
15. Assignee。
16. Tags。
17. Related Resource。
18. Parent Task / Subtasks。
19. Dependencies：Blocks / Blocked By。
20. Comments / Activity。

### 6.3 Scheduled 与 Deadline

UI 必须用两个独立字段和不同 Icon：

- `计划执行`：Calendar Clock。
- `截止时间`：Flag / Deadline。

修改 Scheduled Time 不自动修改 Deadline。

### 6.4 Recurrence

显示：

- Daily / Weekly / Workday / Monthly / Yearly / Custom。
- 模式：Fixed Schedule / Completion-based。

重复实例可 `跳过本次`，不强迫标记 Completed。

---

## 7. Subtasks

Task Detail 内独立 Checklist Section：

- Title。
- Completed。
- 可拖动排序。
- `添加子任务`。

超过合理复杂度时可 `转换为 Project`，但不自动转换。

---

## 8. Dependencies

以关系卡片显示：

- `被以下任务阻塞`。
- `完成后将解除以下任务`。

Blocked Task 顶部显示 Warning Banner，并提供跳转阻塞项。

---

## 9. Project 列表

Project Card：

- Name。
- Status。
- Target Date。
- Progress：Completed Tasks / Total（仅辅助，不等同 Goal Progress）。
- Owner / Members。
- Related Goal。
- Overdue Count。

支持 List / Grid。

---

## 10. Project 详情

Header：Name、Description、Status、Target Date、Members、Related Goal、Add Task。

Tabs：

- List。
- Kanban。
- Timeline。
- Files / Related Resources。
- Activity。

Section 可配置 Backlog / Design / Development / Review / Done 等。

---

## 11. Kanban

### 11.1 Desktop

横向 Columns；每列 Header：Name、Count、Add。

Task Card：Title、Priority、Deadline、Assignee、Estimate、Blocked Icon。

Drag Card 改变对应分组字段；拖动前后显示目标状态，Drop 失败回滚并 Snackbar。

### 11.2 Mobile

默认单列 Column Selector + Card List；允许横向滑动切换列。

长按 Card 后拖动排序只在显式 Edit Mode 中启用，减少误操作。

---

## 12. Eisenhower Matrix

四象限 2×2：

- 重要且紧急。
- 重要不紧急。
- 不重要但紧急。
- 不重要不紧急。

Task 仍是同一对象，Matrix 仅 View。

Mobile 使用四个 Tabs / 可折叠 Section，避免 2×2 卡片在窄屏不可读。

拖动 Task 跨象限时只修改 Important / Urgent。

---

## 13. Calendar

### 13.1 View

- Day。
- Week。
- Month。
- Agenda。
- Desktop 可 Multi-day / Year（后续）。

### 13.2 Calendar Item 视觉

每项必须带类型标记：

- Event。
- Task Schedule。
- Deadline。
- Time Block。
- Habit。

### 13.3 Day / Week

左侧 Time Axis；全天区单独显示。

Time Block 可拖拽改变时间；Fixed Item 拖动前提示或禁止。

### 13.4 Drag Task to Calendar

Desktop 从右侧 Unscheduled Tasks 拖入时间区域：

- 默认创建 Scheduled Time / Time Block。
- **不修改 Deadline**。

Mobile 通过 Task More → `安排时间`。

---

## 14. Time Block 编辑

字段：

- Title。
- Start / End。
- Time Zone。
- Flexible / Fixed。
- Related Task。
- Related Goal / Project。
- Notes。
- Reminder。

冲突时显示 Warning：

- 冲突对象。
- 重叠时长。
- `仍然保存`。
- `查找其他时间`（AI / Planning 能力可用时）。

不强制阻止重叠。

---

## 15. Goal 列表

Filter：Active / Completed / Archived、Period、Type。

Goal Card：

- Title。
- Period / Target Date。
- Progress。
- Progress Source：Manual / Derived。
- Confidence / Health（如果业务有）。
- Related Project Count。
- Next Milestone。

---

## 16. Goal 详情

Header：Title、Description、Period、Owner、Progress、Status、Update Progress。

Sections：

- Milestones。
- Related Projects。
- Related Tasks。
- Habits。
- Resources。
- Check-ins。
- Activity。

Progress 由系统派生时，`Update Progress` 改为“查看计算方式 / Check-in”，避免用户误以为手动百分比一定是事实源。

---

## 17. OKR

### 17.1 Cycle 列表

Card：`2026 Q3`、Objective Count、Overall Status、Date Range。

### 17.2 Objective

- Objective Title。
- Owner。
- Progress Summary。
- Key Result Count。
- Confidence Summary。

### 17.3 Key Result Row

- KR Title。
- Type：Numeric / Percentage / Boolean / Milestone。
- Start Value → Current → Target。
- Progress Bar。
- Confidence：On Track / At Risk / Off Track。
- Last Check-in。

### 17.4 Check-in Sheet

字段：

- Current Value。
- Confidence。
- Status。
- Note。
- Blocker。
- Date。

Confidence 与百分比分开显示，不从一个自动推断另一个。

---

## 18. Habit

### 18.1 Habit 首页

Today Habit Cards：

- Name。
- Metric Type：Boolean / Count / Duration / Numeric。
- Target。
- Current。
- Streak。
- Check-in Button。

### 18.2 Check-in

Boolean：Tap 完成，Undo。

Count / Numeric：Numeric Bottom Sheet。

Duration：输入或从 Focus Session 关联。

### 18.3 Habit Detail

- Calendar Heatmap / Completion Grid。
- Trend。
- Schedule。
- Related Goal / KR。
- Notes。

不将 Habit 与 Recurring Task 混为一体。

---

## 19. Focus

### 19.1 Focus Start

页面中心：

- 选择 Target：Task / Project / Goal / Resource / None。
- Mode：Stopwatch / Pomodoro / Custom Timer。
- Duration。
- `开始专注`。

### 19.2 Focus Mode

沉浸页面：

- Target Title。
- 大号 Timer。
- Pause / Resume。
- Finish。
- Minimal Note。

隐藏大部分导航；通知可按 Focus Policy 静音。

### 19.3 Finish Sheet

- Duration。
- Completed / Interrupted。
- Note。
- `是否完成相关 Task` 独立 Checkbox，默认不因 Focus 结束自动勾选。

---

## 20. Review

### Daily Review

- Planned Tasks。
- Completed。
- Incomplete / Deferred。
- Focus Time。
- Habit。
- Calendar Summary。
- Goal Progress。
- Reflection Note。

### Weekly Review

- Completed Tasks。
- Carry-over。
- Overdue。
- Project Progress。
- Goal / OKR。
- Focus Time。
- Habit Completion。
- Estimate vs Actual。
- Next Week Important Items。

Monthly / Quarterly 加入长期 Goal、OKR、Time Allocation、Completed Projects、Resource Consumption / Creation。

Review 可 `创建复盘文档`，跳到普通 Document，不把统计结果和主观笔记混成同一事实。

---

## 21. Offline

必须优先支持：

- Today / Inbox / Upcoming 离线查看。
- Offline 创建 Task。
- Offline Complete Task。
- Habit Check-in。
- 简单字段编辑。
- Focus Session 本地记录。

待同步项在顶部状态栏和对象 Row 上显示 `待同步`。

同一字段冲突不能静默覆盖；完成状态等幂等操作可按业务规则自动合并。

---

## 22. 响应式

- Compact：Today 单列；Calendar Day / Agenda 优先；Kanban 单列切换。
- Medium：Today 左 Timeline + 右 Unscheduled；Calendar Week 可用。
- Expanded：左子系统导航 / 中主内容 / 右 Context Pane；Calendar 可挂 Unscheduled Task Sidebar。
- Large：Project Kanban / Timeline 利用宽度，但单卡仍限制最大宽度。
