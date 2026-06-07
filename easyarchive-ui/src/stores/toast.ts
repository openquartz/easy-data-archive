import { readonly, ref } from "vue";

export type ToastType = "success" | "error";

export interface ToastItem {
  id: number;
  type: ToastType;
  message: string;
  durationMs: number;
}

const toasts = ref<ToastItem[]>([]);
const timers = new Map<number, ReturnType<typeof setTimeout>>();
let seed = 0;

function clearToastTimer(id: number): void {
  const timer = timers.get(id);
  if (!timer) {
    return;
  }
  clearTimeout(timer);
  timers.delete(id);
}

export function removeToast(id: number): void {
  clearToastTimer(id);
  toasts.value = toasts.value.filter((item) => item.id !== id);
}

function pushToast(type: ToastType, message: string, durationMs = 5000): number {
  const id = ++seed;
  toasts.value = [...toasts.value, { id, type, message, durationMs }];
  timers.set(id, setTimeout(() => removeToast(id), durationMs));
  return id;
}

export function showSuccessToast(message: string, durationMs = 5000): number {
  return pushToast("success", message, durationMs);
}

export function showErrorToast(message: string, durationMs = 5000): number {
  return pushToast("error", message, durationMs);
}

export function clearToasts(): void {
  const ids = Array.from(timers.keys());
  for (const id of ids) {
    removeToast(id);
  }
  toasts.value = [];
}

export function getToastSnapshot(): ToastItem[] {
  return [...toasts.value];
}

export const toastStore = readonly(toasts);
