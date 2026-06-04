package com.openquartz.easyarchive.core.common;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class SysUser extends BaseEntity {

    private Long id;

    private String username;

    private String password;

    private String realName;

    private String mobile;

    private String email;

    private String roleCode;

    private Integer status;

    private Date lastLoginTime;

    private String remark;
}
