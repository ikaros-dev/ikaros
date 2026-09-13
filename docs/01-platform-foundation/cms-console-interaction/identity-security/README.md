# System / Access — CMS Console 交互规格

> 身份、角色、权限和认证策略统一归入 System / Access，不再作为全局一级“身份与安全中心”。当前用户自己的安全设置仍从头像菜单进入 `/account/security`。

## 1. Access Home

**Route：** `/system/access`

展示用户、角色、权限、认证策略和近期安全 Attention 摘要。

只有拥有至少一个 Access 管理能力的用户才能看到 System / Access 入口。

## 2. Users

**Route：** `/system/access/users`

列表字段：用户、状态、角色摘要、最近登录、会话状态、更新时间。

操作按能力显示：创建、禁用/启用、分配角色、查看会话、触发安全动作。

用户详情不得因为管理员可以管理账号就自动授予 Drive、Finance、Private Notes、Passwords 等业务内容读取权限。

## 3. Roles

**Route：** `/system/access/roles`

展示角色名称、说明、成员数、Permission 摘要和更新时间。

角色编辑使用明确的 Permission Catalog，不允许页面自行创造与后端不同的同义 Capability。

## 4. Permissions

**Route：** `/system/access/permissions`

提供 Permission Catalog / Matrix 浏览和按角色配置能力。

高风险权限修改显示影响摘要并按照策略要求重新验证。

## 5. Authentication

**Route：** `/system/access/authentication`

管理平台级认证策略、Step-up 规则、恢复策略和必要安全设置。

当前用户自己的会话、个人恢复材料、改密等入口从 `/account/security` 进入。

## 6. Sessions

管理员级会话治理可以作为 Users Detail 或 Authentication 子页，不创建新的全局 Session Center。

终止他人会话等高影响动作必须明确目标用户、设备/会话和后果。

## 7. 审计

所有角色、权限、认证策略和高风险账户操作产生审计记录，并可从 `/system/audit` 查询。

Audit 本身不授予目标业务数据读取权限。

## 8. 权限变化体验

修改权限后：

- 目标用户刷新时 Sidebar / Apps / System 可见性及时收敛；
- 直接访问 canonical URL 仍由服务端授权；
- 缓存菜单不能继续暴露已撤销入口；
- 当前用户基础 Profile / Preferences 不因管理权限变化消失。

## 9. 验收

- 身份治理全部归 `/system/access/**`；
- 当前用户安全归 `/account/security`；
- Platform Admin 不自动获得敏感业务内容读取权限；
- Capability 与后端 Permission Catalog 一一映射；
- 不再使用全局一级 Identity Center 或历史 `/identity-center/*` 路由。
