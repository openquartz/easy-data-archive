import { http } from "../utils/http";

export interface TaskItem {
  id: number;
  groupId: number;
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
}

export interface TaskLogQuery {
  page: number;
  size: number;
  executePhase?: string;
}

export function getTasksApi(query: TaskQuery): Promise<PagedResult<TaskItem>> {
  const status = query.status ? `&status=${encodeURIComponent(query.status)}` : "";
  return http.get<PagedResult<TaskItem>>(`/task-log/tasks?page=${query.page}&size=${query.size}${status}`);
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
