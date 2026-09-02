# Ikaros V2 Photo / Album / Image Asset 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Photo / Album / Image Asset 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md`、`Offline-Cache-Device-Synchronization-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 图片与相册领域中的 Photo、Original Asset、Photo Metadata / EXIF、Capture Time、Location、Manual / Smart Album、Derived Preview、Thumbnail、OCR / AI Artifact 与未来 Burst / Live Photo 的服务端边界。
>
> 图片领域必须保持 Original 与 Derived Preview 分离。Viewer 默认使用适合屏幕的派生内容不代表 Original 被替换；清理 Preview 不得影响原图。

---

## 1. 设计目标

Photo 子系统需要解决：

1. 一张逻辑 Photo 与实际 Original Attachment 如何分离。
2. 同一图片存在 RAW + JPEG、编辑导出版本或多个来源时如何表达。
3. EXIF、文件元数据、用户修改值与 AI 建议如何通过 Provenance 共存。
4. Capture Time、Added At、File Modified Time 如何严格区分。
5. 没有时区的 EXIF 拍摄时间如何保存不确定性，而不是伪造绝对时刻。
6. Location / GPS 为什么需要比普通 EXIF 更严格的隐私控制。
7. Manual Album 与 Smart Album 如何分离。
8. Smart Album 为什么保存 Query Definition，而不是把动态结果永久写成第二份真相。
9. Original、Preview、Thumbnail、OCR、AI Description 如何通过 Derived Attachment / Artifact 追踪来源。
10. Timeline 如何建立稳定分组语义。
11. Photo Download 与 Cache 如何复用 Offline 设计。
12. 批量 Favorite、Album、Tag 等离线修改如何进入 Pending Mutation。
13. Visual Similarity / Duplicate Detection 如何作为候选，不进行危险自动合并。
14. 未来 Live Photo / Burst 如何扩展而不破坏普通 Photo 模型。

核心原则：

> **Original 是用户内容事实；Preview / Thumbnail / OCR / AI Description 是可重建或可重新生成的派生能力。派生失败不能损坏 Original。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Photo Resource 专业模型；
- Original Image Asset 角色；
- Photo technical metadata；
- EXIF / XMP / embedded metadata provenance；
- Capture Time / timezone uncertainty；
- Location / GPS 隐私语义；
- Manual Album；
- Smart Album Query Definition；
- Album Membership / Order；
- Preview / Thumbnail / Display Variant；
- OCR / AI Description / Semantic Feature Artifact；
- Duplicate / Similarity Candidate；
- Photo Availability；
- Offline Download / Pending Metadata Sync 集成；
- Burst / Live Photo 扩展边界。

### 2.2 本子系统不负责

- Blob Placement / Replica；
- 文件系统扫描；
- 完整专业 RAW 编辑器；
- 非破坏性照片调色算法的最终实现；
- 地图 Provider；
- 人脸识别身份库；
- AI 模型实现；
- 通用 Resource Tag / Favorite 底层实现；
- Share Token / Room；
- 客户端 Gallery Grid UI。

---

## 3. 核心不变量

1. **Photo Identity ≠ File Path**：Photo 使用内部 UUIDv7，不能以路径或对象存储 Key 作为主键。
2. **Original 与 Derived 分离**：Preview / Thumbnail 清理不能删除 Original。
3. **用户元数据优先于自动提取**：EXIF 重新扫描不能静默覆盖用户锁定 Title / Date / Location 修正。
4. **Capture Time ≠ Added At**：导入时间不能伪装成拍摄时间。
5. **未知时区必须保留不确定性**：没有 offset 的 EXIF DateTimeOriginal 不应无依据当作 UTC 或应用默认时区事实。
6. **GPS 属于敏感元数据**：Search、Share、AI、Analytics 不得默认传播精确坐标。
7. **Manual Album Membership 是业务事实；Smart Album Result 是派生结果**。
8. **Album 不拥有 Photo 字节**：从 Album 移除 Photo 不删除 Photo / Blob。
9. **Hash Duplicate 与 Visual Similarity 分离**：相同 Blob 可精确去重；视觉相似只生成候选。
10. **AI Tag / Description 默认是 Suggestion / Artifact**，用户确认后才能成为高优先级业务元数据。
11. **Offline Download ≠ Cache**：本地缓存 Preview 不表示用户已下载原图。
12. **Photo Availability 是对 Storage 状态的专业投影**，不复制 Storage Replica 真相。
13. **批量操作也经过单项权限 /领域规则**，不能因 Selection Mode 绕过 ACL。

