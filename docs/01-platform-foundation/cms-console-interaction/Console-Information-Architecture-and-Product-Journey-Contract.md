# Ikaros V2 Console 信息架构契约

> 状态：设计基线。
>
> 当前 V2 Console 按从零设计处理。本文只描述目标最终态，不要求保留历史菜单、历史路由或历史页面结构。

## 1. 设计目标

Console 面向用户任务，而不是后端模块。后端继续保持 Resource、Attachment、Blob、Task、Provider 等领域模型，但这些模型不能直接决定全局导航。

默认 Console 只回答五类一级问题：当前需要关注什么、资源如何管理、数据如何存储、启用了哪些应用、系统如何治理。

用户可见菜单使用中文业务语言；Route、Workspace ID、Capability 等内部契约继续使用稳定英文标识。

**菜单层级与路由层级必须一致。一个页面只有一个 canonical route；可展开目录节点本身不是页面，不得复用子页面组件。**

## 2. Sidebar 菜单层级

```text
Ikaros 后台管理系统
│
├─ 仪表盘
│
├─ 资源
│  ├─ 资源库
│  ├─ 添加资源
│  └─ 活动中心
│
├─ 存储
│  ├─ 存储概览
│  ├─ 存储提供方
│  ├─ 存储策略
│  ├─ 归档管理
│  ├─ 备份管理
│  └─ 存储维护
│
├─ 应用
│  ├─ 云盘
│  ├─ 文档
│  ├─ 媒体
│  ├─ 计划
│  ├─ 财务
│  ├─ 私密笔记
│  ├─ 密码库
│  ├─ AI
│  ├─ 分享
│  ├─ 数据分析
│  └─ 自动化
│
└─ 系统
   ├─ 访问控制
   │  ├─ 用户管理
   │  ├─ 角色与权限
   │  └─ 身份认证
   ├─ 集成
   │  ├─ 应用管理
   │  ├─ 外部集成
   │  └─ 事件投递
   ├─ 通知与审计
   │  ├─ 通知中心
   │  └─ 审计日志
   ├─ 平台配置
   │  └─ 系统参数
   └─ 运维
      ├─ 系统健康
      └─ 系统诊断
```

“资源”“存储”“应用”“系统”是一级目录节点；System 下的“访问控制”“集成”“通知与审计”“平台配置”“运维”是二级目录节点。目录节点可以执行默认子路由 redirect，但**不渲染独立页面**。

Profile、Preferences 和当前账号 Security 从头像菜单进入，不进入 Sidebar。

功能 Issue 不得自行增加新的一级 Sidebar Entry，也不得因为存在新的 Controller、Task、Provider 或后端包就增加“XX 中心”。

## 3. 菜单页面与 Canonical route

| 菜单页面 | Canonical route | 职责 |
|---|---|---|
| 仪表盘 | `/dashboard` | Attention、异常和正在进行的工作 |
| 资源 / 资源库 | `/resources/library` | Resource 浏览、搜索和 canonical Resource Detail |
| 资源 / 添加资源 | `/resources/add` | 上传、扫描、识别、确认导入 |
| 资源 / 活动中心 | `/resources/activity` | 所有长期后台工作 |
| 存储 / 存储概览 | `/storage/overview` | 容量、分层、可用性和异常 |
| 存储 / 存储提供方 | `/storage/providers` | Provider 连接、健康、容量和凭据状态 |
| 存储 / 存储策略 | `/storage/policy` | Tier / Lifecycle 策略 |
| 存储 / 归档管理 | `/storage/archive` | Archive / Restore |
| 存储 / 备份管理 | `/storage/backup` | Backup / Restore Point |
| 存储 / 存储维护 | `/storage/maintenance` | Integrity / GC / Placement / Repair |
| 系统 / 访问控制 / 用户管理 | `/system/access/users` | 用户管理 |
| 系统 / 访问控制 / 角色与权限 | `/system/access/roles-permissions` | Role / Permission 管理 |
| 系统 / 访问控制 / 身份认证 | `/system/access/authentication` | 认证与安全策略 |
| 系统 / 集成 / 应用管理 | `/system/integrations/apps` | App 管理 |
| 系统 / 集成 / 外部集成 | `/system/integrations/external` | Connector / Webhook / Metadata Source 等外部连接 |
| 系统 / 集成 / 事件投递 | `/system/integrations/events` | Event Delivery / Retry / Diagnostics |
| 系统 / 通知与审计 / 通知中心 | `/system/communications/notifications` | 系统通知策略和投递 |
| 系统 / 通知与审计 / 审计日志 | `/system/communications/audit` | Audit 查询 |
| 系统 / 平台配置 / 系统参数 | `/system/settings/parameters` | 平台参数和运行配置 |
| 系统 / 运维 / 系统健康 | `/system/operations/health` | Health / Readiness |
| 系统 / 运维 / 系统诊断 | `/system/operations/diagnostics` | 高级诊断 |

