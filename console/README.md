# Ikaros Console

Ikaros Web Console 模块

# 环境

- Node.js: 22.22.1
- pnpm: 10.32.1

Gradle 的 `buildFrontend` 任务会自动下载并使用上述精确版本。本地直接执行前端命令时，需先通过 Corepack 启用 `package.json` 中锁定的 pnpm 版本。

### 编译依赖包

```
pnpm packagesBuild
```

这个命令执行了安装对应的`packages`目录下的两个包：

- `@runikaros/api-client`: 这个包封装了`axios`的`OpenAPI`接口
- `@runikaros/shared`: 这个包是一些通用类型定义


### 安装依赖

安装依赖前，请先进行[编译依赖包](#编译依赖包)，不然运行的时候会报错。

本地开发和构建打包前，都需要前安装依赖。

```
pnpm install
```

### 本地开发

```
pnpm dev
```

### 构建打包

```
pnpm build
```

构建的产物在父目录的对应目录里：`../server/src/main/resources/console`

### 初始化综合命令（可选）

如不清楚上述命令是否需要执行，只需要执行一次下方命令即可，后续可以正常`pnpm dev` 或者 `pnpm build`

```
pnpm packagesBuild && pnpm install && pnpm build
```
