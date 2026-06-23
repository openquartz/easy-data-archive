package com.openquartz.easyarchive.starter.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ArchiveGroupItemByTimeRequest {

    @NotBlank(message = "来源表不能为空")
    private String sourceTable;

    @NotBlank(message = "目标表不能为空")
    private String targetTable;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotBlank(message = "SQL不能为空")
    private String fetchSql;

    private String deleteWhere;

    @NotBlank(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @NotNull(message = "保留天数不能为空")
    private Integer keepDay;

    @NotNull(message = "步长时间不能为空")
    private Integer stepMinutes;

    @NotNull(message = "步长不能为空")
    private Integer stepCount;

    private Integer pauseMs;

    private Integer enableClean;

    private Integer enableWrite;

    private Integer enableStatus;

    private String idColumn;
}
