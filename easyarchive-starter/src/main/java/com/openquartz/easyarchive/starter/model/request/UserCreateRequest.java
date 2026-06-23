package com.openquartz.easyarchive.starter.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;

    private String mobile;

    private String email;

    private String roleCode;

    private Integer status;

    private String remark;
}
