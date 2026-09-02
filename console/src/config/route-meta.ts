export type SecureDomain = 'public' | 'finance' | 'private_notes' | 'passwords' | 'security'
export type ConsoleRouteMeta = { subsystem: string; requiredCapability: string; secureDomain: SecureDomain; breadcrumb: string[] }

const meta: Record<string, ConsoleRouteMeta> = {
  dashboard: { subsystem: 'workbench', requiredCapability: 'workbench.read', secureDomain: 'public', breadcrumb: ['工作台', '概览'] },
  search: { subsystem: 'workbench', requiredCapability: 'search.read', secureDomain: 'public', breadcrumb: ['工作台', '全局搜索'] },
  library: { subsystem: 'content', requiredCapability: 'resource.read', secureDomain: 'public', breadcrumb: ['内容与创作', '统一资源库'] },
  attachments: { subsystem: 'storage', requiredCapability: 'attachment.read', secureDomain: 'public', breadcrumb: ['附件与存储', '附件与 Blob'] },
  'private-notes': { subsystem: 'private-notes', requiredCapability: 'private_note.read', secureDomain: 'private_notes', breadcrumb: ['私密笔记', '保险库'] },
  passwords: { subsystem: 'passwords', requiredCapability: 'password_vault.read', secureDomain: 'passwords', breadcrumb: ['密码管理', '密码保险库'] },
  'security/users': { subsystem: 'identity-security', requiredCapability: 'user.read', secureDomain: 'security', breadcrumb: ['身份与安全', '用户与角色'] },
  'security/permissions': { subsystem: 'identity-security', requiredCapability: 'permission.read', secureDomain: 'security', breadcrumb: ['身份与安全', '权限矩阵'] },
  'ops/health': { subsystem: 'system-operations', requiredCapability: 'system.health.read', secureDomain: 'security', breadcrumb: ['系统运维', '系统健康与告警'] },
  'ops/background': { subsystem: 'system-operations', requiredCapability: 'system.task.read', secureDomain: 'security', breadcrumb: ['系统运维', '后台任务'] }
}

export function getRouteMeta(path: string): ConsoleRouteMeta {
  return meta[path] || { subsystem: 'console', requiredCapability: 'console.read', secureDomain: 'public', breadcrumb: ['Console', path] }
}
