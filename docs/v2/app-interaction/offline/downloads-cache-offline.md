# Offline：我的下载、缓存、空间与离线同步

## 1. 产品语义

客户端必须清楚区分：

```text
Download
= 用户明确要求长期离线可用、可管理的本地副本

Cache
= 客户端内部访问加速数据，可淘汰、可重建
```

存在 Cache 不等于 Download Complete；清理 Cache 不等于删除用户下载。

---

## 2. 页面目录

- 我的下载。
- 下载队列。
- Download Detail。
- 离线可用内容。
- 本机空间。
- Cache 设置。
- Pending Sync。
- Sync Conflict。
- Secure Offline Data 状态。

---

## 3. 我的下载

### 3.1 App Bar

- 标题 `我的下载`。
- Search。
- Filter。
- Select。
- More：下载设置、空间管理。

### 3.2 顶部 Storage Summary

横向 Card：

- Downloads `32.4 GB`。
- Cache `8.1 GB`。
- Secure Encrypted Data `1.2 GB`。
- Device Free Space `74 GB`。

点击进入本机空间页。

### 3.3 Filter

- All。
- Video。
- Music。
- Reading。
- Photo / Document。
- Completed。
- Downloading。
- Failed。
- Offline Ready。

---

## 4. Download Item

字段：

- Cover / Icon。
- Resource Title。
- Child Scope，例如 Season 1 / Chapter 1–20 / Album。
- Version / Quality。
- Downloaded Size / Total Size。
- Progress。
- Status。
- Integrity State。
- Updated At。
- More。

Status：Queued / Downloading / Paused / Completed / Failed / Verifying / Needs Repair。

Completed 只有满足完整性与离线使用要求后才显示。

---

## 5. 下载队列

顶部操作：Pause All / Resume All。

每项：

- Priority Drag Handle。
- Title。
- Current File / Chunk。
- Speed。
- Downloaded / Total。
- ETA（可靠时才显示）。
- Pause / Resume。
- Cancel。

ETA 不可靠时显示 `估算中`，不展示不断跳动的虚假精确秒数。

Desktop 可多任务并行详情；Mobile 默认紧凑列表。

---

## 6. 发起下载

Resource / Episode / Album / Book 点击 Download 打开 Sheet：

1. Download Scope。
2. Version / Quality。
3. Included Attachments，例如 Subtitle / Lyrics / Preview。
4. Estimated Size。
5. Device Free Space。
6. Network Policy：Wi‑Fi Only（Mobile）。
7. `下载`。

如果已有有效 Cache：显示 `可复用 1.3 GB 缓存`，但仍进行 Download 完整性校验。

---

## 7. Download Detail

Sections：

- Resource。
- Download Scope。
- Quality / Version。
- Local Size。
- Created / Completed At。
- Integrity Status。
- Included Attachments。
- Offline Playback / Read Test（可选）。

Actions：Open Offline、Repair、Change Quality（重新下载）、Delete Download。

Delete Dialog：`删除下载副本不会删除服务器上的 Resource。缓存是否同时清理由下一步选择。`

---

## 8. 删除下载

确认 Sheet：

- 删除 Download `2.4 GB`。
- 可选 `同时清理可复用缓存 600 MB`。
- Server Data：`不受影响`。
- `删除本机副本`。

Secure Domain 下载按 Secure Data 规则加密删除，不与普通 Cache 合并处理。

---

## 9. Cache 页面

### 9.1 Summary

- Total Cache Size。
- Video Cache。
- Audio Cache。
- Image Preview Cache。
- Document Cache。
- AI Artifact Cache（如有）。

### 9.2 Actions

- Clear All Cache。
- Clear by Type。
- Set Max Size。
- Auto Cleanup Policy。

每项说明：`缓存可重建；清理不会删除“我的下载”。`

### 9.3 Clear Cache

无需极高风险验证，但 Dialog 显示：

- Will Free X GB。
- 可能导致下次打开重新联网下载。
- Downloads 不受影响。

---

## 10. 离线可用内容

不同于“我的下载”，该页聚合：

- Completed Downloads。
- Secure Vault 已同步加密本地数据（锁定后只显示安全摘要）。
- 允许完整离线读取的 Pin 文档。

普通临时 Cache 若不能保证完整消费，不列入 `离线可用`。

Card 上显示 `离线可用` + 内容范围。

---

## 11. Offline Mode

断网进入主 App：

### 全局

顶部 Offline Banner。

### Navigation

在线专属页面不消失，但进入后显示 Offline State + 可用缓存，不让用户误以为功能被删除。

### 写操作

支持离线的业务写入 Local Pending Queue：

- Task。
- Habit。
- Playback / Reading Progress。
- 普通文档编辑。
- Finance Transaction。
- Private Notes / Password Vault Ciphertext Change。

不支持离线的高风险操作直接 Disabled + 原因。

---

## 12. Pending Sync 页面

入口：Offline Banner / 我的下载页面 More / 我的设置。

Group by Subsystem：

- Planning。
- Documents。
- Finance。
- Media Progress。
- Private Notes。
- Password Vault。

每项：Object Safe Title、Change Type、Occurred At、Status、Retry Count、Error。

Secure Domain 未解锁时不显示解密标题，只显示 `私密笔记变更`。

Actions：Retry、Open Object、Discard Local Change（高影响时确认）。

---

## 13. Sync Conflict

页面统一列出需要用户处理的跨端冲突，但实际 Resolver 跳对应子系统：

- Document Conflict → Document Resolver。
- Private Note → Secure Resolver。
- Vault Item → Password Resolver。
- Finance → Finance Conflict Flow。

Global Conflict Card 只显示安全摘要与 `处理`。

---

## 14. Download 网络策略

Settings：

- Wi‑Fi Only。
- Allow Cellular。
- Ask for Files > N MB / GB。
- Max Concurrent Downloads。
- Background Download（平台支持）。
- Battery Saver Behavior。

切换到 Cellular 时如果存在大下载，弹一次非阻断确认。

---

## 15. 自动下载

可选用户规则：

- Favorite Anime New Episode。
- Playlist New Track。
- Book New Chapter。

自动下载本质上可通过 Automation / Download Policy 实现；设置页显示关联 Rule，并允许跳转 Automation Detail。

空间不足时暂停并通知，不自动删除显式 Download 来腾空间，除非用户明确配置保留策略。

---

## 16. Secure Offline Data

Private Notes / Password Manager：

- 本地只显示 Encrypted Cache Size。
- `清除本机加密副本` 需要 Vault Locked / Confirm。
- 清除前说明：若服务器不可达，之后无法离线访问，直到重新同步。
- 清除不是删除服务端 Vault。

已解锁状态下清除本机数据需要先安全锁定并关闭相关 Viewer。

---

## 17. 空间不足

下载前检测 Free Space。

不足时 Sheet：

- Required。
- Free。
- Difference。
- `管理空间`。
- `降低质量`（适用时）。
- Cancel。

不自动清 Cache / Downloads 后直接开始。

---

## 18. 响应式

- Compact：Download Card 列表；空间页面竖向卡。
- Medium：左 Downloads / 右 Detail。
- Expanded：Data Table + Detail Pane；Storage Summary 4 卡一行。
- Large：Queue / Network Stats 可并排，Progress 列保持固定宽度。
