# Ikaros V2 Reading / Comic / Novel / Ebook 子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Ikaros V2 Reading / Comic / Novel / Ebook 子系统设计 |
| 适用版本 | Ikaros V2 |
| 文档版本 | v0.1 |
| 编写日期 | 2026-08-31 |
| 状态 | 草案（Draft） |
| 产品基线 | `Product-Requirements-Document.md` |
| 系统基线 | `System-Overview-Design.md` |
| API 基线 | `API-Convention-Design.md` |
| 数据库基线 | `Database-Overview-Design.md` |
| 依赖设计 | `Core-Resource-Library-Subsystem-Design.md`、`Attachment-Blob-Storage-Subsystem-Design.md`、`Content-Ingestion-Metadata-Synchronization-Subsystem-Design.md`、`Offline-Cache-Device-Synchronization-Subsystem-Design.md` |

> 本文档定义 Ikaros V2 漫画、小说、电子书阅读领域中的 Work、Volume、Chapter、Page / Text Content、Edition、Reading Locator、Reading Session、Progress、Completion、Annotation 与派生阅读资产边界。
>
> 阅读器 UI 的分页算法、字体渲染框架或具体 EPUB SDK 不是领域事实。服务端需要保存跨设备可解释的逻辑阅读位置，而不是某台设备上的屏幕像素 Offset。

---

## 1. 设计目标

Reading 子系统需要解决：

1. Comic、Novel、Ebook 如何在统一 Resource 身份下保留不同专业结构。
2. Work / Volume / Chapter 如何表达强领域顺序和层级。
3. 一个作品存在不同 Edition / Release 时如何表达，而不是把不同文件简单覆盖。
4. 漫画页面如何引用 Attachment，并支持 Page Order、双页、封面和条漫。
5. 小说 / Ebook 的正文如何以格式适配层提供稳定 Locator。
6. 跨设备进度为什么不能只保存像素 Offset 或百分比。
7. Reading Session、Progress、History、Completion 如何分离。
8. 用户显式“从头阅读 / 标记未读”如何与普通向前进度区分。
9. OCR、翻译、缩略图等派生内容如何追踪来源且不覆盖原始内容。
10. Annotation / Quote 如何稳定引用特定 Edition / Chapter / Locator。
11. Download、Cache、Offline Progress 如何复用 Offline Sync 契约。
12. AI 阅读辅助如何遵守权限、隐私和 Provenance。

核心原则：

> **Reading Progress 保存逻辑内容位置，不保存设备渲染位置。屏幕像素、滚动 Offset 和当前页码都可能因设备、字体和版式改变。**

---

## 2. 范围与非目标

### 2.1 本子系统负责

- Comic / Manga Work；
- Novel / Ebook Work；
- Volume / Arc / Chapter；
- Edition / Reading Version；
- Comic Page；
- Text / Ebook Content Descriptor；
- Reading Locator；
- Reading Session；
- Reading Progress / Completion / History；
- Reading Preference 的书籍级覆盖语义；
- Annotation / Quote 的阅读锚点；
- OCR / Translation / Preview 等派生阅读资产；
- Reading Availability；
- 与 Offline、AI、Analytics、Automation 的契约。

### 2.2 本子系统不负责

- Attachment / Blob 的物理存储；
- Scanner / Import Plan；
- 客户端具体图片解码或 EPUB 渲染引擎；
- 字体文件系统安装；
- Private Notes 的安全正文；
- AI OCR / Translation 模型实现；
- 通用 Share / Room；
- 客户端 Download Queue 实现。

---

## 3. 核心不变量

1. **Work 身份与文件分离**：漫画 / 小说作品不以 CBZ / EPUB 文件路径作为内部身份。
2. **Volume / Chapter 顺序是领域事实**：不能让客户端通过标题自然排序猜测正确章节顺序。
3. **Edition 与 Work 分离**：不同出版社、扫描版、翻译版、排版版可以属于同一 Work 的不同 Edition。
4. **Comic Page 引用 Attachment**：页面不保存宿主绝对路径。
5. **小说 Locator 必须跨设备稳定**：禁止只保存屏幕 pixel offset。
6. **百分比不是唯一恢复点**：Progress Percent 是展示 / 派生值，不能替代 Chapter + Locator。
7. **Session 与长期 Progress 分离**：一次阅读会话结束不等于删除长期进度。
8. **旧离线设备不能静默覆盖新进度**：使用版本、Intent 和领域合并规则。
9. **显式重读允许合法回退**：不能简单使用“最大进度永远赢”。
10. **OCR / Translation 是 Derived Artifact**：默认不修改原始 Page / Text。
11. **Page / Chapter 删除不能错误 GC 历史仍引用的 Attachment**：最终由 Storage 引用规则决定。
12. **阅读设置分层**：Global Default 与 Per-Book Override 不应互相污染。

