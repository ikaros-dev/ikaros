# Ikaros V2 Music Library / Playback / Queue 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Music Library / Playback / Queue 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md`、`Offline-Cache-Device-Synchronization-Subsystem-Design.md`、`Sharing-Collaboration-Room-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 音乐领域中的 Artist、Album、Edition / Release、Disc、Track、Audio Source、Lyrics、Playlist、Playback Queue、Music Playback Session 与 Together Listening 的服务端边界。
>
> 音乐领域不把客户端播放器内存队列、文件名或音频 Fingerprint 当作永久业务身份。Playlist 是持久用户集合；Queue 是一次播放上下文，两者不能混为一套模型。

---

## 1. 设计目标

Music 子系统需要解决：

1. Artist、Album、Disc、Track 如何建立稳定专业关系。
2. 同一 Album 的普通版、豪华版、重制版、不同地区版如何表达。
3. 同一 Track 有多个音源 / 编码版本时如何选择。
4. 音频技术信息、ReplayGain / Loudness、Fingerprint 如何作为派生数据管理。
5. Playlist 与 Core Collection 如何复用而保留音乐排序语义。
6. Playback Queue 为什么不能直接等于 Playlist。
7. Shuffle 开启后如何保持队列可解释、可恢复和可关闭。
8. Repeat Off / Queue / One 如何成为明确 Playback Policy。
9. Lyrics 如何支持纯文本、时间轴、翻译、罗马音和 Provenance。
10. 播放历史为什么不需要每秒写服务端。
11. 一首歌的断点 Progress 与“听过 / 播放完成”如何区分。
12. Download / Cache / Offline Queue 如何复用统一离线契约。
13. Together Listening 如何同步 Track、Queue、Play/Pause 与 Anchor，而不绕过每个成员对 Track 的访问权限。
14. Search、Analytics、AI、Automation 如何消费音乐事件。

核心原则：

> **Playlist 表达“用户长期保存的曲目集合”；Queue 表达“这一轮准备按什么顺序播放”。Queue 可以来自 Playlist，但播放中的修改不能自动改写源 Playlist。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Artist；
- Album；
- Album Edition / Release；
- Disc；
- Track；
- Track Credit；
- Audio Source Version；
- Audio Probe / Fingerprint / Loudness 等技术派生信息；
- Lyrics；
- Playlist 音乐语义；
- Playback Queue；
- Shuffle / Repeat Policy；
- Music Playback Session；
- Track History / Completion；
- Together Listening 的 Music State；
- 音乐领域事件、权限、可观测性和测试。

### 2.2 本子系统不负责

- Blob / Replica / physical storage；
- Scanner / directory traversal；
- OS Audio Session 的具体 API；
- VLC / mpv / ExoPlayer / AVPlayer 等实现选型；
- Room Membership / Presence / Chat；
- 通用 Collection 的底层实现；
- 推荐算法最终实现；
- 商业流媒体 DRM / licensing；
- 音乐版权管理系统。

---

## 3. 核心不变量

1. **Artist / Album / Track 拥有稳定内部身份**，不能使用文件路径或第三方 ID 作为主键。
2. **Album 与 Edition 分离**：Remaster / Deluxe / Region Release 不应覆盖同一个 Edition 记录。
3. **Track 与 Audio Source 分离**：逻辑 Track 可以有多个实际音频来源。
4. **音频编码质量不是 Track 身份**：FLAC / AAC / MP3 只是 Source Version 的技术属性。
5. **Audio Fingerprint 是 Match Signal，不是唯一业务身份**：Live、Remaster、剪辑版可能产生相似或不同 Fingerprint。
6. **Playlist 是持久集合；Queue 是播放会话状态**。
7. **Shuffle 必须保留基准顺序与实际顺序的可解释关系**，不能打开 Shuffle 后直接永久打乱 Playlist。
8. **Queue 修改默认不回写源 Playlist**。
9. **Lyrics 是独立内容 / Artifact**：不同来源、语言和时间轴版本可共存。
10. **用户长期听歌历史不要求每秒写入**：高频播放器位置必须节流。
11. **Together Room 不转发成员无权访问的媒体字节**。
12. **Download ≠ Cache**：本地命中缓存不应在 UI / Server State 中伪装成显式下载。
13. **AI 生成歌词翻译 / 标签默认是 Derived Artifact / Suggestion**，不覆盖原始来源。
14. **音乐专业关系由 Music 拥有**，不依赖全部塞入自由 Generic Relation。

