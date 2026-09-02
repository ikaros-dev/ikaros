# Ikaros V2 核心资源库子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 核心资源库子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |

> 本文档补充 `System-Overview-Design.md` 中“核心内容资源子系统”的详细设计，定义 Resource、Collection、Relation、Tag、External Identity、Metadata Provenance、用户资源状态、生命周期与搜索投影之间的领域边界和稳定契约。
>
> 本文档不固定最终 Java 类名、数据库表名或所有 HTTP 路径；具体实现应遵守系统概要、数据库概要与 API 约定。

---

## 1. 设计目标

核心资源库子系统是 Ikaros V2 内容平台的领域底座，回答以下问题：

1. 一个逻辑内容在 Ikaros 中“是什么”。
2. 同一内容的多标题、多语言、外部平台身份与元数据来源如何表达。
3. Resource 如何通过 Collection、Tag 与 Relation 被组织。
4. 用户对 Resource 的收藏、评分、进度和历史如何与 Resource 本体解耦。
5. 用户人工修改与自动元数据同步发生冲突时如何处理。
6. Resource 生命周期如何与 Attachment / Blob 的物理生命周期解耦。
7. 搜索索引如何作为派生投影，而不是新的业务真相源。
8. 媒体、阅读、音乐、图片、创作、AI、分析、自动化等子系统如何安全引用核心资源能力。

核心原则：

> **Resource 表达逻辑内容身份；Attachment 表达可关联的内容对象；Blob 表达实际内容身份。三者不得互相替代。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Resource 的统一内部身份与基本生命周期；
- Resource Type 与类型扩展标识；
- 多标题、别名与语言信息；
- 通用元数据及其来源追踪；
- External Identity 映射；
- Collection 与成员关系；
- Tag 与标签关联；
- Resource Relation；
- Favorite、Rating、Progress、History 等用户资源状态的通用契约；
- Resource 搜索文档的投影来源；
- 资源级 Command / Event 契约；
- 与权限、Attachment、插件、自动化、AI、Analytics 的集成边界。

### 2.2 本子系统不负责

- Blob 字节保存、分层、复制、恢复与物理清理；
- 视频转码、音轨、字幕解析等媒体专属逻辑；
- 漫画页、小说排版、音乐播放队列等专业领域逻辑；
- 文档协作算法与实时 Room 状态；
- 搜索引擎自身的部署与运维；
- AI 推理和模型调用；
- 外部 Provider 的具体抓取协议。

这些能力由对应子系统拥有，但应通过 Resource ID 与本子系统建立稳定关系。

---

## 3. 核心不变量

实现必须长期保持以下不变量：

1. **内部身份稳定**：Resource 使用系统统一的 UUIDv7；第三方 ID 不得成为 Resource 主键。
2. **外部身份可变**：一个 Resource 可以关联多个 Provider / Namespace 的 External Identity，映射可新增、修正或移除而不改变 Resource ID。
3. **逻辑内容与字节分离**：Resource 不保存本地绝对路径、S3 Object Key 等物理存储身份。
4. **类型化而非 JSON 万能化**：通用字段进入核心模型，专业领域字段由对应领域模型拥有；不得把所有专业字段塞入无约束 JSON。
5. **用户修改优先**：用户人工确认或修改的字段默认不得被后台同步静默覆盖。
6. **来源可解释**：会被自动同步覆盖的关键元数据必须能够追踪来源和管理策略。
7. **用户状态隔离**：收藏、评分、进度和历史默认属于 `User + Resource` 关系，不直接污染 Resource 公共元数据。
8. **逻辑删除与 Blob GC 解耦**：Resource 永久删除也只能释放引用，是否物理删除 Blob 由 Attachment / Blob 引用与保留策略决定。
9. **索引不是事实源**：搜索索引、推荐特征和分析投影均可重建，不得成为唯一业务真相源。
10. **跨子系统只通过稳定契约协作**：其他子系统不得直接修改核心资源表绕过领域规则。

---

## 4. 领域模型

### 4.1 Resource

Resource 是一个逻辑内容的统一身份。

建议至少具有以下通用语义：

