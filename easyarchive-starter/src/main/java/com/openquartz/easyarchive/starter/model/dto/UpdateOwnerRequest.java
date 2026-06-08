package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateOwnerRequest {

    @NotNull(message = "负责人ID不能为空")
    private Long newOwnerUserId;
}
