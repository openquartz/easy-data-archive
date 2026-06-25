package com.openquartz.easyarchive.starter.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ArchiveGroupItemByIdRequest {

    @NotBlank(message = "来源表不能为空")
    private String sourceTable;

    @NotBlank(message = "目标表不能为空")
    private String targetTable;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotBlank(message = "SQL不能为空")
    private String fetchSql;

    private String deleteWhere;

    private String startId;

    private String endId;

    @NotNull(message = "步长不能为空")
    private Integer stepCount;

    private Long stepRounds;

    private Integer pauseMs;

    private Integer enableClean;

    private Integer enableWrite;

    private Integer enableStatus;

    private String idColumn;
}
