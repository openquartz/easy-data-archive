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
  ownerDisplayName?: string;
  enableStatus: number;
  notifyEnabled?: number;
  notifyChannel?: "IN_APP" | "FEISHU" | "WECOM";
  notifyWebhookUrl?: string;
  remark?: string;
  activeTaskId?: number;
  activeTaskStatus?: number;
  activeTaskStartTime?: string;
  activeTaskProcessedRecords?: number;
  activeTaskProcessedSpeed?: number;
  activeTaskHeartbeatTime?: string;
  canTrigger?: boolean;
  canCancelActiveTask?: boolean;
  canViewActiveTask?: boolean;
  createdTime?: string;
  updatedTime?: string;
}

export interface ArchiveGroupItemStats {
  totalCount: number;
  enabledCount: number;
  disabledCount: number;
  idTypeCount: number;
  timeTypeCount: number;
}

export interface ArchiveGroupTaskStats {
  totalCount: number;
  successCount: number;
  failedCount: number;
  runningCount: number;
  lastExecuteStatus?: number;
  lastExecuteTime?: number;
}

export interface ArchiveGroupOverview {
  group: ArchiveGroup;
  itemStats: ArchiveGroupItemStats;
  taskStats: ArchiveGroupTaskStats;
  recentTasks: TaskItem[];
}

export type ArchiveGroupPayload = Omit<
  ArchiveGroup,
  | "id"
  | "activeTaskId"
  | "activeTaskStatus"
  | "activeTaskStartTime"
  | "activeTaskProcessedRecords"
  | "activeTaskProcessedSpeed"
  | "activeTaskHeartbeatTime"
  | "canTrigger"
  | "canCancelActiveTask"
  | "canViewActiveTask"
  | "createdTime"
  | "updatedTime"
  | "ownerDisplayName"
>;

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

export function getArchiveGroupOverviewApi(id: number): Promise<ArchiveGroupOverview> {
  return http.get<ArchiveGroupOverview>(`/archive/groups/${id}/overview`);
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

export function cancelArchiveGroupActiveTaskApi(id: number, cancelReason?: string): Promise<TaskItem> {
  return http.post<TaskItem>(`/archive/groups/${id}/cancel-active-task`, { cancelReason });
}

export interface PageResult<T> {
  data: T[];
  total: number;
  page: number;
  size: number;
}

export interface ArchiveGroupPageParams {
  page?: number;
  size?: number;
  enableStatus?: number;
  keyword?: string;
  ownerUserId?: number;
}

export function getArchiveGroupsPageApi(params: ArchiveGroupPageParams): Promise<PageResult<ArchiveGroup>> {
  const query = new URLSearchParams();
  if (params.page != null) query.set("page", String(params.page));
  if (params.size != null) query.set("size", String(params.size));
  if (params.enableStatus !== undefined) query.set("enableStatus", String(params.enableStatus));
  if (params.keyword) query.set("keyword", params.keyword);
  if (params.ownerUserId != null) query.set("ownerUserId", String(params.ownerUserId));
  return http.get<PageResult<ArchiveGroup>>(`/archive/groups/page?${query.toString()}`);
}
