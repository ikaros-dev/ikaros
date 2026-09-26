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

export const listStorageProviders = () =>
  http.request<StorageProvider[]>("get", "/admin/storage-providers");

export const getStorageProvider = (providerId: string) =>
  http.request<StorageProvider>("get", `/admin/storage-providers/${providerId}`);

export const getStorageProviderStatus = (providerId: string) =>
  http.request<StorageProviderStatus>("get", `/admin/storage-providers/${providerId}/status`);

export const probeStorageProvider = (providerId: string) =>
  http.request<StorageProviderProbe>("post", `/admin/storage-providers/${providerId}/probe`);
