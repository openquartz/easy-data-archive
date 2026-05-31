package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 权限实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysPermission {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Long parentId;
    private String routePath;
    private String component;
    private Integer sortNo;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long creatorId;
    private Long updaterId;
    private Integer deleted;

}