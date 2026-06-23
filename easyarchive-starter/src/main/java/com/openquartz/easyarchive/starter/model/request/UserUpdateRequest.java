package com.openquartz.easyarchive.starter.model.request;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String realName;

    private String mobile;

    private String email;

    private String roleCode;

    private Integer status;

    private String remark;
}
