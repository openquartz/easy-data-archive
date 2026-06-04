import { http } from "../utils/http";

export interface OperationLogItem {
  id: number;
  operateTime?: string;
  operator?: string;
  operatorName?: string;
  operatorUsername?: string;
  moduleCode?: string;
  moduleName?: string;
  buttonName?: string;
  resultStatus?: string | number | boolean;
  content?: string;
  errorMessage?: string;
  failureReason?: string;
}

export interface OperationLogQuery {
  page: number;
  size: number;
  startTime?: string;
  endTime?: string;
  operator?: string;
  moduleCode?: string;
  resultStatus?: string;
}

export interface PagedOperationLogResult {
  list: OperationLogItem[];
  total: number;
  page: number;
  size: number;
}

export function getOperationLogsApi(query: OperationLogQuery): Promise<PagedOperationLogResult> {
  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size)
  });

  if (query.startTime) {
    params.set("startTime", query.startTime);
  }
  if (query.endTime) {
    params.set("endTime", query.endTime);
  }
  if (query.operator) {
    params.set("operator", query.operator);
  }
  if (query.moduleCode) {
    params.set("moduleCode", query.moduleCode);
  }
  if (query.resultStatus) {
    params.set("resultStatus", query.resultStatus);
  }

  return http.get<PagedOperationLogResult>(`/system/logs?${params.toString()}`);
}
