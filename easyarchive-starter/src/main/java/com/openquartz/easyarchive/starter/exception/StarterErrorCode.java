package com.openquartz.easyarchive.starter.exception;

import com.openquartz.easyarchive.common.exception.EasyArchiveErrorCode;
import lombok.Getter;

@Getter
public enum StarterErrorCode implements EasyArchiveErrorCode {

    CURRENT_USER_MISSING("01", "未获取到当前登录用户"),
    CURRENT_USER_NOT_FOUND("02", "当前登录用户不存在"),
    ADMIN_PERMISSION_REQUIRED("03", "无管理员权限"),
    DATASOURCE_ID_REQUIRED("04", "数据源ID不能为空"),
    DATASOURCE_ACCESS_DENIED("05", "无权限访问该数据源"),
    ARCHIVE_GROUP_ID_REQUIRED("06", "分组ID不能为空"),
    ARCHIVE_GROUP_NOT_FOUND("07", "归档分组不存在"),
    TASK_ID_REQUIRED("08", "任务ID不能为空"),
    TASK_NOT_FOUND("09", "任务不存在"),
    USER_NOT_FOUND("10", "用户不存在"),
    DATASOURCE_NOT_FOUND("11", "归档连接不存在"),
    DATASOURCE_REQUIRED("12", "归档连接不能为空"),
    DATASOURCE_STATUS_REQUIRED("13", "归档连接状态不能为空"),
    DATASOURCE_STATUS_MANUAL_UPDATE_UNSUPPORTED("14", "请先测试归档连接，测试成功后系统会自动启用"),
    DATASOURCE_STATUS_INVALID("15", "数据源状态值不合法"),
    ARCHIVE_GROUP_CODE_REQUIRED("15", "分组编码不能为空"),
    ARCHIVE_GROUP_DISABLED("16", "Archive group is disabled"),
    ARCHIVE_GROUP_HAS_ACTIVE_TASK("17", "Archive group has active task"),
    ARCHIVE_GROUP_HAS_NO_ENABLED_ITEM("18", "Archive group has no enabled item"),
    ARCHIVE_GROUP_HAS_NO_ACTIVE_TASK("19", "Archive group has no active task"),
    UNSUPPORTED_DATASOURCE_TYPE("20", "Unsupported datasource type: {0}", true),
    ARCHIVE_GROUP_ITEM_REQUIRED("21", "归档明细不能为空"),
    ARCHIVE_GROUP_ITEM_ID_REQUIRED("22", "归档明细ID不能为空"),
    ARCHIVE_GROUP_ITEM_NOT_FOUND("23", "归档明细不存在"),
    UNSAFE_CLEAN_WITHOUT_WRITE("24", "启用归档明细时不能只清理源数据而不写入目标数据"),
    PRIORITY_REQUIRED("25", "优先级不能为空"),
    PRIORITY_DUPLICATED("26", "同一分组内优先级不能重复"),
    SOURCE_TABLE_REQUIRED("27", "来源表不能为空"),
    TARGET_TABLE_REQUIRED("28", "目标表不能为空"),
    FETCH_SQL_REQUIRED("29", "查询SQL不能为空"),
    ID_COLUMN_REQUIRED("30", "ID字段不能为空"),
    ENABLE_STATUS_INVALID("31", "启用状态不合法"),
    CLEAN_FLAG_INVALID("32", "清理开关不合法"),
    WRITE_FLAG_INVALID("33", "写入开关不合法"),
    STEP_COUNT_INVALID("34", "步长必须大于0"),
    PAUSE_MS_INVALID("35", "暂停时间不能小于0"),
    START_ID_REQUIRED("36", "开始ID不能为空"),
    END_ID_REQUIRED("37", "结束ID不能为空"),
    STEP_ROUNDS_INVALID("38", "滚动步长必须大于0"),
    START_TIME_REQUIRED("39", "开始时间不能为空"),
    KEEP_DAY_INVALID("40", "保留天数不能小于0"),
    STEP_MINUTES_INVALID("41", "时间滚动步长必须大于0"),
    ARCHIVE_GROUP_REQUIRED("42", "归档分组不能为空"),
    ARCHIVE_GROUP_NAME_REQUIRED("43", "分组名称不能为空"),
    ARCHIVE_GROUP_DATASOURCE_REQUIRED("44", "源和目标数据源不能为空"),
    ARCHIVE_GROUP_CODE_DUPLICATED("45", "分组编码已存在"),
    DATASOURCE_ENABLE_STATUS_REQUIRED("46", "{0}归档连接必须为已启用状态", true),
    ARCHIVE_GROUP_ACTIVE_TASK_CONFLICT("47", "分组存在执行中的任务，无法执行当前操作"),
    ARCHIVE_TASK_TERMINAL_CANNOT_CANCEL("48", "任务已结束，无法取消"),
    USER_ROLE_INVALID("49", "用户角色不合法"),
    ARCHIVE_GROUP_NOTIFICATION_RECIPIENT_REQUIRED("50", "启用站内通知时至少选择一个通知成员"),
    ARCHIVE_GROUP_NOTIFY_CHANNEL_REQUIRED("51", "启用执行完成通知时必须选择通知方式"),
    ARCHIVE_GROUP_NOTIFY_WEBHOOK_REQUIRED("52", "启用执行完成通知时必须填写通知地址"),
    ARCHIVE_GROUP_NOTIFY_OWNER_REQUIRED("53", "站内通知必须指定负责人"),
    USER_ROLE_INVALID_FOR_CREATOR("54", "无权创建该角色的用户"),
    OWNER_UPDATE_NOT_ALLOWED("55", "普通用户不允许变更负责人"),
    OWNER_USER_INVALID("56", "负责人用户不存在"),
    OWNER_USER_DISABLED("57", "负责人用户已禁用"),
    OWNER_USER_NOT_CREATED_BY_YOU("58", "该用户不是你创建的");

    private static final String BASE_CODE = "StarterError-";

    private final String errorCode;
    private final String errorMsg;
    private final boolean replacePlaceHold;

    StarterErrorCode(String errorCode, String errorMsg) {
        this(errorCode, errorMsg, false);
    }

    StarterErrorCode(String errorCode, String errorMsg, boolean replacePlaceHold) {
        this.errorCode = BASE_CODE + errorCode;
        this.errorMsg = errorMsg;
        this.replacePlaceHold = replacePlaceHold;
    }
}
