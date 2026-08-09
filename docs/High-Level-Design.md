# Ikaros 概要设计文档（HLD）

| 项目 | 内容 |
|------|------|
| 产品名称 | Ikaros（Ίκαρος） |
| 文档类型 | 概要设计（High-Level Design） |
| 文档版本 | v1.0 |
| 编写日期 | 2026-08-08 |
| 代码版本基线 | 1.2.1（`bbccbf32`） |
| 关联文档 | [Product-Requirements-Document.md](./Product-Requirements-Document.md)、[Low-Level-Design.md](./Low-Level-Design.md) |

---

## 1. 设计目标

### 1.1 架构目标

| 目标 | 说明 |
|------|------|
| 高并发响应式 | 全栈非阻塞（WebFlux + R2DBC），支撑海量附件与条目的低延迟访问 |
| 可插拔扩展 | 插件化架构（PF4J），本体最小化，能力通过扩展点注入 |
| 数据驱动 | 元数据、条目、剧集、附件引用关系统一建模，支持三方平台同步 |
| 可自托管 | 单 Jar 可运行、Docker 一键部署，数据落在用户自有环境 |
| 安全优先 | 认证（JWT/2FA）+ 授权（RBAC）+ 路径/SSRF/注入防护多层防线 |
| 可观测可维护 | Actuator 全端点、日志滚动、任务可追踪、索引可重建 |

### 1.2 设计约束

