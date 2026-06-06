export type TaskLogConsoleTone = "success" | "warning" | "danger" | "neutral";

export function getTaskLogConsoleLevelTone(level?: string): TaskLogConsoleTone {
  switch ((level || "").toUpperCase()) {
    case "INFO":
      return "success";
    case "WARN":
      return "warning";
    case "ERROR":
      return "danger";
    default:
      return "neutral";
  }
}

export function formatTaskLogConsoleMeta(logType?: string, executePhase?: string): string {
  const segments = [logType, executePhase].filter((value): value is string => Boolean(value && value.trim()));
  return segments.length ? segments.join(" / ") : "-";
}
