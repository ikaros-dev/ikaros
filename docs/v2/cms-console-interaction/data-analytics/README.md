# 数据分析 — CMS Console 交互规格

## 1. 个人概览

**路由：** `/console/analytics`

### 页面标题区
- 标题：`个人数据概览`。
- 周期选择器预设：7 天、30 天、90 天、本年、自定义。
- 对比 Switch：支持时可选择上一周期 / 去年同期。
- 次操作：`导出报表`。

### KPI 卡片

默认卡片：
- 新增 Resource；
- 消费时长/进度事件；
- 已完成任务；
- 专注时长；
- 存储增长；
- 自动化/同步活动。

每张卡显示当前值、对比变化值和可访问的文字解释。点击卡片跳转到对应分析页面，并继承当前周期条件。

### 图表
- 活动趋势。
- 内容类型分布。
- 完成/进度趋势。
- 存储增长。

图表支持 Legend 开关、Hover/Focus 数值和 Data Table 替代。隐藏 Series 默认只对当前视图生效，除非用户显式保存 Dashboard 偏好。

## 2. 内容分析

**路由：** `/console/analytics/content`

筛选：周期、Resource 类型、Collection、Provider/Source、Tag、生命周期、Owner。

KPI：新增资源、完成资源、增加收藏、平均进度、最活跃 Collection。

区域：
- Resource 创建趋势；
- 按类型统计消费；
- 活动量最高 Resource；
- 元数据来源/冲突趋势；
- Collection 增长。

Top Resource 表格列：Resource、类型、打开/查看或业务活动次数、进度/完成度、最近活动、收藏状态。点击行进入“内容与创作”详情。

## 3. 存储分析

**路由：** `/console/analytics/storage`

KPI：持久化字节数、Attachment 逻辑字节数、去重节省空间、Cache 字节数、Archive 字节数、完整性失败数。其中 Cache 必须与持久化存储在视觉和语义上明确区分。

图表：
- 按 Tier 展示持久化字节变化；
- 按 Backend 展示字节占用；
- Attachment MIME/类型分布；
- 归档/恢复吞吐；
- Cache 命中/Eviction 趋势。

表格：
- 按逻辑 Attachment 大小排序的最大 Resource；
- 最大 Blob；
- Backend 使用率；
- 完整性事故。

点击 Backend/Blob 跳转到“附件与存储”对应详情。

## 4. 效率分析

**路由：** `/console/analytics/planning`

KPI：计划任务数、完成率、逾期率、计划时长、实际/专注时长、习惯完成率。

图表：
- 已完成任务趋势；
- 计划 vs 实际时长；
- Project 投入分布；
- Deadline 达成情况；
- Focus Session 分布。

支持按 Project、Task Tag、Goal、Period 筛选。`查看原始任务` 跳转到效率与计划页面，并继承对应筛选。

## 5. 系统历史

**路由：** `/console/analytics/system`

该页面展示运维历史指标，不展示安全审计事件。

KPI：可用率、采集时的请求量、错误率、后台任务吞吐、同步成功率、存储健康事故。

图表：可用时展示 CPU、Memory、Request Latency、Job Duration、Queue Depth、DB/Storage Latency、Connector Availability。

指标采集缺口必须显示为 `无数据` Gap，不能当作数值 0。

## 6. 指标目录

**路由：** `/console/analytics/metrics`

### 表格列
- Metric Key；
- 显示名称；
- 业务域；
- 类型：Counter/Gauge/Histogram/Derived；
- 单位；
- Dimension；
- Retention；
- 状态；
- 最近 Sample；
- 操作。

Metric 详情展示：定义、Derived Metric 的公式、来源 Event/Table、Aggregation Window、Dimension/Cardinality 警告、Retention Policy、示例可视化。

`预览` 打开所选周期的 Time Series。系统内置 Metric Definition 只读；支持自定义 Derived Metric 时使用带校验和 Sample Evaluation 的公式编辑器。

## 7. 报表与重建

**路由：** `/console/analytics/reports`

Tabs：`已保存报表`、`定时报表`、`重建任务`。

### 已保存报表

列：名称、Owner、Scope、周期规则、Widget/Metric 数量、更新时间、操作。

报表编辑器：
- 名称；
- 描述；
- 默认周期；
- 筛选；
- Widget 列表，每项包含 Metric、Visualization、Grouping、Ordering；
- 预览。

拖拽调整 Widget 顺序。Metric/Filter 组合无效时在保存前显示 Inline Error。

### 定时报表

列：报表、周期、Recipient/Destination、下次运行、上次运行、状态。

编辑器配置 Schedule、Timezone、输出格式、Delivery Target。除非报表 Policy 明确允许，不能添加 Private Domain Data。

### 重建任务

用于配置/Schema 修改后重建 Aggregate/Materialized Statistics。

列：Job ID、Scope、时间范围、状态、进度、已处理 Row/Event、开始时间、耗时、发起人。

`开始重建` Dialog 要求选择范围和日期，并尽可能估算影响。重建异步执行，不能把现有分析页面清空；受影响 Dashboard 显示 `正在重建 — 数据可能暂时过期` Banner。

## 通用分析规则
- 解释结果依赖时区/聚合粒度时必须明确展示。
- 对比变化需要区分“百分点”和“百分比变化”。
- 无数据、因隐私被隐藏的数据、真实的 0 三者必须视觉和文字上明确区分。
- Aggregation 必须遵守当前用户权限；管理员/跨用户分析必须明确说明统计 Scope。
- 导出文件元数据中包含当前筛选、周期、时区和生成时间。
