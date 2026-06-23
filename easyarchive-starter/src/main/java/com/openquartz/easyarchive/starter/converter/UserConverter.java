package com.openquartz.easyarchive.starter.converter;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.model.request.UserCreateRequest;
import com.openquartz.easyarchive.starter.model.request.UserUpdateRequest;
import com.openquartz.easyarchive.starter.model.vo.UserVO;
import org.springframework.stereotype.Component;

/**
 * 用户转换器：Request ↔ DB实体 ↔ VO
 */
@Component
public class UserConverter {

    /**
     * Request → DB实体（创建）
     */
    public SysUser toEntity(UserCreateRequest request) {
        if (request == null) return null;
        SysUser entity = new SysUser();
        entity.setUsername(request.getUsername());
        entity.setPassword(request.getPassword());
        entity.setRealName(request.getRealName());
        entity.setMobile(request.getMobile());
        entity.setEmail(request.getEmail());
        entity.setRoleCode(request.getRoleCode());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        return entity;
    }

    /**
     * Request → DB实体（更新）
     */
    public SysUser toEntity(UserUpdateRequest request) {
        if (request == null) return null;
        SysUser entity = new SysUser();
        entity.setRealName(request.getRealName());
        entity.setMobile(request.getMobile());
        entity.setEmail(request.getEmail());
        entity.setRoleCode(request.getRoleCode());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        return entity;
    }

    /**
     * DB实体 → VO
     */
    public UserVO toVO(SysUser entity) {
        if (entity == null) return null;
        return UserVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .realName(entity.getRealName())
                .mobile(entity.getMobile())
                .email(entity.getEmail())
                .roleCode(entity.getRoleCode())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .lastLoginTime(entity.getLastLoginTime())
                .createdTime(entity.getCreatedTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }
}
