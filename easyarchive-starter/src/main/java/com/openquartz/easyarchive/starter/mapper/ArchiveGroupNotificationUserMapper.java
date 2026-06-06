package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.entity.ArchiveGroupNotificationUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArchiveGroupNotificationUserMapper {

    int insert(ArchiveGroupNotificationUser entity);

    int batchInsert(@Param("list") List<ArchiveGroupNotificationUser> list);

    int deleteByGroupId(@Param("groupId") Long groupId);

    List<ArchiveGroupNotificationUser> selectByGroupId(@Param("groupId") Long groupId);

    List<Long> selectUserIdsByGroupId(@Param("groupId") Long groupId);
}
