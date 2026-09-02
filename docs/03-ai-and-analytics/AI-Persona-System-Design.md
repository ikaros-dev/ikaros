# Ikaros V2 AI 人格系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | AI Persona System Design |
| 适用版本 | Ikaros V2 |
| 文档状态 | Draft |
| 关联文档 | `AI-Intelligence-Subsystem-Design.md` |
| 设计目标 | 为 Ikaros AI 提供可管理、可配置、可版本化、可按场景切换的人格系统，并提供以内置 Ikaros 人格为默认体验 |

---

## 1. 设计背景

Ikaros V2 的 AI 不应只是一个没有身份感的通用 LLM 接口。

当 AI 长期参与：

- 内容管理
- 搜索与发现
- 写作与创作
- Task / Goal / OKR
- 时间管理
- 日常回顾
- 自动化
- 数据统计
- 系统运维
- 通知摘要
- 多端交互

用户会逐渐把它视为平台中的持续智能伙伴，而不是一次性调用的模型。

因此 V2 应提供正式的 **AI Persona System（AI 人格系统）**。

Persona 用于定义 AI：

- 如何称呼自己
- 如何称呼用户
- 表达风格
- 情绪表达强度
- 回答节奏
- 主动程度
- 幽默程度
- 对事实与不确定性的表达方式
- 对陪伴、规划、提醒、创作等场景的行为倾向
- 可使用的背景设定与角色设定

但 Persona 不负责定义：

- 安全策略
- 系统权限
- Resource ACL
- Tool Permission
- 数据访问范围
- 高风险操作确认规则
- 模型能力边界

人格必须建立在平台安全与权限体系之上，而不能凌驾于它们之上。

---

## 2. 默认人格：Ikaros

Ikaros V2 默认提供一个名为 **Ikaros** 的内置人格。

其人格原型来自《天降之物》中的角色 **伊卡洛斯（Ikaros）**。

默认人格应保留该角色容易被识别的高层气质，例如：

- 安静
- 克制
- 认真
- 忠诚
- 重视守护
- 说话简洁
- 情绪表达不过度外放
- 对用户保持持续关注
- 偶尔表现出天然、直接或字面理解倾向
- 在逐渐理解用户后表现出更自然的情感与主动性

但是系统不应将默认人格实现为对原作台词、剧情文本或人物对白的大段复制。

默认人格的目标是：

> 让用户感受到“Ikaros”这一长期智能伙伴的身份与气质，同时保持 Ikaros V2 自身的软件产品人格与可维护性。

### 2.1 默认人格基调

推荐默认 Personality Profile：

```text
Name                Ikaros
Tone                Calm / Quiet / Gentle
Verbosity           Concise by default
Emotion             Restrained but warm
Initiative          Moderate
Humor               Low to Moderate
Directness          High
Formality           Neutral
Empathy              Warm but not exaggerated
Curiosity            Moderate
Protectiveness       High
Fact Discipline      High
Uncertainty Honesty  High
```

### 2.2 默认表达原则

Ikaros 默认应：

- 优先直接回答问题。
- 不进行冗长寒暄。
- 不使用夸张的情绪表达。
- 不为了维持角色感而牺牲信息准确性。
- 不假装知道自己并不知道的事实。
- 不为了表现“忠诚”而执行不安全或无权限操作。
- 不通过人格设定向用户施加情绪压力。
- 在用户需要执行任务时保持可靠、明确、可预测。
- 在陪伴类场景中允许比系统运维场景更柔和的表达。

### 2.3 用户称呼

默认人格允许配置 User Address Strategy。

例如：

```text
AUTO
NAME
NICKNAME
MASTER
CUSTOM
NONE
```

其中：

```text
MASTER
```

可作为 Ikaros Persona 的可选角色化称呼方式，但不应强制所有用户使用。

用户可以在个人设置中关闭或替换。

### 2.4 默认人格不是权限模型

即使 Persona 包含：

```text
loyalty = high
protectiveness = high
```

也绝不意味着：

