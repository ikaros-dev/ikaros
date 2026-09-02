# Notifications：通知中心与用户通知偏好

## 1. 页面目录

- 通知中心。
- Notification Detail。
- Notification Group / Digest。
- 用户通知偏好。
- Quiet Hours。

管理员公告、模板、Provider 和系统级投递治理在 CMS；App 负责用户接收、阅读和偏好。

---

## 2. 通知中心

### 2.1 App Bar

- 标题 `通知`。
- 未读数量 Badge。
- Search（通知量大时）。
- More：全部已读、Archived、通知设置。

### 2.2 Tabs

- 全部。
- 未读。
- 重要。
- 已归档。

### 2.3 Filter Chips

- Content。
- Planning。
- Finance。
- Security。
- Storage / Download。
- Automation / Sync。
- Collaboration。
- AI。

---

## 3. Notification Row

从左到右：

- Source Icon。
- Unread Dot。
- Title。
- Body Preview，最多 2 行。
- Time。
- Priority Icon（High 时）。
- More。

下方可有最多两个 Inline Action，例如：

- `查看`。
- `重试`。
- `批准 / 拒绝`（安全流程必须跳详情，不在列表直接执行高风险批准）。

Tap 整行：Mark Read + Deep Link Target。

---

## 4. Source 与视觉

Notification 必须显示来源，例如：

- Media。
- Planning。
- Finance。
- Security。
- Backup / Storage。
- Plugin。
- Automation。
- Room。

Severity / Priority 使用 Icon + Label，不只用颜色。

Security 高风险通知可使用 Warning Container，但不允许插件随意伪装 Security Source。

---

## 5. Notification Detail

字段：

- Source。
- Title。
- Full Body。
- Created At。
- Priority / Severity。
- Related Object Card。
- Actions。
- Delivery Info（用户需要时折叠）。

Related Object Card 点击跳 Resource / Task / Session / Rule / Room 等。

---

## 6. 敏感通知脱敏

默认禁止锁屏 / 通知正文暴露：

- Password / Token / TOTP。
- Private Note Title / Body（除非用户显式允许安全摘要）。
- 完整银行卡号 / PIN。
- OTP Code。

Private Reminder 默认：`私密提醒已到期`。

Security OTP 本身不得通过普通 Notification Center 作为可回看的正文长期保存。

---

## 7. Swipe / Quick Actions

Mobile：

- 右滑：Mark Read / Unread。
- 左滑：Archive。

均支持 Snackbar Undo。

不提供 Swipe Delete 作为默认动作，避免误删重要通知。

Desktop Hover 显示 Read / Archive 图标。

---

## 8. 批量操作

进入 Selection Mode：

- Mark Read。
- Mark Unread。
- Archive。

`全部已读` Dialog 显示当前筛选范围，例如“将 38 条未读通知标记为已读”。

---

## 9. Notification Group / Digest

高频低优先级通知可以聚合：

例如：

```text
元数据同步更新了 18 个资源
2 个字段因人工锁定而保留
```

Group Card：Source、Summary、Item Count、Time Range、Expand。

展开后显示原始通知，不用 Digest 取代原始记录。

AI Digest 开启时明确 `AI 摘要`，并可查看原始通知。

---

## 10. Notification Preferences

### 10.1 Channel

按通知类别设置：

- In-app。
- Push。
- Email。
- Plugin Channel（用户可选时）。

每行：Category、各 Channel Toggle。

系统安全强制通知如果不能完全关闭，Toggle Disabled 并解释原因。

### 10.2 Category

至少：

- Content Update。
- Planning Reminder。
- Finance Bill / Budget。
- Download / Restore。
- Sync / Import。
- Collaboration / Room。
- Security。
- AI Job / Digest。

---

## 11. Quiet Hours

字段：

- Enabled。
- Start Time。
- End Time。
- Days of Week。
- Allow High Priority Override。
- Allow Security Override（受系统策略）。

跨午夜示例 `22:00–07:00` 正确显示。

Quiet Hours 影响投递，不把通知本身删除。

---

## 12. Deep Link

点击通知时：

1. Mark Read。
2. 解析 Target。
3. 校验当前 Server。
4. 校验权限 / Secure Unlock。
5. 打开目标。

Private Note 通知：先 Unlock，再目标 Note；取消 Unlock 返回 Notification Center，不泄露 Note Title。

---

## 13. Offline

- 已缓存通知可离线查看。
- Mark Read / Archive 可作为 Pending Change。
- Deep Link 到 Remote-only Resource 时显示 Offline State。
- Room Invite 等实时对象离线显示“当前无法加入”。

---

## 14. 响应式

- Compact：列表 → Detail 全屏。
- Medium：列表 + Detail Side Sheet。
- Expanded：左 420dp List、右 Detail。
- Large：右侧 Detail 中 Related Object / Action 可固定，不增加通知正文行宽。
