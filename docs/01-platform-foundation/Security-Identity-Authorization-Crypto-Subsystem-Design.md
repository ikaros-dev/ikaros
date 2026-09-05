# Ikaros V2 安全认证、授权与密码学子系统设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Security / Identity / Authorization / Crypto Subsystem Design |
| 适用版本 | Ikaros V2 |
| 文档状态 | Draft |
| 设计目标 | 统一负责身份认证、JWT Token 校验、授权、Step-up Verification、Secure Domain 加解密、密钥版本与恢复 |

---

## 1. 子系统定位

Ikaros V2 需要一个统一的 **Security Subsystem**，作为平台身份、安全与密码学能力的核心边界。

该子系统负责：

- Authentication：确认“你是谁”
- Stateless JWT Validation：校验签名、有效期、用户状态与 `security_version`
- Authorization：确认“你可以做什么”
- Step-up Verification：高风险操作前提升认证保证等级，并签发短期 Purpose-bound Grant
- Secure Domain Unlock：管理客户端 / Secure Domain 的敏感数据解锁边界，不等同于服务端登录 Session
- Key Management：密钥创建、包装、版本、轮换、恢复与撤销
- Crypto Capability：对 Secure Domain 数据执行受控加密与解密
- Security Audit：记录所有高风险认证、授权、密钥与解密行为

该子系统不是普通业务模块，也不是单纯的 RBAC 服务。

它是：

```text
Identity
   +
Authentication
   +
Stateless JWT Validation
   +
Authorization
   +
Verification Assurance / Step-up Grant
   +
Key Management
   +
Crypto Capability
```

的统一平台能力。

### 1.1 登录认证明确采用无状态 JWT

Ikaros V2 登录认证基于 JWT Token，服务端不建立、持久化或管理：

```text
Login Session
Security Session
Session ID / sid
Per-device Session
Session Token Digest
Refresh Token Digest as session state
```

JWT 是自包含签名凭据。服务端对请求进行 Token 校验，不依赖“查找当前登录会话”来恢复认证状态。

因此：

- 不存在“活跃登录会话列表”；
- 不存在“撤销单个服务端 Session”；
- 普通 Logout 只清除客户端本地 Token / Credential Cache；
- Token 正常失效依赖 `exp`；
- 需要让某用户全部既有 JWT 提前失效时，提升该用户的 `security_version`；
- Step-up Verification 产生短时、Purpose-bound 的签名 Grant，而不是修改服务端 Session 状态。

业务领域中的 `Playback Session`、`Reading Session`、`Upload Session`、AI Conversation 等独立业务对象不属于这里所说的登录 Session，不受此规则影响。

---

## 2. 核心边界

### 2.1 普通登录、业务权限与安全认证必须分离

三个概念不能混在一起：

```text
Authentication
证明当前主体是谁

Authorization
判断当前主体是否拥有目标业务权限

Security Verification / Step-up
确认当前主体是否以足够高的可信等级执行某个高风险操作
```

例如某用户拥有：

```text
permission = private_note.key.reset
```

并不代表可以立即执行密钥重置。

还需要满足目标操作要求的 Verification Level。

完整判定：

```text
Authenticated
   AND
Authorized
   AND
Verification Level >= Required Level
   AND
Security Policy Allows
   ↓
Execute Security Command
```

### 2.2 高等级认证不自动授予业务权限

产品上存在：

```text
人脸识别 > 身份证认证 > 手机短信 > 邮件验证码
```

这一顺序表达的是 **身份与操作认证保证等级**，而不是 RBAC 权限等级。

例如：

```text
普通用户 + 人脸认证
```

不能因此获得：

```text
ADMIN
USER_MANAGEMENT
SYSTEM_CONFIG_WRITE
```

高保证认证只允许用户在其原本授权范围内执行更高风险的动作。

因此：

> Verification Level 可以限制 Permission 的使用，但不能创造 Permission。

### 2.3 Crypto 只能通过受控 Capability 使用

上层业务不得直接：

- 获取 Root Key
- 获取 KEK 明文
- 获取其他 Vault 的密钥
- 绕过 Security Subsystem 调用底层 Crypto Library
- 直接读取 Key Snapshot

