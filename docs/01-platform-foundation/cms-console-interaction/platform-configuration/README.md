# 系统 / 平台配置 — CMS Console 交互规格

> 平台参数和运行配置统一归入“系统 / 平台配置”，不再作为全局一级“平台配置中心”。

## 1. 系统参数

**Route：** `/system/settings/parameters`

按配置域组织：

- General；
- Runtime；
- Resource defaults；
- Storage defaults（只放平台默认值，真实 Provider/Policy 仍在“存储”）；
- Notification defaults；
- Feature flags / experimental capability（如后端明确支持）；
- 其他平台级配置。

页面应解释每个配置的影响范围、默认值来源和是否需要重启。

`/system/settings` 只作为“平台配置”目录根，可 redirect 到 `/system/settings/parameters`，不渲染系统参数页面本身。

## 2. Parameters

平台参数使用有类型的 Form，而不是任意 Key/Value 编辑器作为默认体验。

只有确实属于动态扩展或内部高级配置的参数才使用通用参数表，并放入 Advanced。

Secret 不返回明文，保存后只展示已配置状态。

## 3. Dictionaries

业务字典只有在后端确实提供可配置 catalog 时才显示。

导航、核心 Resource 类型、权限 Key 和领域状态机不是“可随意编辑字典”，不得通过通用字典页重新定义产品契约。

## 4. Navigation

Console 用户可见 Sidebar 由 IA 契约固定为：

```text
仪表盘                  /dashboard
资源                    /resources/*
  资源库                /resources/library
  添加资源              /resources/add
  活动中心              /resources/activity
存储                    /storage/*
应用                    /apps/*
系统                    /system/*
```

存储、应用、系统的完整子菜单继续由 IA 契约定义。平台配置不提供“平台菜单”页面让管理员任意编辑该全局结构。

菜单层级与 route 层级必须一致；目录节点只做组织和 redirect，不复用子页面组件。

插件和 App 的可见性来自启用状态、Capability 和注册契约，而不是手工编辑全局菜单树。

## 5. Configuration Changes

配置修改必须：

- 展示当前值和来源；
- 说明生效方式；
- 执行字段校验；
- 使用版本/并发控制；
- 高风险配置按策略要求确认或 Step-up；
- 写入 Audit。

需要异步应用或重建的配置修改进入“资源 / 活动中心”（`/resources/activity`）。

## 6. 与其他工作区边界

- 存储提供方 → `/storage/providers`；
- 存储策略 → `/storage/policy`；
- 用户管理 → `/system/access/users`；
- 角色与权限 → `/system/access/roles-permissions`；
- 身份认证 → `/system/access/authentication`；
- 插件管理 → `/system/integrations/plugins`；
- 外部集成 → `/system/integrations/external`；
- 事件投递 → `/system/integrations/events`；
- 系统健康 → `/system/operations/health`；
- 通知中心 → `/system/communications/notifications`；
- 审计日志 → `/system/communications/audit`；
- 当前用户偏好 → `/account/preferences`。

## 7. 验收

- 系统参数页面只使用 `/system/settings/parameters`；
- `/system/settings` 只作为目录根 redirect；
- 用户可见名称使用“系统参数”，不把 Settings 暴露为新的一级菜单；
- 不提供任意重写一级 IA 的菜单编辑能力；
- 配置域边界不复制存储 / 访问控制 / 集成页面；
- 异步应用进入 `/resources/activity`；
- 不再使用全局一级 Platform Config 或历史 `/platform-config/*` 路由。
