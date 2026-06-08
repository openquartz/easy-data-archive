package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.security.model.PlatformCapabilityEnum;
import java.util.Set;

public interface RoleCapabilityService {
    boolean hasCapability(String roleCode, PlatformCapabilityEnum capability);
    Set<PlatformCapabilityEnum> listCapabilities(String roleCode);
}
