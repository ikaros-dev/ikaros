# Ikaros V2 Console 产品旅程验收契约

> 状态：候选规范（本变更合并后生效）
>
> 本文为 Console 增加 Product Journey Acceptance。既有 A/B/C/R 系列 Issue 继续负责领域能力验收；涉及 Console 的能力还必须通过适用的端到端旅程，才能视为产品层完成。

## 1. 两层验收

### Capability Acceptance

验证 API、持久化、权限、幂等、并发、状态迁移、失败恢复和领域不变量。

### Product Journey Acceptance

验证用户从真实入口出发，能理解上下文、完成目标、看到后台进度、处理失败，并最终回到业务对象验证结果。

以下证据不能单独代表产品完成：API 已存在、页面能打开、按钮能发请求、内部 ID 能查到结果、单个 happy path 通过。

## 2. 通用要求

每条核心旅程至少验证：入口、加载、空状态、成功、典型失败、无权限、后台进度、重试/恢复、最终业务结果和可理解的用户文案。

实现对象只能在必要的 Advanced / Diagnostics 区域出现，不得要求普通用户理解 Task Attempt、Worker、Lease、Blob Placement 等内部结构才能完成任务。

## 3. GP01 首次配置并导入内容

路径：`Overview/Setup → Storage/Providers → Add Content → Preview → Confirm → Activity → Library`

验收：

- [ ] 没有可用存储后端时，明确说明阻塞原因并提供配置入口。
- [ ] 存储后端配置后执行真实健康探测；未知状态不得显示为正常。
- [ ] Add Content 使用来源选择、预览、重复项策略、确认导入等用户步骤，不直接要求操作 Source/Scan/Plan/Run。
- [ ] 长任务进入 Activity，离开导入页后仍可观察。
- [ ] 成功后可从 Activity 进入新 Resource，且 Library 中可搜索到。
- [ ] 部分失败时列出可理解的失败项并允许安全重试。

## 4. GP02 导入失败并恢复

路径：`Overview Attention → Activity Detail → Retry → Library/Resource`

验收：

- [ ] 导入失败进入 Overview 的待处理区域。
- [ ] 主文案使用业务动作和业务对象，不只显示 Task ID。
- [ ] Activity Detail 展示错误摘要、关联对象和下一步操作。
- [ ] 重试不会重复创建已成功对象。
- [ ] 成功后待处理状态收敛，并可在 Library 验证结果。
- [ ] Worker/Attempt/Lease 仅在 Advanced 中显示。

## 5. GP03 内容不可用并恢复

路径：`Library → Resource Detail → 内容项 → Availability → Restore/Repair → Activity → 返回内容项`

验收：

- [ ] 内容项使用 Available/Cached/Remote/Processing/Restoring/Missing/Corrupted 等可理解状态。
- [ ] 问题解释从剧集、章节、曲目、图片等业务对象出发。
- [ ] 只有归档副本时明确说明“内容已保存但需要恢复”。
- [ ] 恢复或修复任务进入 Activity 并显示进度。
- [ ] 完成后原内容项刷新为可访问状态。
- [ ] Blob/Placement/Replica 等仅在高级诊断中出现。

## 6. GP04 存储后端异常处理

路径：`Overview Attention → Storage/Providers → Provider Detail → Probe/Update → Verify`

验收：

- [ ] 真实探测失败会产生待处理项；未探测时显示 Unknown。
- [ ] Providers 页面聚焦连接、容量、健康、凭据状态和启停，不混入 Blob 清理主操作。
- [ ] 配置修复后必须重新探测，真实成功后才清除待处理状态。
- [ ] 日常修复不要求用户理解 Placement。

## 7. GP05 元数据冲突处理

路径：`Overview/Resource Attention → Resource Detail/Metadata → Compare → Accept/Keep → Verify`

验收：

- [ ] 外部同步不得静默覆盖用户人工确认值。
- [ ] 冲突视图显示当前值、候选值、来源和必要时间信息。
- [ ] 接受外部值或保留人工值后，重新读取能得到稳定结果和来源。
- [ ] 处理完成后对应待处理状态消失。
- [ ] Candidate ID、Sync Run ID 不是主界面必需信息。

## 8. GP06 统一观察长期后台工作

验收：

- [ ] 导入、恢复、同步、备份、下载、AI、自动化等长任务进入统一 Activity。
- [ ] Activity 支持按类型、状态、关联对象筛选。
- [ ] 支持取消或重试的任务在统一详情提供操作。
- [ ] 从业务页面发起的任务都有返回业务对象的入口。
- [ ] Execution Attempt 是 Advanced 信息，不是独立一级用户入口。

## 9. GP07 Storage 分层策略与恢复

验收：

- [ ] Storage Overview 展示真实的层级分布、容量和异常，数据缺失时显示 Unknown。
- [ ] Provider 配置与 Tier/Policy 配置分离。
- [ ] Archive & Restore 与 Maintenance 分离。
- [ ] 策略引起的迁移/恢复工作进入 Activity。
- [ ] GC、Replica、Placement、Integrity 等进入 Advanced Maintenance。

## 10. GP08 权限变化后的真实体验

路径：`System/Access → 修改角色或权限 → 目标用户刷新 → 导航/直接 URL 验证`

验收：

- [ ] 无访问能力的入口不显示，但服务端访问控制仍然生效。
- [ ] 直接访问无权限 URL 得到一致的拒绝语义。
- [ ] 当前用户自己的 Profile/Preferences 基础入口不因管理权限不足而消失。
- [ ] 权限撤销后，缓存导航和页面访问能够及时收敛。
- [ ] Audit 中能查到对应管理操作。

## 11. GP09 旧路由兼容迁移

验收：

- [ ] 每个被替换的旧路由都有 redirect、兼容页或明确的废弃策略。
- [ ] 旧 Resource Detail 链接能进入新的 canonical Resource Detail。
- [ ] 旧任务入口能落到 Activity 对应筛选，而不是直接 404。
- [ ] 旧 Storage 深链接在可映射时进入新的 Storage 子页。
- [ ] 不出现循环 redirect。
- [ ] 自动化路由测试覆盖关键旧→新映射。

## 12. Issue 与 PR 要求

涉及 Console 的 Issue/PR 必须写明：

- 关联的 Golden Path 编号；
- 当前改动覆盖旅程的哪一段；
- 是否引入新的用户概念或导航入口；
- 成功、失败、无权限和后台进度的验证证据；
- 是否仍暴露内部实现词汇，如是则说明为什么必须显示。

## 13. Console IA Reset 完成门槛

- [ ] GP01～GP05 全部通过。
- [ ] GP06 Unified Activity 落地。
- [ ] GP07 Storage 分层落地。
- [ ] GP08 权限体验通过。
- [ ] GP09 路由迁移通过。
- [ ] Overview 不存在无真实数据支撑的 Healthy/正常状态。
- [ ] 默认 Sidebar 不再随着后端子系统数量增长。
- [ ] 新功能 Issue 默认不能自行创建一级导航。
- [ ] 核心旅程具有自动化 E2E 或可重复的端到端验收脚本。

关联规范：[Console 信息架构与产品旅程契约](./Console-Information-Architecture-and-Product-Journey-Contract.md)。
