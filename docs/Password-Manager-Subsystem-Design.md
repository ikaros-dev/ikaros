# Ikaros V2 密码管理子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Password Manager Subsystem Design |
| 适用版本 | Ikaros V2 |
| 文档状态 | Draft |
| 设计目标 | 提供 Zero-knowledge、端到端加密的密码、Passkey、TOTP、身份、银行卡、密钥与其他 Secret 管理能力 |

---

## 1. 产品定位

Password Manager 是 Ikaros V2 中专门管理凭据和 Secret 的高安全等级业务子系统。

产品能力可参考 Bitwarden 等成熟密码管理器的核心方向：

- Vault
- Login Credential
- Secure Note
- Card
- Identity
- Password / Passphrase Generator
- Autofill
- TOTP
- Passkey
- Encrypted Attachment
- Offline Access
- Import / Export
- Sharing
- Vault Health
- Emergency Access
- SSH Key / API Credential 等 Secret

但其数据与 Ikaros 其他子系统统一通过 Secure Data Foundation、Identity、Audit、Notification 和 Device Trust 能力衔接。

---

## 2. 最高安全等级

Password Manager 默认且必须使用：

```text
security_profile = USER_LOCKED_E2EE
```

服务端不应拥有可以直接解密用户 Vault 的万能密钥。

核心目标：

> 即使 Ikaros 数据库、对象存储和备份全部被复制，攻击者仍不能仅凭这些落盘数据恢复用户密码和 Secret。

---

## 3. 登录与 Vault 解锁分离

```text
Ikaros Login
    ≠
Password Vault Unlock
```

用户登录 Ikaros 后，Password Vault 仍可以保持 Locked。

这可以防止：

- 普通 Web Session 被盗后直接读取所有密码
- 管理员权限天然变成 Secret 解密权限

---

## 4. 核心领域模型

```text
Password Vault
│
├── Vault Item
│   ├── Login
│   ├── Secure Note
│   ├── Card
│   ├── Identity
│   ├── TOTP
│   ├── Passkey
│   ├── SSH Key
│   ├── API Credential
│   └── Custom Secret
│
├── Folder
├── Collection / Shared Vault
├── Attachment
├── Generator Policy
├── Emergency Access
└── Security Report
```

---

## 5. Vault

每个用户至少拥有一个 Personal Vault。

也可以支持：

- Family Shared Vault
- Project Shared Vault
- Separate High-security Vault

不同 Vault 可使用独立 Key。

---

## 6. Vault Item

统一 Item Envelope：

```text
VaultItem
├── id
├── vault_id
├── item_type
├── encrypted_name
├── encrypted_payload
├── encrypted_custom_fields
├── favorite
├── revision
├── created_at
├── updated_at
└── tombstone
```

除最小同步元数据外，业务字段默认全部加密。

---

## 7. Login Item

至少支持：

- Username
- Password
- URI / Origin
- Match Policy
- TOTP Reference
- Notes
- Custom Fields
- Attachment
- Password History

Login URI 本身也可能泄露用户使用哪些网站，因此默认属于加密元数据。

---

## 8. URI Match Policy

自动填充不能只做字符串包含匹配。

支持：

```text
EXACT_ORIGIN
HOST
BASE_DOMAIN
STARTS_WITH
REGEX_ADVANCED
NEVER_AUTOFILL
```

默认优先安全的 Origin / Host 匹配。

对可疑域名不自动填充。

---

## 9. Autofill

目标客户端：

- Browser Extension
- Android Autofill Service
- iOS Password AutoFill / Credential Provider
- Desktop Integration（按 OS 能力）

Autofill 流程：

```text
Current Origin
    ↓
Local Vault Match
    ↓
Phishing / Match Policy Check
    ↓
User Gesture / Policy
    ↓
Fill Credential
```

密码不需要发送回 Ikaros Server 才能 Autofill。

---

## 10. Phishing Protection

当用户尝试在不匹配的 Origin 使用密码时，应提示：

```text
Saved: https://example.com
Current: https://examp1e.com
```

默认不静默填充。

---

## 11. Password Generator

支持生成：

- Password
- Passphrase
- Username

Password Policy：

- Length
- Uppercase
- Lowercase
- Number
- Symbol
- Avoid Ambiguous
- Minimum Numbers
- Minimum Symbols

不得使用可预测伪随机数。

---

## 12. Passphrase Generator

支持：

- Word Count
- Separator
- Capitalization
- Number Suffix

随机源必须使用 CSPRNG。

---

## 13. Secure Note

Password Manager 内的 Secure Note 用于短 Secret / Recovery 信息。

与 Private Notes 区分：

