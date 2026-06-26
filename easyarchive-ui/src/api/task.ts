import { http } from "../utils/http";

export interface TaskItem {
  id: number;
  groupId: number;
  groupName?: string;
  startTime?: string;
  endTime?: string;
  executeStatus: number;
  errorMsg?: string;
  processedRecords?: number;
  processedSpeed?: number;
  heartbeatTime?: string;
  finishedFlag?: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface TaskLogItem {
  id: number;
  taskId: number;
  logType?: string;
  logLevel?: string;
  logContent?: string;
  logTime?: string;
  processedCount?: number;
  processSpeed?: number;
  executePhase?: string;
  createdTime?: string;
  updatedTime?: string;
}

export interface PagedResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export interface TaskQuery {
  page: number;
  size: number;
  status?: string;
  groupId?: number;
}

export interface TaskLogQuery {
  page: number;
  size: number;
  executePhase?: string;
}

export function getTasksApi(query: TaskQuery): Promise<PagedResult<TaskItem>> {
  const params = new URLSearchParams();
  params.set("page", String(query.page));
  params.set("size", String(query.size));
  if (query.status) params.set("status", encodeURIComponent(query.status));
  if (query.groupId != null) params.set("groupId", String(query.groupId));
  return http.get<PagedResult<TaskItem>>(`/task-log/tasks?${params.toString()}`);
}

export interface ArchiveGroupOption {
  id: number;
  name: string;
}

export interface ArchiveGroupSearchOption {
  id: number;
  name: string;
}

export function getArchiveGroupOptionsApi(): Promise<ArchiveGroupOption[]> {
  return http.get<ArchiveGroupOption[]>("/archive/groups/options");
}

export function searchArchiveGroupsApi(keyword: string): Promise<ArchiveGroupSearchOption[]> {
  const params = new URLSearchParams();
  params.set("page", "1");
  params.set("size", "20");
  if (keyword) params.set("keyword", keyword);
  return http.get<{ list: ArchiveGroupSearchOption[] }>(`/archive/groups/page?${params.toString()}`)
    .then((result) => result.list || []);
}

export function getTaskDetailApi(taskId: number): Promise<TaskItem> {
  return http.get<TaskItem>(`/task-log/tasks/${taskId}`);
}

export function getTaskLogsApi(taskId: number, query: TaskLogQuery): Promise<PagedResult<TaskLogItem>> {
  const executePhase = query.executePhase ? `&executePhase=${encodeURIComponent(query.executePhase)}` : "";
  return http.get<PagedResult<TaskLogItem>>(
    `/task-log/tasks/${taskId}/logs?page=${query.page}&size=${query.size}${executePhase}`
  );
}

export function cancelTaskApi(taskId: number, cancelReason?: string): Promise<string> {
  return http.post<string>(`/task-log/tasks/${taskId}/cancel`, { cancelReason });
}
