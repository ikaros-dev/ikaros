# @runikaros/api-client

Ikaros 的 JavaScript API 客户端请求库。使用
[OpenAPI Generator](https://openapi-generator.tech/) 生成。

## 说明
当前包版本 x.y.z 中 x.y 部分与 服务端版本的 x.y部分一致，
也就是服务端版本0.4.z的api包版本肯定也是0.4.z，其中z大概率不同

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



~~5. npm 发布，如果未登录需要先登录~~

自`v0.10.4`后不发布到中央仓库，后如果有需要再看。

```bash
npm login
npm publish
```


6. git 提交

选择当前目录下的更改进行`git add .`

```bash
git commit -am "build: gen new api-client v0.12.0"
```

合成版(powershell)，升级 package.json 版本，并启动服务端后，在 api-client 路径下：

```
pnpm gen | pnpm build | git add .

```

7. 更新

每次更新后，需要在`console` 目录下，运行 `pnpm i` 重新安装下依赖