```text
Password Secure Note
= 凭据 Vault 的 Secret Item

Private Notes
= 完整的私密知识 / 笔记业务系统
```

长篇日记或复杂笔记优先使用 Private Notes。

---

## 14. Card Item

支持保存：

- Cardholder Name
- Card Number
- Expiration
- Security Code
- Billing Metadata
- Notes

UI 默认只显示：

```text
•••• •••• •••• 1234
```

Reveal Security Code 需要 Vault 解锁状态，并可按策略再次验证。

银行卡 PIN 不应与普通展示字段混在一起，应作为独立高敏感 Secret Field。

---

## 15. Identity Item

可保存：

- Name
- Address
- Email
- Phone
- Identification Metadata
- Custom Fields

用于安全 Autofill。

---

## 16. TOTP

支持存储 TOTP Seed 并生成验证码。

Seed 必须作为 Secret 加密。

支持：

- otpauth URI Import
- QR Scan
- Period
- Digits
- Algorithm

TOTP Code 可以短暂显示，但不得写入 Log、Clipboard History 或普通 Notification。

---

## 17. Passkey

Password Manager 应支持 Passkey 管理作为正式能力，而不是只保存“Passkey 备注”。

逻辑对象需要支持：

- RP ID
- Credential ID
- User Handle
- Display Metadata
- Private Key Material
- Created At
- Last Used At

Private Key Material 必须处于 E2EE Secret Payload 中。

具体平台集成遵循 WebAuthn / OS Credential API 的能力边界。

---

## 18. SSH Key

支持：

- Private Key
- Public Key
- Comment
- Fingerprint
- Algorithm
- Optional Passphrase

P1 可提供 Desktop SSH Agent Integration。

优先实现：

```text
Use Key Without Revealing Raw Private Key
```

---

## 19. API Credential

支持保存：

- API Key
- Token
- Client ID
- Client Secret
- Endpoint
- Expiration
- Scope

可被 Ikaros Plugin / Integration 通过 Secret Reference 使用。

---

## 20. Custom Secret Fields

字段类型：

```text
TEXT
HIDDEN
BOOLEAN
LINKED_SECRET
```

Hidden Field 默认：

- 不显示
- 不进入普通搜索
- 不进入 AI
- Copy 后自动清理 Clipboard

---

## 21. Secret Reference

Password Manager 是平台 `secret://` Reference 的主要 Provider。

```text
secret://vault/{vaultId}/item/{itemId}/field/{fieldId}
```

其他子系统引用 Secret 时只保存 Reference。

例如：

```text
StorageProvider
credential_ref = secret://vault/...
```

---

## 22. Use Without Reveal

优先支持“使用 Secret”与“展示 Secret”分离。

```text
Subsystem
    ↓
Request Credential Use
    ↓
Password Manager Capability
    ↓
Inject Secret into outbound operation
```

理想情况下调用方不需要得到原始 Secret 字符串。

这对：

- Plugin
- HTTP Connector
- Storage Provider
- External Integration

特别重要。

---

## 23. Folder

Folder 用于个人组织：

- Work
- Personal
- Finance
- Servers

Folder 名称默认加密。

---

## 24. Shared Vault / Collection

共享不能通过复制一份明文密码实现。

需要共享 Vault Key / Item Key。

权限至少：

```text
OWNER
MANAGER
EDITOR
VIEWER
```

并可进一步控制：

- Reveal Secret
- Autofill
- Edit
- Share
- Export

---

## 25. Offline-first

密码管理器必须支持离线访问。

客户端保存：

```text
Encrypted Vault Cache
```

解锁后本地使用。

断网时仍可以：

- 查找 Login
- Autofill
- 生成密码
- 查看 TOTP
- 新建 / 修改 Item

联网后同步 Ciphertext Delta。

---

## 26. Sync

同步不要求服务端理解 Secret Payload。

```text
Vault Item Ciphertext
        ↓
Revision / ETag
        ↓
Sync Server
```

支持：

- Incremental Sync
- Conflict Detection
- Device Resume
- Tombstone
- Attachment Sync

---

## 27. Conflict

Secret 冲突不能简单 Last Write Wins。

若两个设备同时修改 Password：

- 保存 Conflict Version
- 提醒用户
- 不静默丢弃一边

Password History 可以帮助恢复。

---

## 28. Password History

用户修改 Password 后，可选择保留加密历史。

历史也属于 Secret。

Retention 可配置：

- Off
- Last N
- N Days

---

## 29. Vault Health

支持本地或隐私保护方式检查：

- Weak Password
- Reused Password
- Old Password
- Missing 2FA Metadata
- Insecure HTTP URI
- Duplicate Item
- Exposed Credential（可选外部数据源）