- `id`：UUIDv7；
- `type`：稳定的 Resource Type；
- `primary_title`：默认展示标题的解析结果或引用；
- `summary`：通用摘要；
- `lifecycle_status`：生命周期状态；
- `created_at` / `updated_at`；
- 必要的版本或并发控制字段。

`primary_title` 只用于快速读取或缓存最终展示值时，不应替代完整的多语言标题模型。

### 4.2 Resource Type

Resource Type 用于决定专业领域能力，例如：

- `anime`
- `tv_series`
- `movie`
- `episode`
- `video`
- `comic`
- `comic_chapter`
- `novel`
- `novel_chapter`
- `artist`
- `album`
- `track`
- `image`
- `album_photo`
- `article`
- `note`
- `document`
- `game`
- `archive`

类型值必须满足：

1. 在 API、事件、插件扩展点中具有稳定标识；
2. 已持久化类型不能被随意重解释；
3. 插件扩展类型必须带命名空间，避免与核心类型冲突，例如 `plugin.example/type-name`；
4. 类型变化若代表领域语义改变，应使用显式迁移或转换 Command，而不是直接更新字符串。

### 4.3 Title / Alias

一个 Resource 可拥有多个标题记录，每条标题至少可以表达：

- 文本；
- BCP 47 语言标签（如 `zh-CN`、`ja-JP`、`en`）；
- 标题角色：primary / original / translated / romanized / alias；
- 来源；
- 是否由用户固定；
- 排序优先级。

约束：

- 同一语言可以存在多个别名；
- “中文标题”“英文标题”不得固化为唯一字段集合；
- 用户可选择默认展示标题策略；
- 标题与别名均进入搜索投影。

### 4.4 External Identity

External Identity 建议具有：

- Resource ID；
- Provider / Namespace；
- Provider Object Type（必要时）；
- External ID；
- Canonical URL（可选）；
- 来源与验证状态；
- 创建 / 更新时间。

核心唯一性：

> 同一 `Provider + Namespace + Object Type + External ID` 在同一 Ikaros Instance 内默认只能映射到一个有效 Resource。

如果 Provider 本身无法保证 ID 在对象类型间唯一，则 Object Type 必须参与唯一性约束。

建立映射时若发现冲突，不得自动合并两个 Resource，应进入显式冲突处理流程。

### 4.5 Metadata Provenance

元数据来源至少区分：

- `USER`
- `FILE_SCAN`
- `IMPORT`
- `PROVIDER`
- `PLUGIN`
- `SYSTEM`
- `AI_SUGGESTION`

对需要同步管理的字段，系统应记录：

- 当前有效值的来源；
- 来源实体 / Provider；
- 来源时间；
- 是否被用户固定（pinned / locked）；
- 自动更新策略；
- 最近一次外部候选值（必要时）。

不要求为每个不可变技术字段建立复杂 provenance；该机制优先用于标题、简介、封面选择、发布日期、分类、人员信息等可能来自多个来源且存在冲突的业务元数据。

### 4.6 Metadata 更新策略

推荐采用以下决策顺序：

```text
用户显式修改并锁定
        ↓ 最高优先级
用户显式确认的值
        ↓
管理员 / 导入规则明确指定
        ↓
已选择的主 Metadata Provider
        ↓
其他 Provider / Scanner 候选
        ↓
系统推断 / AI 建议
```

外部同步返回新值时：

- 对未人工干预的托管字段，可按策略自动更新；
- 对用户已固定字段，只保存候选差异，不覆盖当前值；
- 对来源冲突且无明确优先级的字段，创建可解释冲突，不随机选择；
- AI 输出默认只能作为建议或低优先级候选，除非用户明确开启自动应用规则。

---

## 5. Collection 与组织模型

### 5.1 Collection

Collection 是 Resource 的通用组织容器，可用于表达：

- 媒体库；
- 播放列表；
- 阅读列表；
- 收藏夹；
- 专题；
- 文件夹式层级；
- 用户自定义集合。

Collection 至少应具有：

- UUIDv7 身份；
- 所有者 / 可见性；
- 名称与描述；
- Collection Kind；
- 静态或动态模式；
- 生命周期；
- 排序策略。

