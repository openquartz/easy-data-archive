package com.openquartz.easyarchive.core.common;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SysUser extends BaseEntity {

    private Long id;

    private String username;

    private String password;
}
