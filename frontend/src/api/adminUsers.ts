import { requestJson } from "./client";
import type { PageResponse } from "./sports";

export type UserRole = "ROLE_ADMIN" | "ROLE_USER";

export interface AdminUserShortDto {
  id: number;
  username: string;
  email: string;
}

export interface AdminUserSearchDto extends AdminUserShortDto {
  roles: string[];
  registeredAt: string;
  lastLogin: string | null;
  linkedProviders: string[];
}

export interface AdminUserDetailDto extends AdminUserShortDto {
  roles: string[];
  registeredAt: string;
  lastLoginAt: string | null;
  linkedProviders: string[];
}

export interface AdminUserCreateRequest {
  username: string;
  password: string;
  email: string;
  roles: UserRole[];
}

export interface AdminUserUpdateRequest {
  username?: string;
  email?: string;
}

export interface AdminUserPasswordRequest {
  newPassword: string;
}

export interface AdminUserMessageDto {
  message: string;
}

export function listAdminUsers(
  params: URLSearchParams,
  signal: AbortSignal,
): Promise<PageResponse<AdminUserShortDto>> {
  return requestJson<PageResponse<AdminUserShortDto>>(
    `/api/admin/users?${params.toString()}`,
    { signal },
  );
}

export function searchAdminUsers(
  params: URLSearchParams,
  signal: AbortSignal,
): Promise<PageResponse<AdminUserSearchDto>> {
  return requestJson<PageResponse<AdminUserSearchDto>>(
    `/api/admin/users/search?${params.toString()}`,
    { signal },
  );
}

export function getAdminUser(
  userId: number,
  signal?: AbortSignal,
): Promise<AdminUserDetailDto> {
  return requestJson<AdminUserDetailDto>(
    `/api/admin/users/${encodeURIComponent(userId)}`,
    { signal },
  );
}

export function createAdminUser(
  body: AdminUserCreateRequest,
): Promise<AdminUserShortDto> {
  return requestJson<AdminUserShortDto>("/api/admin/users", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateAdminUser(
  userId: number,
  body: AdminUserUpdateRequest,
): Promise<AdminUserShortDto> {
  return requestJson<AdminUserShortDto>(
    `/api/admin/users/${encodeURIComponent(userId)}`,
    {
      method: "PATCH",
      body: JSON.stringify(body),
    },
  );
}

export function setAdminUserPassword(
  userId: number,
  body: AdminUserPasswordRequest,
): Promise<AdminUserMessageDto> {
  return requestJson<AdminUserMessageDto>(
    `/api/admin/users/${encodeURIComponent(userId)}/updateUserPassword`,
    {
      method: "PATCH",
      body: JSON.stringify(body),
    },
  );
}

export function deleteAdminUser(
  userId: number,
): Promise<AdminUserMessageDto> {
  return requestJson<AdminUserMessageDto>(
    `/api/admin/users/${encodeURIComponent(userId)}`,
    { method: "DELETE" },
  );
}
