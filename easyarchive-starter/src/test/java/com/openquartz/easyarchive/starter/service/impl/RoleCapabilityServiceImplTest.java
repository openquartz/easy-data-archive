package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.PlatformCapabilityEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleCapabilityServiceImplTest {

    private RoleCapabilityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleCapabilityServiceImpl();
    }

    @Test
    void archiveAdminShouldNotCreateArchiveAdmin() {
        assertFalse(service.hasCapability(RoleConstants.ARCHIVE_ADMIN, PlatformCapabilityEnum.USER_CREATE_ARCHIVE_ADMIN));
    }

    @Test
    void normalUserShouldNotManageDatasource() {
        assertFalse(service.hasCapability(RoleConstants.NORMAL_USER, PlatformCapabilityEnum.DATASOURCE_EDIT_AUTHORIZED));
    }

    @Test
    void platformAdminShouldHaveAllCapabilities() {
        assertTrue(service.hasCapability(RoleConstants.PLATFORM_ADMIN, PlatformCapabilityEnum.DATASOURCE_ASSIGN_MANAGE));
    }

    @Test
    void archiveAdminShouldTriggerArchiveGroup() {
        assertTrue(service.hasCapability(RoleConstants.ARCHIVE_ADMIN, PlatformCapabilityEnum.ARCHIVE_GROUP_TRIGGER));
    }

    @Test
    void normalUserShouldViewDashboard() {
        assertTrue(service.hasCapability(RoleConstants.NORMAL_USER, PlatformCapabilityEnum.DASHBOARD_VIEW));
    }
}
