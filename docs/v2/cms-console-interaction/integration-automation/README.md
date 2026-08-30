# 集成与自动化 — CMS Console 交互规格

## 1. 自动化规则

**路由：** `/console/integration/automation`

### 页面标题区
- 标题：`自动化规则`。
- 主操作：`新建规则`。
- 支持时显示次操作：`导入规则`。
- Filter Chip：启用状态、Trigger 类型、Action 类型、Owner、最近执行结果。

### 规则表格

列：
- Enabled Switch；
- 名称；
- Trigger 摘要；
- Condition 摘要；
- Action 摘要；
- Owner；
- 最近运行状态/时间；
- 定时规则显示下次运行时间；
- 操作。

只有规则本身有效时，Enabled Switch 才允许直接保存。尝试启用无效规则时打开编辑器并显示 Validation Summary。

### 规则构建器

使用完整页面，纵向分为：
1. 名称与描述。
2. Trigger。
3. Condition。
4. Action。
5. Error/Retry Policy。
6. Test 与 Review。

Trigger Card 支持 Event、Schedule、Manual/API 和 Connector 专属 Trigger。选择某一种 Trigger 后只展示其 Schema 声明的配置字段。

Condition Builder 使用嵌套 AND/OR Group，并以“字段 / 操作符 / 值”组织。每个字段明确来源业务域和数据类型；类型不兼容的比较禁止保存。

Action List 有明确顺序并支持拖拽。每个 Action Card 展示 Connector/Capability、配置摘要、Retry 行为和删除操作。涉及敏感数据外发或高风险修改的 Action 显示 Warning Banner，说明数据去向和修改范围。

`测试规则` 尽可能针对选定/示例 Event 使用 Dry-run。测试结果展示命中的 Condition、渲染后的 Action Input 和是否产生 Side Effect。真正执行 Side Effect 的测试必须由用户显式选择。

每次保存形成新版本。已启用规则修改后再次 Publish/Enable 时，可展示与上一生效版本的 Diff Summary。

## 2. 执行记录与 Trace

**路由：** `/console/integration/executions`

### 表格

筛选：Rule、Connector、Status、Date、Event Type、Correlation/Trace ID。

列：Execution ID、Rule、Trigger/Event、Status、Start Time、Duration、Attempt Count、Action Count、Initiator/Source、操作。

状态统一：`排队中`、`运行中`、`成功`、`部分成功`、`失败`、`已取消`。

### Execution 详情

标题区展示 Execution ID、Rule Link、状态、开始时间/耗时，以及符合条件时的 `重试`。

Timeline/Tree：
- 接收 Trigger；
- Condition Evaluation；
- 各 Action；
- Retry/Backoff；
- 完成。

每个 Stage 可展开查看安全处理后的 Input/Output、耗时、Connector Response Metadata 和 Error Category。Credential、Authorization Header、受保护内容必须 Redact。

`重试失败操作` 必须明确成功 Action 是否会再次执行。执行模型支持时默认只重试安全且失败的部分；无法做到时必须在执行前让用户审阅重复执行影响。

## 3. 事件与失败队列

**路由：** `/console/integration/events`

Tabs：`事件流`、`失败/死信队列`。

### 事件流

筛选：Event Type、Source Subsystem、Entity Type、Correlation ID、Date、Delivery State。

列：时间、Event Type、Source、Entity Safe Identifier、Correlation ID、Subscriber/Delivery Summary、Status。

Event 详情根据 Permission/Redaction Policy 展示 Header/Metadata 和 Payload。

只有明确 Replay-safe 的 Event Type 才显示 `Replay`。点击 Replay 必须先打开 Review Dialog，列出可能再次触发的 Subscriber/Action。

### 失败队列

列：Failure ID、Event/Action、Destination、Reason Category、Attempts、First/Last Failure、Next Retry、State、操作。

操作：`立即重试`、`重新安排`、`查看`、`丢弃`。

丢弃必须确认并记录原因。批量重试前展示最大处理数量和 Rate Warning。

## 4. 导入与同步

**路由：** `/console/integration/sync`

Tabs：`来源`、`同步任务`、`导入任务`、`冲突`。

### 来源

Card/Table 字段：来源名称、Connector/Provider、Data Domain、Account/Endpoint Safe Label、Enabled、最近同步、下次同步、健康状态、操作。

Source Editor 根据 Connector Schema 动态渲染：Endpoint、Identity、Secret Credential、Scope、Schedule、Conflict Policy、映射的 Collection/Tag。条件允许时，在启用之前要求先执行 `测试连接`。

### 同步任务

列：Job ID、Source、Mode（Incremental/Full）、State、Discovered、Created、Updated、Skipped、Conflicts、Failures、Started/Duration。

Job Detail 展示阶段进度和逐实体结果 Table。只有 Backend 明确可以安全停止时才显示 `取消`。

### 冲突

行字段：Target Resource/Metadata Field、本地/人工值及来源、Incoming Source Value、Policy、Detected Time。

解决操作：`保留本地`、`采用来源`、`合并/编辑`、`修改规则`。人工/用户拥有的元数据必须在视觉上具有更高优先级，并且绝不能被静默覆盖。

### 导入任务

向导可以根据业务域扩展，但必须始终包含：Source Selection、Preview、Mapping、Validation、Duplicate Policy、Review、异步 Commit。

## 5. 插件与连接器

**路由：** `/console/integration/plugins`

Tabs：`已安装`、`连接器`，以及支持时的 `市场/可用插件`。

### 已安装插件

Card/Table 字段：名称、图标、版本、Publisher/Source、Enabled、Capability、Permission、Health、Update State、操作。

Plugin Detail Tabs：`概览`、`配置`、`权限`、`路由/能力`、`日志`、`更新`。

配置页根据插件声明的 Schema 使用标准 M3 Form Field 渲染。Secret Field 始终 Masked。不能把未知或不受信任 HTML 当作配置 UI 直接渲染。

权限页列出插件请求的 Capability/Data Domain，并展示 Risk Description。扩大权限或启用高风险插件可要求 Admin Confirmation/Re-authentication。

禁用 Dialog 必须说明哪些 Route、Job、Automation、Metadata Source 会停止工作。

卸载前预览依赖 Rule/Config/Data，并在支持时提供 Export。

### 连接器

Connector Instance 表格：名称、Plugin/Provider、Account/Endpoint、Scope、Enabled、Health、Last Activity、操作。Backend 支持时允许同一个 Provider 存在多个 Instance。

## 通用规则
- 所有外部数据传输在相关场景下明确显示 Destination Identity 和 Sensitivity Consequence。
- Secret 保存后永不展示明文。
- Execution/Event 页面必须区分“技术重试”和“业务 Replay”。
- Rule、Plugin、Connector 的修改产生 Audit Event。
- Plugin 不能仅凭自己声明 Menu 就绕过权限显示导航项；当前用户仍必须具有对应 Capability。
