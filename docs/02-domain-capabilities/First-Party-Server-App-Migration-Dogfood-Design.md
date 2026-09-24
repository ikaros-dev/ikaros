# Ikaros V2 First-party Server App Migration / Dogfood Design

| 项目 | 内容 |
|---|---|
| 适用版本 | Ikaros V2 |
| 状态 | Draft / Migration Gate |
| 上位决策 | ADR-005 Platform / Server App / Client App |
| 关联设计 | Module Package Ownership、Implementation Roadmap、Media / Video / Anime Playback、App Runtime |

> 本文档定义现有第一方专业业务如何从“Server 内部模块”迁移成真正遵守 App Runtime Contract 的 First-party Server App。
>
> 首个 Dogfood 选择 Anime / Media Slice，产品级 Server App identity 为 `run.ikaros.anime`。目标不是一次重写 Media，而是用真实业务证明 App Runtime、Resource/Attachment 边界、Public App API、Task/Event admission 和独立 Client App 模型成立。

---

## 1. 为什么必须先 Dogfood

如果 App Runtime 只约束未来第三方 App，而第一方业务继续直接注入 Platform Repository、直接注册 Controller、不受 ENABLED state、使用隐藏内部 API，那么 App Runtime 只是外围框架。

第一方 App 必须先作为约束对象。

## 2. 首个 Dogfood：Ikaros Anime

稳定 identity：

```text
app_id = run.ikaros.anime
display_name = Ikaros Anime
```

独立 Client 产品：

```text
Ikaros Anime iOS
Ikaros Anime Android
Ikaros Anime Desktop (optional)
Third-party Anime Client
```

现有 `media-api` / `media` 可以作为过渡实现载体，但不能因此把 `run.ikaros.anime` 变成“只是一条标签”。

## 3. 首批范围

迁移：

- Anime；
- Season；
- Episode；
- Anime library；
- Episode playback query；
- watch/progress state；
- media association；
- media probe task；
- metadata refresh task；
- anime-related notification / event。

暂不强制迁：

- generic movie；
- generic TV；
- live media；
- full CDN / archive restore；
- music；
- photo；
- drive。

通用媒体交付、Attachment / Blob / Storage 仍属于 Platform/shared infrastructure。

## 4. Target Module Ownership

目标逻辑：

```text
run.ikaros.anime
├── anime-api
└── anime
```

过渡期允许现有 `media-api` / `media` 承载部分实现，但必须建立 package ownership test，禁止新 Anime 业务继续写入 Platform/Media 通用包。

最终：

- `anime-api`：Public Capability / Command / Query / Event / DTO；
- `anime`：Domain / Application / Persistence；
- `application`：composition only。

## 5. App-owned Data

目标 Schema：

```text
app_anime
```

Anime App 拥有：

```text
anime
season
episode
media_release
subtitle_binding
playback_session
playback_history
anime_app_configuration
```

Platform 继续拥有：

```text
resource
attachment
blob
placement
task runtime
event outbox
notification runtime
audit
identity / authorization
```

Anime App 只能保存稳定 Platform ID reference。

## 6. Resource Mapping

建议 Resource Type：

```text
run.ikaros.anime/anime
run.ikaros.anime/season
run.ikaros.anime/episode
```

不是所有 Anime 内部记录都必须成为 Resource。

`media_release`、`subtitle_binding`、`playback_state` 可以保持 App-owned Entity。

Resource 用于全局内容 identity、Tag/Collection/Relation、Attachment、Search、Share 与跨 App 引用。

### 6.1 Playback Progress 的唯一权威

当前主线已经存在 Platform-owned `ResourceProgressService`，`PersistentMediaPlaybackService` 会把视频播放位置写为 Resource Progress。

Dogfood 冻结：

```text
Anime-owned
├── PlaybackSession
├── PlaybackHistory
├── Episode / MediaRelease association
└── Anime-specific playback semantics

Platform Resource-owned
└── Generic current progress
    └── VIDEO_SECONDS / generic completion state
```

