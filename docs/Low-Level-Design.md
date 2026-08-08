# Ikaros 详细设计文档（LLD）

| 项目 | 内容 |
|------|------|
| 产品名称 | Ikaros（Ίκαρος） |
| 文档类型 | 详细设计（Low-Level Design） |
| 文档版本 | v1.0 |
| 编写日期 | 2026-08-08 |
| 代码版本基线 | 1.2.1（`bbccbf32`） |
| 关联文档 | [Product-Requirements-Document.md](./Product-Requirements-Document.md)、[High-Level-Design.md](./High-Level-Design.md) |

---

## 1. 模块设计总则

### 1.1 代码组织约定

| 层 | 位置 | 约定 |
|----|------|------|
| 领域模型/DTO | `api/.../core/<domain>/` | Lombok `@Data @Builder @Accessors(chain=true)`，JSON 字段 `@JsonProperty`（snake_case） |
| 操作接口 | `api/.../core/<domain>/XxxOperate.java` | 供插件/服务调用的领域能力接口 |
| 服务接口 | `server/.../core/<domain>/service/XxxService.java` | 服务层接口，返回 Mono/Flux |
| 服务实现 | `server/.../core/<domain>/service/impl/DefaultXxxService.java` | 默认实现，全量中文 Javadoc |
| 路由端点 | `server/.../core/<domain>/endpoint/XxxEndpoint.java` | 实现 `CoreEndpoint`，函数式路由 |
| 实体 | `server/.../store/entity/XxxEntity.java` | 继承 `BaseEntity`（含 ol_version 乐观锁） |
| 仓库 | `server/.../store/repository/XxxRepository.java` | Spring Data R2DBC `ReactiveCrudRepository` |
| 事件 | `server/.../core/<domain>/event/` + `listener/` | 领域事件与监听器 |
| 枚举 | `api/.../store/enums/XxxType.java` | 领域枚举 |

### 1.2 统一返回结构

```
CommonResult<T> { code, message, data }
PagingWrap<T>   { total, page, size, items: List<T> }
```

---

## 2. 安全与认证模块详细设计

### 2.1 类结构

```
server/security/
├── SecurityConfiguration          # WebFlux Security 装配（双 FilterChain）
│   ├── apiFilterChain             # /api/**, /apis/**, /login, /logout → 认证+授权
│   └── portalFilterChain          # GET /** + text/html → permitAll（门户/主题页）
├── SecurityProperties             # ikaros.security.* 配置绑定
├── MasterInitializer              # 首次启动初始化 MASTER 用户/角色
├── DefaultUserDetailService       # ReactiveUserDetailsService 实现
├── IkarosGrantedAuthority         # 权限载体（ROLE_ 前缀）
├── authentication/
│   ├── SecurityConfigurer         # 认证配置器 SPI
│   ├── basicauth/                 # BasicAuthenticationConfigurer + Filter
│   ├── formlogin/                 # FormLoginConfigurer + Success/FailureHandler
│   ├── jwt/
│   │   ├── JwtAuthenticationConfigurer/Filter/Provider
│   │   ├── JwtReactiveAuthenticationManager
│   │   ├── JwtApplyParam / JwtApplyResponse   # 申请/响应模型
│   ├── oauth2/                    # OAuth2Configurer（预留）
│   ├── totp/
│   │   ├── TotpService            # RFC 6238 核心（见下）
│   │   └── TotpEndpoint / TotpSetupResponse / TotpStatusResponse / TotpValidateParam
│   └── logout/                    # LogoutConfigurer + LogoutSuccessHandler
├── authorization/RequestAuthorizationManager   # 基于权限目标(路径)的访问决策
├── exception/                     # 认证异常 → JSON EntryPoint
└── CorsConfigurer / CsrfConfigurer
```

### 2.2 TOTP 服务设计（RFC 6238）

```
TotpService
├── generateSecret()                # SecureRandom 20字节 → Base32(去=) → 160bit 密钥
├── generateOtpAuthUri(user, secret) # otpauth://totp/Ikaros:{user}?secret=...&issuer=Ikaros&algorithm=SHA1&digits=6&period=30
├── validate(secret, code)          # HMAC-SHA1(counter) → 截断 → 6位 → 时间窗比较
│   constants: SECRET_SIZE=20, CODE_DIGITS=6, TIME_STEP=30s, HMAC_ALGORITHM=HmacSHA1
└── (由 TotpEndpoint 暴露)          # 开启/关闭/重绑定/状态查询
```

**登录流程（分步认证）**：

