package com.openquartz.easyarchive.starter.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveGroupItemByIdVO {

    private Long id;

    private String sourceTable;

    private String targetTable;

    private Long groupId;

    private Integer priority;

    private String fetchSql;

    private String deleteWhere;

    private String startId;

    private String endId;

    private Integer stepCount;

    private Integer stepRounds;

    private Integer pauseMs;

    private Integer enableClean;

    private Integer enableWrite;

    private Integer enableStatus;

    private String idColumn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updatedTime;
}
