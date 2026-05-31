package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysRole {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer status;
    private String dataScopeType;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long creatorId;
    private Long updaterId;
    private Integer deleted;

}