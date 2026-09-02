# Ikaros V2 私密笔记子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Private Notes Subsystem Design |
| 适用版本 | Ikaros V2 |
| 文档状态 | Draft |
| 设计目标 | 提供面向高度敏感个人信息的端到端加密笔记、附件、同步、检索与版本管理能力 |

---

## 1. 产品定位

Private Notes 是 Ikaros V2 中专门用于保存高度敏感个人信息的笔记子系统。

它与普通 Note / Document 的主要区别不是编辑器，而是安全边界。

典型内容包括：

- 私人日记
- 身份证件信息
- 医疗或法律相关私人记录
- 银行卡相关备忘
- 私密联系人信息
- 私人计划
- 恢复说明
- 高敏感附件
- 其他不希望普通系统管理员、Storage Provider 或数据库备份直接读取的数据

但以下内容应优先进入 Password Manager，而不是私密笔记：

- 网站密码
- 银行卡 PIN
- TOTP Secret
- Passkey
- Recovery Code
- SSH Private Key
- API Token

Private Notes 可以引用 Password Manager 中的 Secret，但不应鼓励用户把所有密码写进自由文本。

---

## 2. 参考方向

产品体验可参考 Joplin 的以下思路：

- Notebook / Note 组织
- 标签
- 多端同步
- 离线优先
- Attachment / Resource
- E2EE
- 冲突处理
- Markdown / Rich Text 编辑

但 Ikaros V2 需要进一步与平台 Resource、Secure Data、AI、Activity、Search 和 Permission 体系融合。

---

## 3. 安全基线

Private Notes 默认：

```text
security_profile = USER_LOCKED_E2EE
```

服务端默认只能看到：

- Note ID
- Vault ID
- Owner ID
- Crypto Version
- Ciphertext Size
- Revision Sequence
- Sync Metadata
- 最小必要时间戳

正文、标题、标签、Notebook 名称、附件文件名等默认全部进入加密区域。

---

## 4. 核心概念

### 4.1 Private Vault

Private Vault 是私密笔记的安全边界。

```text
PrivateVault
├── id
├── owner
├── security_profile
├── crypto_context
├── settings
├── sync_policy
└── lifecycle
```

一个用户可以创建多个 Vault，例如：

- Personal
- Family Private
- Legal
- Archive

不同 Vault 可以使用不同密钥。

### 4.2 Private Note

逻辑结构：

```text
PrivateNote
├── id
├── vault_id
├── encrypted_title
├── encrypted_body
├── encrypted_properties
├── created_at
├── updated_at
├── revision
└── tombstone
```

### 4.3 Private Notebook

用于层级组织：

```text
Notebook
└── Notebook
    └── Note
```

Notebook 名称和结构同样可能泄露隐私，默认加密。

### 4.4 Private Tag

Tag 默认属于加密数据。

服务端不应通过普通 Tag Analytics 统计私密 Tag。

### 4.5 Private Attachment

所有附件走 Secure Data Foundation 的 Secure Blob。

```text
Private Note
    ↓
Private Attachment
    ↓
Encrypted Blob
    ↓
Object Storage
```

---

## 5. 编辑能力

至少支持：

- Markdown
- Rich Text
- Checklist
- Table
- Code Block
- Callout
- Internal Link
- Attachment
- Image
- File
- Document Link

编辑器能力可以复用普通 Document Engine，但数据存取必须走 Private Notes 安全边界。

---

## 6. Offline-first

Private Notes 应以 Offline-first 体验为目标。

```text
Local Encrypted Store
        ↓
Unlock
        ↓
Edit
        ↓
Local Encrypted Commit
        ↓
Sync Ciphertext
        ↓
Server
```

断网时用户仍应能够：

- 查看已同步笔记
- 编辑
- 创建
- 删除
- 搜索本地索引
- 添加本地附件

联网后同步密文变更。

---

## 7. Sync

