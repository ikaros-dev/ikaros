# Passwords App — CMS Console 交互规格

> Passwords 是 Apps 下的高敏感业务产品。Secret 值不得进入普通 URL、日志、Analytics、通知预览、审计描述或通用搜索结果。

## 1. App Entry

Base Route：`/apps/passwords`

```text
/apps/passwords
/apps/passwords/generator
/apps/passwords/health
/apps/passwords/devices
```

只有 Passwords 已启用且用户拥有对应能力时显示 App。

## 2. Vault

`/apps/passwords` 是条目主入口。未解锁时不返回敏感条目内容。

解锁后可以按安全策略展示条目名称、安全摘要和分类。Secret 默认遮罩，Reveal / Copy 等动作按安全策略记录必要审计，但不记录 Secret 本身。

## 3. Generator

`/apps/passwords/generator` 提供本地或安全服务端生成能力。生成结果不写入 Telemetry 或普通日志。

## 4. Health

`/apps/passwords/health` 展示弱密码、重复使用、过期或其他已实现安全指标。指标只服务当前有权访问的数据，不通过统计泄露其他用户条目。

需要批量扫描的长期工作进入 `/activity`。

## 5. Devices / Access

`/apps/passwords/devices` 展示受信设备、访问状态和必要安全操作。高影响撤销或恢复动作按安全策略要求重新验证。

## 6. Search

通用全局搜索在 Vault 未解锁时不返回条目识别信息。解锁后的搜索词和结果摘要不得进入普通 URL 或 Analytics。

## 7. 验收

- Passwords 只从 Apps 进入；
- Secret 不出现在普通日志、URL 或通用搜索；
- 长期安全扫描进入 `/activity`；
- 不再存在全局一级 Password Center 或历史 `/console/password*` 设计路由。
