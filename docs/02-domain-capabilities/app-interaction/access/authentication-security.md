# Access：登录、服务端连接与安全认证

## 1. 页面目录

- Server Profile 列表。
- 添加 / 编辑服务器。
- 登录。
- Email OTP / Step-up Verification。
- 登录失败 / 兼容性错误。
- 登录状态与令牌安全。
- 安全设置。
- Secure Domain 恢复入口。

> Ikaros 登录认证基于 JWT Token，为无状态认证模型。服务端不建立、持久化或管理“登录会话 / 安全会话”，因此客户端也不提供活跃会话列表、单会话撤销、Session Detail 等交互。本文中的“登录状态”仅表示客户端本地是否持有当前可用 Token。

---

## 2. Server Profile 列表页

### 2.1 入口

- 首次启动。
- 登录页左上角“切换服务器”。
- 我的 → 客户端设置 → 服务器。

### 2.2 页面结构

App Bar：`服务器` + `添加`。

列表卡片字段：

- Server Display Name。
- Base URL，展示 Host，完整 URL 放详情。
- 当前连接状态：Online / Offline / Unknown。
- 最近连接时间。
- 当前账号名称（如本机已保存登录凭据 / Token）。
- 服务端版本。
- `默认` Chip。

卡片尾部更多菜单：编辑、设为默认、重新检测、移除本机配置。

### 2.3 交互

Tap 卡片：切换并尝试连接。

移除只删除本机 Server Profile，不删除服务端账号；Dialog 必须明确这一点。

---

## 3. 添加服务器页

### 3.1 表单字段

1. `服务器地址 *`
   - 支持 HTTPS / HTTP。
   - 输入示例放 Helper Text，不预填真实域名。
2. `名称`
   - 可选；为空时连接成功后使用服务端返回名称或 Host。
3. `高级选项` 折叠区：
   - 自定义代理（如果客户端支持）。
   - 是否允许本地开发环境 HTTP。
   - 自签名证书策略只在开发 / 明确高级设置中出现。

底部：`测试连接`、`保存并登录`。

### 3.2 测试结果卡

成功：

- Server Name。
- Version。
- API Version。
- Capability Discovery 摘要。
- TLS 状态。

失败：

- DNS / Timeout / TLS / 版本不兼容分类。
- `复制诊断信息`。

---

## 4. 登录页

### 4.1 Layout

Compact：单列，左右 24dp，表单最大宽度 480dp。

Desktop：页面中央 420–480dp Login Card；左上角返回服务器列表。

### 4.2 字段顺序

1. 当前服务器小卡：Name + Host + `切换`。
2. `用户名 / 邮箱 *`。
3. `密码 *`，尾部 Reveal 图标。
4. 可选 `保持登录`。
5. `登录` Filled Button。
6. `忘记密码 / 账号恢复` Text Button（服务端支持时）。

不在第一屏直接显示 Secure Vault 解锁字段，因为 Ikaros Login 与 Vault Unlock 必须分离。

`保持登录` 只决定客户端如何安全保存 / 刷新 Token，不代表服务端创建长期 Session。

若实现启用 Refresh Token，则 Refresh Token 也必须遵守无状态认证约束：它不能映射到服务端 Session 或 Token Digest 登录态，并且必须绑定并校验当前用户的 `security_version`。`security_version` 提升后，旧 Refresh Token 不得再换取新的 Access Token。

### 4.3 交互

- Enter 提交。
- 提交时按钮 Loading。
- 401：只显示“凭据无效”，不把服务端内部异常完整暴露。
- 需要额外验证：转 Step-up 页面。
- 成功：安全保存 JWT Token，并返回原 Deep Link 或首页。

---

## 5. Email OTP / Step-up Verification 页

### 5.1 页面用途

用于：

- Login Step-up。
- 修改安全设置。
- Secure Recovery。
- 高风险导出。
- 其他要求 SVL 的动作。

### 5.2 页面结构

1. Shield 图标。
2. 标题，例如“验证你的身份”。
3. Purpose 文本，例如“为了导出密码保险库，需要重新验证”。
4. 脱敏邮箱：`a***@example.com`。
5. 6 位 OTP Input。
6. `验证` Filled Button。
7. 倒计时后 `重新发送`。
8. `取消`。

### 5.3 规则

- 明确显示验证用途，不允许用户误以为验证码可用于任何操作。
- 错误次数接近限制时显示剩余尝试次数（若安全策略允许）。
- OTP 不进入剪贴板历史提示、Analytics 或普通日志。
- 验证成功后，服务端签发短时、Purpose-bound 的 Step-up Grant；Grant 可以是独立签名 JWT，不建立服务端 Session。
- Step-up Grant 必须绑定并校验 `purpose`、`target_reference`（适用时）、`exp` 和达到的 SVL；错误用途、错误目标、过期或等级不足均不得复用。
- Step-up Grant 到期后自然失效；高风险动作要求 fresh verification 时必须重新验证。
- 验证成功后自动回到原动作并继续。

