# Music：音乐库、队列与播放器

## 1. 页面目录

- 音乐首页。
- Artist 列表 / 详情。
- Album 列表 / 详情。
- Track 列表。
- Playlist / Collection。
- Now Playing。
- Queue。
- Lyrics。
- Together Room。

---

## 2. 音乐首页

从上到下：

1. Continue / Recently Played。
2. Quick Mix：收藏、最近添加、随机播放。
3. 最近专辑。
4. 常听 Artist。
5. Playlist / Music Collection。
6. 下载的音乐。

Music 首页进入后 Mini Player 持续存在（有活跃播放时）。

---

## 3. Artist 列表

### Grid

Artist Avatar / Cover、Name、Album Count。

### Detail

Header：Artist Image、Name、Alternative Name、`播放热门`、`随机播放`、Favorite。

区块：

- Popular Tracks。
- Albums。
- Related Resources。
- Info。

---

## 4. Album 列表与详情

### Album Card

- 1:1 Cover。
- Album Title。
- Artist。
- Year。
- Downloaded / Remote 状态。

### Album Detail Header

- 1:1 Cover。
- Title。
- Artist Link。
- Year / Type。
- Track Count / Duration。
- `播放`。
- `随机`。
- Download。
- Favorite。
- More。

### Track Row

- Index。
- Title。
- Artist（合集时）。
- Duration。
- Favorite。
- Download State。
- More。

当前播放 Track 使用 Playing Indicator，不仅靠颜色。

---

## 5. Track More

- 下一首播放。
- 添加到队列末尾。
- 添加到 Playlist / Collection。
- 收藏。
- 下载。
- 查看专辑。
- 查看 Artist。
- 分享。
- 查看信息。

---

## 6. Now Playing

### 6.1 Compact

竖向布局：

1. Top Bar：Down / Back、播放设备、More。
2. 大封面，宽约屏幕 72–84%。
3. Title。
4. Artist。
5. Favorite。
6. Progress Slider。
7. Current / Duration。
8. Previous / Play-Pause / Next。
9. Shuffle / Repeat。
10. Lyrics / Queue / Room 快捷按钮。

### 6.2 Desktop

两列：

- 左：Cover + Track Info + Controls。
- 右：Lyrics 或 Queue，可 Tab 切换。

窗口很宽时不无限放大 Cover，建议最大 520dp。

---

## 7. Progress

- Slider 支持 Scrub。
- 支持 Buffered 状态（流媒体）。
- Seek 后本地立即更新；服务端 Activity / Progress 节流同步。
- 播放历史不要求每秒写服务端。

---

## 8. Shuffle / Repeat

Repeat 三态：Off / Queue / One。

Shuffle 开关必须保持 Queue 可解释：打开后展示实际播放顺序，关闭后恢复基准队列语义由播放器层定义。

---

## 9. Queue 页 / Sheet

### Header

- `播放队列`。
- 当前 Queue Source，例如“专辑：xxx”。
- Clear（不清除正在播放项前需确认产品行为）。

### Item

- Drag Handle。
- Cover 40dp。
- Title / Artist。
- Duration。
- More。

Desktop 支持 Drag Reorder；Mobile 长按 Handle 后拖动。

Swipe Delete 支持 Undo。

---

## 10. Lyrics

- 当前句高亮并自动滚动。
- 用户手动滚动后暂停自动跟随，出现 `回到当前歌词` Floating Button。
- 支持时间轴歌词与纯文本歌词。
- Translation / Romanization 若存在，使用次级文本。

歌词不可用：显示 Empty + `查看歌曲信息`，不使用无限 Loading。

---

## 11. Playlist / Music Collection

列表：Cover Mosaic、Name、Track Count、Duration、Owner / Shared。

详情：

- Play。
- Shuffle。
- Download。
- Share。
- Edit Order。

音乐 Playlist 可以复用 Collection 底层能力，但 UI 使用音乐语义。

---

## 12. Download

Album / Playlist 下载前 Bottom Sheet：

- Track 数量。
- Estimated Size。
- Quality / Version（多个音源时）。
- 当前设备剩余空间。
- `下载`。

下载中 Album Header 显示总进度，单 Track 显示独立状态。

---

## 13. Together Listening Room

Now Playing → Room：

- 创建 / 加入。
- 同步 Track、Progress、Play/Pause、Queue。
- Room Panel 展示成员、Queue、Chat。

成员无目标 Track 访问权限时显示权限错误，不由 Room 转发媒体文件绕过 ACL。

---

## 14. Audio Session 与系统集成

客户端应映射平台能力：

- Background Playback。
- Media Notification / Lock Screen Controls。
- Headset Play/Pause。
- OS Media Session。

系统控制触发与 App 内控制使用同一 Player State。

---

## 15. Offline

- 已下载 Track 优先本地播放。
- 本地下载不存在但 Cache 有效时可命中缓存。
- Cache 不显示为“已下载”。
- Offline Queue 中 Remote-only Track 在到达时明确提示并允许跳过。

---

## 16. 错误状态

- Attachment Missing：选择其他音源。
- Network：重试 / 使用本地。
- Decode：报告不支持格式 / 选择派生版本。
- Permission：返回资源详情。

播放错误只跳过当前 Track需用户设置允许，默认暂停并解释。

---

## 17. 响应式

- Compact：Now Playing 全屏；Queue / Lyrics 用 Bottom Sheet / 独立页。
- Medium：Now Playing + Bottom Tabs。
- Expanded：左右双栏，Lyrics / Queue 常驻。
- Mini Player 在所有非沉浸页保持一致状态。
