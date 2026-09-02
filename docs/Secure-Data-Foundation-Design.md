# Ikaros V2 私密数据安全基础层设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Secure Data Foundation Design |
| 适用版本 | Ikaros V2 |
| 文档状态 | Draft |
| 设计目标 | 为明确声明为高敏感域的业务子系统提供“落盘即密文”的统一安全基础能力 |

---

## 1. 设计定位

Ikaros V2 中的大多数业务数据仍使用正常的平台持久化能力：

```text
Business Subsystem
      ↓
PostgreSQL / Attachment / Blob / Object Storage
```

例如：

- Resource / Collection
- 动画、影视、音乐、图片
- 普通笔记与文档
- Productivity / Planning
- Accounting 的普通账本数据
- 平台管理与运维数据
- Analytics 聚合数据

这些子系统不因为平台存在 Secure Data Foundation，就默认把全部业务数据转换成应用层密文。

Secure Data Foundation 是一个 **可选的高安全等级基础能力**。

只有明确声明进入 Secure Data Boundary 的业务对象，才使用：

```text
Sensitive Subsystem / Secure Domain
            ↓
    Secure Data Foundation
            ↓
      Encrypt Before Persist
            ↓
       Ciphertext at Rest
```

其核心目标是：

> 对于被纳入 Secure Data Foundation 管理的数据，任何持久化副本默认都只能保存密文；但该约束不扩散到整个平台的普通业务数据。

---

## 2. 适用范围

### 2.1 默认适用

当前明确使用 Secure Data Foundation 的上层业务包括：

- Private Notes
- Password Manager
- 未来明确声明为 Secure Domain 的高敏感子系统

### 2.2 默认不适用

以下业务默认使用普通平台存储，不要求全量应用层加密：

- 普通 Resource
- 普通 Attachment
- Media
- 普通 Note / Document
- Productivity / Planning
- Accounting / Personal Finance
- Analytics
- Platform Administration
- Operations

这些业务如果存在少量 Secret，应通过：

- Secret Reference
- 字段级受保护值
- Credential Store

解决，而不是把整个业务域升级成 Secure Domain。

### 2.3 子系统必须显式声明安全边界

任何业务接入 Secure Data Foundation 时，都必须明确：

```text
Secure Domain
├── 哪些对象属于安全域
├── 哪些字段属于 Protected Metadata
├── 哪些字段属于 Secret Payload
├── 哪些 Attachment 属于 Secure Blob
├── 谁可以解锁
├── 解锁发生在哪一侧
└── 哪些数据允许离开安全域
```

禁止模糊地把“比较隐私”直接等同于“整个子系统必须全部密文落盘”。

---

## 3. 核心原则

### 3.1 Secure Boundary 内禁止明文落盘

对于被 Secure Data Foundation 管理的数据：

- PostgreSQL 持久化值必须是密文或非敏感最小元数据
- Object Storage 中必须保存加密 Blob
- Server Local Cache 不得持久保存明文
- Client Persistent Cache 必须加密
- Backup 必须保持密文
- Export 默认必须加密
- Search Index 不得产生未经授权的明文索引
- Background Task Payload 不得持久保存敏感明文
- 临时文件必须受到同等级保护并及时销毁

原则：

> “全量落盘密文”是 Secure Domain 的强约束，不是 Ikaros 全平台默认约束。

### 3.2 普通业务不自动继承 Secure Boundary

例如 Accounting 可能包含敏感财务信息，但其普通交易、分类、预算和统计为了支持服务器端查询与分析，可以正常保存在业务数据库中。

如果 Accounting 需要连接银行 API：

```text
Accounting
   ↓
credential_ref
   ↓
Password Manager / Secret Store
```

而不是：

```text
Accounting 整个 Ledger
   ↓
强制 USER_LOCKED_E2EE
```

### 3.3 Application-layer Encryption

Secure Domain 内的数据不能仅依赖：

- Full Disk Encryption
- PostgreSQL Disk Encryption
- S3 Server-side Encryption
- TLS

这些能力仍属于纵深防御。

Secure Domain 的敏感数据必须在进入普通持久化层之前完成应用层加密。

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

### 3.4 Zero-knowledge Capable

Secure Data Foundation 必须支持服务端无法获得业务明文的 Zero-knowledge / E2EE 模式。

