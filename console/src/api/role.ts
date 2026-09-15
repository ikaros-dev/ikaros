import { http } from "@/utils/http";

export type ManagedRole = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  builtIn: boolean;
  permissions: string[];
};

export type CreateRoleRequest = {
  code: string;
  name: string;
  description?: string;
};

export type ReplaceRolePermissionsRequest = {
  permissions: string[];
};

export const listManagedRoles = () =>
  http.request<ManagedRole[]>("get", "/admin/roles");

export const createManagedRole = (data: CreateRoleRequest) =>
  http.request<ManagedRole>("post", "/admin/roles", { data });

export const replaceManagedRolePermissions = (
  roleId: string,
  data: ReplaceRolePermissionsRequest
) => http.request<ManagedRole>("put", `/admin/roles/${roleId}/permissions`, { data });

export const assignUserRole = (userId: string, roleId: string) =>
  http.request<void>("post", `/admin/roles/users/${userId}/roles/${roleId}`);

export const revokeUserRole = (userId: string, roleId: string) =>
  http.request<void>("post", `/admin/roles/users/${userId}/roles/${roleId}/actions/revoke`);
