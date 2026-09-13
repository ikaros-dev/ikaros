# AI App — CMS Console 交互规格

> AI 是 Apps 下的可选产品能力，不是全局一级工作区。AI 长任务统一进入全局 Activity。

## 1. App Entry

Base Route：`/apps/ai`

```text
/apps/ai
/apps/ai/models
/apps/ai/personas
/apps/ai/privacy
/apps/ai/usage
```

只有 AI 已启用且当前用户拥有相应 `ai.*` 能力时显示 App。

## 2. AI Home / Assistant

`/apps/ai` 提供 AI 助手和可用能力入口。

涉及 Resource、Document、Media 等对象时必须重新检查目标对象读取/写入权限。AI 权限本身不能扩大业务数据访问范围。

## 3. Models / Providers

`/apps/ai/models` 管理用户可选择或平台允许暴露的模型能力。

平台级 Provider Credential、全局 Endpoint 和系统级启停属于 `/system/integrations` 或 `/system/settings`；AI App 只展示当前用户可使用的模型和必要业务配置。

## 4. Personas

`/apps/ai/personas` 管理 Persona、提示配置和允许的上下文策略。Persona 不获得独立于调用用户的额外数据权限。

## 5. Privacy

`/apps/ai/privacy` 展示 AI 上下文、数据发送边界、历史/记忆策略和用户可控设置。

敏感域内容进入 AI 前必须满足对应域的读取和数据使用策略。

## 6. Usage

`/apps/ai/usage` 展示当前用户有权查看的调用量、费用或配额。系统级 Provider 总账或组织级配置属于 System。

## 7. AI Jobs

不再设计独立 AI Jobs / AI Task Center。

需要异步处理的 AI 工作注册到 `/activity`，Activity 行显示业务动作、关联 Resource/Document、状态和进度。模型内部 Trace、Attempt、Request ID 等进入 Activity Advanced 或 System Diagnostics。

## 8. 验收

- AI 只从 Apps 进入；
- AI 权限不扩大业务对象权限；
- 平台 Provider 配置与用户 AI App 分离；
- AI 长任务只通过全局 Activity 观察；
- 不再存在全局一级 AI Center 或历史 `/ai-center/*` 设计路由。
