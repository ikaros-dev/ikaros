# Ikaros V2 Media / Video / Anime / Playback 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Media / Video / Anime / Playback 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md`、`Background-Task-Scheduler-Design.md`、`Sharing-Collaboration-Room-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 动画、影视与通用视频领域中的 Work、Season、Episode、Release / Media Version、Track、Subtitle、Media Probe、Playback Source、Transcoding、Playback Session 与播放进度的服务端边界。
>
> 本文档不把播放器 UI、FFmpeg、VLC、HLS 或 DASH 某一种实现当作领域模型。专业媒体领域负责“一个内容如何被组织和播放”，Storage 负责“字节在哪里”，客户端负责“如何解码和呈现”。

---

## 1. 设计目标

Media 子系统需要解决：

1. 动画、电视剧、电影和通用视频如何在统一 Resource 身份下保留专业结构。
2. Work / Season / Episode 之间如何建立强领域关系，而不是全部塞进 Generic Relation。
3. 一个 Episode 存在多个压制组、文件版本或来源时如何表达 Release / Media Version。
4. “原始版本”与“同一原始版本的 1080p / 720p 转码清晰度”如何严格区分。
5. 内嵌 Video / Audio / Subtitle Track 与外挂 Subtitle Attachment 如何统一暴露给播放器。
6. Media Probe 如何作为可重建技术元数据，而不是 Resource 业务真相源。
7. 原始媒体不可直接播放时如何生成 Derived Attachment / Adaptive Variant。
8. 转码如何通过 Background Task 执行、幂等、重试、取消和清理。
9. 播放前如何根据权限、Storage Availability、客户端能力和用户偏好选择可解释的 Playback Source。
10. Playback Session、Progress、History、Completed 如何分工。
11. 跨设备进度乱序更新如何避免旧状态覆盖新状态。
12. Together Room 如何复用 Media 权威播放状态而不让 Room 直接改 Media 私有表。
13. Download / Offline 如何与服务器播放源、Blob、客户端缓存保持边界。
14. Search、Analytics、AI、Automation 如何消费媒体事件。

核心原则：

> **Media Release / Version 表达“这是哪个源版本”；Playback Variant 表达“这个源版本可以用什么方式播放”。版本和清晰度不是同一个概念。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Media Work / Season / Episode；
- Movie / Standalone Video；
- Media Release / Source Version；
- Attachment 与 Episode / Video 的媒体角色关联；
- Media Probe Result；
- Video / Audio / Subtitle Track 技术描述；
- External Subtitle；
- Playback Source Resolution；
- Direct Play / Direct Stream / Transcode 语义；
- Playback Variant / Adaptive Rendition；
- Transcoding Job 的业务契约；
- Playback Session；
- Playback Progress / Completion；
- 媒体可用状态；
- Room 播放同步的 Media Capability；
- 媒体事件、可观测性与测试基线。

### 2.2 本子系统不负责

- Blob Placement / Replica / Archive Restore；
- 文件扫描、目录遍历和 Candidate Match；
- 客户端具体解码器；
- VLC / libmpv / ExoPlayer / AVFoundation 等 SDK 选型；
- HTTP Range 的系统级通用协议约定；
- Room Membership / Presence / Chat；
- 客户端本地下载数据库；
- 音乐专辑 / Artist 等音乐专业模型；
- Search Engine 实现；
- DRM 商业内容授权系统。

---

## 3. 核心不变量

