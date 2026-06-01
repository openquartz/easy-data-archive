export const API_SUCCESS_CODE = "SUCCESS";

export interface ApiResponse<T> {
  code: string;
  message: string;
  requestId?: string;
  data: T;
}

export interface ApiError extends Error {
  code?: string;
  status?: number;
  requestId?: string;
}

