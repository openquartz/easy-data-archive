import { API_SUCCESS_CODE, type ApiError, type ApiResponse } from "../types/api";
import { showErrorToast } from "../stores/toast";
import axios, { type AxiosError, type AxiosRequestConfig } from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL || "/api/v1";
const AUTH_TOKEN_KEY = "easyarchive:token";
const AUTH_EXPIRED_EVENT = "easyarchive:auth-expired";

function createApiError(message: string, extras?: Partial<ApiError>): ApiError {
  const error = new Error(message) as ApiError;
  if (extras) {
    Object.assign(error, extras);
  }
  return error;
}

function getToken(): string {
  return localStorage.getItem(AUTH_TOKEN_KEY) || "";
}

function clearAuthAndSignal(): void {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
}

function emitHttpErrorToast(message: string): void {
  if (message.trim()) {
    showErrorToast(message, 5000);
  }
}

const client = axios.create({
  baseURL
});

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  <T>(response: { data: ApiResponse<T> | T | null; status: number }) => {
    const payload = response.data as ApiResponse<T> | T | null;
    if (payload && typeof payload === "object" && "code" in payload) {
      const envelope = payload as ApiResponse<T>;
      if (envelope.code !== API_SUCCESS_CODE) {
        const message = envelope.message || "Request failed";
        emitHttpErrorToast(message);
        throw createApiError(message, {
          status: response.status,
          code: envelope.code,
          requestId: envelope.requestId
        });
      }
      return envelope.data;
    }
    return payload as T;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status;
    const payload = error.response?.data;

    if (status === 401) {
      clearAuthAndSignal();
      const message = payload?.message || "Unauthorized";
      emitHttpErrorToast(message);
      throw createApiError(message, { status: 401 });
    }

    const message = payload?.message || error.message || "Request failed";
    emitHttpErrorToast(message);
    throw createApiError(message, {
      status,
      code: payload?.code,
      requestId: payload?.requestId
    });
  }
);

function request<T>(config: AxiosRequestConfig): Promise<T> {
  return client.request<unknown, T>(config);
}

export const http = {
  get<T>(path: string, headers?: Record<string, string>): Promise<T> {
    return request<T>({ url: path, method: "GET", headers });
  },
  post<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return request<T>({ url: path, method: "POST", data: body, headers });
  },
  put<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return request<T>({ url: path, method: "PUT", data: body, headers });
  },
  patch<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return request<T>({ url: path, method: "PATCH", data: body, headers });
  },
  delete<T>(path: string, headers?: Record<string, string>): Promise<T> {
    return request<T>({ url: path, method: "DELETE", headers });
  }
};

export { AUTH_EXPIRED_EVENT, AUTH_TOKEN_KEY };
