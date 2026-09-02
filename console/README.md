# Ikaros Console

基于 Vue 3、Vite、TypeScript 的 Ikaros V2 CMS Console 前端。

## 开发

```bash
pnpm install
pnpm dev
```

默认入口：`/console/dashboard`。

需要调用 Drive 与 Planning 用户接口时，在环境中配置 `VITE_ACTOR_ID`；请求会通过 `X-Ikaros-Actor-Id` 传递当前用户主体。

## 构建

```bash
pnpm build
```

当前前端使用文档定义的演示数据实现交互骨架。后端接入时，将 `App.vue` 中的演示数据替换为 `/api/v2` 查询，并保留页面状态、权限元数据和危险操作确认流程。
