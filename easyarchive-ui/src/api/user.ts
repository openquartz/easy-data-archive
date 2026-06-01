import { http } from "../utils/http";

export interface User {
  id: number;
  username: string;
  password?: string;
  realName?: string;
  mobile?: string;
  email?: string;
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
  status?: number;
  remark?: string;
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