推荐调用：

```text
Private Notes
   ↓
SecureCryptoCapability.decrypt(...)
   ↓
Security Context / Permission / Verification Check
   ↓
Key Resolution
   ↓
Crypto Runtime
```

---

## 3. Security Verification Level

为避免与外部标准中的 AAL / IAL 概念直接混淆，Ikaros 内部定义：

**SVL（Security Verification Level）**。

当前规划：

| SVL | 验证方式 | 状态 | 相对等级 |
|---|---|---|---|
| SVL-1 | Email OTP | V2 初期实现 | 最低 |
| SVL-2 | SMS OTP | 规划 | 高于 Email |
| SVL-3 | Identity Document Verification | 规划 | 高于 SMS |
| SVL-4 | Face Identity Verification | 规划 | 最高 |

产品等级：

```text
SVL-4 Face
   >
SVL-3 Identity Document
   >
SVL-2 SMS
   >
SVL-1 Email
```

不同操作定义：

```text
required_verification_level
```

例如未来：

```text
普通资料修改                  SVL-1
绑定新手机号                  SVL-2
修改高安全恢复策略            SVL-3
重置最高级 Secure Vault Key   SVL-4
```

在对应等级尚未实现时，由部署策略决定：

- 暂时降级到当前最高可用等级
- 或禁用该高风险能力

不能在代码中悄悄绕过认证等级。

---

## 4. 当前 V2 初期范围：Email OTP

当前阶段只实现：

```text
SVL-1 = EMAIL_OTP
```

短信、身份证和人脸认证只保留：

- Domain Model
- Provider Interface
- Verification Level
- Policy Extension Point

不在当前阶段实现实际认证服务。

### 4.1 Email OTP 使用场景

V2 初期 Email OTP 可用于：

- 邮箱所有权验证
- 登录 Step-up Verification
- 安全设置变更确认
- Secure Domain Recovery 流程确认
- 密钥重置确认（仅允许于支持 Recovery 的安全 Profile）

### 4.2 Email OTP 安全要求

OTP 必须：

- 一次性使用
- 短时有效
- Purpose-bound
- User-bound
- Challenge-bound
- 验证成功后立即失效
- 超时失效
- 达到失败次数后锁定 Challenge
- 具备发送频率限制
- 具备验证频率限制
- 不写入明文日志
- 不进入 Analytics Fact
- 不进入 Notification Body Audit

逻辑模型：

```text
VerificationChallenge
├── id
├── user_id
├── method = EMAIL_OTP
├── purpose
├── target_reference
├── otp_digest
├── issued_at
├── expires_at
├── attempt_count
├── max_attempts
├── consumed_at
└── status
```

只保存 OTP 的不可逆校验值，不保存可直接读取的 OTP 明文。

`VerificationChallenge` 是一次性验证对象，不是登录 Session。

### 4.3 Purpose Binding

验证码必须绑定用途。

例如：

```text
LOGIN_STEP_UP
CHANGE_SECURITY_SETTING
RESET_SECURE_KEY
EXPORT_SECURE_VAULT
CHANGE_RECOVERY_POLICY
```

收到：

```text
purpose = LOGIN_STEP_UP
```

的验证码不得拿去执行：

```text
RESET_SECURE_KEY
```

### 4.4 Anti-enumeration

邮件验证码接口不得通过响应暴露：

- 某邮箱是否存在
- 某用户是否启用了 Secure Vault
- 某用户是否拥有特定 Secret

外部响应应保持统一语义。

---

## 5. 未来 SMS OTP

规划等级：

```text
SVL-2
```

实现时需要：

- Phone Ownership Verification
- OTP Challenge
- Rate Limit
- Anti-enumeration
- SIM Swap 风险控制
- 最近手机号变更冷却期
- Recovery 操作风险策略

手机号刚完成修改时，不应立即允许利用新号码执行最高风险恢复操作。

---

## 6. 未来身份证认证

规划等级：

```text
SVL-3
```

Identity Document Verification 不应只是：

```text
上传一张身份证照片
```

