package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.mapper.InAppNotificationMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationRecipientMapper;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationListItem;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationRecipientView;
import com.openquartz.easyarchive.starter.model.entity.InAppNotification;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppNotificationQueryServiceImplTest {

    @Mock
    private InAppNotificationMapper notificationMapper;

    @Mock
    private InAppNotificationRecipientMapper recipientMapper;

    @Mock
    private DataPermissionService dataPermissionService;

    @InjectMocks
    private InAppNotificationQueryServiceImpl service;

    @Test
    void shouldReturnLatestNotificationsForCurrentUser() {
        CurrentUserInfo user = new CurrentUserInfo();
        user.setUserId(9L);
        when(dataPermissionService.getCurrentUser()).thenReturn(user);

        InAppNotificationRecipientView unread = new InAppNotificationRecipientView();
        unread.setNotificationId(101L);
        unread.setReadStatus(0);
        InAppNotificationRecipientView read = new InAppNotificationRecipientView();
        read.setNotificationId(102L);
        read.setReadStatus(1);
        when(recipientMapper.selectLatestByUserId(9L, 20)).thenReturn(Arrays.asList(unread, read));

        InAppNotification first = new InAppNotification();
        first.setId(101L);
        first.setTitle("first");
        first.setContentSummary("first summary");
        first.setTaskStatus("FAILED");
        first.setGroupId(11L);
        first.setGroupName("g1");
        first.setTaskId(1001L);
        InAppNotification second = new InAppNotification();
        second.setId(102L);
        second.setTitle("second");
        second.setContentSummary("second summary");
        second.setTaskStatus("SUCCESS");
        second.setGroupId(12L);
        second.setGroupName("g2");
        second.setTaskId(1002L);
        when(notificationMapper.selectByIds(Arrays.asList(101L, 102L))).thenReturn(Arrays.asList(first, second));

        List<InAppNotificationListItem> result = service.listLatest(0);

        assertEquals(2, result.size());
        assertEquals(101L, result.get(0).getNotificationId());
        assertEquals("first", result.get(0).getTitle());
        assertEquals(Integer.valueOf(0), result.get(0).getReadStatus());
        assertEquals(102L, result.get(1).getNotificationId());
    }

    @Test
    void shouldClampRequestedListLimit() {
        CurrentUserInfo user = new CurrentUserInfo();
        user.setUserId(7L);
        when(dataPermissionService.getCurrentUser()).thenReturn(user);
        when(recipientMapper.selectLatestByUserId(eq(7L), anyInt())).thenReturn(Collections.emptyList());

        service.listLatest(999);

        verify(recipientMapper).selectLatestByUserId(7L, 50);
    }
}