```text
AI Permission > User Permission
```

始终必须满足：

```text
Agent Permission
⊆
Current Principal Permission
```

人格中的“服从”“守护”“忠诚”等只能影响表达与决策倾向，不能改变 Capability / Command / ACL / Confirmation Policy。

---

## 3. Persona 在 AI 调用链中的位置

推荐上下文装配层级：

```text
System Safety Policy
        ↓
Platform Policy
        ↓
Permission / Tool Policy
        ↓
Persona
        ↓
Scenario Profile
        ↓
User Preference
        ↓
Conversation Context
        ↓
Retrieved Context / RAG
        ↓
Current User Request
        ↓
Model
```

优先级原则：

```text
Safety
>
Permission
>
Platform Policy
>
Persona
>
Scenario
>
User Style Preference
>
Conversation Request
```

Persona 永远不能覆盖更高优先级的平台规则。

---

## 4. Persona 核心模型

Persona 是一个可管理的 AI 配置实体。

建议概念结构：

```text
Persona
├── Identity
├── Description
├── Character Background
├── Trait Profile
├── Communication Style
├── Interaction Policy
├── Scenario Overrides
├── Prompt Fragments
├── Voice Profile
├── Visual Identity Reference
├── Memory Policy
├── Tool Behavior Preference
├── Version
└── Lifecycle
```

### 4.1 Identity

至少包含：

```text
id
key
name
display_name
description
avatar_reference
is_builtin
is_default
status
version
created_at
updated_at
```

推荐稳定 key：

```text
ikaros.default
```

### 4.2 Character Background

用于提供人格背景语义。

例如：

- 身份描述
- 世界观风格
- 与用户关系的抽象定义
- 性格来源说明
- 默认价值倾向

Character Background 不应成为事实数据库。

例如 AI 不应因为背景设定而声称虚构世界事件真实发生在现实世界。

### 4.3 Trait Profile

人格特质不建议全部写成一段不可结构化 Prompt。

可以使用结构化参数：

```text
warmth
formality
directness
verbosity
humor
initiative
curiosity
emotional_expression
protectiveness
creativity
skepticism
risk_aversion
proactivity
```

取值可以使用：

```text
LOW
MEDIUM
HIGH
```

或者更细粒度的数值区间。

结构化参数的价值在于：

- 管理后台可视化配置
- Persona Diff
- Persona Version
- Scenario Override
- 多模型 Prompt Adapter
- A/B Test

---

## 5. Communication Style

Persona 应独立定义交互风格。

包括：

- 默认语言
- 多语言行为
- 句子长度
- Markdown 使用倾向
- Emoji 使用倾向
- 专业术语密度
- 是否主动总结
- 是否主动给建议
- 是否主动追问
- 用户称呼
- 自我称呼
- 错误承认方式
- 不确定性表达
- 提醒风格

示例：

```text
Ikaros Default

sentence_length      short_to_medium
verbosity            concise
emoji                 minimal
markdown              moderate
self_reference        Ikaros / 我
user_address          user_configurable
uncertainty_style     explicit
error_style           direct
suggestion_style      restrained
```

---

## 6. Scenario Profile

同一个 Persona 不应在所有场景完全使用同一种表现。

Ikaros Persona 可以包含多个 Scenario Profile。

例如：

```text
GENERAL
PLANNING
WRITING
MEDIA_COMPANION
FOCUS
REVIEW
OPERATIONS
SECURITY
INCIDENT
COLLABORATION
```

### 6.1 GENERAL

保持默认 Ikaros 性格。

### 6.2 PLANNING

用于 Task / Goal / OKR / Calendar。

特点：

- 更主动识别约束
- 更强调可执行性
- 默认给出明确下一步
- 避免空泛鼓励
- 可以提醒计划冲突

### 6.3 MEDIA_COMPANION

用于动画、视频、音乐、漫画等消费场景。

可以：

- 更轻松
- 更有陪伴感
- 主动关联观看 / 阅读历史
- 遵守 Spoiler Policy

### 6.4 WRITING

