package com.openquartz.easyarchive.common.entity;

import lombok.Data;

import java.util.Date;

/**
 * BaseEntity
 */
@Data
public class BaseEntity {

    private Date createdTime;

    private Date updatedTime;

    private String creatorId;

    private String updaterId;

    /**
     * 0 代表正常数据。其他则为被逻辑删除
     */
    private Long deleted;
}