### 5.2 Collection Membership

Resource 与 Collection 是多对多关系。

Membership 可包含：

- Resource ID；
- Collection ID；
- 手动排序位置；
- 加入时间；
- 加入者；
- 可选的成员级备注或展示覆盖。

静态 Collection 的成员关系是业务真相；动态 Collection 的成员列表是由查询规则计算的结果，不应把每次计算结果都永久写回成为另一份真相。

### 5.3 层级 Collection

若 Collection 支持父子层级：

- 必须阻止循环引用；
- 移动节点必须作为显式 Command；
- 删除父节点时必须明确子节点策略；
- 不得依赖 UI 保证树合法性。

---

## 6. Tag

Tag 是轻量级、用户可理解的分类能力，不替代专业领域类型和结构化属性。

Tag 至少需要定义：

- 名称；
- 所有者或作用域；
- 可选颜色 / 图标等展示信息；
- 标准化比较规则；
- Resource 关联。

原则：

1. `anime`、`movie` 等稳定领域类型应由 Resource Type 表达，不应退化为 Tag。
2. Season、Episode、Album、Track 等结构关系应由专业模型或 Relation 表达。
3. Tag 名称是否大小写敏感、是否允许同名必须在数据库约束中明确。
4. 批量打标签需要支持幂等，重复请求不得制造重复关联。

---

## 7. Resource Relation

Relation 用于表达两个 Resource 之间有业务语义的关系。

典型关系：

- `contains` / `part_of`
- `prequel` / `sequel`
- `adaptation_of` / `adapted_as`
- `version_of`
- `derived_from`
- `related`

Relation 至少包含：

- source Resource ID；
- target Resource ID；
- relation type；
- 可选排序 / 业务上下文；
- 来源；
- 创建者和时间。

设计要求：

1. Relation Type 必须有稳定语义，不允许业务逻辑依赖任意自由文本。
2. 对具有反向语义的关系，系统应统一定义 canonical 写入方式及反向读取规则，避免存储两条互相漂移的事实。
3. 对天然无向关系，应保证 `(A, B)` 与 `(B, A)` 不形成重复。
4. 禁止 Resource 自关联的类型必须由领域规则阻止。
5. 删除 Resource 时必须明确 Relation 的级联清理或失效策略。

---

## 8. 用户资源状态

### 8.1 设计原则

Resource 公共信息与“某个用户怎么看待 / 使用这个 Resource”必须分离。

用户状态由 `User + Resource` 作用域表达，通用能力包括：

- Favorite；
- Rating；
- Progress；
- History；
- Last Accessed；
- 可选的个人备注或状态。

### 8.2 Favorite

收藏操作必须幂等：

- 重复收藏不产生多条有效记录；
- 重复取消收藏不报内部一致性错误；
- 可记录首次收藏时间和最近状态更新时间。

### 8.3 Rating

评分量纲必须是系统级稳定契约，例如 0–10 或 1–10；客户端不得自行使用不同范围后直接写入。

若未来支持不同评分体系，应在 API 层明确标准化，不让同一字段混用多套量纲。

### 8.4 Progress

Progress 是通用能力，但具体值需保留类型语义，例如：

- 视频：时间位置；
- 漫画：章节 / 页；
- 小说：章节 / 位置；
- 音乐：通常不作为长期阅读式进度，但可记录最近播放位置；
- 文档：最近阅读位置。

核心资源子系统只定义统一的所有权、更新时间和查询入口；专业进度结构由对应内容域定义。

跨设备更新进度时应防止旧客户端把较新的状态无条件覆盖。实现可使用版本、事件时间与领域合并规则，但必须明确处理乱序写入。

### 8.5 History / Activity 边界

History 表达用户对 Resource 的消费事实；Activity 是更广泛的平台活动流。

推荐：

- 专业子系统产生“播放完成”“阅读章节”等领域事件；
- 用户状态投影更新 Progress / Last Accessed；
- Activity 子系统根据需要生成可展示活动；
- Analytics 消费事件形成统计投影。

不得为了统计而让业务请求同步写入大量派生统计表。

---

## 9. Resource Lifecycle

统一状态至少包含：

