# Ikaros V2 Server App Package / Distribution / Trust Design

| 项目 | 内容 |
|---|---|
| 适用版本 | Ikaros V2 |
| 状态 | Draft / Implementation Gate |
| 上位决策 | ADR-005 Platform / Server App / Client App |
| 关联设计 | App Runtime / Identity / Client Architecture、Plugin Runtime / SDK / Lifecycle、Security / Identity / Authorization / Crypto |

> 本文档冻结 Server App 从“代码模块”演进为“可分发服务端应用包”所需的 Package、Manifest、完整性、Publisher Trust、兼容性、安装、升级、回滚与供应链边界。
>
> 本设计不改变当前 Modular Monolith 基线，也不要求首阶段支持 JVM Hot Loading。首个 Package Runtime 可以采用“安装包 -> 校验 -> 落盘 -> 记录 Registry -> 重启后装载”的模型。

---

## 1. 目标

Server App Package 必须解决：

- 一个 Server App 的稳定身份是什么；
- 包里允许包含什么；
- Platform 如何在执行任何 App 代码前验证包；
- Publisher 如何被识别和信任；
- Package Integrity 与 Publisher Signature 如何分离；
- Platform API / App API / Package Version 如何独立演进；
- App Permission、Scope、Dependency、Migration 如何进入 Manifest；
- Install / Upgrade / Rollback 如何避免把 Instance 留在半安装状态；
- First-party App 与 Third-party App 如何使用同一 Package Contract；
- 为什么“签名可信”不等于“代码被安全沙箱隔离”。

## 2. 非目标

本设计不负责 Client App APK / IPA / Desktop Installer 分发、OAuth / PKCE、AppAuthorizationGrant、Dynamic JVM Hot Unload、Marketplace 商业模型，也不把同 JVM 第三方代码描述成强安全沙箱。

## 3. Package Identity

Server App 使用稳定：

```text
app_id = reverse-DNS stable identity
package_version = app package release version
package_format_version = package container contract version
```

示例：

```text
app_id: run.ikaros.anime
package_version: 1.4.0
package_format_version: 1
```

规则：

- App 升级不得修改 `app_id`；
- Package Version 不等于 Platform API Version；
- Package Version 不等于 Server App Public API Major；
- Package Version 不等于 App-owned Database Migration Version；
- 同一个 `app_id + package_version` 的内容摘要必须不可变；
- 同版本出现不同 Digest 必须视为供应链异常，不能覆盖更新。

## 4. Package Container

推荐物理扩展名：

```text
*.ikaros-app
```

首版使用 ZIP-compatible container。

逻辑布局：

```text
run.ikaros.anime-1.4.0.ikaros-app
├── manifest.yaml
├── server/
│   └── app.jar
├── migrations/
├── api/
│   └── openapi/
├── assets/
│   └── admin/
├── static/
├── META-INF/
│   ├── checksums.sha256
│   ├── signature.json
│   └── publisher.json
└── LICENSE / NOTICE (optional)
```

Package 不得包含 APK / IPA、Instance Secret、明文 Publisher Private Key、指向宿主任意路径的符号链接或可逃逸 Package Root 的路径。解包必须阻止 Zip Slip、绝对路径、`../`、设备文件和不受控符号链接。

## 5. Manifest

`manifest.yaml` 是 Server App 身份与能力声明的唯一入口。

首版至少包含：

```yaml
package_format_version: 1

app_id: run.ikaros.anime
name: Ikaros Anime
version: 1.4.0

publisher:
  id: run.ikaros
  key_id: ikaros-release-2026

platform:
  api:
    min: 2.0
    max: 2.x

server:
  runtime: jvm
  entrypoint: run.ikaros.anime.AnimeApp

public_api:
  majors: [1]

permissions:
  - resource.read
  - attachment.read
  - task.submit
  - notification.send

scopes:
  - anime.library.read
  - anime.library.write
  - anime.playback
  - anime.progress.write

dependencies:
  - app_id: run.ikaros.document
    api: "^2"
    required: false

resource_types:
  - run.ikaros.anime/anime
  - run.ikaros.anime/season
  - run.ikaros.anime/episode

data:
  schema: app_anime
  migration_version: 12
```

Manifest declaration 不等于 Permission Grant，也不等于用户授权。

## 6. Digest 与 Canonicalization

Package Digest 针对签名范围内的文件字节计算，而不是 YAML 解析后的对象。

`META-INF/checksums.sha256` 对每个签名范围内文件记录：

```text
relative-path + SHA-256
```

规则：

- 路径统一 UTF-8 和正斜杠；
- 不允许不同输入规范化成相同路径；
- checksum entry 确定性排序；
- Signature 签 canonical checksum document 的 Digest，而不是 ZIP 原始字节。