服务端同步不依赖解密内容。

同步单元至少包含：

- Object ID
- Revision
- Ciphertext
- Updated At
- Tombstone
- Crypto Metadata

支持：

- Incremental Sync
- Conflict Detection
- Tombstone Sync
- Device Resume
- Attachment Resume

---

## 8. Conflict

因为服务端可能看不到正文，不能依赖服务端语义 Merge。

冲突策略：

```text
base revision = N
client A -> N+1
client B -> N+1
```

产生 Conflict Revision。

客户端解锁后向用户提供：

- Keep Mine
- Keep Remote
- Keep Both
- Manual Merge

文本三方合并可在客户端完成。

---

## 9. Revision

Private Note 支持版本历史。

Revision 也必须加密落盘。

```text
PrivateNote
├── Revision 1 ciphertext
├── Revision 2 ciphertext
└── Revision 3 ciphertext
```

版本保留策略可配置：

- N days
- N revisions
- Manual Keep

---

## 10. Search

默认使用客户端本地加密索引。

```text
Encrypted Notes
      ↓ unlock
Local Index Builder
      ↓
Encrypted Local Index
      ↓
Search
```

支持：

- 标题
- 正文
- Tag
- Notebook
- Attachment OCR Text（可选）

不将私密明文写入普通全局 Search Index。

---

## 11. Global Search Integration

全局搜索可以展示一个安全占位结果：

```text
Private Notes
12 results available
Unlock to view
```

但不得把实际标题作为普通 Search Document 长期明文保存。

解锁状态下，客户端可合并：

```text
Global Search Result
+
Private Local Search Result
```

---

## 12. Internal Link

私密笔记之间支持 Wiki-like Link：

```text
[[Private Note]]
```

Link Target 的语义索引默认仅存在于加密区域。

---

## 13. Resource Link

Private Note 可以引用普通 Ikaros Resource：

```text
PrivateNote
  └── RELATED_TO → Resource
```

但反向关系默认不得在普通 Resource 页面公开“存在一条私密笔记”。

否则关系本身会造成隐私泄露。

因此关系需要支持：

```text
visibility = PRIVATE_SIDE_ONLY
```

---

## 14. Password Manager Link

允许插入安全 Secret Reference：

```text
[[secret://vault/item/field]]
```

编辑器显示：

```text
Bank Login Password  [Reveal]
```

而不是把 Password 明文复制进 Note。

---

## 15. Templates

支持加密模板，例如：

- Private Diary
- Emergency Information
- Medical Record
- Legal Notes
- Personal Profile
- Recovery Instructions

Template 内容本身存于 Vault。

---

## 16. Lock Behavior

用户锁定 Vault 后：

- Note Body 从 UI 移除
- 标题按策略隐藏
- Local Search Index Lock
- Clipboard Sensitive Data 清理
- Preview Cache 清理
- Decrypted Attachment Viewer 关闭
- Key Material 从内存移除

---

## 17. Screenshot / Recent Apps

移动端应支持安全模式：

- App Switcher Preview 隐藏敏感内容
- 可选阻止系统 Screenshot
- Background 时自动 Lock

具体平台能力按 OS 支持情况实现。

---

## 18. Attachment Viewer

高敏感 Attachment 默认流式解密。

不得自动写入公共 Downloads 目录。

显式“导出明文附件”需要：

- 用户确认
- 重新验证或 Unlock
- 明确目标位置
- Audit

---

## 19. Sharing

Private Note 分享不是普通 Share Link。

支持：

- 加密分享给指定 Ikaros User
- One-time Secure Share
- Expiring Share
- Revocation

默认禁止匿名永久公开链接。

---

## 20. Collaboration

P1/P2 可支持有限协作 Vault。

由于 E2EE 多用户协作会引入 Group Key 管理，需要独立设计：

```text
Vault Key
  ↓ encrypted separately
Member A
Member B
Member C
```

