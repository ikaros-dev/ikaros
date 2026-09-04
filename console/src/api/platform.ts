import { http } from "@/utils/http";

export type HealthResponse = { status?: string; [key: string]: unknown };

export const getHealth = () => http.request<HealthResponse>("get", "/health/live");