```
POST /api/v1/security/auth/token/jwt/apply {username, password, totpCode?}
  → JwtAuthenticationProvider
      ├─ 校验用户名密码（UserDetails + PasswordEncoder）
      ├─ 若用户已启用 TOTP：校验 totpCode（缺失/错误 → 401 提示输入验证码）
      └─ 签发 JWT Access Token + Refresh Token（有效期来自 SecurityProperties）
PUT  /api/v1/security/auth/token/jwt/refresh {refreshToken} → 新 Token 对
```

### 2.3 授权模型

```
authority 表：type(权限类型) + target(路径模式, 如 /api/v1/subject/**) + authority + allow
  ┌─ 角色(Role) ─< role_authority >─ 权限(Authority)
  ┌─ 用户(User) ─< ikuser_role >─ 角色(Role)
RequestAuthorizationManager 决策：
  1. 匿名用户：仅 anonymous 角色权限（公开端点）
  2. 认证用户：聚合 用户→角色→权限，target 与请求路径匹配
  3. MASTER 角色：所有路径放行（ROLE_MASTER）
SecurityConst.Authorization.Target 预定义常量：API_CORE_USER、API_CORE_USERS、API_CORE_USER_ME、
API_CORE_ROLE、API_CORE_ATTACHMENT 等（每模块一组）。
```

---

## 3. 用户/角色模块详细设计

### 3.1 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/v1/users` | GET | 用户列表 |
| `/api/v1/user` | POST/PUT | 新增/编辑用户 |
| `/api/v1/user/id/{id}` | DELETE | 删除用户 |
| `/api/v1/user/username/exists/{username}`、`/user/email/exists/{email}` | GET | 唯一性校验 |
| `/api/v1/user/{username}/role` | PUT | 变更用户角色 |
| `/api/v1/user/me` | GET | 当前用户信息（SecurityContext 取用户，防 BOLA） |
| `/api/v1/user/me` | PUT | 更新个人资料 |
| `/api/v1/user/me/username/{newUsername}` | PUT | 改用户名 |
| `/api/v1/user/me/password` | PUT | 改密码 |
| `/api/v1/user/me/email`、`/telephone` | PUT | 绑定邮箱/手机 |
| `/api/v1/user/me/verificationCode/{type}` | PUT | 发送验证码 |
| `/api/v1/role` | GET/POST/PUT/DELETE | 角色 CRUD |
| `/api/v1/role/authorities` | POST/DELETE/GET | 角色-权限绑定 |
| `/api/v1/user/roles` | POST/DELETE | 用户-角色绑定 |
| `/api/v1/user/role/userId/{userId}` | GET | 查询用户角色 |

### 3.2 关键实体

```
UserEntity(BaseEntity): username(UK) password(@JsonIgnore) nickname avatar email
                        telephone site introduce enable non_locked
RoleEntity(BaseEntity): name description parent_id(角色层级)
AuthorityEntity(BaseEntity): allow type target authority
UserRoleEntity / RoleAuthorityEntity：关联表（唯一约束防重复）
UserTotpEntity: user_id(UK) secret(enabled, create_time, update_time)
```

---

## 4. 条目（Subject）模块详细设计

### 4.1 类结构

```
api/core/subject/
├── Subject            # 领域模型（id/type/name/nameCn/infobox/summary/nsfw/airTime/cover/score）
├── SubjectOperate     # 领域操作接口（find/create/update/deleteBy...）
├── SubjectFinder      # 查询接口（按平台/平台ID等）
├── SubjectSynchronizer# 三方同步扩展点（插件实现）
├── SubjectSync        # 同步记录模型
├── SubjectRelation / SubjectRelationOperate
├── Episode / EpisodeOperate / EpisodeRecord(Episode+resources) / EpisodeResource
├── vo/FindSubjectCondition   # 条件查询VO
└── vo/PostSubjectSyncCondition

server/core/subject/
├── SubjectOperator / SubjectRelationOperator / SubjectSyncOperator   # 内部操作器
├── service/SubjectService(impl/SubjectServiceImpl)                   # 服务层
├── service/SubjectRelationService / SubjectSyncService
├── endpoint/SubjectEndpoint / SubjectRelationEndpoint / SubjectSyncEndpoint
├── event/ + listener/   # 封面变更/收藏事件等监听
└── platform同步适配（经插件 SubjectSynchronizer）
```

### 4.2 条件查询设计（FindSubjectCondition）

