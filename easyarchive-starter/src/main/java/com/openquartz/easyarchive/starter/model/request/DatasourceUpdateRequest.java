package com.openquartz.easyarchive.starter.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DatasourceUpdateRequest {

    @NotBlank(message = "数据源名称不能为空")
    private String datasourceName;

    @NotBlank(message = "数据源类型不能为空")
    private String datasourceType;

    @NotBlank(message = "连接地址不能为空")
    private String jdbcUrl;

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String passwordCipher;

    private String remark;
}