Private Notes 和 Password Manager 默认采用这一方向。

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

### 3.5 登录权限与解密权限分离

```text
Ikaros Login
    ≠
Secure Domain Unlock
```

即使用户已经登录 Ikaros，也不代表其 Private Notes 或 Password Vault 已经解锁。

同样：

```text
Platform Admin
    ≠
Secure Domain Decryption Authority
```

管理员权限不能天然成为万能解密权限。

---

## 4. Security Profile

Secure Domain 至少支持以下 Profile。

### 4.1 USER_LOCKED_E2EE

特征：

- 用户侧或受信客户端解锁
- 服务端只保存密文
- 服务端不能执行依赖明文的全文搜索、AI 和深度统计
- 适用于 Password Manager
- 适用于 Private Notes

### 4.2 SERVER_ASSISTED_ENCRYPTED

特征：

- 持久化仍为应用层密文
- 受控服务端运行时可在授权上下文中解密
- 可用于未来确实需要服务端处理、但又要求应用层加密落盘的 Secure Domain
- 不作为普通业务子系统的默认方案

### 4.3 LOCAL_ONLY

特征：

- 数据不上传服务端
- 仅客户端本地加密存储
- 可用于极端隐私场景

---

## 5. 核心数据结构

### 5.1 Secure Object

SecureObject 表达一个处于 Secure Boundary 内的逻辑安全对象。

概念模型：

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

它是一项安全契约，不要求所有上层业务强制共用一张数据库表。

### 5.2 Encrypted Envelope

密文必须使用自描述 Envelope：

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

不能只保存裸 `BYTEA` 并假设当前代码永远知道使用的算法与密钥版本。

### 5.3 Protected Metadata

Secure Domain 内需要区分：

```text
Minimal Public Metadata
Protected Metadata
Secret Payload
```

例如 Private Notes：

```text
Minimal Public Metadata
- note_id
- owner_id
- crypto_version
- revision_sequence

Protected Metadata
- title
- notebook
- tags
- attachment_filename

Secret Payload
- note body
- sensitive attachment content
```

### 5.4 Secure Blob

处于 Secure Domain 的 Attachment 使用 Secure Blob：

```text
Secure Attachment
      ↓
Encrypted Blob
      ↓
Blob Placement / Replica
      ↓
Storage Provider
```

Storage Provider 只持有密文字节。

普通 Attachment / Blob 不因此自动变成 Secure Blob。

### 5.5 Secret Reference

Secret Reference 用于普通子系统安全引用密码、Token、Credential 等 Secret：

```text
secret://vault/{itemId}/{fieldId}
```

例如：

```text
Storage Provider
credential_ref = secret://vault/.../access-key
```

```text
Accounting Bank Connector
credential_ref = secret://vault/.../token
```

引用者默认不复制 Secret 明文。

---

## 6. 密钥层级

推荐采用分层密钥结构：

```text
User Secret / Device Secret / Recovery Secret
                 ↓
               KDF
                 ↓
          Root Unlock Key
                 ↓
         Key Encryption Key
                 ↓
        Secure Domain Key
                 ↓
             Item DEK
```

关键要求：

- 用户口令不能直接作为数据加密密钥
- Root Unlock Key 不得明文持久化
- DEK 与 KEK 分离
- 支持密钥轮换
- 支持算法升级
- 支持设备撤销
- 支持 Recovery

可在详细设计阶段评估 Argon2id、AES-256-GCM、XChaCha20-Poly1305 等成熟方案。

禁止自制密码学协议。

---

## 7. Secure Session

Secure Session 管理一次解锁后的受控明文访问窗口。

支持：

- Manual Unlock
- Idle Timeout
- Explicit Lock
- App Background Lock
- Device Lock 联动
- Biometric Unlock
- Key Eviction

敏感密钥不得无限期驻留内存。

---

## 8. Device Trust 与 Recovery

受信设备可以持有经过包装的密钥材料，但必须支持：

- Device Identity
- Device Public Key
- Trust Status
- Last Used
- Revoked At
- Device Revocation

Zero-knowledge 场景必须明确恢复模型。

可支持：

- Recovery Key
- Recovery Code
- Trusted Device
- Hardware Security Key
- Emergency Contact

不得为了“方便找回”偷偷保留系统万能明文 Master Key。

