# Ikaros V2 Console 产品旅程验收契约

> 状态：设计基线。
>
> 既有 A/B/C/R 系列 Issue 继续负责领域能力验收；涉及 Console 的能力还必须通过适用的端到端产品旅程，才能视为产品层完成。

## 1. 两层验收

### Capability Acceptance

验证 API、持久化、权限、幂等、并发、状态迁移、失败恢复和领域不变量。

### Product Journey Acceptance

验证用户从 canonical 入口出发，能理解上下文、完成目标、观察后台进度、处理失败，并最终回到业务对象验证结果。

以下证据不能单独代表产品完成：API 已存在、页面能打开、按钮能发请求、内部 ID 能查到结果、单个 happy path 通过。

## 2. 通用要求

核心旅程至少验证：入口、加载、空状态、成功、典型失败、无权限、后台进度、重试或恢复、最终业务结果和用户文案。

Task Attempt、Worker、Lease、Blob Placement 等实现对象只能在必要的 Advanced / Diagnostics 区域出现。

用户可见菜单使用中文名称；Golden Path 描述界面路径时使用“仪表盘 / 资源 / 存储 / 应用 / 系统”等产品语言，并使用与菜单层级一致的 canonical route。

## 3. GP01 首次配置并导入资源

路径：`仪表盘 (/dashboard) → 存储 / 存储提供方 (/storage/providers) → 资源 / 添加资源 (/resources/add) → 预览 → 确认 → 资源 / 活动中心 (/resources/activity) → 资源 / 资源库 (/resources/library)`

- [ ] 没有可用存储后端时明确说明阻塞原因并提供配置入口。
- [ ] 存储提供方配置后执行真实健康探测；Unknown 不得显示为正常。
- [ ] 添加资源通过来源、预览、重复项策略和确认完成，不直接操作 Source/Scan/Plan/Run。
- [ ] 长任务进入活动中心，离开添加资源后仍可观察。
- [ ] 成功后可从活动中心进入新 Resource，且资源库可检索。
- [ ] 部分失败时列出业务失败项并允许安全重试。

## 4. GP02 导入失败并恢复

路径：`仪表盘 Attention → 资源 / 活动中心 / 详情 (/resources/activity/:activityId) → 重试 → 资源库 / Resource Detail (/resources/library/:resourceId)`

- [ ] 导入失败进入仪表盘 Attention。
- [ ] 主文案使用业务动作和业务对象，不只显示 Task ID。
- [ ] 活动详情展示错误摘要、关联对象和下一步。
- [ ] 重试不重复创建已成功对象。
- [ ] 成功后 Attention 收敛，并可在资源库验证。
- [ ] Worker/Attempt/Lease 只出现在 Advanced。

## 5. GP03 资源不可用并恢复

路径：`资源 / 资源库 (/resources/library) → Resource Detail (/resources/library/:resourceId) → 资源项 → Availability → Restore/Repair → 活动中心 (/resources/activity) → 返回资源项`

- [ ] 资源项使用 Available、Cached、Remote、Processing、Restoring、Missing、Corrupted 等可理解状态。
- [ ] 问题解释从剧集、章节、曲目、图片等业务对象出发。
- [ ] 只有归档副本时明确说明资源已保存但需要恢复。
- [ ] Restore/Repair 进入活动中心。
- [ ] 完成后原资源项变为可访问状态。
- [ ] Blob/Placement/Replica 只在高级诊断中出现。

## 6. GP04 存储后端异常处理

路径：`仪表盘 Attention → 存储 / 存储提供方 (/storage/providers) → Provider Detail (/storage/providers/:providerId) → Probe/Update → Verify`

- [ ] 探测失败产生 Attention；未探测显示 Unknown。
- [ ] 存储提供方页面聚焦连接、容量、健康、凭据状态和启停。
- [ ] 配置修复后重新探测，真实成功后才清除 Attention。
- [ ] 日常修复不要求用户理解 Placement。

## 7. GP05 元数据冲突处理

路径：`仪表盘 / Resource Attention → Resource Detail (/resources/library/:resourceId) / Metadata → Compare → Accept/Keep → Verify`

- [ ] 外部同步不静默覆盖人工确认值。
- [ ] 冲突视图显示当前值、候选值、来源和必要时间信息。
- [ ] 接受外部值或保留人工值后重新读取结果稳定。
- [ ] 处理后对应 Attention 消失。
- [ ] Candidate ID、Sync Run ID 不是主界面必需信息。

## 8. GP06 统一观察后台工作