---

## 4. 领域模型总览

```text
Reading Work Resource
   ├── Edition A
   │    ├── Volume 1
   │    │    ├── Chapter 1
   │    │    │    ├── Comic Pages
   │    │    │    └── or Text Content
   │    │    └── Chapter 2
   │    └── Volume 2
   └── Edition B

User
   ├── Reading Session
   ├── Reading Progress
   ├── Reading History
   └── Reading Preference Override
```

---

## 5. Reading Resource Type

可以包括：

- `comic`；
- `comic_chapter`；
- `novel`；
- `novel_chapter`；
- `ebook`。

Volume 是否成为 Resource 取决于是否需要独立：

- External Identity；
- Share；
- Favorite；
- Search；
- 生命周期。

若只是 Work 内部结构节点，可保留为 Reading Entity。

---

## 6. Work

Reading Work 表达逻辑作品。

通用标题、Alias、Tag、External Identity 继续由 Core Resource 管理。

Reading 专业元数据可以包括：

- reading kind；
- serialization status；
- original language；
- author / illustrator relation references；
- default reading direction；
- default edition；
- completion status。

作者本身如果需要成为可搜索 / 可关联实体，可以由 Resource / Person 模型后续扩展，而不是把所有作者字段塞进自由字符串。

---

## 7. Edition / Reading Version

Edition 表示同一 Work 的具体阅读版本，例如：

- 不同出版社版本；
- 原文 / 官方译本；
- 不同扫描组；
- EPUB / PDF / Web 导入版本；
- 修订版；
- 彩色版 / 黑白版。

Edition 至少包括：

- UUIDv7；
- work id；
- name；
- language；
- publisher / source；
- edition metadata；
- source attachment / manifest reference；
- preference weight；
- availability；
- provenance。

### 7.1 Edition ≠ Render Mode

单页 / 双页 / 连续滚动 / 字体大小不是 Edition。

这些属于客户端阅读模式 / Preference。

---

## 8. Volume / Arc

Volume / Arc 用于章节分组。

至少表达：

- id；
- edition id；
- kind：VOLUME / ARC / PART；
- display label；
- structured number；
- sort order；
- title；
- optional cover attachment；
- chapter range。

不能仅通过文件夹名决定长期业务排序。

---

## 9. Chapter

Chapter 是阅读进度的基本逻辑单元。

至少具有：

- Resource ID 或稳定 Chapter ID；
- work / edition；
- volume；
- structured chapter number；
- display label；
- title；
- sort order；
- content kind；
- page count / text length（派生可空）；
- publication date；
- availability；
- lifecycle。

Chapter Number 必须支持：

- 1；
- 1.5；
- 0；
- Extra；
- Special；
- Prologue / Epilogue。

机器排序与展示标签分离。

---

## 10. Comic Page

Page 是 Comic Chapter 内的有序内容单元。

至少包含：

- page id；
- chapter id；
- attachment id；
- page order；
- page role；
- width / height（Probe）；
- spread hint；
- source provenance；
- optional crop / orientation metadata。

Page Role 可包括：

- COVER；
- NORMAL；
- SPREAD_LEFT；
- SPREAD_RIGHT；
- DOUBLE_SPREAD；
- INSERT。

### 10.1 双页

双页显示是客户端布局，但服务端可以提供 Spread Hint，帮助：

- 封面单页；
- 左右配对；
- 跨页大图。

不能仅靠“奇数偶数”作为所有作品的绝对规则。

---

## 11. Comic Reading Direction

Work / Edition 可设置默认：

- LEFT_TO_RIGHT；
- RIGHT_TO_LEFT；
- VERTICAL_SCROLL；
- WEBTOON / LONG_STRIP。

用户可以按书籍覆盖。

Direction 影响客户端交互，不改变 Page 的 canonical order。

---

## 12. Novel / Ebook Content

Novel / Ebook 可以来自：

- EPUB；
- HTML / XHTML；
- Markdown / Text；
- PDF（受限重排）；
- Provider Content；
- 其他格式适配器。

Reading 子系统需要统一 `Readable Content Descriptor`，不要求所有格式先永久转换成同一种内部正文格式。