1. **逻辑作品与媒体字节分离**：Work / Episode 不保存物理路径或 Object Key。
2. **Episode 等强领域关系由 Media 拥有**：`Episode -> Season -> Work` 不依赖自由 Generic Relation 才成立。
3. **一个 Episode 可以有零个、一个或多个 Release**：没有媒体文件也仍可以是合法的 Episode Resource。
4. **Release 与 Transcode Variant 分离**：不同压制组 / 不同源文件是 Release；720p / 1080p adaptive rendition 是 Variant。
5. **原始 Attachment 与 Derived Attachment 可追溯**：转码结果必须能追踪来源。
6. **Probe 是派生技术信息**：Probe 丢失后可重新从媒体 Attachment 生成。
7. **播放选择必须可解释**：系统不能静默选择 Missing、Corrupted 或用户无权访问的版本。
8. **播放权限必须在服务端验证**：知道 Blob URL 不意味着拥有媒体访问权限。
9. **Playback Session 不拥有用户长期进度真相**：长期 Progress 属于 User + Resource 状态或 Media 专业进度投影。
10. **旧设备不能无条件覆盖新 Progress**：进度更新必须有 version / sequence / event time 合并规则。
11. **转码是异步工作**：长耗时媒体处理不能占用普通 HTTP 请求线程等待完成。
12. **Derived Variant 可重建**：清理可重建转码不影响原始媒体。
13. **Room 控制调用 Media Command**：Room 不直接修改 Media 表或伪造 Progress。
14. **客户端能力参与选择但不决定业务权限**：Codec Support 只能影响播放方案，不能扩大 ACL。

---

## 4. 领域模型总览

```text
Media Work Resource
   ├── Season
   │    ├── Episode Resource
   │    │     ├── Media Release A
   │    │     │      ├── Source Attachment
   │    │     │      ├── Probe Result
   │    │     │      ├── Embedded Tracks
   │    │     │      ├── External Subtitle
   │    │     │      └── Playback Variants
   │    │     └── Media Release B
   │    └── Episode ...
   └── Metadata / Relations

Movie / Standalone Video Resource
   └── Media Release(s)
```

---

## 5. Media Resource Types

核心 Resource Type 可以包括：

- `anime`；
- `tv_series`；
- `movie`；
- `episode`；
- `video`。

是否额外引入 `season` Resource 取决于 Season 是否需要独立被：

- 搜索；
- 分享；
- 收藏；
- 关联 External Identity；
- 作为自动化目标。

推荐规则：

> Season 若具有稳定业务身份和独立生命周期，可以成为 Resource；如果只是 Work 内部组织节点，可以只作为 Media Entity。

不应为了“所有东西都是 Resource”强迫所有内部结构升级为 Resource。

---

## 6. Media Work

Media Work 表达一部作品。

例如：

- 一部动画；
- 一部电视剧；
- 一部电影；
- 系列视频集合。

专业字段可以包括：

- media kind；
- release / air status；
- original language；
- first air / release date；
- episode ordering policy；
- default season；
- professional metadata extension。

标题、Alias、External Identity、Tag、Collection 等通用能力仍由 Core Resource 提供。

---

## 7. Season

Season 用于组织 Episode。

至少可以表达：

- id；
- work id；
- season number；
- display title；
- sort order；
- episode count（可派生）；
- air / release date range；
- optional external identity；
- lifecycle / visibility。

约束：

- 同一 Work 内 season order 必须稳定；
- Season 移动到另一个 Work 是显式迁移，不是普通字段 PATCH；
- 删除 Season 时不能默认级联永久删除 Episode / Blob。

---

## 8. Episode

Episode 是可独立播放和记录进度的逻辑内容。

至少具有：

- Resource ID；
- work id；
- season id（可选）；
- episode number / index；
- absolute number（可选）；
- title；
- air date；
- expected duration（业务元数据，可空）；
- sort order；
- special flag / episode kind；
- lifecycle。

### 8.1 Episode Number

不能假设所有内容只有整数集数。

需要支持：

- 1、2、3；
- 0；
- 12.5；
- SP / OVA / OAD；
- 无正式编号的视频。

推荐分离：

- machine sort order；
- structured episode number；
- display label。

客户端不应通过解析展示字符串决定排序。

---

## 9. Movie / Standalone Video

Movie 和通用 Video 可以直接挂 Media Release，不要求伪造 Season / Episode。

```text
Movie Resource
   └── Release(s)

Video Resource
   └── Release(s)
```

