# Foundation：App Shell、导航与响应式布局

## 1. 页面与组件范围

Foundation 不是业务菜单项，而是所有页面共同使用的 App Shell。

包含：

- 启动页 / Bootstrap。
- 响应式主壳层。
- 分组导航。
- 全局 App Bar。
- 全局搜索快捷入口。
- Deep Link 路由。
- Offline / Sync Banner。
- 全局 Mini Player。
- 全局安全锁定遮罩。
- 键盘、鼠标、触摸基础交互。

---

## 2. 启动页 Bootstrap

### 2.1 页面布局

全屏 Surface，中心纵向排列：

1. Ikaros Logo，72dp。
2. `Ikaros` 产品名。
3. 当前阶段文本，例如“正在连接家庭服务器”。
4. 220–320dp 宽线性进度条。
5. 底部版本号与“诊断”文本按钮，仅初始化超过合理时间或失败时显示。

### 2.2 初始化顺序

1. 加载本地 App Settings。
2. 加载最近使用的 Server Profile。
3. 加载本地认证状态。
4. 加载离线数据库与下载索引。
5. 如果可联网，执行服务器 Capability Discovery。
6. 同步用户 Profile、菜单可用能力与未读通知数量。
7. 若存在 Secure Domain，仅加载 Locked 元数据，不自动解密。
8. 进入主壳层。

### 2.3 状态

- 无 Server Profile：转“添加服务器”。
- Token 失效且在线：转登录页。
- Token 失效但离线：允许进入离线模式，只展示本地可用能力。
- 服务端版本不兼容：显示兼容性说明、服务器地址、客户端版本与升级动作。
- Server 暂不可达：提供“离线进入”“重试”“切换服务器”。

---

## 3. Compact 主壳层 `<600dp`

### 3.1 页面结构

```text
┌────────────────────────┐
│ App Bar                │
├────────────────────────┤
│                        │
│ Current Page           │
│                        │
├────────────────────────┤
│ Mini Player (optional) │
├────────────────────────┤
│ Bottom Navigation      │
└────────────────────────┘
```

### 3.2 Bottom Navigation

固定 5 项：

- 首页
- 资源库
- Today
- AI
- 我的

每个 Destination：图标 + 标签。未读通知不占独立 Destination，通过“我的”或 App Bar Bell 显示徽标。

### 3.3 Drawer

App Bar 左侧菜单按钮打开全高 `NavigationDrawer`。

Drawer 从上到下：

1. 当前服务器卡片：服务器名称、URL Host、在线/离线圆点。
2. 当前用户卡片：头像、Display Name。
3. “切换服务器”小按钮。
4. 可滚动分组菜单。
5. 底部：下载、设置、关于。

所有菜单分组默认收起。

分组展开动画 180–240ms；展开后子项缩进 24dp。

---

## 4. Medium 主壳层 `600–839dp`

左侧 `NavigationRail` 72–80dp：

- 顶部 Menu。
- 首页。
- 资源库。
- Today。
- AI。
- 我的。

底部可放当前头像。

点击 Menu 打开 320dp 宽 Drawer。内容区允许使用双栏，例如 Resource List + Preview Pane。

---

## 5. Expanded / Large 主壳层

### 5.1 永久侧栏

宽 280dp，Large 可 300dp。

顶部：

- Logo + Ikaros。
- 当前服务器切换器。
- 在线状态。

中部：分组菜单。

底部：

- Download 状态按钮。
- 用户头像、名称。
- Settings 图标。

### 5.2 菜单分组组件

每一行高度 48dp：

- 左：组图标 24dp。
- 中：组名。
- 可选：徽标，例如“3”。
- 右：Chevron。

点击整行切换展开。

子项高度 44dp：

- 左侧 24dp 缩进。
- 当前项使用 Secondary Container 背景。
- Hover 显示 State Layer。
- Focus 可用键盘上下导航。

### 5.3 默认收起语义