---

## 4. 领域模型总览

```text
Photo Resource
   ├── Photo Asset Set
   │    ├── Original Asset
   │    ├── Alternate Original / RAW (optional)
   │    └── Companion Asset (future Live Photo)
   ├── Technical Metadata
   ├── User Metadata Override
   ├── Location
   ├── Derived Preview / Thumbnail
   ├── OCR / AI Artifacts
   └── Album Memberships

Album
   ├── Manual Album -> Memberships
   └── Smart Album  -> Query Definition -> Dynamic Result
```

---

## 5. Photo Resource

Photo 作为 Core Resource Type `image` / `photo` 的专业扩展。

通用能力由 Core Resource 提供：

- title / alias；
- tag；
- favorite；
- collection；
- external identity；
- lifecycle；
- share；
- search base metadata。

Photo 专业字段包括：

- capture time model；
- dimensions / orientation；
- camera / lens metadata；
- GPS / location；
- image asset roles；
- photo grouping；
- derivative state。

---

## 6. Photo Asset Set

一张逻辑 Photo 可以关联多个资产角色：

- ORIGINAL_PRIMARY；
- ORIGINAL_RAW；
- ORIGINAL_JPEG；
- EDITED_EXPORT（若未来支持）；
- LIVE_PHOTO_VIDEO；
- DEPTH_MAP；
- PREVIEW；
- THUMBNAIL；
- OCR_ARTIFACT；
- OTHER_COMPANION。

每个资产使用 Attachment ID。

### 6.1 Primary Original

Photo 必须能明确当前 Primary Original。

切换 Primary Original 是显式 Command，不能由后台“发现更大文件”后静默切换。

---

## 7. Original Version / Alternate Asset

以下情况不应简单覆盖同一 Attachment：

- RAW + JPEG；
- 用户导入同照片的高分辨率原图；
- 不同扫描版本；
- Edited Export；
- Provider 重下载版本。

Photo 可以维护 Asset Version / Role，并由用户或导入规则决定 Primary。

Blob Hash 相同则 Storage 可以物理去重，但 Photo 业务引用仍保持可解释来源。

---

## 8. Technical Metadata

技术元数据可以包括：

- width / height；
- format；
- color profile；
- bit depth；
- orientation；
- alpha；
- animated flag；
- file size；
- camera maker / model；
- lens；
- ISO；
- aperture；
- shutter；
- focal length；
- exposure compensation；
- flash；
- white balance；
- embedded orientation；
- embedded GPS；
- tool / parser version。

技术解析结果是可重建 Projection。

---

## 9. EXIF / XMP Provenance

Metadata Source 至少区分：

- EXIF；
- XMP；
- FILESYSTEM；
- IMPORT；
- USER；
- PROVIDER；
- AI_SUGGESTION；
- SYSTEM_DERIVED。

字段级冲突遵守 Core Resource Metadata Provenance。

例如：

```text
EXIF capture time = 2025-01-01 10:00
User corrected     = 2025-01-01 11:00
```

重新 Probe 后仍应保持用户修正，EXIF 只作为候选来源。

---

## 10. Capture Time Model

图片时间至少区分：

- captured_at；
- captured_local_datetime（当源数据没有 offset）；
- capture_timezone / offset（已知时）；
- timezone_confidence / source；
- added_at；
- source_file_modified_at（仅导入参考）。

### 10.1 有明确 Offset

如果 EXIF / XMP 提供完整时区：

- 转为真实 `timestamptz`；
- 同时保留来源 offset / timezone context（必要时）。

### 10.2 无 Offset

传统 EXIF 经常只有：

```text
2025:08:01 14:32:10
```

