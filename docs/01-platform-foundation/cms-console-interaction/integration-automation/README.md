# Automation App 与 System Integrations — CMS Console 交互规格

> 自动化规则属于 Apps 业务产品；Plugin、Connector、Webhook、Metadata Source 等平台连接属于 System / Integrations。两者不再合并成全局一级“集成自动化中心”。

## 1. Automation App

**Base Route：** `/apps/automation`

建议视图：

```text
/apps/automation
/apps/automation/rules
/apps/automation/history
```

Automation App 只在能力启用且当前用户拥有 `automation.*` 权限时显示。

### Rules

规则编辑器使用 Trigger → Conditions → Actions 模型，并明确每个 Action 所需能力和目标 Scope。

保存前校验循环、无效引用、缺失权限和潜在高风险动作。

### Execution History

Automation App 可以展示业务历史摘要，但真实长期执行实例统一链接到 `/activity`。

不创建独立 Automation Execution Center。Attempt、Retry、Worker、Payload 等进入 Activity Advanced 或 System Diagnostics。

## 2. System Integrations

**Base Route：** `/system/integrations`

承载平台级连接：

- Plugins；
- Connectors；
- Webhooks；
- Metadata Sources；
- External Protocol / Publishing Integration；
- 其他需要平台管理员配置的外部连接。

## 3. Plugins

插件安装、启用、升级、权限授权和卸载属于 `/system/integrations/plugins`。

插件必须声明：

- Capability；
- 提供的 App Entry / Resource Type / Action；
- 所需外部连接；
- 安全和数据访问范围。

插件不能通过前端菜单注册绕过后端权限。

## 4. Connectors / Metadata Sources

`/system/integrations/connectors` 和适用子页负责平台级来源配置、Credential 状态、Health 和启停。

用户实际“添加内容”仍从 `/add` 开始；Add Content 只引用已配置 Connector，不要求用户在导入流程里学习系统级连接管理。

Metadata Source 配置属于 System；具体 Resource 的元数据冲突在 `/library/:resourceId` 处理。

## 5. Webhooks / Events

平台级 Webhook 配置和 Delivery Diagnostics 属于 System Integrations / Diagnostics。

长期 Retry / Replay 进入 `/activity`，业务页面使用可理解的动作描述；内部 Event ID / Delivery Attempt 进入 Advanced。

## 6. Device Sync

Drive Device Sync 的用户产品配置在 `/apps/drive/sync`。平台级 Connector / Device Registry 只有确实属于跨 App 平台治理时才进入 System Integrations。

## 7. 验收

- Automation 只作为 `/apps/automation` App；
- Plugins / Connectors / Webhooks / Metadata Sources 只在 `/system/integrations/**`；
- Add Content 与系统级 Connector 配置分离；
- Automation / Integration 长期执行进入 `/activity`；
- 不再存在全局一级 Integration Center 或独立 Executions Center；
- 不再使用历史 `/integration-center/*` 设计路由。