统一播放器可以基于 `Playable Media Target` Capability 读取可播放目标，而不是要求所有内容先转换成 Episode。

---

## 10. Media Release / Version

Media Release 表达一个逻辑媒体目标的具体源版本。

典型差异：

- 不同压制组；
- Blu-ray vs Web；
- 不同剪辑版；
- Director's Cut；
- 不同语言配音源；
- 不同供应来源；
- 用户自行导入的两个独立文件。

至少包含：

- UUIDv7；
- target resource id；
- display name；
- source / release group；
- edition；
- source attachment id；
- release metadata；
- preference weight；
- availability summary；
- created_at / updated_at。

### 10.1 Release 唯一性

同一 Attachment 不应被无意义重复绑定为多个相同 Release。

但同一 Blob 可能因为业务来源不同形成不同 Attachment 语义，是否复用 Release 由明确导入规则决定。

---

## 11. Attachment Media Role

Media 与 Attachment 的关系需要明确 Role，例如：

- SOURCE_VIDEO；
- EXTERNAL_SUBTITLE；
- POSTER；
- THUMBNAIL；
- PREVIEW_SPRITE；
- CHAPTER_METADATA；
- TRANSCODE_OUTPUT；
- AUDIO_DESCRIPTION（未来）；
- OTHER_MEDIA_ASSET。

Role 不是 Attachment 自身类型；同一个 Attachment 在不同业务对象中的角色可以不同。

---

## 12. Media Probe

Probe 从 Source Attachment 提取技术信息。

Probe Result 可以包含：

- container；
- duration；
- bitrate；
- dimensions；
- frame rate；
- HDR / color metadata；
- video codec；
- audio codec；
- stream list；
- subtitle streams；
- chapters；
- attachment streams；
- probe tool version；
- probed_at。

### 12.1 Probe 是派生数据

Probe Result：

- 可以缓存；
- 可以重建；
- 工具升级后可以重新生成；
- 不直接替代用户元数据。

例如文件内 Title Tag 不应无条件覆盖用户为 Episode 设置的标题。

### 12.2 Probe Job

Probe 对大文件可能耗时，应允许作为 Background Task。

幂等键可基于：

```text
attachment_id + blob_hash + probe_profile_version
```

同一 Blob 未变化时不需要无控制重复 Probe。

---

## 13. Track

Track 描述媒体容器内可选择的流。

### 13.1 Video Track

至少包括：

- stream index / stable technical key；
- codec；
- profile / level；
- width / height；
- frame rate；
- bitrate；
- pixel format；
- color space；
- HDR metadata；
- default / forced flags。

### 13.2 Audio Track

至少包括：

- stream key；
- language；
- title；
- codec；
- channel count / layout；
- sample rate；
- bitrate；
- default flag；
- commentary / descriptive role（可选）。

### 13.3 Embedded Subtitle Track

至少包括：

- stream key；
- language；
- title；
- codec / format；
- forced；
- default；
- hearing impaired flag（可选）。

Track ID 可以是 Media 子系统稳定实体 ID，也可以是 `(probe/release + stable stream key)` 的内部标识；但不能仅依赖 UI 列表下标作为公共稳定身份。

---

## 14. External Subtitle

外挂字幕通过独立 Attachment 表达，并关联到 Release / Playable Target。

元数据至少包括：

- attachment id；
- language；
- title；
- format；
- source / provider；
- synchronization offset default；
- forced / hearing impaired；
- provenance。

规则：

- 字幕文件不直接以本地 `.ass` 路径作为领域身份；
- 同一字幕可由多个 Release 复用时需要明确引用关系；
- 用户字幕偏移是用户 / playback preference，不修改原字幕 Blob。

---

## 15. Playback Variant

Playback Variant 是从某个 Release / Source 解析出的具体播放方案。

类型可以包括：

- DIRECT_PLAY；
- DIRECT_STREAM / REMUX；
- TRANSCODED_FILE；
- HLS / DASH adaptive rendition；
- REMOTE_STREAM（明确受支持时）。