```
FindSubjectCondition {
  page, size
  name        # Base64 编码模糊查询
  nameCn      # Base64 编码模糊查询
  type        # SubjectType
  tagIds/tags # 标签过滤
  sort        # 排序字段（updateTime/score/airTime...）与方向
  keyword     # 冒号关键词格式: "type:ANIME name:xxx"
}
实现：SubjectServiceImpl 组装 R2dbcTemplate Query（条件拼接 + count + 分页）
     → 返回 PagingWrap<Subject>；更新时维护 updateTime；日期空值排后。
```

### 4.3 关系与同步

```
subject_relation: subject_id + relation_type + relation_subject_id
  创建/删除时校验存在性；关系类型见枚举 SubjectRelationType(13种)。

subject_sync: subject_id + platform + platform_id (UK)
  SubjectSyncService.sync(platform, platformId, type...)
    → 查已有映射（幂等去重）
    → 调用 SubjectSynchronizer 插件拉取
    → 创建/更新条目 + 自动下载封面(SSRF校验) + 写入映射
```

### 4.4 条目-事件联动

```
SubjectServiceImpl.create/update/delete
  → 发布 SubjectAddEvent / 封面变更事件
  → 监听器：重建 Lucene 索引、清理旧封面引用、创建默认剧集(EP1)、级联删除收藏
```

---

## 5. 剧集（Episode）模块详细设计

### 5.1 端点

| 端点 | 说明 |
|------|------|
| POST/PUT/DELETE `/api/v1/episode`、`/episode/id/{id}` | 剧集增删改 |
| GET `/api/v1/episode/{id}` | 单集详情 |
| GET `/api/v1/episode/subjectId/{id}` | 按条目+分组+序号查 |
| GET `/api/v1/episodes/subjectId/{id}` | 条目全部剧集 |
| GET `/api/v1/episode/records/subjectId/{id}` | 剧集+资源组合记录 |
| GET `/api/v1/episode/attachment/refs/{id}` | 剧集附件引用 |
| GET `/api/v1/episode/count/total/subjectId/{id}` | 条目剧集总数 |
| GET `/api/v1/episode/count/matching/subjectId/{id}` | 已匹配资源的剧集数 |
| POST/PUT/DELETE/GET `/api/v1/episode/sequence-regular*` | 正则规则管理 |
| GET `/api/v1/episode/sequence-regular/match` | 文件名→序号/分组匹配 |

### 5.2 正则规则责任链（EpisodeSequenceRegularChain）

```
EpisodeSequenceRegularHandler（api 接口）
 ├─ DB 规则适配器（EpisodeSequenceRegularEntity 包装）
 └─ 插件 Handler（EpisodeSequenceRegularPluginHook 注册）
EpisodeSequenceRegularChain.match(fileName):
  1. 合并 DB 规则 + 插件规则
  2. 按 priority 降序排序
  3. 依次尝试：regex 匹配文件名
       └─ 命中 → 返回 EpisodeSequenceRegularResult {epGroup, sequence}
       └─ 未命中 → 下一个
  4. 全部未命中 → 兜底规则（Integer.MIN_VALUE）或返回空
规则字段：name/regex/ep_group/sequence/priority/description/enabled
内置规则（EpisodeSequenceRegularInitializer 启动时按名称去重插入）：
  [01]~[100]  [EP01]~[EP100]  [01v2]~[100vX]  [NCOP]→OP [NCED]→ED [SP] [CM] [OVA] [OAD]
  [NCOP01]~[NCOP100]  [NCED01]~[NCED100]  [SP01]~[SP100]  " 01 "~" 99 "
```

### 5.3 序列解析逻辑（EpisodeSequenceRegularServiceImpl）

```
输入：附件文件名
输出：EpisodeSequenceRegularResult（分组 + 可选固定序号；无固定序号时从匹配组提取小数序号）
场景：BindEpisodesStep 批量匹配、字幕自动匹配、SP 子目录处理均复用该服务。
```

---

## 6. 附件（Attachment）模块详细设计

### 6.1 类结构

