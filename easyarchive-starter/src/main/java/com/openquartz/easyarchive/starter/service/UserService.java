package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.common.SysUser;

import java.util.List;

/**
 * 用户服务
 */
public interface UserService {

    List<SysUser> findAll();

    SysUser findById(Long id);

    SysUser create(SysUser user);

    SysUser update(SysUser user);

    void updateStatus(Long id, Integer status);
}
