# A01-02 模块依赖边界决策

日期：2026-09-10

## 决策

保持根 `pom.xml` 的 Maven Multi-Module 和单进程 Modular Monolith。模块间的稳定协作边界为：

- `*-api` 只承载公开 Capability、Command、Query、Event Contract、DTO 和权限契约；不得依赖业务实现模块。
- 业务实现模块默认只依赖自己的 `*-api`、其他模块的 `*-api` 以及平台公开 API。
- `application` 是唯一 Composition Root，可以聚合业务实现模块；业务模块不得依赖 `application`。
- Controller 进入 Owner 的 Application API；跨模块不得依赖 Entity、Repository、Persistence、私有 SQL 或内部 Bean。

## 当前实现与取舍

当前 POM 已经表达了大部分上述边界，但保留以下历史实现依赖作为迁移例外：

| 依赖方 | 被依赖方 | 处理 |
|---|---|---|
| `integration` | `common` | 暂保，后续提取为公开平台契约 |
| `operations` | `common` | 暂保，后续提取为公开平台契约 |
| `storage` | `common` | 暂保，后续提取为公开平台契约 |
| `storage` | `integration` | 暂保，后续改为 `integration-api` 能力 |

这些例外是显式白名单，不代表目标架构合规；Architecture Test 会阻止任何新的实现模块直连和 `*-api` 反向依赖实现模块。这里不切换 Gradle、不重排 Maven 聚合、不在 A01-02 中跨领域迁移代码。

## 验收样例

- 通过：`resource -> resource-api`、`media -> storage-api`、`application -> resource`。
- 失败：任意 `resource-api -> resource`、任意业务模块 `-> application`、新增业务实现模块 `-> storage` 等实现直连。

## 验证

`application/src/test/java/run/ikaros/common/ModuleDependencyBoundaryTest.java` 读取根 POM 的实际依赖，校验公开 API、实现模块和 Composition Root 的方向；白名单以外的新增实现直连会使 Maven 测试失败。
