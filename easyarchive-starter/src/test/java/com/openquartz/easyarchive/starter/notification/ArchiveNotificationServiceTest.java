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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveNotificationServiceTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
    private final NotificationMessageBuilder messageBuilder = mock(NotificationMessageBuilder.class);
    private final NotificationClient feishuClient = mock(NotificationClient.class);
    private final NotificationClient wecomClient = mock(NotificationClient.class);

    private final ArchiveNotificationService service = new ArchiveNotificationService(
            groupMapper,
            sysUserMapper,
            archiveLogRepository,
            messageBuilder,
            Arrays.asList(feishuClient, wecomClient)
    );

    @Test
    void shouldSkipWhenNotificationDisabled() {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        group.setNotifyEnabled(0);
        when(groupMapper.selectById(10L)).thenReturn(group);

        service.notifyTaskCompletion(new TaskEndEvent(100L, 10L, true, 88L, 5000L, null));

        verify(messageBuilder, never()).build(any(), any(), any(), any(), any());
        verify(feishuClient, never()).send(any(), any());
    }

    @Test
    void shouldSendRenderedMessageToMatchedClient() {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        group.setOwnerUserId(2L);
        group.setNotifyEnabled(1);
        group.setNotifyChannel("FEISHU");
        group.setNotifyWebhookUrl("https://open.feishu.cn/open-apis/bot/hook/test");
        when(groupMapper.selectById(10L)).thenReturn(group);

        SysUser owner = new SysUser();
        owner.setId(2L);
        owner.setRealName("Alice");
        when(sysUserMapper.selectById(2L)).thenReturn(owner);

        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(100L);
        when(archiveLogRepository.queryTaskById(100L)).thenReturn(task);

        ArchiveTaskLog detail = new ArchiveTaskLog();
        detail.setLogContent("规则完成:src_order -> arc_order, 处理:10行, 耗时:200ms");
        when(archiveLogRepository.queryLogsByTaskId(100L, 1, 1000, "RULE_END"))
                .thenReturn(Collections.singletonList(detail));

        ArchiveNotification notification = new ArchiveNotification();
        when(messageBuilder.build(any(), eq(task), eq(group), eq(owner), any())).thenReturn(notification);
        when(messageBuilder.render(notification, NotificationChannelEnum.FEISHU)).thenReturn("payload");
        when(feishuClient.getChannel()).thenReturn(NotificationChannelEnum.FEISHU);
        when(wecomClient.getChannel()).thenReturn(NotificationChannelEnum.WECOM);

        service.notifyTaskCompletion(new TaskEndEvent(100L, 10L, true, 10L, 200L, null));

        verify(feishuClient).send("https://open.feishu.cn/open-apis/bot/hook/test", "payload");
        verify(wecomClient, never()).send(any(), any());
    }
}