未来应至少考虑：

- Document Authenticity
- Document Expiration
- Name / Identity Match
- Document Number Protection
- Front / Back Validation
- OCR Result Verification
- Data Minimization
- Identity Proofing Audit
- Sensitive Identity Data Retention Policy

原始证件图属于极高敏感数据。

默认不应无限期保存原始证件照片。

如业务只需要保留“已经通过认证”的事实，则应优先保存：

```text
verification_result
provider
assurance_level
verified_at
expiry
minimal evidence reference
```

而不是永久保存完整身份证影像。

---

## 7. 未来人脸识别认证

规划等级：

```text
SVL-4
```

这是 Ikaros 规划中的最高 Security Verification Level。

可用于：

- 最高等级 Secure Vault 恢复
- 重置关键恢复策略
- 重建 Root Unlock / Recovery Binding
- 高风险密钥管理
- 其他最高风险安全操作

### 7.1 人脸不是“超级管理员身份”

人脸通过表示：

> 当前执行者以最高认证保证等级证明了自己的身份。

它不表示：

> 当前执行者自动拥有整个 Ikaros 的所有业务权限。

仍然必须：

```text
RBAC / ACL Permission
   AND
SVL-4
```

### 7.2 人脸认证必须考虑活体检测

未来实现 Face Identity Verification 时，不允许仅进行普通照片相似度比较。

需要考虑：

- Liveness / Presentation Attack Detection
- Replay Protection
- Camera / Device Trust
- Verification Challenge
- Face Template Protection
- Biometric Revocation Strategy
- Failure Rate Limit
- Anti-spoofing

对于最高等级操作，Face Verification 应与受信设备或其他认证因子绑定，而不是把裸人脸模板当成独立万能密码。

这是因为生物特征不是可自由更换的 Secret，一旦泄露不能像密码一样简单重置。

未来实现应参考 NIST SP 800-63B 等成熟数字身份规范对生物特征认证、多因素认证与 Presentation Attack Detection 的要求。

### 7.3 Biometric Data Protection

以下内容均属于最高敏感等级：

- Raw Face Image
- Face Template
- Embedding
- Liveness Evidence
- Verification Video

不得进入：

- 普通 Blob
- 普通 Search
- Analytics
- AI Context
- Runtime Log
- Plugin API

如必须持久化，应进入专用 Secure Domain，并设置严格 Retention Policy。

---

## 8. Stateless JWT 与 Step-up Verification

### 8.1 普通登录 JWT

普通登录成功后签发自包含 JWT。至少需要表达：

```text
AccessJwt
├── sub = user_id
├── security_version
├── iat
├── exp
├── issuer / audience（启用时）
└── authorization snapshot / references（按实现策略）
```

服务端不为该 Token 建立 `SecuritySession` 行，也不要求 JWT 包含 `sid` 来关联服务端登录状态。

每次请求至少校验：

```text
signature valid
AND exp / nbf / iat valid
AND subject user exists and is allowed to authenticate
AND token.security_version == user.security_version
```

JWT 正常到期由 `exp` 控制。

需要提前使该用户所有旧 JWT 失效时：

```text
identity.invalidate-user-tokens
        ↓
increment user.security_version
        ↓
all JWTs carrying previous security_version become invalid
```

这是一种用户级 Token Epoch，不是 Session Store。

### 8.2 Step-up 使用短期 Verification Grant

完成 Email OTP / SMS / Identity / Face Verification 后，不修改某个服务端 Session 的 `current_svl`。

Security Subsystem 签发短期、Purpose-bound 的验证结果，例如：

```text
VerificationGrant
├── sub = user_id
├── method
├── achieved_svl
├── purpose
├── target_reference?
├── verified_at
├── iat
└── exp
```

`VerificationGrant` 可以实现为独立签名 JWT 或等价不可伪造凭据。

高风险 Command 在执行时同时校验：

```text
Access JWT
AND Permission
AND Verification Grant
AND purpose / target binding
AND achieved_svl >= minimum_svl
AND Security Policy
```

Verification Grant 只证明一次短期提升验证，不延长普通 Access JWT 生命周期，也不形成服务端登录 Session。

