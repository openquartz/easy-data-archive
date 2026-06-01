import { http } from "../utils/http";

export interface Datasource {
  id: number;
  datasourceCode: string;
  datasourceName: string;
  datasourceType: string;
  jdbcUrl: string;
  username: string;
  schemaName?: string;
  status: number;
  lastCheckTime?: string;
  ownerUserId?: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface DatasourcePayload {
  datasourceCode: string;
  datasourceName: string;
  datasourceType: string;
  jdbcUrl: string;
  username: string;
  passwordCipher?: string;
  schemaName?: string;
  status?: number;
  ownerUserId?: number;
  remark?: string;
}

export function getDatasourcesApi(): Promise<Datasource[]> {
  return http.get<Datasource[]>("/archive/datasources");
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
