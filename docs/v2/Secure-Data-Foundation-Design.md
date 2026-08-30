# Ikaros V2 私密数据安全基础层设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Secure Data Foundation Design |
| 适用版本 | Ikaros V2 |
| 文档状态 | Draft |
| 设计目标 | 为私密笔记、密码管理、记账及其他高敏感业务提供统一的加密数据、密钥、访问与审计基础能力 |

---

## 1. 设计背景

Ikaros V2 将承载普通数字内容，也会承载极高敏感度的个人数据，例如：

- 私密笔记
- 银行卡、证件等敏感资料
- 密码、Passkey、TOTP Secret、恢复码
- 个人财务账户与交易记录
- 私密附件
- 密钥、Token 与其他 Secret

这类数据不能仅依赖磁盘加密、数据库透明加密或对象存储服务端加密。

Ikaros 需要一个独立的 **Secure Data Foundation（私密数据安全基础层）**，统一解决：

- 应用层加密
- 端到端加密
- 密钥派生
- 密钥层级
- 密钥轮换
- 加密 Blob
- 加密结构化字段
- Secret Reference
- 解锁 Session
- 恢复与紧急访问
- 安全导入导出
- 搜索边界
- AI 数据边界
- 审计
- 安全删除

该基础层不是某个具体业务子系统，而是上层敏感业务共享的安全基础设施。

---

## 2. 核心设计原则

### 2.1 Persisted Plaintext Forbidden

Secure Data Foundation 管理的数据，默认禁止以明文形式落盘。

覆盖：

- PostgreSQL
- Object Storage
- Server Local Cache
- Client Persistent Cache
- Export
- Backup
- Search Index
- Background Task Payload
- Runtime Temporary File

原则：

> 敏感业务数据只允许在明确授权、已解锁且生命周期受控的内存上下文中短暂以明文存在。

### 2.2 Application-layer Encryption

不能把以下能力视为 Secure Data Foundation 的替代品：

- Full Disk Encryption
- PostgreSQL Disk Encryption
- S3 Server-side Encryption
- TLS

它们仍然有价值，但只作为纵深防御。

真正敏感的数据应在进入普通持久化层之前完成应用层加密。

```text
Plain Data
   ↓
Secure Data API
   ↓
Encrypt
   ↓
Ciphertext
   ↓
PostgreSQL / Object Storage / Backup
```

### 2.3 Zero-knowledge Capable

安全基础层必须支持服务端无法获取业务明文的 Zero-knowledge / E2EE 模式。

密码管理和高敏感私密笔记默认应使用这一模式。

```text
Client
  │ Plaintext
  ↓
Encrypt
  │ Ciphertext
  ↓
Ikaros Server
  │ Ciphertext only
  ↓
Storage
```

### 2.4 Security Profile

不是所有敏感业务都必须采用完全相同的信任模型。

定义至少以下安全 Profile：

#### USER_LOCKED_E2EE

- 用户侧解锁
- 服务端只保存密文
- 服务端不能执行依赖明文的搜索、AI 和统计
- 适合 Password Manager、最高等级私密笔记

#### SERVER_ASSISTED_ENCRYPTED

- 仍然全部应用层加密落盘
- 受控服务端运行时可以在权限允许时解密
- 适合需要服务器端统计、自动化或搜索的部分私密业务
- 密钥不得与业务密文以可直接组合的明文形式共同落盘

#### LOCAL_ONLY

- 数据不上传到服务端
- 仅客户端本地加密存储
- 可用于极端隐私场景

上层业务必须明确声明使用何种 Profile。

### 2.5 Metadata Minimization

加密正文不意味着隐私问题已经解决。

以下信息也可能泄露敏感事实：

- 标题
- 标签
- URL
- 账户名称
- 交易对象
- 文件名
- Attachment MIME
- Note Folder Name
- Login URI

因此 Secure Data 对象应明确区分：

```text
Public Metadata
Protected Metadata
Secret Payload
```

