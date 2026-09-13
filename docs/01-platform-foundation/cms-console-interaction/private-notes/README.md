# Private Notes App — CMS Console 交互规格

> Private Notes 是 Apps 下的受保护业务产品。解锁状态、内容读取和导出继续遵守私密域安全边界。

## 1. App Entry

Base Route：`/apps/private-notes`

```text
/apps/private-notes
/apps/private-notes/conflicts
/apps/private-notes/recovery
```

只有 App 已启用且用户拥有相应 `private_note.*` 能力时显示。

## 2. Vault Home

`/apps/private-notes` 是主入口。未解锁时只显示安全解锁界面，不返回解密标题、摘要、Tag 或搜索内容。

解锁后展示笔记列表、搜索、Tag、更新时间和同步状态。

## 3. Conflicts

`/apps/private-notes/conflicts` 展示版本与同步冲突。冲突解决必须明确保留哪一侧、是否生成新版本以及是否存在数据覆盖影响。

长期同步或恢复动作进入全局 `/activity`。

## 4. Recovery

`/apps/private-notes/recovery` 承载恢复、导出和必要的安全材料流程。高影响操作按照安全策略要求重新验证。

## 5. 搜索与日志

解密后的标题、摘要、Tag 和搜索词不得写入普通 URL、Telemetry、通知预览或非安全日志。

全局搜索在 Vault 未解锁时只返回安全占位，不泄露笔记识别信息。

## 6. 验收

- Private Notes 只从 Apps 进入；
- 未解锁状态不泄露解密信息；
- Sync/Recovery 长任务进入 `/activity`；
- 不再存在全局一级 Private Notes Center 或历史 `/console/private-notes/*` 设计路由。