这样 ZIP timestamp / compression level 不影响签名语义。

## 7. Integrity 与 Publisher Signature

### 7.1 Integrity

回答“当前文件是否与打包时一致”。

首版使用 SHA-256：

```text
container safety
→ checksum manifest parse
→ every signed entry digest
→ reject unexpected executable/migration entry
→ integrity PASS
```

### 7.2 Publisher Signature

回答“谁声明发布了这份包”。

推荐首版：

```text
Ed25519
```

签名对象至少绑定：

- app_id；
- package_version；
- package_format_version；
- checksum document digest；
- publisher_id；
- key_id。

Signature Valid 不等于 Publisher Trusted。

## 8. Publisher Identity 与 Key Rotation

Publisher 使用：

```text
publisher_id
publisher_key_id
public_key
trust_state
valid_from
revoked_at?
```

Key rotation：

1. 新 Key 在旧 Key 仍可信时登记；
2. 新版本开始用新 Key；
3. 旧 Key 进入 retiring；
4. 无继续发布需求后 revoke。

Key revoke 不自动删除已安装 App；Platform 将相关版本标为风险状态并要求管理员处理。

## 9. Trust State

至少区分：

```text
BUILT_IN
TRUSTED_PUBLISHER
UNVERIFIED_LOCAL
BLOCKED
```

`BUILT_IN` 仍有 app_id、Manifest、Permission Declaration 和 Lifecycle，不得绕过 Authorization。

`TRUSTED_PUBLISHER` 表示签名有效且 Publisher Key 被当前 Instance 信任。

`UNVERIFIED_LOCAL` 表示管理员手工导入但 Publisher Trust 未验证。若仍在同 JVM 执行，必须显式风险确认。

`BLOCKED` 表示 Digest、Signature、Publisher、Compatibility 或 Policy 禁止执行。

## 10. Permission Review

Manifest 的 `permissions` 只是 Requested Platform Permission。

安装时：

```text
Declared Permission
∩ Instance Policy
→ Administrator Review
→ AppPermissionGrant
```

升级新增 Permission 时不得静默继续 ENABLED：

```text
UPGRADE STAGED
→ NEW_PERMISSION_REVIEW_REQUIRED
→ administrator approves
→ migration / activate
```

Permission 缩减可以自动收紧。

## 11. Compatibility

安装前验证：

- Package Format Version；
- Platform API min/max；
- runtime requirement；
- required dependency；
- required dependency App API；
- Migration predecessor；
- App ID 与 Installation 一致；
- Upgrade path。

Compatibility 失败时进入 `INCOMPATIBLE`，不得执行 App entrypoint。

## 12. Dependency Resolution

Manifest Dependency：

```yaml
dependencies:
  - app_id: run.ikaros.document
    api: "^2"
    required: true
```

Enable 必须满足：

```text
dependency installed
AND dependency enabled
AND requested API compatible
```

Optional Dependency 不满足时 App 可 ENABLED，但对应 Capability 显式 unavailable。禁止 Repository fallback。禁止 dependency cycle 进入 ENABLED。

## 13. Migration Packaging

App-owned Migration 只允许修改 App-owned Schema。

Package migration 必须有：

- deterministic migration ID；
- checksum；
- ordering/predecessor；
- app_id ownership；
- package version provenance。

App Runtime 记录 `app_runtime.app_migration_history`，但 Migration 内容 Owner 仍是 Server App。

禁止修改 Platform Schema、其他 App Schema，禁止通过 Migration 获取任意宿主 DataSource 后执行跨 Owner SQL。

## 14. Install Pipeline

```text
RECEIVE
→ SIZE / CONTAINER SAFETY
→ MANIFEST PARSE
→ INTEGRITY VERIFY
→ SIGNATURE VERIFY
→ PUBLISHER TRUST
→ PLATFORM COMPATIBILITY
→ DEPENDENCY PLAN
→ PERMISSION REVIEW
→ MIGRATION PLAN
→ STAGE PACKAGE
→ REGISTER VERSION
→ RUN APP-OWNED MIGRATION
→ COMMIT INSTALLATION
→ INSTALLED
```

任何一步失败都不得进入 ENABLED，也不得留下“Registry 成功但 Package 未落盘”的假安装状态。

## 15. Enable

Enable 不是 Install 的隐式尾步骤。

Enable 前：

```text
installation state allows enable
AND package verified
AND publisher policy allows execution
AND platform compatible
AND dependencies satisfied
AND migrations applied
AND required config present
AND secrets resolvable
AND permissions granted
AND public API registration valid
```

然后：

```text
ENABLING
→ register routes/tasks/events
→ health check
→ ENABLED
```

当前 Foundation 直接进入稳定态；Package Runtime 实现时必须补中间态和失败恢复。