默认情况下，高敏感业务的业务标题、描述、标签、URI 等也属于 Protected Metadata。

### 2.6 No Secret in Log / Event

Secret 和敏感明文不得进入：

- Runtime Log
- Operation Log
- Login Log
- Event Payload
- Analytics Fact
- Notification Body
- Trace
- Error Stack Context

事件只允许携带安全引用，例如：

```text
secure_object_id
object_type
operation
actor
result
```

而不是明文内容。

---

## 3. 总体架构

```text
                    ┌────────────────────────┐
                    │   Business Subsystem   │
                    │ Notes / Finance / Vault│
                    └───────────┬────────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │ Secure Data Capability │
                    └───────────┬────────────┘
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
      Crypto Runtime        Key Service       Secure Session
             │                  │                  │
             └──────────────────┼──────────────────┘
                                ▼
                       Encrypted Envelope
                                │
             ┌──────────────────┼──────────────────┐
             ▼                  ▼                  ▼
         PostgreSQL       Object Storage        Backup
         Ciphertext        Cipher Blob        Ciphertext
```

在 USER_LOCKED_E2EE 模式下，Crypto Runtime 的关键解密操作应位于客户端。

---

## 4. 核心数据结构

### 4.1 Secure Object

所有安全对象共享一个逻辑安全身份。

建议概念模型：

```text
SecureObject
├── id
├── owner / scope
├── security_profile
├── object_type
├── crypto_version
├── key_reference
├── protected_metadata_ciphertext
├── payload_ciphertext
├── created_at
├── updated_at
└── tombstone
```

SecureObject 不要求所有上层业务共用一张表。

它定义的是安全契约，不是强制的物理 Schema。

### 4.2 Encrypted Envelope

每段密文必须是自描述的加密 Envelope，而不是单纯 BYTEA。

逻辑结构：

```text
EncryptedEnvelope
├── format_version
├── algorithm
├── key_id
├── nonce
├── ciphertext
├── auth_tag / implicit tag
├── aad_version
└── optional compression metadata
```

不得依赖“代码里默认大家都知道算法和版本”。

### 4.3 AAD

Authenticated Additional Data 应绑定密文与其业务上下文。

例如：

- object_id
- owner_id
- object_type
- field_name
- crypto_version

防止密文被无声复制到另一个对象或字段后仍被接受。

### 4.4 Secure Blob

私密附件必须作为 Secure Blob 存储。

```text
Secure Attachment
      ↓
Encrypted Blob
      ↓
Blob Placement / Replica
      ↓
Storage Provider
```

Storage Provider 永远只看到密文 Blob。

普通 Blob 内容摘要不能直接替代 Secure Blob 的完整性验证机制。

如果对明文做哈希用于去重，应评估哈希本身造成的存在性泄露。

默认不跨用户对 Secure Blob 做基于明文内容的全局去重。

### 4.5 Secret Reference

上层业务若需要引用 Password、API Token、Credential 等 Secret，应使用 Secret Reference：

```text
secret://vault/{itemId}/{fieldId}
```

业务表不得复制 Secret 明文。

例如 Storage Provider：

```text
credential_ref = secret://vault/xxx/access-key
```

而不是：

```text
access_key = AKIA...
```

---

## 5. 密钥层级

推荐逻辑层级：

```text
User Secret / Device Secret / Recovery Secret
                 ↓
               KDF
                 ↓
          Root Unlock Key
                 ↓
         Key Encryption Key
                 ↓
      ┌──────────┼───────────┐
      ▼          ▼           ▼
   Vault Key   Note Key   Finance Key
      │          │           │
      ▼          ▼           ▼
   Item DEK   Item DEK    Item DEK
```

### 5.1 Root Unlock Key

Root Unlock Key 不得以可直接使用的明文形式持久化。

### 5.2 Key Encryption Key

KEK 用于包装下层数据密钥。

### 5.3 Data Encryption Key

DEK 用于实际数据加密。

不同对象、集合或 Vault 可采用独立 DEK，以降低单一密钥泄露影响范围。