---

## 4. 领域模型总览

```text
Artist Resource
    │ credits
    ↓
Album Resource
    ├── Edition / Release A
    │     ├── Disc 1
    │     │    ├── Track 1 Resource
    │     │    │     ├── Audio Source A
    │     │    │     ├── Audio Source B
    │     │    │     └── Lyrics
    │     │    └── Track 2
    │     └── Disc 2
    └── Edition / Release B

Playlist (persistent)
       ↓ materialize
Playback Queue (session context)
       ↓
Music Playback Session
       ↓
History / Analytics
```

---

## 5. Resource Types

Core Resource 可以提供：

- `artist`；
- `album`；
- `track`。

Disc / Edition 是否成为 Resource 取决于是否具有独立产品身份。

默认它们可以是 Music 内部实体，避免无意义 Resource 泛化。

---

## 6. Artist

Artist 可以表示：

- Person；
- Band / Group；
- Orchestra；
- Unit；
- Virtual Artist；
- 其他表演主体。

专业字段：

- artist kind；
- sort name；
- disambiguation；
- profile / bio source；
- image reference；
- active period（可选）。

多语言 Name / Alias 和 External Identity 继续由 Core Resource 负责。

---

## 7. Credit

音乐不能只保存一个 `artist_name` 字符串。

Track / Album Credit 至少可以表达：

- artist id；
- role；
- display credit；
- sort order；
- credited-as name；
- optional instrument / contribution。

Role 可包括：

- PRIMARY_ARTIST；
- FEATURED_ARTIST；
- COMPOSER；
- LYRICIST；
- ARRANGER；
- PRODUCER；
- PERFORMER；
- CONDUCTOR；
- REMIXER；
- OTHER。

这样“feat.”等关系不需要塞进不可解析标题字符串。

---

## 8. Album

Album 表达逻辑发行作品，例如：

- Album；
- Single；
- EP；
- Soundtrack；
- Compilation；
- Live Album；
- Other Release Group。

字段：

- Resource ID；
- release group kind；
- primary artists / credits；
- original release date；
- default edition；
- metadata provenance。

---

## 9. Album Edition / Release

Edition 表达 Album 的具体发行版本：

- Original；
- Remaster；
- Deluxe；
- Anniversary；
- Region-specific；
- Digital / CD / Vinyl；
- Provider-specific imported release。

至少包含：

- UUIDv7；
- album id；
- title suffix / edition name；
- release date；
- country / region；
- label；
- catalog number；
- barcode / external identity hints；
- medium type；
- language；
- availability；
- provenance。

### 9.1 Edition ≠ Audio Quality

同一 Digital Edition 下：

- FLAC 24-bit；
- AAC；
- MP3；

可以是不同 Audio Source / Derived Variant，而不是三个 Album Edition。

---

## 10. Disc

Disc / Medium 负责 Album Edition 内部的物理 / 逻辑分组。

字段：

- id；
- edition id；
- disc number；
- title；
- medium format；
- sort order；
- track count（派生）。

多 Disc Album 必须保存 Disc + Track Order，不能将所有 Track 打平后依赖文件名恢复。

---

## 11. Track

Track 是可独立播放、收藏、分享和进入 Playlist 的逻辑音乐对象。

至少包含：

- Resource ID；
- album / edition / disc membership；
- structured track number；
- absolute sort order；
- title；
- credits；
- duration expected / canonical（可空）；
- ISRC / external identity；
- explicit flag（可选）；
- metadata provenance。

同一 Track 可以出现在多个 Album / Compilation，需要使用 Membership / Credit，而不是复制 Track 身份。