Health Report 不应把用户完整密码发送给外部服务。

---

## 30. Breach Check

若接入外部泄露密码检测，必须采用隐私保护协议，例如只发送足以完成 k-anonymity 类查询的有限摘要前缀，而不是密码或完整哈希。

具体 Provider 属于插件/集成能力。

---

## 31. Emergency Access

支持可选 Emergency Contact。

典型模式：

```text
Trusted Contact Requests Access
        ↓
Waiting Period
        ↓
Owner can reject
        ↓
Timeout / Explicit Approve
        ↓
Recover allowed scope
```

必须明确：

- Access Scope
- Waiting Period
- Revoke
- Audit

Emergency Access 必须是用户主动配置的能力，不能成为系统管理员后门。

---

## 32. Account Recovery 与 Vault Recovery

平台账号能恢复，不代表 Vault 一定能恢复。

```text
Ikaros Account Recovery
        ≠
Password Vault Key Recovery
```

UI 必须明确区别。

---

## 33. Biometric Unlock

客户端可支持：

- Windows Hello
- Touch ID
- Face ID
- Android Biometrics

生物识别用于保护本地包装密钥/解锁流程，不把生物特征数据上传到 Ikaros。

---

## 34. Hardware Security Key

可用于：

- Ikaros Account 2FA
- Vault Unlock Key Protection（后续设计）
- Recovery

具体方案应避免混淆认证 Key 与数据加密 Key。

---

## 35. Clipboard

Copy Username / Password / TOTP 后支持：

- 15 / 30 / 60 / 120 秒自动清理
- 用户自定义
- Immediately on Lock

不得将 Clipboard 内容写入 Activity。

Audit 只记录：

```text
PASSWORD_COPIED
```

不记录 Password。

---

## 36. Reveal

Secret Reveal 可配置：

- Require Unlocked Vault
- Require Re-auth
- Require Biometric

重要 Secret 可单独设置更严格策略。

---

## 37. Attachments

支持加密附件：

- Recovery PDF
- License File
- Private Key File
- Certificate
- Scan

Attachment 使用 Secure Blob。

默认禁止解密内容进入普通 Download Cache。

---

## 38. Import

支持未来导入：

- Bitwarden JSON / CSV
- 1Password-compatible Export
- KeePass CSV / XML
- Browser Password Export
- Generic CSV

安全流程：

```text
Plain Import File
      ↓ Local Parse
Encrypt Immediately
      ↓
Vault
      ↓
Warn User to securely remove source file
```

不上传明文 Import File 到服务器后台再处理。

---

## 39. Export

默认推荐：

```text
Encrypted Vault Export
```

Plaintext Export 必须：

- Re-auth
- Explicit Warning
- Audit
- Local Client Generation

服务器端不应该为了 Export 请求解密整个 USER_LOCKED_E2EE Vault。

---

## 40. Secure Send

P1 可提供一次性安全发送：

- Text Secret
- File

能力：

- Expiration
- Max Access Count
- Password Protection
- Delete After View
- Revocation

服务端尽量只保存密文。

---

## 41. AI Boundary

Password Manager 对 AI 的默认策略必须比其他系统更严格：

```text
Password / TOTP / Passkey / Private Key / Recovery Code
AI_ACCESS = DENY
```

即使使用 Local Model，也默认不把原始 Secret 放进 Prompt。

AI 可以处理非 Secret 的可选任务，例如：

- 建议 Folder
- 解释某项安全风险
- 生成密码规则说明

但不读取 Password 本身。

---

## 42. Search

Vault Search 默认客户端执行。

可搜索：

- Item Name
- Username
- URI
- Folder
- Non-secret Custom Field

这些索引同样必须加密落盘。

Password Field 不参与全文搜索。

---

## 43. Analytics Boundary

允许：

```text
vault.item.count
vault.weak_password.count
vault.reused_password.count
```

不允许 Analytics Fact 保存：

- Password
- Username
- URL
- TOTP Seed
- Card Number

统计优先本地计算后只展示给用户。

---

## 44. Audit

安全事件：

- Vault Unlock Success / Failure
- Secret Reveal
- Secret Copy
- Autofill
- Item Create / Update / Delete
- Export
- Import
- Device Add / Revoke
- Shared Access Change
- Emergency Access Request / Approve / Reject
- Recovery
- Key Rotation

所有 Audit Payload 必须脱敏。

---

## 45. Device Management

用户可以查看：

```text
Device
├── Name
├── Type
├── Last Sync
├── Last Unlock
├── Trust State
└── Revoke
```

设备丢失后可以撤销其继续同步新密钥材料的权限。

