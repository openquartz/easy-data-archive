package com.openquartz.easyarchive.starter.model.dto;

import com.openquartz.easyarchive.core.common.SysUser;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArchiveGroupNotificationMemberConfig {

    private List<Long> recipientUserIds = new ArrayList<>();

    private List<SysUser> recipientUsers = new ArrayList<>();
}