- `ACTIVE`
- `ARCHIVED`
- `TRASHED`
- `DELETED`（逻辑上完成永久删除后的终态语义；实现可采用审计 tombstone）

典型转换：

```text
ACTIVE <-> ARCHIVED
  |
  v
TRASHED -> ACTIVE
  |
  v
permanent delete
```

规则：

1. 进入回收站不删除 Attachment / Blob。
2. 永久删除 Resource 时，先撤销其业务引用，再由 Attachment / Blob 子系统依据引用计数、保留策略、备份和 Revision 状态决定是否可 GC。
3. 归档 Resource 默认仍可被管理员和有权限用户检索，但客户端可在普通列表中隐藏。
4. Share、Automation、Room 等引用已进入回收站的 Resource 时应获得明确的不可用状态，而不是返回模糊 404。
5. 永久删除应记录必要 Audit 信息，但 Audit 不应保存已删除的敏感内容明文。

---

## 10. 搜索与发现投影

### 10.1 搜索边界

搜索是核心资源能力的重要读取入口，但搜索索引是 **可重建 Projection**。

搜索文档可聚合：

- Resource ID / Type；
- 多标题与 Alias；
- Summary；
- Tag；
- Collection 可检索信息；
- External Identity 的可检索别名；
- 专业子系统公开给搜索的字段；
- 权限过滤所需的最小投影信息。

### 10.2 索引更新

推荐流程：

```text
Domain Command
    ↓
PostgreSQL 提交业务事实
    ↓
Domain / Integration Event
    ↓
Index Projector
    ↓
Search Index
```

要求：

- 数据库提交成功后索引失败，不得回滚已成功的业务事务；
- 索引更新必须可重试、幂等；
- 提供按 Resource 重建与全量重建能力；
- 重建期间允许索引短暂最终一致；
- 管理端应能观察积压、失败和最近成功位置。

### 10.3 权限过滤

搜索结果不得先返回用户无权访问的 Resource 再依赖客户端隐藏。

实现必须在服务端完成授权过滤。对于复杂 ACL，可采用候选集 + 服务端二次授权，但任何优化都不能泄露：

- 标题；
- 封面；
- 摘要；
- 是否存在；
- 私有 Collection 名称等敏感元数据。

---

## 11. Command 设计

核心写操作应以显式 Command 表达，例如：

- CreateResource
- UpdateResourceMetadata
- ChangeResourceType
- AddResourceTitle / RemoveResourceTitle
- BindExternalIdentity / UnbindExternalIdentity
- ResolveMetadataConflict
- CreateCollection / MoveCollection
- AddCollectionMember / RemoveCollectionMember
- AddTag / RemoveTag
- CreateRelation / RemoveRelation
- FavoriteResource / UnfavoriteResource
- UpdateResourceProgress
- ArchiveResource / RestoreResource
- TrashResource / RestoreFromTrash
- PermanentlyDeleteResource

统一要求：

1. Command 入口执行身份认证、授权、参数校验与领域不变量校验。
2. 对可能重试的创建 / 导入 / 自动化请求支持 API 约定中的幂等机制。
3. 高风险永久删除遵守 Step-up Verification / Audit 规则。
4. 跨子系统操作不得依赖一个超大数据库事务；优先使用提交本域事实后发出事件的方式推进后续处理。

---

## 12. Event 契约

建议对外发布稳定的 Integration Event，而不是暴露内部 ORM Entity。

典型事件：

- `resource.created`
- `resource.metadata.updated`
- `resource.external-identity.bound`
- `resource.lifecycle.changed`
- `collection.created`
- `collection.membership.changed`
- `resource.relation.changed`
- `user-resource.favorite.changed`
- `user-resource.progress.updated`

事件至少包含：

- event ID（UUIDv7）；
- event type / schema version；
- occurred at；
- actor / subject；
- Resource ID 等必要引用；
- correlation / causation 信息；
- 幂等消费所需标识。

事件 Payload 只携带消费者需要的稳定信息；大对象和敏感内容应按权限通过 API 再读取。

---

## 13. API 约定

所有公开 HTTP API 必须遵守 `API-Convention-Design.md`，尤其是：

