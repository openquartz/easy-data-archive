package com.openquartz.easyarchive.common.statistic;

import com.openquartz.easyarchive.common.util.StringUtils;
import java.util.Date;
import java.util.Objects;
import lombok.Data;

/**
 * 归档统计信息
 *
 * @author svnee
 */
@Data
public class ArchiveStatisticInfo {

    /**
     * 分组
     */
    private String group;

    /**
     * 来源表
     */
    private String fromTableName;

    /**
     * 目标表
     */
    private String toTableName;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 执行耗时
     * 单位：毫秒
     */
    private long exeMillisecond;

    private long exeMinutes;

    /**
     * 归档行数
     */
    private int archiveRows;

    public ArchiveStatisticInfo(String group, String fromTableName) {
        this.group = group;
        this.fromTableName = fromTableName;
    }

    /**
     * 如果不存在开始时间则放入
     *
     * @param startTime 开始时间
     */
    public ArchiveStatisticInfo putStartTimeIfAbsent(Date startTime) {
        if (Objects.isNull(this.startTime)) {
            this.startTime = startTime;
        }
        return this;
    }

    /**
     * 放入结束时间
     *
     * @param endTime endTime
     */
    public ArchiveStatisticInfo putEndTime(Date endTime) {
        if (Objects.isNull(this.endTime) || endTime.compareTo(this.endTime) > 0) {
            this.endTime = endTime;
        }
        return this;
    }

    public ArchiveStatisticInfo fill(String toTableName) {
        if (StringUtils.isNotBlank(toTableName) && StringUtils.isBlank(this.toTableName)) {
            this.toTableName = toTableName;
        }
        return this;
    }

    /**
     * 累加归档行数
     *
     * @param archiveRows 归档行数
     * @return 归档统计信息
     */
    public ArchiveStatisticInfo plusArchiveRows(int archiveRows) {
        this.archiveRows += archiveRows;
        return this;
    }

    /**
     * 执行时长 分钟
     *
     * @return 时长
     */
    public long getExeMinutes() {
        return exeMillisecond / (1000 * 60);
    }
}