Descriptor 至少可以提供：

- chapter / spine item；
- content reference；
- canonical order；
- stable locator scheme；
- media / style dependencies；
- format version。

---

## 13. Reading Locator

Locator 是跨设备恢复阅读位置的关键契约。

逻辑结构可包括：

```text
ReadingLocator
├── edition_id
├── chapter_id
├── locator_type
├── locator_value
├── fallback_progress
└── content_version
```

Locator Type 可以包括：

- COMIC_PAGE + optional intra-page offset；
- PARAGRAPH_ID；
- TEXT_POSITION；
- CFI-like；
- EPUB_LOCATION；
- PDF_PAGE + normalized position；
- provider-specific stable locator（命名空间）。

### 13.1 禁止仅使用 Pixel Offset

以下值不能单独成为跨设备进度：

```text
scrollY = 38421
```

因为字体、屏宽、排版、主题、系统缩放变化后失效。

### 13.2 Percent

Percent 可以用于：

- UI 展示；
-粗略 fallback；
- completion 判断辅助。

但必须由更稳定 Locator 支撑。

---

## 14. Content Version 与 Locator Migration

Edition 内容可能被修正：

- 章节插入；
- EPUB 更新；
- 页顺序修正；
- 文本替换。

Progress 保存 `content_version`。

更新后：

1. Locator 仍可解析 → 保持；
2. 可映射 → 迁移并记录来源；
3. 无法映射 → fallback 到最近 Chapter / Percent 并提示；
4. 禁止错误映射到完全不同内容。

---

## 15. Reading Session

Session 表示一次阅读活动。

字段：

- UUIDv7；
- user；
- work / edition；
- started chapter / locator；
- current locator；
- started_at；
- last_active_at；
- ended_at；
- device；
- offline flag；
- session version。

Session 用于 History / Analytics，不等于当前长期 Progress。

---

## 16. Reading Progress

长期状态作用域：

```text
User + Reading Work / Edition
```

根据产品策略，可以同时维护：

- Work-level Continue Reading；
- Edition-level exact Locator；
- Chapter read state。

Progress 至少包括：

- current edition；
- current chapter；
- locator；
- chapter progress；
- work progress（派生）；
- completed；
- updated_at；
- source session；
- version；
- intent。

---

## 17. Progress Intent

建议至少：

- PROGRESS_FORWARD；
- NAVIGATE；
- RESET_FROM_BEGINNING；
- MARK_CHAPTER_READ；
- MARK_CHAPTER_UNREAD；
- MARK_WORK_READ；
- MARK_WORK_UNREAD；
- OFFLINE_REPLAY。

这样可以区分：

- 正常往后读；
- 用户手动跳回前文；
- 明确重读；
- 旧设备离线补交。

禁止简单用“最大 Locator 永远赢”。

---

## 18. Chapter Completion

Comic 常见：到达最后 Page / Chapter End。

Novel 常见：到达章节结尾附近并进入下一章。

服务端定义可配置 Completion Rule，不由每个客户端自行决定。

重复完成事件必须幂等。

`chapter.completed` 可以驱动：

- Progress；
- Analytics；
- Automation；
- Next Chapter suggestion。

---

## 19. Reading History

History 记录：

- 何时阅读；
- 哪个 Work / Chapter；
- 阅读时长（可靠时）；
- 起止 Locator 摘要；
- Device。

删除 History 不等于：

- 标记未读；
- 清 Progress；
- 删除 Resource。

---

## 20. Reading Preference

设置层级：

```text
Platform / User Global Default
        ↓
Reading Kind Default
        ↓
Per-Book Override
```

例如 Comic：

- mode；
- direction；
- fit；
- gap；
- cover single-page；
- auto next。

Novel：

- font family；
- font size；
- line height；
- width；
- theme；
- pagination / scroll。

这些主要是 User Preference，不应写入 Work 公共元数据。

---

## 21. Annotation / Quote

Annotation 可以表达：

- Highlight；
- Quote；
- Note Link；
- Bookmark。

锚点必须保存：

- work / edition；
- chapter；
- Locator / range；
- content version；
- selected text fingerprint（文本类）；
- page region（漫画可选 normalized coordinates）。

内容变化无法安全迁移时进入 DETACHED / NEEDS_REVIEW。

### 21.1 Quote 与普通 Note

用户可以从选中文本创建普通 Note / Document Reference。

引用应保存来源关系，不把整本受版权 / 权限保护的正文复制到另一个公开对象。

Private Note 引用遵守 Secure Domain，不能通过反向链接泄露其存在。

