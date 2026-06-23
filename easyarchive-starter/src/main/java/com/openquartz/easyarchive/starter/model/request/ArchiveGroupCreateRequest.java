package com.openquartz.easyarchive.starter.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ArchiveGroupCreateRequest {

    private Long parentId;

    @NotBlank(message = "分组编码不能为空")
    private String groupCode;

    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    private Integer groupLevel;

    @NotBlank(message = "源数据源不能为空")
    private Long sourceDatasourceId;

    @NotBlank(message = "目标数据源不能为空")
    private Long targetDatasourceId;

    private Long ownerUserId;

    private Integer notifyEnabled;

    private String notifyChannel;

    private String notifyWebhookUrl;

    private String remark;
}
