package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 按ID归档分组明细Mapper
 */
@Mapper
public interface ArchiveGroupItemByIdMapper {

    int insert(ArchiveGroupItemById item);

    int update(ArchiveGroupItemById item);

    int updateStatus(@Param("id") Long id, @Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);

    int deleteById(@Param("id") Long id, @Param("groupId") Long groupId);

    ArchiveGroupItemById selectById(@Param("id") Long id, @Param("groupId") Long groupId);

    List<ArchiveGroupItemById> selectByGroupId(@Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);

    int countByGroupId(@Param("groupId") Long groupId);

    int countByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);

    int countEnabledByGroupId(@Param("groupId") Long groupId);

    int countPriority(@Param("groupId") Long groupId, @Param("priority") Integer priority, @Param("excludeId") Long excludeId);
}
