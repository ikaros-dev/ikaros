# Ikaros Console

基于 Vue 3、Vite、TypeScript 的 Ikaros V2 CMS Console 前端。

## 开发

```bash
pnpm install
pnpm dev
```

默认入口：`/console/dashboard`。

需要调用受保护的 Resource、Drive、Planning、Finance 等接口时，在环境中配置当前用户 UUID `VITE_ACTOR_ID`；请求会通过 `X-Ikaros-Actor-Id` 传递当前用户主体。开发服务器默认将 `/api` 代理到 `http://localhost:8080`，可通过 `VITE_DEV_API_TARGET` 覆盖。

## 构建

```bash
pnpm build
```

页面数据优先来自后端 `/api` 接口；后端未提供对应查询能力的模块显示空态，并保留页面权限元数据和危险操作确认流程。
