import { http } from "@/utils/http";

export type AuditEvent = {
  id: string;
  actorType: string;
  actorId: string | null;
  action: string;
  targetType: string;
  targetId: string | null;
  details: string | null;
  occurredAt: string;
  version: number | null;
  requestId: string | null;
  correlationId: string | null;
  result: string;
  riskLevel: string;
  detailsSchemaVersion: number | null;
};

export type AuditEventPage = {
  items: AuditEvent[];
  total: number;
  page: number;
  size: number;
};

export type AuditEventQuery = {
  actor_id?: string;
  request_id?: string;
  from?: string;
  to?: string;
  page: number;
  size: number;
};

export const listAuditEvents = (params: AuditEventQuery) =>
  http.request<AuditEventPage>("get", "/audit-events", { params });

export const getAuditEvent = (eventId: string) =>
  http.request<AuditEvent>("get", `/audit-events/${eventId}`);
