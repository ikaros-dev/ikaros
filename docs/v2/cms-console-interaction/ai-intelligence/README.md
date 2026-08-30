# AI 智能 — CMS Console 交互规格

## 1. AI 助手

**路由：** `/console/ai/assistant`

### 页面标题区
- 标题：`AI 助手`。
- Persona 选择器。
- Model/Provider 选择器：展示当前实际路由策略，不展示 Secret Credential。
- `新建会话`。

### 页面布局

桌面端三栏：左侧会话列表，中间会话 Canvas，右侧可折叠 Context Inspector。

会话列表字段：标题、Persona、最近使用的 Model/Provider、更新时间、置顶状态。搜索只在 AI 域内执行。

会话 Canvas：
- 消息气泡显示角色、时间；Assistant 消息显示 Model/Persona 来源信息；
- 支持 Markdown/Code 渲染；
- Attachment/Resource Citation Chip；
- 每条 Assistant 回复提供：复制、重新生成、分支、反馈、有权限时查看 Trace。

Composer：
- 多行输入框；
- 添加 Resource/Attachment；
- 上下文范围按钮；
- 有权限时允许 Model Override；
- 发送按钮；
- Streaming 时显示停止生成按钮。

交互：
- Streaming 回复在原位置持续增长，并提供停止按钮。
- 停止后保留已生成内容并标记 `已停止`。
- `重新生成` 创建兄弟版本/回复，不得静默覆盖原历史。
- `从此处分支` 从选中消息创建新会话。
- Resource Chip 打开对应 Resource 详情。

Context Inspector 分区：
- 已选择 Resource；
- 显式用户上下文；
- 已启用 Memory；
- Tool/Capability；
- 隐私分类。

发送前用户可以移除上下文。涉及高敏感数据时，在请求发送前必须允许用户检查最终上下文摘要。

## 2. 模型与提供方

**路由：** `/console/ai/models`

Tabs：`提供方`、`模型`、`路由`。

### Provider 表格

列：Provider 名称、类型、Endpoint/Region、是否启用、健康状态、Credential 状态（`已配置`、`缺失`、`无效`）、支持能力、最近检查、操作。

Provider 编辑器：
- 显示名称；
- Provider 类型；
- 支持时的 Base Endpoint；
- API Credential Secret 字段；
- 适用时的 Organization/Project 标识；
- Timeout/Retry 限制；
- 支持时的 Proxy/Network 选项；
- Enabled Switch。

`测试连接` 使用当前输入值执行，但响应和 UI 日志中绝不能出现 Secret。保存后只展示 Masked/Configured 状态。

### Model 表格

列：Model Alias、Provider、上游 Model ID、Modalities、Context Limit、Tool Support、Enabled、Cost Metadata、Default Role、操作。

只有在需要覆盖/补充 Provider 自动发现能力时，Model 编辑器才允许修改 Capability；否则发现结果只读。

### 路由

规则按优先级排序。规则字段：Request Class / Persona / Capability / Privacy Class → 首选 Model → Fallback。

支持拖拽调整优先级。`模拟路由` 输入一组虚拟请求属性，仅展示最终选择路径，不能把真实内容发送给 Provider。

## 3. Persona 管理

**路由：** `/console/ai/personas`

### 列表

Card/Table 字段：头像/图标、名称、描述、是否启用、默认模型策略、Memory 策略、Tool 策略、更新时间。

主操作：`新建 Persona`。

### Persona 编辑器

使用完整页面，Tabs：
- `身份`。
- `指令`。
- `模型`。
- `工具`。
- `记忆与上下文`。
- `安全与隐私`。
- `测试`。

身份：名称必填、头像/图标、简短描述、Tone Tag。

指令：结构化 System Behavior / Instruction 字段、版本指示、未保存修改保护。

模型：默认路由策略、允许使用的 Model Class、Fallback 行为。

工具：Capability 列表，显示启用/禁用、风险级别、权限原因。启用高敏感 Tool 时可要求 Admin Permission，并明确说明其数据访问后果。

记忆与上下文：Memory Scope、Retention、允许业务域、排除项。Private Notes、Password Manager、Finance 等敏感域默认禁止，除非产品明确设计且用户完成授权。

测试 Tab 提供 Sandbox 会话，并允许查看组装后的 Persona 配置。默认不得写入生产 Memory；只有用户明确选择 `将测试结果保存为记忆` 时才允许。

## 4. 上下文、隐私与记忆

**路由：** `/console/ai/privacy`

Tabs：`上下文策略`、`记忆`、`数据共享`。

### 上下文策略

Policy Card/Rule 定义“哪些数据域可以被哪些 Persona/Model/Provider 使用”。

每行字段：数据域、敏感级别、允许目标、是否要求显式同意、保留策略、启用状态。

规则编辑器必须用 Warning/Error 明确展示后果，例如：`私密笔记内容可能发送到 Provider X`。敏感规则修改根据策略要求重新认证或更高权限。

### Memory

表格：Memory Label/摘要、Scope、Owner、来源 Conversation、创建时间、最近使用、过期时间、状态。内容展示必须遵循敏感级别策略。

操作：查看、支持时编辑摘要、禁用、删除。删除确认需说明是否影响历史 Conversation。

`清除记忆` 必须可按 Persona/业务域/日期限定范围；批量删除要求输入确认文本。

### 数据共享

按 Provider 展示披露矩阵：哪些数据类别可能离开服务器、Retention/配置说明、Telemetry 状态，以及路由策略入口。该页面必须基于实际配置和 Policy 展示，不能写成营销宣传文案。

## 5. 作业、Trace 与用量

**路由：** `/console/ai/jobs`

Tabs：`作业`、`Trace`、`用量`。

### 作业

列：Job ID、类型、输入实体安全 Label、Persona/Model、状态、进度、创建时间、耗时、发起人、操作。失败行展示 Error Category 和是否可重试。

Job 详情 Timeline：排队 → 构建上下文 → Provider 请求/Tool Call → 后处理 → 持久化。每阶段显示耗时和状态。Prompt/内容默认隐藏，只有显式拥有 Trace Content 权限时才能查看。

### Trace

筛选：Trace ID、Conversation、Model/Provider、Tool、状态、日期。

列：Trace、Request Class、Model、Tool Count、Latency、Token/Input-Output Unit、状态、创建时间。

Trace 详情使用可展开 Event Tree，展示路由、Tool Call、Retry 和 Timing。Credential、Authorization Header 永久 Redact；Private Content 根据业务域 Policy Redact。

### 用量

周期选择器 + KPI：请求数、Input/Output Token 或 Provider Unit、预估成本、失败率、延迟。图表按 Model/Provider/Persona 分组。允许导出聚合用量元数据；导出 Prompt Content 必须是独立的特权操作。

## 通用规则
- AI 功能必须清晰区分“确定性系统动作”和“模型生成建议”。
- 所有 `应用建议` 操作必须先预览即将产生的具体数据修改，再执行。
- Provider/Model 故障时可以展示 Fallback Path，但绝不能静默切换到隐私策略禁止的数据目标。
- Prompt、Response、Tool、Trace Content 遵循敏感级别分类；内容被 Redact 时仍可展示允许公开的运维元数据。
