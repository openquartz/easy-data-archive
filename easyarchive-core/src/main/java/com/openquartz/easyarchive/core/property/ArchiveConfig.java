package com.openquartz.easyarchive.core.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ArchiveConfig
 *
 * @author svnee
 */
@Component
@Getter
public class ArchiveConfig {

    /**
     * 归档 源数据库连接
     */
    @Value("${sync.connection.source:}")
    private String sourceConnection;

    /**
     * 归档 目标数据库连接
     */
    @Value("${sync.connection.target:}")
    private String targetConnection;

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

}