- UUIDv7 的字符串表达；
- RFC 3339 时间；
- Problem Details / 统一错误模型；
- 分页、排序、过滤规范；
- Idempotency-Key；
- ETag / If-Match 或约定的乐观并发机制；
- 权限不足与资源不存在时的信息泄露边界；
- 长耗时操作转 Background Task。

资源更新接口不应允许客户端提交完整数据库实体覆盖更新。应使用明确 DTO / Patch 语义，只暴露允许修改的字段。

批量操作需定义：

- 最大批量大小；
- 全部成功还是逐项结果；
- 是否支持异步 Task；
- 幂等行为；
- 部分失败错误结构。

---

## 14. 并发、一致性与幂等

### 14.1 乐观并发

用户编辑、Provider 同步和后台任务可能同时修改同一 Resource。

对于会发生覆盖冲突的更新，应采用版本号、ETag 或等价乐观并发控制：

1. 客户端读取当前版本；
2. 更新时提交预期版本；
3. 版本不匹配返回冲突；
4. 客户端重新读取并决定合并或覆盖。

Provider 同步不得通过“最后写入者获胜”覆盖用户锁定字段。

### 14.2 幂等

以下操作尤其需要幂等：

- 外部身份绑定；
- Collection membership；
- Tag membership；
- Favorite；
- 导入任务创建 Resource；
- Webhook / Automation 触发的 Resource 更新；
- 索引投影消费。

幂等必须由数据库唯一约束与业务键共同保证，不能只依赖应用层“先查再插”。

---

## 15. 数据库设计约束

具体表结构以 `Database-Overview-Design.md` 和后续迁移为准，本子系统至少要求：

1. 核心实体主键使用 PostgreSQL `uuid`，值为 UUIDv7。
2. 时间点使用 `timestamptz`。
3. External Identity 建立能表达 Provider 作用域的唯一约束。
4. Collection Membership、Tag Membership 对有效关系建立唯一约束。
5. Relation 对方向性 / 无方向性分别定义防重复策略。
6. 生命周期删除不能依赖随意的物理级联删除 Blob。
7. 高频列表和查询字段使用明确列与索引；JSONB 只用于确实需要扩展且可校验的元数据。
8. 所有“唯一性”规则尽量落到数据库约束，不仅写在 Service 代码中。
9. 搜索索引和 Analytics 表不得通过外键反向成为业务表删除的阻塞真相源。

---

## 16. 权限与隐私

默认权限检查对象包括：

- Resource；
- Collection；
- 用户资源状态；
- 元数据编辑；
- 生命周期操作；
- External Identity 管理。

原则：

- 用户资源状态默认只对状态所有者和被明确授权的管理能力可见；
- 私有 Collection 的名称和成员关系同样属于受保护数据；
- Search、AI、Analytics、Automation 不因“内部子系统”身份绕过授权；
- AI Context 构建只能包含调用者当前有权读取的 Resource；
- 插件以 Capability / Permission 访问数据，不直接获得数据库连接权限；
- 高风险永久删除和批量改写应写 Audit。

---

## 17. 与其他子系统的集成

### 17.1 Attachment / Blob

核心资源只保存 Attachment 的逻辑关联，不拥有 Blob Placement。

当 Resource 被删除时：

```text
Resource 删除业务引用
        ↓
Attachment 重新计算有效引用
        ↓
Blob / Replica 按保留策略判断
        ↓
可回收时进入 GC / Background Task
```

### 17.2 专业内容域

媒体、阅读、音乐、图片、创作等子系统使用 Resource ID 作为统一入口，并拥有自己的专业实体。

例如 Episode 可以是 Resource，同时其播放版本、字幕轨和技术参数由媒体子系统拥有。

### 17.3 Plugin / Provider

Provider 可以：

- 查询 Resource；
- 提交 External Identity；
- 提交元数据候选；
- 发起受控同步 Command。

Provider 不可以：

- 直接覆盖用户固定字段；
- 用外部 ID 替换内部 ID；
- 直接修改数据库；
- 绕过权限和 Audit。

### 17.4 Automation

Automation 通过公开 Command 触发资源操作，并使用 Integration Event 作为触发源。