### 5.4 Envelope Encryption

推荐采用 Envelope Encryption：

```text
Data
 ↓ encrypted by DEK
Ciphertext

DEK
 ↓ encrypted by KEK
Wrapped DEK
```

密钥轮换时优先重新包装 DEK，而不是必须重新加密所有大 Blob。

---

## 6. 密码与密钥派生

用户口令不得直接作为 Encryption Key。

需要 Memory-hard KDF。

具体算法参数属于详细设计，但产品与架构层必须满足：

- 可版本化
- 参数可升级
- 用户间使用独立 Salt
- 支持渐进迁移
- 不降低旧数据可恢复性

实现可优先评估 Argon2id 等现代 KDF。

---

## 7. 对称加密要求

所有数据加密必须使用具有认证能力的 AEAD 模式。

候选例如：

- XChaCha20-Poly1305
- AES-256-GCM

不得使用：

- ECB
- 无认证 CBC
- 自定义 XOR
- 自制 Crypto Scheme

算法必须通过 `crypto_version` 和 `algorithm` 可迁移。

---

## 8. Secure Session / Unlock

安全子系统与普通登录认证分离。

```text
Login Session
≠
Secure Vault Unlock Session
```

用户即使已经登录 Ikaros，也不代表私密数据已经解锁。

Secure Session 应支持：

- 手动解锁
- 自动锁定
- Idle Timeout
- App Background Lock
- Device Lock 联动
- Biometric Unlock
- Memory Key Eviction
- Explicit Lock

敏感密钥不得无限驻留内存。

---

## 9. Device Trust

支持受信设备概念：

```text
Device
├── Device Identity
├── Public Key
├── Trust Status
├── Last Used
├── Revoked At
└── Encrypted Key Material
```

用户应能够：

- 查看已授权设备
- 撤销设备
- 查看最近访问
- 重新授权

设备撤销后应阻止其继续获得新的加密密钥材料。

---

## 10. Recovery

Zero-knowledge 系统必须明确：

> 如果没有恢复机制，而用户丢失所有密钥，服务端无法“找回明文”。

因此可提供可选恢复方案：

- Recovery Key
- Recovery Code
- Hardware Security Key
- Trusted Device
- Emergency Contact
- Shamir-like 分片恢复（后续评估）

恢复能力必须显式开启，不得偷偷保留后门 Master Key。

---

## 11. Key Rotation

必须支持：

- KEK Rotation
- Vault Key Rotation
- Device Key Rotation
- Crypto Algorithm Migration
- Compromised Key Revocation

Rotation 状态应可观察：

```text
PENDING
RUNNING
PARTIAL
COMPLETED
FAILED
```

轮换不得造成业务对象无声丢失。

---

## 12. Search

### 12.1 禁止明文全文索引

USER_LOCKED_E2EE 数据不得在服务器 Search Index 中产生明文索引。

### 12.2 Client-side Search

高隐私模式优先：

```text
Encrypted Data
  ↓ client decrypt
Local Secure Index
  ↓
Search
```

客户端索引本身也必须加密落盘。

### 12.3 Blind Index

对于需要服务端等值搜索的极少数字段，可评估 keyed hash / blind index。

但必须明确泄露模型：

- 可观察相同值重复出现
- 低熵字段可能被枚举

因此不能把 Blind Index 当作普通全文搜索方案。

---

## 13. Analytics

Secure Data 默认不进入普通 Analytics Fact。

允许进入统计系统的只能是经过明确分类的非敏感衍生指标，例如：

```text
vault.item.count = 120
```

而不能包含：

```text
bank_name
account_number
password
transaction_counterparty
private_note_title
```

USER_LOCKED_E2EE 的深度统计优先在客户端本地计算。

---

## 14. AI 边界

Secure Data Foundation 默认禁止将明文自动发送给 AI Provider。

定义 AI Policy：

```text
DENY
LOCAL_ONLY
EXPLICIT_CONFIRMATION
ALLOW_TRUSTED_PROFILE
```

