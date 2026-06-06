package com.openquartz.easyarchive.starter.notification.inapp;

import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationPayload;
import com.openquartz.easyarchive.starter.model.entity.InAppNotification;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationBizTypeEnum;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationCategoryEnum;
import com.openquartz.easyarchive.starter.model.enums.InAppNotificationLevelEnum;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class InAppNotificationMessageBuilder {

    public InAppNotification build(TaskEndEvent event, ArchiveGroupExecuteTask task, ArchiveGroup group) {
        InAppNotification notification = new InAppNotification();
        notification.setBizType(InAppNotificationBizTypeEnum.ARCHIVE_GROUP_TASK.name());
        notification.setBizId(event.getTaskId());
        notification.setCategory(InAppNotificationCategoryEnum.TASK_RESULT.name());
        notification.setLevel(resolveLevel(event).name());
        notification.setGroupId(group == null ? event.getGroupId() : group.getId());
        notification.setGroupName(group == null ? "-" : defaultText(group.getGroupName(), "-"));
        notification.setTaskId(event.getTaskId());
        notification.setTaskStatus(resolveTaskStatus(event));
        notification.setTitle(buildTitle(group, event));
        notification.setContentSummary(buildSummary(event, task));
        notification.setPayloadJson(toPayloadJson(event, task, group));
        notification.setSourceTime(resolveSourceTime(event, task));
        return notification;
    }

    private String buildTitle(ArchiveGroup group, TaskEndEvent event) {
        String groupName = group == null ? "未知分组" : defaultText(group.getGroupName(), "未知分组");
        if (event.isCancelled()) {
            return "归档分组 " + groupName + " 已取消";
        }
        return "归档分组 " + groupName + (event.isSuccess() ? " 执行成功" : " 执行失败");
    }

    private String buildSummary(TaskEndEvent event, ArchiveGroupExecuteTask task) {
        if (event.isCancelled()) {
            return "任务 #" + event.getTaskId() + " 已取消，累计处理 " + resolveRowCount(event, task) + " 行";
        }
        if (event.isSuccess()) {
            return "任务 #" + event.getTaskId() + " 已完成，归档 " + resolveRowCount(event, task) + " 行";
        }
        return "任务 #" + event.getTaskId() + " 执行失败：" + defaultText(event.getErrorMsg(), "未知原因");
    }

    private String toPayloadJson(TaskEndEvent event, ArchiveGroupExecuteTask task, ArchiveGroup group) {
        InAppNotificationPayload payload = new InAppNotificationPayload();
        payload.setGroupId(group == null ? event.getGroupId() : group.getId());
        payload.setGroupCode(group == null ? null : group.getGroupCode());
        payload.setGroupName(group == null ? null : group.getGroupName());
        payload.setTaskId(event.getTaskId());
        payload.setTaskStatus(resolveTaskStatus(event));
        payload.setTotalRows(resolveRowCount(event, task));
        payload.setElapsedMs(event.getElapsedMs());
        payload.setSourceTimestamp(resolveSourceTime(event, task).getTime());

        return "{"
                + "\"groupId\":" + payload.getGroupId() + ","
                + "\"groupCode\":\"" + escape(payload.getGroupCode()) + "\","
                + "\"groupName\":\"" + escape(payload.getGroupName()) + "\","
                + "\"taskId\":" + payload.getTaskId() + ","
                + "\"taskStatus\":\"" + payload.getTaskStatus() + "\","
                + "\"totalRows\":" + payload.getTotalRows() + ","
                + "\"elapsedMs\":" + payload.getElapsedMs() + ","
                + "\"sourceTimestamp\":" + payload.getSourceTimestamp()
                + "}";
    }

    private Date resolveSourceTime(TaskEndEvent event, ArchiveGroupExecuteTask task) {
        if (task != null && task.getEndTime() != null) {
            return task.getEndTime();
        }
        return new Date(event.getTimestamp());
    }

    private Long resolveRowCount(TaskEndEvent event, ArchiveGroupExecuteTask task) {
        if (task != null && task.getProcessedRecords() != null) {
            return task.getProcessedRecords();
        }
        return event.getTotalRows();
    }

    private String resolveTaskStatus(TaskEndEvent event) {
        if (event.isCancelled()) {
            return "CANCELED";
        }
        return event.isSuccess() ? "SUCCESS" : "FAILED";
    }

    private InAppNotificationLevelEnum resolveLevel(TaskEndEvent event) {
        if (event.isCancelled()) {
            return InAppNotificationLevelEnum.WARN;
        }
        return event.isSuccess() ? InAppNotificationLevelEnum.INFO : InAppNotificationLevelEnum.ERROR;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
