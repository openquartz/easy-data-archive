package com.openquartz.easyarchive.core.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ArchiveConfig
 *
 * @author svnee
 */
@Getter
@ConfigurationProperties(prefix = "sync")
public class ArchiveConfig {


    /**
     * 归档 配置表数据库连接
     */
    @Value("${sync.config.connection:}")
    private String configConnection;

    /**
     * 归档 配置表
     */
    @Value("${sync.config.table:}")
    private String configTable;

    /**
     * 一次最大加载行数
     */
    @Value("${sync.reader.load.max.rows:5000}")
    private Integer maxLoadRows;

    /**
     * 单位时间内最大尝试加载次数,避免死循环
     */
    @Value("${sync.reader.load.unit-time.max.try.frequency:10000}")
    private Integer maxTryLoadFrequencyUnitTime;

    /**
     * 归档间隔时间
     */
    @Value("${sync.archive.step.interval.time:50}")
    private Long archiveStepIntervalTime;

    /**
     * 暂停时间
     */
    @Value("${sync.archive.pause.ms:100}")
    private Integer archivePauseMs;

    /**
     * 是否启用执行日志
     */
    @Value("${sync.log.enabled:true}")
    private boolean logEnabled;

    /**
     * 日志保留天数
     */
    @Value("${sync.log.retention-days:30}")
    private int logRetentionDays;

    /**
     * 进度更新间隔(毫秒)
     */
    @Value("${sync.archive.progress.update.interval.ms:5000}")
    private Long progressUpdateIntervalMs;

}