对于：

- Password
- TOTP Secret
- Passkey Private Material
- Recovery Code
- 银行卡 PIN
- Private Key

默认必须是：

```text
AI_ACCESS = DENY
```

即使用户启用了通用 AI，也不能自动进入上下文。

私密笔记若用户明确选择“用本地模型总结”，可以在 LOCAL_ONLY Policy 下执行。

---

## 15. Notification 边界

通知不得泄露私密数据。

错误：

```text
“招商银行卡密码已修改为 xxxxx”
```

正确：

```text
“一个密码条目已更新”
```

锁屏通知默认使用脱敏摘要。

---

## 16. Clipboard Security

密码管理等业务需要安全剪贴板策略：

- 可配置 N 秒自动清除
- 不写入应用日志
- 尽量标记 Sensitive Clipboard
- App 切后台后可清理
- 操作产生 Audit Event，但不记录内容

---

## 17. Temporary Data

解密后的临时文件必须避免普通 `/tmp` 长期残留。

如果业务必须生成临时明文：

- 使用受控临时目录
- 最短生命周期
- 权限最小化
- 使用后立即清理
- Crash Recovery Cleanup
- 不进入普通磁盘缓存

高敏感附件优先流式解密到消费端，避免完整明文落盘。

---

## 18. Backup

Secure Data Backup 必须继续保持密文。

备份内容包括：

- Ciphertext
- Wrapped Keys
- Crypto Metadata
- Version Metadata

不得为了备份“方便”先批量解密再打包。

恢复流程必须验证：

- Envelope Integrity
- Key Availability
- Crypto Version
- Object Ownership

---

## 19. Export

导出分为：

### Encrypted Export

默认推荐。

用于：

- 迁移
- 备份
- 跨客户端

### Plaintext Export

必须：

- 显式确认
- 二次认证或重新解锁
- 清楚提示风险
- 不自动上传到普通 Attachment
- 产生 Audit Event

---

## 20. Secure Delete

现代对象存储、SSD、数据库 WAL 和备份环境中无法可靠保证传统意义上的逐字节覆写。

因此安全删除主要依赖 **Crypto Erasure**：

```text
Destroy DEK / Wrapped Key
        ↓
Ciphertext becomes unrecoverable
```

同时继续正常执行：

- Blob GC
- Replica Cleanup
- Backup Retention Expiry
- Cache Cleanup

---

## 21. Audit

审计至少记录：

- Secure Vault Unlock
- Unlock Failure
- Item Create / Update / Delete
- Secret Reveal
- Secret Copy
- Plain Export
- Key Rotation
- Device Add / Revoke
- Recovery Operation
- Emergency Access
- Permission Change

不得记录敏感明文。

审计事件示例：

```text
actor=user:123
operation=SECRET_REVEAL
object=vault-item:456
result=SUCCESS
```

---

## 22. Permission

Secure Data Permission 必须叠加普通 Ikaros Identity / Permission。

```text
Platform Permission
        ∩
Resource / Vault ACL
        ∩
Secure Unlock State
        =
Effective Access
```

管理员权限不能自动等于解密权限。

这是 Zero-knowledge 模式的重要原则。

---

## 23. Sharing

私密数据分享必须使用单独安全协议，而不是普通公开 Share Link。

可支持：

- Recipient Public Key Encryption
- Shared Vault Key
- Time-limited Secure Share
- One-time Secret Share
- Revocable Access

服务端若不需要明文，则应继续只保存密文。

---

## 24. Background Task

后台任务不得把解密数据直接序列化到 Job Payload。

推荐：

```text
Job
├── secure_object_id
├── requested_operation
└── authorization_context
```

需要解密时由受控执行环境短暂获取必要密钥。

USER_LOCKED_E2EE 任务如果没有在线解锁客户端，不应假装能在服务器后台解密执行。

---

## 25. Event Integration

安全子系统可以发布事件：

