# Migration Version Policy

## 1. 目的与范围

本文是 Ikaros V2 数据库 Migration 文件版本与命名的正式契约，适用于所有生产 DDL、约束、索引、确定性 Seed 和必要的结构化 DML Migration。

本文是对以下架构规则的具体化：

- [Technical Architecture Design](../Technical-Architecture-Design.md)；
- [P0 Database Schema Design](./P0-Database-Schema-Design.md)；
- [ADR-001 Maven Multi-Module](../adr/ADR-001-maven-multi-module.md)。

## 2. 文件归属与路径

Migration 必须由拥有目标 Schema 的实现模块持有：

```text
<owner-module>/src/main/resources/db/migration/
```

`application` 只负责把各 Owner 模块的 Migration 聚合到运行时 classpath，并交给 `r2dbc-migrate` 执行；`application` 不拥有业务 Migration。

Owner 模块只能维护自己的 Schema。跨模块 Schema 不得通过私有 SQL 或 Repository 直接修改。

## 3. 文件名契约

文件名必须符合：

```text
V<version>__<description>.sql
```

当前版本号格式为：

```text
VyyyyMMddHHmm__<OWNER>_<OPERATION>.sql
```

示例：

```text
V202609131100__DML_AUTHORIZATION_CONSOLE_ROUTE_PERMISSIONS.sql
V202609141000__DDL_STORAGE_RESTORE_INDEX.sql
```

约束：

- `V` 为固定前缀；
- `version` 为全仓库共享的十进制单调版本号；
- 时间使用 `UTC+8` 时区计算；
- `description` 使用大写字母、数字和下划线，并应同时表达 Owner 与变更目的；
- 文件扩展名必须为 `.sql`；
- 不使用模块本地独立序列，不使用数据库自增值，不使用 Git commit hash。

## 4. 版本号生成规则

### 4.1 基础版本号

以创建 Migration 时的 `UTC+8` 本地时间生成候选版本号：

```text
yyyyMMddHHmm
```

例如，UTC+8 时间 `2026-09-13 11:00` 的候选版本号为：

```text
202609131100
```

### 4.2 全局单调性与冲突处理

生成前必须检查所有 Owner 模块 Migration 目录以及当前变更中的文件名：

1. 取得全仓库已有的最大版本号 `M`；
2. 生成当前 UTC+8 时间候选值 `C`；
3. 若 `C > M` 且文件名未占用，使用 `C`；
4. 若 `C <= M` 或文件名已占用，使用大于 `M` 且未占用的最小十进制整数；
5. 版本依赖顺序不得被文件发现顺序、模块构建顺序或操作系统目录顺序改变。

因此，版本号是“基于 UTC+8 时间、受全局单调约束的逻辑版本号”。发生同一分钟并发创建或补丁版本追赶时，最终值可能不再是严格的自然时间值，但必须保持全局唯一和单调递增。

### 4.3 已发布版本

已经进入共享环境或正式环境的 Migration：

- 不得原地修改；
- 不得重命名；
- 不得复用版本号；
- 修复必须追加新的、更大的 Migration；
- 不得通过删除 Migration 文件伪造回滚。

## 5. SQL 内容规则

- 生产 DDL、约束和索引必须进入对应 Owner 的 Migration；
- Permission、Built-in Role 等 Seed 必须 deterministic，并使用幂等冲突策略；
- 结构变更遵循 Expand → Migrate → Contract；
- 大规模 Backfill 不得塞入启动阻塞 Migration；
- Migration 不得调用不可回滚的外部 API；
- Repository、`DatabaseClient` 和普通启动逻辑不得偷偷修改 Schema；
- 不提供假装安全的 destructive down migration。

## 6. 执行与校验

`application` 聚合后的运行时资源路径为：

```text
classpath*:/db/migration/*.sql
```

`r2dbc-migrate` 使用数据库 Migration 历史记录判断待执行版本。默认历史表为：

```text
public.migrations
```

提交前必须至少完成以下检查：

```powershell
Get-ChildItem -Recurse -File -Filter 'V*__*.sql' | Sort-Object Name
mvn -pl application -am process-resources
```

并确认目标文件已进入：

```text
application/target/classes/db/migration/
```

如果数据库中的最大已执行版本高于新文件版本，`r2dbc-migrate` 不会执行该新文件。因此，迟到的修复 Migration 必须使用更大的全局版本号。

## 7. 命名示例

```text
authorization/src/main/resources/db/migration/
└── V202609141000__DML_AUTHORIZATION_CONSOLE_ROUTE_PERMISSIONS.sql

storage/src/main/resources/db/migration/
└── V202609141001__DDL_STORAGE_RESTORE_INDEX.sql
```

上例中的两个版本号属于同一个全局序列，即使文件位于不同 Owner 模块，也不能重复或倒置。
