package com.openquartz.easyarchive.starter.notification.inapp;

import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.model.entity.InAppNotification;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationBizTypeEnum;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationCategoryEnum;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationLevelEnum;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InAppNotificationMessageBuilderTest {

    private final InAppNotificationMessageBuilder builder = new InAppNotificationMessageBuilder();

    @Test
    void shouldBuildGenericSuccessNotification() {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");

        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(100L);
        task.setStartTime(new Date(1717488000000L));
        task.setEndTime(new Date(1717488060000L));
        task.setProcessedRecords(88L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_SUCCESS);

        TaskEndEvent event = new TaskEndEvent(100L, 10L, true, 88L, 60000L, null);

        InAppNotification notification = builder.build(event, task, group);

        assertEquals(InAppNotificationBizTypeEnum.ARCHIVE_GROUP_TASK.name(), notification.getBizType());
        assertEquals(InAppNotificationCategoryEnum.TASK_RESULT.name(), notification.getCategory());
        assertEquals(InAppNotificationLevelEnum.INFO.name(), notification.getLevel());
        assertEquals(100L, notification.getBizId());
        assertTrue(notification.getTitle().contains("Order Archive"));
        assertTrue(notification.getContentSummary().contains("任务 #100"));
        assertTrue(notification.getPayloadJson().contains("ORDER_ARCHIVE"));
    }
}
