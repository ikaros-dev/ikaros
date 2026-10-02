import { http } from "@/utils/http";

export type StorageProvider = {
  id: string;
  provider_key: string;
  provider_type: string;
  display_name: string;
  tier: "HOT" | "WARM" | "COLD" | "ARCHIVE" | "DEEP_ARCHIVE";
  enabled: boolean;
  drain_status: "NORMAL" | "DRAINING" | "DRAINED";
  capabilities: Record<string, unknown>;
  configuration: Record<string, unknown>;
  version: number;
};

export type StorageProviderProbe = {
  provider_id: string;
  status: "HEALTHY" | "DEGRADED" | "FAILED" | "UNSUPPORTED";
  connection: boolean;
  read: boolean;
  write: boolean;
  checked_at: string;
  error_code?: string | null;
};

export type StorageProviderStatus = {
  provider_id: string;
  provider_status: string;
  health: StorageProviderProbe;
  capacity_bytes?: number | null;
  used_bytes?: number | null;
  checked_at: string;
};

export type BlobPlacement = {
  id: string;
  provider: string;
  tier: "HOT" | "WARM" | "COLD" | "ARCHIVE" | "DEEP_ARCHIVE";
  object_key: string;
  state:
    | "ACTIVE"
    | "VERIFYING"
    | "UNAVAILABLE"
    | "DELETING"
    | "RESTORING"
    | "READY_TEMPORARILY";
};

export const listBlobPlacements = (blobId: string) =>
  http.request<BlobPlacement[]>("get", `/admin/blobs/${blobId}/placements`);

export type CreateStorageProviderRequest = {
  provider_key: string;
  provider_type: string;
  display_name: string;
  tier: StorageProvider["tier"];
  capabilities: Record<string, unknown>;
  credential_ref?: string;
  configuration: Record<string, unknown>;
  access_key_id?: string;
  secret_access_key?: string;
  session_token?: string;
};

export const listStorageProviders = () =>
  http.request<StorageProvider[]>("get", "/admin/storage-providers");

export const getStorageProvider = (providerId: string) =>
  http.request<StorageProvider>("get", `/admin/storage-providers/${providerId}`);

export const getStorageProviderStatus = (providerId: string) =>
  http.request<StorageProviderStatus>("get", `/admin/storage-providers/${providerId}/status`);

export const probeStorageProvider = (providerId: string) =>
  http.request<StorageProviderProbe>("post", `/admin/storage-providers/${providerId}/probe`);

export const createStorageProvider = (data: CreateStorageProviderRequest, idempotencyKey: string) =>
  http.request<StorageProvider>("post", "/admin/storage-providers", {
    data,
    headers: { "Idempotency-Key": idempotencyKey }
  });

export type UpdateStorageProviderRequest = {
  provider_type: string;
  display_name: string;
  tier: StorageProvider["tier"];
  configuration: Record<string, unknown>;
};

export const updateStorageProvider = (
  providerId: string,
  data: UpdateStorageProviderRequest,
  version: number
) =>
  http.request<StorageProvider>("put", `/admin/storage-providers/${providerId}`, {
    data,
    headers: { "If-Match": `"${version}"` }
  });

export type ReplaceStorageProviderCredentialsRequest = {
  access_key_id: string;
  secret_access_key: string;
  session_token?: string;
};

export const replaceStorageProviderCredentials = (
  providerId: string,
  data: ReplaceStorageProviderCredentialsRequest
) =>
  http.request<StorageProviderProbe>(
    "post",
    `/admin/storage-providers/${providerId}/credentials`,
    { data }
  );

export const enableStorageProvider = (providerId: string) =>
  http.request<StorageProvider>("post", `/admin/storage-providers/${providerId}/enable`);

export const disableStorageProvider = (providerId: string) =>
  http.request<StorageProvider>("post", `/admin/storage-providers/${providerId}/disable`);

export const deleteStorageProvider = (providerId: string, version: number) =>
  http.request<void>("delete", `/admin/storage-providers/${providerId}`, {
    headers: { "If-Match": `"${version}"` }
  });