- [ ] 导入、恢复、同步、备份、下载、AI、自动化等长任务进入统一活动中心 `/resources/activity`。
- [ ] 活动中心支持按类型、状态、关联对象筛选。
- [ ] 支持的取消或重试动作在统一详情 `/resources/activity/:activityId` 提供。
- [ ] 从业务页面发起的任务有返回业务对象入口。
- [ ] Execution Attempt 是 Advanced 信息，不是独立导航入口。

## 9. GP07 存储分层

- [ ] `/storage` 只作为目录根 redirect，不渲染存储概览页面。
- [ ] 存储概览使用独立 route `/storage/overview`。
- [ ] 存储提供方 `/storage/providers` 与存储策略 `/storage/policy` 分离。
- [ ] 归档管理 `/storage/archive`、备份管理 `/storage/backup`、存储维护 `/storage/maintenance` 职责分离。
- [ ] 策略导致的迁移和恢复进入 `/resources/activity`。
- [ ] GC、Replica、Placement、Integrity 进入存储维护的高级区域。

## 10. GP08 权限变化后的体验

路径：`系统 / 访问控制 / 角色与权限 (/system/access/roles-permissions) → 修改角色或权限 → 目标用户刷新 → 导航和直接 URL 验证`

- [ ] 无访问能力的入口不显示，服务端访问控制仍然生效。
- [ ] 直接访问无权限 canonical URL 得到一致拒绝语义。
- [ ] 当前用户个人资料 / 偏好设置基础入口不因管理权限不足而消失。
- [ ] 权限撤销后缓存导航和页面访问及时收敛。
- [ ] 审计日志 `/system/communications/audit` 可查询对应管理操作。

## 11. GP09 Canonical IA 一致性

当前 V2 不验证旧路由兼容，而验证最终态是否彻底收敛：

- [ ] Sidebar 一级节点仅有仪表盘、资源、存储、应用、系统。
- [ ] 资源分组固定包含资源库、添加资源、活动中心，分别映射 `/resources/library`、`/resources/add`、`/resources/activity`。
- [ ] `/resources` 只作为一级目录根 redirect，不渲染业务页面。
- [ ] 存储分组包含存储概览、存储提供方、存储策略、归档管理、备份管理、存储维护；概览使用 `/storage/overview`。
- [ ] `/storage`、`/apps`、`/system` 等目录根不复用任何子页面组件。
- [ ] 应用工作区没有独立总览菜单和页面；`/apps` 默认进入 `/apps/drive`，业务 App 使用 `/apps/<app>/**`。
- [ ] 系统按访问控制、集成、通知与审计、平台配置、运维形成二级目录；三级页面使用 `/system/<group>/<page>`。
- [ ] Router 不再注册 `/library`、`/add`、`/activity` 作为 canonical 产品路由，也不恢复历史 `*-center` 或旧 `/console/*` 产品路由。
- [ ] 各设计文档使用同一 canonical route tree 和中文菜单显示名称。
- [ ] 不存在 AI Task、Operations Task、Import Task 等平级任务入口；都进入活动中心。
- [ ] 自动化 Router 测试覆盖 canonical routes、权限、菜单层级和目录 redirect。

## 12. GP10 页面与路由一一对应

- [ ] 每个 Sidebar 页面只有一个 canonical route。
- [ ] 二级菜单页面使用二层 route；三级菜单页面使用三层 route。
- [ ] 目录节点不直接渲染默认子页面，只允许 redirect。
- [ ] Resource Detail、Activity Detail、Provider Detail 等参数化详情页面不进入 Sidebar。
- [ ] 页面内 Tab 不因为视觉分区自动生成新 route；只有需要独立 deep link、权限或生命周期时才建立 route。

## 13. Issue 与 PR 要求

涉及 Console 的 Issue/PR 必须写明：

- 关联 Golden Path；
- 覆盖旅程哪一段；
- 是否引入新的用户概念；
- 是否改变菜单层级或 canonical route；
- 成功、失败、无权限和后台进度验证；
- 是否仍暴露内部实现词汇，如是说明原因。

功能 Issue 默认不得引入新的全局一级导航。

## 14. Console IA Reset 完成门槛

- [ ] GP01～GP05 全部通过。
- [ ] GP06 Unified Activity 落地，并以“活动中心”作为用户可见名称。
- [ ] GP07 Storage 分层落地。
- [ ] GP08 权限体验通过。
- [ ] GP09 canonical IA 一致性通过。
- [ ] GP10 页面与 route 一一对应通过。
- [ ] 仪表盘不存在无真实数据支撑的 Healthy 状态。
- [ ] 默认 Sidebar 不随后端子系统数量增长。
- [ ] 核心旅程具有自动化 E2E 或可重复端到端验收脚本。

关联：[Console 信息架构契约](./Console-Information-Architecture-and-Product-Journey-Contract.md)。
