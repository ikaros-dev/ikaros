# 系统 / 通知与审计 — CMS Console 交互规格

> 系统级通知治理和审计统一归“系统 / 通知与审计”。当前用户自己的通知偏好从头像菜单进入。

## 1. 通知中心

**Route：** `/system/communications/notifications`

用于管理：

- Notification Rules；
- Channel Configuration；
- Delivery Failures；
- Templates（后端支持时）；
- Announcements（产品启用时）。

规则明确 Audience、Scope、Channel、Priority、Quiet Hours 和更新时间。

需要异步重发或批量投递时，执行状态进入“资源 / 活动中心”（`/resources/activity`）。

`/system/communications` 只是二级目录根，可 redirect 到第一个可访问子页面，不渲染独立页面。

## 2. 当前用户通知

**Route：** `/account/notifications`

只管理当前用户自己的通知偏好。系统强制通知不因个人偏好被关闭。

## 3. 审计日志

**Route：** `/system/communications/audit`

支持按 Actor、Action、Target Type、Result、Time Range 和 Risk Level 筛选。

Audit 详情展示经过权限和脱敏处理的必要信息。

Audit Deep Link 进入目标业务对象时重新执行目标权限；`audit.read` 本身不能扩大业务数据读取范围。

## 4. Announcements

如果系统支持公告，优先作为通知中心页面内 Tab；只有确实需要独立 deep link、权限或生命周期时才建立 `/system/communications/notifications/announcements` 等子路由，不创建新的 Sidebar 页面。

## 5. Delivery Failures

投递失败可以产生 Dashboard Attention。用户看到 Channel、目标 Scope、错误摘要和下一步；内部 Attempt 信息进入 Advanced。

## 6. 验收

- 通知中心只使用 `/system/communications/notifications`；
- 审计日志只使用 `/system/communications/audit`；
- `/system/communications` 只作为目录根，不渲染业务页面；
- 当前用户偏好归 `/account/notifications`；
- 通知和审计不扩大目标业务对象权限；
- 长期重发进入 `/resources/activity`；
- 不再使用旧 `/system/notifications`、`/system/audit` 或历史 `/communications-center/*` 作为 canonical 设计路由。
