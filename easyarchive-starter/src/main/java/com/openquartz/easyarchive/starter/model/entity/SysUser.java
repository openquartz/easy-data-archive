package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysUser {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String mobile;
    private String email;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long creatorId;
    private Long updaterId;
    private Integer deleted;

}