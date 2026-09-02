# Password Manager：密码保险库与 Secret 管理

## 1. 安全前提

Password Manager 默认且必须使用 `USER_LOCKED_E2EE`。

核心 UI 约束：

- Ikaros Login ≠ Password Vault Unlock。
- 服务端管理员不天然拥有解密权限。
- Secret 默认隐藏。
- “使用 Secret”优先于“展示 Secret”。
- 本地离线缓存必须加密。
- Copy / Reveal / Export 必须具备独立安全反馈。
- Vault 锁定后立即移除所有解密内容。

---

## 2. 页面目录

- Vault 列表。
- Vault Unlock。
- Vault Home。
- Search / Folder / Collection。
- Vault Item 详情。
- Login Item。
- Secure Note Item。
- Card Item。
- Identity Item。
- TOTP。
- Passkey。
- SSH Key。
- API Credential。
- Custom Secret。
- Password / Passphrase Generator。
- Vault Health。
- Shared Vault。
- Conflict / Password History。
- Emergency Access。
- Export / Recovery。
- Device / Biometric Settings。

---

## 3. Vault 列表与解锁

### 3.1 Vault Card

Locked 状态只显示安全允许的信息：

- Vault Safe Name / Alias。
- Personal / Shared / High-security 类型。
- Lock Icon。
- Local Encrypted Cache 状态。
- Last Sync。
- Conflict Count。
- `解锁`。

不显示 Item Title、URI、用户名等业务内容。

### 3.2 Unlock 页面

- Vault Name。
- Lock Icon。
- Unlock Method。
- 主解锁输入 / 动作。
- `使用生物识别`（已配置时）。
- `无法解锁？`。

生物识别只保护本地解锁密钥，不上传生物数据。

---

## 4. Vault Home

### 4.1 App Bar

- Vault Name。
- Lock Now。
- Search。
- Add Item。
- More。

### 4.2 顶部 Quick Filter

Chips：

- 全部。
- 收藏。
- Login。
- Card。
- Identity。
- TOTP。
- Passkey。
- SSH / API。

### 4.3 Item Row

- Item Type Icon。
- Name。
- Username / Safe Secondary Label（根据类型）。
- Favorite。
- Folder / Collection。
- TOTP Indicator（存在时）。
- Attachment Indicator。
- Conflict / Pending Sync。
- More。

密码、完整卡号、TOTP Seed、Private Key 不出现在列表摘要。

---

## 5. Search

Vault 解锁后本地搜索：

- Name。
- Username。
- URI / Origin。
- Folder。
- 非隐藏 Custom Field（策略允许）。

默认不搜索 Hidden Secret Value。

Search Query 不进入普通 App 全局搜索历史。

锁定后 Search UI 和 Result 立即销毁。

---

## 6. Add Item

Bottom Sheet：

- Login。
- Secure Note。
- Card。
- Identity。
- TOTP。
- Passkey（平台能力允许时）。
- SSH Key。
- API Credential。
- Custom Secret。

选择后进入对应 Editor。

---

## 7. Login Item 详情

### 7.1 Header

- Site / Service Icon。
- Item Name。
- Favorite。
- Edit。
- More。

### 7.2 字段顺序

1. Username。
   - 右侧 Copy。
2. Password。
   - 默认 `••••••••`。
   - Reveal。
   - Copy。
   - Password History。
3. URI / Origin 列表。
   - URI。
   - Match Policy。
   - Open Link。
4. TOTP Reference。
   - 当前 Code（若已解锁）。
   - 倒计时环。
   - Copy。
5. Notes。
6. Custom Fields。
7. Attachments。
8. Updated At / Revision。

### 7.3 Reveal Password

Tap Reveal：

- 已满足 Vault Unlock 且策略允许：短暂显示。
- 需要 Fresh Verification：先 Step-up。
- 显示后提供 `隐藏`。
- 页面离开 / App 后台自动隐藏。

不要在 Reveal 后把明文写入普通状态日志。

### 7.4 Copy Password

复制后 Snackbar：`已复制，30 秒后尝试清除剪贴板`。

倒计时时长由本机安全设置决定。

平台无法可靠清理剪贴板时，文案不能承诺“必定删除”。

---

## 8. Login Item 编辑

字段：

- Name *。
- Username。
- Password。
- URI，可多条。
- 每条 URI Match Policy：Exact Origin / Host / Base Domain / Starts With / Advanced Regex / Never Autofill。
- TOTP Reference。
- Folder。
- Collection / Shared Vault Scope。
- Notes。
- Custom Fields。
- Attachments。

Password Field 右侧：`生成`。

保存前 URI 与 Match Policy 显示 phishing 风险提示；可疑宽松规则需要明确确认。

---

## 9. Phishing Protection UI

当 Autofill / 使用密码目标不匹配：

Warning Sheet：

```text
已保存：example.com
当前：examp1e.com
```

显示：

- Origin 差异。
- Match Policy。
- `取消` 主推荐。
- `仍然使用` 需要二次确认 / Step-up（策略决定）。

默认不静默填充。

---

## 10. Card Item

### 10.1 默认展示

- Card Name。
- Cardholder。
- Card Number：`•••• •••• •••• 1234`。
- Expiration：可显示。
- Security Code：`•••`。
- Billing Metadata。
- Notes。

### 10.2 Reveal

完整 Card Number / CVC 分别拥有 Reveal，不因显示卡号自动显示 CVC。

PIN 作为独立高敏感 Secret Field，放 `更多安全字段` 中。

Copy 后应用剪贴板安全策略。

---

## 11. Identity Item

字段：

- Name。
- Address。
- Email。
- Phone。
- Identification Metadata。
- Custom Fields。