---

## 22. OCR

漫画 OCR 产生 Derived Artifact：

- OCR text；
- bounding boxes；
- language；
- model / engine version；
- confidence；
- source page attachment；
- provenance。

OCR 不改变原始 Page。

可以用于：

- Search（按权限）；
- AI context；
- translation；
- accessibility。

---

## 23. Translation

Translation 可以是：

- text overlay artifact；
- translated chapter content；
- translated subtitle-like layer；
- user / provider imported translation。

必须记录：

- source content version；
- language pair；
- provenance；
- AI model / provider policy（若 AI）；
- generated_at。

默认不能覆盖原文。

---

## 24. Preview / Thumbnail

Comic Page Thumbnail、Chapter Cover、Ebook Preview 都属于 Derived Attachment / 可重建资产。

清理 Preview：

- 不删除 Original Page；
- 不改变 Chapter Progress；
- 下次访问可重新生成。

---

## 25. Availability

Reading Availability 可归一化：

- AVAILABLE；
- DOWNLOADED（客户端语义）；
- REMOTE；
- RESTORING；
- PROCESSING；
- MISSING；
- CORRUPTED；
- UNSUPPORTED_FORMAT。

服务端不把客户端 Download 当成 Storage Replica 真相。

---

## 26. Offline

Download Manifest 可以以：

- Chapter；
- Volume；
- Work；

为 Scope。

Comic Manifest 包含 Required Pages。

Ebook Manifest 包含：

- source / processed reading package；
- required styles / images / fonts（按安全策略）；
- metadata。

Offline Progress 使用 `Offline-Cache-Device-Synchronization-Subsystem-Design.md` 的 Mutation ID、Cursor 和冲突规则。

Cache 不等于 Downloaded。

---

## 27. Edition Selection

如果 Work 有多个 Edition：

优先级可考虑：

1. 用户明确选择；
2. 当前 Progress 对应 Edition；
3. Downloaded / Offline Ready Edition；
4. user preferred language / provider；
5. default edition；
6. available fallback。

切换 Edition 时需要尝试 Locator Migration；不能假设 Chapter Index 100% 对齐。

---

## 28. Search

Search Projection 可以包含：

- Work title / Alias；
- Chapter title；
- author；
- provider metadata；
-可授权的正文；
- OCR text；
- translation text（明确标注来源）。

历史 /未授权 Edition 不应泄露。

Search index 是可重建 Projection。

---

## 29. AI 阅读辅助

AI 可以：

- summarize chapter；
- explain selected text；
- translate selected text；
- character/entity recap；
- OCR；
- semantic retrieval。

必须：

- permission-aware；
- 遵守 Provider Privacy Policy；
- 生成内容有标识；
- 结果默认 Artifact / Suggestion；
- 不改原始 Page / Chapter；
- 用户关闭 AI 时核心阅读仍可用。

---

## 30. Command 契约

典型：

- CreateReadingWork
- CreateEdition
- AddVolume
- MoveVolume
- AddChapter
- ReorderChapter
- AttachComicPage
- ReorderComicPage
- BindEbookContent
- UpdateReadingProgress
- MarkChapterRead / Unread
- MarkWorkRead / Unread
- ResetReadingProgress
- SelectReadingEdition
- CreateReadingAnnotation
- ResolveAnnotation
- RequestOCR
- RequestTranslation
- RequestReadingDownloadManifest

所有 Command 经过当前权限与领域校验。

---

## 31. Query / Capability

建议：

- GetReadingWork
- ListEditions
- ListVolumes
- ListChapters
- GetChapter
- ListComicPages
- ResolveReadableContent
- ResolveReadingLocator
- GetReadingProgress
- GetContinueReading
- ListReadingHistory
- GetReadingAvailability
- GetReadingPreferences

---

## 32. Integration Event

建议：

- `reading.work.created`
- `reading.edition.added`
- `reading.chapter.created`
- `reading.chapter.reordered`
- `reading.progress.updated`（节流）
- `reading.chapter.completed`
- `reading.work.completed`
- `reading.annotation.created`
- `reading.ocr.completed`
- `reading.translation.ready`
- `reading.availability.changed`

高频 scroll 不进入全局 Event Bus。

---

## 33. 数据库关键约束

