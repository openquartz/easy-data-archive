import { http } from "../utils/http";

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username?: string;
}

export interface UserProfile {
  username: string;
  displayName?: string;
  email?: string;
}

export function loginApi(payload: LoginPayload): Promise<LoginResponse> {
  return http.post<LoginResponse>("/auth/login", payload);
}

export function logoutApi(): Promise<void> {
  return http.post<void>("/auth/logout");
}

export function meApi(): Promise<UserProfile> {
  return http.get<UserProfile>("/auth/me");
}

