package com.openquartz.easyarchive.starter.notification.inapp;

import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationRecipientMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.entity.InAppNotification;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationBizTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveInAppNotificationServiceTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final InAppNotificationMapper notificationMapper = mock(InAppNotificationMapper.class);
    private final InAppNotificationRecipientMapper recipientMapper = mock(InAppNotificationRecipientMapper.class);
    private final ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
    private final InAppNotificationMessageBuilder messageBuilder = mock(InAppNotificationMessageBuilder.class);

    private final ArchiveInAppNotificationService service = new ArchiveInAppNotificationService(
            groupMapper,
            sysUserMapper,
            notificationMapper,
            recipientMapper,
            archiveLogRepository,
            messageBuilder
    );

    @Test
    void shouldCreateOneNotificationAndInboxRowForOwnerWhenUsingInAppChannel() {
        when(groupMapper.selectById(7L)).thenReturn(groupWithInAppChannel());
        when(sysUserMapper.selectById(11L)).thenReturn(enabledUser(11L));
        InAppNotification notification = new InAppNotification();
        notification.setId(101L);
        when(messageBuilder.build(any(), any(), any())).thenReturn(notification);

        service.notifyTaskCompletion(new TaskEndEvent(99L, 7L, true, 33L, 500L, null));

        verify(notificationMapper).insert(any(InAppNotification.class));
        verify(recipientMapper).insertBatch(argThat(list -> list.size() == 1
                && list.get(0).getRecipientUserId().equals(11L)));
    }

    @Test
    void shouldIgnoreDuplicateTaskTerminalEvent() {
        when(groupMapper.selectById(7L)).thenReturn(groupWithInAppChannel());
        when(sysUserMapper.selectById(11L)).thenReturn(enabledUser(11L));
        when(messageBuilder.build(any(), any(), any())).thenReturn(new InAppNotification());
        when(notificationMapper.insert(any(InAppNotification.class))).thenThrow(new DuplicateKeyException("dup"));

        assertDoesNotThrow(() -> service.notifyTaskCompletion(new TaskEndEvent(99L, 7L, true, 33L, 500L, null)));

        verify(recipientMapper, never()).insertBatch(anyList());
    }

    @Test
    void shouldSkipWhenChannelIsNotInApp() {
        ArchiveGroup group = groupWithInAppChannel();
        group.setNotifyChannel("FEISHU");
        when(groupMapper.selectById(7L)).thenReturn(group);

        service.notifyTaskCompletion(new TaskEndEvent(99L, 7L, true, 33L, 500L, null));

        verify(notificationMapper, never()).insert(any(InAppNotification.class));
        verify(recipientMapper, never()).insertBatch(anyList());
    }

    @Test
    void shouldCreateOwnerChangedNotificationForNewOwner() {
        ArchiveGroup before = groupWithInAppChannel();
        ArchiveGroup after = groupWithInAppChannel();
        after.setOwnerUserId(15L);
        when(sysUserMapper.selectById(15L)).thenReturn(enabledUser(15L));

        service.notifyOwnerChanged(before, after);

        verify(notificationMapper).insert(argThat(notification ->
                InAppNotificationBizTypeEnum.ARCHIVE_GROUP_OWNER_CHANGED.name().equals(notification.getBizType())
                        && notification.getBizId().equals(7L)
                        && notification.getTitle().contains("负责人")));
        verify(recipientMapper).insertBatch(argThat(list -> list.size() == 1
                && list.get(0).getRecipientUserId().equals(15L)));
    }

    private ArchiveGroup groupWithInAppChannel() {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(7L);
        group.setNotifyEnabled(1);
        group.setNotifyChannel("IN_APP");
        group.setOwnerUserId(11L);
        group.setGroupCode("archive_order");
        group.setGroupName("Archive Order");
        return group;
    }

    private SysUser enabledUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(0);
        return user;
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T argThat(org.mockito.ArgumentMatcher<T> matcher) {
        return org.mockito.ArgumentMatchers.argThat(matcher);
    }
}
