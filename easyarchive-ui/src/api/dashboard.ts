import { http } from "../utils/http";

export interface DashboardStatusCount {
  status?: number;
  executeStatus?: number;
  count: number;
}

export interface DashboardDatasourceStatusCount {
  status: number;
  count: number;
}

export interface DashboardDatasourceSummary {
  total: number;
  enabled: number;
  disabled: number;
  statusCounts: DashboardDatasourceStatusCount[];
}

export interface DashboardTaskItem {
  id: number;
  groupId: number;
  executeStatus: number;
  startTime?: string;
  endTime?: string;
  errorMsg?: string;
  processedRecords?: number;
  processedSpeed?: number;
  createdTime?: string;
  updatedTime?: string;
}

export interface DashboardDailyTrendItem {
  day: string;
  submittedCount: number;
  successCount: number;
  failedCount: number;
}

export interface DashboardOverview {
  taskStatusCounts: DashboardStatusCount[];
  dailyTaskTrend: DashboardDailyTrendItem[];
  recentTasks: DashboardTaskItem[];
  failedTasks: DashboardTaskItem[];
  datasourceStatusSummary: DashboardDatasourceSummary;
}

export function getDashboardOverviewApi(): Promise<DashboardOverview> {
  return http.get<DashboardOverview>("/dashboard/overview");
}
