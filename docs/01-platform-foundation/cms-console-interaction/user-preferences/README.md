# Account 与 Console Preferences 交互规格

> 当前登录用户自己的资料、偏好、通知、账号安全、会话和 API 令牌从 Top App Bar 头像菜单进入，不作为 Sidebar 工作区。

## 1. Canonical Routes

```text
/account/profile
/account/preferences
/account/notifications
/account/security
/account/sessions
/account/api-tokens
```

当前 V2 不要求兼容历史 `/console/account/*` 路由。

## 2. Profile

`/account/profile` 展示当前用户可维护的显示名称、头像、地区/语言等资料。

登录标识、邮箱等安全相关字段修改按后端策略要求重新认证或 Step-up。

## 3. Preferences

`/account/preferences` 管理当前用户自己的 Console 展示偏好，例如：Theme、Language / Locale、Timezone / Date format、Table density、可选的默认资源库视图和其他纯展示偏好。

业务筛选优先保存在 URL，不把可分享状态只藏在本地偏好中。

Access Token、Secret、解密私密内容不得存入 LocalStorage 作为偏好。

## 4. Notifications

`/account/notifications` 管理当前用户自己的通知偏好。系统级通知规则和 Channel 治理在“系统 / 通知与审计 / 通知中心”（`/system/communications/notifications`）。

## 5. Account Security

`/account/security` 管理当前账号自己的改密、恢复和安全状态。

当前账号的会话管理使用 `/account/sessions`；API 令牌管理使用 `/account/api-tokens`。

平台级治理分别进入：

- 用户管理：`/system/access/users`；
- 角色与权限：`/system/access/roles-permissions`；
- 身份认证：`/system/access/authentication`。

`/system/access` 只是 System 内目录根，不渲染 Access 页面。

## 6. Navigation

头像菜单至少包含：Profile、Preferences、Notifications、Security、Sessions、API Tokens、Theme/Language 快捷项和 Logout。

基础个人入口不因用户缺少平台管理权限而消失。

## 7. 验收

- Account 页面全部从头像菜单进入；
- 不创建一级“个人中心”；
- 当前账号安全与平台 Access 管理边界明确；
- 系统通知治理使用 `/system/communications/notifications`；
- 平台级 Access 页面使用 `/system/access/<page>` 的三级结构；
- 不再使用旧 `/system/notifications` 或把 `/system/access` 当业务页面；
- 不再使用历史 `/console/account/*` 设计路由。
