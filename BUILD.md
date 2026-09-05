# Ikaros 构建与本地开发

本文档对应当前仓库结构：后端是 Maven 单模块 Spring Boot 应用，前端位于 `console/`，数据库使用 PostgreSQL，表结构由应用启动时的 `r2dbc-migrate` 自动升级。

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20.19+ 或 22.13+
- pnpm 9+
- PostgreSQL 18+
- Docker Desktop（运行 Testcontainers 测试时需要）

当前主要版本：Spring Boot 4.0.5、Spring Framework 7.0.6、R2DBC PostgreSQL、r2dbc-migrate 4.0.1、Vue 3.5、Vite 7。

## 获取代码

```shell
git clone https://github.com/ikaros-dev/ikaros.git
cd ikaros
git submodule update --init --recursive
```

## PostgreSQL

可以使用 Docker 启动本地数据库：

```shell
docker run -d --name ikarosdb_dev -p 5432:5432 `
  -e POSTGRES_DB=ikaros `
  -e POSTGRES_USER=ikaros `
  -e POSTGRES_PASSWORD=openpostgresql `
  postgres:18-alpine
```

默认连接配置：

```text
R2DBC URL: r2dbc:postgresql://localhost:5432/ikaros
用户名:    ikaros
密码:      openpostgresql
```

可通过 `IKAROS_R2DBC_URL`、`IKAROS_DB_USERNAME`、`IKAROS_DB_PASSWORD` 覆盖。应用启动时会执行 `src/main/resources/db/migration/` 下的版本化 SQL，不要手动重复执行迁移脚本。

## 后端配置与运行

主配置文件是 `src/main/resources/application.yaml`，默认 HTTP 端口为 `10000`。本地私有配置使用未提交的 `src/main/resources/application-local.yaml`，该文件按需创建。

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

IntelliJ IDEA 运行配置：主类 `run.ikaros.IkarosApplication`，Active profiles 设置为 `local`，项目 JDK 使用 21。

应用启动后：

- API：`http://localhost:10000/api`
- OpenAPI：`http://localhost:10000/openapi.json`
- Swagger UI：`http://localhost:10000/swagger-ui.html`
- 就绪检查：`http://localhost:10000/api/health/ready`

存储 Provider 的凭据使用加密密钥保护。开发环境建议配置 `IKAROS_STORAGE_CREDENTIAL_ENCRYPTION_KEY`；共享环境还必须覆盖 `IKAROS_JWT_SECRET`，不要使用默认密钥。

## 前端开发

```shell
cd console
pnpm install
pnpm dev
```

前端默认运行在 `http://localhost:8848`。`console/.env.development` 会把 `/api` 代理到 `http://localhost:10000`；后端端口改变时修改 `VITE_API_PROXY`。

```shell
pnpm dev          # 开发服务器
pnpm build        # 生产构建
pnpm preview      # 预览构建产物
pnpm typecheck    # TypeScript 与 Vue 类型检查
pnpm lint         # ESLint、Prettier、Stylelint
```

## 构建与测试

```shell
mvn compile
mvn test
mvn -Dtest=AttachmentPreviewServiceTest test
mvn clean package -DskipTests
```

运行完整测试前需要启动 Docker Desktop，因为部分测试使用 Testcontainers。Spring Boot JAR 位于 `target/ikaros-2.0.0-SNAPSHOT.jar`，运行方式：

```shell
java -jar target/ikaros-2.0.0-SNAPSHOT.jar --spring.profiles.active=local
```

## 存储与 Delivery Provider 验证

附件访问链路至少需要：启用且可读的 Storage Provider、启用的 Delivery Provider，以及两者之间的 Delivery Binding。Binding 的 `priority` 数值越小，默认选择优先级越高。

Delivery Provider 的分发类型：

- `DIRECT`：使用 Storage Provider 生成的 presigned URL。
- `SERVER_PROXY`：使用 Ikaros 的 `/api/attachments/{id}/content` 代理接口；未配置代理基址时返回相对地址。
- `CDN`：使用已在 ESA、EdgeOne 等云服务商控制台配置好的 CDN 对外域名，由 CDN 回源 Storage Provider。

预览接口可以通过 Provider Key 选择分发配置：

```text
GET /api/attachments/{attachmentId}/preview-url?delivery_provider={providerKey}
```

不传、传空值或传入无效 Provider Key 时，服务端按 Binding 优先级选择默认 Provider。响应会返回所有可选 Provider，但只为当前选中的 Provider 生成访问 URL。

创建或手动检测 Delivery Provider 会生成后台任务：

```text
GET /api/background-tasks
GET /api/background-tasks/{taskId}
```

`UNKNOWN` 表示尚未得到有效探测结果，不等同于健康。需要确认 Provider 已绑定到可读的 Storage Provider，再手动触发检测；任务完成后结果会写回 Provider。

## 常见问题

### 启动时报数据库连接错误

确认 PostgreSQL 已启动，并检查 `IKAROS_R2DBC_URL`、用户名和密码。

### 前端页面能打开但 API 请求失败

确认后端运行在 `10000` 端口；如果端口不同，修改 `console/.env.development` 中的 `VITE_API_PROXY` 后重启 Vite。

### 预览接口返回“附件没有可用 Delivery Binding”

检查附件是否存在活动 Blob Placement，并确认对应 Storage Provider、Delivery Provider 和 Binding 都处于可用状态。被禁用的 Binding/Provider 或健康状态为 `UNHEALTHY` 的 Provider 不会参与选择。

## 提交前检查

```shell
mvn test
cd console
pnpm typecheck
```

## Git 工作流

### 提交 PR 后

更新本地主分支：

```shell
git checkout main | git pull upstream main | git push origin main | git remote prune origin
```

查看本地分支，并删除工作分支：

```shell
git branch
git branch -D {you_branch_name}
```

上游 PR 合并后，更新本地主分支。

如已添加上游仓库可跳过此命令：

```shell
git remote add upstream https://github.com/ikaros-dev/ikaros.git
```

```shell
git checkout main
git pull upstream main
```

查看当前分支和工作区状态：

```shell
git status
git branch
git log --oneline -10
```

提交本地修改前，先确认差异内容：

```shell
git diff
git diff --check
```

提交修改：

```shell
git add <文件路径>
git commit -m "type(scope): description"
```

添加上游仓库（只需执行一次）：

```shell
git remote add upstream https://github.com/ikaros-dev/ikaros.git
```

同步上游主分支：

```shell
git fetch upstream
git switch main
git pull --ff-only upstream main
```

查看远程仓库和远程分支：

```shell
git remote -v
git branch -r
```

确认分支已经合并且不再需要时，可以删除本地工作分支：

```shell
git branch -d <branch-name>
```

`git branch -D` 会强制删除未合并分支，除非明确确认，否则不要使用。推送远程分支前请先确认目标仓库和分支：

```shell
git push origin <branch-name>
```

## 相关文档

- [README.md](README.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CHANGELOG.md](CHANGELOG.md)
- [在线文档](https://docs.ikaros.run/)