### 8.3 Verification 有独立有效期

高等级验证不能无限有效。

例如：

```text
Access JWT TTL          deployment policy
SVL-1 Step-up Grant TTL short-lived
SVL-4 Step-up Grant TTL very short-lived
```

具体时长属于详细设计和安全策略。

高风险操作可以要求：

```text
fresh_verification = true
```

即使客户端仍持有未过期的旧 Step-up Grant，也可以要求重新验证。

---

## 9. Security Policy

每种高风险 Command 必须定义安全策略。

逻辑模型：

```text
SecurityPolicy
├── action
├── permission
├── minimum_svl
├── require_fresh_verification
├── allowed_methods
├── cooldown
├── risk_rules
└── audit_level
```

例如：

```text
action = RESET_PRIVATE_VAULT_KEY
permission = private_note.key.reset
minimum_svl = SVL-1   // 当前阶段临时值
require_fresh_verification = true
allowed_methods = EMAIL_OTP
```

未来人脸认证上线后可以调整为：

```text
minimum_svl = SVL-4
allowed_methods = FACE
```

而不需要重写业务逻辑。

---

## 10. Secure Domain Recovery Model

密钥恢复是 Security Subsystem 与 Secure Data Foundation 的交叉核心能力。

必须明确：

> “忘记用户密钥后，平台还能通过身份认证恢复旧数据”意味着系统必须存在受保护的 Key Recovery Path。

因此定义两种恢复模式。

### 10.1 RECOVERABLE_SECURE

适用于希望：

- 忘记密钥后仍可恢复
- 通过高保证身份认证重置密钥
- 继续读取历史密文

的 Secure Domain。

逻辑：

```text
User Unlock Secret
        ↓
Current Key Binding
        ↓
Vault Key

同时：

Vault Key Version
        ↓ wrapped by
Recovery Key / Recovery Envelope
        ↓
Protected Key Snapshot
```

当用户忘记原密钥：

```text
Step-up Verification
        ↓
Recovery Authorization
        ↓
Recover historical wrapped key material
        ↓
Create new active key version
        ↓
Re-wrap historical key chain
        ↓
Continue decrypting historical ciphertext
```

### 10.2 ZERO_KNOWLEDGE

严格 Zero-knowledge 模式下：

```text
Server
```

不存在一个仅凭账号身份认证即可恢复用户历史解密密钥的后门。

此模式下忘记密钥后，只能通过：

- 用户持有的 Recovery Key
- Trusted Device
- Hardware Key
- 其他预先建立的用户控制恢复机制

恢复。

Email / SMS / Identity / Face Verification 只能确认身份，不能凭空重新生成丢失的解密密钥。

因此：

```text
Identity Recovery
≠
Cryptographic Recovery
```

除非用户选择了 `RECOVERABLE_SECURE`。

---

## 11. 当前阶段恢复策略

当前 V2 初期只实现：

```text
EMAIL_OTP
```

因此对于 `RECOVERABLE_SECURE` Domain，可以配置：

```text
minimum_recovery_svl = SVL-1
```

允许：

```text
Email OTP
   ↓
Recovery Authorization
   ↓
Reset Key
```

但必须把这一事实在 UI 明确展示为安全等级较低的恢复方式。

未来更高等级认证可用后，应支持管理员或用户把恢复策略提升到：

```text
SVL-2
SVL-3
SVL-4
```

并可选择：

```text
禁止低等级 Recovery Fallback
```

例如用户启用：

```text
minimum_recovery_svl = SVL-4
```

后，Email OTP 即使验证成功，也不能降级执行 Key Reset。

---

## 12. Key Ring

Secure Domain 的密钥不能被理解为“只有一个当前密码”。

需要完整 Key Ring。

```text
KeyRing
│
├── KeyVersion 1  DECRYPT_ONLY
├── KeyVersion 2  DECRYPT_ONLY
├── KeyVersion 3  DECRYPT_ONLY
└── KeyVersion 4  ACTIVE
```

每份 Ciphertext 必须记录：

```text
key_version
crypto_version
```

