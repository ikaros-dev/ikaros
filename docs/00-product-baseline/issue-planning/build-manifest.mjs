import fs from 'node:fs';
import path from 'node:path';
const root = process.cwd();
const dir = path.join(root, 'docs/00-product-baseline/issue-planning');
const config = JSON.parse(fs.readFileSync(path.join(dir, 'github-config.json'), 'utf8').replace(/^\uFEFF/, ''));
const commit = config.repository.defaultBranchRef.target.oid;
const groups = fs.readFileSync(path.join(dir, 'approved-plan.txt'), 'utf8').trim().split(/\r?\n/).map(line => {
  const [key, domain, title, items] = line.split('|');
  return {key, domain, title, items: items.split('；')};
});
function files(p) {
  if (!fs.existsSync(p)) return [];
  return fs.readdirSync(p, {withFileTypes:true}).flatMap(e => e.isDirectory() ? files(path.join(p,e.name)) : [path.join(p,e.name)]);
}
const docs = files(path.join(root, 'docs')).filter(p => p.endsWith('.md') && !p.includes('issue-planning'));
const docNames = {
 platform:'Technical-Architecture-Design', operations:'Platform-Administration-Operations-Subsystem-Design',
 event:'P0-Event-Payload-Schema-Registry', task:'Background-Task-Scheduler-Design',
 identity:'Security-Identity-Authorization-Crypto-Subsystem-Design', security:'Security-Identity-Authorization-Crypto-Subsystem-Design', verification:'Security-Identity-Authorization-Crypto-Subsystem-Design', audit:'Security-Identity-Authorization-Crypto-Subsystem-Design',
 resource:'Core-Resource-Library-Subsystem-Design', metadata:'Content-Ingestion-Metadata-Synchronization-Subsystem-Design', collection:'Core-Resource-Library-Subsystem-Design', relation:'Core-Resource-Library-Subsystem-Design', progress:'Core-Resource-Library-Subsystem-Design',
 storage:'Attachment-Blob-Storage-Subsystem-Design', search:'Search-Discovery-Subsystem-Design', ingestion:'Content-Ingestion-Metadata-Synchronization-Subsystem-Design', plugin:'Plugin-Runtime-SDK-Lifecycle-Design',
 media:'Media-Video-Anime-Playback-Subsystem-Design', reading:'Reading-Comic-Novel-Ebook-Subsystem-Design', music:'Music-Library-Playback-Queue-Subsystem-Design', photo:'Photo-Album-Image-Asset-Subsystem-Design', document:'Content-Creation-Revision-Collaborative-Document-Subsystem-Design', game:'Game-Digital-Asset-Archive-Subsystem-Design',
 drive:'Personal-Drive-File-Synchronization-Subsystem-Design', offline:'Offline-Cache-Device-Synchronization-Subsystem-Design', client:'app-shell-navigation-responsive', sharing:'Sharing-Collaboration-Room-Subsystem-Design', planning:'Productivity-Planning-Subsystem-Design', notes:'Private-Notes-Subsystem-Design', password:'Password-Manager-Subsystem-Design', finance:'Personal-Finance-Accounting-Subsystem-Design', automation:'Platform-Integration-Automation-Design', ai:'AI-Intelligence-Subsystem-Design', analytics:'Data-Analytics-Statistics-Subsystem-Design', integration:'Platform-Integration-Automation-Design', backup:'Backup-Restore-Data-Portability-Subsystem-Design', migration:'Backup-Restore-Data-Portability-Subsystem-Design', performance:'Technical-Architecture-Design', release:'Implementation-Roadmap-and-Dependency-Graph'
};
const domainRules = {
 platform:'现有 Maven 单模块与设计中的模块隔离/迁移路线必须明确取舍；不得在无决策的情况下更换构建系统或推测公开路由。',
 operations:'异常不能显示为健康或成功；诊断输出不得泄露凭据；启动和数据库升级失败必须有明确的就绪状态。',
 event:'业务真相与 Outbox 保持原子性；至少一次投递需要消费者幂等；保留 correlation/causation 信息。',
 task:'任务与执行尝试分离；租约失效不能导致并发重复副作用；取消、重试和完成的状态迁移必须可解释。',
 identity:'会话归属和撤销由服务端验证；公开输出不得包含密码、令牌或可复用认证材料。',
 security:'授权在业务入口执行，覆盖直接 API 调用；权限撤销后不能继续访问私有对象。',
 verification:'验证绑定主体、用途和有效期，不能跨操作重放；验证码和验证凭据不得写入日志。',
 audit:'审计与用户 Activity 分离；保留主体、目标、时间、结果和关联信息，敏感字段脱敏。',
 resource:'Resource 身份稳定；乐观并发失败不能覆盖新值；逻辑生命周期操作不得隐式删除共享 Blob。',
 metadata:'外部身份保持唯一且来源可追溯；持续同步不得覆盖用户手动修改；冲突需要显式结果。',
 collection:'Collection 层级不得形成循环；解除组织关系不得删除被引用的 Resource。',
 relation:'关系类型、方向和双方资源授权均需校验；拒绝重复及违反约束的关系。',
 progress:'收藏、评分和进度按用户隔离；更新后能重新查询到同一状态，活动记录与审计分离。',
 storage:'Attachment、Blob、Placement 保持身份分离；内容访问必须授权；失败恢复与清理不能破坏活动引用。',
 search:'结果按实时授权过滤；投影可重建且不取代业务真相；来源版本过期不能覆盖新投影。',
 ingestion:'扫描、导入和持续同步边界明确；重试不重复创建资源；逐项失败可追踪。',
 plugin:'插件只获得声明并授予的能力；生命周期和兼容性明确；禁止跨域私有持久化访问。',
 media:'播放附件和 Resource 生命周期分离；保存用户进度；字幕、音轨和归档状态不能绕过访问控制。',
 reading:'目录/章节/页序稳定可寻址；阅读位置按用户保存；解析失败不得产生伪成功内容。',
 music:'歌曲身份与文件存储分离；队列顺序和当前曲目一致；播放列表修改不删除歌曲原件。',
 photo:'缩略图是可重建的派生内容；相册变更不得删除原图；原图访问仍需授权。',
 document:'保存、Revision 和发布状态分离；编辑冲突不得静默覆盖；恢复历史产生可追溯结果。',
 game:'资料包明确关联游戏及版本；版本变更不得错配下载附件；下载由统一授权访问链路提供。',
 drive:'文件身份不随路径变化；Revision、回收站和删除传播分离；同步冲突不得静默丢失任一方内容。',
 offline:'自动缓存与用户明确下载分离；清理缓存不能删除保留下载；离线可用状态反映实际内容完整性。',
 client:'权限由服务端保证；客户端展示加载、失败、断网和重连状态；跨端操作不得丢失用户进度或文件。',
 sharing:'分享和房间不能扩大底层资源授权；过期、撤销及成员变化必须在后续操作生效。',
 planning:'任务、项目与日程身份和归属明确；提醒修改或取消后不能继续执行过期安排。',
 notes:'保险库锁定后拒绝读取；私密正文及附件受加密边界保护；导出和恢复需要明确身份验证与密钥策略。',
 password:'凭据不进入普通日志/索引/分析；锁定与解锁边界明确；历史和导出与当前凭据同等保护。',
 finance:'账本授权按对象校验；金额与币种精确处理；转账保持两侧原子一致且不重复计入收支。',
 automation:'规则动作使用明确授权；同一触发的重试不重复产生副作用；不得形成无界循环执行。',
 ai:'只读取已授权数据；来源、模型和结果可追溯；权限撤销和预算限制在执行前再次检查。',
 analytics:'指标口径可核对；重复事件不重复计数；统计可从真相重建，不采集未授权私密数据。',
 integration:'外部访问仍遵守对象权限与能力声明；发布失败和重试可追踪，不发送凭据或未授权内容。',
 backup:'备份范围和密钥依赖显式记录；完整性可验证；恢复失败可定位且不能伪报全部成功。',
 migration:'迁移预检、映射和恢复策略先确认；不得静默丢弃源数据；逐项结果可核对与重试。',
 performance:'使用可重复的数据集和测量条件；大对象访问保持有界内存；禁止以禁用授权换取性能。',
 release:'发布证据覆盖安装、升级和恢复；失败检查不能标记通过；发布决策需要维护者验收。'
};
const base = {
 A01:[],A02:['A01-01'],A03:['A02-02'],A04:['A02-02'],A05:['A02-02'],A06:['A05-02'],A07:['A05-02'],A08:['A05-02','A03-01'],A09:['A05-02','A06-01','A03-01'],A10:['A09-01'],A11:['A09-01'],A12:['A09-01'],A13:['A09-01','A05-02'],A14:['A15-01','A09-01'],A15:['A05-01'],A16:['A15-01','A14-03'],A17:['A15-01','A04-01'],A18:['A14-03','A04-01'],A19:['A09-01'],A20:['A04-01','A14-03'],A21:['A10-06','A20-03'],A22:['A01-02','A06-01'],A23:['A04-01','A03-01'],A24:['A02-02'],
 B01:['A09-01','A14-03'],B02:['B01-03','A16-04'],B03:['A14-03','A04-01'],B04:['B03-02'],B05:['A14-03','A04-01'],B06:['B05-02'],B07:['A14-03'],B08:['B07-01','A16-04'],B09:['B07-01'],B10:['A14-03'],B11:['B10-01'],B12:['A09-01'],B13:['B12-02'],B14:['A09-01','A14-03'],B15:['A05-02','A14-03'],B16:['B15-01'],B17:['B15-01','A14-03'],B18:['B17-04'],B19:['A16-05'],B20:['B19-04'],B21:['A05-02','A09-02'],B22:['B17-04','B10-01'],
 C01:['A06-03','A16-06'],C02:['A05-02','A06-01'],C03:['C02-03','B02-01'],C04:['C02-03','B08-02'],C05:['B12-04','B13-01','C02-03'],C06:['A09-01','A06-03'],C07:['A05-02'],C08:['C07-01'],C09:['A07-02'],C10:['C09-02'],C11:['A07-02'],C12:['C11-03'],C13:['A05-02','A06-01'],C14:['C13-04'],C15:['A03-03','A04-01'],C16:['C15-03'],C17:['A07-02'],C18:['C17-02','C19-01','C19-03','A04-01'],C19:['A06-03'],C20:['C17-02','C19-01'],C21:['A09-01','A13-04'],C22:['C21-01','A03-03'],C23:['A22-03','A16-06'],
 R01:['A04-01','A15-01'],R02:['R01-04'],R03:['A09-02','A16-05'],R04:['A01-01','R02-04'],R05:['A09-02','A19-01','A16-05'],R06:['A02-03','R02-04','R04-06','R05-02','R05-03','R05-04','R05-05']
};
const extra = {
 'A01-04':['A01-02','A01-03'], 'A03-03':['A03-02'], 'A04-04':['A04-02'], 'A04-05':['A04-04'],
 'A06-03':['A09-01'], 'A06-04':['A09-01'], 'A06-05':['A06-02'], 'A07-02':['A07-05'],
 'A09-06':['A09-05'], 'A09-07':['A09-05'], 'A10-04':['A10-03'], 'A11-05':['A11-04'],
 'A14-03':['A14-02'], 'A14-04':['A14-03'], 'A14-05':['A14-03'],
 'A16-03':['A16-02'], 'A16-04':['A16-03'], 'A16-05':['A16-03'], 'A16-06':['A06-03','A16-04'],
 'A17-03':['A17-02'], 'A17-04':['A17-03'], 'A17-06':['A17-03'], 'A18-03':['A18-02'], 'A18-06':['A18-04','A18-05','A08-01'],
 'A19-03':['A06-03'], 'A19-04':['A03-03'], 'A19-05':['A19-04'], 'A20-03':['A20-02'], 'A20-05':['A20-03'], 'A20-06':['A20-03'],
 'A21-05':['A21-04'], 'A22-03':['A22-02'], 'A22-04':['A22-03'], 'A22-05':['A22-03'], 'A22-06':['A22-04'],
 'B02-02':['A13-04'], 'B02-03':['B02-02'], 'B02-04':['B01-04'], 'B02-06':['A17-06'],
 'B03-05':['B03-04'], 'B04-04':['B04-03'], 'B06-05':['B06-04'], 'B08-03':['B08-02'], 'B09-04':['B09-02','B08-01'],
 'B10-05':['B10-04'], 'B11-04':['B11-02'], 'B12-03':['A14-03'], 'B13-02':['B13-01'], 'B13-05':['B13-04'],
 'B14-03':['B14-02'], 'B14-05':['B14-03','A16-05'], 'B16-03':['B16-02'], 'B16-05':['B16-04'], 'B16-06':['B16-05'],
 'B17-03':['B17-02'], 'B17-04':['B17-03'], 'B17-05':['B17-03'], 'B18-02':['B18-01'], 'B18-03':['B18-02'], 'B18-04':['B18-03'], 'B18-05':['B16-04'],
 'B19-02':['A16-05'], 'B20-03':['B20-02'], 'B20-05':['B20-04'], 'B20-06':['B20-05'], 'B21-02':['A14-03'], 'B21-04':['A13-04'],
 'B22-03':['B22-02'], 'B22-04':['B22-02'], 'C01-06':['C01-05'], 'C02-03':['C02-02'],
 'C03-05':['C03-02','C03-03'], 'C04-05':['C04-03'], 'C05-03':['C05-01'], 'C05-05':['B13-01'],
 'C06-05':['A23-01'], 'C08-02':['C07-01'], 'C08-05':['C08-04','A23-01'], 'C08-06':['C08-05'],
 'C09-03':['C09-02'], 'C09-04':['C09-02'], 'C10-03':['A14-03'], 'C11-03':['C11-02'], 'C11-04':['C11-03'], 'C11-05':['C11-03'],
 'C13-04':['C13-03'], 'C14-05':['C14-01','C14-02'], 'C14-06':['C14-05'], 'C15-03':['C15-02'], 'C16-02':['C16-01'],
 'C18-03':['A04-03'], 'C18-05':['C18-04'], 'C19-04':['C19-03'], 'C20-03':['C20-02','C20-04'],
 'C22-04':['C22-01','C22-02'], 'C23-05':['C23-04'], 'C23-06':['C23-05'], 'R01-01':['R01-02'], 'R01-04':['R01-01'], 'R01-06':['R01-04'],
 'R02-03':['R02-02'], 'R02-04':['R02-03'], 'R02-06':['R02-05'], 'R03-03':['R03-02'], 'R03-05':['A20-03','R03-03'],
 'R04-03':['R04-02'], 'R04-04':['R04-03'], 'R04-05':['R04-03'], 'R04-06':['R04-04','R04-05'], 'R06-06':['R06-01','R06-02','R06-03','R06-04','R06-05']
};
const independentFirst = new Set(['A01','A13','A24','R01']);
const hitl = new Set(['A01-01','A01-02','B18-03','B18-05','C05-01','C09-01','C09-05','C11-01','C20-01','C20-04','C20-06','R04-01','R06-06']);
const requirements = {resource:'FR-LIB',metadata:'FR-LIB-05～08 / FR-SYNC',collection:'FR-LIB-03',relation:'FR-LIB-04',progress:'FR-STATE',storage:'FR-STORAGE',identity:'FR-AUTH',security:'FR-AUTH',verification:'FR-AUTH',audit:'FR-AUDIT',search:'FR-SEARCH',ingestion:'FR-PORT / FR-SYNC',plugin:'FR-PLUGIN / FR-API',task:'FR-TASK',media:'FR-VIDEO',reading:'FR-READ',music:'FR-MUSIC',photo:'FR-PHOTO',document:'FR-DOC / FR-PUBLISH',game:'FR-GAME',drive:'FR-DRIVE',offline:'FR-CLIENT / FR-STORAGE',client:'FR-CLIENT / UX',sharing:'FR-SHARE / FR-COLLAB',backup:'FR-BACKUP / FR-PORT',migration:'FR-PORT-02',performance:'NFR-03 / NFR-04',release:'NFR / V2 产品验收原则'};
const refLink = p => `[${path.basename(p,'.md')}](https://github.com/ikaros-dev/ikaros/blob/${commit}/${path.relative(root,p).replaceAll('\\','/')})`;
function evidence(domain) {
 const d = domain==='platform' ? 'common' : domain;
 const java = files(path.join(root,'src/main/java/run/ikaros',d)).filter(p=>p.endsWith('.java'));
 const tests = files(path.join(root,'src/test/java/run/ikaros',d)).filter(p=>p.endsWith('.java'));
 return java.length ? `基于 ${commit.slice(0,8)} 的目录盘点，该领域已有 ${java.length} 个 Java 源文件、${tests.length} 个测试文件。本任务是现有能力的差距补齐与集成验收；文件存在不代表该行为已通过验收。复用既有实现，只修改可复现的缺口；若行为已满足，提交执行证据即可关闭，不要求重写。` : `基于 ${commit.slice(0,8)} 的目录盘点，未发现同名独立服务端包；能力可能位于其他模块或客户端。实施时先定位复用点，依据验收证据判断新增还是补齐，不能由目录缺失直接推断功能不存在。`;
}
function checks(title, domain) {
 let edge;
 if (/恢复|重试|重连|续播|续读|中断/.test(title)) edge='在中断或失败后重复执行，不丢失已提交状态，不重复产生业务副作用，并展示恢复成功或具体失败原因。';
 else if (/删除|清理|移除|取消|撤销|禁用|停用|锁定|归档/.test(title)) edge='分别验证允许和被禁止的状态；操作后重新访问目标确认状态生效，并验证其他用户、共享引用和未选中对象不受误影响。';
 else if (/查询|查看|浏览|展示|搜索|筛选|列表|分页/.test(title)) edge='验证正常、空结果、目标不存在及无权访问；列表分页无重复遗漏，失败或过期状态不得显示为成功。';
 else if (/冲突|重复|循环|幂等/.test(title)) edge='使用两个并发或重复请求复现边界；拒绝或保留冲突的结果符合契约，不静默覆盖、重复写入或形成循环。';
 else if (/权限|验证|凭据|授权|登录|会话|密钥|解锁/.test(title)) edge='验证合法身份、错误身份、过期凭据和直接 API 调用；拒绝路径不泄露目标数据或可复用认证材料。';
 else if (/校验|检查|核对|明确|确定|建立|门禁/.test(title)) edge='提供明确的通过与失败样例、可重复执行步骤和判定依据；失败能够阻止不满足约束的后续行为。';
 else edge='验证正常操作与非法输入/目标不存在；需要持久化的结果在重新查询后保持一致，失败不留下伪成功或部分写入状态。';
 return [`完成“${title}”的独立操作路径，相关入口能够触发行为并观察结果；不得仅交付未接入的接口或静态页面。`,edge,domainRules[domain], '提供针对本行为的自动化或可重复验收证据，覆盖上述成功和失败分支；涉及界面时同时验证入口、加载、结果和错误状态。'];
}
const manifest=[];
for (const g of groups) {
 const phase=g.key.startsWith('A')?'P0':g.key.startsWith('B')?'P1':g.key.startsWith('C')?'P2':(['R01','R02','R03','R05'].includes(g.key)?'P1':'P2');
 const priority=phase==='P0'||['notes','password','security','migration','release'].includes(g.domain)?'High':phase==='P1'?'Medium':'Low';
 const sourceName=g.key==='C20'?'AI-Persona-System-Design':docNames[g.domain];
 const source=docs.find(p=>path.basename(p,'.md')===sourceName);
 if(!source)throw Error(`Missing design ${g.key}: ${sourceName}`);
 const refs=[source];
 if(['A17','A16'].includes(g.key))refs.push(docs.find(p=>path.basename(p)==='Media-Delivery-CDN-Archive-Restore-Design.md'));
 const sources=refs.map(refLink).join('\n- ');
 const req=requirements[g.domain]??`对应 ${sourceName} 的功能与验收章节`;
 const rootKey=g.key;
 const groupHitl=g.items.some((_,i)=>hitl.has(`${g.key}-${String(i+1).padStart(2,'0')}`));
 const sharedLabels=['version/v2',`phase/${phase.toLowerCase()}`,`subsystem/${g.domain}`];
 const areas=['client','offline'].includes(g.domain)?['area/web','area/client']:['platform','event','performance','release','migration'].includes(g.domain)?['area/server','area/docs']:g.domain==='plugin'?['area/server','area/plugin','area/console']:['area/server',g.key.startsWith('B')||['sharing','planning','notes','password','finance'].includes(g.domain)?'area/web':'area/console'];
 const type=['platform','operations','event','audit','performance','migration','release'].includes(g.domain)?'Task':'Feature';
 const scope=`产品交付批次：${phase}（按已确认开发计划；与子系统文档内部优先级分别表达）。归属：${g.domain}。\n需求关联：${req}。`;
 const state=evidence(g.domain);
 const parentBody=`<!-- ikaros-v2-plan:${g.key} -->\n## 功能目标\n\n交付“${g.title}”这一具体功能。此 Issue 只汇总本功能及其子任务，没有全局总跟踪父 Issue。\n\n${scope}\n\n## 当前基线与复用要求\n\n${state}\n\n## 功能范围\n\n${g.items.map((x,i)=>`- ${g.key}-${String(i+1).padStart(2,'0')}：${x}`).join('\n')}\n\n## 整体验收\n\n- [ ] 全部直接子 Issue 独立验收完成，GitHub Sub-issues progress 可反映真实进度。\n- [ ] 本功能跨 API、持久化与适用界面的组合路径通过联调。\n- [ ] ${domainRules[g.domain]}\n- [ ] 记录验证证据和剩余限制；不以代码文件存在或子任务数量替代整体验收。\n\n## 执行方式\n\n${groupHitl?'HITL：包含需要维护者确认的设计或验收决策；以对应子任务为准。':'AFK：子任务的契约与前置依赖满足后可以独立实施。'}\n\n## 设计依据\n\n- ${sources}\n\n## 依赖\n\n{{DEPENDENCIES}}\n`;
 manifest.push({key:rootKey,parent:null,title:`[V2][${g.key}] ${g.title}`,domain:g.domain,phase,priority,effort:'High',mode:groupHitl?'HITL':'AFK',type,labels:[...sharedLabels,...areas,'kind/feature-group',type==='Feature'?'feature':'improvement',groupHitl?'workflow/hitl':'workflow/afk'],dependencies:base[g.key],body:parentBody,status:'Backlog'});
 for(let i=0;i<g.items.length;i++) {
  const key=`${g.key}-${String(i+1).padStart(2,'0')}`;
  let deps=[...base[g.key],...(i&&!independentFirst.has(g.key)?[`${g.key}-01`]:[]),...(extra[key]??[])];
  // R01's scope definition intentionally precedes creating the first backup.
  if(key==='R01-02')deps=[...base[g.key]];
  deps=[...new Set(deps)].filter(x=>x!==key);
  const mode=hitl.has(key)?'HITL':'AFK';
  const decision=mode==='HITL'?`\n## 需要确认的决策\n\n先提出本行为涉及的契约、状态或安全边界及验收样例，记录维护者决议后再实施依赖该决议的代码。\n`:'';
  const body=`<!-- ikaros-v2-plan:${key} -->\n## Parent\n\n{{PARENT}}\n\n## 要完成的行为\n\n在“${g.title}”中完成并验收：**${g.items[i]}**。从用户或调用方触发到结果可观察，覆盖必要的数据、公开契约和适用界面。\n\n${scope}\n\n## 当前基线与工作边界\n\n${state}\n\n仅处理本行为及其必要集成。相邻功能由同级子 Issue 跟踪；不以完成整个父功能作为关闭条件。目标工作量为半天到两天；发现超过三天的独立行为时继续创建真实细分子 Issue。\n\n## 验收条件\n\n${checks(g.items[i],g.domain).map(x=>`- [ ] ${x}`).join('\n')}\n\n## 执行方式\n\n${mode}${decision}\n\n## 设计依据\n\n- ${sources}\n\n## Blocked by\n\n{{DEPENDENCIES}}\n`;
  manifest.push({key,parent:rootKey,title:`[V2][${key}] ${g.items[i]}`,domain:g.domain,phase,priority,effort:mode==='HITL'?'High':/浏览|查看|展示|选择|标记|筛选|配置/.test(g.items[i])?'Low':'Medium',mode,type:mode==='HITL'?'Task':type,labels:[...sharedLabels,...areas,'kind/feature-slice',type==='Feature'?'feature':'improvement',mode==='HITL'?'workflow/hitl':'workflow/afk'],dependencies:deps,body,status:deps.length?'Backlog':'Ready'});
 }
}
const map=new Map(manifest.map(x=>[x.key,x]));
if(map.size!==manifest.length)throw Error('Duplicate plan keys');
const visiting=new Set(),visited=new Set(),ordered=[];
function visit(key) {
 if(visited.has(key))return;
 if(visiting.has(key))throw Error(`Dependency cycle: ${[...visiting,key].join(' -> ')}`);
 const item=map.get(key);if(!item)throw Error(`Missing key ${key}`);
 visiting.add(key);
 // Parents are created independently of blocking links; children require an existing parent.
 if(item.parent)visit(item.parent);
 // Feature parents may depend on leaf work, so only order leaf dependencies here.
 if(item.parent)for(const dep of item.dependencies)visit(dep);
 visiting.delete(key);visited.add(key);ordered.push(key);
}
for(const item of manifest)visit(item.key);
// Separately validate the actual blocking graph, without parent membership edges.
visiting.clear();visited.clear();
function checkDependencies(key){if(visited.has(key))return;if(visiting.has(key))throw Error(`Blocking cycle: ${[...visiting,key].join(' -> ')}`);visiting.add(key);for(const d of map.get(key).dependencies){if(!map.has(d))throw Error(`Missing dependency ${d}`);checkDependencies(d);}visiting.delete(key);visited.add(key);}
for(const item of manifest)checkDependencies(item.key);
const output={approvedAt:'2026-09-05',repository:'ikaros-dev/ikaros',project:'Ikaros Project',baselineCommit:commit,counts:{parents:groups.length,children:manifest.length-groups.length,total:manifest.length},order:ordered,issues:manifest};
fs.writeFileSync(path.join(dir,'manifest.json'),JSON.stringify(output,null,2)+'\n');
const audit=groups.map(g=>({key:g.key,domain:g.domain,evidence:evidence(g.domain)}));
fs.writeFileSync(path.join(dir,'baseline-audit.json'),JSON.stringify(audit,null,2)+'\n');
console.log(JSON.stringify({counts:output.counts,dependencyEdges:manifest.reduce((n,x)=>n+x.dependencies.length,0),labels:[...new Set(manifest.flatMap(x=>x.labels))],ready:manifest.filter(x=>x.status==='Ready').map(x=>x.key)},null,2));
