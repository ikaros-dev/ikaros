export type Problem = { title?: string; detail?: string; status?: number; errors?: Record<string, string[]> }

export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly problem?: Problem) {
    super(message)
    this.name = 'ApiError'
  }
}

const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api/v2').replace(/\/$/, '')

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

export const api = {
  listResources: (params = '') => request<Page<ResourceRecord> | ResourceRecord[]>(`/resources${params}`),
  getResource: (id: string) => request<ResourceRecord>(`/resources/${encodeURIComponent(id)}`),
  createResource: (body: Record<string, unknown>) => request<ResourceRecord>('/resources', { method: 'POST', body: JSON.stringify(body) }),
  archiveResource: (id: string, etag?: string) => request<void>(`/resources/${encodeURIComponent(id)}/actions/archive`, { method: 'POST', headers: etag ? { 'If-Match': etag } : undefined }),
  listUsers: (params = '') => request<Page<UserRecord> | UserRecord[]>(`/admin/users${params}`),
  listRoles: () => request<unknown>('/admin/roles'),
  listPermissions: () => request<unknown>('/admin/permissions'),
  getBackgroundTask: (id: string) => request<BackgroundTaskRecord>(`/background-tasks/${encodeURIComponent(id)}`),
  cancelBackgroundTask: (id: string) => request<void>(`/background-tasks/${encodeURIComponent(id)}/actions/cancel`, { method: 'POST' })
}

export function unwrapPage<T>(value: Page<T> | T[]): T[] {
  return Array.isArray(value) ? value : value.items || value.content || []
}
