import { http } from "../utils/http";

export type ArchiveGroupItemType = "ID" | "TIME";

export interface ArchiveGroupItemSummary {
  itemType: ArchiveGroupItemType;
  id: number;
  groupId: number;
  sourceTable: string;
  targetTable: string;
  rangeStart?: string;
  rangeEnd?: string;
  priority: number;
  stepCount?: number;
  enableWrite?: number;
  enableClean?: number;
  enableStatus?: number;
}

export interface ArchiveGroupItemById {
  id: number;
  sourceTable: string;
  targetTable: string;
  groupId: number;
  priority: number;
  fetchSql: string;
  deleteWhere?: string;
  startId: string;
  endId: string;
  stepCount: number;
  stepRounds: number;
  pauseMs?: number;
  enableClean: number;
  enableWrite: number;
  enableStatus: number;
  idColumn: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface ArchiveGroupItemByTime {
  id: number;
  sourceTable: string;
  targetTable: string;
  groupId: number;
  priority: number;
  fetchSql: string;
  deleteWhere?: string;
  startTime: string;
  keepDay: number;
  stepMinutes: number;
  stepCount: number;
  pauseMs?: number;
  enableClean: number;
  enableWrite: number;
  enableStatus: number;
  idColumn: string;
  createdTime?: string;
  updatedTime?: string;
}

export type ArchiveGroupItemByIdPayload = Omit<ArchiveGroupItemById, "id" | "groupId" | "createdTime" | "updatedTime">;
export type ArchiveGroupItemByTimePayload = Omit<
  ArchiveGroupItemByTime,
  "id" | "groupId" | "createdTime" | "updatedTime"
>;

export function getArchiveGroupItemsApi(groupId: number, enableStatus?: number): Promise<ArchiveGroupItemSummary[]> {
  const query = enableStatus === undefined ? "" : `?enableStatus=${enableStatus}`;
  return http.get<ArchiveGroupItemSummary[]>(`/archive/groups/${groupId}/items${query}`);
}

export function getArchiveGroupItemByIdApi(groupId: number, itemId: number): Promise<ArchiveGroupItemById> {
  return http.get<ArchiveGroupItemById>(`/archive/groups/${groupId}/items/id/${itemId}`);
}

export function createArchiveGroupItemByIdApi(
  groupId: number,
  payload: ArchiveGroupItemByIdPayload
): Promise<ArchiveGroupItemById> {
  return http.post<ArchiveGroupItemById>(`/archive/groups/${groupId}/items/id`, payload);
}

export function updateArchiveGroupItemByIdApi(
  groupId: number,
  itemId: number,
  payload: ArchiveGroupItemByIdPayload
): Promise<ArchiveGroupItemById> {
  return http.put<ArchiveGroupItemById>(`/archive/groups/${groupId}/items/id/${itemId}`, payload);
}

export function updateArchiveGroupItemByIdStatusApi(
  groupId: number,
  itemId: number,
  enableStatus: number
): Promise<void> {
  return http.patch<void>(`/archive/groups/${groupId}/items/id/${itemId}/status?enableStatus=${enableStatus}`);
}

export function deleteArchiveGroupItemByIdApi(groupId: number, itemId: number): Promise<void> {
  return http.delete<void>(`/archive/groups/${groupId}/items/id/${itemId}`);
}

export function getArchiveGroupItemByTimeApi(groupId: number, itemId: number): Promise<ArchiveGroupItemByTime> {
  return http.get<ArchiveGroupItemByTime>(`/archive/groups/${groupId}/items/time/${itemId}`);
}

export function createArchiveGroupItemByTimeApi(
  groupId: number,
  payload: ArchiveGroupItemByTimePayload
): Promise<ArchiveGroupItemByTime> {
  return http.post<ArchiveGroupItemByTime>(`/archive/groups/${groupId}/items/time`, payload);
}

export function updateArchiveGroupItemByTimeApi(
  groupId: number,
  itemId: number,
  payload: ArchiveGroupItemByTimePayload
): Promise<ArchiveGroupItemByTime> {
  return http.put<ArchiveGroupItemByTime>(`/archive/groups/${groupId}/items/time/${itemId}`, payload);
}

export function updateArchiveGroupItemByTimeStatusApi(
  groupId: number,
  itemId: number,
  enableStatus: number
): Promise<void> {
  return http.patch<void>(`/archive/groups/${groupId}/items/time/${itemId}/status?enableStatus=${enableStatus}`);
}

export function deleteArchiveGroupItemByTimeApi(groupId: number, itemId: number): Promise<void> {
  return http.delete<void>(`/archive/groups/${groupId}/items/time/${itemId}`);
}