已经复制到该设备且可被其本地密钥解锁的数据，需要在 Threat Model 中诚实说明无法远程“收回历史字节”。

---

## 46. Lock Policy

支持：

- Immediately
- App Background
- Browser Restart
- System Lock
- Idle N Minutes
- Device Restart

Vault Lock 后：

- 清除内存 Key
- 隐藏 Item Content
- 清理 Clipboard
- 关闭 Secret Viewer

---

## 47. Browser Extension

密码管理业务要完整，P1 应有浏览器扩展。

主要能力：

- Vault Search
- Autofill
- Save New Login
- Update Existing Login
- Password Generator
- Passkey Integration
- TOTP Fill
- Lock / Unlock

扩展与主服务交互必须保持 E2EE 边界。

---

## 48. Mobile Integration

移动端：

- Autofill
- Biometric Unlock
- TOTP
- Passkey Provider（平台支持时）
- QR Import
- Secure Clipboard

---

## 49. Desktop Integration

P1/P2：

- Global Quick Search
- SSH Agent
- Browser Native Messaging
- Biometric Unlock
- Secret Injection for CLI

---

## 50. CLI / Developer Secrets

P2 可提供安全 CLI：

```text
ikaros secret get <ref>
ikaros secret exec -- <command>
```

优先：

```text
secret exec
```

让 Secret 仅进入目标进程环境/标准输入，而不是打印到终端。

---

## 51. Plugin Integration

Plugin 默认不能 Reveal Secret。

权限能力拆分：

```text
secret:list-metadata
secret:use
secret:reveal
```

`secret:use` 与 `secret:reveal` 必须是不同权限。

---

## 52. Accounting Integration

Accounting Account 可以引用网银 Credential：

```text
Account
  └── LOGIN_WITH → Secret Reference
```

Accounting 不复制 Password。

---

## 53. Private Notes Integration

Private Note 可以引用 Secret Item。

显示：

```text
[Bank PIN Secret]
```

Reveal 时调用 Password Manager Capability。

私密笔记正文不保存 Secret 副本。

---

## 54. Password Change Workflow

可支持：

```text
Open Website
    ↓
Generate New Password
    ↓
Update Website
    ↓
Update Vault Item
    ↓
Store Previous Password in History
```

P2 可探索自动密码更新，但高风险且依赖站点能力，不作为 P0。

---

## 55. Passkey Migration / Portability

Passkey 导入导出必须遵循平台和标准实际支持能力。

不得设计一个实际上无法被浏览器/OS 使用的“伪 Passkey JSON”。

---

## 56. Data Lifecycle

```text
ACTIVE
 ↓
TRASH
 ↓
PURGED
 ↓
CRYPTO_ERASURE
```

Trash 仍然是密文。

对于被撤销共享成员，需要评估后续密钥轮换。

---

## 57. P0

- Personal Vault
- USER_LOCKED_E2EE
- Login Item
- Secure Note
- Card
- Identity
- Custom Fields
- Password / Passphrase Generator
- TOTP
- Folder
- Favorite
- Offline Encrypted Cache
- Sync
- Password History
- Search
- Biometric-capable Unlock Architecture
- Encrypted Import / Export Path
- Audit
- Device Management
- Secret Reference

---

## 58. P1

- Browser Extension
- Android / iOS Autofill
- Passkey
- Shared Vault
- Encrypted Attachment
- Vault Health
- Secure Send
- Emergency Access
- SSH Key
- API Credential

---

## 59. P2

- SSH Agent
- CLI Secret Exec
- Advanced Shared Organization
- Hardware-backed Unlock
- Password Change Automation
- Advanced Developer Secret Integration

---

## 60. 非目标

Password Manager 不应该：

- 把密码当普通 Note 文本存储
- 把 Vault Key 明文保存在服务器数据库
- 允许 Admin 默认解密所有用户 Vault
- 把 Password 发给 AI 做“分析”
- 为了服务器全文检索而建立 Password 明文索引
- 在日志中记录 Autofill 内容
- 把普通 Ikaros Login Session 当作 Vault 解锁

---

## 61. 核心结论

Password Manager 应成为 Secure Data Foundation 上安全级别最高的正式业务子系统之一：

```text
Password Manager
        ↓
USER_LOCKED_E2EE
        ↓
Secure Data Foundation
        ↓
Ciphertext Sync / Storage
```

同时通过 Secret Reference 与 Ikaros 其他子系统联动：

```text
Password Vault
   ├── Accounting
   ├── Storage Provider
   ├── Plugin
   ├── External Integration
   └── Private Notes
```

联动的核心不是“让更多模块看到密码”，而是：

> 让其他模块能够在最小权限下安全使用 Secret，并尽可能做到 Use Without Reveal。
