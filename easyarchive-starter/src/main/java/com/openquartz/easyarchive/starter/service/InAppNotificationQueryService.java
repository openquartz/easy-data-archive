package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.model.dto.InAppNotificationListItem;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationUnreadCountView;

import java.util.List;

public interface InAppNotificationQueryService {

    InAppNotificationUnreadCountView getUnreadCount();

    List<InAppNotificationListItem> listLatest(int limit);

    void markRead(Long notificationId);

    void markAllRead();
}
