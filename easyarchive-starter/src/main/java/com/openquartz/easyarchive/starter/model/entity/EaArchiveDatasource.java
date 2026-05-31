package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据源实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EaArchiveDatasource {

    private Long id;
    private String datasourceCode;
    private String datasourceName;
    private String datasourceType;
    private String jdbcUrl;
    private String username;
    private String passwordCipher;
    private String schemaName;
    private Integer status;
    private LocalDateTime lastCheckTime;
    private Long ownerUserId;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long creatorId;
    private Long updaterId;
    private Integer deleted;

}