解密时：

```text
Ciphertext
   ↓ key_version = 2
Key Ring
   ↓
Resolve Key Version 2
   ↓
Decrypt
```

---

## 13. Key Snapshot

用户要求的“旧密钥保留快照”不能保存裸密钥明文。

定义：

```text
WrappedKeySnapshot
```

逻辑结构：

```text
WrappedKeySnapshot
├── key_ring_id
├── key_version
├── algorithm
├── wrapped_key_material
├── wrapping_key_reference
├── created_at
├── activated_at
├── retired_at
├── status
└── recovery_policy_version
```

其中：

```text
wrapped_key_material
```

必须仍然是被安全包装后的密文。

禁止：

```text
old_key_plaintext
```

直接落盘。

---

## 14. Key Version State

至少支持：

```text
PENDING
ACTIVE
DECRYPT_ONLY
REVOKED
COMPROMISED
DESTROY_PENDING
DESTROYED
```

正常轮换：

```text
ACTIVE V3
   ↓ reset / rotate
DECRYPT_ONLY V3
ACTIVE V4
```

新数据只使用 V4。

旧数据仍可以使用 V3 解密。

---

## 15. 密钥重置

Key Reset 与简单修改密码不同。

完整流程：

```text
User requests key reset
        ↓
Permission Check
        ↓
Fresh Step-up Verification
        ↓
Recovery Policy Check
        ↓
Create Recovery Operation
        ↓
Resolve historical key snapshots
        ↓
Create new Key Version
        ↓
Set previous ACTIVE → DECRYPT_ONLY
        ↓
Set new version → ACTIVE
        ↓
Re-wrap Key Chain if required
        ↓
Raise user.security_version if policy requires old JWT invalidation
        ↓
Security Audit
        ↓
Notify User
```

任何失败都不得造成旧 Key Snapshot 被覆盖。

---

## 16. 历史数据迁移

密钥重置后不要求立即重新加密所有历史大对象。

可以采用：

### 16.1 Keep Historical Key Version

```text
Old Ciphertext
→ Old Key Version
```

继续读取。

### 16.2 Lazy Re-encryption

读取旧数据时：

```text
Decrypt with V2
   ↓
Re-encrypt with ACTIVE V4
   ↓
Persist new ciphertext
```

### 16.3 Background Migration

后台任务：

```text
ReEncryptSecureDataTask
```

批量将旧版本密文迁移到当前 Active Key。

迁移过程必须：

- 可暂停
- 可重试
- 有进度
- 幂等
- 保留旧密文直到新密文验证成功
- 失败不得导致数据不可恢复

---

## 17. Key Snapshot 删除规则

旧 Key Snapshot 不能因为轮换完成立即删除。

只有同时满足：

```text
没有 Ciphertext 引用该 key_version
AND
没有 Backup / Revision 引用
AND
Retention Policy 允许
AND
没有 Recovery Hold
AND
没有 Security Incident Hold
```

才可以进入：

```text
DESTROY_PENDING
```

最终执行 Crypto Erasure。

---

## 18. Recovery Operation

每次恢复都必须拥有独立业务对象。

```text
RecoveryOperation
├── id
├── user_id
├── secure_domain
├── target_key_ring
├── requested_at
├── requested_by
├── required_svl
├── achieved_svl
├── verification_method
├── policy_version
├── old_active_version
├── new_active_version
├── status
└── completed_at
```

状态：

```text
REQUESTED
VERIFYING
AUTHORIZED
ROTATING
REWRAPPING
COMPLETED
FAILED
CANCELLED
```

---

## 19. Cooldown 与风险控制

高风险恢复不一定需要验证成功后立即生效。

未来可以根据 Security Policy 支持：

```text
cooldown = 24h
```

例如：

```text
新的身份证认证完成
   ↓
申请重置 Password Vault Root Key
   ↓
24h Security Cooldown
   ↓
通知全部受信设备
   ↓
无撤销
   ↓
执行恢复
```

当前 Email-only MVP 可以先不启用长 Cooldown，但数据模型与 Policy 应预留。

---

## 20. Recovery Notification

