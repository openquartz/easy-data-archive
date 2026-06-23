package com.openquartz.easyarchive.starter.model.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ArchiveGroupUpdateRequest {

    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    private Integer notifyEnabled;

    private String notifyChannel;

    private String notifyWebhookUrl;

    private String remark;
}
