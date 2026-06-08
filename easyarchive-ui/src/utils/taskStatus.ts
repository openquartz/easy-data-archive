const TERMINAL_TASK_STATUSES = new Set([2, 3, 5]);

export function isTaskTerminalStatus(status?: number): boolean {
  return typeof status === "number" && TERMINAL_TASK_STATUSES.has(status);
}