以下事件必须通知用户：

- 发起 Key Reset
- Email OTP 验证成功
- Email OTP 连续失败
- Recovery Operation 创建
- 新 Key Version 激活
- Trusted Device 被撤销
- Recovery Policy 变更
- Secure Vault Export
- 高等级 Verification 完成

通知内容不得包含：

- Secret
- Key Material
- OTP
- Private Note Title
- Vault Item 内容

---

## 21. Security Audit

必须独立记录：

```text
AUTHENTICATION
VERIFICATION
AUTHORIZATION_DENIED
SECURE_UNLOCK
SECURE_LOCK
DECRYPT
KEY_RESET
KEY_ROTATION
KEY_RECOVERY
KEY_SNAPSHOT_ACCESS
RECOVERY_POLICY_CHANGE
TRUSTED_DEVICE_CHANGE
TOKEN_INVALIDATION
```

对于普通解密操作，可以采用安全聚合或采样策略避免产生不可控日志量；高风险密钥操作必须完整审计。

Audit 不得保存明文 Secret、JWT 或 Step-up Grant。

---

## 22. 与 RBAC / ACL 的关系

Platform Permission 继续负责：

```text
security.policy.read
security.policy.write
security.audit.read
user.security.manage
```

Resource / Secure Domain ACL 继续负责：

```text
private_note.read
private_note.write
private_note.key.reset
password_vault.read
password_vault.export
password_vault.key.reset
```

Security Verification 负责：

```text
这些 Permission 在当前风险等级下能否真正执行
```

因此：

```text
Permission
≠
Verification Level
```

两者必须同时满足。

---

## 23. 与 Private Notes 的关系

Private Notes 可以选择：

```text
RECOVERABLE_SECURE
```

作为默认产品体验。

这意味着：

- 所有持久化正文仍然是密文
- 普通管理员不能读取正文
- 用户忘记解锁密钥后，可以通过满足 Recovery Policy 的高保证认证执行恢复
- 历史 Key Snapshot 用于继续解密旧数据

隐私极端用户仍可以选择：

```text
ZERO_KNOWLEDGE
```

此时身份认证不能替代用户 Recovery Key。

---

## 24. 与 Password Manager 的关系

Password Manager 安全等级更高。

默认建议：

```text
ZERO_KNOWLEDGE
```

如未来提供：

```text
RECOVERABLE_SECURE
```

必须由用户显式开启，并明确告诉用户：

> 启用身份恢复意味着平台存在受保护的 Recovery Key Path，因此不再属于最严格意义上的服务器不可恢复 Zero-knowledge 模式。

不能在 UI 中同时声称：

```text
服务器绝无任何恢复能力
```

和：

```text
忘记所有密钥后只刷脸/收邮件即可恢复所有历史 Secret
```

两者在密码学上不可同时成立。

---

## 25. 与 AI 的关系

Security Subsystem 的以下信息默认禁止进入 AI Context：

- OTP
- Face Template
- Identity Document Raw Image
- Key Material
- Wrapped Key Snapshot
- Recovery Key
- JWT / Refresh Token
- Step-up Verification Grant
- Password / TOTP / Passkey Secret

AI 可以协助解释安全状态，例如：

```text
“你的 Private Notes 当前恢复方式只有 Email OTP，建议未来启用更高等级认证。”
```

但 AI 不可以：

- 读取 Key Snapshot
- 替用户完成 Verification
- 降低 minimum_svl
- 绕过 Cooldown
- 自动批准 Key Reset

---

## 26. 与 Automation 的关系

Security Event 可以触发 Automation：

```text
security.verification.failed
security.key.reset.requested
security.key.rotated
security.recovery.completed
security.device.revoked
identity.user.tokens-invalidated
```

但 Automation 不能自动降低安全策略。

例如禁止：

```text
WHEN Email OTP failed 3 times
THEN disable verification requirement
```

允许：

```text
WHEN Email OTP failed 5 times
THEN lock challenge
AND notify user
```

---

## 27. 与 Notification 的关系

Notification Provider 用于：

- 发送 Email OTP
- 发送安全告警
- Recovery 状态通知

