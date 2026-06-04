package com.openquartz.easyarchive.starter.notification;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.enums.NotificationChannelEnum;
import com.openquartz.easyarchive.starter.notification.model.ArchiveNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveNotificationService {

    private final ArchiveGroupMapper groupMapper;
    private final SysUserMapper sysUserMapper;
    private final ArchiveLogRepository archiveLogRepository;
    private final NotificationMessageBuilder messageBuilder;
    private final List<NotificationClient> notificationClients;

    public void notifyTaskCompletion(TaskEndEvent event) {
        ArchiveGroup group = groupMapper.selectById(event.getGroupId());
        if (group == null || group.getNotifyEnabled() == null || group.getNotifyEnabled() != 1) {
            return;
        }
        if (!NotificationChannelEnum.supports(group.getNotifyChannel())
                || group.getNotifyWebhookUrl() == null
                || group.getNotifyWebhookUrl().trim().isEmpty()) {
            log.warn("[ArchiveNotificationService] skip invalid notification config, groupId:{}", event.getGroupId());
            return;
        }

        ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(event.getTaskId());
        SysUser owner = group.getOwnerUserId() == null ? null : sysUserMapper.selectById(group.getOwnerUserId());
        List<ArchiveTaskLog> details = archiveLogRepository.queryLogsByTaskId(event.getTaskId(), 1, 1000, "RULE_END");
        if (details == null) {
            details = Collections.emptyList();
        }

        NotificationChannelEnum channel = NotificationChannelEnum.valueOf(group.getNotifyChannel().trim().toUpperCase());
        ArchiveNotification notification = messageBuilder.build(event, task, group, owner, details);
        String message = messageBuilder.render(notification, channel);
        NotificationClient client = resolveClient(channel);
        if (client == null) {
            log.warn("[ArchiveNotificationService] notification client not found, channel:{}", channel);
            return;
        }
        client.send(group.getNotifyWebhookUrl().trim(), message);
    }

    private NotificationClient resolveClient(NotificationChannelEnum channel) {
        for (NotificationClient client : notificationClients) {
            if (client.getChannel() == channel) {
                return client;
            }
        }
        return null;
    }
}
