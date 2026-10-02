import { http } from "@/utils/http";

/**
 * 资源（Resource）接口。注意：resource-api 的 DTO 未标注 `@JsonNaming`，
 * 线上为 camelCase（已实测），因此这里按实际格式建模。
 */

export type ResourceType =
  | "VIDEO"
  | "COMIC"
  | "BOOK"
  | "MUSIC"
  | "PHOTO"
  | "ARTICLE"
  | "DOCUMENT"
  | "GAME"
  | "ARCHIVE"
  | "OTHER";

export const resourceTypes: ResourceType[] = [
  "VIDEO",
  "COMIC",
  "BOOK",
  "MUSIC",
  "PHOTO",
  "ARTICLE",
  "DOCUMENT",
  "GAME",
  "ARCHIVE",
  "OTHER"
];

export type CreateResourceRequest = {
  type: ResourceType;
  title: string;
  locale: string;
};

export type BeginAttachmentUploadRequest = {
  file_name: string;
  size_bytes: number;
  media_type: string;
  provider: string;
  object_key: string;
  sha256: string;
};

export type AttachmentUploadIntent = {
  provider: string;
  tier: string;
  method: string;
  url: string;
  object_key: string;
  expires_at: string;
  sha256: string;
  deduplicated: boolean;
  session_id: string;
};

export type CommitAttachmentUploadRequest = {
  sha256: string;
  upload_sha256: string;
  deduplicated: boolean;
  size_bytes: number;
  media_type: string;
  file_name: string;
  kind: "ORIGINAL";
  provider: string;
  tier: string;
  object_key: string;
  idempotency_key: string;
};

export type ResourceAttachment = {
  id: string;
  resourceId: string;
  fileName: string;
  kind: string;
  sha256: string;
  sizeBytes: number;
  mediaType: string;
  availability: string;
};

export type ResourceLifecycle = "ACTIVE" | "ARCHIVED" | "TRASHED" | "PURGED";

export const resourceLifecycles: ResourceLifecycle[] = [
  "ACTIVE",
  "ARCHIVED",
  "TRASHED",
  "PURGED"
];

export type IngestionSourceType =
  | "LOCAL_FILESYSTEM"
  | "NAS_MOUNT"
  | "OBJECT_STORAGE"
  | "MANUAL_UPLOAD"
  | "REMOTE_URL"
  | "PROVIDER_COLLECTION"
  | "PLUGIN_SOURCE";

export type IngestionSource = {
  id: string;
  type: IngestionSourceType;
  displayName: string;
  rootReference: string;
  credentialConfigured: boolean;
  scanPolicy: Record<string, unknown>;
  status: "ENABLED" | "DISABLED";
  healthStatus: string;
};

export type IngestionScan = {
  id: string;
  sourceId: string;
  status: "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELLED";
  discoveredCount: number;
  changedCount: number;
  skippedCount: number;
  errorSummary: string | null;
};

export type IngestionCandidate = {
  id: string;
  suggestedResourceType: string;
  titleHint: string | null;
  externalIdHint: string | null;
  confidence: number;
  status: string;
};

export type ImportPlan = {
  id: string;
  scanRunId: string;
  dryRun: boolean;
  status: string;
  version: number | null;
  itemCount: number;
};

export type ImportPlanItem = {
  id: string;
  planId: string;
  candidateId: string;
  action: string;
  targetId: string | null;
  reason: string | null;
  confidence: number;
  version: number | null;
};

export type ImportRun = {
  id: string;
  planId: string;
  status: string;
  completedCount: number;
  failedCount: number;
  skippedCount: number;
};

export const listIngestionSources = () =>
  http.request<IngestionSource[]>("get", "/ingestion/sources");

export const createIngestionSource = (data: {
  type: IngestionSourceType;
  displayName: string;
  rootReference: string;
}) => http.request<IngestionSource>("post", "/ingestion/sources", { data });

export const startIngestionScan = (sourceId: string) =>
  http.request<IngestionScan>("post", `/ingestion/sources/${sourceId}/scans`, {
    data: { trigger: "USER_REQUESTED" }
  });

export const getIngestionScan = (scanId: string) =>
  http.request<IngestionScan>("get", `/ingestion/sources/scans/${scanId}`);

export const listIngestionCandidates = (scanId: string) =>
  http.request<IngestionCandidate[]>("get", `/ingestion/scans/${scanId}/candidates`);

export const generateImportPlan = (scanId: string) =>
  http.request<ImportPlan>("post", `/ingestion/scans/${scanId}/plans`, {
    data: { dryRun: false, policySnapshot: {} }
  });

export const listImportPlanItems = (planId: string) =>
  http.request<ImportPlanItem[]>("get", `/ingestion/scans/plans/${planId}/items`);

export const updateImportPlanItem = (
  planId: string,
  itemId: string,
  data: { expectedVersion: number; action: "CREATE_RESOURCE" | "SKIP"; reason?: string }
) =>
  http.request<ImportPlanItem>(
    "patch",
    `/ingestion/scans/plans/${planId}/items/${itemId}`,
    { data }
  );

export const approveImportPlan = (planId: string, expectedVersion: number) =>
  http.request<ImportPlan>("post", `/ingestion/scans/plans/${planId}/approve`, {
    data: { expectedVersion }
  });

export const startImportRun = (planId: string, expectedPlanVersion: number) =>
  http.request<ImportRun>("post", `/ingestion/plans/${planId}/runs`, {
    data: { expectedPlanVersion },
    headers: { "Idempotency-Key": crypto.randomUUID() }
  });

export type ResourceClassification =
  | "PUBLIC"
  | "SHARED"
  | "PRIVATE"
  | "SENSITIVE"
  | "SECURE";

