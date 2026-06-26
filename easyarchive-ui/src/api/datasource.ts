import { http } from "../utils/http";

export interface Datasource {
  id: number;
  datasourceCode: string;
  datasourceName: string;
  datasourceType: string;
  jdbcUrl: string;
  username: string;
  passwordCipher?: string;
  status: number;
  lastCheckTime?: string;
  ownerUserId?: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface DatasourceTypeOption {
  code: string;
  name: string;
}

export interface DatasourcePagedResult {
  data: Datasource[];
  total: number;
  page: number;
  size: number;
}

export interface DatasourceQuery {
  page: number;
  size: number;
  keyword?: string;
  status?: number;
}

export interface DatasourcePayload {
  id?: number;
  datasourceCode: string;
  datasourceName: string;
  datasourceType: string;
  jdbcUrl: string;
  username: string;
  passwordCipher?: string;
  status?: number;
  ownerUserId?: number;
  remark?: string;
}

export function getDatasourceTypesApi(): Promise<DatasourceTypeOption[]> {
  return http.get<DatasourceTypeOption[]>("/archive/datasources/types");
}

export function getDatasourcesApi(query: DatasourceQuery): Promise<DatasourcePagedResult> {
  const params = new URLSearchParams();
  params.set("page", String(query.page));
  params.set("size", String(query.size));
  if (query.keyword) params.set("keyword", encodeURIComponent(query.keyword));
  if (query.status != null) params.set("status", String(query.status));
  return http.get<DatasourcePagedResult>(`/archive/datasources/page?${params.toString()}`);
}

export function createDatasourceApi(payload: DatasourcePayload): Promise<Datasource> {
  return http.post<Datasource>("/archive/datasources", payload);
}

export function updateDatasourceApi(id: number, payload: DatasourcePayload): Promise<Datasource> {
  return http.put<Datasource>(`/archive/datasources/${id}`, payload);
}

export function updateDatasourceStatusApi(id: number, status: number): Promise<void> {
  return http.patch<void>(`/archive/datasources/${id}/status?status=${status}`);
}

export function testDatasourceConnectionApi(payload: DatasourcePayload): Promise<boolean> {
  return http.post<boolean>("/archive/datasources/test", payload);
}