Variant 至少表达：

- source release；
- derived attachment（若有）；
- container；
- video / audio codec；
- resolution；
- bitrate；
- HDR / SDR；
- compatible capability profile；
- availability；
- derivation profile version。

### 15.1 Version ≠ Quality

例如：

```text
Release A: BDRip 4K HEVC
    ├── Direct Play 4K
    ├── HLS 1080p
    └── HLS 720p

Release B: WEB-DL 1080p AVC
    ├── Direct Play 1080p
    └── HLS 720p
```

Release A 与 B 是两个源版本。

1080p / 720p 是各自的播放 Variant。

播放器的“版本选择”和“清晰度选择”必须保持两个概念。

---

## 16. Derived Attachment 与 Transcode Profile

转码输出使用 Storage 的 Derived Attachment 能力。

Transcode Profile 至少描述：

- profile id / version；
- target container；
- video codec；
- audio codec；
- max resolution；
- bitrate policy；
- HDR policy；
- subtitle burn / passthrough policy；
- segment format；
- hardware acceleration hint（实现级）；
- compatibility class。

Profile 是平台配置 / Media 配置，不应把最终 FFmpeg CLI 作为公共领域契约。

---

## 17. Transcoding Job

典型流程：

```text
Resolve Playback Need
      ↓
No compatible variant
      ↓
RequestTranscode
      ↓
Background Task
      ↓
Media Transcode Handler
      ↓
read Source Attachment
      ↓
generate Derived Blob / Attachment
      ↓
register Playback Variant
      ↓
media.variant.ready Event
```

### 17.1 幂等

幂等键建议包含：

```text
source_blob_hash
+ transcode_profile_version
+ relevant track selection
```

同一结果已经存在且健康时应复用，而不是重复转码。

### 17.2 取消

取消转码：

- 终止 / 协作式停止当前 Task；
- 临时输出必须清理；
- 已完成并成功注册的 Derived Attachment 不因另一个等待者取消而被删除；
- 多个播放请求共享同一转码时需要 waiter / lease 语义，不能由任意一个客户端取消公共任务。

### 17.3 失败

错误分类至少包括：

- SOURCE_UNAVAILABLE；
- SOURCE_CORRUPTED；
- UNSUPPORTED_CODEC；
- TRANSCODER_FAILURE；
- TEMP_SPACE_EXHAUSTED；
- OUTPUT_VALIDATION_FAILED；
- CANCELLED。

错误不得统一成“播放失败”。

---

## 18. Direct Play / Direct Stream / Transcode

推荐定义：

### Direct Play

客户端可以直接读取当前 Source / Variant，容器和编码均兼容，不需要媒体内容转换。

### Direct Stream / Remux

媒体编码兼容，但容器或传输形式需要轻量重新封装。

### Transcode

至少一个主要轨道需要重新编码。

这些是播放策略，不应通过 Attachment Type 模糊表达。

---

## 19. Client Capability Profile

客户端请求播放时可以上报能力摘要：

- platform / client version；
- supported containers；
- video codecs；
- audio codecs；
- subtitle formats；
- HDR capabilities；
- max resolution；
- adaptive protocol；
- bandwidth hint；
- local downloaded variant IDs（必要时）。

服务端把该信息作为 Playback Resolution 输入。

安全原则：

> Client Capability 是不可信能力声明，可以影响选择，但不能绕过 Permission 或生成任意服务器命令。

---

## 20. Playback Source Resolution

播放请求推荐流程：

```text
User requests Playable Target
       ↓
Authenticate + Authorize
       ↓
Resolve target Episode / Movie / Video
       ↓
List allowed Releases
       ↓
Check Availability
       ↓
Apply user preference + client capability
       ↓
Select Release
       ↓
Select Direct / Variant / Transcode
       ↓
Create Playback Session
       ↓
Return Playback Descriptor
```

### 20.1 选择优先级

可考虑：