---

## 12. Track Membership

Album Edition 中的 Track Membership 至少保存：

- edition / disc；
- track id；
- track number；
- sort order；
- edition-specific title override（必要时）；
- edition-specific duration；
- hidden / pregap metadata（可选）。

同一个 Track 在不同 Edition 的排序和展示可以不同。

---

## 13. Audio Source Version

Audio Source 表达实际可播放的音频来源。

例如：

- 用户导入 FLAC；
- MP3 版本；
- 不同 Master；
- 不同 Provider 文件；
- Live / edit source（如果仍映射到同逻辑 Track，需要明确依据）。

至少包括：

- UUIDv7；
- track id；
- source attachment id；
- source / release info；
- codec / container；
- duration；
- sample rate；
- bit depth；
- channels；
- bitrate；
- lossless flag；
- availability；
- preference weight；
- provenance。

---

## 14. Audio Probe

Audio Probe 是可重建技术数据：

- codec；
- container；
- duration；
- sample rate；
- bit depth；
- channel layout；
- bitrate；
- embedded tags；
- embedded cover；
- ReplayGain tags；
- loudness info；
- stream metadata；
- tool / profile version。

Embedded Tag 只是 Metadata Candidate，不能静默覆盖用户锁定元数据。

---

## 15. Audio Fingerprint

Fingerprint 可以用于：

- duplicate detection；
- candidate matching；
- track identification；
- metadata lookup。

必须记录：

- algorithm；
- algorithm version；
- fingerprint；
- source attachment / audio source；
- generated_at。

Fingerprint 不应单独执行不可逆 Track Merge。

---

## 16. Loudness / ReplayGain

可以派生：

- track gain；
- album gain；
- integrated loudness；
- true peak。

这些是 Playback Metadata，不改变原音频 Blob。

如果用户启用 Loudness Normalization，播放器在输出阶段应用。

---

## 17. Lyrics

Lyrics 是独立内容实体 / Artifact。

类型：

- PLAIN；
- TIMED_LINE；
- TIMED_WORD（可选）；
- TRANSLATION；
- ROMANIZATION。

字段：

- lyrics id；
- track id；
- language；
- type；
- content / attachment reference；
- timing data；
- source；
- provenance；
- confidence；
- created_at；
- version。

### 17.1 多歌词版本

同一 Track 可以同时有：

- 原文 LRC；
- 纯文本；
- 中文翻译；
- 罗马音；
- Provider A / B 不同时间轴。

系统通过用户偏好 / Provider Priority 选默认，不删除其他版本。

---

## 18. Playlist

Music Playlist 可以复用 Core Collection 的持久容器和成员能力，但需要音乐专业扩展：

- track-only / mixed music policy；
- canonical item order；
- optional duplicated track entries；
- entry-level note / added_at；
- owner；
- share；
- collaborative editing（可选）。

### 18.1 Playlist 是否允许重复 Track

音乐 Playlist 通常可能允许同一 Track 多次出现。

因此不能机械套用“Collection Membership 对 Resource 唯一”的通用集合规则。

Music Playlist Entry 应拥有独立 Entry ID，以支持：

```text
Track A
Track B
Track A
```

这是专业模型覆盖通用 Collection 默认约束的典型场景。

---

## 19. Playback Queue

Queue 是一次播放上下文。

至少包含：

- queue id；
- owner / playback session context；
- source type / source id；
- base item order；
- active play order；
- current entry id；
- shuffle state；
- repeat mode；
- version；
- created_at / updated_at。

Queue Source 可以是：

- Album；
- Playlist；
- Search Result Snapshot；
- Artist Top Tracks；
- Manual Ad-hoc Queue；
- Room Shared Queue。

---

## 20. Queue Entry

Queue Entry 具有独立 ID：

- entry id；
- track id；
- selected audio source preference（可选）；
- insertion source；
- inserted_by；
- base position；
- active position；
- state。

使用 Entry ID 能支持同一 Track 重复加入且拖拽时不混淆。

---

## 21. Queue 与 Playlist 边界

