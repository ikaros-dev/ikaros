import { http } from "@/utils/http";

export type StorageProvider = {
  id: string;
  provider_key: string;
  provider_type: string;
  display_name: string;
  tier: "HOT" | "WARM" | "COLD" | "ARCHIVE";
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

export type CreateStorageProviderRequest = {
  provider_key: string;
  provider_type: string;
  display_name: string;
  tier: StorageProvider["tier"];
  capabilities: Record<string, unknown>;
  credential_ref: string;
  configuration: Record<string, unknown>;
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

export const enableStorageProvider = (providerId: string) =>
  http.request<StorageProvider>("post", `/admin/storage-providers/${providerId}/enable`);

export const disableStorageProvider = (providerId: string) =>
  http.request<StorageProvider>("post", `/admin/storage-providers/${providerId}/disable`);

export const deleteStorageProvider = (providerId: string, version: number) =>
  http.request<void>("delete", `/admin/storage-providers/${providerId}`, {
    headers: { "If-Match": `"${version}"` }
  });
