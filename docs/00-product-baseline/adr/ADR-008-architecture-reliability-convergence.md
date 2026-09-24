# ADR-008：架构可靠性与事实来源收敛

| 项目 | 内容 |
|---|---|
| 状态 | Accepted |
| 日期 | 2026-09-23 |
| 关联决策 | ADR-003、ADR-004、ADR-005、ADR-006 |
| Owner | Authentication、Integration、Operations、Storage、Media、Backup |

## 背景

主线架构评审发现 Storage/Media 恢复边界互相矛盾、账号级 Step-up 复用缺少持有证明、Outbox 全局确认不能表达多个 Consumer，以及 Task 原子提交/续租与验收门禁没有完整落地。

## 决策

1. **恢复范围由业务 App 展开。** Media 拥有 Season 兼容 HTTP 入口，先校验 Season 权限，再通过 Storage Public API 解析 Attachment，调用 `StorageRestoreCapability`。Storage 只接收有界 Attachment ID 集合，独立重新授权、预算检查、持久化所选集合；不依赖 `media-api`。旧任务只能使用已持久化的 Attachment 选择，缺少选择时失败关闭，不重新展开历史 Season。
2. **取消无持有证明的账号级 Step-up 换发。** ADR-003 第 9 条被本决策替代。发起 Step-up 必须创建新的 OTP Challenge；拥有普通 Access JWT 不足以借用同账号历史验证。客户端仍可在有效期内使用自己持有的 Verification Grant，不延长其 `verified_at` 或 `exp`。未来换发协议必须先冻结原 Grant 持有证明、Client/Device 绑定、安全版本和新鲜度规则；P0 不新增该协议，不保留一个配置开关恢复账号级换发。
3. **投递结果属于 Consumer。** Integration 以 `(consumer_id,event_id)` 保存投递状态、重试次数、可执行时间及稳定错误分类；Inbox 与 Consumer 的数据库副作用原子提交。旧 `event_outbox.dispatched_at` 仅为兼容遥测，不再决定任何 Consumer 是否收到事件。新 Consumer 默认处理仍保留的历史事件；历史清理/补投策略必须显式声明。
4. **任务写入是短事务。** Claim、Task、Attempt、Outbox 原子提交。Finalize、Heartbeat、取消、超时和过期租约恢复必须在同一短事务中锁定并重新验证 Task；不能使用事务外的先查后写。SQL 条件更新或行锁均可实现该不变量。Handler/Provider 在事务外执行。
5. **Runtime 拥有自动续租和并发上限。** Worker admission 有可配置上限；每次执行定期续租，失败即取消执行订阅，停止以旧租约提交。调度方法返回不代表 Handler 已结束。无法撤销的外部副作用仍由业务幂等机制保护。
6. **恢复激活默认隔离。** Backup Restore 不得自动启动外部副作用；清理旧租约、分类重放、更新认证信任材料并完成人工/自动核对后才可激活，详见 Backup 设计。该条是交付门禁，不宣称恢复运行时已经实现。
7. **验收需要执行证据。** 架构检查扫描根 POM 的所有生产模块，零样本失败；新增数据库可靠性测试在真实 PostgreSQL 上执行。单元 Mock、DDL 文件存在、测试名称均不能代替事务/并发证据。
8. **设计正文唯一。** `90-diagrams/v2-*` 是从主文档生成的图表视图；ADR 编号唯一；CI 验证相对链接和生成视图。原审计权限 ADR-005 更名 ADR-007，内容语义不变。

## 范围与兼容

不改变 Modular Monolith、无登录 Session/JTI blacklist、现有业务 UUID、公开路由和角色权限。安全修正取消历史 OTP 自动换发；Season 路由保持可用，内部由 Media 编排，响应采用统一 `ATTACHMENT_SET` 契约。生产 DDL 只追加 Migration。

## 验收

- 同账号另一客户端仅凭 Access JWT 不能换发历史 Step-up Grant。
- 两个 Consumer 都能收到事件；一个失败不阻止另一个；重放不重复副作用。
- Task+Attempt+Event 任一步失败整体回滚；旧租约不能覆盖新执行状态。
- 超过原租约时长的 Handler 持续续租；失去续租时停止执行；并发不超过配置。
- Storage 无 Media 依赖，恢复执行使用已冻结的 Attachment 集合。
- 所有模块 Controller 被架构检查覆盖，真实数据库测试不因无 Docker 而静默跳过 CI。
