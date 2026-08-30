# Collaboration：分享、Room、评论与协作

## 1. 页面目录

- 我的分享。
- Create Share。
- Share Detail。
- Share Landing（登录用户客户端打开）。
- Room 列表。
- Create / Join Room。
- Room Detail。
- Room Chat / Event。
- Document / Resource Comments。
- Collaborator / Permission Sheet。

Private Notes / Password Manager 的安全共享使用各自 Secure Domain 流程，不直接套用普通 Share Link。

---

## 2. 我的分享

### 2.1 App Bar

- 标题 `分享`。
- Filter：Active / Expired / Revoked。
- Create。

### 2.2 Share Card

字段：

- Target Icon + Title。
- Target Type：Resource / Collection / Document。
- Permission Summary。
- Expiration。
- Access Count / Max Count（配置时）。
- Download Allowed。
- Password Protected Icon。
- Status。
- Created At。
- More。

More：Copy Link、Edit、View Access Summary、Revoke。

---

## 3. Create Share

### 3.1 Target

- 当前 Resource / Collection / Document 自动带入。
- 可修改 Scope（允许时）。

### 3.2 字段

1. Expiration：1h / 24h / 7d / Custom / No Expiry（策略允许时）。
2. Password Protection：Switch + Password / Generator。
3. Allow Download：Switch。
4. Maximum Access Count：Optional。
5. Maximum Download Count：Optional。
6. Content Scope：Current Only / Included Children / Selected Items。
7. Optional Message。

### 3.3 Preview

创建前显示权限摘要：

```text
可查看：某 Collection 内 12 个 Resource
可下载：否
有效期：72 小时
密码：是
```

`创建分享` 后显示 Link + QR（支持时）+ Copy。

Share Token 不继承创建者其他权限，UI 用 Helper Text 解释。

---

## 4. Share Detail

Header：Target、Status、Revoke。

Sections：

- Link / QR。
- Permission。
- Expiration。
- Access / Download Count。
- Created At / Updated At。
- Recent Access Summary（权限与隐私允许时）。

### Revoke

确认文案：`撤销后该链接立即失效，不会删除原资源。`

撤销可立即执行，不使用“删除资源”视觉语义。

---

## 5. Share Landing in App

Deep Link 打开：

1. 显示 Share Target Summary。
2. 若需要密码，输入 Share Password。
3. 显示 Share 权限范围。
4. `打开内容`。

若当前登录账号本身有更高权限，用户仍能以正常账号打开，但 Share Scope 的展示必须清楚，避免误认为 Token 提升账号权限。

Expired / Revoked：显示明确状态与返回。

---

## 6. Room 列表

Tabs：

- Active。
- Invited。
- History。

Room Card：

- Room Name。
- Type：Watch / Listen / General Queue。
- Current Resource。
- Host。
- Member Avatar Stack + Count。
- Status。
- Last Activity。
- `加入` / `继续`。

---

## 7. Create Room

字段：

- Room Name。
- Room Type：Watch / Listen。
- Initial Resource / Queue。
- Persistent / Temporary（能力支持时）。
- Invite Scope。
- Member Playback Control：Host Only / Admin / All Members。
- Chat Enabled。
- `创建`。

创建后进入 Room Detail / Player。

---

## 8. Join Room

Invite Deep Link 页面：

- Room Name。
- Host。
- Member Count。
- Current Resource。
- Required Permission Summary。
- `加入房间`。

若用户无 Resource READ / Stream 权限：

- 显示“你可以加入房间，但无法访问当前媒体”或按服务端规则阻止加入。
- 不由 Room 转发媒体内容绕过 ACL。

---

## 9. Room Detail Panel

播放器中用 Side Sheet / Bottom Sheet；普通 Room 可独立页。

Tabs：

- Members。
- Queue。
- Chat。
- Events。

### Members

每行：Avatar、Name、Role：Host / Admin / Member、Connection State、More。

Host / Admin 可执行：Promote、Demote、Remove、Transfer Host（能力支持时）。

### Queue

Resource Card + Drag Handle + Added By + More。

队列变更实时同步。

### Chat

Message：Author、Time、Body、Optional Resource Link。

输入框：Text + Attachment / Resource Reference（允许时）。

### Events

系统事件：Member Joined、Paused、Seek、Resource Changed、Permission Changed。

事件使用紧凑 Timeline，不和聊天正文完全混排。

---

## 10. Room 同步反馈

播放器发生远端控制：

- `房主已暂停`。
- `房主跳转到 12:30`。
- `队列切换为下一集`。

短 Toast / Overlay 1–2 秒。

本地网络抖动：

- `正在重新连接房间`。
- 重连成功后显示 `已同步到 12:32`。

如果本地用户刚拖动进度但无控制权限，立刻回弹并显示权限解释，不假装成功后再跳回。

---

## 11. Comments

### 11.1 Comment Panel

可用于 Resource / Document / Task / Goal 等支持 Comment 的对象。

顶部：Object Title、Comment Count、Sort。

Comment Card：

- Avatar。
- Author。
- Time。
- Body。
- Reply Count。
- Resolved（批注类）。
- More。

### 11.2 Composer

- Multiline Text。
- Mention（支持时）。
- Resource Link。
- Submit。

无 COMMENT Permission 时 Composer 替换成只读提示。

---

## 12. Collaborator Sheet

显示：

- Owner。
- Members / Groups。
- Role / ACL Summary。
- Pending Invite。

有 Manage Permission 时：Add Member、Change Permission、Remove。

修改权限属于高风险动作时调用 Step-up / Confirmation Policy。

---

## 13. Document Collaboration

- 在线协作者 Avatar Stack。
- Cursor / Selection Presence。
- Comments / Annotation。
- Connection State。
- Revision。

离线后转 Local Edit；重新连接时若产生冲突，显示 Resolver，不因为 Room / Collaboration 存在就关闭 Revision 机制。

---

## 14. Notifications

事件可产生：

- Share 即将过期。
- 新评论 / Reply。
- Room Invite。
- Room Started。
- Permission Changed。

Notification 点击 Deep Link 回对应对象。

---

## 15. Offline

- Share 创建 / 修改需要在线确认服务端 Token 状态，离线时禁用并解释。
- Room 必须在线。
- 已缓存 Comment 可查看；新评论离线是否允许取决于同步能力，允许时标 Pending。
- 文档本地编辑按 Documents Offline 规则。

---

## 16. 响应式

- Compact：Room Panel Bottom Sheet / Fullscreen；Comments 独立页或 Bottom Sheet。
- Medium：Player + Room Side Sheet。
- Expanded：Room / Comments 320–400dp 常驻右栏。
- Large：Members / Chat 可双栏，但媒体画面仍保持主要空间。
