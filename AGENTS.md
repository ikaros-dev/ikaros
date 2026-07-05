# 工作流程

每次功能修改（包括修复 bug、新增功能、重构等）完成后，必须提交 commit，不得累积修改。

- **严禁修改组件和依赖版本**（Docker 镜像版本、Gradle 依赖版本等），如非要修改，必须经过用户确认

---

# 通用规范

- **AGENTS.md 严禁清空内容**，只允许修改指定章节
- 项目所有涉及编码的地方统一使用 **UTF-8**（无 BOM）
- 项目中涉及 npm 操作（安装依赖、运行脚本等）统一使用 **pnpm**，不得使用 npm 或 yarn

---

# 单元测试规范

- 方法测试统一使用 AssertJ 断言
- 所有 `Repository` 都应编写基本的 CRUD 单测
- 所有 `Service` 的每个 public 方法都应有对应单测
- 所有 `Controller` 都应编写接口级单测
- 涉及数据库的单测统一使用 Testcontainers 启动临时类级别数据库容器，测试完自动销毁

---

# DB 迁移规范

## 基本原则

- **已存在的 SQL 严禁修改**——只允许新增 SQL 文件
- **所有方案以服务端逻辑优先，非必要不修改数据库字段**——能通过代码解决的问题，不应通过改表结构解决
- **禁止使用数据库层面的外键引用，全部使用服务端逻辑层面的外键引用**——外键约束应在应用程序代码中维护，不在数据库中定义

## 命名规则

```
V{yyyyMMddHHmm}__{操作类型}_{表名}[_{属性/索引}].sql
```

| 段 | 说明 | 示例 |
|---|------|------|
| `V` | 迁移文件前缀 | |
| `{yyyyMMddHHmm}` | 12 位时间戳，按创建时间递增，新迁移必须大于当前最大版本号 | `202605301926` |
| `__` | 版本号与操作类型间的分隔符（双下划线） | |
| `{操作类型}` | `DDL` / `DML` 等，使用大写 | `DDL` |
| `{表名}` | 数据库表名，英文字母大写，多个单词使用 `_` 分隔 | `EPISODE_LIST` |
| `{属性/索引}` | 可选，涉及具体列、索引或补充说明时追加，使用大写并以 `_` 分隔 | `SEQUENCE_REGULAR` |

文件名中的操作类型、表名、属性、索引和补充说明统一使用**大写英文**，单词之间使用 `_` 连接，文件扩展名使用小写 `.sql`。

完整示例：`V202605301926__DDL_EPISODE_SEQUENCE_REGULAR.sql`



# API 规范

- 所有 `Controller` 的每个接口方法**必须**使用 SpringDoc (`@Operation` + `@ApiResponses`) 注解，用中文写明接口作用、参数说明和返回值说明，内容尽可能详细
- **前端接收的数据格式解析问题，能前端修改就前端修改，非必要不要修改后端接口**——优先保持后端接口稳定，数据展示层面的适配在前端完成
- **接口调用，一律优先使用流式接口**——有流式版本就优先使用流式，不主动降级为非流式

---

# 服务层规范

- 服务层必须面向接口编程，禁止直接暴露实现类
- 接口命名为 `XxxService`，默认实现命名为 `DefaultXxxService`（如 `UserService` + `DefaultUserService`）
- 接口与实现放在同一包下
- 控制器等消费者只依赖接口类型，通过 Spring DI 注入
- 接口中的每个方法**必须**用中文 Javadoc 注释，说明方法作用、参数和返回值，描述力求精练通俗

---

# 代码注释规范

- 每个 Java 类都需要加上中文注释，尽可能精简详细，必须包括当前类的作用介绍
- 每个属性都需要加上中文注释，尽可能精简详细，必须包括当前属性的作用介绍

---

# R2DBC 实体规范

所有 `@Id` 实体**必须**添加 `@Version private Long version` 字段，否则 `save()` 会因 ID 非空误判为已有记录而走 UPDATE（而非 INSERT），导致 `TransientDataAccessResourceException`。

同时 migration 中须为该表添加 `version BIGINT DEFAULT 0` 列。

ID 类型统一使用 `UUID`，数据库层使用 `uuid_v7()` 函数自动生成时间有序的主键。

---

# Commit 规范

```
type(scope): message
```

`type` 取值：
- `feat` — 新功能
- `fix` — 修复
- `refactor` — 重构
- `chore` — 杂项（构建、依赖、配置等）
- `docs` — 文档
- `config` — 配置变更
- `style` — 代码格式（非语义变更）
- `perf` — 性能优化
- `test` — 测试

`scope`（可选）: 模块名，如 `db`, `web`, `mobile`, `api`, `config` 等

`message`: 中文描述，首字母无需大写，末尾不加句号

`Co-Authored-By` 和 `Signed-off-by` 等尾部信息**一律禁止出现**，commit 消息纯净只含 type / scope / message

示例：
```
feat(db): 为表及字段添加中文 COMMENT ON
fix(web): 修复日期选择器时区问题
chore: 升级 Spring Boot 至 3.4.0
```

---

# 项目版本升级规范

升级项目版本时需要同时修改以下 **3 个文件**（缺一不可）：

| 文件 | 说明 | 示例 |
|------|------|------|
| `gradle.properties` | 项目版本号 | `version=1.2.0` |
| `server/src/main/resources/application.yaml` | `ikaros.plugin.system-version` 字段 | `system-version: 1.2.0` |
| `CHANGELOG.md` | 在文件最顶部新增版本标题行 | `# 1.2.0`（后跟空行） |

commit 消息统一使用 `chore: 升级版本至x.x.x`。