App 页面继续位于 `/apps/<app>/**`；Console 不再提供通用插件应用承载页，插件声明的 App Entry 不会自动注册为 Console 路由。

## 4. Canonical route tree

```text
/dashboard

/resources
  /resources/library
  /resources/library/:resourceId
  /resources/library/collections
  /resources/library/search
  /resources/add
  /resources/activity
  /resources/activity/:activityId

/storage
  /storage/overview
  /storage/providers
  /storage/providers/:providerId
  /storage/policy
  /storage/archive
  /storage/backup
  /storage/maintenance

/apps
  /apps/drive/**
  /apps/documents/**
  /apps/media/**
  /apps/planning/**
  /apps/finance/**
  /apps/private-notes/**
  /apps/passwords/**
  /apps/ai/**
  /apps/sharing/**
  /apps/analytics/**
  /apps/automation/**

/system
  /system/access/users
  /system/access/roles-permissions
  /system/access/authentication
  /system/integrations/apps
  /system/integrations/external
  /system/integrations/events
  /system/communications/notifications
  /system/communications/audit
  /system/settings/parameters
  /system/operations/health
  /system/operations/diagnostics

/account/profile
/account/preferences
/account/notifications
/account/security
/account/sessions
/account/api-tokens
```

目录根可以使用 redirect，但 redirect 不是页面：

- `/resources` → 第一个可访问的资源子页面，默认 `/resources/library`；
- `/storage` → `/storage/overview`；
- `/apps` → `/apps/drive`；
- `/system`、`/system/access`、`/system/integrations`、`/system/communications`、`/system/settings`、`/system/operations` → 当前用户第一个可访问的子页面。

当前设计不定义旧 route alias 或兼容页。历史 `/library`、`/add`、`/activity`、`/storage` 页面语义应直接收敛到以上最终结构。

## 5. 仪表盘（Dashboard）

仪表盘必须 Attention-first，而不是 KPI-first。优先展示：导入失败、资源不可用、Storage Provider 异常、元数据冲突、备份或同步失败、需要人工动作的后台工作。

Unknown、未探测、请求失败或数据缺失不得显示为 Healthy。每条 Attention 优先进入最接近业务对象的详情页。

## 6. 资源

“资源”是 Resource 相关日常任务的统一一级目录，由资源库、添加资源和活动中心组成。不得再出现平级的“内容”“资源中心”“内容与媒体中心”等重复入口。

### 6.1 资源库（Library）与 Resource Detail

资源库 canonical route 为 `/resources/library`。Resource Detail 为 `/resources/library/:resourceId`。

资源库是 Resource 浏览和管理的唯一主入口，支持业务类型、Collection、Tag、Lifecycle、Availability、Source 等筛选。

Resource Detail 是资源调查和资源级操作的 canonical 起点。Resource ID、Attachment ID、Blob ID、Placement、Replica、Task Attempt、Worker、Lease、Provider Key、Idempotency Key 等实现信息只在 Advanced / Diagnostics 中出现。

### 6.2 添加资源（Add Content）

添加资源 canonical route 为 `/resources/add`。

用户流程固定为：来源选择 → 扫描或上传 → 预览识别 → 重复和映射处理 → 确认 → 活动中心 → 资源库。

Source、Scan、Candidate、Plan、Run 等可以继续作为后端模型，但不能成为普通用户的主操作语言。

### 6.3 活动中心（Activity）

