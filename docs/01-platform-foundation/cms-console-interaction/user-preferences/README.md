# Account 与 Console Preferences 交互规格

> 当前登录用户自己的资料、偏好、通知和账号安全从 Top App Bar 头像菜单进入，不作为 Sidebar 工作区。

## 1. Canonical Routes

```text
/account/profile
/account/preferences
/account/notifications
/account/security
```

当前 V2 不要求兼容历史 `/console/account/*` 路由。

## 2. Profile

`/account/profile` 展示当前用户可维护的显示名称、头像、地区/语言等资料。

登录标识、邮箱等安全相关字段修改按后端策略要求重新认证或 Step-up。

## 3. Preferences

`/account/preferences` 管理当前用户自己的 Console 展示偏好，例如：

- Theme；
- Language / Locale；
- Timezone / Date format；
- Table density；
- 可选的默认 Library 视图；
- 其他纯展示偏好。

业务筛选优先保存在 URL，不把可分享状态只藏在本地偏好中。

Access Token、Secret、解密私密内容不得存入 LocalStorage 作为偏好。

## 4. Notifications

`/account/notifications` 管理当前用户自己的通知偏好。系统级通知规则和 Channel 治理在 `/system/notifications`。

## 5. Account Security

`/account/security` 管理当前账号自己的会话、改密、恢复和安全状态。

平台级用户、角色、权限和认证策略在 `/system/access`。

## 6. Navigation

头像菜单至少包含：Profile、Preferences、Notifications、Security、Theme/Language 快捷项和 Logout。

基础个人入口不因用户缺少平台管理权限而消失。

## 7. 验收

- Account 页面全部从头像菜单进入；
- 不创建一级“个人中心”；
- 个人安全与平台 Access 管理边界明确；
- 不再使用历史 `/console/account/*` 设计路由。
