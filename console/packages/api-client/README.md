# @runikaros/api-client

Ikaros 的 JavaScript API 客户端请求库。使用
[OpenAPI Generator](https://openapi-generator.tech/) 生成。

## 开发环境

```bash
pnpm install
```

```bash
# 根据 OpenAPI 3.0 生成类型和网络请求的代码，此步骤需要启动 Ikaros 后端。
pnpm gen
```

```bash
pnpm build
```

## 发布版本

1. 先启动服务端

2. `package.json` 版本 patch 号加一，
   例子：从 `0.0.0` => `0.0.1`

3. 生成 TS 代码

```bash
pnpm gen
```

4. 编译 TS 代码

```bash
pnpm build
```

5. npm 发布，如果未登录需要先登录

```bash
npm login
```

```bash
npm publish
```

6. git 提交

选择当前目录下的更改进行`git add`

```bash
git commit -m "build: gen new api-client@x.y.z and publish to npm center repo in @runikaros/api-client"
```