活动中心 canonical route 为 `/resources/activity`，详情为 `/resources/activity/:activityId`。

Import、Metadata Sync、Archive/Restore、Backup、Download、AI、Automation、Index rebuild、Blob verify/repair 等长期工作都在这里统一观察。Attempt、Worker、Lease、Raw Payload 等只进入 Advanced。

## 7. 存储（Storage）

存储目录下每个菜单页面均使用二层路由：

- 存储概览：`/storage/overview`；
- 存储提供方：`/storage/providers`；
- 存储策略：`/storage/policy`；
- 归档管理：`/storage/archive`；
- 备份管理：`/storage/backup`；
- 存储维护：`/storage/maintenance`。

Provider、Policy、GC 和 Placement Repair 不得继续混在同一默认页面。

## 8. 应用（Apps）

应用工作区不设置独立总览页；`/apps` 默认进入首个业务应用 `/apps/drive`。Drive、Documents、Media、Planning、Finance、Private Notes、Passwords、AI、Sharing、Analytics、Automation 等业务产品使用 `/apps/<app>/**`。

只有当前部署启用且当前用户有权访问的 App 才显示。App 产生的后台工作仍进入 `/resources/activity`。

## 9. 系统（System）

System 的目录层级必须反映到 route prefix：

- 访问控制 → `/system/access/**`；
- 集成 → `/system/integrations/**`；
- 通知与审计 → `/system/communications/**`；
- 平台配置 → `/system/settings/**`；
- 运维 → `/system/operations/**`。

目录节点不渲染业务页面；真正页面继续使用三级 route，例如 `/system/access/users`、`/system/communications/audit`、`/system/operations/health`。

当前用户自己的 Profile、Preferences、Security 不属于 System 管理菜单。

## 10. 页面与导航规则

- 一个页面只拥有一个 canonical route；
- Sidebar 中有独立菜单项的页面必须拥有与菜单层级一致的 route；
- 目录节点不得与默认子页面共享同一页面组件；
- 目录根 redirect 只用于进入第一个可访问子页面；
- 页面内 Tab 不自动升级为 Sidebar 菜单和独立 route，除非其需要深链接、独立权限或独立生命周期；
- Detail 页面可以使用参数化子路由，但默认不显示在 Sidebar。

## 11. 产品语言

主导航、页面标题、主按钮和主要状态必须使用用户可理解的中文业务语言。Blob、Placement、Lease 等实现词汇默认进入 Advanced。

`Resource` 在产品主导航中统一称为“资源”；`Library` 的用户可见名称统一为“资源库”；`Add Content` 的用户可见名称统一为“添加资源”；`Activity` 的用户可见名称统一为“活动中心”。“内容”仍可在描述具体媒体、文档正文或业务载荷时使用，但不得再作为 Console 一级业务域名称。

## 12. Issue 与验收规则

A/B/C/R 等能力 Issue 负责领域能力，不负责自行扩张全局 IA。涉及 Console 的功能除了 API、数据库、权限等 Capability Acceptance，还必须通过适用的 Product Journey Acceptance。

当前阶段设计只描述最终态，不创建临时兼容导航或旧页面保留区。

## 13. 完成标准

- Sidebar 一级节点固定为仪表盘、资源、存储、应用、系统；
- 菜单页面与 route 层级一一对应；
- 资源分组固定包含资源库、添加资源、活动中心；
- 资源页面统一位于 `/resources/**`；
- 存储页面统一位于 `/storage/**`，概览使用 `/storage/overview`；
- `/apps` 默认进入首个业务应用 `/apps/drive`，业务 App 位于 `/apps/<app>/**`；
- System 的三级菜单页面与 `/system/<group>/<page>` 对齐；Sidebar 以不可点击分组标题平铺展示三级页面，不渲染为可展开的父子菜单；
- 仪表盘是默认入口并承担 Attention-first 总览；
- 活动中心是后台工作唯一用户入口；
- 账号入口归头像菜单；
- 所有 Console 设计文档和路由矩阵使用同一菜单命名和 canonical IA；
- 核心 Golden Path 有可重复验收证据。

关联：[Console 产品旅程验收契约](./Console-Product-Journey-Acceptance-Contract.md)。