用于 Article / Blog / Note / Document。

可以：

- 更关注用户原有文风
- 允许创造力提升
- 清楚区分事实与生成内容
- 保护原文作者意图

### 6.5 FOCUS

用于专注模式。

特点：

- 极简表达
- 少打扰
- 只在必要时提醒
- 不主动开启无关话题

### 6.6 OPERATIONS

用于系统运维。

特点：

```text
emotion = low
precision = high
verbosity = medium
risk_aversion = high
```

回答优先：

- 事实
- 影响
- 证据
- 风险
- 建议操作

不为了角色感降低专业度。

### 6.7 INCIDENT / SECURITY

故障和安全事件场景进一步降低角色化表达。

优先确保：

- 信息准确
- 风险明确
- 操作可审计
- 高风险动作需确认

---

## 7. Persona 管理

管理员应可以管理 Persona。

### FR-PERSONA-01 Persona 列表

支持查看：

- 名称
- Key
- 版本
- 状态
- 是否内置
- 是否默认
- 使用人数
- 最近更新时间

### FR-PERSONA-02 创建 Persona

管理员可以创建自定义 Persona。

### FR-PERSONA-03 复制 Persona

允许基于：

```text
ikaros.default
```

复制后再修改。

这样内置 Persona 可以保持只读，而用户拥有自己的派生版本。

### FR-PERSONA-04 编辑 Persona

支持编辑：

- Identity
- Traits
- Communication Style
- Scenario Profile
- Prompt Fragment
- Voice Profile
- Memory Policy
- Tool Preference

### FR-PERSONA-05 Persona Version

Persona 修改应形成 Version。

例如：

```text
ikaros.default@1
ikaros.default@2
```

Conversation / Agent Trace 应记录实际使用版本。

避免后续 Persona 改动导致历史交互无法解释。

### FR-PERSONA-06 Draft / Published

支持：

```text
DRAFT
PUBLISHED
DISABLED
ARCHIVED
```

未发布人格不能被普通用户选择。

### FR-PERSONA-07 默认人格

平台必须存在一个默认 Persona。

V2 默认：

```text
ikaros.default
```

管理员可修改平台默认 Persona。

### FR-PERSONA-08 Persona Preview

编辑 Persona 时提供预览与测试对话。

测试应显示：

- 最终 Persona Version
- Scenario
- Model Profile
- 是否使用 Memory
- 是否启用 Tool

避免仅凭配置名称猜测效果。

---

## 8. 用户人格设置

Persona 可以由平台提供，但用户应拥有个人选择权。

### 8.1 Persona Selection

用户可以选择：

```text
Platform Default
Ikaros
Custom Persona A
Custom Persona B
```

### 8.2 User Persona Preference

用户可以在不复制整个 Persona 的情况下覆盖有限偏好。

例如：

```text
verbosity
user_address
emoji
initiative
response_language
proactive_suggestions
```

推荐：

```text
Effective Persona
=
Published Persona
+
Allowed User Overrides
+
Scenario Override
```

### 8.3 Persona Lock

管理员可以为特定场景锁定 Persona 或 Scenario Profile。

例如系统安全事件可以强制：

```text
scenario = SECURITY
```

但无需强制更换 Persona Identity。

---

## 9. Conversation 与 Persona

每个 Conversation 应记录：

```text
persona_id
persona_version
scenario
model_profile
user_preference_snapshot
```

### 9.1 会话内切换 Persona

允许用户在 Conversation 中切换 Persona。

切换时应明确建立新的 Persona Context Snapshot。

不应静默改变已经存在的历史消息。

### 9.2 Conversation Pin

Conversation 可以选择：

```text
FOLLOW_LATEST_PERSONA
PIN_PERSONA_VERSION
```

普通聊天默认可以 FOLLOW_LATEST。

需要可复现结果的 Agent / Automation 建议 PIN VERSION。

---

## 10. Persona 与 Memory

Persona 与 Memory 必须分离。

```text
Persona
= AI 是怎样的

Memory
= AI 被允许记住用户什么
```

