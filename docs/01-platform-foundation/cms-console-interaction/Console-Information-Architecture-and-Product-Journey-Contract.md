# Ikaros V2 Console 信息架构契约

> 状态：设计基线。
>
> 当前 V2 Console 按从零设计处理。本文只描述目标最终态，不要求保留历史菜单、历史路由或历史页面结构。

## 1. 设计目标

Console 面向用户任务，而不是后端模块。后端继续保持 Resource、Attachment、Blob、Task、Provider 等领域模型，但这些模型不能直接决定全局导航。

默认 Console 只回答七类问题：当前需要关注什么、内容在哪里、如何添加内容、后台正在做什么、数据是否安全、启用了哪些应用、系统如何管理。

## 2. 一级导航

| 入口 | Canonical route | 职责 |
|---|---|---|
| Dashboard | `/dashboard` | Attention、异常和正在进行的工作 |
| Library | `/library` | 内容浏览、搜索和 Resource 管理 |
| Add Content | `/add` | 上传、扫描、识别、确认导入 |
| Activity | `/activity` | 所有长期后台工作 |
| Storage | `/storage` | 存储分布、Provider、策略、恢复和维护 |
| Apps | `/apps` | Drive、Documents、Media、Planning、Finance、AI 等可选产品 |
| System | `/system` | Access、Audit、Integrations、Settings、Health、Diagnostics |

Profile、Preferences 和当前账号 Security 从头像菜单进入。

功能 Issue 不得自行增加新的一级 Sidebar Entry，也不得因为存在新的 Controller、Task、Provider 或后端包就增加“XX 中心”。

## 3. Canonical route tree

```text
/dashboard
/library
/library/:resourceId
/library/collections
/library/search
/add
/activity
/activity/:activityId
/storage
/storage/providers
/storage/providers/:providerId
/storage/policy
/storage/archive
/storage/maintenance
/storage/backup
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
/system/access/**
/system/audit/**
/system/integrations/**
/system/notifications/**
/system/settings/**
/system/health/**
/system/diagnostics/**
/account/profile
/account/preferences
/account/notifications
/account/security
```

当前设计不定义旧 route alias、redirect 或兼容页。实现发现历史 route 时直接收敛到以上最终结构。未来若进入稳定版本后需要兼容，另行建立版本化迁移契约。

## 4. Dashboard

Dashboard 必须 Attention-first，而不是 KPI-first。优先展示：导入失败、内容不可用、Storage Provider 异常、元数据冲突、备份或同步失败、需要人工动作的后台工作。

Unknown、未探测、请求失败或数据缺失不得显示为 Healthy。每条 Attention 优先进入最接近业务对象的详情页。

## 5. Library 与 Resource Detail

Library 是内容唯一主入口，不再设计平级的“资源中心”和“内容与媒体中心”。支持业务类型、Collection、Tag、Lifecycle、Availability、Source 等筛选。

Resource Detail 是内容调查和内容级操作的 canonical 起点。默认展示标题、类型、内容结构、Availability、元数据来源、关系、集合、标签和直接业务动作。

Resource ID、Attachment ID、Blob ID、Placement、Replica、Task Attempt、Worker、Lease、Provider Key、Idempotency Key 等实现信息只在 Advanced / Diagnostics 中出现。

内容问题必须从剧集、章节、曲目、图片等业务对象解释，而不是要求用户手工沿 Resource → Attachment → Blob → Placement 排查。

## 6. Add Content

用户流程固定为：来源选择 → 扫描或上传 → 预览识别 → 重复和映射处理 → 确认 → Activity → Library。

Source、Scan、Candidate、Plan、Run 等可以继续作为后端模型，但不能成为普通用户的主操作语言。

## 7. Activity

Activity 是后台工作的唯一用户入口。Import、Metadata Sync、Archive/Restore、Backup、Download、AI、Automation、Index rebuild、Blob verify/repair 等都在这里统一观察。

列表使用业务类型、动作描述、关联对象、状态、进度、更新时间和下一步。Attempt、Worker、Lease、Raw Payload 等只进入 Advanced。

各业务文档不得再定义独立的“任务中心”。

## 8. Storage

Storage 固定分为：

- Overview：容量、分层、可用性、异常；
- Providers：连接、容量、健康、凭据状态、启停和探测；
- Policy：HOT/WARM/COLD/ARCHIVE/DEEP 等策略；
- Archive & Restore：归档与恢复；
- Maintenance：Integrity、GC、Placement/Replica、Repair、Delivery/CDN 高级诊断；
- Backup：备份与恢复。

Provider、Policy、GC 和 Placement Repair 不得继续混在同一默认页面。

## 9. Apps

Drive、Documents、Media、Planning、Finance、Private Notes、Passwords、AI、Sharing、Analytics、Automation 等业务产品统一进入 Apps。

只有当前部署启用且当前用户有权访问的 App 才显示。App 内可以有二级导航，但不能提升为全局一级入口。App 产生的后台工作仍进入全局 Activity。

## 10. System

System 统一承载：

- Access：Users、Roles、Permissions、Authentication；
- Audit；
- Integrations：Plugins、Connectors、Webhooks、Metadata Sources；
- Notifications；
- Settings；
- Health；
- Diagnostics。

当前用户自己的 Profile、Preferences、Security 不属于 System 管理菜单。

## 11. 产品语言

主导航、页面标题、主按钮和主要状态必须使用用户可理解的业务语言。Blob、Placement、Lease 等实现词汇默认进入 Advanced。

Provider 只有在真实外部系统配置场景中使用，并带业务限定，例如“存储 Provider”。

## 12. Issue 与验收规则

A/B/C/R 等能力 Issue 负责领域能力，不负责自行扩张全局 IA。涉及 Console 的功能除了 API、数据库、权限等 Capability Acceptance，还必须通过适用的 Product Journey Acceptance。

当前阶段设计只描述最终态，不创建临时兼容导航或旧页面保留区。

## 13. 完成标准

- Sidebar 固定为七个一级工作区；
- Dashboard 是默认入口并承担 Attention-first 总览；
- Library 是内容主入口；
- Resource Detail 是内容调查起点；
- Add Content 不暴露 Ingestion 状态机；
- Activity 是后台工作唯一入口；
- Storage 完成职责分层；
- 可选产品归 Apps；
- 平台治理归 System；
- 账号入口归头像菜单；
- 所有 Console 设计文档和路由矩阵使用 canonical IA；
- 核心 Golden Path 有可重复验收证据。

关联：[Console 产品旅程验收契约](./Console-Product-Journey-Acceptance-Contract.md)。
