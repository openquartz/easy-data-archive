import { http } from "../utils/http";

export interface InAppNotificationListItem {
  notificationId: number;
  title: string;
  summary?: string;
  taskStatus?: string;
  groupId?: number;
  groupName?: string;
  taskId?: number;
  readStatus: number;
  createdTime?: string;
}

export interface InAppNotificationUnreadCount {
  unreadCount: number;
}

export function getInAppNotificationUnreadCountApi(): Promise<InAppNotificationUnreadCount> {
  return http.get<InAppNotificationUnreadCount>("/in-app-notifications/unread-count");
}

export function getInAppNotificationsApi(limit = 20): Promise<InAppNotificationListItem[]> {
  return http.get<InAppNotificationListItem[]>(`/in-app-notifications?limit=${limit}`);
}

export function markInAppNotificationReadApi(notificationId: number): Promise<void> {
  return http.post<void>(`/in-app-notifications/${notificationId}/read`);
}

export function markAllInAppNotificationsReadApi(): Promise<void> {
  return http.post<void>("/in-app-notifications/read-all");
}
