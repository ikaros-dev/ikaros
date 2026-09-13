# Console Golden Path 人工验收脚本（#1404）

本脚本用于真实部署的重复验收，必须连接真实服务。它不创建数据库夹具、不拦截或伪造 API、不以 HTTP 状态码代替用户旅程证据。

## 执行约定

前置条件：

1. 使用当前待验收 Console 构建，并连接真实 PostgreSQL 和真实可用的后端 API。
2. 准备一个普通用户和一个具备 Access/权限管理能力的管理员用户；不要直接修改数据库制造状态。
3. 准备一个可安全重试的真实导入来源、一个可探测的存储 Provider，以及一个可以暂时断开/恢复的测试 Provider 或测试网络边界。
4. 每条旅程都记录入口、加载态、空状态、成功态、典型失败、无权限、后台进度、重试/恢复和最终业务结果。若能力尚未接入，记录为 `blocked`，不要用假数据替代。

证据至少包括：关键页面截图或录屏、页面 URL、业务对象标题、Activity 详情和最终业务结果。Task ID、Attempt、Lease、Blob ID 仅可作为 Advanced 证据，不能作为主验收结果。

## GP01

路径：`Overview → Storage/Providers → Add Content → Preview → Confirm → Activity → Library`

- [ ] Overview 首次加载能看到真实 Provider 状态；没有可用后端时显示阻塞原因和“配置存储”入口。
- [ ] 验证加载 skeleton、真实空 Provider/source 状态和 API 失败提示；Unknown 不得显示为健康。
- [ ] 配置真实 Provider 后点击 Probe，只有真实成功才显示 Healthy。
- [ ] 从 Add Content 选择真实来源，完成扫描/上传、预览识别、重复项和映射处理，再确认导入。
- [ ] 确认后离开 Add Content，Activity 仍显示真实业务动作、关联内容、状态和进度。
- [ ] 成功后从 Activity 进入 Resource，并在 Library 用标题或筛选检索到同一业务对象。
- [ ] 使用来源中可安全失败的一项验证部分失败列表和安全重试；已成功对象不能重复创建。

## GP02

路径：`Overview Attention → Activity Detail → Retry → Library/Resource`

- [ ] 让一个真实导入进入失败状态，确认 Overview Attention 出现业务动作和业务对象文案，而不是只显示 Task ID。
- [ ] 打开 Activity Detail，确认有错误摘要、关联对象、下一步和 Advanced 区域。
- [ ] 点击 Retry，确认已成功的对象保持一份，失败项重新进入 Activity；不要刷新后仅凭 HTTP 202 判定成功。
- [ ] 重试完成后回到 Library/Resource 验证最终对象和内容，而不是只验证 Activity 状态。

## GP03

路径：`Library → Resource Detail → 内容项 → Availability → Restore/Repair → Activity → 返回内容项`

- [ ] 用真实资源验证 Available、Cached、Remote、Processing、Restoring、Missing、Corrupted 的可理解展示；数据缺失必须显示 Unknown 或明确未知。
- [ ] 从剧集、章节、曲目、图片等业务对象解释不可用原因；Blob/Placement/Replica 只能在 Advanced 中出现。
- [ ] 仅有归档副本时，页面说明“内容已保存但需要恢复”，并从业务内容入口发起 Restore/Repair。
- [ ] 确认恢复请求进入 Activity，离开 Resource Detail 后仍能观察真实进度、失败和重试。
- [ ] 完成后返回原内容项，验证 Availability 变为可访问状态并能实际打开/读取。

## GP04

路径：`Overview Attention → Storage/Providers → Provider Detail → Probe/Update → Verify`

- [ ] 未探测 Provider 显示 Unknown；真实探测失败产生 Overview Attention 和用户可理解的下一步。
- [ ] Providers 聚焦连接、容量、健康、凭据状态和启停，不要求用户理解 Placement。
- [ ] 修复真实连接配置并重新 Probe；只有真实成功后 Attention 才消失，失败仍保持 Unknown/异常。
- [ ] 在 Provider Detail/Verify 记录最终健康状态和最近探测时间。

## GP05

路径：`Overview/Resource Attention → Resource Detail/Metadata → Compare → Accept/Keep → Verify`

- [ ] 使用真实同步来源产生一个与人工确认值不同的候选值；验证人工值不会被静默覆盖。
- [ ] 冲突视图同时显示当前值、候选值、来源和必要时间信息；Candidate ID/Sync Run ID 只在 Advanced 展示。
- [ ] 分别验证 Accept external 和 Keep manual 两条路径，重新读取 Resource Detail，结果必须稳定。
- [ ] 处理后对应 Attention 消失，并在 Resource/Library 验证最终元数据。

## GP06

- [ ] 分别从导入、恢复、同步、备份、下载、AI 或自动化中发起真实长任务，确认都进入 `/activity`。
- [ ] 在 Activity 按业务类型、状态和关联对象筛选；验证加载、空结果、失败提示和真实列表。
- [ ] 对支持的任务验证统一详情中的取消或重试；完成后进入业务对象验证结果。
- [ ] 确认 Attempt、Worker、Lease、Raw Payload 只在 Advanced 中出现，不存在平级任务中心入口。

## GP07

- [ ] Storage Overview 展示真实层级分布、容量和异常；缺失数据为 Unknown，不显示伪造 Healthy。
- [ ] 验证 Providers、Policy、Archive & Restore、Maintenance、Backup 各自职责清晰且互相有明确入口。
- [ ] 让策略驱动一次真实迁移/恢复，确认进度进入 Activity。
- [ ] 在 Advanced Maintenance 验证 GC、Replica、Placement、Integrity；日常修复不要求用户处理这些实现对象。

## GP08

路径：`System/Access → 修改角色或权限 → 目标用户刷新 → 导航和直接 URL 验证`

- [ ] 管理员撤销目标用户对一个 workspace/app 的能力；目标用户刷新后入口不显示。
- [ ] 目标用户直接打开 canonical URL，得到一致的无权限语义；服务端仍是最终授权判断。
- [ ] 验证 Profile、Preferences、Security 等当前用户入口不因管理权限不足而消失。
- [ ] 重新授予权限后刷新，导航和页面访问恢复；验证缓存不会保留旧权限。
- [ ] 在 System/Audit 查询对应角色/权限管理操作，记录审计证据。

## GP09

- [ ] Sidebar 只有 Overview、Library、Add Content、Activity、Storage、Apps、System 七个一级工作区。
- [ ] 逐一从七个入口进入页面，确认 canonical route 归属：内容在 Library，应用在 Apps，平台治理在 System，后台工作在 Activity。
- [ ] 直接访问历史 `*-center` 或 `/console/*` 地址，确认它们未注册为产品路由；不验收旧路由兼容。
- [ ] 确认不存在 AI Task、Operations Task、Import Task 等平级任务入口。
- [ ] 保存路由树、Sidebar 和直接 URL 的验收证据；发现新一级入口时阻断合并并回到 IA 契约评审。

## 结果记录

| Golden Path | 结果（passed/failed/blocked） | 真实环境/账号 | 证据位置 | 备注 |
| ----------- | ----------------------------- | ------------- | -------- | ---- |
| GP01        |                               |               |          |      |
| GP02        |                               |               |          |      |
| GP03        |                               |               |          |      |
| GP04        |                               |               |          |      |
| GP05        |                               |               |          |      |
| GP06        |                               |               |          |      |
| GP07        |                               |               |          |      |
| GP08        |                               |               |          |      |
| GP09        |                               |               |          |      |