1. 核心实体 ID 使用 UUIDv7。
2. 时间点用 `timestamptz`。
3. Edition 归属 Work 明确。
4. Chapter 的 canonical sort order 在 Edition / Volume 内一致。
5. Comic Page Order 在 Chapter 内唯一 / 可重排。
6. Page 引用有效 Attachment ID。
7. Progress 对 `User + Work` / `User + Edition` 的 active state 唯一语义明确。
8. Locator 保存 content version。
9. Annotation Anchor 可以验证对应 Work / Edition / Chapter。
10. Reading 不跨域修改 Blob Placement。

---

## 34. 权限与隐私

能力至少区分：

- metadata read；
- content read；
- download；
- annotate；
- manage edition；
- manage chapter；
- request OCR / AI；
- share。

规则：

- Metadata 可见不自动表示正文可读；
- Download 权限可比 Read 更严格；
- OCR / AI Context 不扩大原内容权限；
- Private Annotation 默认只属于用户；
- Shared Annotation 需要显式能力。

---

## 35. 与 Ingestion 的关系

Ingestion 可以解析：

- CBZ / CBR / ZIP；
- EPUB；
- PDF；
- Markdown / Text；
- Directory；
- Provider chapter feed。

产生 Candidate / Import Plan 后调用 Reading Command。

扫描器不能直接写 Chapter / Page 私有表。

---

## 36. 与 Storage 的关系

Reading 通过 Attachment ID 访问：

- Page image；
- Ebook source；
- Cover；
- OCR artifact；
- Preview。

Storage 决定：

- Replica；
- Restore；
- Blob GC；
- physical location。

---

## 37. 与 Core Resource 的关系

Core Resource 拥有：

- Resource identity；
- title / alias；
- tag / collection；
- favorite；
- generic lifecycle；
- external identity。

Reading 拥有：

- Edition；
- Volume / Chapter；
- Page / Readable Content；
- Locator；
- reading progress semantics；
- annotation reading anchors。

---

## 38. 可观测性

至少监控：

- Reading Work / Chapter count；
- broken chapter order；
- missing page attachment；
- unsupported format；
- Locator migration failure；
- Progress conflict；
- OCR / translation queue and failure；
- reading availability；
- download manifest integrity error；
- detached annotation count。

---

## 39. 测试与验收基线

实现至少覆盖：

1. Work 不依赖文件路径作为身份。
2. 一个 Work 可以有多个 Edition。
3. Chapter 排序不依赖展示标题字符串。
4. 漫画 Page Order 可稳定重排，Attachment 身份不改变。
5. RTL / LTR 改变客户端行为，不改变 canonical Page Order。
6. Novel Progress 不仅保存 pixel offset。
7. 字体 / 屏宽改变后仍能按 Locator 恢复接近正确位置。
8. Content Version 改变后 Locator 无法安全迁移时明确降级，不错误跳转。
9. 旧离线设备不会静默覆盖新 Progress。
10. 显式 Reset / Mark Unread 能合法回退。
11. History 删除不清空 Progress。
12. OCR / Translation 不覆盖 Original。
13. Preview 删除不删除 Original Page。
14. Cache 不标记为 Downloaded。
15. Chapter / Volume Download 只有 Manifest 完整后 Offline Ready。
16. Annotation 锚点失效时进入 Detached。
17. 未授权用户不能通过 Search / OCR 获取受保护正文。
18. AI 关闭后核心 Reading 能力不受影响。

---

## 40. P0 / P1 / P2

### P0

- Comic / Novel / Ebook Work；
- Edition；
- Volume / Chapter；
- Comic Page；
- basic Ebook content adapter；
- stable Locator；
- Progress / History / Completion；
- reading preferences；
- Download integration。

### P1

- Annotation / Quote；
- OCR；
- Translation；
- Preview / thumbnail；
- advanced EPUB / PDF locator migration；
- AI reading assistant。

### P2

- collaborative annotation；
- advanced semantic reading graph；
- complex edition alignment；
- panel-level comic understanding；
- server-assisted pagination cache（若有必要）。

---

## 41. 核心结论

Ikaros V2 Reading 领域应围绕：

```text
Work
  ↓
Edition
  ↓
Volume / Chapter
  ↓
Page or Readable Content
  ↓
Stable Reading Locator
  ↓
Session / Progress / History
```

展开。

其中最关键的长期契约是：

- Work 与文件分离；
- Edition 与阅读模式分离；
- Chapter / Page 有服务端 canonical order；
- 进度使用逻辑 Locator，不依赖设备 pixel offset；
- Offline Mutation 进入统一 Sync，但冲突仍由 Reading 领域处理；
- OCR / Translation / Preview 都是可追溯派生能力，不替代原始内容。