export type ResourceTitle = {
  id: string;
  locale: string;
  value: string;
  primary: boolean;
  kind: "TITLE" | "ALIAS";
};

export type ExternalIdentity = {
  id: string;
  provider: string;
  type: string;
  value: string;
};

export type Resource = {
  id: string;
  type: ResourceType;
  primaryTitle: string | null;
  summary: string | null;
  dataClassification: ResourceClassification;
  lifecycle: ResourceLifecycle;
  titles: ResourceTitle[];
  externalIdentities: ExternalIdentity[];
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type ResourcePage = {
  items: Resource[];
  total: number;
  page: number;
  size: number;
};

export type ResourceLibraryFilters = {
  type?: string;
  query?: string;
  lifecycle_status?: string;
  collection_id?: string;
  tag?: string;
  source_provider?: string;
  page?: number;
  size?: number;
};

export type Collection = {
  id: string;
  parentId: string | null;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
  version: number | null;
};

export type ResourceTag = {
  id: string;
  name: string;
  color: string | null;
};

export type ResourceMetadata = {
  id: string;
  fieldKey: string;
  value: string;
  source: "USER" | "FILE_SCAN" | "IMPORT" | "PROVIDER" | "PLUGIN" | "SYSTEM";
  sourceReference: string | null;
  manuallyLocked: boolean;
  applied: boolean;
};

export type ResourceRelation = {
  id: string;
  targetResourceId: string;
  type: string;
  position: number;
};

export type FavoriteState = {
  resourceId: string;
  favorite: boolean;
};

export const listResources = (filters: ResourceLibraryFilters = {}) =>
  http.request<ResourcePage>("get", "/resources", { params: filters });

export const createResource = (data: CreateResourceRequest, idempotencyKey: string) =>
  http.request<Resource>("post", "/resources", {
    data,
    headers: { "Idempotency-Key": idempotencyKey }
  });

export const beginAttachmentUpload = (
  resourceId: string,
  data: BeginAttachmentUploadRequest,
  idempotencyKey: string
) =>
  http.request<AttachmentUploadIntent>(
    "post",
    `/resources/${resourceId}/attachments/upload-intents`,
    { data, headers: { "Idempotency-Key": idempotencyKey } }
  );

export const commitAttachmentUpload = (
  resourceId: string,
  data: CommitAttachmentUploadRequest
) =>
  http.request<ResourceAttachment>(
    "post",
    `/resources/${resourceId}/attachments/commit`,
    { data }
  );

export const getResource = (resourceId: string) =>
  http.request<Resource>("get", `/resources/${resourceId}`);

export const updateResource = (
  resourceId: string,
  data: { primary_title?: string; summary?: string },
  version: number
) =>
  http.request<Resource>("patch", `/resources/${resourceId}`, {
    data,
    headers: {
      "If-Match": `"${version}"`,
      "Content-Type": "application/merge-patch+json"
    }
  });

export const archiveResource = (resourceId: string, version: number) =>
  http.request<Resource>("post", `/resources/${resourceId}/actions/archive`, {
    headers: { "If-Match": `"${version}"` }
  });

export const restoreResource = (resourceId: string, version: number) =>
  http.request<Resource>("post", `/resources/${resourceId}/actions/restore`, {
    headers: { "If-Match": `"${version}"` }
  });

export const trashResource = (resourceId: string, version: number) =>
  http.request<void>("delete", `/resources/${resourceId}`, {
    headers: { "If-Match": `"${version}"` }
  });

export const purgeResource = (resourceId: string, version: number) =>
  http.request<void>("post", `/resources/${resourceId}/actions/purge`, {
    headers: { "If-Match": `"${version}"`, "X-Ikaros-Confirmation": "PURGE" }
  });

export const listCollections = () =>
  http.request<Collection[]>("get", "/collections");

export const listTagCatalog = (query = "") =>
  http.request<{ items: ResourceTag[]; total: number }>("get", "/tags", {
    params: { query, page: 0, size: 100 }
  });

export const listResourceTags = (resourceId: string) =>
  http.request<ResourceTag[]>("get", `/resources/${resourceId}/tags`);

export const addResourceTag = (
  resourceId: string,
  data: { name: string; color?: string }
) =>
  http.request<ResourceTag>("post", `/resources/${resourceId}/tags`, { data });

export const deleteResourceTag = (resourceId: string, tagId: string) =>
  http.request<void>(
    "delete",
    `/resources/${resourceId}/tags/${tagId}`
  );

export const getFavorite = (resourceId: string) =>
  http.request<FavoriteState>("get", `/resources/${resourceId}/favorite`);

export const addFavorite = (resourceId: string) =>
  http.request<FavoriteState>("post", `/resources/${resourceId}/favorite`);

export const removeFavorite = (resourceId: string) =>
  http.request<void>("delete", `/resources/${resourceId}/favorite`);

export const listResourceMetadata = (resourceId: string) =>
  http.request<ResourceMetadata[]>(
    "get",
    `/resources/${resourceId}/metadata`
  );

export const setResourceMetadata = (
  resourceId: string,
  fieldKey: string,
  value: string
) =>
  http.request<ResourceMetadata>(
    "put",
    `/resources/${resourceId}/metadata/${encodeURIComponent(fieldKey)}`,
    { data: { value } }
  );

export const restoreAutomaticMetadata = (
  resourceId: string,
  fieldKey: string
) =>
  http.request<ResourceMetadata>(
    "post",
    `/resources/${resourceId}/metadata/${encodeURIComponent(fieldKey)}/restore-automatic`
  );

export const listResourceRelations = (resourceId: string) =>
  http.request<ResourceRelation[]>(
    "get",
    `/resources/${resourceId}/relations`
  );
