import { API_SUCCESS_CODE, type ApiError, type ApiResponse } from "../types/api";

const baseURL = import.meta.env.VITE_API_BASE_URL || "/api/v1";
const AUTH_TOKEN_KEY = "easyarchive:token";
const AUTH_EXPIRED_EVENT = "easyarchive:auth-expired";

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

interface RequestOptions {
  method?: HttpMethod;
  body?: unknown;
  headers?: Record<string, string>;
}

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

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method || "GET";
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...options.headers
  };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${baseURL}${path}`, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });

  if (response.status === 401) {
    clearAuthAndSignal();
    throw createApiError("Unauthorized", { status: 401 });
  }

  let payload: ApiResponse<T>;
  try {
    payload = (await response.json()) as ApiResponse<T>;
  } catch {
    throw createApiError("Invalid response payload", { status: response.status });
  }

  if (!response.ok) {
    throw createApiError(payload.message || "Request failed", {
      status: response.status,
      code: payload.code,
      requestId: payload.requestId
    });
  }

  if (payload.code !== API_SUCCESS_CODE) {
    throw createApiError(payload.message || "Request failed", {
      status: response.status,
      code: payload.code,
      requestId: payload.requestId
    });
  }

  return payload.data;
}

export const http = {
  get<T>(path: string, headers?: Record<string, string>): Promise<T> {
    return request<T>(path, { method: "GET", headers });
  },
  post<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return request<T>(path, { method: "POST", body, headers });
  },
  put<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return request<T>(path, { method: "PUT", body, headers });
  },
  patch<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return request<T>(path, { method: "PATCH", body, headers });
  },
  delete<T>(path: string, headers?: Record<string, string>): Promise<T> {
    return request<T>(path, { method: "DELETE", headers });
  }
};

export { AUTH_EXPIRED_EVENT, AUTH_TOKEN_KEY };

