import { http } from "../utils/http";

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username?: string;
  roleCode?: string;
}

export interface UserProfile {
  id?: number;
  username: string;
  realName?: string;
  email?: string;
  status?: number;
  roleCode?: string;
  isAdmin?: boolean;
}

export function loginApi(payload: LoginPayload): Promise<LoginResponse> {
  return http.post<LoginResponse>("auth/login", payload);
}

export function logoutApi(): Promise<void> {
  return http.post<void>("auth/logout");
}

export function meApi(): Promise<UserProfile> {
  return http.post<UserProfile>("auth/me");
}