- **运行环境**：JDK 21+，PostgreSQL 18+（R2DBC 驱动），可选 Redis。
- **构建**：Gradle 8.14.5 多模块（api / server / console），Checkstyle 9.3 强制规范。
- **前端**：Vue 3 + Vite + pnpm workspaces（shared 与 api-client 为内部包）。
- **协议**：REST（`/api/v1/**`）+ Subsonic 兼容（`/rest/**`）+ 静态资源（`/static/**`）。
- **版本兼容**：插件必须声明兼容的 Core 版本（当前 1.2.1）。
- 完整技术栈版本详见 [PRD 6.1 技术栈](./Product-Requirements-Document.md#61-技术栈)。

---

## 2. 系统架构

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端层 (Clients)                        │
│  Web Console (Vue3 SPA) │ Windows/Android App │ Subsonic 客户端  │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTPS / HTTP
┌───────────────────────────────┴─────────────────────────────────┐
│                       接入层 (Security)                          │
│  SecurityWebFilterChain：Basic / Form / JWT / OAuth2 / TOTP      │
│  匿名用户 → RequestAuthorizationManager(RBAC) → 端点路由         │
├───────────────────────────────┬─────────────────────────────────┤
│                    路由层 (Router / Endpoint)                    │
│  CoreEndpointsBuilder (函数式路由 + SpringDoc)                   │
│  CustomEndpointsBuilder (自定义Scheme动态端点)                   │
│  PluginCompositeRouterFunction (插件端点)                        │
│  SubsonicRouter (/rest/** 协议分发)                              │
├───────────────────────────────┼─────────────────────────────────┤
│                    领域服务层 (Core Services)                    │
│  Subject/Episode/Collection/Attachment/Tag/Binding/Music/...     │
│  ★ 服务接口(api模块) + 默认实现(server模块, XxxService/Default)  │
├───────────────────────────────┼─────────────────────────────────┤
│                    横切能力层 (Infrastructure)                   │
│  缓存(CacheAspect: 内存/Redis) │ 任务(Task) │ 事件(Event)        │
│  Lucene搜索 │ 邮件通知 │ WebClient │ 全局异常处理                 │
├───────────────────────────────┼─────────────────────────────────┤
│                      数据访问层 (Store)                          │
│  R2DBC Repository (Spring Data R2DBC + 响应式 R2dbcTemplate)     │
├───────────────────────────────┼─────────────────────────────────┤
│                       数据存储层                                 │
│  PostgreSQL 18 │ Redis(可选) │ Lucene索引(工作目录/indices)      │
│  文件系统(附件) │ 工作目录(~/.ikaros: plugins/themes/statics)    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块架构（Gradle 多模块）

```
ikaros
├── api/          ★ API契约层（独立可被插件依赖）
│   ├── constant/     常量（OpenApi/Security/String/File/App）
│   ├── core/         领域模型与操作接口（subject/episode/collection/attachment/
│   │                 tag/authority/role/user/meta/music/subsonic/binding/...）
│   ├── custom/       自定义Scheme模型（Custom注解/客户端/异常）
│   ├── endpoint/     端点接口（Endpoint/CustomEndpoint）
│   ├── infra/        工具（utils）与异常体系（exception）
│   ├── plugin/       插件API（BasePlugin/ExtensionPoint/事件/常量）
│   ├── search/       搜索契约（SearchParam/Result/SubjectDoc/Hint/Service）
│   ├── store/        枚举（enums）与仓库接口约定
│   └── wrap/         统一包装（CommonResult/PagingWrap）
│
├── server/        ★ 服务端实现（Spring Boot 4 应用）
│   ├── core/         领域服务实现（subject/attachment/binding/music/subsonic/...）
│   │                 ├── <domain>/endpoint    函数式路由端点
│   │                 ├── <domain>/service     服务接口+impl实现
│   │                 ├── <domain>/listener    事件监听
│   │                 └── <domain>/handler     责任链步骤/处理器
│   ├── security/     认证授权（basicauth/formlogin/jwt/oauth2/totp/logout）
│   ├── plugin/       PF4J 插件管理（加载器/扩展点发现/上下文隔离）
│   ├── search/       Lucene 索引实现
│   ├── cache/        缓存抽象（Aspect + 内存/Redis Manager）
│   ├── store/        实体（entity）+ 仓库（repository）
│   ├── custom/       自定义Scheme运行时
│   ├── console/      Console 静态资源托管
│   ├── theme/        主题服务
│   ├── config/       R2DBC/任务/异步/调度/全局异常等配置
│   └── resources/db/migration/   Flyway 风格 SQL 迁移
│
├── console/       ★ Web 控制台（Vue 3 SPA，构建产物嵌入 bootJar）
│   └── packages/     shared（共享类型/菜单）/ api-client（生成的API客户端）
│
├── platform/      平台相关模块
└── config/        Checkstyle 等构建配置
```

### 2.3 关键技术决策（ADR 摘要）

| 决策 | 方案 | 理由 |
|------|------|------|
| ADR-01 响应式栈 | Spring WebFlux + R2DBC | IO 密集场景（流媒体/大目录扫描）下高并发、低资源占用 |
| ADR-02 函数式路由 | RouterFunction + SpringDoc 编程式文档 | 与 WebFlux 契合，端点定义集中、便于插件聚合 |
| ADR-03 插件框架 | PF4J（org.pf4j） | 成熟、类加载器隔离、状态机完整（RESOLVED→STARTED→STOPPED/DISABLED/FAILED） |
| ADR-04 数据库迁移 | db-migration（Flyway 风格版本化 SQL） | 无缝升级、破坏性变更受控（v1.1 起全面 PG） |
| ADR-05 搜索 | Lucene + IKAnalyzer（中文分词） | 毫秒级全文检索，索引落盘工作目录，支持重建 |
| ADR-06 缓存 | 注解切面（Mono/Flux Cacheable/Evict）+ 内存/Redis 可切换 | 对业务代码侵入小，可开关（ikaros.cache.enable） |
| ADR-07 ID 生成 | UUID v7（时间有序）为主键默认值 | 分布式友好、索引性能优于 UUID v4 |
| ADR-08 附件驱动 | Fetcher 扩展点 + 挂载服务 | 本地/WebDAV/插件自定义统一抽象，安全校验集中 |
| ADR-09 授权模型 | 权限目标字符串（Authority Target）+ 角色绑定 | 细粒度到接口路径，MASTER 超管兜底 |

---

## 3. 核心设计模式与机制

### 3.1 契约分层（API-First）

- `api` 模块定义**所有领域模型、操作接口（Operate）、插件扩展点**，`server` 模块提供默认实现。
- 插件依赖 `api` 模块而非 `server` 模块 → 编译期隔离，版本通过 `plugin.system-version` 校验。
- 服务实现命名规范：`XxxService`（接口）+ `DefaultXxxService`（实现），全量中文 Javadoc。

### 3.2 事件驱动（Event-Driven）

| 事件 | 触发场景 | 监听器动作 |
|------|----------|-----------|
| 条目创建/更新 | 条目保存 | 重建 Lucene 索引；缓存驱逐 |
| 条目标签更新 | 标签变更 | 重建对应条目索引 |
| 条目封面变更 | 封面字段更新 | 清理旧封面引用（SubjectCoverChange） |
| 剧集附件引用新增 | 绑定资源 | 自动匹配同名视频字幕文件并建立 VIDEO_SUBTITLE 关系 |
| 附件驱动启用/禁用 | 驱动开关 | 挂载/卸载目录、刷新附件树 |
| 剧集更新 | 剧集保存 | 邮件更新通知（配合蜜柑计划等插件） |
| 插件配置变更 | ConfigMap 更新 | PluginConfigMapChangeEvent 通知插件 |
| 条目收藏新增 | 收藏条目 | 自动创建该条目全部剧集的默认收藏 |

### 3.3 责任链（Chain of Responsibility）

两处典型应用：

1. **目录绑定工作流**（`DirectoryBindingChain`）—— 见 3.4。
2. **剧集序号解析**（`EpisodeSequenceRegularChain`）—— DB 规则 + 插件规则（`EpisodeSequenceRegularPluginHook`）合并，按 `priority` 降序尝试，匹配成功即返回（分组/序号），通用兜底规则优先级最低（`Integer.MIN_VALUE`）。

### 3.4 目录绑定工作流（半自动导入）

```
POST /api/v1/binding/directory  ──▶ DirectoryBindingService
                                      │
                                      ▼
                           DirectoryBindingChain.execute(ctx)
   ┌────────────────────────────────────────────────────────────┐
   │ order 10  ParseDirectoryNameStep       解析目录名→关键词/标签  │
   │ order 15  CleanDirectoryNameStep       清洗目录名            │
   │ order 20  FindSubjectInfoStep          本地查重               │
   │ order 30  FetchAndCreateSubjectStep    拉取元数据→创建条目     │
   │ order 40  CreateSubjectTagsStep        创建条目标签           │
   │ order 50  ListFilesStep                列出目录附件           │
   │ order 60  SyncSubjectByPlatformIdStep  平台ID同步(如Bangumi)  │
   │ order 70  BindEpisodesStep             批量匹配剧集+绑定附件   │
   │ order 75  ProcessSpSubdirectoriesStep  处理SP子目录           │
   └────────────────────────────────────────────────────────────┘
     │ 任一步骤 FAIL
     ▼
   rollbackCompletedSteps：按完成顺序逆序调用 rollback()
```

- 上下文 `DirectoryBindingContext` 贯穿全链：目录信息 → 条目 → 附件列表 → 剧集/引用/标签创建结果，并记录 `stepResults` / `stepErrors`。
- 内置步骤 order 用 10/15/20... 留出间隙，插件可插入自定义步骤。
- 工作流状态持久化至 `directory_binding_workflow` 表，关联 `task`。

### 3.5 缓存机制

```
@MonoCacheable(value="attachments:", key="#searchCondition.toString()")
Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(...)
```
- 注解切面 `CacheAspect`（`ikaros.cache.enable=true` 时生效）。
- 支持 `MonoCacheable/FluxCacheable` 与 `MonoCacheEvict/FluxCacheEvict`。
- Key 通过 **SpEL** 表达式计算；缓存名 + key 组成最终键。
- Manager 抽象：`ReactiveCacheManager` → `MemoryReactiveCacheManager`（Caffeine/ConcurrentHashMap）或 `RedisReactiveCacheManager`，`ikaros.cache.type=memory|redis` 切换。
- **写路径**：保存方法标注 `@MonoCacheEvict`，先删缓存后提交数据（避免脏读）。

### 3.6 安全机制

```
请求 ─▶ SecurityWebFilterChain (apiFilterChain)
         │
         ├─ 认证链：BasicAuthenticationFilter / FormLogin / JwtAuthenticationFilter
         │         ├─ JwtAuthenticationProvider（校验access token）
         │         └─ TOTP 分步认证（凭据→验证码，RFC6238 HMAC-SHA1 30s 6位）
         │
         ├─ 授权：RequestAuthorizationManager
         │         ├─ 从 authority 表加载权限目标(API路径模式)
         │         ├─ MASTER 角色 → 全路径放行
         │         └─ 匿名用户 → 仅公开端点（如非NSFW搜索）
         │
         └─ 会话：logout → LogoutSuccessHandler；CSRF/CORS 配置化
```

- 双令牌：Access Token（默认 3 天）+ Refresh Token（默认 3 个月），`/security/auth/token/jwt/apply|refresh`。
- 密码存储：`PasswordEncoderFactories.createDelegatingPasswordEncoder()`（BCrypt 等前缀可扩展）；`password` 字段 `@JsonIgnore`。
- 路径安全（附件）：可信根目录校验 + 符号链接防护 + 动态目录解析白名单 + fsPath 校验。
- 数据安全：SSRF 防护（封面下载 SsrfUtils）、SQL 注入防护（迁移导出）、Zip Slip 防护（CSV 导入解压）。

### 3.7 插件机制（PF4J）

```
IkarosPluginManager (PF4J PluginManager)
 ├─ 加载器：YamlPluginLoader / IkarosJarPluginLoader / FixedPathDevelopmentPluginRepository
 ├─ 扩展点发现：IkarosExtensionFinder / ExtensionComponentsFinder
 ├─ 生命周期：RESOLVED → STARTED → STOPPED / DISABLED / FAILED（插件状态持久化）
 ├─ 上下文隔离：PluginApplicationContext（每个插件独立 Spring 上下文）
 │             └─ 共享：SharedApplicationContext（插件可访问核心Bean）
 ├─ 配置：ConfigMap（创建/更新事件 → 插件感知）
 └─ 校验：版本兼容（system-version）、安装包安全（路径穿越/文件名注入）
```

插件可实现的扩展点（`IkarosExtensionPoint` 子接口）：详见 [PRD 4.10.2 插件扩展点](./Product-Requirements-Document.md#4.10.2-插件扩展点fr-plugin-02)。

---

## 4. 数据架构

### 4.1 核心 ER 关系（逻辑视图）

```
ikuser ─┬─< ikuser_role >─ role ─< role_authority >─ authority
        ├─< ikuser_totp
        ├─< subject_collection            (用户收藏条目)
        ├─< episode_collection            (用户剧集进度)
        └─< episode_list_collection       (用户收藏歌单/列表)

subject ─┬─< episode ─< episode_list_episode >─ episode_list(歌单)
         ├─< subject_relation             (条目关系: 前传/后传/OST/OVA...)
         ├─< subject_sync                 (三方平台映射: platform+platform_id)
         ├─< subject_collection           (被收藏)
         ├─< tag                          (条目标签, master_id=subject)
         ├─< subject_person >─ person ─< person_character >─ character
         ├─< subject_character >─ character
         └─< attachment_reference         (封面/资源引用)

attachment ─┬─< attachment_driver         (所属驱动)
            ├─< attachment_reference      (被引用: SUBJECT/EPISODE/USER_AVATAR)
            ├─< attachment_relation       (附件间关系: VIDEO_SUBTITLE)
            └─< tag                       (附件标签)

task ─< directory_binding_workflow       (绑定工作流)
episode_sequence_regular                 (剧集序号正则规则, 独立配置表)
custom + custom_metadata                 (自定义Scheme实体, GVK唯一)
```

### 4.2 存储策略

| 数据 | 存储 | 说明 |
|------|------|------|
| 结构化数据 | PostgreSQL 18 | 全部业务表，UUID v7 主键，乐观锁（ol_version） |
| 缓存 | 内存 / Redis（可选） | `ikaros.cache.*` 配置，默认关闭 |
| 搜索索引 | Lucene FSDirectory | `{work-dir}/indices/subjects`，可重建 |
| 附件文件 | 本地文件系统 / 网络驱动 | 由附件驱动托管，DB 存元数据+路径 |
| 插件/主题 | `{work-dir}/plugins`、`{work-dir}/themes` | 上传或仓库安装 |
| 静态资源/字体 | `{work-dir}/statics`（含 fonts） | `/static/**` 访问 |
| 日志 | 滚动文件 | 10MB/文件、总量1GB、保留2份历史 |

### 4.3 数据一致性策略

- **乐观锁**：`BaseEntity.olVersion` 字段，更新时校验版本，冲突返回失败（避免条目/剧集并发覆盖）。
- **唯一约束**：username、subject_sync(platform,platform_id)、role_authority(role_id,authority_id)、ikuser_totp(user_id)、episode(subject_id,ep_group,sequence,name)、custom(gvkn) 等。
- **级联清理**：删除条目 → 清理收藏/封面引用/关系；删除附件 → 校验引用后删除。
- **软删除**：核心实体 `delete_status` 逻辑删除标记。

---

## 5. 部署架构

### 5.1 部署形态

| 形态 | 方式 | 适用 |
|------|------|------|
| Docker | `ikarosrun/ikaros` 镜像 + PG/Redis 容器 | 主流推荐 |
| Fast Jar | `java -jar ikaros-server.jar`（含 Console 静态资源） | 单机快速部署 |
| 源码构建 | Gradle `buildFrontend` + `bootJar` | 开发/定制 |

### 5.2 运行时拓扑

```
[客户端] ──HTTPS──▶ [Ikaros :9999] ──R2DBC──▶ [PostgreSQL 18]
                        │  ──(可选)Redis──▶ [Redis]
                        │  ──FS──▶ {work-dir}: plugins/themes/indices/statics
                        │  ──FS──▶ 附件驱动目录(本地) / WebDAV(网络)
                        └  ──HTTP──▶ 三方平台(元数据/同步, 经WebClient)
```

### 5.3 关键配置项（`application.yaml`）

```yaml
ikaros:
  external-url: http://localhost:9999   # 外部访问地址
  work-dir: ${user.home}/.ikaros        # 工作目录
  show-theme: true                      # 是否启用主题
  cache: { enable: false, type: memory, redis: {...} }
  security.expiry: { access-token-day: 3, refresh-token-month: 3 }
  plugin: { runtime-mode: deployment, plugins-root: ..., system-version: 1.2.1 }
  task: { core-pool-size: 4, maximum-pool-size: 40, queue-count: 10000 }
server.port: 9999
spring.r2dbc: { url: r2dbc:pool:postgresql://localhost:5432/ikaros, ... }
```

---

## 6. 接口设计总览

### 6.1 路由约定

| 前缀 | 归属 | 说明 |
|------|------|------|
| `/api/v1/**` | Core 核心端点 | 函数式路由 + SpringDoc（tag: `v1/<domain>`） |
| `/apis/**` | 插件端点 | `PluginCompositeRouterFunction` 聚合 |
| `/rest/**` | Subsonic 兼容 | `SubsonicRouter` 按操作名分发 |
| `/static/**` | 静态资源 | 主题/字体等 |
| `/login` `/logout` | 认证 | Form Login / Logout |
| `/actuator/**` | 运维 | 健康/指标/Prometheus |
| `/` | Console | SPA 托管（HTML 门户链放行） |

### 6.2 核心端点矩阵（节选）

| 领域 | 端点 | 说明 |
|------|------|------|
| 认证 | POST `/security/auth/token/jwt/apply` | 申请 JWT（含TOTP校验） |
| 认证 | PUT `/security/auth/token/jwt/refresh` | 刷新令牌 |
| 用户 | GET/POST/PUT/DELETE `/api/v1/users*`、`/api/v1/user/me*` | 用户管理/个人信息 |
| 角色 | `/api/v1/role*`、`/api/v1/user/roles*` | 角色与授权 |
| 条目 | GET `/api/v1/subjects/{page}/{size}`、`/subjects/condition` | 分页/条件查询 |
| 条目 | POST/PUT/DELETE `/api/v1/subject`、`/subject/{id}` | 增删改 |
| 关系 | GET/POST/DELETE `/api/v1/subject/relation*` | 条目关系 |
| 同步 | POST `/api/v1/subject/sync/platform` | 平台同步 |
| 剧集 | `/api/v1/episode*`（含 `/sequence-regular/*`） | 剧集与正则规则 |
| 附件 | POST `/api/v1/attachment/upload`、`/fragment/*` | 上传/分片 |
| 附件 | GET `/api/v1/attachment/stream/id/{id}` | 流式读取(Range) |
| 附件 | PUT `/api/v1/attachment/driver*` | 驱动管理 |
| 附件 | PUT/DELETE `/api/v1/attachment/reference*`、`/relation*` | 引用/关系 |
| 绑定 | POST `/api/v1/binding/directory|directories` | 触发绑定工作流 |
| 绑定 | GET `/api/v1/binding/workflow/{id}` | 工作流状态 |
| 音乐 | `/api/v1/music/album*`、`/music/song*` | 专辑/歌曲 |
| Subsonic | `/rest/ping|getArtists|stream|...` | 协议兼容 |
| 通知 | POST `/api/v1/notify/mail/test` | 邮件测试 |
| 任务 | GET `/api/v1/task*`、`/tasks/condition` | 任务查询 |
| 搜索 | GET/POST `/api/v1/indices/subject` | 搜索/重建索引 |
| 元数据 | GET `/api/v1/metaInfo/search|subject` | 三方元数据查询 |

---

## 7. 非功能设计

### 7.1 性能设计

- 索引查询路径：Lucene 索引读取（FSDirectory）+ 高亮片段（Highlighter/IKAnalyzer）。
- 大数据量分页：条件查询走索引/条件过滤 + count，返回 `PagingWrap`。
- 附件目录刷新：增量同步（按 size+mtime 判断变更）、同驱动并发刷新合并、SHA-1 异步线程池。
- 缓存注解化：高频读接口（附件列表、剧集查询）默认接入缓存。
- 流媒体：Range 分段读取（`getSteam(att, start, end)`），视频拖动无需全量加载。

### 7.2 可用性设计

- 健康检查：`/actuator/health`（show-details 仅 MASTER 可见）。
- 任务可追踪：Task 表记录状态/进度/失败信息，工作流失败回滚。
- 日志滚动：按天+大小分割，防止磁盘占满。
- 迁移可回退：版本化 SQL + 发布前测试（Testcontainers）。

### 7.3 安全性设计

详见 3.6；另包括：CORS 配置化、CSRF 适配、Referrer-Policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)、X-Frame-Options(SAMEORIGIN)、匿名用户受限角色、2FA 强制可选。

### 7.4 可测试性设计

- 单测：JUnit 5 + Reactor Test + Testcontainers（PG）。
- 覆盖率门禁：Codecov CI 集成，目标 ≥80%。
- 测试分层：RepositoryTest（R2DBC）、ServiceTest、Endpoint 集成、安全/工具类专项（AesEncrypt、JsonUtils、PathUtils 等）。

---

## 8. 演进路线（设计视角）

| 阶段 | 架构重点 |
|------|----------|
| 已完成 | 响应式核心、附件驱动抽象、Lucene 搜索、插件体系（PF4J）、RBAC、缓存抽象、目录绑定工作流、Subsonic、音乐模块、2FA |
| 进行中 | 测试覆盖率提升、构建链简化（bootJar 自动打包前端）、国际化文本统一 |
| 规划 | 客户端（App）完善、插件生态丰富、更多协议兼容、多用户协作场景评估 |

---

## 9. 附录

### 9.1 关联文档

- [Product-Requirements-Document.md](./Product-Requirements-Document.md) — 产品需求文档（需求编号 FR-XXX / NFR-XXX 与本文档对应）
- [Low-Level-Design.md](./Low-Level-Design.md) — 详细设计文档（类图、接口签名、时序、表结构 DDL）
- [BUILD.md](../BUILD.md) — 编译与本地开发
- 架构图：`diagrams/plugin-architecture.drawio`、`diagrams/plugin-loading-flowchart.drawio`
