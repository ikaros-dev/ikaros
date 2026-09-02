export type Problem = { title?: string; detail?: string; status?: number; errors?: Record<string, string[]> }

export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly problem?: Problem) {
    super(message)
    this.name = 'ApiError'
  }
}

const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api/v2').replace(/\/$/, '')
function requestId() { return crypto.randomUUID() }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    ...init,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...init?.headers },
    credentials: 'include'
  })
  if (!response.ok) {
    let problem: Problem | undefined
    try { problem = await response.json() as Problem } catch { /* non-json error responses are still reported */ }
    throw new ApiError(response.status, problem?.detail || problem?.title || `请求失败（${response.status}）`, problem)
  }
  if (response.status === 204) return undefined as T
  return await response.json() as T
}

export type ResourceRecord = { id: string; title?: string; resource_type?: string; lifecycle?: string; updated_at?: string }
export type Page<T> = { items?: T[]; content?: T[]; next_cursor?: string | null; total?: number }
export type UserRecord = { id: string; display_name?: string; username?: string; status?: string; roles?: string[]; mfa_enabled?: boolean; last_active_at?: string }
export type BackgroundTaskRecord = { id: string; task_type?: string; owning_subsystem?: string; state?: string; progress?: number; current_stage?: string; created_at?: string }
export type RoleRecord = { id: string; code?: string; name?: string; description?: string; builtIn?: boolean; permissions?: string[] }
export type StorageProviderRecord = { id: string; providerKey?: string; providerType?: string; tier?: string; status?: string; updatedAt?: string }

export const api = {
  listResources: (params = '') => request<Page<ResourceRecord> | ResourceRecord[]>(`/resources${params}`),
  getResource: (id: string) => request<ResourceRecord>(`/resources/${encodeURIComponent(id)}`),
  createResource: (body: Record<string, unknown>) => request<ResourceRecord>('/resources', { method: 'POST', headers: { 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  updateResource: (id: string, body: Record<string, unknown>, etag: string) => request<ResourceRecord>(`/resources/${encodeURIComponent(id)}`, { method: 'PATCH', headers: { 'Content-Type': 'application/merge-patch+json', 'If-Match': etag }, body: JSON.stringify(body) }),
  archiveResource: (id: string, etag?: string) => request<void>(`/resources/${encodeURIComponent(id)}/actions/archive`, { method: 'POST', headers: etag ? { 'If-Match': etag } : undefined }),
  restoreResource: (id: string, etag?: string) => request<ResourceRecord>(`/resources/${encodeURIComponent(id)}/actions/restore`, { method: 'POST', headers: etag ? { 'If-Match': etag } : undefined }),
  listUsers: (params = '') => request<Page<UserRecord> | UserRecord[]>(`/admin/users${params}`),
  listRoles: () => request<RoleRecord[]>('/admin/roles'),
  listPermissions: () => request<string[]>('/admin/permissions'),
  listStorageProviders: () => request<StorageProviderRecord[]>('/admin/storage-providers'),
  enableStorageProvider: (id: string) => request<StorageProviderRecord>(`/admin/storage-providers/${encodeURIComponent(id)}/enable`, { method: 'POST' }),
  disableStorageProvider: (id: string) => request<void>(`/admin/storage-providers/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  getBackgroundTask: (id: string) => request<BackgroundTaskRecord>(`/background-tasks/${encodeURIComponent(id)}`),
  listBackgroundTasks: (status?: string) => request<BackgroundTaskRecord[]>(`/background-tasks${status ? `?status=${encodeURIComponent(status)}` : ''}`),
  cancelBackgroundTask: (id: string) => request<void>(`/background-tasks/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  changeUserStatus: (id: string, status: string, actorId: string) => request<UserRecord>(`/admin/users/${encodeURIComponent(id)}/status/${encodeURIComponent(status)}`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId } }),
  assignRole: (userId: string, roleId: string, actorId: string) => request<void>(`/admin/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleId)}`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId } })
}

export function unwrapPage<T>(value: Page<T> | T[]): T[] {
  return Array.isArray(value) ? value : value.items || value.content || []
}
