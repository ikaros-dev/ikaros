# 自动化应用与系统集成 — CMS Console 交互规格

> 自动化规则属于“应用 / 自动化”；Plugin、Connector、Webhook、Metadata Source 等平台连接属于“系统 / 集成”。两者不再合并成全局一级“集成自动化中心”。

## 1. 自动化应用

**Base Route：** `/apps/automation`

建议视图：

```text
/apps/automation
/apps/automation/rules
/apps/automation/history
```

自动化应用只在能力启用且当前用户拥有 `automation.*` 权限时显示。

### Rules

规则编辑器使用 Trigger → Conditions → Actions 模型，并明确每个 Action 所需能力和目标 Scope。

保存前校验循环、无效引用、缺失权限和潜在高风险动作。

### Execution History

自动化应用可以展示业务历史摘要，但真实长期执行实例统一链接到“资源 / 活动中心”（`/resources/activity`）。

不创建独立 Automation Execution Center。Attempt、Retry、Worker、Payload 等进入活动中心 Advanced 或“系统 / 运维 / 系统诊断”（`/system/operations/diagnostics`）。

## 2. 系统 / 集成

`/system/integrations` 是二级目录根，只负责组织和 redirect，不渲染独立集成页面。

用户可见菜单按以下职责组织：

- 插件管理：`/system/integrations/plugins`；
- 外部集成：`/system/integrations/external`；
- 事件投递：`/system/integrations/events`。

## 3. 插件管理

插件安装、启用、升级、权限授权和卸载属于 `/system/integrations/plugins`。

插件必须声明：

- Capability；
- 提供的 App Entry / Resource Type / Action；
- 所需外部连接；
- 安全和数据访问范围。

插件不能通过前端菜单注册绕过后端权限。插件提供的业务 App 进入“应用 / 插件应用”，并位于 `/apps/plugins/<appId>/**` 或受控子路径，不提升为新的全局一级入口。

## 4. 外部集成 / Metadata Sources

`/system/integrations/external` 负责 Connector、Metadata Source、External Protocol / Publishing Integration 等外部连接的配置、Credential 状态、Health 和启停。

用户实际“添加资源”仍从“资源 / 添加资源”（`/resources/add`）开始；添加资源只引用已配置 Connector，不要求用户在导入流程里学习系统级连接管理。

Metadata Source 配置属于“系统 / 集成 / 外部集成”；具体 Resource 的元数据冲突在 `/resources/library/:resourceId` 处理。

## 5. 事件投递

平台级 Webhook、Event Delivery、Retry / Replay 与投递诊断属于 `/system/integrations/events`。

长期 Retry / Replay 进入“资源 / 活动中心”（`/resources/activity`），业务页面使用可理解的动作描述；内部 Event ID / Delivery Attempt 进入 Advanced。跨系统底层问题可进一步进入 `/system/operations/diagnostics`。

## 6. Device Sync

Drive Device Sync 的用户产品配置在 `/apps/drive/sync`。平台级 Connector / Device Registry 只有确实属于跨 App 平台治理时才进入“系统 / 集成 / 外部集成”。

## 7. 验收

- 自动化只作为“应用 / 自动化”（`/apps/automation`）；
- `/system/integrations` 只作为目录根，不渲染业务页面；
- 插件管理使用 `/system/integrations/plugins`；
- 外部集成使用 `/system/integrations/external`；
- 事件投递使用 `/system/integrations/events`；
- 添加资源使用 `/resources/add`，与系统级 Connector 配置分离；
- Automation / Integration 长期执行进入 `/resources/activity`；
- 不再存在全局一级 Integration Center、独立 Executions Center 或历史 `/integration-center/*` 设计路由。
