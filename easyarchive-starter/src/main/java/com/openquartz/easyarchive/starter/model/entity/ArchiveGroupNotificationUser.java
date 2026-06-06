package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ArchiveGroupNotificationUser {

    private Long id;

    private Long groupId;

    private Long userId;

    private Date createdTime;

    private Date updatedTime;
}