```text
secure.object.created
secure.object.updated
secure.object.deleted
secure.vault.unlocked
secure.vault.locked
secure.key.rotated
secure.device.revoked
```

但事件 Payload 只携带最小化元数据。

Automation 不能通过 Event 绕过 Secure Unlock 或 Permission。

---

## 26. Threat Model

设计至少需要覆盖：

- 数据库泄露
- 对象存储泄露
- Backup 泄露
- 恶意或失陷 Storage Provider
- Server Disk 被复制
- 网络窃听
- API Token 泄露
- 设备丢失
- Session 劫持
- 日志泄露
- AI Context 泄露
- Plugin 越权
- 内部管理员试图直接读库

不声称仅靠软件可以防御：

- 已完全控制且正在解锁状态的客户端恶意软件
- 键盘记录器
- 屏幕录制
- 用户主动导出并泄露明文

---

## 27. Plugin 边界

Plugin 默认无权访问 Secure Data 明文。

需要单独 Capability：

```text
secure-data:read-metadata
secure-data:decrypt
secure-secret:reveal
secure-secret:use-without-reveal
```

优先提供 `use-without-reveal`。

例如 Plugin 需要调用第三方 API：

```text
Plugin
  ↓ request credential use
Secret Capability
  ↓
HTTP Credential Injection
```

Plugin 本身不一定需要得到 Secret 字符串。

---

## 28. 与 Password Manager 的关系

Password Manager 是 Secure Data Foundation 的高安全等级消费者。

默认：

```text
USER_LOCKED_E2EE
```

其 Secret 字段、URI、Notes、TOTP Seed、Passkey Material、附件等均使用安全基础层。

---

## 29. 与 Private Notes 的关系

Private Notes 默认使用：

```text
USER_LOCKED_E2EE
```

也可由用户创建较低安全等级 Vault 使用 SERVER_ASSISTED_ENCRYPTED，以换取服务器端搜索、AI 和 Automation。

安全等级必须由用户明确知道。

---

## 30. 与 Accounting 的关系

Accounting 中：

- Account Number
- Transaction Note
- Counterparty
- Balance
- Attachment
- Institution Metadata

都属于 Sensitive Data。

默认应用层加密落盘。

用户可选择：

- SERVER_ASSISTED_ENCRYPTED：允许服务器端统计
- USER_LOCKED_E2EE：统计主要在客户端完成

---

## 31. UX 要求

安全状态必须可见，而不是隐式。

UI 应明确显示：

- Locked / Unlocked
- Security Profile
- Last Unlock
- Device Trust
- Sync State
- Encryption Migration State
- Key Rotation State
- Recovery Configured / Not Configured

对“导出明文”“展示密码”“复制密码”“降低安全等级”等操作使用明确风险提示。

---

## 32. 非目标

本设计不要求：

- 自研密码学算法
- 通过混淆代替加密
- 为了跨用户去重牺牲 Secret 隐私
- 管理员拥有万能解密后门
- 所有安全业务强制依赖云端 KMS
- 所有 Secure Data 必须允许服务器搜索

---

## 33. 后续设计文档

后续应继续形成：

- Secure Data Cryptography Specification
- Key Hierarchy & Recovery Design
- Secure Blob Format
- Secure Sync Protocol
- Device Trust Design
- Password Manager Subsystem Design
- Private Notes Subsystem Design
- Accounting Subsystem Design

---

## 34. 核心结论

Ikaros V2 的高敏感业务不能只依靠“数据库有权限控制”来保护。

必须形成统一的安全数据基础层：

```text
Sensitive Business Data
        ↓
Secure Data Foundation
        ↓
Encrypt Before Persist
        ↓
Ciphertext Everywhere
```

并进一步保证：

> 登录权限不等于解密权限，管理员权限不等于万能解密权限，AI 权限不等于 Secret 权限。

对于 Password Manager 和最高等级 Private Notes，默认目标应是：

> Ikaros Server 即使完整数据库与对象存储被复制，也无法仅凭这些落盘数据恢复用户明文 Secret。