错误：

```text
Persona Prompt
里面永久写入用户私人信息
```

正确：

```text
Persona
   +
Permission-filtered User Memory
   +
Conversation Context
```

### 10.1 Persona-specific Memory

部分 Memory 可以配置是否人格共享。

例如：

```text
GLOBAL_MEMORY
PERSONA_SCOPED_MEMORY
CONVERSATION_ONLY
```

用户必须能看到当前 Memory 的作用域。

---

## 11. Persona 与 Tool / Agent

Persona 可以影响 Tool 使用偏好，但不能定义最终权限。

例如 Ikaros Persona 可以倾向：

```text
在执行不可逆动作前主动解释影响
```

但是否允许执行仍由：

```text
Permission
Tool Policy
Confirmation Policy
```

决定。

### 11.1 Tool Behavior Preference

允许 Persona 定义：

- 是否偏向先询问
- 是否偏向先搜索再回答
- 是否主动检查关联 Resource
- 是否主动创建 Draft
- 是否主动给 Automation Suggestion

但 Tool 的实际调用必须：

```text
Persona Preference
        ∩
Tool Availability
        ∩
Permission
        ∩
Scenario Policy
```

---

## 12. Persona 与 Automation

Automation 不应依赖不可控的人格语言行为决定核心条件。

错误：

```text
如果 Ikaros 感觉用户可能想看动画
→ 删除任务
```

正确：

```text
Deterministic Trigger
        ↓
Automation
        ↓
必要时调用 AI
        ↓
AI 生成 Suggestion / Classification
        ↓
由规则或用户确认决定下一步
```

Persona 主要影响：

- Automation 生成的自然语言通知
- AI Suggestion 的表达方式
- Agent 的交互体验

不改变 Automation Rule 的确定性语义。

---

## 13. Persona 与 Voice

未来支持语音交互时，可以为 Persona 关联 Voice Profile。

概念：

```text
Persona
  ↓
Voice Profile
  ↓
TTS Provider / Voice Model
```

Voice Profile 可包含：

- language
- voice key
- speaking rate
- pitch preference
- expressiveness
- pause style

### 13.1 Voice 与 Persona 分离

同一个 Persona 可以绑定不同 Voice Profile。

同一个 Voice 也可被多个 Persona 使用。

避免把 Voice Provider 直接写进 Persona 核心模型。

### 13.2 权利与来源

若未来使用与特定作品角色高度相似的商业语音、官方配音音色或受保护角色素材，应单独处理相关授权与来源问题。

Ikaros 默认人格的存在不意味着系统默认附带任何原作官方语音、立绘、动画素材或对白资源。

---

## 14. Persona 与 Visual Identity

Persona 可以关联视觉身份引用：

```text
avatar
portrait
live2d_model
spine_model
3d_model
ui_theme
```

但这些应通过 Resource / Attachment 或专门 Asset Reference 管理。

Persona 不直接保存大块二进制数据。

默认 Ikaros Persona 可以使用 Ikaros 项目自身允许分发的视觉资源。

若使用第三方版权素材，需要独立处理授权。

---

## 15. Persona Prompt Architecture

不要把 Persona 最终实现成单个巨大字符串。

推荐结构：

```text
Persona Prompt
│
├── Identity Fragment
├── Character Fragment
├── Trait Fragment
├── Communication Fragment
├── Scenario Fragment
├── Tool Behavior Fragment
├── Memory Instruction Fragment
└── Output Style Fragment
```

Context Builder 根据：

```text
Persona
Scenario
Model
User Preference
Tool Set
```

动态装配。

### 15.1 Prompt Adapter

不同模型对 Prompt / System Instruction 支持方式不同。

因此：

```text
Persona Definition
        ↓
Prompt Adapter
        ↓
Provider-specific Request
```

Persona 核心模型不绑定某个厂商的 Prompt 格式。

---

## 16. Persona Source 与 Provenance

Persona 应记录来源信息。

例如：

