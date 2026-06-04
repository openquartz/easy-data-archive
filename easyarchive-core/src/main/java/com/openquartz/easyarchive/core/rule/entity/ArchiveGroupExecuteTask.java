package com.openquartz.easyarchive.core.rule.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import com.openquartz.easyarchive.common.enums.ArchiveTaskStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 归档执行任务
 *
 * execute_status: 0-等待 1-运行中 2-成功 3-失败 4-取消中 5-已取消
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArchiveGroupExecuteTask extends BaseEntity {

    public static final int STATUS_WAITING = ArchiveTaskStatusEnum.WAITING.getCode();
    public static final int STATUS_RUNNING = ArchiveTaskStatusEnum.RUNNING.getCode();
    public static final int STATUS_SUCCESS = ArchiveTaskStatusEnum.SUCCESS.getCode();
    public static final int STATUS_FAILED = ArchiveTaskStatusEnum.FAILED.getCode();
    public static final int STATUS_CANCELLING = ArchiveTaskStatusEnum.CANCELLING.getCode();
    public static final int STATUS_CANCELLED = ArchiveTaskStatusEnum.CANCELLED.getCode();

    private Long id;

    /**
     * 归档分组ID
     * @see ArchiveGroup#getId()
     */
    private Long groupId;

    /**
     * 执行开始时间
     */
    private Date startTime;

    /**
     * 执行结束时间
     */
    private Date endTime;

    /**
     * 执行状态: 0-等待 1-运行中 2-成功 3-失败 4-取消中 5-已取消
     */
    private Integer executeStatus;

    /**
     * 执行异常信息
     */
    private String errorMsg;

    /**
     * 已经处理记录数
     */
    private Long processedRecords;

    /**
     * 处理速度，(记录/秒)
     */
    private BigDecimal processedSpeed;

    /**
     * 最新心跳时间点
     */
    private Date heartbeatTime;

    /**
     * 是否已完成标记
     * 0-为未完成。否则为id
     */
    private Long finishedFlag;

    public boolean isTerminal() {
        return ArchiveTaskStatusEnum.isTerminal(executeStatus);
    }
}
