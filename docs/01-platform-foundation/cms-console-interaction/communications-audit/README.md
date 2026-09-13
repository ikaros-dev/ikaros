# System / Notifications 与 Audit — CMS Console 交互规格

> 系统级通知治理和审计统一归 System。当前用户自己的通知偏好从头像菜单进入。

## 1. System Notifications

Base Route：`/system/notifications`

用于管理：

- Notification Rules；
- Channel Configuration；
- Delivery Failures；
- Templates（后端支持时）；
- Announcements（产品启用时）。

规则明确 Audience、Scope、Channel、Priority、Quiet Hours 和更新时间。

需要异步重发或批量投递时，执行状态进入全局 `/activity`。

## 2. Current User Notifications

Route：`/account/notifications`

只管理当前用户自己的通知偏好。系统强制通知不因个人偏好被关闭。

## 3. Audit

Route：`/system/audit`

支持按 Actor、Action、Target Type、Result、Time Range 和 Risk Level 筛选。

Audit 详情展示经过权限和脱敏处理的必要信息。

Audit Deep Link 进入目标业务对象时重新执行目标权限；`audit.read` 本身不能扩大业务数据读取范围。

## 4. Announcements

如果系统支持公告，使用 `/system/notifications/announcements` 或 Notifications 内部 Tab，不创建独立全局入口。

## 5. Delivery Failures

投递失败可以产生 Overview Attention。用户看到 Channel、目标 Scope、错误摘要和下一步；内部 Attempt 信息进入 Advanced。

## 6. 验收

- 系统通知治理归 `/system/notifications/**`；
- 当前用户偏好归 `/account/notifications`；
- Audit 归 `/system/audit`；
- 通知和审计不扩大目标业务对象权限；
- 长期重发进入 `/activity`；
- 不再存在全局一级 Communications Center 或历史 `/communications-center/*` 设计路由。