此时不能无依据声称这是 UTC+8。

应保留：

- local datetime；
- timezone unknown；
- 可选推断候选。

用户确认时区后才生成高可信绝对时间。

### 10.3 Timeline 分组

Timeline 需要明确使用：

- confirmed capture local date；
- 或应用定义的 fallback date。

UI 可以提示“拍摄时间时区未知”，避免不同设备看到照片突然跨天移动却无法解释。

---

## 11. Location / GPS

Location 是高隐私元数据。

至少可表达：

- latitude / longitude；
- altitude；
- precision；
- source；
- captured_at relation；
- user corrected；
- geocoded place label（派生）。

### 11.1 精度

系统可以提供：

- exact；
- approximate；
- city-level；
- hidden。

Share 时默认可根据 Policy 降低精度或移除。

### 11.2 传播限制

精确 GPS 默认不进入：

- 公共 Search 文档；
- 普通 Analytics；
- 外部 AI Context；
- Share Payload；
-公开日志。

只有明确权限 / 用户授权时使用。

---

## 12. Orientation

EXIF Orientation 与实际像素旋转需要区分。

Viewer / Preview 必须输出视觉正确方向，但 Original 默认保持字节不变。

派生 Preview 可以 bake orientation。

Metadata 编辑 Orientation 不应通过无提示破坏性重写 Original。

---

## 13. Manual Album

Manual Album 是用户显式维护的照片集合。

可以复用 Core Collection，但 Photo Album 专业扩展需要：

- cover photo；
- manual order；
- photo count；
- capture date range（派生）；
- optional album date / location；
- share state；
- membership entry id。

从 Album 删除 Membership：

- 不删除 Photo；
- 不释放 Original Attachment；
- 不改变其他 Album。

---

## 14. Album Membership

字段：

- membership id；
- album id；
- photo id；
- sort order；
- added_at；
- added_by；
- optional caption override。

同一 Photo 在同一普通 Album 默认只出现一次；如果未来支持 Story / Layout 重复，需要显式新的专业模型，而不是随意取消唯一约束。

---

## 15. Album Cover

Cover 可以是：

- explicit selected photo；
- first photo；
- latest photo；
- generated mosaic。

只有 explicit selection 是强业务事实。

Generated Mosaic 是 Derived Attachment，可重建。

Cover Photo 被移出 Album 后：

- 根据策略选择 fallback；
- 不删除原 Photo。

---

## 16. Smart Album

Smart Album 保存 Query Definition，例如：

- date range；
- tag；
- favorite；
- camera；
- location region；
- media kind；
- AI-confirmed category（若允许）。

核心结构：

```text
SmartAlbum
├── id
├── owner
├── name
├── query_definition
├── query_schema_version
└── sort_policy
```

Result 是动态 Projection。

不能每次 Query 结果变化都创建永久 Membership 作为另一份真相。

---

## 17. Smart Query 安全

Query Definition 不是任意 SQL。

必须使用受控 Filter DSL / Query Model：

- 可验证字段；
- 可限制复杂度；
- 权限过滤；
- 插件扩展命名空间；
- versioning。

避免管理员 / 用户通过 Smart Album 注入任意数据库查询。

---

## 18. Preview / Display Variant

Viewer 默认应使用适合当前设备的 Preview。

Preview 可以按：

- max dimension；
- format；
- quality；
- color profile；
- orientation；
- alpha support；
- device class；

生成。

Preview 是 Derived Attachment。

### 18.1 Original ≠ Preview

```text
Original
  ├── Preview 2048
  ├── Preview 1024
  └── Thumbnail 320
```

清理任何 Derived：

- Original 仍可读取；
- 系统可重新生成；
- Photo Resource 身份不改变。

---

## 19. Thumbnail

Thumbnail 用于：

- Grid；
- Scrubber；
- Album Mosaic；
- Search Result。

必须限制生成尺寸 / 数量，避免每个客户端尺寸都制造永久 Derived Blob。

建议使用有限 Profile Catalog。

---

## 20. Derived Image Job

生成 Preview / Thumbnail：