从 Playlist 播放：

```text
Playlist Snapshot
      ↓
Create Queue
      ↓
User drags / removes / inserts
      ↓
Queue changes only
```

只有显式：

```text
Save Queue to Playlist
Apply Queue Order to Playlist
```

才修改持久 Playlist。

---

## 22. Shuffle

Shuffle 开启时必须保留：

- base order；
- generated play order；
- current entry；
- shuffle seed / algorithm version（若需要重建）；
- queue version。

目标：

- UI 能展示真实下一首；
- 重连 / 重启后顺序不随机跳变；
- 关闭 Shuffle 后恢复基准 Queue 语义；
- 用户手动插入“下一首播放”有明确位置规则。

禁止通过直接随机重写 Playlist 排序实现 Shuffle。

---

## 23. Repeat

稳定枚举：

- OFF；
- QUEUE；
- ONE。

Repeat 是 Queue / Playback Policy，不是 Track 公共元数据。

Together Room 中 Repeat Policy 是否由 Owner / Moderator 控制，需要 Room Capability 判断。

---

## 24. Queue Command 与并发

典型：

- EnqueueNext；
- EnqueueLast；
- RemoveQueueEntry；
- MoveQueueEntry；
- ClearQueue；
- EnableShuffle；
- DisableShuffle；
- SetRepeatMode。

每项使用 Queue Version / ETag 防止多个客户端同时拖拽导致静默覆盖。

Room Shared Queue 使用 Room Sequence + Music Queue Version 协同。

---

## 25. Audio Source Resolution

播放 Track 时：

```text
Track
  ↓
List allowed Audio Sources
  ↓
Availability
  ↓
User quality / lossless preference
  ↓
Client capability
  ↓
Offline local state
  ↓
Select Source / Derived Variant
```

可考虑优先级：

1. 用户明确选择；
2. Offline Ready Source；
3. preferred edition / master；
4. preferred quality；
5. compatible direct play；
6. derived compatible audio；
7. available fallback。

选择必须可解释。

---

## 26. Music Playback Session

Session 至少保存：

- UUIDv7；
- user；
- queue id；
- queue entry；
- track；
- audio source；
- device；
- room id（可选）；
- started_at；
- last activity；
- ended_at；
- position；
- playback state；
- sequence / version。

播放器 heartbeat 可节流 / 聚合，不要求每秒持久化。

---

## 27. Track Progress 与 Resume

音乐通常不需要像小说 / 长视频一样长期维护显著 Continue Progress，但以下场景仍有价值：

- podcast-like long audio（若未来扩展）；
- audiobook 不属于 Music 时应转 Reading / AudioBook 专业域；
- Track 暂停后短期 resume；
- App crash recovery。

对普通歌曲，长期业务重点应是：

- History；
- play count；
- last played；
- completion / skip。

不应因为播放器每秒位置存在，就给所有歌曲制造大量长期 Progress 记录。

---

## 28. Completion / Skip

Music 可以定义：

- PLAYED；
- COMPLETED；
- SKIPPED；
- ABANDONED。

规则可基于：

- 播放时长；
- 播放比例；
- 用户主动 Next 的位置；
- Track Duration。

必须由服务端 / 产品统一口径，供 Analytics 使用。

重复 Completion Event 幂等。

---

## 29. Playback History

History 记录：

- track；
- album context；
- queue source；
- started / ended；
- listened duration；
- completion / skip；
- device / room context。

History 与 Analytics Fact 可以分层，但不能由 Aggregate 反推业务事实。

---

## 30. Together Listening Room

Room 共享 Music State 可以包括：

- queue id / queue version；
- current queue entry；
- track；
- selected source policy；
- PLAYING / PAUSED；
- anchor position；
- anchor server time；
- playback rate；
- repeat / shuffle state（按 Room Policy）；
- state version。

### 30.1 权限

每个成员进入 Track 时必须单独验证：

```text
member canRead/canStream(track)
```

Room 不允许 Host 作为“媒体代理”绕过 ACL 给无权限成员转发音频。

