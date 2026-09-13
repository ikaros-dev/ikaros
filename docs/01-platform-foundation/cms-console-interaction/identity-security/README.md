# 系统 / 访问控制 — CMS Console 交互规格

> 身份、角色、权限和认证策略统一归入“系统 / 访问控制”，不再作为全局一级“身份与安全中心”。当前用户自己的安全设置仍从头像菜单进入 `/account/security`。

`/system/access` 是二级目录根，只负责组织和 redirect，不渲染独立 Access Home 页面。

## 1. 用户管理

**Route：** `/system/access/users`

列表字段：用户、状态、角色摘要、最近登录、会话状态、更新时间。

操作按能力显示：创建、禁用/启用、分配角色、查看会话、触发安全动作。

用户详情不得因为管理员可以管理账号就自动授予 Drive、Finance、Private Notes、Passwords 等业务内容读取权限。

## 2. 角色与权限

**Route：** `/system/access/roles-permissions`

这是一个 Sidebar 页面，统一承载 Role 与 Permission 管理。页面内部可以使用 `Roles` / `Permissions` Tab，但默认共享同一 canonical route。

能力包括：

- 角色名称、说明、成员数和 Permission 摘要；
- Permission Catalog / Matrix 浏览；
- 按角色配置能力；
- 高风险权限修改影响摘要；
- 按安全策略要求 Step-up Verification。

页面不得自行创造与后端不同的同义 Capability。

## 3. 身份认证

**Route：** `/system/access/authentication`

管理平台级认证策略、Step-up 规则、恢复策略和必要安全设置。

当前用户自己的会话、个人恢复材料、改密等入口从 `/account/security` 进入。

## 4. Sessions

管理员级会话治理可以作为用户详情或身份认证页面内子视图，不创建新的全局 Session Center。

终止他人会话等高影响动作必须明确目标用户、设备/会话和后果。

## 5. 审计

所有角色、权限、认证策略和高风险账户操作产生审计记录，并可从“系统 / 通知与审计 / 审计日志”（`/system/communications/audit`）查询。

Audit 本身不授予目标业务数据读取权限。

## 6. 权限变化体验

修改权限后：

- 目标用户刷新时 Sidebar / Apps / System 可见性及时收敛；
- 直接访问 canonical URL 仍由服务端授权；
- 缓存菜单不能继续暴露已撤销入口；
- 当前用户基础 Profile / Preferences 不因管理权限变化消失。

## 7. 验收

- `/system/access` 只作为目录根，不渲染独立页面；
- 用户管理只使用 `/system/access/users`；
- 角色与权限只使用 `/system/access/roles-permissions`；
- 身份认证只使用 `/system/access/authentication`；
- 当前用户安全归 `/account/security`；
- Platform Admin 不自动获得敏感业务内容读取权限；
- Capability 与后端 Permission Catalog 一一映射；
- 审计查询进入 `/system/communications/audit`；
- 不再使用旧 `/system/access/roles`、`/system/access/permissions`、`/system/audit` 或历史 `/identity-center/*` 作为 canonical 设计路由。