```
api/core/attachment/
├── Attachment              # 领域模型（见 HLD 4.2；url=driver_id://remote_path）
├── AttachmentOperate       # 附件操作接口（上传/目录/查询/删除）
├── AttachmentDriver        # 驱动模型
├── AttachmentDriverFetcher # ★ 驱动获取器扩展点（插件可实现）
│   ├── getChildren(driverId, parentAttId, remotePath) → Flux<Attachment>
│   ├── calculateSha1(attachment)
│   ├── parseReadUrl / parseDownloadUrl
│   └── getSteam(att) / getSteam(att, start, end)     # Range 流
├── AttachmentAccessUrlProvider  # 附件URL提供扩展点
├── AttachmentReference / AttachmentReferenceOperate   # 引用（SUBJECT/EPISODE/USER_AVATAR）
├── AttachmentRelation / AttachmentRelationOperate     # 关系（VIDEO_SUBTITLE）
├── AttachmentSearchCondition / AccessUrlCondition / AttachmentUploadCondition / AttachmentStreamVo
└── VideoSubtitle          # 字幕视图模型

server/core/attachment/
├── service/AttachmentService(impl)            # 附件CRUD/查询/上传（缓存注解）
├── service/AttachmentDriverService(impl)      # 驱动CRUD/启用禁用/刷新
├── service/AttachmentDriverMountService       # 挂载/卸载/重绑定（1.2.1 独立）
├── service/AttachmentSha1Service              # SHA-1 异步计算
├── service/AttachmentReferenceService / AttachmentRelationService
├── extension/LocalAttachmentPathValidator     # 本地路径安全校验
├── extension/LocalDiskAttachmentDriverFetcher # 本地磁盘驱动实现
├── endpoint/AttachmentEndpoint / AttachmentDriverEndpoint / AttachmentReferenceEndpoint / AttachmentRelationEndpoint
└── listener/ 驱动启用/禁用、封面变更、字幕匹配监听器
```

### 6.2 驱动生命周期

```
创建驱动(PUT /attachment/driver) → 保存实体
启用驱动(PUT /attachment/driver/enable/id/{id})
  → AttachmentDriverEnableListener
  → AttachmentDriverMountService.mount(driver)
      ├─ 校验挂载路径（LocalAttachmentPathValidator）
      │   ├─ 路径存在/可读/是目录
      │   ├─ 可信根目录内（阻止逃逸）
      │   ├─ 符号链接解析后仍在根目录内
      │   └─ 动态目录解析仅限已注册目录（DynamicDirectoryResolver）
      └─ 创建驱动根附件记录 + 建立目录映射
禁用驱动 → unmount()：卸载映射 + 隐藏根附件
修改驱动(PUT) → rebind(previous, current)：旧目录→新目录重绑定
刷新驱动 → 增量扫描：size/mtime 变更才更新 + 异步 SHA-1 + 并发刷新合并
```

### 6.3 SHA-1 异步计算（AttachmentSha1Service）

```
触发：驱动刷新发现新文件/文件变化
流程：submit(attachment) → 线程池执行
      1. 读取文件流计算 SHA-1（期间校验文件状态防哈希失真）
      2. 回写 attachment.sha1 + modified_time
幂等：同一附件并发去重；按 size/mtime 判断是否需要重算
```

### 6.4 附件流式读取（Range）

```
GET /api/v1/attachment/stream/id/{id}  [Range: bytes=start-end]
  → AttachmentEndpoint.getStreamById
  → 普通附件：fsPath 直接读文件
  → 驱动附件：AttachmentDriverFetcher.getSteam(att, start, end)
  → 响应 Content-Range / 206 Partial Content
字幕接口：动态解析流 URL（VideoSubtitle）
```

### 6.5 附件引用/关系

```
attachment_reference(type, attachment_id, reference_id)
  类型：SUBJECT（封面）、EPISODE（剧集资源）、USER_AVATAR（头像）
  批量接口：POST /attachment/references/subject/episodes（条目剧集批量绑定）
  POST /attachment/references/episode（单集绑定）

attachment_relation(attachment_id, type, relation_attachment_id)
  类型：VIDEO_SUBTITLE
  自动匹配：剧集附件引用新增事件 → 按文件名模糊查字幕(先驱动后普通) → 建关系
  手动管理：PUT /attachment/relation/{masterAttachmentId}
  查询：GET /attachment/relation/videoSubtitle/subtitles/{attachmentId}
```

### 6.6 分片上传

```
POST /attachment/fragment/unique                 → 生成 unique（会话令牌）
PATCH /attachment/fragment/patch/{unique}        → 追加分片（multipart）
DELETE /attachment/fragment/revert?unique=       → 回滚已传分片
完成后由调用方 PUT /attachment/update 落库
```

---

## 7. 收藏（Collection）模块详细设计

### 7.1 类结构

```
api/core/collection/
├── SubjectCollection / SubjectCollectionOperate
├── EpisodeCollection / EpisodeCollectionOperate
├── vo/FindCollectionCondition    # 条件：userId/type/日期范围/分页
└── event/ 收藏变更事件

server/core/collection/
├── DefaultCollectionService      # 收藏聚合服务
├── EpisodeCollectionServiceImpl  # 剧集收藏
└── listener/ SubjectCollectionCreateEventListener（建条目收藏→建剧集收藏）
```

