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
public class ArchiveGroupVO {

    private Long id;

    private Long parentId;

    private String groupCode;

    private String groupName;

    private String groupPath;

    private Integer groupLevel;

    private Long sourceDatasourceId;

    private Long targetDatasourceId;

    private Long ownerUserId;

    private Integer enableStatus;

    private Integer notifyEnabled;

    private String notifyChannel;

    private String notifyWebhookUrl;

    private String remark;

    private String ownerDisplayName;

    private Long activeTaskId;

    private Integer activeTaskStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date activeTaskStartTime;

    private Long activeTaskProcessedRecords;

    private java.math.BigDecimal activeTaskProcessedSpeed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date activeTaskHeartbeatTime;

    private Boolean canTrigger;

    private Boolean canCancelActiveTask;

    private Boolean canViewActiveTask;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updatedTime;
}
