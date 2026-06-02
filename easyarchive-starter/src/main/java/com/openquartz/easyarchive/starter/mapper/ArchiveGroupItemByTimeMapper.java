package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 按时间归档分组明细Mapper
 */
@Mapper
public interface ArchiveGroupItemByTimeMapper {

    int insert(ArchiveGroupItemByTime item);

    int update(ArchiveGroupItemByTime item);

    int updateStatus(@Param("id") Long id, @Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);

    int deleteById(@Param("id") Long id, @Param("groupId") Long groupId);

    ArchiveGroupItemByTime selectById(@Param("id") Long id, @Param("groupId") Long groupId);

    List<ArchiveGroupItemByTime> selectByGroupId(@Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);

    int countEnabledByGroupId(@Param("groupId") Long groupId);

    int countPriority(@Param("groupId") Long groupId, @Param("priority") Integer priority, @Param("excludeId") Long excludeId);
}
