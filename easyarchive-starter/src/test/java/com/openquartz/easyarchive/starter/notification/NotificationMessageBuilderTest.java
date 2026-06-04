package com.openquartz.easyarchive.starter.notification;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.model.enums.NotificationChannelEnum;
import com.openquartz.easyarchive.starter.notification.model.ArchiveNotification;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationMessageBuilderTest {

    private final NotificationMessageBuilder builder = new NotificationMessageBuilder();

    @Test
    void shouldBuildAndRenderSuccessNotification() {
        ArchiveGroup group = new ArchiveGroup();
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");
        group.setRemark("daily cleanup");

        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(100L);
        task.setStartTime(new Date(1717488000000L));
        task.setEndTime(new Date(1717488060000L));
        task.setProcessedRecords(88L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_SUCCESS);

        SysUser owner = new SysUser();
        owner.setRealName("Alice");

        ArchiveTaskLog detail = new ArchiveTaskLog();
        detail.setLogContent("规则完成:src_order -> arc_order, 处理:88行, 耗时:60000ms");
        detail.setProcessedCount(88L);

        TaskEndEvent event = new TaskEndEvent(100L, 10L, true, 88L, 60000L, null);

        ArchiveNotification notification = builder.build(event, task, group, owner, Arrays.asList(detail));
        String message = builder.render(notification, NotificationChannelEnum.FEISHU);

        assertEquals("SUCCESS", notification.getStatus());
        assertEquals("ORDER_ARCHIVE", notification.getGroupCode());
        assertEquals(1, notification.getDetails().size());
        assertTrue(message.contains("执行任务 ID: 100"));
        assertTrue(message.contains("归档分组编码: ORDER_ARCHIVE"));
        assertTrue(message.contains("负责人: Alice"));
        assertTrue(message.contains("src_order -> arc_order"));
    }
}