---

## 6. 登录状态与令牌安全页

### 6.1 入口

我的 → 安全与认证。

### 6.2 页面字段

顶部显示：

- 当前账号。
- 当前 Server。
- JWT 有效期 / 下次需要重新认证的时间。
- 最近一次 Step-up Verification 摘要（存在且允许展示时）。
- `退出当前设备`。
- `使所有设备重新登录` 高风险操作。

不得展示：

- Active Session Count。
- 服务端“活跃设备”列表。
- Session ID。
- Last Seen Session。
- 单设备 / 单 Session 撤销按钮。

原因是 JWT 登录为无状态认证，服务端不存在可枚举的登录 Session 实体。

### 6.3 退出当前设备

`退出当前设备` 的语义是：

1. 清除本机 Access / Refresh Token 与相关认证缓存。
2. Lock Password Manager / Private Notes 等本机 Secure Domain。
3. 清理解密 Key Material。
4. 返回登录页。

普通退出不向服务端发送“撤销当前 Session”命令，因为服务端没有当前 Session。

### 6.4 使所有设备重新登录

当用户怀疑 Token 泄露、修改关键安全设置或希望强制所有客户端重新认证时，可执行：

```text
identity.invalidate-user-tokens
```

服务端提升用户：

```text
security_version
```

后，之前签发且携带旧版本的 Access / Refresh JWT 均不再被接受；旧 Refresh JWT 也不能用于签发新的 Access JWT。

该操作：

- 必须 Step-up Verification。
- 会使当前设备自己的旧 Token 同时失效。
- 完成后当前客户端清理本机 Token 并返回登录页。
- UI 文案使用“使所有设备重新登录 / 使所有旧 Token 失效”，不使用“撤销所有会话”。

---

## 7. 安全设置页

分区：

### 7.1 账号安全

- 邮箱状态。
- 修改密码。
- 双重验证（当服务端支持）。
- Recovery 设置。
- 使所有旧 Token 失效。

密码修改、账号禁用、关键凭据变更等操作是否自动提升 `security_version`，由 Security Policy 决定。

### 7.2 Step-up Verification

只展示当前可用方式，例如 V2 初期 Email OTP。

每项：

- 方法。
- 当前状态。
- 最近验证。
- 安全等级说明。

这里展示的是验证能力与最近验证事实，不是服务端 Session 状态。

### 7.3 本机解锁

- 使用系统生物识别解锁 Password Manager。
- 使用系统生物识别解锁 Private Notes。
- App 回后台后自动锁定：立即 / 1 分钟 / 5 分钟 / 从不。
- 阻止敏感页面截图（平台支持时）。

这些开关只影响本机，必须标记 `此设备`。

本机 Secure Domain 的 Locked / Unlocked 状态属于客户端安全状态，不等同于服务端登录 Session。

---

## 8. Secure Domain 恢复页

### 8.1 入口

仅从 Private Notes / Password Manager 的“无法解锁？”进入，不放在普通首页显眼位置。

### 8.2 第一屏必须解释

- 账号恢复不等于加密数据恢复。
- 当前 Vault 的 Security Profile。
- 当前是否存在 Recovery Path。
- Zero-knowledge 且无 Recovery Key 时，可能无法恢复历史数据。

### 8.3 Recoverable Secure

展示：

- 当前 Recovery Policy。
- 要求的 Verification Level。
- `开始身份验证`。

验证成功后再进入 Key Reset / Recovery 流程。

### 8.4 Zero Knowledge

展示可用方式：

- Recovery Key。
- Trusted Device。
- Hardware Key（规划能力）。

绝不显示“联系管理员重置密码即可恢复全部 Vault”之类误导文案。

---

## 9. 安全确认通用 Dialog

高风险动作 Dialog 内容顺序：

1. 动作名称。
2. 影响对象。
3. 是否可恢复。
4. 对本地 / 服务端的影响。
5. 必要的验证等级。
6. Cancel。
7. Continue。

极高风险动作完成 Step-up 后，确认 Dialog 仍保留；身份验证不是操作确认的替代品。

---

## 10. 响应式

- Compact：全屏独立页面。
- Medium：表单居中，最大 560dp。
- Expanded：安全设置可使用左侧 Section Navigation + 右侧设置内容。
- 登录状态与令牌安全页在大屏仍使用单一安全摘要页，不构造 Session 列表 / 详情双栏。