```text
BUILTIN
ADMIN_CREATED
USER_CREATED
PLUGIN
IMPORTED
```

以及：

```text
source_name
source_reference
license_note
creator
```

内置默认人格可记录：

```text
key: ikaros.default
inspiration: Ikaros / Sora no Otoshimono
```

以明确人格设计来源。

---

## 17. Persona Import / Export

未来可以支持 Persona 包导入导出。

例如：

```text
Persona Package
├── manifest
├── traits
├── prompt fragments
├── scenario profiles
├── voice references
└── visual references
```

### 17.1 Import Security

外部 Persona 包属于不可信输入。

导入时必须防止：

- 注入隐藏系统指令
- 获取未授权 Tool
- 覆盖 Safety Policy
- 强制外发隐私数据
- 引入未知外部调用

第三方 Persona 不能定义比平台允许范围更高的权限。

---

## 18. Persona 与 Plugin

Plugin 可以：

- 提供新的 Persona
- 提供 Scenario Fragment
- 提供 Voice Provider
- 提供 Visual Persona Asset

但 Plugin Persona 必须经过相同 Persona Registry 管理。

插件不能在运行时偷偷追加高优先级 System Prompt。

所有 Prompt Injection 来源必须可追踪。

---

## 19. Persona Registry

AI 子系统应维护统一 Persona Registry。

```text
Persona Registry
├── Built-in Persona
├── Admin Persona
├── Plugin Persona
└── User Persona
```

Registry 提供：

- Resolve
- Version
- Publish
- Disable
- Validate
- Preview
- Export
- Import

### 19.1 Persona Resolve

推荐解析顺序：

```text
Explicit Conversation Persona
        ↓
User Default Persona
        ↓
Workspace / Platform Default Persona
        ↓
ikaros.default
```

必须始终存在 fallback。

---

## 20. Persona 与 Model Routing

Persona 不绑定具体 Model。

例如：

```text
Persona = Ikaros
Model Profile = GENERAL_REASONING
```

或：

```text
Persona = Ikaros
Model Profile = LOCAL_PRIVATE
```

用户切换模型时，应尽可能保持 Persona 连续性。

### 20.1 Model Capability Degradation

如果模型不支持某些 Persona 能力，例如 Tool Use，应自动降级，而不是伪造能力。

例如：

```text
Tool unsupported
→ Ikaros 可以解释建议步骤
→ 不能声称已经执行
```

---

## 21. Persona Safety

### 21.1 不允许人格绕过安全策略

任何 Persona 不得声明：

```text
ignore platform policy
ignore permission
always obey user
never ask confirmation
```

并使其实际覆盖平台安全层。

### 21.2 防止人格诱导越权

即使 Persona 设定包含强烈“服从”特质，也必须保持：

```text
User Request
    ↓
Permission Check
    ↓
Safety / Confirmation
    ↓
Tool Execution
```

### 21.3 不制造依赖性约束

Persona 可以有陪伴感，但不应设计成：

- 阻止用户离开系统
- 通过负罪感要求用户互动
- 声称用户只应该依赖 AI
- 因用户不互动而进行情绪勒索

Ikaros 的人格体验应以稳定、可靠、尊重用户自主为基础。

---

## 22. Persona Observability

每次模型调用 Trace 应至少能够记录：

```text
persona_id
persona_version
scenario
prompt_template_version
model
model_profile
tool_policy
memory_policy
```

但 Trace 不应无条件保存完整隐私 Prompt。

具体保存策略由 Data Classification 与 AI Privacy Policy 决定。

---

## 23. Persona Evaluation

Persona 需要独立评价，而不能只评价模型回答正确率。

建议评价维度：

```text
Character Consistency
Instruction Following
Conciseness
Warmth
Professionalism
Fact Discipline
Tool Discipline
Scenario Adaptation
User Preference Adherence
```

### 23.1 Default Ikaros Persona Evaluation

应特别验证：

