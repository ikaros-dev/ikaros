(function () {
  const NAV_GROUPS = [
    { id:'work', label:'工作台', icon:'dashboard', items:[
      ['dashboard','概览','P0'],['search','全局搜索','P0'],['activity','我的活动与收藏','P0']
    ]},
    { id:'content', label:'内容与创作', icon:'library', items:[
      ['library','统一资源库','P0'],['collections','集合、标签与关系','P0'],['documents','文章与文档','P1'],['media','媒体消费','P1'],['sharing','分享与协作','P2']
    ]},
    { id:'storage', label:'附件与存储', icon:'storage', items:[
      ['attachments','附件与 Blob','P0'],['tiers','持久化存储层','P0'],['downloads','缓存与我的下载','P1'],['archive','归档恢复与回收站','P0'],['backup','备份与恢复','P2']
    ]},
    { id:'planning', label:'效率与计划', icon:'tasks', items:[
      ['today','收集箱与今天','P0'],['projects','项目与任务','P0'],['calendar','日历与时间块','P0'],['goals','目标与 OKR','P1'],['focus','习惯、专注与复盘','P1']
    ]},
    { id:'finance', label:'个人记账', icon:'wallet', items:[
      ['finance-overview','账本总览','P0'],['accounts','账户','P0'],['transactions','交易','P0'],['budgets','预算与周期账','P1'],['reconcile','对账与导入','P1']
    ]},
    { id:'private', label:'私密笔记', icon:'lock', items:[
      ['private-vault','保险库','P0'],['private-conflicts','版本与同步冲突','P0'],['private-export','恢复与导出','P1']
    ]},
    { id:'password', label:'密码管理', icon:'key', items:[
      ['password-vault','密码保险库','P0'],['password-generator','生成器与条目','P0'],['password-health','健康与安全发送','P1'],['password-devices','设备与权限','P1']
    ]},
    { id:'ai', label:'AI 智能', icon:'spark', items:[
      ['ai-assistant','助手','P0'],['ai-models','模型与提供方','P0'],['ai-personas','人格管理','P0'],['ai-privacy','上下文、隐私与记忆','P0'],['ai-jobs','AI 作业、Trace 与用量','P0']
    ]},
    { id:'analytics', label:'数据分析', icon:'chart', items:[
      ['analytics-overview','个人概览','P0'],['analytics-content','内容分析','P0'],['analytics-storage','存储分析','P0'],['analytics-planning','效率分析','P0'],['analytics-system','系统历史','P0'],['metrics','指标目录','P0'],['reports','报表与重建','P1']
    ]},
    { id:'integration', label:'集成与自动化', icon:'automation', items:[
      ['automation','自动化规则','P1'],['executions','执行记录与链路','P1'],['events','事件与失败队列','P1'],['sync','导入与同步','P0'],['plugins','插件与连接器','P1']
    ]},
    { id:'identity', label:'身份与安全', icon:'shield', items:[
      ['users','用户与角色','P0'],['roles','权限矩阵','P0'],['sessions','活跃会话','P0'],['security','验证、密钥与恢复','P0']
    ]},
    { id:'platform', label:'平台配置', icon:'settings', items:[
      ['parameters','参数','P0'],['dictionaries','字典','P1'],['menus','菜单','P1']
    ]},
    { id:'comms', label:'沟通与审计', icon:'bell', items:[
      ['announcements','公告','P0'],['notifications','通知中心与投递','P0'],['audit','审计与安全事件','P0']
    ]},
    { id:'ops', label:'系统运维', icon:'pulse', items:[
      ['health','系统健康与告警','P0'],['jobs','定时任务','P0'],['background','后台任务','P0']
    ]}
  ];

  const RESOURCES = [
    {id:'res-001',type:'动画剧集',title:'星海邮差 · 第 08 话',alt:'Starlit Courier — Episode 08',provider:'Bangumi / bgm:demo-802',status:'Available',collection:'2026 夏季观看',relation:'EPISODE_OF → 星海邮差',favorite:true,progress:42,metadata:'manual',conflict:true,updated:'今天 09:24'},
    {id:'res-002',type:'电影',title:'潮汐以北',alt:'North of the Tide',provider:'TMDB / tmdb:demo-170',status:'Cached',collection:'周末电影',relation:'RELATED_TO → 海岸摄影集',favorite:false,progress:0,metadata:'provider',updated:'昨天 21:10'},
    {id:'res-003',type:'音乐',title:'玻璃温室里的雨',alt:'Rain in the Glasshouse',provider:'MusicBrainz / mb:demo-43',status:'Available',collection:'晚间播放列表',relation:'TRACK_OF → 微光专辑',favorite:true,progress:68,metadata:'scanner',updated:'昨天 18:40'},
    {id:'res-004',type:'图片',title:'城市观察：高架桥下',alt:'Urban Study: Underpass',provider:'本地导入 / import:photo-31',status:'Processing',collection:'2026 城市观察',relation:'COVER_OF → 城市步行手记',favorite:false,progress:0,metadata:'manual',updated:'8 月 28 日'},
    {id:'res-005',type:'小说/漫画',title:'纸上岛屿 · 第三卷',alt:'Paper Islands, Vol. 3',provider:'本地扫描 / scan:book-77',status:'Remote',collection:'待读',relation:'VOLUME_OF → 纸上岛屿',favorite:true,progress:31,metadata:'manual',updated:'8 月 27 日'},
    {id:'res-006',type:'文档',title:'Ikaros V2 存储边界评审',alt:'Storage Boundary Review',provider:'人工创建 / doc:284',status:'Available',collection:'Ikaros V2',relation:'TARGET_OF ← 任务 #T-104',favorite:true,progress:0,metadata:'manual',updated:'8 月 26 日'},
    {id:'res-007',type:'游戏资料',title:'远日点：模组与存档索引',alt:'Apoapsis — Mods & Saves',provider:'本地导入 / import:game-09',status:'Restoring',collection:'游戏归档',relation:'RELATED_TO → 远日点',favorite:false,progress:62,metadata:'import',updated:'8 月 25 日'},
    {id:'res-008',type:'文章',title:'为什么缓存不是存储层',alt:'Why Cache Is Not a Storage Tier',provider:'人工创建 / article:56',status:'Available',collection:'技术写作',relation:'DERIVED_FROM → 存储设计',favorite:false,progress:0,metadata:'manual',updated:'8 月 24 日'}
  ];

  const TASKS = [
    {id:'T-104',title:'完成存储边界评审',status:'planned',project:'Ikaros V2',scheduled:'今天 10:00–11:30',deadline:'9 月 2 日 18:00',estimate:90,actual:35,important:true,urgent:true,resource:'res-006'},
    {id:'T-105',title:'观看《星海邮差》第 08 话',status:'planned',project:'2026 夏季观看',scheduled:'今天 20:30–21:00',deadline:'无',estimate:30,actual:0,important:false,urgent:false,resource:'res-001'},
    {id:'T-106',title:'核对 8 月账单导入差异',status:'in-progress',project:'个人财务',scheduled:'今天 16:00–16:45',deadline:'8 月 31 日 22:00',estimate:45,actual:18,important:true,urgent:false,resource:null},
    {id:'T-107',title:'整理图片 OCR 派生附件',status:'inbox',project:'收集箱',scheduled:'未安排',deadline:'无',estimate:25,actual:0,important:false,urgent:true,resource:'res-004'},
    {id:'T-108',title:'更新 Persona 安全场景说明',status:'completed',project:'Ikaros V2',scheduled:'昨天 14:00–15:00',deadline:'8 月 29 日',estimate:60,actual:54,important:true,urgent:true,resource:null}
  ];

  const ACCOUNTS = [
    {id:'A-01',name:'日常借记卡',type:'储蓄账户',masked:'尾号 2841',currency:'CNY',balance:12680.40},
    {id:'A-02',name:'生活钱包',type:'电子钱包',masked:'尾号 7712',currency:'CNY',balance:2384.56},
    {id:'A-03',name:'旅行储备',type:'外币账户',masked:'尾号 0937',currency:'USD',balance:820.00},
    {id:'A-04',name:'信用卡',type:'信用账户',masked:'尾号 4519',currency:'CNY',balance:-1864.20}
  ];

  const TRANSACTIONS = [
    {id:'TX-109',date:'08-30 08:16',type:'EXPENSE',account:'A-02',target:null,category:'餐饮 / 早餐',payee:'街角面包房',amount:28.5,currency:'CNY',source:'手工录入'},
    {id:'TX-108',date:'08-29 21:34',type:'EXPENSE',account:'A-01',target:null,category:'数字内容',payee:'本地书店',amount:86,currency:'CNY',source:'CSV 导入'},
    {id:'TX-107',date:'08-29 18:10',type:'TRANSFER',account:'A-01',target:'A-02',category:'内部转账',payee:'账户间转账',amount:600,currency:'CNY',source:'手工录入'},
    {id:'TX-106',date:'08-28 09:00',type:'INCOME',account:'A-01',target:null,category:'收入 / 项目',payee:'示例合作方',amount:3200,currency:'CNY',source:'手工录入'},
    {id:'TX-105',date:'08-27 14:25',type:'EXPENSE',account:'A-03',target:null,category:'旅行 / 交通',payee:'示例铁路',amount:42,currency:'USD',source:'CSV 导入'}
  ];

  const NOTIFICATIONS = [
    {id:'N-01',source:'STORAGE',title:'归档恢复已完成',body:'“游戏资料：远日点”已恢复到 Warm 层，可以访问。',time:'5 分钟前',read:false,archived:false,priority:'normal'},
    {id:'N-02',source:'SECURITY',title:'新的演示会话',body:'Windows · Edge，最近活动于 10:31。若不是你的操作，请撤销会话。',time:'24 分钟前',read:false,archived:false,priority:'high'},
    {id:'N-03',source:'PRODUCTIVITY',title:'时间块存在冲突',body:'16:00 的账单对账与“团队例会”重叠 15 分钟。',time:'1 小时前',read:true,archived:false,priority:'normal'},
    {id:'N-04',source:'PLUGIN',title:'元数据同步部分成功',body:'已更新 18 个资源，2 个字段因人工锁定而保留。',time:'昨天',read:true,archived:false,priority:'normal'},
    {id:'N-05',source:'BACKUP',title:'备份恢复验证通过',body:'备份集 2026-08-29 可用于演示恢复验证。',time:'昨天',read:true,archived:true,priority:'normal'}
  ];

  const GENERIC_PAGES = {
    search:{title:'全局搜索',subtitle:'关键词检索在无 AI 时仍可用；私密域只显示需要解锁的安全占位。',kind:'search'},
    activity:{title:'我的活动与收藏',subtitle:'业务活动、收藏与消费进度；不与管理审计混合。',facts:[['继续观看','星海邮差 · 第 08 话','42%'],['最近编辑','存储边界评审','今天 09:24'],['最近收藏','纸上岛屿 · 第三卷','待读']],rows:[['观看进度更新','星海邮差 · 第 08 话','今天 09:18'],['完成任务','更新 Persona 安全场景说明','昨天 15:02'],['添加到集合','玻璃温室里的雨 → 晚间播放列表','昨天 18:40']]},
    collections:{title:'集合、标签与关系',subtitle:'用 Collection 组织内容，用有类型的 Relation 保留跨资源语义。',facts:[['Collection','2026 夏季观看','12 个资源'],['动态集合','最近 30 天未完成','8 个资源'],['关系异常','目标已归档','1 条待检查']],rows:[['2026 夏季观看','手动集合 · 更新于今天','12 个资源'],['技术写作','手动集合 · 更新于昨天','6 个资源'],['高架桥观察','智能集合 · type=PHOTO AND tag=城市','18 个资源']]},
    sharing:{title:'分享与协作',subtitle:'可撤销、有时效的共享入口；Share Token 不继承登录用户的其他权限。',facts:[['临时分享','存储边界评审','72 小时后过期'],['协作房间','晚间一起听','2 位成员'],['待处理评论','版本 7 · 第 2 节','3 条']],rows:[['存储边界评审','仅查看 · 禁止下载 · 72 小时','有效'],['城市观察相册','查看与评论 · 12 次访问','有效'],['晚间一起听','Room · 2 位成员 · 同步队列','进行中']]},
    attachments:{title:'附件与 Blob',subtitle:'严格展示 Resource → Attachment → Blob → Placement，并区分原始与派生附件。',kind:'attachments'},
    tiers:{title:'持久化存储层',subtitle:'Hot、Warm、Cold、Archive 是持久化 Placement；服务器缓存与客户端缓存另行展示。',kind:'tiers'},
    downloads:{title:'缓存与我的下载',subtitle:'缓存是可淘汰加速数据；主动下载是用户可管理的离线副本。',kind:'downloads'},
    backup:{title:'备份与恢复',subtitle:'备份必须通过恢复验证；本页只模拟任务编排，不操作真实文件。',kind:'backup'},
    projects:{title:'项目与任务',subtitle:'项目组织 Task；Section 支持 List 与 Kanban 视图，但不复制任务数据。',facts:[['Ikaros V2','进行中','3 个未完成'],['个人财务','进行中','1 个未完成'],['2026 夏季观看','轻量列表','1 个未完成']],rows:[['存储与缓存设计','Design · 3/5 完成','9 月 2 日'],['Persona 管理原型','Review · 4/4 完成','已完成'],['账单导入与对账','In progress · 1/3 完成','8 月 31 日']]},
    goals:{title:'目标与 OKR',subtitle:'目标进度与 KR 信心分开；完成 Task 不会机械等比例推进目标。',kind:'goals'},
    accounts:{title:'账户',subtitle:'普通账本使用正常平台存储；完整卡号、Token 与 PIN 只通过 secret:// 引用。',kind:'accounts'},
    budgets:{title:'预算与周期账',subtitle:'预算、已发生交易与周期规则是三种独立事实。',kind:'budgets'},
    reconcile:{title:'对账与导入',subtitle:'差异通过显式调整处理；本地示例 CSV 先预览、映射与去重，再确认。',kind:'reconcile'},
    'private-conflicts':{title:'版本与同步冲突',subtitle:'密文同步冲突在解锁客户端处理，支持保留本地、远端、两份或手工合并。',kind:'private-conflicts'},
    'private-export':{title:'恢复与导出',subtitle:'加密导出是默认路径；明文导出需要风险提示与演示再验证。',kind:'private-export'},
    'password-health':{title:'健康与安全发送',subtitle:'健康统计不包含 URL、用户名或密码；安全发送属于 P1 演示范围。',kind:'password-health'},
    'password-devices':{title:'设备与权限',subtitle:'Use Secret 与 Reveal Secret 权限分离；撤销无法收回已复制的历史字节。',kind:'password-devices'},
    'ai-models':{title:'模型与提供方',subtitle:'业务使用 Provider-neutral Model Profile，凭据只保存 Secret Reference。',kind:'ai-models'},
    'ai-privacy':{title:'上下文、隐私与记忆',subtitle:'Persona、Memory 与业务状态彼此分离；所有上下文先经过权限与数据分级。',kind:'ai-privacy'},
    'ai-jobs':{title:'AI 作业、Trace 与用量',subtitle:'有限步骤、时长、Token 与预算；Trace 默认不保存完整敏感 Prompt。',kind:'ai-jobs'},
    'analytics-content':{title:'内容分析',subtitle:'口径来自 Metric Catalog；零值与没有数据使用不同展示。',kind:'analytics'},
    'analytics-storage':{title:'存储分析',subtitle:'Logical、Unique、Replica、Cache 与 Download 分开统计。',kind:'analytics'},
    'analytics-planning':{title:'效率分析',subtitle:'只帮助理解计划与实际，不生成“效率人格评分”。',kind:'analytics'},
    'analytics-system':{title:'系统历史',subtitle:'历史聚合与趋势分析；当前实时状态请前往系统运维。',kind:'analytics'},
    metrics:{title:'指标目录',subtitle:'指标先定义口径再查询；版本、单位、时间字段与血缘均可追溯。',kind:'metrics'},
    reports:{title:'报表与重建',subtitle:'长时间报表、重建与校对都进入后台任务，不阻塞业务页面。',kind:'reports'},
    executions:{title:'执行记录与链路',subtitle:'成功、部分成功、失败与跳过；重试失败 Action 不重复已成功动作。',kind:'executions'},
    events:{title:'事件与失败队列',subtitle:'至少一次投递、消费者幂等、循环深度与去重窗口清晰可见。',kind:'events'},
    sync:{title:'导入与同步',subtitle:'一次 Import 不等于持续 Sync；人工锁定元数据不会被静默覆盖。',kind:'sync'},
    plugins:{title:'插件与连接器',subtitle:'插件停用后依赖规则降级并保留，不删除已有资源与规则。',kind:'plugins'},
    roles:{title:'权限矩阵',subtitle:'Platform RBAC 与 Resource ACL 分层；菜单隐藏不是后端授权。',kind:'roles'},
    sessions:{title:'活跃会话',subtitle:'展示最近活动时间而非绝对在线状态；Token 永不展示。',kind:'sessions'},
    security:{title:'验证、密钥与恢复',subtitle:'当前仅 Email OTP / SVL-1 可用；更高等级保留为规划且不创造业务权限。',kind:'security'},
    parameters:{title:'参数',subtitle:'类型化校验，明确动态、重载或重启生效；环境覆盖值只读锁定。',kind:'parameters'},
    dictionaries:{title:'字典',subtitle:'系统关键项受保护；被业务引用的字典项不能静默删除。',kind:'dictionaries'},
    menus:{title:'菜单',subtitle:'菜单树只控制导航与可见性预览，不承担授权。',kind:'menus'},
    announcements:{title:'公告',subtitle:'草稿、定时、发布与撤回是内容生命周期；通知投递是独立流程。',kind:'announcements'},
    audit:{title:'审计与安全事件',subtitle:'只读、脱敏、可筛选；不提供编辑审计记录的入口。',kind:'audit'},
    jobs:{title:'定时任务',subtitle:'调度定义触发 Background Task，和用户待办保持独立。',kind:'jobs'},
    background:{title:'后台任务',subtitle:'等待、进行、成功、失败与取消均有终态，不使用无限旋转等待。',kind:'background'},
    prototype:{title:'原型说明与阶段范围',subtitle:'评审用交互原型；展示产品语义，不宣称已实现生产后端能力。',kind:'prototype'}
  };

  Object.assign(window, { NAV_GROUPS, RESOURCES, TASKS, ACCOUNTS, TRANSACTIONS, NOTIFICATIONS, GENERIC_PAGES });
})();