1. 用户明确选择的 Release；
2. 已下载 / 本地可用版本；
3. 用户默认 Version Rule；
4. Compatible Direct Play；
5. 已存在兼容 Variant；
6. 可接受的 Transcode；
7. 需要 Restore / Processing 时返回明确状态。

不得选择：

- MISSING；
- CORRUPTED；
- 无权访问；
- 已被用户明确禁用的 Release。

### 20.2 可解释结果

Playback Descriptor 应能解释：

- selected release；
- selected variant；
- direct / transcode；
- selected tracks；
- availability；
- fallback reason；
- expires_at（临时访问 URL 时）。

---

## 21. Playback Descriptor

对客户端返回的播放描述可以包含：

- playback session id；
- media target id；
- release id；
- variant id；
- stream URL / Attachment read reference；
- protocol；
- duration；
- video info；
- selectable audio tracks；
- selectable subtitles；
- chapter info；
- resume position；
- authorization expiration；
- server capabilities。

不应暴露：

- Storage Credential；
- 任意宿主文件路径；
- Object Storage Root Secret；
- 其他用户信息。

---

## 22. Playback Session

Playback Session 表示一次媒体消费会话。

至少包含：

- UUIDv7；
- user / principal；
- target resource id；
- release / variant；
- client / device summary；
- started_at；
- last heartbeat / activity；
- ended_at；
- current position；
- playback state；
- room id（如果属于 Together Room）；
- session version / sequence。

Session 是运行 / 历史事实，不是媒体文件本体。

### 22.1 状态

可以包括：

- STARTED；
- PLAYING；
- PAUSED；
- ENDED；
- COMPLETED；
- ABANDONED / EXPIRED。

不要求每次 pause 都同步产生数据库重写；可以节流聚合。

---

## 23. Progress

长期播放进度作用域：

```text
User + Playable Resource
```

通常 Episode 的 Progress 记录在 Episode Resource 上，而不是整个 Series Work。

至少需要：

- position；
- duration snapshot；
- percent（可派生）；
- completed；
- updated_at；
- source session id；
- version / sequence；
- device / client hint。

### 23.1 更新时机

客户端可以：

- 周期性节流提交；
- Pause 时提交；
- Seek 完成后提交；
- Exit / End 时提交；
- Offline 后重连补交。

服务端需要限频，不能要求每一帧都落数据库。

### 23.2 乱序保护

不能简单：

```text
last request wins
```

推荐考虑：

- server version；
- client base version；
- occurred_at；
- session id；
- explicit rewind intent；
- completed state。

例如用户在新设备已看到 20:00，旧离线设备后来上传 05:00，不应无条件回退。

但用户明确执行“从头播放 / 重置进度”时必须允许回退，因此“位置越大永远赢”也不正确。

建议使用显式 Intent：

- PROGRESS_FORWARD；
- SEEK；
- RESET；
- MARK_WATCHED；
- MARK_UNWATCHED；
- OFFLINE_REPLAY。

---

## 24. Completion

完成阈值必须是稳定产品规则，例如：

- 播放达到时长百分比；
- 或距离结尾小于阈值；
- 特殊超短内容采用单独规则。

阈值由服务端 / 产品统一配置，客户端不能各自决定。

完成后产生：

- `playback.completed`；
- Progress completed = true；
- Analytics Fact；
- 可选 next episode recommendation。

重复到达完成阈值不能重复制造相同 Completion 副作用。

---

## 25. Playback History

History 与 Progress 分离。

Progress 回答：

> 我现在看到哪里？

History 回答：

> 我什么时候、在哪个客户端、播放过什么？

History 可以记录 Session 摘要。

从 History 中删除一条记录不应：

- 删除 Resource；
- 删除 Blob；
- 自动清空当前 Progress，除非用户明确选择。

---

## 26. Availability

媒体播放可用性综合来自：

- Release；
- Source Attachment；
- Blob / Replica；
- Derived Variant；
- Transcode Task；
- Archive Restore。

对 UI 至少可归一化：