## 16. Upgrade / Rollback

Upgrade 使用 staged model：

```text
ENABLED v1
→ stage v2
→ verify
→ permission / dependency / migration plan
→ disable v1 admission
→ apply migration
→ switch active package
→ enable v2
→ health check
→ ENABLED v2
```

Migration 必须声明：

```text
ROLLBACK_SAFE
ROLL_FORWARD_ONLY
MANUAL_RECOVERY_REQUIRED
```

只有 Data Migration compatible、旧 Package retained、Publisher policy 仍允许时才能自动 rollback。

## 17. Package Storage

Package Artifact 与业务 Attachment/Blob 不同。

Package Store 必须：

- content-addressed；
- checksum verified；
- 不暴露成普通用户 Attachment；
- 区分 staged / active / retained-previous；
- 不允许 App 自己覆盖 active package bytes。

## 18. Supply-chain Audit

至少审计：

- package received；
- checksum verified / failed；
- signature verified / failed；
- publisher trust decision；
- permission grant decision；
- install / upgrade / rollback；
- publisher key revoke；
- package blocked。

不记录 Private Key、Package Secret、Client Token。

## 19. In-process Security Boundary

必须明确：

> Signature 解决来源与完整性，Platform Permission 解决合作式最小权限；二者都不能把任意 JVM 代码变成强安全沙箱。

同 JVM 第三方代码仍可能通过 Reflection、static state、thread、classpath、unsafe/native access 等突破逻辑 Capability Boundary。

因此 V2 Package Runtime 的信任模型首先是：

```text
trusted / administrator-approved server code
```

真正不可信 Marketplace App 需要 Out-of-process App Host、Process/Container isolation 或 WASM。

## 20. First-party Distribution

第一方 App 也必须有 Manifest。

早期：

```text
distribution = BUILT_IN
code = Maven module
manifest = build-time resource
```

后续转独立 Package：

```text
same app_id
same App Runtime contract
distribution changes
```

不得因为 First-party 编译进 Server 而创建第二套内部生命周期。

## 21. Client Metadata

Package 可以声明官方 Client metadata，但不包含 APK / IPA，不自动创建用户 Grant，也不禁止第三方 Client。

## 22. Package Format Version

`package_format_version` 只描述容器和 Manifest 结构。

- 新增 optional field：同版本可兼容；
- 改签名 canonicalization：升级 format；
- 改 required entry semantics：升级 format；
- Platform 不认识 required format：拒绝安装。

## 23. 首版实现顺序

### Slice A — Package Parser

container safety、manifest parser、checksum、Package Descriptor，不加载代码。

### Slice B — Trust

publisher key registry、Ed25519 verify、trust policy、audit。

### Slice C — Install Plan

compatibility、dependency、permission diff、migration plan、staging。

### Slice D — Restart-load

active package selection、restart after install/upgrade、activation、health/failure。

之后再决定是否需要 Hot Loading。

## 24. Package Resource Safety Limits

Package Parser 必须在解压和读取 Manifest 前后同时执行资源上限，防止 Zip Bomb、超大 entry、entry 数量耗尽和路径膨胀。

至少提供以下 Platform Policy：

```text
max_package_bytes
max_entry_count
max_single_entry_uncompressed_bytes
max_total_uncompressed_bytes
max_compression_ratio
max_manifest_bytes
max_path_length
```

规则：

- 下载/上传阶段先限制压缩包总字节；
- 解析 Central Directory 时先检查 entry count 与声明大小；
- 实际解压时按累计字节再次检查，不能只信任 ZIP metadata；
- 超过任何限制立即 fail closed；
- 不允许通过大量零字节 entry、嵌套 archive 或稀疏路径绕过限制；
- Package Runtime 不递归解压 App 自己携带的任意 archive；
- 可由 Instance Policy 调低限制，但实现必须提供安全默认值与不可无限放大的硬上限。

首个 Package Parser PR 必须把默认数值写入配置契约和测试，而不是把“无限制”作为默认。

---

## 25. Version / Dependency Constraint Syntax

为避免不同语言生态的 SemVer Range 语义不一致，V2 首版冻结：

### 25.1 Package Version

`package_version` 必须是 canonical SemVer 2.0.0：

```text
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
```

Registry 比较版本时忽略 build metadata 的排序影响，但 `app_id + package_version` 仍要求唯一 Digest。

### 25.2 Server App Public API Dependency

首版 `dependencies[].api` 只接受：

```text
^<positive-major>
```

例如：

```text
^2
```

表示“依赖目标 App Public API Major 2”，不表示 npm/cargo 的完整 caret-range 语义。

任意复杂表达式：

```text
>=2.1 <3
2.x
~2.3
latest
```