因此：

- `ResourceProgressService` 是“用户当前观看到哪里”的通用权威；
- Anime `PlaybackSession` 保存一次播放会话的状态、起止时间、Release 等；
- Anime `PlaybackHistory` 保存历史事实；
- Anime App 不再创建第二份 authoritative `current_position_seconds` / `completed` 状态；
- Anime-specific state 只保存 Platform Progress 无法表达的领域信息；
- Client 查询当前续播位置时，由 Anime Application API 组合 Session/Release 与 Resource Progress；
- Legacy Media API 与新 Anime API 必须调用同一 Application/Capability，不得双写两份 Progress。

跨 Owner 更新不通过共享数据库事务强行原子化。若一次播放更新同时涉及 Anime Session 与 Platform Progress：

1. 先校验 App/Resource/Release；
2. 通过公开 `resource-api` 更新 Progress；
3. 更新/结束 Anime Playback Session；
4. 使用稳定 idempotency / version token 避免重试产生倒退；
5. 任何补偿都通过公开 Capability，不直接操作 Resource Persistence。

在 Dogfood 完成前必须增加测试，证明新旧 API 查询同一 Resource 时得到一致续播位置。

## 7. Platform Permission

Anime Server App 首批申请：

```text
resource.read
resource.write
attachment.read
task.submit
notification.send
```

是否需要 `attachment.create`、`network.external`、`secret.reference.resolve` 取决于 metadata/import provider 是否属于 App 本体。

未使用的 Permission 不声明；First-party 不自动 grant。

## 8. Client Scope

首批：

```text
anime.library.read
anime.library.write
anime.playback
anime.progress.write
```

后续：

```text
anime.download
anime.metadata.manage
anime.admin
```

Client 不获得 `resource.write`、`attachment.read`、`task.submit`；它们属于 Server App → Platform。

## 9. Public App API

新 namespace：

```text
/api/apps/run.ikaros.anime/v1/...
```

首批建议：

```text
GET    /library
GET    /anime/{id}
GET    /anime/{id}/episodes
GET    /episodes/{id}
GET    /episodes/{id}/playback
PUT    /episodes/{id}/progress
POST   /library
DELETE /library/{anime_id}
```

最终 Operation 必须先进入 HTTP Registry / OpenAPI。

### 9.1 当前主线实现 Inventory

迁移前必须以当前代码为输入，而不是按目标模型重新猜现状。

当前已存在的主要入口：

| 当前 Route | 当前实现 | Dogfood 目标 |
|---|---|---|
| `/api/media` | `MediaCatalogController` | compatibility adapter → Anime Application API |
| `/api/media/resources/{resourceId}/releases` | `MediaReleaseController` | Anime Release Query/API |
| `/api/media/resources/{resourceId}/playback-source` | `PlaybackSourceController` | Anime playback source；底层 delivery 仍调 Storage Capability |
| `/api/media/playback/**` | `MediaPlaybackController` | Anime Playback Session API；current progress 继续由 ResourceProgressService 权威 |
| `/api/media/resources/{resourceId}/availability` | `MediaAvailabilityController` | Anime availability composition |
| `/api/media/releases/{releaseId}` | `MediaTechnicalMetadataController` | Anime-owned release/probe metadata |
| `/api/media/seasons/{seasonId}/restore-requests` | `MediaRestoreSeasonController`（media owner） | Media 展开 Attachment 集合，调用 StorageRestoreCapability；Storage 独立重新授权与预算检查 |

当前关键 Persistence / Migration：

```text
V202609030100__DDL_MEDIA_CORE_P0.sql
  media_subject / media_season / episode/release related facts

V202609030200__DDL_MEDIA_PLAYBACK_P0.sql
  media_playback_session / playback history

V202609030300__DDL_MEDIA_TECHNICAL_METADATA_P0.sql
  media_probe / technical metadata / subtitle related facts
```

Dogfood 迁移必须逐表形成：