- AVAILABLE；
- CACHED；
- REMOTE；
- PROCESSING；
- RESTORING；
- MISSING；
- CORRUPTED；
- UNSUPPORTED（当前客户端 / 当前策略）。

Media 不复制 Storage 全部状态，只形成面向播放的可解释 Projection。

---

## 27. Archive Restore

当 Source Blob 位于冷存储：

```text
Play Request
    ↓
Storage Capability -> RESTORE_REQUIRED
    ↓
Request Restore Command
    ↓
Background Task
    ↓
Media Availability = RESTORING
    ↓
storage restored event
    ↓
Media playback becomes available
```

客户端应获得“正在恢复”而不是模糊 404 或无限 loading。

---

## 28. Preview / Thumbnail / Sprite

派生能力可以包括：

- poster / frame thumbnail；
- preview thumbnails；
- seek sprite；
- waveform（音频相关）；
- chapter preview。

这些均属于 Derived Attachment / 可重建资产。

生成任务：

- 异步；
- 幂等；
- 可清理；
- 不影响原始媒体。

---

## 29. Subtitle Processing

可以支持：

- encoding detection / normalization；
- format conversion；
- Web-compatible subtitle derivative；
- font attachment resolution；
- subtitle offset preference；
- AI translation / transcription（通过 AI 子系统）。

原字幕保持原始 Attachment；转换后生成 Derived Attachment。

AI 翻译字幕具有 Provenance，不能覆盖用户原始字幕文件。

---

## 30. Room / 一起看

Room 子系统负责成员与实时序列，Media 负责播放状态语义。

Together Room 的 Media State 可以包括：

- target Episode / Video；
- selected Release / policy；
- play / pause；
- anchor position；
- anchor server time；
- playback rate；
- state version。

### 30.1 Anchor Model

不建议服务器每 100ms 广播当前位置。

推荐：

```text
state = PLAYING
anchor_position = 120.0s
anchor_server_time = T
rate = 1.0
```

客户端在时刻 `now` 估算：

```text
position = anchor_position + (now - T) * rate
```

Pause / Seek / Rate Change 时创建新的权威 Anchor。

### 30.2 控制权限

Room 发出控制意图：

```text
Room Member
   ↓
ControlPlaybackIntent
   ↓ Room permission
Media Command
   ↓
Authoritative Media State
   ↓
Room Sequence Broadcast
```

Room 不能直接写 Media State Storage。

### 30.3 Resume Progress 与 Room

Room 中每个用户的长期个人 Progress 仍独立保存。

共同播放状态不等于大家拥有一条共享 Progress 记录。

---

## 31. Offline / Download

服务端区分：

- Streamable Variant；
- Downloadable Attachment / Variant；
- Client Cache；
- Explicit Download。

客户端本地“已下载”不是服务器 Blob Placement。

下载请求仍需要权限检查。

离线播放时：

- 播放不依赖服务器保持在线；
- Progress 写本地 Pending；
- reconnect 后按 Progress 冲突规则同步；
- Share / ACL 撤销后的离线副本失效策略由 Offline / Device Sync 设计进一步定义。

---

## 32. Command 契约

典型 Command：

- CreateMediaWork
- CreateSeason
- MoveSeason
- CreateEpisode
- ReorderEpisode
- AddMediaRelease
- RemoveMediaRelease
- AttachSourceMedia
- AttachExternalSubtitle
- RequestMediaProbe
- RequestTranscode
- CancelTranscodeRequest
- ResolvePlaybackSource
- StartPlaybackSession
- UpdatePlaybackProgress
- EndPlaybackSession
- MarkWatched
- MarkUnwatched
- ResetPlaybackProgress
- SelectPreferredRelease
- RequestArchiveRestore

所有 Command 必须遵守目标权限、幂等、并发与 Event 规则。

---

## 33. Query / Capability

建议能力：

