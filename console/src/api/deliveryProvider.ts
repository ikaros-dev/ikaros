import { http } from "@/utils/http";

/**
 * 分发（Delivery）接口。注意：`/admin/delivery-providers` 与
 * `/storage/providers/{id}/delivery-bindings` 目前返回的是 camelCase
 * （相关 DTO 未标注 `@JsonNaming`，与 API 的 snake_case 约定不一致），
 * 因此这里按实际线上格式建模；后端统一后需要同步调整。
 */

export type DeliveryProviderType = "DIRECT" | "CDN" | "SERVER_PROXY";

export type DeliveryProviderHealthStatus =
  | "UNKNOWN"
  | "HEALTHY"
  | "DEGRADED"
  | "UNHEALTHY";

export type DeliveryGrantRevocationLevel =
  | "IMMEDIATE"
  | "KEY_VERSION_BOUND"
  | "TTL_BOUNDED"
  | "NOT_REVOCABLE_BEFORE_EXPIRY";

export type DeliveryProvider = {
  id: string;
  providerKey: string;
  providerType: DeliveryProviderType;
  displayName: string;
  credentialRef: string | null;
  config: Record<string, unknown>;
  capabilities: Record<string, unknown>;
  grantRevocationMode: DeliveryGrantRevocationLevel;
  signingKeyVersion: number;
  healthStatus: DeliveryProviderHealthStatus;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type DeliveryBindingCacheKeyPolicy =
  | "CONTENT_IDENTITY"
  | "FULL_REQUEST"
  | "NO_CACHE";

export type DeliveryBindingRangePolicy =
  | "PASSTHROUGH"
  | "FIXED_CHUNK"
  | "UNSUPPORTED";

export type DeliveryBinding = {
  id: string;
  storageProviderId: string;
  deliveryProviderKey: string;
  priority: number;
  enabled: boolean;
  cacheKeyPolicy: DeliveryBindingCacheKeyPolicy;
  rangePolicy: DeliveryBindingRangePolicy;
  fallbackParticipation: boolean;
  createdAt: string;
  updatedAt: string;
  version: number | null;
};

export type DeliveryBindingRequest = {
  deliveryProviderKey: string;
  priority: number;
  enabled: boolean;
  cacheKeyPolicy: DeliveryBindingCacheKeyPolicy;
  rangePolicy: DeliveryBindingRangePolicy;
  fallbackParticipation: boolean;
};

export const listDeliveryProviders = () =>
  http.request<DeliveryProvider[]>("get", "/admin/delivery-providers");

export const listDeliveryBindings = (storageProviderId: string) =>
  http.request<DeliveryBinding[]>(
    "get",
    `/storage/providers/${storageProviderId}/delivery-bindings`
  );

export const createDeliveryBinding = (
  storageProviderId: string,
  data: DeliveryBindingRequest
) =>
  http.request<DeliveryBinding>(
    "post",
    `/storage/providers/${storageProviderId}/delivery-bindings`,
    { data }
  );

export const updateDeliveryBinding = (
  storageProviderId: string,
  bindingId: string,
  data: DeliveryBindingRequest,
  version: number
) =>
  http.request<DeliveryBinding>(
    "put",
    `/storage/providers/${storageProviderId}/delivery-bindings/${bindingId}`,
    { data, headers: { "If-Match": `"${version}"` } }
  );

export const deleteDeliveryBinding = (
  storageProviderId: string,
  bindingId: string
) =>
  http.request<void>(
    "delete",
    `/storage/providers/${storageProviderId}/delivery-bindings/${bindingId}`
  );
