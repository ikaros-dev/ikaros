# Automation：自动化、Import / Sync 与用户级集成

## 1. App 边界

App 负责用户自己的自动化与外部数据连接体验：

- User Automation Rule。
- Rule 执行历史。
- Import。
- Sync Relationship。
- 冲突处理。
- 用户有权使用的 Plugin / Connector。

系统级 Automation、Plugin 安装 / 升级、Secret Provider 管理、Dead Letter Queue 全局治理等管理员能力属于 CMS。

---

## 2. 页面目录

- Automation 列表。
- Rule Builder。
- Trigger Picker。
- Condition Builder。
- Action Builder。
- Rule Review。
- Execution History。
- Execution Detail。
- Import Center。
- Import Preview。
- Sync Source 列表。
- Sync Detail / Conflict。
- Integrations / Connectors。

---

## 3. Automation 列表

### 3.1 App Bar

- 标题 `自动化`。
- Search。
- Filter：Enabled / Disabled / Failed。
- `新建`。

### 3.2 Rule Card

字段：

- Rule Name。
- Enabled Switch。
- Trigger Summary。
- Condition Count。
- Action Summary。
- Last Run。
- Last Result：Success / Partial / Failed / Skipped。
- Execution Count（可选）。
- Owner：Me / Shared Context。
- More。

Enabled Switch 切换前若 Rule 当前为 Degraded / Missing Connector，显示原因，不允许“看起来开启但实际无法执行”。

---

## 4. 新建 Rule：步骤式 Builder

使用 Stepper / 多页面 Flow：

1. When。
2. If。
3. Then。
4. Review。

Mobile 每步全屏；Desktop 左侧 Steps 220dp、右侧编辑区。

顶部始终显示 Draft Rule Name，可编辑。

---

## 5. Trigger Picker

### 5.1 分类

- Resource。
- Media。
- Planning。
- Finance。
- Storage User Events（有权限）。
- Time / Schedule。
- Webhook（能力允许）。
- Plugin Event。
- Manual Trigger。

### 5.2 Trigger Card

- Icon。
- Name，例如 `新剧集创建`。
- Event Key（Advanced 中）。
- Description。
- Provider / Plugin（扩展 Trigger）。
- Required Permission。

Tap 后进入 Trigger Config。

### 5.3 Trigger Config

按 Trigger 动态字段，例如 Scheduled Time：

- Frequency。
- Time。
- Time Zone。
- Start / End。

Resource Event：

- Resource Type。
- Collection / Tag Scope。

---

## 6. Condition Builder

### 6.1 行结构

每条 Condition：

1. Field / Capability。
2. Operator。
3. Value。
4. Remove。

例如：

```text
Resource.type = ANIME_EPISODE
AND Parent.favorite = true
```

### 6.2 Group

支持 AND / OR Group；Mobile 使用嵌套 Card，最大视觉缩进受限，深层逻辑转 Advanced Expression View。

### 6.3 字段来源

字段由公开 Capability / Event Schema 提供，UI 不允许用户选数据库表字段。

---

## 7. Action Builder

Action 分类：

- Create Task。
- Complete Task。
- Send Notification。
- Add Tag。
- Add to Collection。
- Archive / Restore Resource。
- Create Share。
- Start Background Task。
- Call Webhook。
- Plugin Action。

每个 Action Card 显示：

- Name。
- Target Scope。
- Required Permission。
- Risk Level。
- Side Effect。

高风险 Action 使用 Warning Icon。

### 7.1 Action 参数

例如 Create Task：

- Title Template。
- Project。
- Scheduled / Deadline Rule。
- Priority。
- Related Resource = Trigger Resource。

模板变量通过 Token Picker 插入，不要求用户手写 `${...}`。

---

## 8. Rule Review

保存前必须展示可读规则：

```text
当：收藏动画出现新剧集
如果：资源属于“2026 夏季观看”
那么：
  1. 创建任务“观看 {{episode.title}}”
  2. 发送应用内通知
```

下方：

- Owner / Principal。
- Permissions Required。
- Connected Services。
- Risk Summary。
- Rate Limit / Dedup Policy 摘要。

按钮：`保存为禁用`、`保存并启用`。

缺少权限时禁用“启用”，并说明缺什么。

---

## 9. AI Natural Language Automation

Automation 首页 `用自然语言创建`：

用户输入描述。

AI 输出 Draft：Trigger、Conditions、Actions。

必须进入同一个 Rule Review；AI 不能直接保存并启用 Rule。

转换结果上标 `AI 生成草稿`。

---

## 10. Rule Detail

Header：Name、Enabled、Edit、Run Now（支持 Manual Trigger / Test 时）、More。