但 OTP 本身属于 Authentication Secret。

Notification 子系统只能获得：

```text
一次性发送任务
```

不得将 OTP 作为普通通知历史长期保存。

---

## 28. Email OTP Provider Interface

当前阶段至少抽象：

```text
VerificationProvider
├── method()
├── issueChallenge(...)
├── verify(...)
├── cancel(...)
└── capabilities()
```

首个实现：

```text
EmailOtpVerificationProvider
```

未来新增：

```text
SmsOtpVerificationProvider
IdentityDocumentVerificationProvider
FaceVerificationProvider
```

不改变 Security Policy 与 Recovery Command 的主体设计。

---

## 29. Verification Result

认证 Provider 不直接修改权限或密钥。

只返回标准结果：

```text
VerificationResult
├── challenge_id
├── method
├── achieved_svl
├── subject_id
├── verified_at
├── expires_at
├── evidence_reference
└── risk_flags
```

Security Subsystem 再根据 Policy 决定是否允许目标 Command，并按需签发短期 Verification Grant。

---

## 30. MVP 实现优先级

### P0

- Security Subsystem 基础模型
- Stateless JWT Authentication / Validation
- User `security_version` Token Invalidation
- Email OTP Provider
- Verification Challenge
- Purpose Binding
- Rate Limit
- Short-lived Step-up Verification Grant
- Security Policy
- Permission + SVL 双重校验
- Key Ring
- Key Version
- Wrapped Key Snapshot
- RECOVERABLE_SECURE
- ZERO_KNOWLEDGE 边界
- Key Reset
- Recovery Operation
- Security Audit
- Private Notes 接入

### P1

- Trusted Device
- Recovery Key
- Lazy Re-encryption
- Background Re-encryption
- Recovery Cooldown
- SMS OTP

### P2

- Identity Document Verification
- Face Identity Verification
- Liveness / PAD
- Hardware Security Key
- 高级 Risk Engine
- 多因素组合策略

---

## 31. 当前阶段明确不做

本阶段不实现：

- 服务端 Login Session / Security Session Store
- 单 Session 查询 / 撤销
- 活跃会话 / 活跃设备登录列表
- SMS 实际发送与验证
- 身份证 OCR / 实名认证 Provider
- 人脸模型
- 人脸 Embedding
- 活体检测
- 生物识别中心化数据库
- Hardware Security Key
- 复杂 Risk Engine

但数据模型和 Provider Extension Point 必须允许未来增加认证方式；新增认证方式也不应默认引入服务端 Login Session。

---

## 32. 关键安全原则总结

### 原则一

```text
Authentication != Authorization
```

### 原则二

```text
Permission != Verification Level
```

### 原则三

```text
Face Verification
!=
Super Admin
```

### 原则四

```text
Key Reset
!=
Delete Old Key
```

而是：

```text
Old Key Version → DECRYPT_ONLY
New Key Version → ACTIVE
```

### 原则五

旧 Key Snapshot 必须是：

```text
Wrapped Ciphertext
```

不能是裸密钥明文。

### 原则六

```text
Identity Recovery
!=
Cryptographic Recovery
```

严格 Zero-knowledge 模式下，仅完成身份认证并不能凭空恢复丢失的加密密钥。

### 原则七

当前 V2 先实现：

```text
EMAIL_OTP / SVL-1
```

其他认证方式保留架构，不提前增加实现复杂度。

### 原则八

```text
JWT Authentication
!=
Server Session
```

JWT 登录状态不在服务端持久化为 Session；正常失效依赖 Token TTL，用户级紧急失效依赖 `security_version`，Step-up 使用独立短期 Grant。

---

## 33. 后续详细设计

后续应继续形成：

- Stateless JWT Token / Claim / Rotation Design
- Email OTP Protocol Design
- Step-up Verification Grant Design
- Key Ring / Key Snapshot Schema
- Private Notes Recovery Flow
- Password Vault Recovery Policy
- Trusted Device Design
- Identity Proofing Design
- Biometric Security Design
- Security Threat Model
- Crypto Algorithm / Key Derivation Design
