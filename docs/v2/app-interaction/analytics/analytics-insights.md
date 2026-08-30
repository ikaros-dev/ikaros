# Analytics：个人数据洞察

## 1. App 边界

App 的 Analytics 重点是“用户理解自己的内容、消费、创作、计划与本机存储使用”。

系统级 Metric Catalog 管理、全局 Security Analytics、Operations 历史、系统报表重建属于 CMS。

普通用户不得因为能看自己的 Insights 就获得系统管理员指标。

---

## 2. 页面目录

- Insights Overview。
- Content Insights。
- Media Consumption。
- Productivity Insights。
- Creation Insights。
- Local / Personal Storage Insights。
- Metric Detail。
- AI Explanation。

Finance 的专业分析主要留在 Finance 子系统，但 Overview 可显示用户启用的安全摘要卡。

---

## 3. 全局筛选栏

所有 Insights 页顶部统一：

- Time Range：7D / 30D / 3M / 1Y / Custom。
- Compare：Previous Period / Previous Year / Off。
- Time Grain：Day / Week / Month（根据范围动态）。
- Optional Dimension Filter。

用户时区用于 Daily / Weekly 归属。

Filter 改变后所有当前页面卡片同步刷新，并在 Header 显示实际日期范围。

---

## 4. Overview

### 4.1 Compact 顺序

1. 本周期摘要。
2. 内容增长。
3. 消费时间。
4. Task / Focus。
5. Creation。
6. Storage / Download。
7. AI Summary（可选）。

### 4.2 摘要卡

示例：

- 新增 Resource `+42`。
- 观看 `8h 20m`。
- 阅读 `4h 10m`。
- Focus `6h 40m`。

每项显示 Comparison Arrow + 绝对差异 / 比例，但点击可打开 Metric Definition。

不要把这些指标合成“个人效率 83 分”。

---

## 5. 图表通用组件

每张 Chart Card 包含：

- Metric Name。
- 当前值。
- Comparison。
- Chart。
- Info Icon。
- More：View Data / Export / Explain with AI。

### Accessibility

Info / More → `查看数据表`，按时间列出数值。

图表不只靠颜色区分 Series；Legend 使用名称 + Marker。

---

## 6. Metric Definition Sheet

点击 Info：

- Metric Name。
- Description。
- Unit。
- Time Semantics。
- Aggregation。
- Supported Dimensions。
- Data Source Summary。
- Version。

让用户能理解“完成率”等指标的口径。

---

## 7. Content Insights

Cards：

- Resource Total Trend。
- New Resource Count。
- Resource Type Distribution。
- Archived / Trash Count。
- Collection Count。
- Tag Usage。
- Metadata Completion（有正式 Metric 时）。
- External Identity Coverage。

Resource Type Distribution 点击类型跳转 Library Filtered View。

---

## 8. Media Consumption

### Summary

- Watch Duration。
- Read Duration。
- Listen Duration。
- Completed Episodes / Movies / Chapters。

### Charts

- Daily Consumption Trend。
- Time by Resource Type。
- Completion Rate。
- Continue Watching / Reading Count。
- Most Accessed Resources。

Most Accessed Resource Card：Cover、Title、Duration / Count、Open Resource。

“打开播放器”不直接等于完成，页面只使用服务端正式 Metric。

---

## 9. Productivity Insights

Cards / Charts：

- Completed Task Count。
- Completion Rate。
- Overdue Count / Rate。
- Planned Duration。
- Actual Focus Duration。
- Estimate Accuracy。
- Time by Project。
- Time by Goal。
- Habit Completion / Streak。
- Goal Progress Trend。
- At Risk Goal Count。

明确页脚：`这些指标用于帮助你理解行为，不代表个人能力或价值评分。`

---

## 10. Creation Insights

- New Documents。
- Published Articles。
- Revision Count。
- Creation / Focus Time。
- Draft Count。
- Word Count Trend（内容类型支持时）。
- Collaboration Sessions。
- Comments / Annotations。

点击 Draft Count 跳 Document Draft Filter。

---

## 11. Personal Storage Insights

普通 App 只展示与当前用户 / 本设备相关的用户语义：

- My Downloads Size。
- Client Cache Size。
- Offline Resource Count。
- Cache Hit Rate（服务端/客户端有正式用户级 Metric 时）。
- Download Network Bytes（可用时）。

不能把服务器 Hot / Warm / Archive 全局成本治理页直接放进普通 App。

卡片底部快捷入口 `管理本机空间`。

---

## 12. Finance Summary

默认关闭，可由用户设置开启。

只显示：

- 本月 Income / Expense。
- Budget Usage。
- Net Worth Trend。

金额受 Finance 的 `隐藏金额` 设置影响。

AI 总结是否可访问交易数据由 AI Data Policy 单独控制。

---

## 13. AI Explanation

Chart More → `AI 解释`。

Side Sheet / Bottom Sheet：

- Metric Name / Current Value / Comparison 作为结构化输入。
- 允许 AI 使用的 Related Metrics 列表。
- Provider / Privacy 提示。
- Generate。

输出必须：

- 明确是解释 / 推断。
- 不创造不存在的数据。
- 显示使用的 Metrics。
- 可打开每个 Metric Detail。

---

## 14. Natural Language Query

AI 可用时 Insights App Bar 提供 `问数据`：

例如：`为什么这个月阅读时间下降？`

系统先解析为 Metric / Dimension Query，展示查询口径，再生成解释。

若数据不足，AI 应显示“不足以判断”，不能补造原因。

---

## 15. Privacy

- Activity 可删除时，相应可重建统计按产品规则更新。
- Private Notes 只进入最小 Count / Bytes / Sync Rate 等允许指标，不分析标题、标签、主题。
- Password Manager Secret 不进入普通 Analytics。
- Security / Login 管理指标只对授权管理员开放，普通 App 不显示。

---

## 16. Empty / Data Delay

### No Data

显示：`这个时间范围还没有足够数据` + 调整范围。

### Processing Delay

Analytics 最终一致性时：`数据更新至 10:20`。

不要把短暂聚合延迟显示成业务对象状态错误。

---

## 17. 响应式

- Compact：单列 Chart Card，高度 220–300dp。
- Medium：2 列。
- Expanded：12 列 Grid，关键趋势 8 列、摘要 4 列。
- Large：图表最大宽度受控；右侧可固定 Metric Detail / AI Explanation。
