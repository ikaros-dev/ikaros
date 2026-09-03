export type Problem = { title?: string; detail?: string; status?: number; errors?: Record<string, string[]> }

export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly problem?: Problem) {
    super(message)
    this.name = 'ApiError'
  }
}

const apiBase = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const sessionStorageKey = 'ikaros-console-auth-session'
export type AuthenticationRecord = { userId: string; sessionId: string; sessionToken: string; expiresAt?: string; user?: CurrentUserRecord }
function authSession() { try { return JSON.parse(localStorage.getItem(sessionStorageKey) || 'null') as AuthenticationRecord | null } catch { return null } }
export function saveAuthSession(session: AuthenticationRecord, remember = true) { if (remember) localStorage.setItem(sessionStorageKey, JSON.stringify(session)); else sessionStorage.setItem(sessionStorageKey, JSON.stringify(session)) }
export function clearAuthSession() { localStorage.removeItem(sessionStorageKey); sessionStorage.removeItem(sessionStorageKey) }
export function currentAuthSession() { return authSession() || (() => { try { return JSON.parse(sessionStorage.getItem(sessionStorageKey) || 'null') as AuthenticationRecord | null } catch { return null } })() }
export function syncRuntimeActorId() { const userId = currentAuthSession()?.userId; if (userId) (import.meta.env as Record<string, string>).VITE_ACTOR_ID = userId }
function requestId() { return crypto.randomUUID() }

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const session = currentAuthSession()
  const sessionHeaders = {
    ...(session?.userId ? { 'X-Ikaros-Actor-Id': session.userId } : {}),
    ...(session?.sessionId ? { 'X-Ikaros-Session-Id': session.sessionId } : {})
  }
  const response = await fetch(`${apiBase}${path}`, {
    ...init,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', ...sessionHeaders, ...(session?.sessionToken ? { Authorization: `Bearer ${session.sessionToken}` } : {}), ...init?.headers },
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

export type ResourceTitleRecord = { id?: string; locale?: string; value?: string; primary?: boolean; kind?: string }
export type ExternalIdentityRecord = { id?: string; provider?: string; type?: string; value?: string }
export type ResourceRecord = { id: string; type?: string; primaryTitle?: string; title?: string; summary?: string; resource_type?: string; lifecycle?: string; titles?: ResourceTitleRecord[]; externalIdentities?: ExternalIdentityRecord[]; createdAt?: string; created_at?: string; updatedAt?: string; updated_at?: string }
export type FavoriteRecord = { resourceId?: string; resource_id?: string; favorite?: boolean; favorited?: boolean }
export type DocumentRecord = { id: string; resourceId?: string; resource_id?: string; kind?: string; currentRevisionId?: string; current_revision_id?: string }
export type WorkingCopyRecord = { documentId?: string; document_id?: string; content?: string; contentSchemaVersion?: string; content_schema_version?: string; baseRevisionId?: string; base_revision_id?: string; updatedAt?: string; updated_at?: string; version?: number }
export type RevisionRecord = { id: string; documentId?: string; document_id?: string; revisionNumber?: number; revision_number?: number; content?: string; contentSchemaVersion?: string; content_schema_version?: string; createdAt?: string; created_at?: string }
export type Page<T> = { items?: T[]; content?: T[]; next_cursor?: string | null; total?: number }
export type UserRecord = { id: string; display_name?: string; username?: string; status?: string; roles?: string[]; mfa_enabled?: boolean; last_active_at?: string }
export type CurrentUserRecord = { id: string; username?: string; displayName?: string; display_name?: string; email?: string; status?: string; roleCodes?: string[]; role_codes?: string[] }
export type BackgroundTaskRecord = { id: string; taskType?: string; task_type?: string; status?: string; state?: string; progress?: Record<string, unknown>; owning_subsystem?: string; current_stage?: string; createdAt?: string; created_at?: string }
export type RoleRecord = { id: string; code?: string; name?: string; description?: string; builtIn?: boolean; permissions?: string[] }
export type StorageProviderRecord = { id: string; providerKey?: string; providerType?: string; tier?: string; status?: string; updatedAt?: string }
export type DriveSpaceRecord = { id: string; name?: string; displayName?: string; status?: string; quota_bytes?: number; quotaBytes?: number; used_bytes?: number; usedBytes?: number; updated_at?: string; updatedAt?: string }
export type DriveNodeRecord = { id: string; name?: string; nodeType?: string; node_type?: string; lifecycle?: string; nodeVersion?: number; node_version?: number; updatedAt?: string; updated_at?: string }
export type SyncBindingRecord = { id: string; deviceId?: string; device_id?: string; driveSpaceId?: string; drive_space_id?: string; localDisplayPath?: string; local_display_path?: string; enabled?: boolean; state?: string; cursor?: number; updatedAt?: string; updated_at?: string }
export type DriveQuotaRecord = { spaceId?: string; space_id?: string; limitBytes?: number; limit_bytes?: number; usedBytes?: number; used_bytes?: number; reservedBytes?: number; reserved_bytes?: number; availableBytes?: number; available_bytes?: number }
export type SyncConflictRecord = { id: string; bindingId?: string; binding_id?: string; nodeId?: string; node_id?: string; state?: string; detectedAt?: string; detected_at?: string; resolvedAt?: string; resolved_at?: string }
export type PlanningTaskRecord = { id: string; title?: string; status?: string; priority?: string; due_at?: string; updated_at?: string }
export type PlanningProjectRecord = { id: string; name?: string; description?: string; status?: string; createdAt?: string; updatedAt?: string; version?: number }
export type PlanningGoalRecord = { id: string; title?: string; description?: string; type?: string; status?: string; progress?: number; deadline?: string; updatedAt?: string }
export type PlanningTimeBlockRecord = { id: string; title?: string; startAt?: string; endAt?: string; kind?: string; status?: string; timeZone?: string }
export type PlanningFocusRecord = { id: string; taskId?: string; mode?: string; status?: string; plannedMinutes?: number; actualMinutes?: number; startedAt?: string; endedAt?: string }
export type SessionRecord = { id: string; userId?: string; loginMethod?: string; currentSvl?: string; verifiedAt?: string; verificationExpiresAt?: string; expiresAt?: string; lastActiveAt?: string }
export type CollectionRecord = { id: string; name: string; description?: string; updatedAt?: string }
export type FinanceLedgerRecord = { id: string; ownerId?: string; owner_id?: string; name?: string; baseCurrency?: string; base_currency?: string; archived?: boolean; createdAt?: string; created_at?: string }
export type FinanceAccountRecord = { id: string; ledgerId?: string; ledger_id?: string; name?: string; type?: string; currency?: string; openingBalance?: number; opening_balance?: number; currentBalance?: number; current_balance?: number; institution?: string; maskedIdentifier?: string; masked_identifier?: string; archived?: boolean }
export type FinanceTransactionRecord = { id: string; type?: string; amount?: number; currency?: string; payee?: string; note?: string; occurredAt?: string; occurred_at?: string; status?: string; source?: string }
export type FinanceBudgetRecord = { id: string; ledgerId?: string; categoryId?: string; month?: string; budget?: number; actual?: number; remaining?: number }
export type MediaHistoryRecord = { id: string; resourceId?: string; sessionId?: string; startedAt?: string; endedAt?: string; watchedSeconds?: number }
export type ResourceActivityRecord = { id: string; resourceId?: string; resource_id?: string; type?: string; details?: string; occurredAt?: string; occurred_at?: string }

export const api = {
  register: (body: { username: string; password: string; displayName: string; email?: string }) => request<AuthenticationRecord>('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login: (body: { username: string; password: string }) => request<AuthenticationRecord>('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  listResources: (params = '', actorId = '') => request<Page<ResourceRecord> | ResourceRecord[]>(`/resources${params}`, { headers: actorId ? { 'X-Ikaros-Actor-Id': actorId } : undefined }),
  getResource: (id: string, actorId = '') => request<ResourceRecord>(`/resources/${encodeURIComponent(id)}`, { headers: actorId ? { 'X-Ikaros-Actor-Id': actorId } : undefined }),
  getFavorite: (id: string, actorId: string) => request<FavoriteRecord>(`/resources/${encodeURIComponent(id)}/favorite`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  addFavorite: (id: string, actorId: string) => request<FavoriteRecord>(`/resources/${encodeURIComponent(id)}/favorite`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId } }),
  removeFavorite: (id: string, actorId: string) => request<void>(`/resources/${encodeURIComponent(id)}/favorite`, { method: 'DELETE', headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listRecentActivity: (actorId: string, limit = 10) => request<ResourceActivityRecord[]>(`/activity?limit=${limit}`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listDocuments: (actorId: string) => request<DocumentRecord[]>('/documents', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createDocument: (body: { title: string; kind: string; locale?: string; content?: string }, actorId: string) => request<DocumentRecord>('/documents', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  getWorkingCopy: (id: string, actorId: string) => request<WorkingCopyRecord>(`/documents/${encodeURIComponent(id)}/working-copy`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  updateWorkingCopy: (id: string, body: { content: string; contentSchemaVersion?: string; expectedVersion: number }, actorId: string) => request<WorkingCopyRecord>(`/documents/${encodeURIComponent(id)}/working-copy`, { method: 'PUT', headers: { 'X-Ikaros-Actor-Id': actorId, 'Content-Type': 'application/json' }, body: JSON.stringify(body) }),
  commitDocumentRevision: (id: string, body: { content: string; contentSchemaVersion?: string; expectedVersion: number }, actorId: string) => request<RevisionRecord>(`/documents/${encodeURIComponent(id)}/revisions`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Content-Type': 'application/json' }, body: JSON.stringify(body) }),
  publishDocument: (id: string, body: { slug?: string; revisionNumber: number }, actorId: string) => request<Record<string, unknown>>(`/documents/${encodeURIComponent(id)}/actions/publish`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Content-Type': 'application/json' }, body: JSON.stringify(body) }),
  createResource: (body: Record<string, unknown>, actorId: string) => request<ResourceRecord>('/resources', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  updateResource: (id: string, body: Record<string, unknown>, etag: string, actorId: string) => request<ResourceRecord>(`/resources/${encodeURIComponent(id)}`, { method: 'PATCH', headers: { 'Content-Type': 'application/merge-patch+json', 'If-Match': etag, 'X-Ikaros-Actor-Id': actorId }, body: JSON.stringify(body) }),
  archiveResource: (id: string, actorId: string, etag?: string) => request<void>(`/resources/${encodeURIComponent(id)}/actions/archive`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, ...(etag ? { 'If-Match': etag } : {}) } }),
  restoreResource: (id: string, actorId: string, etag?: string) => request<ResourceRecord>(`/resources/${encodeURIComponent(id)}/actions/restore`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, ...(etag ? { 'If-Match': etag } : {}) } }),
  listUsers: (params = '') => request<Page<UserRecord> | UserRecord[]>(`/admin/users${params}`),
  listRoles: () => request<RoleRecord[]>('/admin/roles'),
  createRole: (body: { code: string; name: string; description?: string }, actorId: string) => request<RoleRecord>('/admin/roles', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  grantRolePermission: (roleId: string, permission: string, actorId: string) => request<RoleRecord>(`/admin/roles/${encodeURIComponent(roleId)}/permissions/${encodeURIComponent(permission)}`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listPermissions: () => request<string[]>('/admin/permissions'),
  listStorageProviders: () => request<StorageProviderRecord[]>('/admin/storage-providers'),
  listDriveSpaces: (actorId: string) => request<DriveSpaceRecord[]>('/drive/spaces', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listDriveChildren: (spaceId: string, actorId: string) => request<DriveNodeRecord[]>(`/drive/spaces/${encodeURIComponent(spaceId)}/children`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listDriveBindings: (actorId: string) => request<SyncBindingRecord[]>('/drive/bindings', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  getDriveQuota: (spaceId: string, actorId: string) => request<DriveQuotaRecord>(`/drive/spaces/${encodeURIComponent(spaceId)}/quota`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listDriveConflicts: (bindingId: string, actorId: string) => request<SyncConflictRecord[]>(`/drive/bindings/${encodeURIComponent(bindingId)}/conflicts`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createDriveSpace: (displayName: string, actorId: string) => request<DriveSpaceRecord>('/drive/spaces', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify({ displayName }) }),
  listTodayTasks: (actorId: string, timeZone = 'Asia/Shanghai') => request<PlanningTaskRecord[]>(`/planning/tasks/today?timeZone=${encodeURIComponent(timeZone)}`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createPlanningTask: (body: { title: string; description?: string; priority?: string; deadline?: string }, actorId: string) => request<PlanningTaskRecord>('/planning/tasks', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Content-Type': 'application/json', 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  listPlanningProjects: (actorId: string) => request<PlanningProjectRecord[]>('/planning/projects', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createPlanningProject: (body: { name: string; description?: string }, actorId: string) => request<PlanningProjectRecord>('/planning/projects', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  listPlanningGoals: (actorId: string) => request<PlanningGoalRecord[]>('/planning/goals', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createPlanningGoal: (body: { title: string; description?: string; type?: string }, actorId: string) => request<PlanningGoalRecord>('/planning/goals', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  listPlanningTimeBlocks: (actorId: string) => request<PlanningTimeBlockRecord[]>('/planning/time-blocks', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createPlanningTimeBlock: (body: { title: string; startAt: string; endAt: string; kind?: string; timeZone?: string }, actorId: string) => request<PlanningTimeBlockRecord>('/planning/time-blocks', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  listPlanningFocus: (actorId: string) => request<PlanningFocusRecord[]>('/planning/focus-sessions', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  startPlanningFocus: (body: { plannedMinutes: number; mode?: string }, actorId: string) => request<PlanningFocusRecord>('/planning/focus-sessions', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  completePlanningFocus: (id: string, body: { actualMinutes: number; note?: string }, actorId: string) => request<PlanningFocusRecord>(`/planning/focus-sessions/${encodeURIComponent(id)}/complete`, { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Content-Type': 'application/json' }, body: JSON.stringify(body) }),
  listMediaHistory: (actorId: string) => request<MediaHistoryRecord[]>('/media/playback/history', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listSessions: (actorId: string) => request<SessionRecord[]>('/me/sessions', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  getCurrentUser: (actorId: string) => request<CurrentUserRecord>('/me', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  revokeSession: (userId: string, sessionId: string, actorId: string) => request<void>(`/users/${encodeURIComponent(userId)}/sessions/${encodeURIComponent(sessionId)}`, { method: 'DELETE', headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listCollections: (actorId: string) => request<CollectionRecord[]>('/collections', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createCollection: (body: { name: string; description?: string }, actorId: string) => request<CollectionRecord>('/collections', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  listFinanceLedgers: (actorId: string) => request<FinanceLedgerRecord[]>('/finance/ledgers', { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  createFinanceLedger: (body: { name: string; baseCurrency: string }, actorId: string) => request<FinanceLedgerRecord>('/finance/ledgers', { method: 'POST', headers: { 'X-Ikaros-Actor-Id': actorId, 'Idempotency-Key': requestId() }, body: JSON.stringify(body) }),
  listFinanceAccounts: (ledgerId: string, actorId: string) => request<FinanceAccountRecord[]>(`/finance/ledgers/${encodeURIComponent(ledgerId)}/accounts`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listFinanceTransactions: (ledgerId: string, actorId: string) => request<FinanceTransactionRecord[]>(`/finance/ledgers/${encodeURIComponent(ledgerId)}/transactions`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  listFinanceBudgets: (ledgerId: string, month: string, actorId: string) => request<FinanceBudgetRecord[]>(`/finance/ledgers/${encodeURIComponent(ledgerId)}/budgets?month=${encodeURIComponent(month)}`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
  searchFinanceTransactions: (ledgerId: string, query: string, actorId: string) => request<FinanceTransactionRecord[]>(`/finance/ledgers/${encodeURIComponent(ledgerId)}/transactions/search?q=${encodeURIComponent(query)}`, { headers: { 'X-Ikaros-Actor-Id': actorId } }),
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
