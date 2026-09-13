# 登录、认证与首次初始化交互规格

> 本文描述登录前入口、首次初始化、普通登录、验证、账号恢复与认证失效场景。它不属于登录后的 Sidebar。

## 1. Canonical Routes

```text
/setup
/login
/login/verify
/recovery/**
```

认证成功后的默认目标是用户原本请求的 canonical route；没有目标时进入 `/overview`。

## 2. First Setup

`/setup` 只在系统未初始化时可用。

流程至少包含：

- 创建初始管理员；
- 必要基础配置；
- 明确完成状态；
- 完成后进入登录或已认证 `/overview`。

初始化状态必须由后端一次性约束，不能只依赖前端隐藏入口。

## 3. Login

`/login` 支持系统实际启用的认证方式。

错误状态区分：凭据错误、账号不可用、认证服务异常、需要额外验证等，不泄露不必要的账号存在性信息。

## 4. Verification

`/login/verify` 用于登录事务或 Step-up Verification。

验证完成后返回原业务动作或 canonical page，不跳到旧 Console 首页。

## 5. Recovery

`/recovery/**` 按后端安全策略实现。恢复材料和敏感信息不得进入普通日志、Telemetry 或 URL Query。

## 6. Session Expiry

认证失效时保留安全的当前目标信息，重新登录后回到用户原来访问的 canonical route。未保存的敏感编辑内容按领域安全规则处理。

## 7. Authorization 与 Authentication 区分

- 401：认证失效或未登录；
- 403：已认证但无权进入；
- Secure Domain 未解锁：进入对应 Unlock 流程；
- 不得把上述状态统一显示成“无权限”。

## 8. 验收

- 登录后默认进入 `/overview`；
- 深链接恢复使用 canonical routes；
- Setup 只在未初始化状态可用；
- Login / Step-up / Recovery 状态语义明确；
- 不依赖历史 Console Route。
