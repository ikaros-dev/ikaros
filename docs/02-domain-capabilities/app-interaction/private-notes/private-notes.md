# Private Notes：私密笔记

## 1. 安全前提

Private Notes 是独立 Secure Domain，默认 `USER_LOCKED_E2EE`。

必须遵守：

- Ikaros Login ≠ Private Vault Unlock。
- 服务端默认不可读取标题、正文、标签、Notebook 名称、附件文件名。
- 本地持久缓存必须加密。
- 普通全局 Search 不保存私密明文索引。
- 普通 Activity / Notification 默认脱敏。
- 锁定后立即清理解密 UI、搜索索引访问、剪贴板敏感内容与预览缓存。

---

## 2. 页面目录

- Private Vault 列表。
- Vault Unlock。
- Notebook / Note 首页。
- 本地搜索。
- Private Note Viewer。
- Private Note Editor。
- Private Attachment Viewer。
- Revision。
- Sync Conflict。
- Templates。
- Secure Share。
- Export。
- Recovery。
- Vault Settings。

---

## 3. Vault 列表

### 3.1 页面布局

App Bar：`私密笔记` + Add Vault（有能力时） + More。

每个 Vault Card 在 Locked 状态只展示允许的最小元数据：

- Vault Display Name：只有客户端拥有可解密本地元数据时显示；否则可显示用户自定义安全别名或 `私密保险库`。
- Lock Icon。
- Security Profile。
- Local Encrypted Cache 状态。
- Last Sync（可显示最小同步元数据时）。
- Conflict Count，只显示数量，不泄露标题。
- `解锁`。

不显示 Note Title Preview。

---

## 4. Vault Unlock

### 4.1 页面

中心 Lock Icon。

- Vault Name / Safe Alias。
- `保险库已锁定`。
- 解锁方法说明。
- Primary Unlock Field / Action。
- Biometric Button（本机已配置时）。
- `无法解锁？`。

### 4.2 生物识别

点击使用系统 Biometric API 解开本地包装密钥；生物特征数据不上传 Ikaros。

失败后仍可返回主解锁方式。

### 4.3 成功

- Key 只进入受控内存状态。
- 构建 / 打开本地加密搜索索引。
- 返回用户原来请求的 Note / Vault 页面。

---

## 5. Vault Home

### 5.1 App Bar

- Vault Name。
- Lock Now 图标。
- Search。
- New Note。
- More。

### 5.2 Desktop Layout

三栏：

- 左 240dp Notebook Tree。
- 中 320–420dp Note List。
- 右 Note Viewer / Editor。

### 5.3 Mobile Layout

首页先显示：

1. Search Bar。
2. Pinned / Recent Notes。
3. Notebook 列表。
4. Tag 横向 Chip。
5. Recent Modified。

点 Note 进入全屏 Viewer。

---

## 6. Notebook Tree

每个节点：

- Folder Icon。
- Notebook Name（仅解锁后）。
- Note Count。
- Expand Chevron。
- More。

支持层级。

操作：New Note、New Sub-notebook、Rename、Move、Export Notebook（高风险）、Delete。

Notebook 名称本身属于加密数据，锁定后整个 Tree 消失，不残留最近名称。

---

## 7. Note List

每个 Row：

- Title。
- 2 行 Body Preview（用户设置允许时）。
- Tags。
- Updated At。
- Attachment Icon。
- Pending Sync / Conflict 状态。
- Pinned。

用户可在 Vault Settings 关闭正文 Preview；关闭后只显示 Title + Metadata。

---

## 8. 本地搜索

### 8.1 入口

Vault 解锁后 Search。

### 8.2 搜索域

- Title。
- Body。
- Tag。
- Notebook。
- Attachment OCR Text（本地 OCR 可用时）。

搜索使用解锁后的本地索引。

### 8.3 锁定

搜索页立即关闭；查询词从输入历史中清除，不进入普通全局 Search History。

---

## 9. Private Note Viewer

### 9.1 Top Bar

- Back。
- Title。
- Lock Now。
- Edit。
- More。

### 9.2 Metadata

- Notebook。
- Tags。
- Updated At。
- Revision。

### 9.3 Body

支持 Markdown / Rich Text / Checklist / Table / Code / Callout / Link / Image / Attachment / Document Link。

Password Manager Secret Reference 渲染为：

```text
[Key] Bank Login Password   [使用/显示]
```

默认不把 Secret 明文直接嵌进 Note DOM / Widget Tree 的长期状态。

### 9.4 普通 Resource Link

可显示普通 Resource Card。

关系默认 `PRIVATE_SIDE_ONLY`；普通 Resource Detail 不显示反向“有私密笔记”提示。

---

## 10. Editor

### 10.1 布局

复用普通 Document Editor 的编辑能力，但底部 / App Bar 必须显示 Secure 状态：`端到端加密`。

字段：

- Title。
- Notebook。
- Tags。
- Body。
- Attachments。

### 10.2 Local Commit

编辑过程：

- Plaintext 只存在解锁内存。
- 自动保存生成 Local Encrypted Commit。
- Sync 上传 Ciphertext。
- UI 状态：`本机已加密保存` / `正在同步密文` / `离线待同步`。