---

## 9. Search / Analytics / AI 边界

### 9.1 Search

USER_LOCKED_E2EE 数据默认不进入服务端明文全文索引。

优先：

```text
Encrypted Data
  ↓ Client Decrypt
Local Secure Index
  ↓
Search
```

### 9.2 Analytics

Secure Domain 数据默认不向普通 Analytics 输出敏感字段。

允许输出经过明确批准的非敏感统计，例如：

```text
private_note.count
vault.item.count
```

禁止输出：

```text
private_note.title
password
account_secret
recovery_code
```

### 9.3 AI

Secure Domain 默认：

```text
AI_ACCESS = DENY
```

可按业务和用户设置选择：

```text
DENY
LOCAL_ONLY
EXPLICIT_CONFIRMATION
ALLOW_TRUSTED_PROFILE
```

Password、TOTP Secret、Passkey Private Material、Recovery Code、Private Key 等 Secret 默认永久禁止进入通用 LLM Context。

---

## 10. Log / Event / Notification 边界

Secure Domain 的敏感明文不得进入：

- Runtime Log
- Operation Log
- Event Payload
- Analytics Fact
- Notification Body
- Trace
- Exception Context

跨子系统联动只传播安全引用：

```text
secure_object_id
object_type
operation
actor
result
```

而不传播明文 Payload。

---

## 11. Plugin 边界

Plugin 不得因为被安装就自动获得 Secure Domain 解密权限。

能力必须显式授予，例如：

```text
secure.notes.metadata.read
secure.notes.content.read
secret.use
secret.reveal
```

其中：

```text
secret.use
≠
secret.reveal
```

平台应优先允许插件“使用 Secret 完成操作”，而不是直接读取 Secret 字符串。

---

## 12. 与上层子系统的关系

### 12.1 Private Notes

```text
Private Notes
    ↓
USER_LOCKED_E2EE
    ↓
Secure Data Foundation
```

标题、正文、标签、私密附件等处于 Secure Boundary 内。

### 12.2 Password Manager

```text
Password Manager
    ↓
USER_LOCKED_E2EE
    ↓
Secure Data Foundation
```

Vault Item、Password、TOTP、Passkey、Private Key、Recovery Code 等均处于最高安全边界。

### 12.3 Accounting

Accounting **默认不是 Secure Domain**。

```text
Accounting Business Data
    ↓
Normal PostgreSQL / Attachment Storage
```

只有真正的 Secret 使用：

```text
Secret Reference
    ↓
Password Manager / Credential Store
```

例如：

- 银行连接 Token
- API Credential
- 网银密码
- 卡片 PIN

交易、分类、预算、账户余额、统计等业务数据不要求通过 Secure Data Foundation 全量密文化。

---

## 13. 非目标

Secure Data Foundation 不负责：

- 把所有 Ikaros 数据库字段都加密
- 替代普通 Permission / ACL
- 替代磁盘加密
- 替代 TLS
- 强迫所有业务采用 Zero-knowledge
- 强迫 Accounting、Media、Planning 等普通子系统放弃服务器端查询
- 让系统管理员自动获得所有私密数据解密权限

---

## 14. 实施顺序建议

### Phase 1

- Encrypted Envelope
- Secure Object Contract
- Secure Blob
- USER_LOCKED_E2EE
- Secure Session
- Key Hierarchy
- Private Notes 接入

### Phase 2

- Password Manager 接入
- Secret Reference
- Device Trust
- Recovery
- Secure Export / Backup

### Phase 3

- SERVER_ASSISTED_ENCRYPTED
- Key Rotation
- Crypto Migration
- Plugin Secret Use
- 更完善的 Local Secure Search

---

## 15. 结论

Ikaros V2 的存储体系应明确存在两条路径：

```text
普通业务数据
    ↓
Normal Platform Storage

高敏感安全域数据
    ↓
Secure Data Foundation
    ↓
Ciphertext-only Persistence
```

Secure Data Foundation 的价值不是“把整个平台都变成密文数据库”，而是为真正需要高安全等级的数据提供一条可复用、可审计、可升级的安全存储路径。

当前明确使用该路径的核心业务是：

- Private Notes
- Password Manager

其他子系统仅在确有 Secret 或高敏感对象时，按最小范围接入，而不是默认整体接入。