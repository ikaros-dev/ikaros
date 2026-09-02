# Video：动画、影视、通用视频与播放器

## 1. 页面目录

- 视频首页。
- 作品详情。
- Season / Episode 浏览。
- Episode 详情 / 版本选择。
- Video Player。
- 字幕 / 音轨 / 版本面板。
- 播放设置。
- Together Room 入口。

---

## 2. 视频首页

### 2.1 内容顺序

1. Continue Watching。
2. 最近更新剧集。
3. 我的追番 / 收藏。
4. 电影。
5. 通用视频。
6. 最近添加。

每区 Header：标题 + 查看全部。

### 2.2 Continue Card

- 16:9 Preview。
- 作品名。
- Episode 标题 / 编号。
- Progress Bar。
- `剩余 xx 分钟`（可计算时）。
- 本地状态 Chip：Downloaded / Cached。

Tap 直接继续播放。

---

## 3. 作品详情

### 3.1 Header

- Poster。
- Display Title。
- Original Title。
- Year / Season / Type Chips。
- 收藏状态。
- 当前观看状态。
- `继续观看` / `播放`。
- `下载`。
- `一起看`。
- More。

### 3.2 Episode Progress Summary

显示：

- 已看 `8 / 12`。
- 总观看进度。
- 下一集。

### 3.3 Tabs

- 剧集。
- 简介。
- 信息。
- 关联。

---

## 4. Season / Episode 浏览

### 4.1 Season Selector

作品存在多个 Season / 篇章时使用 Dropdown / Segmented 控件。

字段：Season Name、Episode Count、完成进度。

### 4.2 Episode List Row

从左到右：

- Episode Number。
- 16:9 Thumbnail（宽屏）或小封面。
- Title。
- Air Date。
- Duration。
- Progress。
- Availability。
- Download State。
- More。

Compact 可折叠为：Thumbnail + Number/Title + Progress，其他信息放第二行。

### 4.3 Episode 状态

- 未观看：无进度。
- 观看中：线性进度。
- 已完成：Check Icon。
- 无媒体附件：Disabled Play + `无可播放内容`。
- Restoring：显示恢复状态，不允许假装播放。

### 4.4 Episode More

- 从头播放。
- 标记已看 / 未看。
- 下载。
- 选择版本。
- 查看详情。
- 分享（有权限时）。

---

## 5. Episode 详情 / 版本选择

当一个 Episode 存在多个 Attachment：

每个 Version Card 显示：

- Display Name / Release Group。
- Resolution。
- Codec。
- HDR / SDR。
- Audio Summary。
- Subtitle Summary。
- File Size。
- Availability。
- Local Download State。

主动作：`播放此版本`。

用户可设置 `默认选择规则`，例如优先本地下载、优先 1080p；规则必须可解释，不静默选择 Missing / Corrupted 版本。

---

## 6. Video Player 基础布局

播放器属于 Immersive Surface。

### 6.1 Compact 横屏

```text
┌────────────────────────────────────┐
│ Back   Title              Cast/More│
│                                    │
│              Video                 │
│                                    │
│             Play/Pause             │
│                                    │
│ 00:12 ━━━━━━━●━━━━━━━━━━ 23:40     │
│ Prev  Next  Speed  Subtitle  Tracks│
└────────────────────────────────────┘
```

控制层默认隐藏，Tap 显示 3–5 秒；拖动 / 菜单操作期间不自动隐藏。

### 6.2 Desktop

- 视频画布占主要区域。
- Bottom Controls 两层：进度 + 控制按钮。
- 可选右侧 Episode / Queue Pane，宽 320–400dp。
- 双击视频切换全屏（Desktop）。

---

## 7. 播放器控件

### 7.1 Top Bar

- Back。
- Episode / Resource Title。
- Room 状态（在房间中时）。
- Picture-in-Picture（平台支持时）。
- More。

### 7.2 Center Controls

- Rewind 10s。
- Play / Pause。
- Forward 10s。

Mobile 左右双击区域可快退 / 快进，但必须显示动画反馈和累计秒数。

### 7.3 Progress

- Current Time。
- Slider。
- Buffered Range（播放器可提供时）。
- Duration。

拖动时显示 Preview Thumbnail（存在派生预览时）+ 时间。

拖动结束后才提交最终 Seek；过程中可节流预览。

### 7.4 Bottom Actions