### 30.2 Queue Control

Room Role 决定：

- add track；
- remove；
- reorder；
- next；
- seek；
- shuffle；
- repeat。

Music Command 执行专业 Queue 规则，Room 负责成员 / Sequence。

---

## 31. Offline

Album / Playlist Download 通过 Offline Download Manifest。

Manifest 可以固定：

- Track Entry；
- selected Edition / Audio Source；
- quality；
- Lyrics（可选）；
- Cover（可选）。

Offline Queue：

- 已下载 Track 优先本地；
- Cache 可以命中但仍不是 Download；
- Remote-only Track 到达时必须明确提示；
- 是否自动跳过由用户 Playback Policy 决定，默认不应无解释跳过。

---

## 32. Download Version / Quality

Download UI 中的 Version / Quality 必须映射到明确服务端语义：

- Edition / Master / Audio Source = Version；
- lossless / bitrate / derived encoding = Quality / Variant。

不能把“FLAC”显示成一个新的 Track 身份。

---

## 33. Search

可索引：

- Artist Name / Alias；
- Album；
- Track；
- Credits；
- Lyrics（权限 /版权策略允许时）；
- Tag；
- External Identity。

Search Projection 可重建。

---

## 34. AI

AI 可以：

- metadata suggestion；
- lyrics translation；
- romanization；
- mood / tag suggestion；
- playlist suggestion；
- semantic search；
- cover understanding。

默认是 Suggestion / Artifact。

AI 不应：

- 直接修改 Playlist 未经确认；
- 伪造播放历史；
- 绕过 Track ACL；
- 把版权歌词发送到不允许的 Provider。

---

## 35. Command 契约

典型：

- CreateArtist
- CreateAlbum
- CreateAlbumEdition
- AddDisc
- AddTrack
- AddTrackMembership
- AddTrackCredit
- AddAudioSource
- RequestAudioProbe
- RequestAudioFingerprint
- AddLyrics
- SetPreferredLyrics
- CreatePlaylist
- AddPlaylistEntry
- MovePlaylistEntry
- RemovePlaylistEntry
- CreatePlaybackQueue
- EnqueueNext / EnqueueLast
- MoveQueueEntry
- RemoveQueueEntry
- EnableShuffle / DisableShuffle
- SetRepeatMode
- StartMusicPlayback
- EndMusicPlayback
- RecordTrackCompletion

---

## 36. Query / Capability

建议：

- GetArtist
- ListArtistAlbums
- GetAlbum
- ListAlbumEditions
- ListAlbumTracks
- GetTrack
- ListTrackSources
- ResolveAudioSource
- GetLyrics
- GetPlaylist
- GetPlaybackQueue
- GetNowPlaying
- ListMusicHistory
- GetMusicAvailability

---

## 37. Integration Event

建议：

- `music.artist.created`
- `music.album.created`
- `music.track.created`
- `music.audio-source.added`
- `music.lyrics.added`
- `music.playlist.updated`
- `music.queue.updated`（仅确有跨域价值时）
- `music.playback.started`
- `music.track.completed`
- `music.track.skipped`
- `music.playback.ended`

高频 progress heartbeat 不进入全局 Event Bus。

---

## 38. 数据库关键约束

1. Artist / Album / Track 等核心实体使用 UUIDv7。
2. Album Edition 归属唯一 Album。
3. Disc Order 在 Edition 内明确。
4. Track Membership 的 Disc + Sort Order 唯一语义明确。
5. Playlist Entry 使用独立 ID，允许同 Track 重复。
6. Queue Entry 使用独立 ID。
7. Queue Version 支持并发控制。
8. Audio Source 指向有效 Attachment。
9. Lyrics Version / Language / Type 可共存，不进行错误唯一覆盖。
10. Music 不跨域修改 Blob Placement。

---

## 39. 权限

至少区分：

- metadata read；
- stream；
- download；
- manage metadata；
- manage source；
- edit playlist；
- share playlist；
- room queue control；
- lyrics manage。

Stream 不自动等于 Download。

