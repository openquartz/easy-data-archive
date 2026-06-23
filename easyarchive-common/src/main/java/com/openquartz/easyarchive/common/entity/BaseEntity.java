package com.openquartz.easyarchive.common.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * BaseEntity
 */
@Data
public class BaseEntity {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date updatedTime;

    private String creatorId;

    private String updaterId;

    /**
     * 0 代表正常数据。其他则为被逻辑删除
     */
    private Long deleted;
}
