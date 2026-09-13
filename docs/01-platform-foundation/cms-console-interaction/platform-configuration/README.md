# System / Settings — CMS Console 交互规格

> 平台参数和运行配置统一归入 System / Settings，不再作为全局一级“平台配置中心”。

## 1. Settings Home

**Route：** `/system/settings`

按配置域组织：

- General；
- Runtime；
- Content defaults；
- Storage defaults（只放平台默认值，真实 Provider/Policy 仍在 Storage）；
- Notification defaults；
- Feature flags / experimental capability（如后端明确支持）；
- 其他平台级配置。

页面应解释每个配置的影响范围、默认值来源和是否需要重启。

## 2. Parameters

平台参数使用有类型的 Form，而不是任意 Key/Value 编辑器作为默认体验。

只有确实属于动态扩展或内部高级配置的参数才使用通用参数表，并放入 Advanced。

Secret 不返回明文，保存后只展示已配置状态。

## 3. Dictionaries

业务字典只有在后端确实提供可配置 catalog 时才显示。

导航、核心内容类型、权限 Key 和领域状态机不是“可随意编辑字典”，不得通过通用字典页重新定义产品契约。

## 4. Navigation

Console 一级导航由 IA 契约固定，不提供“平台菜单”页面让管理员任意编辑 Overview / Library / Add Content / Activity / Storage / Apps / System。

插件和 App 的可见性来自启用状态、Capability 和注册契约，而不是手工编辑全局菜单树。

## 5. Configuration Changes

配置修改必须：

- 展示当前值和来源；
- 说明生效方式；
- 执行字段校验；
- 使用版本/并发控制；
- 高风险配置按策略要求确认或 Step-up；
- 写入 Audit。

需要异步应用或重建的配置修改进入 `/activity`。

## 6. 与其他工作区边界

- Storage Provider / Tier Policy → `/storage`；
- Users / Roles / Permission → `/system/access`；
- Plugin / Connector → `/system/integrations`；
- System Health → `/system/health`；
- Notification delivery policy → `/system/notifications`；
- 当前用户偏好 → `/account/preferences`。

## 7. 验收

- 平台配置全部归 `/system/settings/**`；
- 不提供任意重写一级 IA 的菜单编辑能力；
- 配置域边界不复制 Storage/Access/Integrations 页面；
- 异步应用进入 `/activity`；
- 不再使用全局一级 Platform Config 或历史 `/platform-config/*` 路由。
