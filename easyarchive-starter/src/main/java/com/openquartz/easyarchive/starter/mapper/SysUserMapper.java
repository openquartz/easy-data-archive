package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper
 */
@Mapper
public interface SysUserMapper {

    int insert(SysUser user);

    int update(SysUser user);

    int deleteById(Long id);

    SysUser selectById(Long id);

    SysUser selectByUsername(String username);

    List<SysUser> selectList(@Param("status") Integer status);

    List<SysUser> selectPage(@Param("start") int start, @Param("size") int size, @Param("status") Integer status);

    int count(@Param("status") Integer status);

}