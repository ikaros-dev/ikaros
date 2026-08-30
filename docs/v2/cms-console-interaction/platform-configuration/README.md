# 平台配置 — CMS Console 交互规格

## 1. 参数

**路由：** `/console/platform/parameters`

### 页面标题区
- 标题：`参数`。
- 副标题明确说明：这里管理的是平台/运行时配置，不是任意数据库字段编辑器。
- 搜索框。
- Filter Chip：子系统、Scope、是否需要重启、来源（`默认值`、`配置文件`、`数据库`、`环境变量`、`有效覆盖值`）。
- 只有 Backend 支持扩展参数时才显示主操作 `添加自定义参数`。

### 参数表格

列：
- Key；
- 显示名称；
- 子系统/分组；
- Effective Value；
- Source；
- Data Type；
- Scope，例如 `Global`、`User-default`；
- 是否需要重启；
- 最近修改时间；
- 操作。

敏感值只显示 `已配置` / Masked State，不展示明文。

### 参数详情 / 编辑 Side Sheet

字段：
- 系统参数的 Key 只读；
- 描述；
- 当前 Effective Value；
- 当前配置 Override；
- Default Value；
- Type 与 Validation Constraint；
- Source Precedence 说明；
- Restart Requirement；
- 受影响的 Subsystem/Service。

交互：
- 根据数据类型使用合适 M3 控件：Boolean 用 Switch，Number 用数字输入，Enum 用 Select，结构化文本用 Textarea，Credential 用 Secret Field。
- `恢复默认值` 的语义是移除 Override，而不是把 Default Value 再复制写入一遍。
- 保存前校验格式，并展示 Before/After Diff。
- 如果修改需要重启，成功后的 Snackbar/Banner 明确显示 `已保存 — 需要重启后生效`，并链接到相关运维说明/状态；不得让用户误以为已经实时生效。
- 来自 Environment 且运行时不可修改的参数显示 Read-only 说明，并告诉用户应该从哪个运维入口修改，但不得暴露 Secret Environment Value。

### 批量配置导入 / 导出

支持时放在 Overflow 中。导入前预览 Unknown Key、Invalid Value、需要重启的修改以及未包含 Secret 的项目，再允许 Apply。Secret 默认从 Export 中排除。

## 2. 字典

**路由：** `/console/platform/dictionaries`

该页面只管理产品明确设计为“运行时可配置”的受控 Dictionary/Enumeration，不是通用 SQL Table Editor。

### Master / Detail 布局

左侧列表：Dictionary Key、名称、Entry 数量、所属子系统、System/Custom、更新时间。

选择 Dictionary 后，右侧展示详情标题和 Entry Table。

Entry 列：
- Value/Key；
- Display Label；
- 是否存在 Localized Label；
- 描述；
- Sort Order；
- Enabled；
- System/Custom；
- 可用时显示引用数量；
- 操作。

### Entry 编辑器

字段：Value/Key（必填）、默认 Label（必填）、Localized Label、描述、顺序、Enabled、可选 Metadata/Schema-defined Field。

交互：
- 顺序有业务意义时展示 Drag Handle；拖放后由服务端保存显式 Sort Index。
- Disable 后已有引用继续有效，但新建选择控件中默认不再出现该 Entry，除非子系统另有定义。
- 删除有引用的 Entry 时必须阻止，或要求选择 Migration Destination。Dialog 显示引用数量和 `替换为` Select。
- System-owned Entry 默认只读；只有产品明确允许时才可修改本地化 Label。

## 3. 菜单

**路由：** `/console/platform/menus`

仅当 V2 支持 Runtime/Plugin Menu 配置时提供本页面。根交互规格定义的“菜单按子系统分组、默认收起”等核心规则始终具有更高优先级。

### 布局

左侧：按子系统分组的 Menu Tree。右侧：当前选中 Item 的 Inspector/Editor。

每个子系统分组作为 Tree Root。Core Group 不允许删除。编辑器必须区分 `Core`、`Plugin`、`Custom` 来源。

Tree Row 字段：
- 可移动时显示 Drag Handle；
- 图标；
- Label；
- Route/Link 摘要；
- Visibility；
- Source Chip；
- Permission Requirement 指示；
- Overflow。

### Menu Item 编辑器

字段：
- Label：必填；
- 可选 Localization Key/Label；
- Icon Token；
- Parent Subsystem/Group；
- Order；
- Destination Type：`内部路由`、`外部 URL`、`插件路由`；
- Destination；
- Required Permission/Capability；
- External Link 是否新标签页打开；
- Visibility/Enabled。

校验：
- Internal Route 必须匹配已注册/允许的 Route Pattern。
- External URL 默认只允许安全 `https` Scheme；其他 Scheme 只有显式 Policy 允许才可使用。
- Plugin/Core Item 如果 Backend 声明 Mandatory Capability，则前端不能删除对应 Permission Requirement。

交互：
- 拖拽排序只能发生在允许的 Parent Boundary 内。
- 把 Custom/Plugin Item 移动到其他子系统分组时要求确认，因为这会改变 Information Architecture。
- Preview Panel 可以模拟某个 Role 看到的 Navigation Drawer，但 Preview 永远不能绕过实际 Permission。
- `恢复默认菜单` 只作用于明确选中的 Customization，并预览将丢失的自定义顺序/Label。

## 通用配置规则
- 每次保存后都展示配置来源和最终 Effective Value。
- Secret 持续 Masked；除非字段本身明确支持安全 Retrieval，否则不提供 Copy/Reveal。
- 影响 Security、Plugin、Storage、Authentication、External Connectivity 的配置修改应能关联到 Audit Record。
- Backend 支持时配置修改应 Versioned/Auditable；`回滚` 前预览准确目标版本和 Diff。
- 无效平台配置必须 Fail Closed，给出可操作字段错误；不能悄悄部分保存。
