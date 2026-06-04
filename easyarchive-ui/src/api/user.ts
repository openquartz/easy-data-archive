import { http } from "../utils/http";

export interface User {
  id: number;
  username: string;
  password?: string;
  realName?: string;
  mobile?: string;
  email?: string;
  roleCode?: string;
  status: number;
  lastLoginTime?: string;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface UserPayload {
  username: string;
  password?: string;
  realName?: string;
  mobile?: string;
  email?: string;
  roleCode?: string;
  status?: number;
  remark?: string;
}

export interface UserDatasourcePermissionItem {
  id: number;
  datasourceCode: string;
  datasourceName: string;
  datasourceType: string;
}

export interface ReplaceUserDatasourcePermissionsPayload {
  datasourceIds: number[];
}

export function getUsersApi(): Promise<User[]> {
  return http.get<User[]>("/users");
}

export function createUserApi(payload: UserPayload): Promise<User> {
  return http.post<User>("/users", payload);
}

export function updateUserApi(id: number, payload: UserPayload): Promise<User> {
  return http.put<User>(`/users/${id}`, payload);
}

export function updateUserStatusApi(id: number, status: number): Promise<void> {
  return http.patch<void>(`/users/${id}/status?status=${status}`);
}

export function getUserDatasourcePermissionsApi(userId: number): Promise<UserDatasourcePermissionItem[]> {
  return http.get<UserDatasourcePermissionItem[]>(`/users/${userId}/datasource-permissions`);
}

export function replaceUserDatasourcePermissionsApi(
  userId: number,
  payload: ReplaceUserDatasourcePermissionsPayload
): Promise<void> {
  return http.put<void>(`/users/${userId}/datasource-permissions`, payload);
}