```text
Photo / Original Attachment
      ↓
Request Image Derivative
      ↓
Background Task
      ↓
read source
      ↓
render / validate
      ↓
Derived Attachment
      ↓
register derivative profile
```

幂等键：

```text
source_blob_hash + derivative_profile_version
```

失败不影响 Original。

---

## 21. OCR

OCR Artifact 至少保存：

- photo id；
- source asset / version；
- text；
- regions / boxes；
- language；
- confidence；
- engine / model；
- generated_at；
- provenance。

OCR 可以进入 Search / AI，但必须继承 Photo ACL。

---

## 22. AI Description / Tag Suggestion

AI 可以生成：

- description；
- OCR；
- suggested tags；
- semantic embedding；
- visual category；
- quality warning；
- duplicate / similarity suggestion。

默认：

```text
AI Artifact / Suggestion
      ↓ user accepts
Core Resource Metadata / Tag Command
```

不能静默给大量图片写高优先级标签。

---

## 23. Visual Similarity

Visual Embedding / perceptual hash 可以用于：

- near duplicate candidate；
- burst candidate；
- visually related search。

但：

> Similarity 不是 Identity。

系统不得因为两张图“看起来很像”就自动永久 Merge Resource。

---

## 24. Exact Duplicate

如果 cryptographic Blob Hash 相同：

- Storage 可以物理去重；
- Import 可以识别 exact duplicate；
- Photo 是否复用现有 Resource 仍需 Import / Ownership / Provenance 规则。

例如同一 Blob 被不同用户分别导入，在单 Instance 多用户模式下不自动意味着他们共享同一可见 Photo Resource。

---

## 25. Photo Version / Edit Extension

V2 初期不要求完整照片编辑器，但模型应避免未来无路可走。

未来 Non-destructive Edit 可以表达：

```text
Original Asset
    ↓
Edit Recipe / Version
    ↓
Rendered Derived Asset
```

Edit Recipe 不修改 Original Blob。

如果用户显式“导出为新文件”，可以产生新的 Attachment / Photo Version，是否成为新 Photo Resource由产品操作决定。

---

## 26. Burst

未来 Burst Group 可表示：

- group id；
- member photos；
- capture sequence；
- key photo；
- temporal range。

Burst Group 不应把多个 Original 合成一个 Blob。

成员仍保持独立 Photo 身份。

---

## 27. Live Photo

未来 Live Photo 可以作为 Composite Photo：

- still Photo Resource；
- primary image Attachment；
- companion video Attachment；
- timing / pairing metadata。

它不是普通 Video Resource 强行塞入 Photo Album，也不能丢失 companion video 权限 /存储关系。

具体播放实现后续扩展。

---

## 28. Availability

Photo Availability 可以归一化：

- AVAILABLE；
- PREVIEW_ONLY；
- REMOTE；
- RESTORING；
- PROCESSING；
- MISSING；
- CORRUPTED；
- UNSUPPORTED。

例如：

- Preview 本地可用、Original 在 Archive → `PREVIEW_ONLY + original=RESTORING/REMOTE`。

不要把一个布尔 `available` 掩盖多层状态。

---

## 29. Original View / Restore

用户选择“查看原图”：

```text
Check Original Availability
   ├── AVAILABLE -> return source descriptor
   ├── REMOTE -> stream / download per policy
   └── ARCHIVE -> request restore
                     ↓
                  RESTORING
```

Preview 继续可以显示，不需要在 Original Restore 时把整个 Viewer 阻断成空白。

---

## 30. Timeline

Timeline 是读取 Projection，不是新的业务真相。

排序建议基于：

1. confirmed capture time；
2. capture local datetime with uncertainty；
3. fallback date；
4. added_at（只有没有 capture info 时）。

需要稳定 tie-breaker，例如 Photo ID / sort key，但 UUIDv7 不替代正式 Capture Time。

---

## 31. Offline Download

Photo Download 可以选择：

- Original；
- suitable Preview；
- Album Scope；
- selected batch。

Download Manifest 明确：

- target Photo / Album snapshot；
- original vs preview；
- expected size / hash；
- required assets。

