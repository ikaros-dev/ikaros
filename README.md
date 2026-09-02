# Ikaros

Ikaros 是一个自托管的个人数字内容平台。当前工程从零实现，产品与领域边界以 [docs/v2/Product-Requirements-Document.md](docs/v2/Product-Requirements-Document.md) 为准。

## 当前基础能力

- Resource-centric 资源、标题、外部身份、Collection 与关系边界
- Attachment → Blob → Placement 的持久化模型与内容摘要去重
- 逻辑删除、恢复与可审计的 Blob GC 候选扫描
- 用户、角色、权限注册表、会话安全等级与权限加验证等级双重校验基础
- 用途绑定的 Email OTP Challenge、一次性消费、失败锁定与发送频率限制基础
- HTTP-first REST API 与 OpenAPI 文档
- PostgreSQL-first R2DBC 持久化与版本化 SQL 迁移

## 本地运行

准备 PostgreSQL 后设置 `IKAROS_R2DBC_URL`、`IKAROS_DB_USERNAME` 与 `IKAROS_DB_PASSWORD`，再执行：

```powershell
mvn spring-boot:run
```

API 文档：`http://localhost:10000/swagger-ui.html`。
