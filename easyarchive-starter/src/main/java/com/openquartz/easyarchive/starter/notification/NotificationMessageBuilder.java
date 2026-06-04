package com.openquartz.easyarchive.starter.notification;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.model.enums.NotificationChannelEnum;
import com.openquartz.easyarchive.starter.notification.model.ArchiveNotification;
import com.openquartz.easyarchive.starter.notification.model.ArchiveNotificationDetail;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationMessageBuilder {

    private static final Pattern SUCCESS_PATTERN = Pattern.compile("规则完成:(.+?) -> (.+?), 处理:(\\d+)行, 耗时:(\\d+)ms");
    private static final Pattern FAILED_PATTERN = Pattern.compile("规则失败:(.+?) -> (.+?), (.+)");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ArchiveNotification build(TaskEndEvent event,
                                     ArchiveGroupExecuteTask task,
                                     ArchiveGroup group,
                                     SysUser owner,
                                     List<ArchiveTaskLog> detailLogs) {
        ArchiveNotification notification = new ArchiveNotification();
        notification.setTaskId(task == null ? event.getTaskId() : task.getId());
        notification.setStatus(resolveStatus(event));
        notification.setStartedAt(task == null || task.getStartTime() == null ? "-" : DATETIME_FORMAT.format(task.getStartTime()));
        notification.setEndedAt(task == null || task.getEndTime() == null ? "-" : DATETIME_FORMAT.format(task.getEndTime()));
        notification.setGroupCode(group == null ? "-" : safe(group.getGroupCode()));
        notification.setGroupName(group == null ? "-" : safe(group.getGroupName()));
        notification.setGroupRemark(group == null ? "-" : defaultText(group.getRemark(), "-"));
        notification.setTotalRows(task == null || task.getProcessedRecords() == null ? event.getTotalRows() : task.getProcessedRecords());
        notification.setOwnerName(owner == null ? "未知" : defaultText(owner.getRealName(), "未知"));
        notification.setReason(defaultText(event.getErrorMsg(), null));
        if (detailLogs != null) {
            for (ArchiveTaskLog detailLog : detailLogs) {
                notification.getDetails().add(parseDetail(detailLog));
            }
        }
        return notification;
    }

    public String render(ArchiveNotification notification, NotificationChannelEnum channel) {
        StringBuilder builder = new StringBuilder();
        builder.append("归档分组执行通知").append('\n')
                .append("执行任务 ID: ").append(notification.getTaskId()).append('\n')
                .append("执行状态: ").append(notification.getStatus()).append('\n')
                .append("开始时间: ").append(notification.getStartedAt()).append('\n')
                .append("结束时间: ").append(notification.getEndedAt()).append('\n')
                .append("归档分组编码: ").append(notification.getGroupCode()).append('\n')
                .append("归档分组名称: ").append(notification.getGroupName()).append('\n')
                .append("分组描述: ").append(notification.getGroupRemark()).append('\n')
                .append("归档分组总行数: ").append(notification.getTotalRows()).append('\n')
                .append("负责人: ").append(notification.getOwnerName()).append('\n');
        if (notification.getReason() != null && !notification.getReason().isEmpty()) {
            builder.append("原因: ").append(notification.getReason()).append('\n');
        }
        builder.append("执行明细:").append('\n');
        for (ArchiveNotificationDetail detail : notification.getDetails()) {
            builder.append("- ")
                    .append(detail.getSourceTable()).append(" -> ").append(detail.getTargetTable())
                    .append(" | 行数: ").append(detail.getProcessedRows())
                    .append(" | 耗时: ").append(detail.getElapsedMs()).append("ms")
                    .append(" | 结果: ").append(detail.getStatus());
            if (detail.getReason() != null && !detail.getReason().isEmpty()) {
                builder.append(" | 原因: ").append(detail.getReason());
            }
            builder.append('\n');
        }
        return channel == NotificationChannelEnum.WECOM ? builder.toString().trim() : builder.toString().trim();
    }

    private ArchiveNotificationDetail parseDetail(ArchiveTaskLog detailLog) {
        ArchiveNotificationDetail detail = new ArchiveNotificationDetail();
        String content = detailLog.getLogContent() == null ? "" : detailLog.getLogContent();
        Matcher successMatcher = SUCCESS_PATTERN.matcher(content);
        if (successMatcher.matches()) {
            detail.setSourceTable(successMatcher.group(1).trim());
            detail.setTargetTable(successMatcher.group(2).trim());
            detail.setProcessedRows(Long.parseLong(successMatcher.group(3)));
            detail.setElapsedMs(Long.parseLong(successMatcher.group(4)));
            detail.setStatus("SUCCESS");
            return detail;
        }
        Matcher failedMatcher = FAILED_PATTERN.matcher(content);
        if (failedMatcher.matches()) {
            detail.setSourceTable(failedMatcher.group(1).trim());
            detail.setTargetTable(failedMatcher.group(2).trim());
            detail.setProcessedRows(detailLog.getProcessedCount());
            detail.setElapsedMs(0L);
            detail.setStatus("FAILED");
            detail.setReason(failedMatcher.group(3).trim());
            return detail;
        }
        detail.setSourceTable("未知");
        detail.setTargetTable("未知");
        detail.setProcessedRows(detailLog.getProcessedCount());
        detail.setElapsedMs(0L);
        detail.setStatus(defaultText(detailLog.getLogType(), "UNKNOWN").toUpperCase(Locale.ROOT));
        detail.setReason(content);
        return detail;
    }

    private String resolveStatus(TaskEndEvent event) {
        if (event.isCancelled()) {
            return "CANCELLED";
        }
        return event.isSuccess() ? "SUCCESS" : "FAILED";
    }

    private String safe(String value) {
        return defaultText(value, "-");
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