Playlist Owner / Collaborator 权限与 Track 自身播放权限分别判断。

---

## 40. 与 Ingestion 的关系

Scanner 可以解析：

- folder / filename；
- embedded tags；
- disc / track number；
- cover；
- cue sheet；
- lyrics；
- fingerprint candidate。

Import Plan 通过 Music Command 落地，不直接改 Music 表。

Embedded Metadata 进入 Provenance Candidate。

---

## 41. 与 Core Resource 的关系

Core Resource 拥有：

- Artist / Album / Track Resource Identity；
- title / alias；
- favorite；
- tag；
- external identity；
- lifecycle。

Music 拥有：

- Credit；
- Edition / Disc；
- Track Membership；
- Audio Source；
- Lyrics；
- Playlist Entry 专业排序；
- Queue / Playback。

---

## 42. 与 Storage 的关系

Music 使用 Attachment / Blob 保存：

- original audio；
- derived audio；
- cover；
- lyrics attachment（若文件型）；
- waveform（可选）。

Storage 决定 Replica / Restore / GC。

Music 只消费 Availability Capability。

---

## 43. 可观测性

至少：

- Artist / Album / Track count；
- unmatched / duplicate source count；
- audio probe / fingerprint failures；
- missing audio source；
- direct playback / derived playback ratio；
- queue conflict；
- playlist reorder conflict；
- lyrics coverage；
- completion / skip event rate；
- offline source failure；
- Together Room queue lag。

---

## 44. 测试与验收基线

实现至少覆盖：

1. Track 身份不依赖文件路径。
2. 一个 Album 可以有多个 Edition。
3. 一个 Track 可以有多个 Audio Source。
4. FLAC / AAC 不会错误创建成不同逻辑 Track（除非实际业务上确实不同版本并经 Match 决策）。
5. Fingerprint 不会单独触发危险自动 Merge。
6. Playlist 允许同一 Track 重复时不会被通用 Collection 唯一约束误删。
7. Queue 修改不会自动改写源 Playlist。
8. Shuffle 不会永久改变 Playlist Order。
9. Shuffle 重连后能恢复可解释实际播放顺序。
10. Repeat One / Queue / Off 语义稳定。
11. Queue 并发拖拽发生版本冲突时不静默覆盖。
12. 多 Lyrics Language / Type 可以共存。
13. AI Translation 不覆盖原始 Lyrics。
14. Cache 命中不显示 Downloaded。
15. 无权限 Room Member 不能通过 Host 获得 Track Stream。
16. 播放 History 不要求每秒持久化。
17. Completion 重复上报幂等。
18. 删除 History 不删除 Track / Playlist。
19. Missing Source 能明确 fallback 或报错。
20. Offline Remote-only Queue Item 有明确用户可理解结果。

---

## 45. P0 / P1 / P2

### P0

- Artist / Album / Edition / Disc / Track；
- Credits；
- Audio Source；
- basic Probe；
- Playlist；
- Queue；
- Shuffle / Repeat；
- Playback Session / History；
- Lyrics basic；
- Offline integration。

### P1

- Fingerprint；
- Loudness normalization metadata；
- advanced Lyrics timing / translation；
- Together Listening shared queue；
- derived audio variants；
- smart playlist / mix。

### P2

- advanced recommendation；
- cross-edition track alignment；
- LAN playback / casting；
- gapless / crossfade server policy metadata；
- advanced audio analysis。

---

## 46. 核心结论

Music 领域的稳定结构应为：

```text
Artist / Album
      ↓
Album Edition
      ↓
Disc / Track Membership
      ↓
Track
      ↓
Audio Source + Lyrics
      ↓
Playlist (persistent)
      ↓
Queue (play context)
      ↓
Playback Session / History
```

其中：

- Edition 不是编码质量；
- Track 不是文件；
- Fingerprint 不是业务主键；
- Playlist 不是 Queue；
- Shuffle 不应破坏基准队列；
- Offline Download 不等于 Cache；
- Room 共享播放状态不能绕过每个成员的媒体权限。