Sections：

- Human-readable Rule。
- Trigger Config。
- Conditions。
- Actions。
- Permission Context。
- Execution Policy。
- Last Executions。

若插件卸载 / Action 不可用：顶部 `Degraded` Banner + Missing Capability。

---

## 11. Execution History

Filter：Status、Rule、Date。

Row：

- Status Icon。
- Rule Name。
- Trigger Time。
- Duration。
- Actions Success `2/2`。
- Retry Count。
- Correlation ID 仅 Advanced。

状态：Pending / Running / Succeeded / Partial / Failed / Skipped / Cancelled。

---

## 12. Execution Detail

### 12.1 Summary

- Rule。
- Execution ID。
- Actor / On Behalf Of。
- Trigger Event。
- Start / End。
- Final Status。

### 12.2 Timeline

按步骤：

1. Trigger Received。
2. Conditions Evaluated。
3. Action 1。
4. Action 2。
5. Final Result。

每步：Status、Duration、Result Summary、Retry。

失败步骤可展开 Error Summary；Sensitive Secret 不写入页面。

---

## 13. Loop / Rate Protection

Rule Detail Advanced 显示：

- Max Chain Depth。
- Re-entry Policy。
- Dedup Window。
- Throttle / Debounce。

普通用户使用预设：`标准保护`，高级用户再展开细项。

Rule 被 Loop Protection 阻止时 Execution 显示 `Skipped: Loop protection`，不记成模糊失败。

---

## 14. Import Center

### 14.1 Source Cards

- Device File。
- Directory / Local Integration（Desktop）。
- Object Storage / NAS Connector。
- WebDAV。
- Plugin Importer。
- V1 Migration Importer（未来）。

每卡显示 Provider、支持的数据类型、是否一次性 Import / 可建立 Sync。

### 14.2 Import Flow

1. 选择 Source。
2. 选择范围。
3. Parse / Scan。
4. Preview。
5. Dedup / Conflict Review。
6. Confirm。
7. Background Task。
8. Result。

---

## 15. Import Preview

顶部统计：

- New Resource。
- Update Candidate。
- Duplicate Candidate。
- Error。
- Attachment Size。

Item Row：

- Source Item。
- Detected Type。
- Suggested Resource。
- Metadata Source。
- Duplicate Confidence / Reason（如有）。
- Action：Import New / Merge Candidate / Skip。

AI Duplicate Suggestion 明确标记 Candidate，不能自动合并。

---

## 16. Sync Source 列表

每个 Sync Card：

- Name。
- Provider / Source Type。
- Scope。
- Enabled。
- Last Sync Time。
- Last Result。
- Conflict Count。
- Next Sync（有计划时）。
- More。

Import 与 Sync 使用不同 Icon / 文案；一次 Import 不显示“正在持续同步”。

---

## 17. Sync Detail

Header：Name、Enabled、`立即同步`、More。

Sections：

- Source。
- Scope。
- Direction：One-way / Two-way（Provider 支持时）。
- Mapping。
- User Override Policy。
- Last Runs。
- Conflicts。
- Credential Reference 状态。

Credential 只显示 `已配置安全凭据`，不 Reveal Secret。

---

## 18. Metadata Conflict

Conflict Card：

- Resource。
- Field。
- Current Value。
- Current Provenance。
- External Value。
- Provider。
- Changed At。

Actions：Keep Mine / Accept External / Resume Auto Sync。

批量处理先预览字段数量与影响 Resource。

---

## 19. Integrations / Connectors

普通 App 列出用户可连接的服务：

- Provider Icon / Name。
- Capability：Import / Sync / Calendar / Task / Search 等。
- Connection Status。
- Account / Safe Identifier。
- Last Used。
- Connect / Disconnect。

安装 Plugin、更新插件代码、授予系统级权限仍跳 CMS / 管理端。

Disconnect 确认说明：是否只断开认证、是否保留已经 Import 的 Resource、是否停止 Sync。

---

## 20. Offline

- Rule List / History 可读缓存。
- 创建 Rule Draft 可离线，但 `Enable` 需要能确认服务端 Schema / Permission 时才执行。
- Import 本地文件可离线预处理，但正式纳入服务端需要连接。
- Sync / Room / Webhook 操作离线不可运行，页面显示明确状态。

---

## 21. 响应式

- Compact：Stepper 每步全屏；Execution Timeline 单列。
- Medium：Rule List + Detail。
- Expanded：左 Rule List、中 Builder、右 Schema / Preview Pane。
- Large：Execution Detail 可左 Timeline 右 Raw / Advanced Detail，但默认不暴露低层 JSON。
