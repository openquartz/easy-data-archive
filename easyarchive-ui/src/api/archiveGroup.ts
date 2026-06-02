import { http } from "../utils/http";
import type { TaskItem } from "./task";

export interface ArchiveGroup {
  id: number;
  parentId?: number;
  groupCode: string;
  groupName: string;
  groupPath?: string;
  groupLevel?: number;
  sourceDatasourceId: number;
  targetDatasourceId: number;
  ownerUserId?: number;
  enableStatus: number;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export type ArchiveGroupPayload = Omit<ArchiveGroup, "id" | "createdTime" | "updatedTime">;

export function getArchiveGroupsApi(enableStatus?: number): Promise<ArchiveGroup[]> {
  const query = enableStatus === undefined ? "" : `?enableStatus=${enableStatus}`;
  return http.get<ArchiveGroup[]>(`/archive/groups${query}`);
}

export function getArchiveGroupTreeApi(): Promise<ArchiveGroup[]> {
  return http.get<ArchiveGroup[]>("/archive/groups/tree");
}

export function getArchiveGroupApi(id: number): Promise<ArchiveGroup> {
  return http.get<ArchiveGroup>(`/archive/groups/${id}`);
}

export function createArchiveGroupApi(payload: ArchiveGroupPayload): Promise<ArchiveGroup> {
  return http.post<ArchiveGroup>("/archive/groups", payload);
}

export function updateArchiveGroupApi(id: number, payload: ArchiveGroupPayload): Promise<ArchiveGroup> {
  return http.put<ArchiveGroup>(`/archive/groups/${id}`, payload);
}

export function updateArchiveGroupStatusApi(id: number, enableStatus: number): Promise<void> {
  return http.patch<void>(`/archive/groups/${id}/status?enableStatus=${enableStatus}`);
}

export function deleteArchiveGroupApi(id: number): Promise<void> {
  return http.delete<void>(`/archive/groups/${id}`);
}

export function triggerArchiveGroupApi(id: number): Promise<TaskItem> {
  return http.post<TaskItem>(`/archive/groups/${id}/trigger`);
}