### 7.2 关键逻辑

```
条目收藏(subject_collection)：user_id+subject_id+type(WISH/DOING/DONE/SHELVE/DISCARD)
  + main_ep_progress + is_private + comment + score
  变更类型 → 联动：DONE 时同步把主剧集标记完成；新建时批量创建剧集收藏
  评分聚合：subject.score 由收藏评分聚合更新（SubjectCollectionScoreUpdateEvent）

剧集收藏(episode_collection)：user_id+subject_id+episode_id+finish+progress+duration
  条件查询支持按日期过滤（历史记录）
剧集列表收藏(episode_list_collection)：用户收藏歌单
```

---

## 8. 目录绑定工作流详细设计

### 8.1 核心接口（api/core/binding）

```
DirectoryBindingStep {
  String name();                        # 步骤名（状态追踪）
  int order();                          # 执行顺序（内置 10/15/20...，插件可插空位）
  default boolean shouldSkip(ctx);      # 跳过条件
  Mono<DirectoryBindingContext> execute(ctx);
  Mono<Void> rollback(ctx);             # 失败逆序回滚
}
DirectoryBindingContext {
  directoryId/directoryName/cleanName/keyword/bracketTags
  platform/platformId → subjectId/subject/subjectSync
  childAttachments / spSubdirectoryAttachments
  createdEpisodes / createdTags / createdAttachmentRefs
  stepResults(Map<name,Status>) / stepErrors(Map<name,msg>) / parameters
}
DirectoryBindingChain.execute(ctx):
  按 order 升序执行；失败 → 已成功步骤逆序 rollback → 记录失败上下文
```

### 8.2 各步骤实现（server/core/binding/handler）

| 步骤 | order | 行为 | rollback |
|------|-------|------|----------|
| ParseDirectoryNameStep | 10 | 目录名→关键词/中括号标签 | - |
| CleanDirectoryNameStep | 15 | 清洗无效字符 | - |
| FindSubjectInfoStep | 20 | 按关键词/平台ID本地查重 | - |
| FetchAndCreateSubjectStep | 30 | 元数据拉取→创建条目 | 删除创建的条目 |
| CreateSubjectTagsStep | 40 | 创建条目标签 | 删除标签 |
| ListFilesStep | 50 | 递归列出目录附件 | - |
| SyncSubjectByPlatformIdStep | 60 | 平台映射写入 subject_sync | 移除映射 |
| BindEpisodesStep | 70 | 文件名→正则链→建剧集→绑定附件引用 | 删除剧集/引用 |
| ProcessSpSubdirectoriesStep | 75 | SP 子目录递归处理 | 回滚子绑定 |

### 8.3 端点

```
POST /binding/directory        # 单目录绑定（返回 workflow 或 task）
POST /binding/directories      # 批量目录绑定
GET  /binding/workflow/{id}    # 工作流状态（status/current_step/step_statuses/fail_message）
GET  /binding/workflow/task/{taskId}
```

---

## 9. 搜索模块详细设计

### 9.1 类结构

```
api/search/
├── SearchParam          # keyword(必填)/limit(默认10,max1000)/offset/高亮前后标签
├── SearchResult<T>      # 命中列表+总数
└── subject/SubjectDoc   # 索引文档：id/type/name/nameCn/infobox/summary/nsfw/airTime/cover/tags
   └── SubjectHint       # 命中视图（含高亮片段）

server/search/
├── IndicesService(impl)                # 索引生命周期（初始化/重建）
├── IndicesEndpoint                     # POST indices/subject(重建) / GET indices/subject(搜索)
├── IndicesInitializer                  # 启动时全量建索引（可配置开关）
├── IndicesConfiguration / IndicesProperties
└── subject/
    ├── LuceneSubjectSearchService      # ★ Lucene 实现
    ├── ReactiveSubjectDocConverter     # 实体→文档转换
    └── SubjectEventListener            # 条目/标签变更→增量更新索引
```

### 9.2 Lucene 实现要点

```
Analyzer: IKAnalyzer(true)（中文智能分词）
索引目录: {work-dir}/indices/subjects (FSDirectory)
字段策略: name/nameCn/summary/infobox → TextField(分词, 可高亮)
         type/nsfw/airTime/tags/cover → StringField/StoredField(精确, 存值)
搜索: QueryParser + 关键词 → IndexSearcher → Sort（按需）→ 前N条
高亮: Highlighter(QueryScorer + SimpleHTMLFormatter + SimpleFragmenter(MAX=100))
安全: 匿名用户搜索时过滤 nsfw=true；JSoup 清洗摘要防 XSS
索引维护: 事件驱动增量（add/update/delete/tagChange）+ 全量重建接口兜底
```