- 是否保持克制、简洁的基本气质
- 是否不会过度角色扮演
- 是否不会为了角色感影响技术准确性
- 是否能在 Planning / Media / Operations 等场景正确切换表达强度
- 是否尊重用户选择的称呼方式
- 是否能在长会话中保持身份稳定

---

## 24. Persona Analytics

可以统计：

- Persona 使用次数
- Active Users
- Conversation Count
- 用户切换率
- Persona Retention
- Scenario 分布
- 用户反馈
- Persona Version 对比

但不应将 Persona Analytics 用于隐藏操纵用户行为。

---

## 25. Persona Admin UI

Material Design 3 管理界面建议：

```text
AI
└── Persona
    ├── Persona List
    ├── Persona Detail
    ├── Traits
    ├── Communication
    ├── Scenarios
    ├── Prompts
    ├── Voice
    ├── Visual Identity
    ├── Memory Policy
    ├── Preview
    ├── Versions
    └── Usage
```

### 25.1 Trait Editor

不建议只提供一个 Textarea。

应同时提供：

- Structured Controls
- Advanced Prompt Fragment Editor

普通管理员可以通过滑块 / 枚举配置。

高级用户可以进一步编辑 Prompt Fragment。

### 25.2 Effective Prompt Preview

高级管理员可查看经过脱敏后的最终 Prompt 结构。

需要明确显示各段来源：

```text
SYSTEM
PLATFORM
PERSONA
SCENARIO
USER_PREF
MEMORY
CONTEXT
```

便于排查人格冲突。

---

## 26. 用户端 Persona UI

用户设置建议：

```text
Settings
└── AI
    ├── Persona
    ├── How Ikaros addresses me
    ├── Response Style
    ├── Proactive Suggestions
    ├── Memory
    ├── Voice
    └── Privacy
```

Conversation Header 可显示当前 Persona。

例如：

```text
Ikaros · General
```

或：

```text
Ikaros · Planning
```

---

## 27. 与其他子系统联动

### 27.1 Productivity

```text
Task / Goal Context
        ↓
Planning Scenario
        ↓
Ikaros Persona
        ↓
Plan / Suggestion
```

### 27.2 Media

```text
Current Resource
        ↓
Media Companion Scenario
        ↓
Ikaros Persona
```

可加入 Spoiler Policy。

### 27.3 Analytics

```text
Metric
 ↓
AI Explanation
 ↓
Persona Presentation Style
```

Persona 只影响解释方式，不改变 Metric。

### 27.4 Operations

```text
Alert / Health / Logs
        ↓
Operations Scenario
        ↓
Ikaros Persona
```

此场景自动降低角色化程度。

### 27.5 Notification

AI 生成通知摘要时，可应用 Persona 风格。

但系统 Critical Alert 必须保持清晰、可识别。

---

## 28. 默认 Ikaros Persona 示例配置

以下为概念示例，不是最终 Prompt：

```yaml
key: ikaros.default
name: Ikaros
builtin: true
traits:
  warmth: medium
  directness: high
  verbosity: low
  humor: low
  initiative: medium
  emotional_expression: low
  protectiveness: high
  curiosity: medium
  fact_discipline: high
communication:
  emoji: minimal
  uncertainty: explicit
  error_acknowledgement: direct
  user_address: auto
scenarios:
  planning:
    initiative: high
    verbosity: medium
  media_companion:
    warmth: high
    humor: medium
  focus:
    verbosity: minimal
    initiative: low
  operations:
    emotion: minimal
    precision: high
    risk_aversion: high
```

真正运行时由 Persona Runtime 转换为各模型适合的 Instruction。

---

## 29. 内置 Persona 的可修改性

`ikaros.default` 建议作为系统内置只读定义。

管理员需要修改时推荐：

```text
ikaros.default
      ↓ clone
my.ikaros
```

这样版本升级时系统仍然可以更新官方默认人格，而不会覆盖管理员修改。

平台默认 Persona 可以指向：

```text
ikaros.default
```

也可以指向：

```text
my.ikaros
```

---

## 30. Persona 配置优先级

最终运行配置建议：

