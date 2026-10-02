# ADR-009：逻辑附件与 Blob 多对多绑定

| 项目 | 内容 |
|---|---|
| 状态 | Accepted（身份与关系决策；执行契约尚待收敛） |
| 日期 | 2026-10-03 |
| Owner | Storage；Media 负责转码与媒体解释，Relation Core 负责附件间关系 |

## 背景

旧设计要求一个已物化 Attachment 绑定一个 Blob，并为转码输出创建新 Attachment。这使同一逻辑视频的不同码率版本被表达为多个业务附件，无法准确区分“同一文件的不同字节表示”与“视频和字幕等不同文件之间的关系”。

## 决策

1. Attachment 表示逻辑文件的业务身份。原件与该文件的转码表示属于同一个 Attachment；字节变化产生新 Blob，不因此产生新 Attachment。
2. Attachment 与 Blob 的持久化关系为多对多，通过 Storage 拥有的 `attachment_blob` 绑定表表达。一个 Attachment 可以绑定多个 Blob；多个 Attachment 可以因内容去重共享同一 Blob。绑定属于业务引用，不能由 Provider 或物理路径推导。
3. Blob 仍表示不可变字节身份，普通内容沿用摘要算法、SHA-256 摘要和大小的去重约束；去重必须遵守既有普通内容 / Secure Domain 隔离规则。名称、用途、权限和用户生命周期不进入去重键。
4. Blob 与 Placement 保持一对多：副本或独立物化的解冻临时对象仍引用原 Blob；转码后的不同字节不得登记为原 Blob 的 Placement。
5. 新增独立的 `blob_metadata` 表，仅保存从对应 Blob 字节提取的文件技术信息，如容器、时长、码率、编码、分辨率、帧率、音轨及声道信息。业务标题、歌手、备注等资源业务元数据通过 Resource Owner 的公开 API 保存到 `resource_metadata`，沿用来源、人工锁定与授权规则；文件内嵌业务标签不能直接成为 Blob 技术元数据或静默覆盖资源业务元数据。`blob_metadata` 与绑定表均由 Storage 拥有；提取器、Media 和 Plugin 通过公开 Capability 提交或查询，不直接访问 Storage persistence。记录类型与版本契约须进一步冻结。
6. 视频与字幕、歌曲与歌词等独立逻辑文件使用 Attachment 之间的关系描述，由 Relation Core 持久化。关系不代替 `attachment_blob` 绑定；转码表示的来源追踪使用具体 Blob / 绑定引用。
7. GC 必须检查所有有效绑定，包括跨附件共享、原件和派生表示；解除一个绑定或删除一个附件不得使仍被其他附件引用的 Blob 可被删除。Retention Hold、Archive Base、Placement 和审计保护继续适用。
8. 默认内容固定为原件。每个已物化附件必须具有唯一有效原件绑定；未明确选择表示时，下载、预览、大小、媒体类型、摘要及可用状态均以原件 Blob 为准。转码表示必须通过明确选择使用，不维护可切换的默认 Blob。原件需要恢复、缺失或损坏时返回原件的相应状态，不静默回退到转码表示；同一原件 Blob 的可读 Placement 选择仍由既有 Storage 策略负责。
9. 下载、预览通过可选请求参数选择 Blob；不传该参数时读取原件，传入时使用当前附件内明确选定的 Blob，不改变附件之后请求的默认原件。必须验证所选 Blob 属于当前附件，重新执行附件授权，并以所选 Blob 解析文件技术元数据、可用性和交付内容。仅知道 Blob ID 不能绕过附件授权；参数名、类型及相关路由须在 API 契约中登记。

## 被替代的规则

本决策替代 Attachment / Blob / Storage 设计 §5.2 的单 Blob 基数，以及 §15 中“转码必定创建新 Attachment”的要求。它不把不同压制组的独立文件、视频与字幕或歌曲与歌词合并成一个 Attachment。

## 执行契约待确认事项

- 明确选择表示的 HTTP/Application 参数、错误码及缓存/Delivery Grant/Lease 的所选 Blob 绑定契约；默认原件规则已确定。
- `blob_metadata` 的技术信息类型、提取工具/版本、结果版本、并发更新与 Secure Domain 边界；技术信息范围已确定。
- 绑定角色、表示标识、来源、版本、解除绑定、幂等键和事件 Payload 的精确契约。
- “替换文件”的业务语义；不能把用户确认的转码规则自动扩展为任意替换规则。
- 附件关系类型、方向、授权与关联查询契约。

上述执行契约未冻结前不得实现未登记的表示选择接口或生产 Migration，也不得自行猜测新 HTTP 路由。

## 收敛与迁移

当前 `AttachmentEntity.blobId`、单 Blob 查询和公开 API 是旧实现，不代表新基数已经落地。先收敛 Subsystem、Schema、Command/Query/Event、OpenAPI 与验收契约，再按 Expand → Migrate → Contract 实施。

- Expand：追加 Owner Migration 引入绑定表及元数据表，保留旧读取路径供迁移期间兼容。
- Migrate：为每个已有附件建立指向原 `blob_id` 的绑定，保持附件与 Blob ID；校验引用数量、内容摘要、授权和可读性。大规模回填由有界可恢复任务完成。
- Contract：全部读取、上传提交、预览、Range、恢复、Retention、GC、Backup/Export 与管理端列表改用绑定后，再追加 Migration 收缩旧字段；已发布 Migration 不原地修改。
- 管理员附件 Blob 查询需要从单对象响应收敛为有界列表；旧接口的兼容方式和新 Operation 必须先登记。

## 验收要求

- 一个附件同时绑定原件与不同码率 Blob，不额外创建逻辑附件。
- 两个附件绑定相同字节时复用一个 Blob，保留独立名称、授权和生命周期。
- 新转码 Blob 不修改原 Blob 的摘要/大小；相同字节的多个 Placement 保持同一 Blob。
- 解除一个附件的引用后，另一个附件仍能读取共享 Blob；GC 与并发新绑定竞争时不得删除有效内容。
- 视频与字幕、歌曲与歌词以两个附件及明确关系表达。
- Metadata 更新不改变 Blob 内容身份，不静默覆盖用户业务元数据。
- 未指定表示时始终读取原件；原件不可用但转码可用时不得静默回退。明确选择转码后读取对应字节及技术信息，拒绝不属于当前附件的表示。
- 不同转码 Blob 的技术信息分别存储；共享同一 Blob 的附件可复用其技术信息，仍保持独立业务元数据与授权。
- 迁移前后附件 ID、Blob ID、摘要、权限及内容读取一致；可用性和默认选择必须按冻结契约验证。