身份证件号码等高敏感字段默认 Masked / Hidden。

用于 Autofill 时显示目标应用 / Origin 与将填写的字段预览。

---

## 12. TOTP

### 12.1 TOTP Item

- Service Name。
- Account。
- 6 / 8 Digit Code。
- Circular Period Progress。
- Copy。
- Linked Login。

Seed 默认永不显示。

### 12.2 添加

- Scan QR。
- Import `otpauth://`。
- Manual。

Manual Advanced：Secret、Period、Digits、Algorithm。

保存前提供测试 Code Preview。

---

## 13. Passkey

Item 详情只显示安全元信息：

- RP / Site。
- Account / User Handle Display。
- Created At。
- Last Used At。
- Device / Sync Metadata（可用时）。

Private Key Material 无普通 Reveal 按钮。

使用 Passkey 通过 OS Credential API / WebAuthn 集成，不导出原始私钥作为常规交互。

---

## 14. SSH Key

字段：

- Name。
- Algorithm。
- Fingerprint。
- Public Key。
- Private Key Hidden。
- Comment。
- Optional Passphrase Hidden。

主动作优先：`使用密钥` / `复制公钥`。

`导出私钥` 属于高风险动作，Step-up + 明确目标位置 + Audit。

---

## 15. API Credential

字段：

- Name。
- Endpoint。
- Client ID。
- API Key / Token Hidden。
- Client Secret Hidden。
- Expiration。
- Scope。
- Related Integration。

若被其他 Ikaros 子系统通过 `secret://` 引用，详情显示：`被 3 个集成使用`，点击查看引用对象（权限允许时）。

---

## 16. Custom Secret

用户可添加字段：

- Text。
- Hidden。
- Boolean。
- Linked Secret。

Hidden Field 默认：

- UI Mask。
- 不进入普通搜索。
- 不进入 AI。
- Copy 后清理策略。

---

## 17. Password Generator

### 17.1 Password Tab

字段：

- Length Slider + 数字输入。
- Uppercase。
- Lowercase。
- Numbers。
- Symbols。
- Avoid Ambiguous。
- Minimum Numbers。
- Minimum Symbols。

顶部大号 Generated Password + Regenerate + Copy。

### 17.2 Passphrase Tab

- Word Count。
- Separator。
- Capitalization。
- Number Suffix。

### 17.3 Username Tab

支持随机 Username 规则（能力落地时）。

生成器可独立使用，不要求保存到 Vault。

---

## 18. Password History

Item Detail → History。

列表只显示：

- Changed At。
- Revision。
- Device / Actor。
- Hidden Password。

点击 Reveal Historical Password 仍需安全策略。

Retention：Off / Last N / N Days。

---

## 19. Conflict Resolver

两个设备同时编辑 Item 时：

- 显示 Mine / Remote Version Metadata。
- 不直接并排显示所有 Secret 明文。
- 字段级比较时 Hidden Field 默认遮罩，逐字段 Reveal。

操作：Keep Mine / Keep Remote / Keep Both / Manual Merge。

Password History 保留冲突前值。

---

## 20. Vault Health

### 20.1 Dashboard

Cards：

- Weak Password。
- Reused Password。
- Old Password。
- Missing 2FA Metadata。
- Insecure HTTP URI。
- Duplicate Item。
- Optional Breach Check。

每卡：Count + `查看`。

### 20.2 Detail

只在本地解锁后显示 Item Name；不把完整 Password 上传用于分析。

Breach Check 若使用外部 Provider，设置页解释隐私保护查询方式与开关。

---

## 21. Shared Vault / Collection

列表显示：Name、Members、Role、Last Sync。

Member Detail 权限：Owner / Manager / Editor / Viewer，以及细粒度：Reveal Secret / Autofill / Edit / Share / Export。

共享变更必须清楚说明 Key / Access 影响，移除成员不是简单隐藏菜单。

---

## 22. Emergency Access

页面显示：

- Trusted Contact。
- Access Scope。
- Waiting Period。
- Status。
- Last Request。

收到请求时 Notification 进入 Emergency Request Detail：Approve / Reject。

Waiting Period 未结束时显示倒计时；Owner 可 Reject。

管理员没有默认 Emergency Access 后门。

---

## 23. Export

首选 Encrypted Vault Export。

明文 / CSV / JSON Export 需要：

- Scope Preview。
- Item Count。
- 是否包含 Attachments。
- Fresh Verification。
- 风险说明。
- 目标位置。

按钮文案：`导出未加密副本`。

---

## 24. Recovery

页面明确：

- Account Recovery ≠ Vault Recovery。
- 当前 Security Profile。
- Recovery Policy。
- Recovery Key / Trusted Device 等可用方法。

Zero-knowledge 无恢复材料时明确数据不可恢复风险。

---

## 25. Offline-first

断网时支持：

- Search。
- Autofill。
- TOTP。
- Generator。
- 查看 / 新建 / 修改 Item。

全部基于 Encrypted Vault Cache。

联网后同步 Ciphertext Delta；Conflict 不使用无提示 Last Write Wins。

---

## 26. App Switcher / Screenshot

默认对 Password Manager 启用安全预览：

- 切后台时隐藏 Item Detail。
- 最近任务卡只显示 Lock Surface。
- Screenshot 阻止作为本机选项，平台支持时开启。

---

## 27. 响应式

- Compact：Vault List → Item List → Item Detail 分页。
- Medium：Item List + Detail 双栏。
- Expanded：Folder / Item List / Detail 三栏。
- Large：Secret Detail 最大宽度约 760dp，避免敏感值跨超宽屏展示；右侧可放 References / History。