- GetMediaWork
- ListSeasons
- ListEpisodes
- GetEpisode
- ListMediaReleases
- GetMediaProbe
- ListTracks
- ListSubtitles
- ResolvePlayableTarget
- GetPlaybackDescriptor
- GetPlaybackProgress
- GetContinueWatching
- ListPlaybackHistory
- GetMediaAvailability
- GetTranscodeStatus

跨子系统只返回公共 DTO。

---

## 34. Integration Event

建议：

- `media.work.created`
- `media.season.created`
- `media.episode.created`
- `media.release.added`
- `media.release.removed`
- `media.probe.completed`
- `media.variant.ready`
- `media.variant.failed`
- `playback.started`
- `playback.progress.updated`（应节流 / 聚合，避免事件风暴）
- `playback.completed`
- `playback.ended`
- `media.availability.changed`

高频播放器 heartbeat 不进入全局持久 Event Bus。

---

## 35. API 原则

遵守 `API-Convention-Design.md`：

- UUIDv7；
- RFC3339；
- `/actions/{action}` 表达业务动作；
- Playback Start / Seek / Mark Watched 不使用任意 status PATCH；
- Idempotency-Key；
- ETag / version；
- HTTP Range / Streaming 使用系统级协议约定；
- 长任务返回 Background Task；
- 临时 Playback URL 具有有限 TTL。

Playback URL 不应成为永久可分享凭据。

---

## 36. 数据库关键约束

至少要求：

1. 核心实体使用 PostgreSQL `uuid` / UUIDv7。
2. 时间点使用 `timestamptz`。
3. Episode 在 Work / Season 内的机器排序具有明确约束。
4. Release -> Target Resource 必须属于允许的 Playable Type。
5. Source Attachment 关联避免无意义重复。
6. Playback Variant 必须引用有效 Source Release / Derivation。
7. Derived Variant Profile Version 明确保存。
8. User + Playable Resource 的当前 Progress 不产生多条互相竞争的 active state。
9. Playback Session 与 Progress 分离。
10. 不通过 Media Schema 跨域修改 Blob Placement。

---

## 37. 权限与安全

媒体至少区分：

- metadata read；
- play / stream；
- download；
- manage releases；
- manage subtitles；
- request transcode；
- manage metadata；
- share；
- room playback control。

重要规则：

- read metadata 不一定代表可以 stream；
- stream 不一定代表可以 download；
- Share Token 能力必须映射到明确媒体能力；
- Transcode Worker 读取 Source 使用内部受控 Capability，不获取通用 Storage Secret；
- 临时流地址需要签名 / Token 且短时有效；
- 日志不记录完整 signed URL / credential。

---

## 38. 与 Ingestion 的关系

Ingestion 可以发现：

- Work / Episode Candidate；
- Source Media；
- Subtitle；
- NFO / Provider metadata；
- Release Group / Episode Number hint。

但最终写入通过 Media Command：

```text
Candidate / Import Plan
   ↓
CreateEpisode / AddMediaRelease / AttachSubtitle
   ↓
Media invariants
```

Scanner 不直接操作 Media 私有表。

---

## 39. 与 Core Resource 的关系

Core Resource 继续拥有：

- 标题；
- External Identity；
- Collection / Tag；
- Favorite；
- 通用 Lifecycle；
- Search 基础；
- User State 通用契约。

Media 拥有：

- Work / Season / Episode 专业结构；
- Release；
- Track；
- Playback；
- Media Progress 语义；
- Variant / Transcode 业务。

不得重复创建第二套 Resource Identity。

---

## 40. 与 Storage 的关系

Media 不关心：

- 实际 NAS Path；
- S3 Bucket Secret；
- Replica 表结构。

Media 使用 Storage Capability：

- resolve readable attachment；
- get availability；
- request restore；
- create derived attachment；
- release references。

Storage 状态变化通过 Event 更新 Media Availability Projection。

---

## 41. 与 AI 的关系

AI 可以提供：

- transcription；
- subtitle translation；
- chapter detection；
- scene / content understanding；
- summary；
- metadata suggestion；
- thumbnail suggestion。