- 新安装：全部收起。
- 用户主动展开状态可本地记忆。
- 若用户启用“每次启动收起菜单”，每次重启恢复全部收起。
- Deep Link 可临时展开当前组，但不覆盖用户持久化偏好。

---

## 6. 内容容器

Expanded / Large：

- 页面内容左右 Padding 24–32dp。
- 主内容最大宽度建议 1440dp。
- 阅读/编辑等沉浸页可突破最大宽度规则。
- 表单主体建议 720–960dp，避免在 4K 屏上拉得过宽。

支持三种页面框架：

### A. List Page

App Bar + Filter + Content List/Grid。

### B. Master Detail

左 360–480dp List，右侧 Detail。

### C. Immersive

播放器、阅读器、编辑器使用全内容区域，并降低 Shell 导航视觉权重。

---

## 7. 全局 Mini Player

音频播放时可出现。

### Compact

位于 Bottom Navigation 上方，高 64dp：

- 48dp 封面。
- 标题一行。
- 副标题一行。
- Play/Pause。
- 下一首。
- 底部 2dp 进度条。

Tap 整体进入 Now Playing。

### Desktop

放在侧栏底部或内容底部浮条，宽度不挤压主内容。支持 Hover 显示更多控制。

视频默认不使用持续 Mini Player；若未来支持 PiP，按系统能力单独实现。

---

## 8. 全局 Offline / Sync Banner

### 8.1 Offline

顶部紧贴 App Bar 下方，使用 Surface Container：

- 离线图标。
- “当前离线，正在使用本地内容”。
- `查看待同步 4 项`。
- 关闭图标只隐藏本次提示，不改变离线状态。

### 8.2 Sync Error

显示错误数量：

- “3 项变更同步失败”。
- `查看`。
- 不以红色全屏阻断正常离线读取。

---

## 9. 全局安全锁定

当 App 进入后台，若用户启用“离开 App 自动锁定 Secure Domain”：

- Private Notes 与 Password Manager 的解密 Widget 必须销毁。
- 最近任务预览隐藏敏感内容。
- 返回 App 时可在当前页面上覆盖 Secure Lock Surface。
- 普通 Library 内容不受影响。

锁定不是退出登录。

---

## 10. Deep Link

至少规划：

- Resource Detail。
- Task / Project。
- Notification Target。
- Share。
- Room Invite。
- AI Conversation。
- Private Note（先进入解锁）。
- Vault Item（先进入解锁）。

Deep Link 流程：

1. 解析目标。
2. 校验当前 Server Profile。
3. 校验登录 / 权限。
4. 校验 Secure Unlock（如需要）。
5. 跳转目标。
6. 失败时显示可理解的错误页，不静默回首页。

---

## 11. 键盘与鼠标

Desktop 必须支持：

- `Ctrl/Cmd + K`：全局搜索。
- `Ctrl/Cmd + N`：在当前支持的业务页新增对象。
- `Esc`：关闭 Menu / Dialog / Bottom Sheet；沉浸页优先退出控制层。
- `Alt + Left`：返回。
- `Ctrl/Cmd + ,`：设置。
- `Space`：播放器页面播放/暂停；表单输入焦点时不拦截。
- Tab Focus Order 与视觉顺序一致。

右键用于非破坏性上下文菜单；破坏性操作仍需要确认。

---

## 12. Touch

- 主要触控目标最小 48×48dp。
- 长按只打开 Action Sheet，不直接删除、分享或 Reveal Secret。
- 横向 Swipe 只用于明确可逆动作，例如 Task 完成 / 稍后；必须支持 Undo。
- 阅读器与播放器可以使用自定义手势，但不能与系统返回手势冲突。

---

## 13. 可访问性

- 所有图标按钮提供 Tooltip / Semantic Label。
- 颜色不是唯一状态表达方式。
- 支持系统字体缩放，关键按钮不可因 200% 文本而消失。
- 动效尊重 Reduce Motion。
- 播放器、阅读器提供键盘可访问控制。
- 图表提供文本摘要与数值列表入口。
