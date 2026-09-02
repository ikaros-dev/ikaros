# Account：个人资料与客户端设置

## 1. 页面目录

- 我的首页。
- 个人资料。
- 外观与语言。
- 可访问性。
- 通知偏好入口。
- AI Persona / Privacy 入口。
- 安全与会话入口。
- Server Profiles。
- Download / Cache 设置。
- Local Device 设置。
- App Update。
- About / Diagnostics。

App 不提供用户 / 角色 / 平台参数等管理员设置。

---

## 2. 我的首页

### 2.1 Header

- Avatar 72–88dp。
- Display Name。
- Username。
- Short Introduction。
- 当前 Server Name + Online Status。
- `编辑资料`。

### 2.2 快捷状态卡

- Notifications 未读。
- Downloads。
- Pending Sync。
- Active Sessions。

只有有数据 / 异常时显示 Badge。

### 2.3 设置分组

#### Account

- 个人资料。
- 安全与会话。
- 通知。

#### Experience

- 外观。
- 语言。
- 可访问性。
- AI Persona / Privacy。

#### Device

- 下载与缓存。
- 离线与同步。
- 本机安全解锁。
- Server Profiles。

#### App

- 检查更新。
- Diagnostics。
- About。
- Logout。

---

## 3. 个人资料

### 3.1 字段

- Avatar。
- Display Name。
- Username（如果允许修改，单独说明规则）。
- Introduction / Bio。
- Email。
- Locale。
- Time Zone。

Time Zone 会影响 Calendar / Analytics / Reminder，需要显示当前选择的示例时间。

### 3.2 Avatar

Tap：拍照 / 相册 / 文件；裁剪 1:1；Upload Progress；失败可重试。

保存前 Preview。

---

## 4. 外观

Settings：

- Theme：System / Light / Dark。
- Optional OLED Black（阅读器可以独立覆盖）。
- Dynamic Color（平台支持时）。
- App Accent / Seed Color（产品允许时）。
- Compact Density（Desktop 可选）。
- Reduce Motion：跟随系统 / On / Off。

媒体播放器 / 阅读器拥有自己的沉浸主题，但返回 App 后恢复全局主题。

---

## 5. 语言与地区

- App Language。
- Content Preferred Language Order。
- Date Format。
- Time Format 12/24h。
- First Day of Week。
- Time Zone。

Content Preferred Language 只影响显示标题优先级，不改 Resource 原始 Metadata。

---

## 6. 可访问性

- Follow System Text Scale。
- Additional Text Size（合理范围）。
- High Contrast（产品支持）。
- Reduce Motion。
- Captions Preference。
- Player Control Size。
- Reader Touch Zone Guidance。

提供 `预览` 卡，实时显示文字、按钮、Chip。

---

## 7. AI 设置入口

Card 显示：

- Current Persona。
- Long-term Memory：On / Off。
- Privacy Profile：Cloud Allowed / Local Only 等用户可理解摘要。

Tap 进入 AI 专属 Persona / Memory / Privacy 页面，不在 Account 复制整套表单。

---

## 8. 安全与会话入口

Card：

- Active Session Count。
- Current Verification Level / Recent Step-up 摘要。
- Secure Domains：Locked / Unlocked（只显示状态，不显示内容）。

Tap 进入 Access 文档定义的安全页。

---

## 9. 下载与缓存设置

摘要：

- Download Size。
- Cache Size。
- Free Space。

入口：

- My Downloads。
- Cache Management。
- Network Policy。
- Auto Download。
- Storage Location（Desktop / Android 能力允许时）。

不要把 Cache Directory 直接当“下载目录”。

---

## 10. Offline / Sync 设置

- Sync on Cellular。
- Background Sync。
- Pending Change Count。
- Last Successful Sync。
- `查看待同步`。

Secure Domain Sync 设置放各 Vault 内，Account 只展示总状态。

---

## 11. Local Device Security

标记 `仅此设备`。

- App Lock。
- Biometric Unlock for Password Manager。
- Biometric Unlock for Private Notes。
- Secure Auto-lock Timeout。
- Hide Sensitive Recent Apps Preview。
- Prevent Screenshot（支持时）。

启用 Biometric 前执行系统能力检测和一次成功验证。

---

## 12. Server Profiles

显示当前：Server Name、Host、Version、API Compatibility、Online State。

操作：Switch Server、Manage Servers、Test Connection。

切换 Server 前若存在 Pending Sync：Warning Sheet 显示数量，允许 Cancel / 继续切换；不能静默丢队列。

---

## 13. App Update

### 13.1 Update Card

- Current Version。
- Build Number。
- Channel：Stable / Beta（产品支持时）。
- `检查更新`。

### 13.2 Update Available

Dialog / Page：

- New Version。
- Release Notes 摘要。
- Download Size。
- Platform Asset。
- Compatibility Note。
- `下载更新`。

下载进度复用 Download / Background Progress 视觉；完成后平台支持时 `安装` / `重启更新`。

不支持内更新的平台提供 `打开发布页面`。

---

## 14. Diagnostics

用户可进入：

- App Version。
- Flutter / Platform Info（必要摘要）。
- Server Host / Version。
- API Compatibility。
- Network State。
- Last Sync。
- Local DB / Cache Summary。
- Notification Permission。
- Media Capability Summary。

操作：`复制诊断信息`。

复制内容必须自动脱敏：Token、Password、Vault Data、完整 Secret、敏感 URL Query 不包含。

---

## 15. About

- Ikaros Logo。
- Version。
- License。
- Source Repository Link。
- Documentation Link。
- Open Source Notices。

---

## 16. Logout

点击 Logout：

Dialog 显示：

- 将退出当前 Server Account。
- Pending Sync Count。
- 是否保留本机 Downloads。
- Secure Encrypted Cache 的处理选项（按安全策略）。

选项：

- `退出并保留离线下载`（安全允许时）。
- `退出并清除此账号本机数据`。

Password / Private Vault 必须先 Lock，清理解密 Key Material。

---

## 17. 响应式

- Compact：Settings 使用 Grouped List。
- Medium：左 Section List、右 Detail。
- Expanded：永久 Settings Navigation 260dp + 内容最大 760–900dp。
- Large：设置表单仍限制宽度，右侧可显示 Preview / Help，不把开关分散到超宽行两端。