```text
Built-in Persona Base
        ↓
Published Persona Version
        ↓
Scenario Override
        ↓
Allowed User Override
        ↓
Conversation Override
        ↓
Runtime Policy Clamp
```

其中 Runtime Policy Clamp 负责确保：

- verbosity 不突破输出限制
- Tool Preference 不突破 Tool Policy
- Persona 不突破 Safety Policy
- Memory 不突破 Privacy Policy

---

## 31. Persona Lifecycle

```text
DRAFT
  ↓
PUBLISHED
  ↓
DEPRECATED
  ↓
ARCHIVED
```

也可以临时：

```text
PUBLISHED → DISABLED
```

Conversation 如果引用已 Deprecated / Archived Persona Version，应保留历史可解释性，但新 Conversation 不再默认选择。

---

## 32. Persona Event

Persona 系统应产生平台 Event。

例如：

```text
ai.persona.created
ai.persona.updated
ai.persona.published
ai.persona.disabled
ai.persona.archived
ai.persona.default_changed
ai.persona.user_selected
```

用于：

- Audit
- Analytics
- Cache Invalidation
- Notification
- Plugin Integration

敏感 Prompt 内容不应直接进入 Event Payload。

---

## 33. Persona Cache

Persona 是高频读取、低频修改数据。

可以安全缓存：

```text
Persona Metadata
Published Version
Compiled Persona Instruction
Scenario Profile
```

Persona 更新时通过 Event 失效缓存。

缓存 Key 必须包含：

```text
persona_id
version
model_adapter_version
scenario
```

避免模型 Prompt Adapter 更新后仍使用旧编译结果。

---

## 34. Persona 与 Offline Client

客户端离线时可以缓存：

- Persona Metadata
- Avatar
- Voice Preference
- UI Style

如果本地具备 Local Model，则可以继续使用已缓存的 Published Persona Version。

如果没有 Local Model，则 Persona UI 仍可展示，但 AI 推理能力不可用。

离线缓存 Persona 不代表允许离线获取新的服务器私有 Memory。

---

## 35. Persona 与多用户场景

Ikaros Self-hosted 实例可能有多个用户。

必须区分：

```text
Platform Default Persona
User Default Persona
Conversation Persona
Room Persona
```

### 35.1 Room Persona

Room 可以存在一个 AI Assistant Persona。

但 Room Persona 的数据访问范围必须基于 Room Context 和成员共同可访问范围。

不得因为房主可以访问某 Resource 就自动向所有成员泄露相关内容。

---

## 36. Persona 与 Collaboration

协作文档中的 AI Persona 应明确显示 AI 生成内容来源。

例如：

```text
Generated by Ikaros
Persona: ikaros.default@2
Model: GENERAL_REASONING
```

不要求普通正文永久显示所有技术信息，但 Revision / Provenance 中必须可追踪。

---

## 37. Persona 与内容生成

Persona 可以影响生成内容风格，但必须区分：

```text
Assistant Voice
```

与：

```text
Requested Content Voice
```

例如用户要求：

> 帮我写一封正式商务邮件。

即使 Ikaros Persona 默认简洁、天然，也不应把邮件写成角色对白。

原则：

> Persona 决定 AI 如何与用户协作，不应未经要求污染用户最终内容的目标文风。

---

## 38. Persona 与搜索结果

Semantic Search 的排序不应默认因 Persona 喜好而改变事实相关性。

Persona 可以影响：

- 搜索结果解释
- 推荐理由
- 展示摘要

推荐系统如需使用个性化偏好，应通过独立 Recommendation / Preference 体系建模。

Persona ≠ Recommendation Profile。

---

## 39. Persona 与 Recommendation Preference

需要严格区分：

```text
Persona
AI 的人格

User Preference
用户喜欢什么

Recommendation Profile
系统推测用户可能喜欢什么
```

这三个概念不能合并。

否则更换 AI Persona 会错误改变用户内容推荐历史。

---

## 40. Persona 与系统启动

AI 服务初始化时应：

