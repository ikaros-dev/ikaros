import { http } from "@/utils/http";

export type Attachment = {
  id: string;
  resourceId: string;
  fileName: string;
  kind: string;
  sha256: string;
  sizeBytes: number;
  mediaType: string;
  availability: "READY" | "PROCESSING" | "RESTORE_REQUIRED" | "MISSING" | "CORRUPTED";
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

export type AttachmentPageQuery = { page: number; size: number };

export const listAccessibleAttachments = (params: AttachmentPageQuery) =>
  http.request<AttachmentPage>("get", "/attachments", { params });

export const listAllManagedAttachments = (params: AttachmentPageQuery & { query?: string }) =>
  http.request<ManagedAttachmentPage>("get", "/admin/attachments", { params });
