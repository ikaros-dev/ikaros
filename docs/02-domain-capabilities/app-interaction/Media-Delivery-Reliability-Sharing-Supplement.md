# Ikaros V2 App Media Delivery Reliability / Sharing 交互补充

| 项目 | 内容 |
|---|---|
| 适用端 | Web / Flutter App |
| 适用版本 | Ikaros V2 |
| 状态 | Draft |
| 依赖 | `../Media-Delivery-Restore-Reliability-Addendum.md`、`../Media-Delivery-Representation-Sharing-Budget-P1-Design.md` |

---

## 1. Delivery Failover UX

正常播放时客户端不展示底层 CDN / DIRECT Provider 名称。

当服务端返回新的 `selection_epoch`：

1. 保留当前 playback position；
2. 停止为旧 Epoch 申请新 Grant；
3. 获取新 Grant；
4. 使用 Range 从当前 offset 恢复；
5. 成功时静默继续播放；
6. 只有切换失败或内容身份不一致时才提示用户。

禁止客户端自行按 URL 失败顺序猜测 Provider 并绕过服务端策略。

---

## 2. 内容身份不一致

Failover 后如果服务端发现 Representation Identity / Content Length / ETag 等关键内容身份不一致：

- 不允许直接从相同 byte offset 续播；
- 重新执行 Playback Source Resolution；
- 若换成新的 Representation，使用时间轴位置而不是旧字节 offset 恢复；
- 必要时显示“播放源已切换”。

---

## 3. Restore Reconciliation UX

当本地任务状态与 Provider 状态暂时不一致时，客户端应使用业务状态：

```text
RESTORING
VERIFYING_AVAILABILITY
READY
RESTORE_REQUIRED
```

`VERIFYING_AVAILABILITY` 表示服务端正在对账，不等于 Restore 失败。

如果 Server 重启后 Reconcile 发现对象其实已经恢复成功，客户端应直接进入 READY，不展示虚假“重新开始恢复”。

---

## 4. Source Archived / Derived Ready

如果源 MKV 已归档但当前 Derived Representation 可播放：

- 正常播放 Derived；
- 不因为 Source 为 ARCHIVE 显示“视频已归档”；
- 只有当前没有任何可播放 Representation 且需要恢复 Source 才显示恢复流程。

如果需要恢复 Source 并重新生成 Variant：

```text
恢复源文件
  ↓
准备播放版本
  ↓
可播放
```

客户端应区分 `RESTORING_SOURCE` 与 `PREPARING_VARIANT`。

---

## 5. Share / Guest 播放

Share Link 不直接打开永久媒体 URL。

流程：

```text
Open Share
  ↓
validate token / establish Guest Principal
  ↓
render shared resource
  ↓
request Delivery Grant when playing
```

UI 必须根据 Share Capability 分开显示：

- 可在线播放；
- 可下载；
- 可加入一起看；
- 可控制播放。

仅有 stream/read 权限时不展示“下载原文件”。

Share 过期或撤销后：

- 新播放请求拒绝；
- 当前播放按 Grant Revocation Contract 收敛；
- 不无限自动续签。

---

## 6. Traffic Budget UX

CDN / Origin Soft Budget 达到阈值时默认不打断正在播放。

可以在管理型用户界面或通知中提示：

- 本月媒体流量接近预算；
- Origin 回源异常升高；
- Server Proxy 已达到保护阈值。

普通观看用户不需要看到复杂云费用术语。

如果 Server Proxy Hard Guard 阻止播放：

> 当前可用的远程媒体交付路径不可用，服务器已阻止低带宽代理回退。

不要伪装成普通网络错误。

---

## 7. Immutable Representation

客户端必须把 `representation_id + representation_version` 当作缓存代际。

新版本发布后：

- 不覆盖旧版本本地缓存内容；
- 新播放会话使用新版本；
- 已经在旧版本播放的会话可以在 Lease 有效期内继续；
- 客户端不主动请求 CDN Purge。

---

## 8. 验收

1. Provider Failover 后能从当前播放位置继续。
2. 单次瞬时 Provider 错误不会造成 UI 反复 Loading。
3. Reconciliation 中不显示错误的永久失败。
4. Source Archive + Derived Ready 时正常播放。
5. Guest stream-only Share 不显示下载入口。
6. Share 撤销后不会无限刷新 Grant。
7. Soft Traffic Budget 不打断活跃播放。
8. Server Proxy Hard Guard 有明确错误语义。
9. Representation Version 改变后不会使用错误的旧缓存内容。