Cache Preview 不标记 Downloaded。

---

## 32. Offline Metadata Mutation

离线可以允许：

- Favorite；
- Add / Remove Manual Album；
- Tag；
- Title / safe metadata（按权限）；

进入 Pending Mutation。

Conflict 由：

- Core Resource；
- Photo；
- Album；

对应领域处理。

Sync Runtime 不做万能 LWW。

---

## 33. Share 与 Privacy Redaction

分享 Photo 时需要独立决定：

- Original / Preview；
- Download allowed；
- EXIF included；
- GPS included；
- capture time precision；
- filename exposure。

默认公开 /匿名 Share 应考虑移除高敏感 EXIF / GPS。

“分享图片”不应自动等价于“分享原始文件的所有元数据”。

---

## 34. Search

Search 可包含：

- title；
- filename display（按权限）；
- date；
- album；
- tag；
- camera model；
- OCR；
- AI-confirmed description；
- approximate location label（按策略）。

精确 GPS 默认不进入普通索引。

---

## 35. Command 契约

典型：

- CreatePhoto
- AttachOriginalAsset
- SetPrimaryOriginal
- UpdatePhotoMetadata
- CorrectCaptureTime
- UpdatePhotoLocation
- CreateManualAlbum
- AddPhotoToAlbum
- RemovePhotoFromAlbum
- ReorderAlbumPhoto
- SetAlbumCover
- CreateSmartAlbum
- UpdateSmartAlbumQuery
- RequestPhotoPreview
- RequestPhotoOCR
- ApplyPhotoAISuggestion
- RequestOriginalRestore
- CreatePhotoDownloadManifest

---

## 36. Query / Capability

建议：

- GetPhoto
- GetPhotoInfo
- ResolvePhotoDisplayVariant
- ResolvePhotoOriginal
- ListTimeline
- ListAlbums
- GetAlbum
- EvaluateSmartAlbum
- GetPhotoAvailability
- ListPhotoDerivatives
- GetPhotoLocation（权限受控）
- ListDuplicateCandidates

---

## 37. Integration Event

建议：

- `photo.created`
- `photo.original.attached`
- `photo.metadata.updated`
- `photo.capture-time.corrected`
- `photo.location.updated`（Payload 最小化）
- `photo.album.membership.changed`
- `photo.preview.ready`
- `photo.ocr.completed`
- `photo.ai-suggestion.created`
- `photo.availability.changed`

敏感 GPS 不应直接塞进普通 Event Payload。

---

## 38. 数据库关键约束

1. Photo / Album 等实体使用 UUIDv7。
2. 时间点使用 `timestamptz`；无时区 Capture Local Time 使用明确独立类型 /字段语义保存，不能伪装成绝对时刻。
3. Primary Original 必须属于当前 Photo Asset Set。
4. Manual Album Membership 唯一性明确。
5. Manual Order 在 Album 内稳定。
6. Smart Album 保存版本化 Query Definition，不保存任意 SQL。
7. Derived Asset 记录 source + profile version。
8. Location 精确字段受数据敏感等级约束。
9. Photo 不跨域修改 Blob Placement。
10. AI / OCR Projection 不反向成为 Original 删除的事实源。

---

## 39. 权限与隐私

至少区分：

- metadata read；
- preview read；
- original read；
- download；
- edit metadata；
- edit location；
- manage album；
- share；
- request AI / OCR。

规则：

- Preview 权限不自动等于 Original Download；
- Location 可以有单独敏感权限；
- Album Share 不自动暴露所有 EXIF；
- AI Context 只包含当前授权 Photo / derivative；
- 日志不记录精确 GPS / signed Original URL。

---

## 40. 与 Ingestion 的关系

Ingestion 可以发现：

- image file；
- EXIF / XMP；
- sidecar；
- directory / album hint；
- exact hash；
- capture date candidate；
- RAW + JPEG pairing candidate。

Import Plan 决定：

- create new Photo；
- attach alternate asset；
- exact duplicate；
- candidate review。

Scanner 不直接覆盖用户 Photo Metadata。

---

## 41. 与 Storage 的关系