---

## 10. 插件系统详细设计

### 10.1 生命周期状态机（PF4J PluginState）

```
RESOLVED ──start──▶ STARTED ──stop──▶ STOPPED
   ▲                    │
   └───────◀────────────┘
   │
   ├──disable──▶ DISABLED（持久化，跳过自动启动）
   └──异常─────▶ FAILED（错误记录，可重试）
```

### 10.2 核心类

```
server/plugin/
├── IkarosPluginManager        # PF4J PluginManager 子类：加载/启动/停止/禁用/状态聚合
├── IkarosJarPluginLoader      # 生产 JAR 加载
├── IkarosDevelopmentPluginLoader / FixedPathDevelopmentPluginRepository  # 开发模式
├── YamlPluginLoader / YamlPluginFinder / YamlPluginDescriptorFinder      # plugin.yaml 描述
├── IkarosExtensionFinder / ExtensionComponentsFinder / IkarosExtensionFactory
├── PluginApplicationContext / PluginApplicationContextRegistry / PluginApplicationInitializer
├── SharedApplicationContext / SharedApplicationContextHolder             # 共享上下文
├── PluginConfiguration / PluginProperties（plugins-root/system-version/runtime-mode）
├── BasePluginFactory
└── listener/ 插件状态事件监听

api/plugin/
├── Plugin / BasePlugin        # 插件基类（插件主类继承）
├── IkarosExtensionPoint       # 所有扩展点必须继承
├── AllowPluginOperate         # 标记接口：允许插件操作的内部能力
├── PluginConst / 事件(event/): PluginAwareEvent / PluginConfigMapCreate/Update/ChangeEvent
└── custom/Plugin              # 插件自定义对象
```

### 10.3 扩展点注册与发现

```
插件 jar 内声明 plugin.yaml（id/version/core 版本/入口类）
加载流程：IkarosJarPluginLoader 解压 → YamlPluginFinder 读描述 → 校验 system-version
扩展发现：IkarosExtensionFinder 扫描实现 IkarosExtensionPoint 的类
         → ExtensionComponentsFinder 注册到宿主 Spring 容器
         → 按扩展点类型注入到使用方（如驱动Fetcher列表）
配置：ConfigMap（每插件一个），变更发布 PluginConfigMapChangeEvent
```

### 10.4 插件端点聚合

```
插件实现 CustomEndpoint → PluginCompositeRouterFunction 收集
  → 注册到 /apis/** 下（插件路由隔离 + 权限校验）
自定义 Scheme：插件声明 @Custom(group/version/kind) 实体
  → CustomSchemeManager 建表/索引 → CustomEndpointsBuilder 生成 CRUD 端点
  → 数据存 custom + custom_metadata（bytea 值）
```

---

## 11. 音乐模块详细设计（v1.2）

### 11.1 设计决策

- 复用既有领域模型：**专辑 = Subject(type=MUSIC)**，**歌曲 = Episode**，**歌单 = EpisodeList**。
- 新增轻量 `Music`（专辑视图）与 `Song`（歌曲视图）DTO 面向 API。

### 11.2 端点

| 端点 | 说明 |
|------|------|
| GET `/api/v1/music/albums/{page}/{size}` | 专辑分页 |
| GET `/api/v1/music/album/{id}` | 专辑详情 |
| POST/PUT/DELETE `/api/v1/music/album`、`/album/{id}` | 专辑增删改 |
| GET `/api/v1/music/album/{id}/songs` | 专辑曲目 |
| POST/PUT/DELETE `/api/v1/music/song`、`/song/{id}` | 歌曲增删改 |
| GET `/api/v1/music/search/{keyword}/{page}/{size}` | 专辑搜索 |

### 11.3 Subsonic 对接

```
SubsonicRouter（RouterFunction，路径 /rest/{action}）
  → 解析操作名 → DefaultSubsonicService.xxx()
  → SubsonicResponseBody（XML 结构兼容，v1.2 新增 SubsonicResponse 模型）
鉴权：Subsonic 用户/密码（或 token+salt）→ authenticate() → SubsonicContext
能力映射：
  音乐浏览  ← MusicService（专辑/歌曲）
  音频流    ← AttachmentService 流接口（stream）
  封面      ← 专辑封面附件（getCoverArt）
  歌单      ← EpisodeList（getPlaylists/getPlaylist/create/delete）
  scrobble  ← EpisodeCollection 进度回写
```

