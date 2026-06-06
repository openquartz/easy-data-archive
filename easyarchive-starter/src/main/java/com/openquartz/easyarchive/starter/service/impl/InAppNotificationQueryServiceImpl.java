package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.mapper.InAppNotificationMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationRecipientMapper;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationListItem;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationRecipientView;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationUnreadCountView;
import com.openquartz.easyarchive.starter.model.entity.InAppNotification;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.service.InAppNotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InAppNotificationQueryServiceImpl implements InAppNotificationQueryService {

    private static final int DEFAULT_LIST_LIMIT = 20;
    private static final int MAX_LIST_LIMIT = 50;

    private final InAppNotificationMapper notificationMapper;
    private final InAppNotificationRecipientMapper recipientMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    public InAppNotificationUnreadCountView getUnreadCount() {
        Long userId = dataPermissionService.getCurrentUser().getUserId();
        return new InAppNotificationUnreadCountView(recipientMapper.countUnreadByUserId(userId));
    }

    @Override
    public List<InAppNotificationListItem> listLatest(int limit) {
        Long userId = dataPermissionService.getCurrentUser().getUserId();
        List<InAppNotificationRecipientView> recipients = recipientMapper.selectLatestByUserId(userId, normalizeLimit(limit));
        if (recipients.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> notificationIds = recipients.stream()
                .map(InAppNotificationRecipientView::getNotificationId)
                .collect(Collectors.toList());
        Map<Long, InAppNotification> notifications = notificationMapper.selectByIds(notificationIds)
                .stream()
                .collect(Collectors.toMap(InAppNotification::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<InAppNotificationListItem> result = new ArrayList<>(recipients.size());
        for (InAppNotificationRecipientView recipient : recipients) {
            InAppNotification notification = notifications.get(recipient.getNotificationId());
            if (notification == null) {
                continue;
            }
            InAppNotificationListItem item = new InAppNotificationListItem();
            item.setNotificationId(notification.getId());
            item.setTitle(notification.getTitle());
            item.setSummary(notification.getContentSummary());
            item.setTaskStatus(notification.getTaskStatus());
            item.setGroupId(notification.getGroupId());
            item.setGroupName(notification.getGroupName());
            item.setTaskId(notification.getTaskId());
            item.setReadStatus(recipient.getReadStatus());
            item.setCreatedTime(recipient.getCreatedTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public void markRead(Long notificationId) {
        Long userId = dataPermissionService.getCurrentUser().getUserId();
        recipientMapper.markRead(notificationId, userId);
    }

    @Override
    public void markAllRead() {
        Long userId = dataPermissionService.getCurrentUser().getUserId();
        recipientMapper.markAllRead(userId);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIST_LIMIT;
        }
        return Math.min(limit, MAX_LIST_LIMIT);
    }
}