不显示“正在把正文上传服务器”这类误导文本。

---

## 11. Private Attachment

### 11.1 Attachment Row

解锁后显示：

- Decrypted Display Filename。
- Type。
- Size。
- Local Encrypted Cache 状态。
- Sync 状态。
- More。

### 11.2 Viewer

使用流式解密；退出 Viewer 清理临时明文缓冲。

不自动保存到公共 Downloads。

### 11.3 导出明文附件

动作流程：

1. `导出明文`。
2. Dialog 说明“导出后的文件离开 Ikaros 加密保护”。
3. Fresh Unlock / Step-up（策略要求时）。
4. 选择系统目标位置。
5. 导出。
6. Audit / Security Activity（按设计）。

---

## 12. Revision

Revision List：Version、Time、Device / Author、Sync State。

内容仅解锁后加载。

操作：Preview、Compare、Restore。

Restore 创建新 Revision，不清除原历史。

Retention 设置：N Days / N Revisions / Manual Keep。

---

## 13. Sync Conflict

### 13.1 Conflict Banner

Note Row 与 Viewer 顶部显示：`存在同步冲突` + Resolve。

### 13.2 Resolver

显示：

- Mine Revision 时间 / Device。
- Remote Revision 时间 / Device。
- Base Revision。

操作：

- Keep Mine。
- Keep Remote。
- Keep Both。
- Manual Merge。

### 13.3 Manual Merge

Desktop 可三栏 Base / Mine / Remote + Result。

Mobile 使用 Tabs，Result 独立编辑区。

任何选项都不静默删除未选版本，直到新合并 Revision 安全提交。

---

## 14. Templates

Template 列表也在 Vault 内解密：

- Private Diary。
- Emergency Information。
- Medical Record。
- Legal Notes。
- Personal Profile。
- Recovery Instructions。
- 用户自定义。

使用模板创建后生成独立 Note，不与模板保持强同步。

---

## 15. Secure Share

Private Note 不使用普通匿名永久 Share Link。

Create Share Sheet：

- Recipient：指定 Ikaros User / One-time Secure Share。
- Expiration。
- Permission：View / Edit（能力支持时）。
- Attachment Access。
- Revoke Policy。

确认页明确“共享会建立加密访问权限”。

---

## 16. AI Integration

默认 AI Access：`DENY`。

Vault Settings 可选：

- Deny。
- Local Only。
- Cloud Per-use Confirmation（未来可选，必须明确 Provider 与发送范围）。

Note 中 AI 按钮只有策略允许时出现。

Local AI 支持：Summary、Classification、Writing Assist、OCR、Semantic Search。

执行前 Context Preview 列出将解密给模型的 Note / Attachment 范围。

---

## 17. Reminder

Private Reminder 创建字段：Date/Time、Repeat、Safe Summary Policy。

系统通知默认内容：`私密提醒已到期`。

用户若选择显示自定义摘要，UI 必须警告该摘要可能出现在锁屏通知。

---

## 18. Lock Behavior

点击 `Lock Now` 或自动锁定：

- 关闭 Note Viewer / Editor 的 Plaintext Widget。
- 清空本地明文搜索 Session。
- 清理敏感 Clipboard（平台允许时）。
- 清理 Decrypted Attachment Preview。
- Key Material 从内存移除。
- 页面切换为 Locked Surface。

未同步的本地编辑必须已经以加密形式落盘后才能完成锁定；若加密保存失败，先显示阻断式错误。

---

## 19. Screenshot / Recent Apps

Vault Settings：

- `隐藏最近任务预览` 默认 On。
- `禁止截图` 可选，平台支持时。
- `切到后台立即锁定`。

启用后 App Switcher 预览使用纯色 + Ikaros Lock Logo，不显示正文。

---

## 20. Export

优先：`Encrypted Ikaros Vault Export`。

明文导出选项：Markdown / JSON / PDF（支持时）。

明文导出页面必须显示：

- 导出范围。
- Note Count / Attachment Count。
- 目标位置。
- 安全风险。
- Step-up Requirement。

按钮文案使用 `导出未加密副本`，不使用模糊“导出”。

---

## 21. Recovery

页面复用 Access 安全恢复流程，并在 Vault 上下文明确：

- Security Profile。
- Recovery Mode。
- 可用 Recovery Method。
- 是否存在无法恢复风险。

Zero-knowledge 无 Recovery Key 时不提供虚假的管理员恢复入口。

---

## 22. Offline-first

断网时仍支持：

- 查看已同步的加密 Vault 内容。
- 新建 / 编辑 / 删除 Note。
- 本地搜索。
- 添加本地 Attachment。
- Revision。

Sync 状态显示在 Note / Vault 层，不阻断本地使用。

---

## 23. 响应式

- Compact：Vault Home / Note Viewer / Editor 分页进入。
- Medium：Notebook + Note List 双栏，Viewer 覆盖 / Side Sheet。
- Expanded：Notebook / List / Viewer 三栏。
- Large：Editor 正文仍限制可读宽度；右侧可固定 Outline / Properties。
- 所有宽度下 Lock 立即覆盖明文区域，不因 Desktop 多栏而残留 Preview。