---

## 12. 缓存/任务/通知/设置模块详细设计

### 12.1 缓存

```
注解：@MonoCacheable/@MonoCacheEvict/@FluxCacheable/@FluxCacheEvict
切面：CacheAspect（@ConditionalOnProperty ikaros.cache.enable=true）
键：SpEL 表达式（如 #searchCondition.toString()）
管理器：ReactiveCacheManager
  ├─ MemoryReactiveCacheManager（进程内）
  └─ RedisReactiveCacheManager（ikaros.cache.redis.*，expiration-time 默认3天）
应用示例：AttachmentServiceImpl.listEntitiesByCondition @MonoCacheable("attachment:entities:")
         save/saveEntity @MonoCacheEvict（先删后写防脏读）
```

### 12.2 任务系统

```
TaskEntity: name/status(CREATE→RUNNING→FINISH|CANCEL|FAIL)/start_time/end_time/total/index/fail_message
TaskService(impl)：创建/进度更新/完成/失败
TaskEndpoint：GET /task/id/{id}、/task/process/{id}、/tasks/condition
线程池：TaskConfiguration（core=4, max=40, queue=10000, keep-alive 可配）
插件任务：PluginTask 接口，插件注册后台任务
```

### 12.3 邮件通知

```
MailService：SMTP 配置化发送（HTML/文本）
NotifyEndpoint：POST /notify/mail/test（控制台"测试邮件"按钮）
使用场景：剧集更新通知（监听剧集更新事件，配合第三方订阅插件）
```

### 12.4 系统设置

```
SettingService(impl)：key-value 设置项（JSON 存储）
SystemSettingInitListener：启动时写入默认设置
控制台设置页：全局 header/footer 配置（CustomScheme 或设置项）
```

---

## 13. 数据库表结构（DDL 汇总）

> 完整 DDL 见 `server/src/main/resources/db/migration/`（Flyway 版本化）。以下为核心表字段速查。

### 13.1 用户与权限

| 表 | 关键字段 | 约束 |
|----|----------|------|
| ikuser | username/password/nickname/avatar/email/telephone/site/introduce/enable/non_locked | username UK |
| role | name/description/parent_id | - |
| authority | allow/type/target/authority | - |
| ikuser_role | user_id/role_id | (user_id,role_id) UK |
| role_authority | role_id/authority_id | (role_id,authority_id) UK |
| ikuser_totp | user_id/secret/enabled | user_id UK, FK→ikuser |

### 13.2 内容

| 表 | 关键字段 | 约束 |
|----|----------|------|
| subject | type/name/name_cn/cover/infobox/summary/nsfw/air_time/score | BaseEntity(ol_version) |
| episode | subject_id/name/name_cn/description/air_time/ep_group/sequence(real) | (subject_id,ep_group,sequence,name) UK |
| subject_relation | subject_id/relation_type/relation_subject_id | - |
| subject_sync | subject_id/platform/platform_id/sync_time | (platform,platform_id) UK |
| episode_sequence_regular | name/regex/ep_group/sequence/priority/description/enabled | - |
| episode_list | name/name_cn/cover/description/nsfw | 歌单 |
| episode_list_episode | episode_list_id/episode_id | 歌单-歌曲 |
| tag | type/master_id/name/user_id/color | - |

### 13.3 收藏

| 表 | 关键字段 | 说明 |
|----|----------|------|
| subject_collection | user_id/subject_id/type/main_ep_progress/is_private/comment/score | 条目收藏 |
| episode_collection | user_id/subject_id/episode_id/finish/progress/duration/update_time | 剧集进度 |
| episode_list_collection | user_id/episode_list_id/update_time | 歌单收藏 |

### 13.4 附件

| 表 | 关键字段 | 说明 |
|----|----------|------|
| attachment | parent_id/type/url/path/fs_path/name/size/update_time/modified_time/deleted/driver_id/sha1 | 附件 |
| attachment_driver | enable/d_type/d_name/mount_name/remote_path/d_order/d_comment/refresh_token/access_token/expire_time/root_dir_id/user_id/avatar/space_total/space_use | 驱动 |
| attachment_reference | type/attachment_id/reference_id | 引用 |
| attachment_relation | attachment_id/type/relation_attachment_id | 关系(VIDEO_SUBTITLE) |

### 13.5 其它