- Previous / Next Episode。
- Speed。
- Subtitle。
- Audio Track。
- Version / Quality。
- Danmaku（插件 / 能力可用时）。
- Fullscreen。

---

## 8. Subtitle 面板

Bottom Sheet / Side Sheet：

顶部 `字幕`。

列表：

- Off。
- 每条 Subtitle：Language、Title、Format、Source、是否本地下载。

选中项 Check。

底部 `字幕样式`：

- 字号。
- 位置。
- 背景 / 描边。
- 字体（平台支持）。
- 延迟 Offset。

字幕切换即时生效，不退出播放器。

---

## 9. Audio Track 面板

每条：

- Language。
- Title。
- Codec。
- Channel Layout。
- Default 标识。

Tap 切换，失败显示 Snackbar 并恢复原 Track。

---

## 10. Version / Quality 面板

区分两个概念：

- `版本`：不同 Attachment / Release。
- `清晰度`：同一源的转码 / Adaptive Variant。

列表项显示：Resolution、Bitrate、Codec、HDR、Size / Estimated Bandwidth、Local / Remote。

弱网络下可提示“当前网络可能无法稳定播放”，但不强制降低质量。

---

## 11. 播放速度

Preset：0.5、0.75、1.0、1.25、1.5、2.0。

可选自定义范围。

当前速度在按钮上显示，例如 `1.25×`。

---

## 12. 手势

Mobile Fullscreen：

- 单击：显示 / 隐藏控制。
- 左区域双击：后退 10s。
- 右区域双击：前进 10s。
- 横向滑动进度仅在设置允许时启用，避免和系统返回冲突。
- 亮度 / 音量竖滑属于平台增强能力，默认可关闭。

所有手势必须在 Settings 有说明。

---

## 13. 键盘快捷键

Desktop：

- Space：Play/Pause。
- Left / Right：±5s。
- `J/L`：±10s（可选）。
- Up / Down：Volume。
- `F`：Fullscreen。
- `M`：Mute。
- `[` / `]`：速度调整（可选）。
- `Esc`：退出全屏 / 关闭 Overlay。

输入框、聊天框聚焦时不拦截。

---

## 14. 播放进度保存

- 周期性节流保存。
- Pause / Exit 时立即尝试保存。
- Offline 时写 Local Pending Progress。
- Reconnect 后同步。
- 冲突时优先采用业务定义的合理进度策略，不简单 Last Write Wins。

播放完成阈值由服务端 / 客户端统一产品规则决定，UI 不自行猜测不同阈值。

---

## 15. Offline

若已下载：

- Player 顶部显示 `离线播放` Chip。
- 不尝试无意义地等待服务器。
- Progress 保存为待同步。

若只有部分 Cache：

- 可以播放缓存范围，但不得把它标记成“已下载”。
- 即将进入未缓存范围且服务器不可达时，明确提示。

---

## 16. Room / 一起看

### 16.1 创建入口

作品详情 `一起看`，或 Player More → `创建房间`。

Create Room Sheet：

- Room Name。
- 当前 Episode。
- Invite Scope。
- 是否允许成员控制播放。
- 创建。

### 16.2 Player 中 Room 状态

Top Bar 显示成员头像叠层 + 人数。

打开 Room Panel：

- 成员列表。
- Host 标识。
- Chat / Event Timeline。
- Queue。
- Control Permission。

### 16.3 同步反馈

远端 Seek / Pause 时显示轻量 Toast：`房主跳转到 12:30`。

网络重连：显示 `正在重新同步房间状态`，完成后明确是否调整了本地进度。

---

## 17. 播放错误

错误分类：

- Network。
- Permission。
- Attachment Missing。
- Corrupted。
- Codec Unsupported。
- Restore Required。
- Processing。

错误 Overlay 必须给对应动作，例如：

- 重试。
- 选择其他版本。
- 等待恢复并通知。
- 下载兼容版本。

不能所有情况只显示“播放失败”。

---

## 18. 响应式

- Compact Portrait：作品详情单列；Player 建议进入横屏沉浸模式但不强制系统旋转。
- Medium：Episode List 可和简介分栏。
- Expanded：作品 Detail 左信息右 Episode；Player 可右侧固定 Episode Pane。
- Large：Player 内容最大化，控制栏不随 4K 宽度无限拉长，中央控制区保持可达。