Storage 拥有：

- Original Blob；
- Preview Blob；
- Replica；
- Archive；
- Restore；
- GC。

Photo 只保存 Attachment 业务引用与 derivative semantic profile。

---

## 42. 与 Core Resource 的关系

Core Resource 拥有：

- Photo Resource identity；
- title / alias；
- favorite；
- tag；
- generic collection；
- lifecycle；
- external identity。

Photo 拥有：

- Original Asset Set；
- Capture Time；
- EXIF / camera metadata；
- location；
- Photo Album 专业语义；
- Smart Album；
- image derivatives；
- photo similarity candidates。

---

## 43. Analytics

可以统计：

- Photo count；
- added photos；
- storage bytes；
- camera distribution（用户允许时）；
- album count；
- preview hit；
- OCR coverage；
- duplicate candidate count。

默认不应建立：

- 精确 GPS 热力图；
- 敏感地点行为画像；

除非用户明确开启且权限 /隐私策略允许。

---

## 44. 可观测性

至少监控：

- Photo / Album count；
- missing Original；
- corrupted Original；
- derivative backlog / failure；
- restore wait；
- EXIF parse error；
- capture timezone unknown count；
- Smart Album query failure / latency；
- duplicate candidate rate；
- OCR / AI Job failure；
- offline download integrity failure。

---

## 45. 测试与验收基线

实现至少覆盖：

1. Photo 不以文件路径作为内部身份。
2. Preview 删除后 Original 仍存在并可重建 Preview。
3. EXIF Refresh 不覆盖用户锁定 Capture Time / Location 修正。
4. Capture Time 无时区时不会错误转换成应用默认时区的绝对事实。
5. Timeline 对未知时区有稳定可解释 fallback。
6. Added At 与 Capture Time 分离。
7. GPS 不进入未授权 Search / Event / AI Context。
8. 从 Manual Album 移除 Photo 不删除 Photo。
9. Smart Album Query Result 不被写成永久 Membership 真相。
10. Smart Query 不能执行任意 SQL。
11. Same Blob Hash 可以物理去重但不会绕过 Resource / ACL 规则。
12. Visual Similarity 不自动 Merge Photo。
13. Primary Original 切换需要显式 Command。
14. Preview-only 状态可以在 Original Restoring 时继续浏览。
15. Cache Preview 不显示为 Downloaded。
16. Offline Album / Favorite Mutation 使用统一 Mutation ID 幂等同步。
17. Share 默认不无意暴露精确 GPS /敏感 EXIF。
18. AI Tag 未接受前不写入正式 Tag。
19. Batch Selection 中单项无权限不会因批量操作绕过检查。
20. Derived Job 失败不破坏 Original。

---

## 46. P0 / P1 / P2

### P0

- Photo Resource；
- Original Asset；
- EXIF / technical metadata；
- Capture Time model；
- Manual Album；
- Timeline；
- Preview / Thumbnail；
- Availability；
- Offline Download integration；
- Location basic privacy。

### P1

- Smart Album；
- OCR；
- AI Description / Tag Suggestion；
- duplicate / similarity candidate；
- RAW + JPEG pairing；
- advanced privacy redaction；
- non-destructive edit extension point。

### P2

- Burst Group；
- Live Photo；
- advanced face / object organization（需独立隐私设计）；
- geospatial browsing；
- professional RAW development integration。

---

## 47. 核心结论

Photo 领域的稳定分层应为：

```text
Photo Resource
      ↓
Original Asset Set
      ↓
Technical / EXIF Metadata + User Override
      ↓
Derived Preview / Thumbnail / OCR / AI Artifact
      ↓
Manual / Smart Album
      ↓
Timeline / Search / Offline
```

其中：

- Original 与 Preview 永远分离；
- Capture Time 与 Added At 永远分离；
- 无时区时间保留不确定性；
- GPS 使用更严格隐私边界；
- Manual Album 是事实，Smart Album Result 是查询结果；
- Visual Similarity 是候选，不是身份；
- Cache 与 Download 分离；
- Storage、Photo、AI、Offline 各自保持清晰所有权。
