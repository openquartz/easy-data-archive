package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 归档分组实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EaArchiveGroup {

    private Long id;
    private Long parentId;
    private String groupCode;
    private String groupName;
    private String groupPath;
    private Integer groupLevel;
    private Long sourceDatasourceId;
    private Long targetDatasourceId;
    private Long ownerUserId;
    private Integer enableStatus;
    private String triggerMode;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long creatorId;
    private Long updaterId;
    private Integer deleted;

}