# AI：Assistant、Persona、Memory 与隐私

## 1. App 定位

AI 在 V2 中是横向能力，不是孤立聊天应用。App 提供统一 Assistant 入口，同时在 Resource、Editor、Planning、Analytics 等页面提供上下文 AI 操作。

普通 App 负责：

- Assistant 会话。
- 用户选择 Persona。
- 用户允许的 Persona 覆盖。
- AI Memory 查看 / 编辑 / 删除。
- Context / Privacy 选择。
- Tool Action Draft 与确认。
- AI Job 状态。
- 用户 Feedback。

Provider、Model Registry、全局 Budget、Prompt Definition 等管理员配置属于 CMS，不进入普通 App 主菜单。

---

## 2. 页面目录

- AI Home / Conversation List。
- Assistant Conversation。
- Context Picker。
- Tool Action Preview / Confirmation。
- Persona Selector。
- Persona Preference。
- AI Memory。
- AI Privacy。
- AI Jobs。
- AI Result Provenance。

---

## 3. AI Home

### 3.1 App Bar

- 标题 `Ikaros AI`。
- New Conversation。
- Search Conversations。
- More：Archived、AI Settings。

### 3.2 顶部 Persona Card

- Persona Avatar。
- Display Name，例如 Ikaros。
- 当前 Scenario：General。
- 当前 Model Profile（只显示用户可理解名称，可选）。
- `切换人格`。

### 3.3 快捷提示

Chips / Cards：

- 帮我规划今天。
- 找最近没看完的内容。
- 总结本周进展。
- 找一个文档。
- 解释数据趋势。

建议项按已启用 Capability 生成；没有 Finance 权限时不出现财务快捷项。

### 3.4 Conversation List

- Title。
- Last Message Preview。
- Persona Avatar。
- Scenario。
- Updated At。
- Pinned / Archived。

---

## 4. Conversation 页面布局

### 4.1 Top Bar

- Back / Conversation List。
- Conversation Title。
- Persona Chip。
- Scenario Chip（有特殊场景时）。
- More：Rename、Pin、Export、Archive、Clear Context Snapshot Info。

### 4.2 Message Area

User Message：普通 Surface。

AI Message 包含：

- Persona Avatar / Name。
- Markdown 内容。
- Source / Context Footnote。
- Tool Call / Action Card（存在时）。
- Model / Trace 信息折叠在 `详情`。
- Feedback：Helpful / Not Helpful / Incorrect / Unsafe。
- Copy。

### 4.3 Composer

从左到右 / 上到下：

- `+` Context / Attachment。
- 多行 Text Field。
- Voice（未来可选）。
- Send / Stop。

Composer 上方显示 Context Chips，例如：

- `当前资源：星海邮差`。
- `文档：V2 Storage Review`。
- `Task：T-104`。

每个 Chip 可移除。

---

## 5. Context Picker

Bottom Sheet / Side Sheet Tabs：

- 当前页面。
- Resource。
- Document。
- Collection。
- Task / Goal。
- Attachment。
- Analytics Metric。
- Activity（用户允许时）。

每项显示：Title、Type、Permission / Data Classification、Selected Check。

Private Notes / Password Manager：

- 默认不可选。
- 若 Private Notes 设置 Local Only AI，可在解锁后由 Private Notes 自身入口调用，不从通用 Context Picker 任意读取所有 Vault。
- Password Secret Payload 不进入通用 AI Context。

底部显示：`将向当前 AI Provider 使用 N 项上下文` + `查看隐私说明`。

---

## 6. AI Response 来源

当回答基于 Ikaros 内容时，Message 底部显示 `来源` 折叠区：

每条：

- Resource / Document Icon。
- Title。
- Relevant Section / Chunk（可用时）。
- `打开`。

用户应能区分模型自由生成与 Retrieved Context。

---

## 7. Tool Call Card

### 7.1 Read-only Tool

例如 Search / Read Metric：

Card：Tool Name、Purpose、Status、Result Summary。

通常无需确认，但仍受权限约束。

### 7.2 Low-risk Write Draft

例如创建 Task：

AI 先显示 Action Draft：

```text
创建任务
标题：完成 V2 数据库设计
截止：下周三
预计：4 小时
子任务：3 项
```

按钮：`确认创建`、`编辑`、`取消`。

### 7.3 High-risk / Destructive

例如 Archive Resource / Modify Permission：

Card 使用 Warning / Error Container：

- Action。
- Target。
- Effect。
- Required Permission。
- Risk Level。
- 是否可恢复。
- `继续验证`。

完成 Step-up 后仍需最终 Confirm。

AI 绝不因为 Persona 的“忠诚 / 主动”跳过确认。

---

## 8. Agent Progress

多步骤 Agent 使用可折叠 Progress Card：

- Goal。
- Current Step。
- Completed Steps。
- Tool Calls。
- Waiting for Confirmation。
- Stop。