```text
current table
→ target owner
→ target schema
→ migration/copy rule
→ read cutover
→ write cutover
→ old table retirement
```

不能只按“media 模块整体迁入 app_anime”处理，因为部分能力已经属于 Resource/Storage Owner。

### 9.2 Media Processing / Delivery 边界

冻结：

```text
Anime App owns
├── Work / Season / Episode semantics
├── MediaRelease association/selection
├── PlaybackSession / PlaybackHistory
├── Probe result needed by Anime domain
└── playback business orchestration

Resource Platform owns
└── generic Resource Progress

Storage Platform owns
├── Attachment / Blob / Placement
├── Delivery Provider / Binding
├── Delivery Grant / Lease
├── Restore truth
└── physical byte lifecycle
```

Transcode / Probe：

- Anime 可以提交 `anime.probe-media` / transcode-like Background Task；
- Worker 读取经授权的 Attachment；
- Probe result 可回写 Anime App-owned metadata；
- Derived Attachment 必须通过 Storage Capability 注册；
- Anime 不拥有 Storage 的 `media_delivery_provider / grant / lease / binding` 表；
- Storage Restore route 不因 Anime Dogfood 被搬进 `app_anime`。

## 10. 旧 Route 兼容

现有：

```text
/api/media/**
```

不得立即删除。

迁移模型：

```text
legacy media route
→ compatibility adapter
→ Anime Application API
```

禁止旧 Controller 继续访问旧 Repository、同时新 App Controller 使用新 Repository，形成两套业务逻辑。

兼容路由只能是 Adapter。

## 11. Compatibility Window

### Stage A

新 App API 上线，旧 Route 保持。

### Stage B

官方 Ikaros Anime Client 只使用 App API。

### Stage C

旧 Route 标记 Deprecated，并返回 Deprecation metadata。

### Stage D

内部 Console / tests / clients 无依赖后移除。

旧 Route 移除必须单独 Contract PR。

## 12. App Availability Gate

所有 Anime 业务入口统一经过：

```text
AppAvailability(run.ikaros.anime)
```

包括：

- Public App HTTP；
- legacy compatibility HTTP；
- Background Task submission；
- Scheduled work；
- Event consumer；
- realtime channel；
- internal cross-App invocation。

状态：

```text
ENABLED       allow
DISABLED      deny / stop admission
UNINSTALLED   deny
FAILED        deny
INCOMPATIBLE  deny
```

不能只隐藏 UI。

## 13. Disable Semantics

Disable 后：

- 新 API 返回 `app.disabled`；
- legacy route 同样不可用；
- 不接收新 Anime Task；
- Anime Event Consumer 停止；
- App-owned Scheduler 停止；
- App data 保留；
- Resource / Attachment 保留；
- Platform 已运行的通用 Storage/GC Task 不被强杀。

## 14. Background Task Migration

首批：

```text
anime.probe-media
anime.refresh-metadata
```

Task Runtime 属于 Platform。

Anime App 拥有 payload schema、business retryability、handler、progress semantics 和 final domain event。

Disable 后不再提交新 Anime Task；queued task 默认进入 blocked/app-disabled，而不是伪装 success。

## 15. Event Migration

Anime Event namespace：

```text
anime.anime.created
anime.episode.created
anime.progress.changed
```

Platform Event：

```text
platform.resource.created
platform.attachment.deleted
```

Anime 不能伪造 Platform Event。

旧 Media Event 若仍被消费，只能通过 Compatibility Translator 过渡，不能双写两套业务事实。

## 16. Search

Anime App 提供 Search Projection：

```text
title
aliases
season
year
genre
staff/cast references
```

Platform Search Runtime 管 index、ACL、checkpoint/rebuild。

App Disable 后历史 Search Document 可以保留，但结果进入业务详情前必须检查 App availability。

## 17. Notification

Anime App 拥有 notification type：

```text
anime.new-episode
anime.metadata-refresh-failed
```

Platform Notification Runtime 负责 storage/delivery/read/push。

