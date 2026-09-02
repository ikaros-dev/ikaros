# Ikaros V2 App 媒体归档恢复交互设计

| 项目 | 内容 |
|---|---|
| 文档名称 | Media Archive Restore Interaction Design |
| 适用端 | Web / Flutter App |
| 适用版本 | Ikaros V2 |
| 编写日期 | 2026-09-02 |
| 状态 | Draft |
| 依赖设计 | `../Media-Delivery-CDN-Archive-Restore-Design.md` |

> 本文档定义归档媒体在客户端的可见状态、恢复动作、进度反馈与播放行为。客户端不得直接理解具体云厂商的 Archive / Deep Archive 产品名。

---

## 1. 目标

归档能力必须让用户明确理解：

- 内容仍然存在；
- 当前为什么不能立即播放；
- 是否正在恢复；
- 大致需要等待多久；
- 可以恢复单集还是整季；
- 恢复完成后能否直接播放；
- 临时恢复副本是否会过期。

禁止把归档状态表现成：

- 文件丢失；
- 网络错误；
- 播放器故障；
- 无限 Loading。

---

## 2. Availability → UI 映射

| MediaAvailability | 详情页 | 播放按钮 |
|---|---|---|
| `READY` | 正常 | 播放 |
| `RESTORE_REQUIRED` | 显示“已归档”与预计等待范围 | 恢复并播放 |
| `RESTORING` | 显示恢复中、进度/状态 | 查看恢复状态 |
| `READY_TEMPORARILY` | 显示“已恢复”，可选显示有效期 | 播放 |
| `UNAVAILABLE` | 显示暂不可用原因 | 重试 / 查看详情 |
| `MISSING` | 显示文件缺失 | 不可播放 |
| `CORRUPTED` | 显示文件损坏 | 不可播放 |

“预计等待范围”只能显示 Provider 能提供的区间，例如“通常在 12–24 小时内可用”，不得伪造精确倒计时。

---

## 3. Episode 详情交互

当 Episode 为 `RESTORE_REQUIRED`：

```text
[ 已归档 ]
该视频当前位于归档存储，需要恢复后才能播放。
预计可用时间：Provider 提供的范围

[恢复本集]  [恢复本季]
```

点击“恢复本集”：

1. 创建 Restore Request；
2. 立即进入 `RESTORING`；
3. 返回详情页，不阻塞 HTTP 请求等待恢复完成；
4. 后台任务状态通过轮询 / SSE / WebSocket 更新；
5. 成功后状态变为 `READY_TEMPORARILY` 或 `READY`。

---

## 4. 恢复本季

Season 详情页允许：

```text
[恢复本季]
```

确认面板至少展示：

- 待恢复 Episode 数；
- 总数据量（可得时）；
- 预计恢复时延范围；
- 恢复后临时可用时长（可得时）；
- 是否可能产生 Provider 取回费用。

费用只做“可能产生费用”的能力级提示；除非系统已经拥有可靠的 Provider Cost Model，否则不得展示伪精确金额。

恢复本季任务允许：

- 部分完成；
- 单集失败；
- 重试失败项；
- 取消尚未提交到 Provider 的剩余任务。

---

## 5. 顺序预热

当用户恢复或开始播放 Episode N 时，客户端可以展示轻量提示：

> 已根据你的播放顺序预热后续剧集。

该提示不需要每次出现。

自动预热属于策略，不提供无限制“全库恢复”。用户显式选择“恢复本季”时，业务意图优先。

---

## 6. 播放器行为

播放器只有在 Availability 为 `READY` / `READY_TEMPORARILY` 且获得有效 Delivery Grant 后才启动媒体请求。

必须处理：

- Delivery Grant 过期：静默重新获取 Grant，再从当前播放位置续播；
- CDN 401 / 403：先刷新 Grant，不立即判定媒体丢失；
- Range 不支持：显示明确能力错误，不循环 Seek；
- Restore 中：播放器不保持一个等待 12–24 小时的长连接；
- 临时恢复副本过期：重新进入 Availability Resolution。

---

## 7. 下载与离线

下载归档媒体时：

- 若 `RESTORE_REQUIRED`，先创建 Restore；
- Restore 完成后再进入正常 Download Manager；
- 客户端本地下载成功后，不因服务端临时 Restore 副本过期而删除本地文件；
- 本地下载状态仍属于 Offline / Device Sync 专项，不回写成 Storage Placement。

---

## 8. 通知

长耗时 Restore 建议支持通知：

- 恢复完成；
- 恢复失败；
- 本季恢复部分失败。

通知文案应指向具体媒体对象，不暴露 Bucket、Object Key、Provider Credential 等存储内部信息。

---

## 9. 批量状态展示

Season Episode List 建议展示：

```text
EP01  可播放
EP02  已恢复
EP03  恢复中
EP04  已归档
EP05  已归档
```

图标 / 颜色不能成为唯一状态表达方式，需要有文本或无障碍语义。

---

## 10. 验收场景

1. 已归档 Episode 不显示普通“播放失败”。
2. 用户可以恢复单集，也可以恢复本季。
3. Restore 请求提交后页面立即返回可操作状态。
4. App 重启后仍能重新查询 Restore Task。
5. Restore 完成后原 Episode / Attachment 身份不变化。
6. Delivery URL 过期可以续签并继续播放。
7. 自动预热不会触发无限量恢复。
8. Provider 不给精确 ETA 时，UI 不显示虚假的分钟级倒计时。