同一事件重复投递不得造成重复 Collection membership、Tag 或 Resource。

### 17.5 AI

AI 可以生成：

- 标签建议；
- 摘要建议；
- 关系建议；
- 元数据修复建议；
- 搜索 Query 改写。

默认都属于 `AI_SUGGESTION` 来源，不能静默成为高优先级用户事实。

### 17.6 Analytics

Analytics 消费 Resource 与用户行为事件形成统计投影，不直接修改 Resource 业务状态。

---

## 18. 典型流程

### 18.1 从第三方 Provider 导入

```text
Provider 返回外部对象
    ↓
按 External Identity 查找映射
    ├── 已映射 → 读取现有 Resource
    └── 未映射 → 创建 Resource
                    ↓
               绑定 External Identity
                    ↓
               写入元数据候选
                    ↓
               按 Provenance 策略应用
                    ↓
               发布 Integration Event
                    ↓
          Search / Analytics / Automation
```

并发导入相同 External Identity 时，数据库唯一约束必须保证最终只有一个有效映射；冲突请求进入重读 / 合并流程。

### 18.2 用户修改标题后再次同步

```text
用户修改标题并标记为人工值
        ↓
Provider 下次同步得到不同标题
        ↓
保存新的 Provider 候选值
        ↓
发现当前值为用户固定
        ↓
不覆盖
        ↓
可选：在管理 UI 提示存在外部更新
```

### 18.3 永久删除 Resource

```text
Step-up Verification
        ↓
权限与引用影响检查
        ↓
Resource 进入永久删除流程
        ↓
撤销业务关系 / Share / 索引投影
        ↓
释放 Attachment 引用
        ↓
后台任务评估 Blob 是否可 GC
        ↓
Audit 记录结果
```

---

## 19. 可观测性

至少应提供以下指标或管理视图：

- Resource 总量及按类型分布；
- 回收站 / 归档数量；
- External Identity 冲突数量；
- Metadata 同步失败 / 冲突数量；
- Search projection backlog；
- Resource index rebuild 状态；
- 批量导入成功 / 失败数量；
- Lifecycle cleanup / GC 关联任务状态。

日志中应携带 Resource ID、correlation ID 和 task / event ID，但避免记录受保护正文、密码或 Secret。

---

## 20. 测试与验收基线

实现阶段至少覆盖以下场景：

1. 同一 External Identity 并发导入不会产生两个有效映射。
2. 用户锁定标题后 Provider 同步不会覆盖该标题。
3. Resource 可拥有多个语言标题和别名，并均可被搜索。
4. 一个 Resource 可加入多个 Collection，重复加入保持幂等。
5. 层级 Collection 无法形成循环。
6. 有向 / 无向 Relation 均不会产生语义重复。
7. 不同用户对同一 Resource 的 Favorite / Rating / Progress 相互隔离。
8. 旧客户端乱序提交 Progress 不会无条件覆盖较新状态。
9. Resource 进入回收站不会立即删除 Blob。
10. Search Index 删除或完全重建不会丢失业务真相。
11. 未授权用户无法通过搜索、外部身份查询或错误差异推断私有 Resource。
12. 永久删除具有权限校验、二次验证和 Audit。
13. 索引消费、Automation 消费与 Webhook 重试均具备幂等性。
14. API 时间、ID、错误、分页和并发控制符合 `API-Convention-Design.md`。

---

## 21. 后续需要独立展开的设计

本设计补齐核心资源库领域底座后，以下能力仍建议后续形成独立子系统详细设计，而不是继续堆入本文档：

- 媒体播放 / 转码 / 字幕与派生媒体；
- 漫画与小说内容模型；
- 音乐领域模型；
- 图片与相册领域模型；
- 内容创作 / Revision / 协作文档；
- Sharing / Collaboration / Room；
- Notification；
- Offline Cache / Device Sync；
- Search Engine / Ranking 的实现级设计（若后续超出 PostgreSQL FTS 能力）。

这些文档均应复用本文档定义的 Resource 身份、Provenance、Lifecycle、Command、Event 与权限边界，避免重新定义第二套核心内容身份模型。