| 表 | 说明 |
|----|------|
| task | 后台任务（状态/进度/失败信息） |
| directory_binding_workflow | 绑定工作流（task_id/directory_id/subject_id/status/current_step/step_statuses/fail_message） |
| person / character / subject_person / subject_character / person_character | 人物/角色体系 |
| custom / custom_metadata | 自定义 Scheme 实体（custom: c_group/version/kind/name UK） |

---

## 14. 关键时序图

### 14.1 驱动附件流式播放

```
Console/App ──GET /api/v1/attachment/stream/id/{id} (Range)──▶ AttachmentEndpoint
   │                                                              │
   │                                                              ▼
   │                                    AttachmentService.getStreamById(id)
   │                                          │ 驱动? ──是──▶ AttachmentDriverFetcher
   │                                          │                 .getSteam(att,start,end)
   │                                          └─否──▶ 本地文件 Flux<DataBuffer>
   │                                                              │
   ◀──────────────── 206 Partial Content + Content-Range ─────────┘
```

### 14.2 目录绑定

```
Console ──POST /api/v1/binding/directory──▶ DirectoryBindingEndpoint
   │                                            │
   │                                            ▼
   │                            DirectoryBindingService.start(dirId, platform)
   │                                            │
   │                                            ▼
   │                            DirectoryBindingChain.execute(ctx)
   │                              ├─ step10 ParseDirectoryName ─┐
   │                              ├─ step20 FindSubjectInfo     │
   │                              ├─ step30 FetchAndCreate      │ (失败逆序回滚)
   │                              ├─ step40 CreateTags          │
   │                              ├─ step50 ListFiles           │
   │                              ├─ step60 SyncPlatform        │
   │                              ├─ step70 BindEpisodes ──▶ EpisodeSequenceRegularChain
   │                              └─ step75 ProcessSp          ─┘
   │                                            │
   │                                            ▼
   │                            workflow 持久化（directory_binding_workflow）
   ◀── workflow {status, stepStatuses, subjectId} ──┘
   Console ──GET /api/v1/binding/workflow/{id}──▶ 轮询状态/失败原因
```

### 14.3 TOTP 登录

```
Client ──POST /security/auth/token/jwt/apply {u,p}──▶ JwtAuthenticationProvider
   │                                                  │ 密码校验
   │                                                  ▼
   │                                       用户启用TOTP?
   │                                          ├─是: 校验totpCode（RFC6238）
   │                                          │    ├─ 无/错 → 401 {"needTotp":true}
   │                                          │    └─ 对 → 签发
   │                                          └─否: 直接签发
   ◀── {accessToken, refreshToken} ───────────┘
后续请求：Authorization: Bearer accessToken → JwtAuthenticationFilter 验证
过期：PUT /security/auth/token/jwt/refresh {refreshToken} → 新token对
```

---

## 15. 测试设计

| 层级 | 覆盖点 | 示例 |
|------|--------|------|
| Repository 测试 | R2DBC CRUD/唯一约束/索引 | AttachmentRepositoryTest |
| 服务测试 | 领域逻辑/缓存/事件 | DefaultAttachmentDriverMountServiceTest、MusicServiceTest、DefaultCollectionServiceTest |
| 安全测试 | 认证/授权/越权 | TotpServiceTest、LogoutSuccessHandlerTest、UserServiceImplTest |
| 工具测试 | 加密/JSON/路径/时间/随机 | AesEncryptUtilsTest、PathUtilsTest、SqlUtilsTest、TimeUtilsTest |
| 搜索测试 | 索引构建/查询/高亮 | LuceneSubjectSearchServiceTest |
| 工作流测试 | 绑定链/回滚/正则链 | SubjectOperatorsTest、EpisodeSequenceRegular 相关 |
| 集成测试 | Testcontainers PG | 端点级集成 |

---

## 16. 附录

### 16.1 文档地图

```
docs/
├── Product-Requirements-Document.md   # 产品需求（功能/非功能需求，FR/NFR 编号）
├── High-Level-Design.md              # 概要设计（架构/模块/数据/部署/接口总览）
├── Low-Level-Design.md               # 详细设计（类级设计/表结构/时序/测试，本文档）
└── diagrams/   # drawio 架构图（plugin-architecture / plugin-loading-flowchart）
```

### 16.2 待完善项（TODO）

- [ ] 控制台前端组件级设计（Vue 组件树、状态管理 Pinia store 划分）
- [ ] API 客户端生成规范（packages/api-client 与 OpenAPI 契约同步机制）
- [ ] 主题系统渲染引擎设计（默认主题模板结构、插件主题约定）
- [ ] WebClient 三方平台适配器规范（重试/限流/缓存策略）
- [ ] 性能基准（附件刷新、搜索 P99 的压测方案）