首版均拒绝，后续若需要扩展必须先升级 Manifest Contract。

### 25.3 Platform API Range

`platform.api.min/max` 使用 Platform 自己冻结的 API Version Contract，不复用 Package Version 比较器。

---

## 26. Configuration / Secret Contract

Manifest 可以声明配置结构，但 Package 不能携带 Instance Secret。

建议：

```yaml
configuration:
  schema_version: 1
  schema:
    type: object
    properties:
      metadata_endpoint:
        type: string
      api_token:
        type: string
        x-ikaros-secret: true
  required:
    - metadata_endpoint
```

规则：

- 普通配置进入 App Runtime Configuration；
- `x-ikaros-secret: true` 只保存 Secret Reference；
- Package 默认值不得包含真实 Credential；
- Manifest 不得声明宿主环境变量名来绕过 Secret API；
- Enable 前验证 required config/secret 是否可解析；
- Upgrade 必须验证新 Configuration Schema 与现有配置兼容；
- Secret 不进入 Package checksum 之外的任何运行时日志、Event 或 Audit payload。

配置 Schema 版本独立于 Package Version 与 App-owned DB Migration Version。

---

## 27. Crash Recovery / Atomic Activation

Install/Upgrade 不能依赖“单次进程不崩溃”的假设。

建议持久化：

```text
package_operation_id
operation_type = INSTALL | UPGRADE | ROLLBACK
app_id
from_version?
to_version
staged_digest
phase
started_at
updated_at
failure_code?
```

阶段至少区分：

```text
STAGING
VERIFIED
PERMISSION_REVIEWED
MIGRATING
ACTIVATING
HEALTH_CHECKING
SUCCEEDED
FAILED
RECOVERY_REQUIRED
```

规则：

1. Package bytes 先写 content-addressed staging。
2. Integrity/Signature/Compatibility 全部成功后才能进入 activation candidate。
3. Active package pointer 的切换必须是单一原子提交；不能“先覆盖 active 文件再写 DB”。
4. Active/previous/staged package 位于同一受控 Package Store；若使用 filesystem rename，必须确保同一 filesystem 并使用 atomic rename 能力。
5. Server crash 后启动时扫描未终态 operation，依据 phase 决定 resume / rollback / manual recovery。
6. Migration 已发生但 binary activation 未完成时，不得无条件切回旧 binary；按 `ROLLBACK_SAFE / ROLL_FORWARD_ONLY / MANUAL_RECOVERY_REQUIRED` 决定恢复路径。
7. Cleanup staged bytes 只能在 operation terminal 且无 recovery reference 后执行。
8. 同一 `app_id` 同时只允许一个 install/upgrade/rollback operation。

---

## 28. Reproducible Build / Provenance

首版不要求所有第三方 Package 都提供可复现构建，但 First-party Release Pipeline 应尽量产生稳定 artifact。

推荐 Package metadata 记录：

```text
source_revision
build_timestamp
builder_id
build_pipeline
source_repository
provenance_digest?
```

这些是供应链 provenance，不参与用户授权。

未来若引入签名 provenance / SBOM：

- 作为独立可验证 artifact；
- 不替代 Package Integrity；
- 不替代 Publisher Trust；
- 不因为存在 SBOM 自动获得更高 Trust State。

---

## 29. Acceptance Invariants

1. 同 app_id + version 不允许不同 Digest 覆盖。
2. Path traversal package 被拒绝。
3. Manifest 与签名 app_id 不一致时拒绝。
4. Signature Valid 但 Publisher 未信任时不得自动 ENABLE。
5. Requested Permission 未批准时不可使用。
6. Upgrade 新增 Permission 必须重新 Review。
7. Required Dependency 缺失时不能 ENABLE。
8. App Migration 不能修改其他 Owner Schema。
9. Integrity 失败时 EntryPoint 从不执行。
10. Uninstall 不删除 Client App binary，也不隐式删除 Platform Resource / Attachment。
11. Built-in App 与安装包 App 经过同一 App Runtime policy。
12. In-process App 明确不被描述为强沙箱。
13. Zip Bomb / 超 entry count / 超总解压大小在执行 App 代码前被拒绝。
14. Dependency API Constraint 使用冻结语法，未知 Range Syntax fail closed。
15. Package 中不得携带真实 Instance Secret；Secret 配置只落 Secret Reference。
16. Crash 发生在 STAGING / MIGRATING / ACTIVATING 时，重启后可确定性恢复或进入 RECOVERY_REQUIRED。
17. Active package pointer 不会出现 DB 指向 v2、文件实际仍是 v1 的撕裂状态。
18. 同一 app_id 同时最多一个 Package lifecycle operation。
19. Reproducible Build / Provenance 不被当作 Publisher Trust 的替代品。