成员撤销时需要重新评估后续 Key Rotation。

---

## 21. AI Integration

默认：

```text
AI_ACCESS = DENY
```

用户可以对某个 Vault 设置：

```text
LOCAL_ONLY
```

然后使用本地 AI：

- 总结
- 分类
- 写作辅助
- OCR
- 语义搜索

对于 Cloud AI，必须逐次显式授权或采用明确 Policy。

AI 不得自动读取所有 Private Notes。

---

## 22. OCR

私密图片 / PDF 可执行 OCR。

在 USER_LOCKED_E2EE 模式下：

- OCR 优先客户端本地
- OCR Result 加密保存
- 不写普通全文索引

---

## 23. Reminder

Private Note 可以创建 Reminder，但通知内容默认脱敏：

```text
Private reminder is due
```

用户可自行允许显示自定义安全摘要。

Reminder 事件不能携带私密正文。

---

## 24. Activity

普通 Activity Timeline 默认只记录：

```text
Private note updated
```

不记录标题或内容。

也可以允许用户完全关闭 Private Notes Activity。

---

## 25. Analytics

默认只允许最小化统计，例如：

- note count
- attachment bytes
- revision count
- sync success rate

禁止普通平台统计自动分析：

- 私密内容主题
- 私密关键词
- 私密标签

---

## 26. Import

支持未来导入：

- Markdown Directory
- Joplin Export
- Standard Notes-like Export
- Encrypted Ikaros Export

导入过程：

```text
Source Plaintext
   ↓ local import
Encrypt
   ↓
Private Vault
```

如果源数据是明文文件，应提醒用户清理原文件。

---

## 27. Export

首选：

- Encrypted Ikaros Vault Export

可选：

- Markdown
- JSON
- PDF

明文导出必须是高风险动作。

---

## 28. Recovery

Private Vault 必须支持独立 Recovery 配置。

UI 必须明确告诉用户：

> 如果采用 Zero-knowledge 且未配置可用恢复机制，丢失解密密钥可能导致数据永久无法恢复。

---

## 29. Data Lifecycle

```text
ACTIVE
  ↓
TRASH
  ↓
PURGE
  ↓
CRYPTO_ERASURE + BLOB_GC
```

Trash 中的数据仍然加密。

---

## 30. Permission

普通平台管理员不能天然读取用户 Private Vault。

```text
Admin
  ≠
Vault Decryption Authority
```

用户可以选择共享或授权，但必须显式。

---

## 31. Private Notes 与普通 Notes 的关系

普通 Note / Document：

- 更容易全文搜索
- 更容易 AI
- 更容易协作
- 可进入统一知识库

Private Note：

- Security First
- 默认 E2EE
- 默认不进入服务器全文索引
- 默认不进入 AI Context
- 默认最小化 Activity / Analytics

两者是不同产品语义，不能仅靠一个 `is_private` 字段区分。

---

## 32. P0

P0 至少包括：

- Private Vault
- USER_LOCKED_E2EE
- Note CRUD
- Notebook
- Tag
- Attachment
- Offline Local Store
- Incremental Sync
- Client Search
- Revision
- Lock / Unlock
- Encrypted Export
- Recovery Key
- Audit

---

## 33. P1

- OCR
- Secure Share
- Secret Reference
- Local AI
- Advanced Search
- Templates
- Biometric Unlock
- Device Trust UI

---

## 34. P2

- Shared E2EE Vault
- Collaborative Editing
- Advanced Key Recovery
- Private Knowledge Graph

---

## 35. 核心结论

Private Notes 不是“普通笔记加一个密码”。

它应建立在 Secure Data Foundation 之上：

```text
Private Notes
      ↓
USER_LOCKED_E2EE
      ↓
Encrypted Local Store
      ↓
Ciphertext Sync
      ↓
Ikaros Server / Object Storage
```

服务器同步和保存数据，但默认不需要知道笔记究竟写了什么。
