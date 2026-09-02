# Ikaros V2 App 媒体交付 / Restore 安全补充交互

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Delivery / Restore Safety Supplement |
| 适用端 | Web / Flutter App |
| 状态 | Draft |
| 基线 | `Media-Archive-Restore-Interaction-Design.md` |
| 工程契约 | `../Media-Delivery-CDN-Archive-Restore-Engineering-Addendum.md` |

> 本文档补充 Restore Budget、Delivery Grant 续签/撤销、Playback Lease 与 Delivery Fallback 在客户端的交互约束。

---

## 1. Restore Budget 确认

当用户主动执行“恢复本季”等大范围动作且服务端返回 `REQUIRE_CONFIRMATION` 时，客户端展示服务端提供的安全摘要：

```text
将恢复 24 个视频
预计数据量：25.6 GB
该操作可能产生对象存储取回费用

[取消] [确认恢复]
```

约束：

- 客户端不自行计算是否超预算；服务端 Budget Guard 才是真相；
- 没有可靠价格模型时不显示伪精确人民币金额；
- 确认后使用服务端返回的 opaque confirmation token/reference 重试；
- confirmation token 不进入 Analytics / Crash Report；
- 自动预热超预算时直接停止，不弹确认框干扰用户。

---

## 2. Restore 去重对用户透明

用户重复点击“恢复本集”或多个设备同时恢复同一内容时：

- App 可以收到同一 Request，或不同 Request 共享同一底层 Operation；
- UI 不显示“重复任务”错误；
- 应统一表现为“恢复中”；
- 不向用户暴露 `operation_key`、Placement、Provider Request ID。

如果本季中的某集已经在恢复：

> “恢复本季”应自动复用已有恢复进度。

---

## 3. Delivery Grant 续签

播放器不得把 Delivery URL 当永久媒体 URL 保存。

当发生：

```text
Grant 即将过期
CDN 401 / 403 due to expired grant
App 从后台恢复
长时间 Pause 后继续播放
```

客户端流程：

```text
pause new segment/range scheduling if needed
        ↓
request new Delivery Grant from Ikaros
        ↓
keep current playback position
        ↓
resume with new URL
```

若 Ikaros 返回当前 Permission 已撤销：

- 停止播放；
- 清理内存中的旧 Grant；
- 显示“你已无权访问该内容”，而不是无限刷新 Token。

---

## 4. Grant Revocation UX

不同 Provider 的旧 Grant 可能具有不同撤销能力，但客户端只依赖 Ikaros 当前结果。

客户端不得：

- 假设旧 URL 一定立即失效；
- 在 Permission 被撤销后继续主动重试旧 URL；
- 把旧 Signed URL 写入播放历史、分享链接或持久化日志。

分享能力必须生成 Share Domain 自己的授权，不复用 Playback Delivery Grant。

---

## 5. Playback Lease

播放器获得 `DeliveryGrant.lease_id` 后：

- 播放期间按低频心跳或 Grant 续签时续租；
- Seek / 每个 Range 请求不单独续租；
- 正常播放结束主动 Release；
- App 崩溃或断网时允许 Lease 自然过期。

客户端无需向用户展示“Lease”术语。

其用户价值只是：

> 正在看的内容不会在播放中途被自动重新归档或清理唯一临时恢复副本。

---

## 6. Fallback UX

Delivery Fallback 应尽量透明，但必须区分可恢复网络问题与权限问题。

### 可自动 Fallback

- CDN 临时不可用；
- 当前 Edge/Origin 网络不可达；
- 当前 Provider 不支持客户端要求的 Range。

### 禁止自动 Fallback

- 401/403 Permission Denied；
- Resource 被删除；
- Blob Missing / Corrupted；
- Data Classification Policy Denied。

低带宽 Profile 下，如果 CDN 和 DIRECT 都不可用：

```text
媒体交付服务暂不可用，请稍后重试
```

不要因为存在 Server Proxy 能力就自动切换并造成极低速播放。

---

## 7. Cache Identity 对客户端透明

客户端可以收到每次都不同的短期 URL，但不得通过 URL 字符串判断“是否同一媒体内容”。

内容身份继续使用：

- Attachment ID；
- Blob / Representation 的公开稳定引用；
- ETag / content length 等协议元数据。

禁止把 Token Query 变化误认为媒体版本变化，从而：

- 清空播放缓存；
- 重置播放进度；
- 创建重复 Download Item。

---

## 8. 临时恢复副本即将过期

如果 `READY_TEMPORARILY` 带有效期：

- 正在播放且 Lease 有效时，不需要显示倒计时威胁用户；
- 如果内容未播放且很快过期，可轻量显示“临时恢复可用至 …”；
- 过期后重新执行 Availability Resolution；
- 不直接把过期视为文件丢失。

---

## 9. App Acceptance

1. 重复 Restore 不生成用户可见的重复错误。
2. 超 Budget 的手动 Season Restore 需要明确确认。
3. 自动预热超 Budget 时静默停止。
4. Grant 过期后可在原播放位置续签并继续。
5. Permission 撤销后不无限刷新 Grant。
6. App 不持久化完整 Signed URL。
7. Playback Lease 续租频率低于 Range 请求频率，不造成心跳风暴。
8. low-bandwidth Profile 下 CDN/DIRECT 都失败时不自动 Server Proxy。
9. Token URL 改变不导致播放进度 / Download identity 重置。
10. Restore 临时副本过期重新走 Availability，而不是显示 404。
