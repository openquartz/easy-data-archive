package com.openquartz.easyarchive.starter.notification.inapp;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationRecipientMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.entity.InAppNotification;
import com.openquartz.easyarchive.starter.model.entity.InAppNotificationRecipient;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationBizTypeEnum;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationReadStatusEnum;
import com.openquartz.easyarchive.starter.model.enums.NotificationChannelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveInAppNotificationService {

    private static final int IN_APP_NOTIFY_ENABLED = 1;
    private static final int DELIVERED_STATUS = 1;

    private final ArchiveGroupMapper groupMapper;
    private final SysUserMapper sysUserMapper;
    private final InAppNotificationMapper notificationMapper;
    private final InAppNotificationRecipientMapper recipientMapper;
    private final ArchiveLogRepository archiveLogRepository;
    private final InAppNotificationMessageBuilder messageBuilder;

    public void notifyTaskCompletion(TaskEndEvent event) {
        ArchiveGroup group = groupMapper.selectById(event.getGroupId());
        if (!isInAppChannelEnabled(group)) {
            return;
        }

        SysUser owner = resolveEnabledOwner(group.getOwnerUserId());
        if (owner == null) {
            log.warn("[ArchiveInAppNotificationService] skip in-app notification because no valid recipients, groupId:{}, taskId:{}",
                    group.getId(), event.getTaskId());
            return;
        }

        ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(event.getTaskId());
        InAppNotification notification = messageBuilder.build(event, task, group);
        try {
            notificationMapper.insert(notification);
        } catch (DuplicateKeyException ex) {
            log.info("[ArchiveInAppNotificationService] in-app notification already exists, taskId:{}, groupId:{}",
                    event.getTaskId(), event.getGroupId());
            return;
        }

        recipientMapper.insertBatch(buildRecipients(notification.getId(), owner.getId()));
    }

    public void notifyOwnerChanged(ArchiveGroup before, ArchiveGroup after) {
        if (before == null || after == null) {
            return;
        }
        if (!isInAppChannelEnabled(after)) {
            return;
        }
        if (after.getOwnerUserId() == null || after.getOwnerUserId().equals(before.getOwnerUserId())) {
            return;
        }
        SysUser newOwner = resolveEnabledOwner(after.getOwnerUserId());
        if (newOwner == null) {
            return;
        }
        InAppNotification notification = buildOwnerChangedNotification(before, after);
        notificationMapper.insert(notification);
        recipientMapper.insertBatch(buildRecipients(notification.getId(), newOwner.getId()));
    }

    private boolean isInAppChannelEnabled(ArchiveGroup group) {
        return group != null
                && group.getNotifyEnabled() != null
                && group.getNotifyEnabled() == IN_APP_NOTIFY_ENABLED
                && NotificationChannelEnum.IN_APP.name().equalsIgnoreCase(group.getNotifyChannel());
    }

    private SysUser resolveEnabledOwner(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        SysUser owner = sysUserMapper.selectById(ownerUserId);
        if (owner == null || owner.getStatus() == null || owner.getStatus() != 0) {
            return null;
        }
        return owner;
    }

    private List<InAppNotificationRecipient> buildRecipients(Long notificationId, Long recipientUserId) {
        List<InAppNotificationRecipient> recipients = new ArrayList<>(1);
        InAppNotificationRecipient recipient = new InAppNotificationRecipient();
        recipient.setNotificationId(notificationId);
        recipient.setRecipientUserId(recipientUserId);
        recipient.setReadStatus(InAppNotificationReadStatusEnum.UNREAD.getCode());
        recipient.setDeliveryStatus(DELIVERED_STATUS);
        recipients.add(recipient);
        return recipients;
    }

    private InAppNotification buildOwnerChangedNotification(ArchiveGroup before, ArchiveGroup after) {
        InAppNotification notification = new InAppNotification();
        notification.setBizType(InAppNotificationBizTypeEnum.ARCHIVE_GROUP_OWNER_CHANGED.name());
        notification.setBizId(after.getId());
        notification.setCategory("GROUP_CHANGE");
        notification.setLevel("INFO");
        notification.setGroupId(after.getId());
        notification.setGroupName(after.getGroupName());
        notification.setTitle("归档分组负责人已变更");
        notification.setContentSummary("归档分组 " + defaultText(after.getGroupName(), "-")
                + " 的负责人已变更为你");
        notification.setPayloadJson("{\"groupId\":" + after.getId()
                + ",\"beforeOwnerUserId\":" + toJsonNumber(before.getOwnerUserId())
                + ",\"afterOwnerUserId\":" + toJsonNumber(after.getOwnerUserId()) + "}");
        notification.setSourceTime(new Date());
        return notification;
    }

    private String defaultText(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String toJsonNumber(Long value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
