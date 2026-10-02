import { http } from "@/utils/http";

export type Attachment = {
  id: string;
  resourceId: string;
  fileName: string;
  kind: string;
  sha256: string;
  sizeBytes: number;
  mediaType: string;
  availability:
    | "READY"
    | "PROCESSING"
    | "RESTORE_REQUIRED"
    | "MISSING"
    | "CORRUPTED";
};

export type AttachmentPage = {
  items: Attachment[];
  total: number;
  page: number;
  size: number;
};

export type ManagedAttachment = {
  id: string;
  resource_id: string;
  file_name: string;
  kind: string;
  sha256: string;
  size_bytes: number;
  media_type: string;
  availability: Attachment["availability"];
};

export type ManagedAttachmentPage = {
  items: ManagedAttachment[];
  total: number;
  page: number;
  size: number;
};

export type AdminAttachmentBlob = {
  id: string;
  hash_algorithm: string;
  sha256: string;
  size_bytes: number;
  media_type: string;
  availability: string;
  created_at: string;
};

export type AttachmentPageQuery = { page: number; size: number };

export type AttachmentPreviewProvider = {
  binding_id: string;
  delivery_provider_id: string;
  delivery_provider_key: string;
  display_name: string;
  provider_type: "DIRECT" | "CDN" | "SERVER_PROXY";
  priority: number;
  selected: boolean;
};

export type AttachmentPreviewUrl = {
  method: string;
  url: string;
  expires_at: string;
  range_supported: boolean;
  content_type: string;
  selected_provider: AttachmentPreviewProvider | null;
  providers: AttachmentPreviewProvider[];
};

export const listAccessibleAttachments = (params: AttachmentPageQuery) =>
  http.request<AttachmentPage>("get", "/attachments", { params });

export const getAttachment = (attachmentId: string) =>
  http.request<Attachment>("get", `/attachments/${attachmentId}`);

export const getAttachmentPreviewUrl = (
  attachmentId: string,
  deliveryProvider?: string
) =>
  http.request<AttachmentPreviewUrl>(
    "get",
    `/attachments/${attachmentId}/preview-url`,
    {
      params: deliveryProvider
        ? { delivery_provider: deliveryProvider }
        : undefined
    }
  );

export const listAllManagedAttachments = (
  params: AttachmentPageQuery & { query?: string }
) =>
  http.request<ManagedAttachmentPage>("get", "/admin/attachments", { params });

export const getAdminAttachmentBlob = (attachmentId: string) =>
  http.request<AdminAttachmentBlob>(
    "get",
    `/admin/attachments/${attachmentId}/blob`
  );
