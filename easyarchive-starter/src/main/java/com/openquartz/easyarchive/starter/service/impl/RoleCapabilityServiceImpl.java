package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.PlatformCapabilityEnum;
import com.openquartz.easyarchive.starter.service.RoleCapabilityService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class RoleCapabilityServiceImpl implements RoleCapabilityService {

    private final Map<String, Set<PlatformCapabilityEnum>> capabilityMap = Map.of(
            RoleConstants.PLATFORM_ADMIN, EnumSet.allOf(PlatformCapabilityEnum.class),
            RoleConstants.ARCHIVE_ADMIN, EnumSet.of(
                    PlatformCapabilityEnum.USER_CREATE_NORMAL_USER,
                    PlatformCapabilityEnum.USER_VIEW,
                    PlatformCapabilityEnum.DATASOURCE_EDIT_AUTHORIZED,
                    PlatformCapabilityEnum.DATASOURCE_DISABLE_AUTHORIZED,
                    PlatformCapabilityEnum.DATASOURCE_TEST_AUTHORIZED,
                    PlatformCapabilityEnum.DATASOURCE_ASSIGN_USE,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_CREATE,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_EDIT,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_VIEW,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_TRIGGER,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_CANCEL,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_DELETE,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_STATUS_UPDATE,
                    PlatformCapabilityEnum.TASK_VIEW,
                    PlatformCapabilityEnum.TASK_LOG_VIEW,
                    PlatformCapabilityEnum.DASHBOARD_VIEW,
                    PlatformCapabilityEnum.NOTIFICATION_VIEW
            ),
            RoleConstants.NORMAL_USER, EnumSet.of(
                    PlatformCapabilityEnum.ARCHIVE_GROUP_CREATE,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_EDIT,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_VIEW,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_TRIGGER,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_CANCEL,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_DELETE,
                    PlatformCapabilityEnum.ARCHIVE_GROUP_STATUS_UPDATE,
                    PlatformCapabilityEnum.TASK_VIEW,
                    PlatformCapabilityEnum.TASK_LOG_VIEW,
                    PlatformCapabilityEnum.DASHBOARD_VIEW,
                    PlatformCapabilityEnum.NOTIFICATION_VIEW
            )
    );

    @Override
    public boolean hasCapability(String roleCode, PlatformCapabilityEnum capability) {
        Set<PlatformCapabilityEnum> caps = capabilityMap.getOrDefault(
                RoleConstants.normalizeRoleCode(roleCode), Collections.emptySet());
        return caps.contains(capability);
    }

    @Override
    public Set<PlatformCapabilityEnum> listCapabilities(String roleCode) {
        return capabilityMap.getOrDefault(
                RoleConstants.normalizeRoleCode(roleCode), Collections.emptySet());
    }
}