默认只显示用户可理解步骤；`查看 Trace` 打开详细执行链路。

停止 Agent 不回滚已经明确完成的外部副作用，UI 必须说明已完成哪些动作。

---

## 9. Persona Selector

### 9.1 列表

每个 Persona Card：

- Avatar。
- Display Name。
- Short Description。
- Style Tags，例如 Calm / Concise。
- Published Version。
- `当前` Chip。

平台默认人格标 `Platform Default`。

### 9.2 选择

选择后：

- 新 Conversation 默认使用新 Persona。
- 当前 Conversation 若切换，显示 Confirm：“历史消息不会被改写，从下一条消息开始使用新 Persona。”
- 记录新的 Persona Context Snapshot。

---

## 10. Persona Preference

用户只能修改允许覆盖的部分：

- Verbosity：简洁 / 平衡 / 详细。
- User Address：自动 / 名称 / 昵称 / Master / Custom / None（Persona 支持时）。
- Emoji：少 / 中 / 多。
- Initiative：低 / 中 / 高。
- Response Language。
- Proactive Suggestions。

顶部 Preview 显示当前 Effective Persona：Published Persona + User Override + Scenario Override。

`重置为人格默认`。

管理员定义的 Safety / Permission / Scenario Lock 不出现在可编辑控件中。

---

## 11. Scenario

Assistant 会话可自动 / 手动显示 Scenario：

- General。
- Planning。
- Writing。
- Media Companion。
- Focus。
- Review。
- Operations（管理员有权限场景）。
- Security / Incident（由系统锁定）。

Scenario 被系统锁定时 Chip 显示 Lock Icon，用户不能切回更轻松模式绕过安全表达要求。

---

## 12. AI Memory 页面

### 12.1 Header

- 标题 `AI 记忆`。
- 总开关：Allow Long-term Memory。
- `全部清除`。

### 12.2 Memory Row

- Memory Content Summary。
- Scope：Global / Persona-scoped。
- Created At。
- Source Conversation。
- Last Used（可用时）。
- Edit。
- Delete。

### 12.3 Edit

用户可直接修改 Memory 文本 / 结构化值，保存形成新版本 / Updated At。

### 12.4 全部清除

高影响但可理解的动作：Dialog 显示条数、范围；清除 AI Memory 不删除 Task / Resource / Profile 等业务状态。

---

## 13. AI Privacy 页面

分区：

### Data Usage

- Allow Resource Metadata。
- Allow Document Content。
- Allow Activity。
- Allow Analytics Metric。
- Allow Attachments。
- Allow Personalization / Recommendations。

每项解释用途。

### Provider Boundary

显示用户当前可用 Provider Profile：

- Cloud / Local。
- Max Data Classification。
- 是否允许 Attachment。

用户不能在 App 中修改管理员 Credential，但可以选择“敏感内容只使用 Local Provider”等允许策略。

### Logging

- Save Prompt / Response History。
- Save AI Trace（用户可见层面的设置，受管理员最小审计要求约束）。

---

## 14. AI Job

### 14.1 List

- Job Type：OCR / Transcript / Summary / Embedding / Metadata 等。
- Target Resource。
- Progress。
- Status。
- Started At。
- Model / Provider（详情）。
- Cancel（可取消时）。

### 14.2 Detail

- Input Scope。
- Output Artifact。
- Provenance。
- Error Summary。
- Retry。
- Background Task Link。

AI Job 完成后原始 Resource 不被自动覆盖；生成内容以 Artifact / Suggestion 形式展示。

---

## 15. AI Provenance

生成 Metadata / Summary / Translation 等结果详情：

- Provider。
- Model。
- Model Identifier / Version。
- Prompt Version（用户可读名称优先）。
- Created At。
- Input Sources。
- User / Automation / Agent。
- Confidence：High / Medium / Low（有可靠语义时）。

不伪造精确百分比可信度。

---

## 16. Natural Language Search / Planning

在 Search / Planning 等页面调用 AI 时，结果保持在目标业务页面：

- “找去年看过的时间循环动画” → Search Result。
- “帮我安排明天上午” → Planning Draft。

不强制把所有 AI 功能跳到 Chat 页面。

---

## 17. Offline

- 已缓存 Conversation 可离线查看。
- 没有本地模型时，发送按钮显示 `需要联网`。
- Local Model 可用时按能力继续工作。
- Tool Action 若目标业务支持 Offline Command，可创建 Pending Draft；高风险动作不在无法验证权限 / Step-up 时离线执行。

---

## 18. 响应式

- Compact：Conversation 全屏，Context Picker Bottom Sheet。
- Medium：Conversation + 可切换 Context Side Sheet。
- Expanded：左 Conversation List 300dp、中 Chat、右 Context / Sources 320dp。
- Large：消息正文限制最大可读宽度；Tool / Source Pane 常驻。