Deep link 面向 Ikaros Anime Client，而不是主 Ikaros App 的 Anime tab。

## 18. Existing Data Migration

迁移现有 Anime/Media 数据：

1. inventory current tables；
2. 分类为 App-owned / Platform Resource+Attachment / projection-cache；
3. App-owned 数据迁入 `app_anime`；
4. 保持公开 UUID；
5. 保持 Resource / Attachment ID；
6. 不复制 Blob bytes；
7. migration verification；
8. 切读；
9. 切写；
10. 移除旧写路径。

禁止长期 dual-write。

## 19. Data Cutover

推荐：

```text
read old / write old
→ migration snapshot
→ short write freeze
→ delta migrate
→ switch app repository
→ verify
→ resume write
```

未来 Package App Migration 再迁入 App Runtime orchestration。

## 20. First-party Manifest

即使代码仍编译进 Server，也提供逻辑 Manifest：

```yaml
app_id: run.ikaros.anime
name: Ikaros Anime
version: server-build
distribution: BUILT_IN

platform:
  api:
    min: 2.0
    max: 2.x

public_api:
  majors: [1]

permissions:
  - resource.read
  - resource.write
  - attachment.read
  - task.submit
  - notification.send

scopes:
  - anime.library.read
  - anime.library.write
  - anime.playback
  - anime.progress.write

data:
  schema: app_anime
```

未来 BUILT_IN 转 Package 时不换 app_id。

## 21. Main Ikaros Client

主 Ikaros Client 不增加 Anime 页面。

它最多负责：

- 查看 Anime App 安装/启用状态；
- 管理权限；
- 提示打开/安装 Ikaros Anime Client；
- Auth Broker。

library/playback 在独立 Anime Client。

## 22. Official Anime Client

第一版最低能力：

- Instance connect；
- App Discovery；
- authorization；
- library list；
- anime detail；
- episode list；
- playback；
- progress sync。

它不直接调用 Resource/Attachment/Task API 重建 Anime 业务。

## 23. Dogfood Gate

在称 App Runtime “可用于业务 App”前，Anime 至少证明：

1. 业务 Controller 经过 App Availability。
2. Disable 后 API / Task / Event Consumer 停止 admission。
3. Anime 不跨 Owner Repository。
4. Platform Permission 实际生效。
5. Client Scope 实际生效。
6. 独立 Client 只调用 Anime Public API。
7. 旧 route 只是 compatibility adapter。
8. App-owned Schema 与 Platform Schema 分离。
9. App uninstall 不隐式删除 Resource/Attachment。
10. First-party 不使用 hidden bypass。
11. Generic current playback progress 只有 ResourceProgressService 一个权威来源。
12. PlaybackSession / PlaybackHistory 不被当作第二份 current progress truth。
13. Storage-owned delivery / restore tables 不迁入 app_anime。
14. 当前每个 /api/media/** route 都有明确 target adapter/API mapping。
15. Media 三组现有 migration 的每张表都有 target owner/cutover 决策后才允许删旧表。

## 24. 为什么不是先 Drive

Drive 与 Attachment/Blob/Storage、Sync、Quota、Trash、Revision 的边界更容易在首个 Dogfood 阶段混淆。

Anime 更适合先验证：

```text
Server App Domain
+ Resource
+ Attachment
+ Task
+ Event
+ Notification
+ Independent Client
```

Anime 成功后，Drive 作为第二个更强压力测试。

## 25. 后续 First-party 顺序

建议：

```text
1. Anime
2. Drive
3. Photos
4. Reading
5. Music
6. Accounting
7. Document / Productivity
```

每个 App 复用 Dogfood Checklist，而不是复制 Anime 领域模型。

## 26. Migration Completion

Anime Migration Done 必须同时满足：

```text
new App API is authoritative
AND official client uses new App API
AND app-owned persistence is isolated
AND lifecycle admission is enforced
AND permissions/scopes enforced
AND old route is only adapter or removed
AND no hidden platform repository access
```

仅新增 app_id 记录不算迁移完成。