AI 结果是 Artifact / Suggestion / Derived Attachment，必须有 Provenance。

AI 不修改：

- Episode identity；
- Playback Progress；
- ACL；
- Blob truth。

---

## 42. Analytics

Analytics 可以消费：

- playback started / ended；
- playback duration；
- completed；
- transcode usage；
- direct play ratio；
- restore wait；
- media error categories。

但 Analytics 不反向决定用户的正式 Progress。

---

## 43. 可观测性

至少监控：

- playable resource count；
- Release availability；
- probe backlog / failure；
- transcode queue / duration / failure；
- direct play / direct stream / transcode ratio；
- playback start latency；
- restore wait time；
- stream error categories；
- progress update conflict count；
- completion event dedupe；
- derived variant storage bytes；
- orphaned variant count。

日志携带 Resource / Release / Session / Task / Correlation ID，不记录未经需要的媒体 URL Token。

---

## 44. 测试与验收基线

实现至少覆盖：

1. Episode 没有 Attachment 时仍合法存在，并显示不可播放而非数据损坏。
2. 同一 Episode 可以绑定多个 Release。
3. Release 与 1080p / 720p Variant 被正确区分。
4. Probe 丢失后可由 Source Attachment 重建。
5. 同一 Blob + Profile 的转码请求幂等复用结果。
6. 转码失败不会破坏 Source Attachment。
7. Derived Variant 清理不会删除原始媒体。
8. 无权播放的用户不能通过已知 Attachment ID 获得 stream URL。
9. 临时 Playback URL 过期后失效。
10. Direct Play 不适用时能选择已有兼容 Variant 或给出明确 Transcode 状态。
11. Missing / Corrupted Release 不会被默认选择。
12. External Subtitle 与 Embedded Subtitle 都能以统一播放器描述返回。
13. 旧离线设备 Progress 不会无条件覆盖较新 Progress。
14. 显式 Reset / Mark Unwatched 可以合法回退 Progress。
15. Completion 重复上报不会重复产生副作用。
16. 删除 History 不会删除 Resource / Progress。
17. Archive Restore 返回 RESTORING，而不是假装可播放。
18. Room 中 Pause / Seek 使用服务端权威 Anchor，并能重连恢复。
19. Room 共同进度不会覆盖其他成员的个人 Progress 语义。
20. Probe / Transcode / Restore 长任务符合 Background Task 规范。

---

## 45. P0 / P1 / P2 建议

### P0

- Work / Season / Episode；
- Movie / Video；
- Media Release；
- Source Attachment；
- Probe；
- Tracks / External Subtitle；
- Direct Play；
- Playback Session；
- Progress / History / Completion；
- Availability；
- basic Playback Source Resolution。

### P1

- Derived Playback Variant；
- HLS / adaptive playback；
- background Transcoding；
- preview thumbnails / sprite；
- Archive Restore integration；
- advanced Release preference；
- Together Room authoritative state。

### P2

- multi-node Transcode Worker；
- advanced hardware acceleration scheduling；
- bandwidth adaptive policy；
- advanced subtitle processing；
- advanced casting / remote playback；
- large-scale realtime gateway optimization。

---

## 46. 核心结论

Ikaros V2 的 Media 领域应保持以下分层：

```text
Logical Content
Work / Season / Episode / Movie / Video
        ↓
Media Release / Version
“哪个源版本”
        ↓
Source Attachment
“源字节对象”
        ↓
Probe / Tracks
“技术属性”
        ↓
Playback Variant
“如何播放”
        ↓
Playback Session
“这一次播放”
        ↓
User Progress / History
“用户消费状态”
```

这样可以同时支持：

- 多版本媒体；
- Direct Play；
- 转码；
- HLS / Adaptive；
- 字幕 / 多音轨；
- NAS / Object Storage / Archive；
- 多客户端；
- Offline；
- Together Room；

并保持 Resource、Storage、Media、Room 与客户端之间的领域边界清晰。
