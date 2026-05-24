package com.openquartz.easyarchive.core.rule.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 归档执行任务
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArchiveGroupExecuteTask extends BaseEntity {

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
     * 执行状态
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
    private Double processedSpeed;

    /**
     * 最新心跳时间点
     */
    private Date heartbeatTime;

    /**
     * 是否已完成标记
     * 0-为未完成。否则为id
     */
    private Long finishedFlag;

    private void wrapFinishedFlag(){
        // 如果 执行状态 进入终态则设置为id 否则为0
    }
}