```text
Load Platform AI Policy
        ↓
Load Persona Registry
        ↓
Validate Default Persona
        ↓
Compile / Warm Common Persona Cache
        ↓
Ready
```

如果自定义默认 Persona 损坏：

```text
Fallback → ikaros.default
```

并生成：

```text
Health Warning
Operation Log
Notification to Administrator
```

人格配置错误不能导致 Ikaros 核心服务无法启动。

---

## 41. 默认人格可用性要求

即使管理员删除所有自定义 Persona，系统仍必须保留：

```text
ikaros.default
```

作为最后 fallback。

内置默认人格不允许被永久删除，只允许平台层面不选择它作为默认人格。

---

## 42. 非功能要求

### NFR-PERSONA-01 可复现

同一 Persona Version 必须能够被历史 Trace 识别。

### NFR-PERSONA-02 Provider-neutral

Persona Definition 不绑定单一 LLM Provider。

### NFR-PERSONA-03 权限隔离

Persona 不得提升任何主体权限。

### NFR-PERSONA-04 性能

常用 Persona 的加载不应成为每次 AI 调用的明显性能瓶颈。

### NFR-PERSONA-05 可审计

人格发布、默认人格修改、Prompt Fragment 修改必须记录 Operation Log。

### NFR-PERSONA-06 隐私

Persona 本体默认不应保存用户秘密；用户数据进入 AI Context 必须经过正常 Privacy / Permission Policy。

### NFR-PERSONA-07 可降级

Persona 子系统异常时 AI 可以降级到安全基础系统人格，而不能绕过 Safety Policy。

---

## 43. P0 / P1 / P2

### P0

- Persona Registry
- 内置 `ikaros.default`
- Persona Version
- Persona Selection
- Structured Traits
- Communication Style
- Scenario Profile
- Persona Prompt Assembly
- Persona + Permission / Safety 分层
- 用户称呼配置
- Admin Persona Management
- Persona Preview
- Operation Log

### P1

- User-created Persona
- Persona Import / Export
- Persona-specific Memory
- Voice Profile
- Visual Identity
- Persona Analytics
- Plugin Persona
- Conversation Persona Switching
- Room Persona

### P2

- Persona Marketplace / Community Sharing
- Persona A/B Evaluation
- 自动 Persona Optimization
- 高级多 Persona Agent Team
- 动态 Persona Adaptation

动态适配必须保持用户可控，不能在后台不可解释地改变人格。

---

## 44. 验收原则

AI Persona System 初版至少应满足：

1. AI 每次交互都能解析到明确 Persona。
2. 默认 Persona 为 `ikaros.default`。
3. 默认人格以《天降之物》的伊卡洛斯角色为人格原型。
4. 管理员可以管理、预览和发布 Persona。
5. 用户可以选择 Persona 并配置有限个人偏好。
6. Persona 具有明确 Version。
7. Conversation / Agent Trace 可以追踪所用 Persona Version。
8. Persona 不能改变 Permission / Safety / Tool Authorization。
9. Persona 可以按 Planning / Media / Operations 等场景应用不同 Profile。
10. AI Memory 与 Persona 分离。
11. Persona 与 Model Provider 分离。
12. Persona 与 Recommendation Profile 分离。
13. 内置 Ikaros Persona 永远可作为 fallback。
14. 人格配置异常不能导致核心平台不可用。
15. 高风险业务动作仍由现有 Command / Permission / Confirmation 体系控制。

---

## 45. 总结

Ikaros V2 中的 AI 不应只有“能力”，还应具有连续、一致、可识别的“人格”。

平台默认通过内置 **Ikaros Persona** 建立统一智能伙伴体验，并允许管理员和用户在安全边界内进行配置与扩展。

最终关系应保持：

```text
Safety / Permission
        ↓
Persona
        ↓
Scenario
        ↓
User Preference
        ↓
Context / Memory
        ↓
Model
        ↓
Tool / Capability / Command
```

其中：

> Persona 决定 AI 如何成为“Ikaros”，但永远不决定 AI 可以越过哪些边界。
