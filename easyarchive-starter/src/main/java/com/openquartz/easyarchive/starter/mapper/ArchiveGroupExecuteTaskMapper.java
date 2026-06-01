package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ArchiveGroupExecuteTaskMapper {

    int insert(ArchiveGroupExecuteTask task);

    int update(ArchiveGroupExecuteTask task);

    ArchiveGroupExecuteTask selectById(@Param("id") Long id);

    List<ArchiveGroupExecuteTask> selectPage(@Param("offset") int offset,
                                              @Param("size") int size,
                                              @Param("status") String status);

    int count(@Param("status") String status);

    int updateExecuteStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteByRetentionDays(@Param("retentionDays") int retentionDays);

    List<Map<String, Object>> countByExecuteStatus();

    List<ArchiveGroupExecuteTask> selectRecentTasks(@Param("limit") int limit);

    List<ArchiveGroupExecuteTask> selectFailedTasks(@Param("limit") int limit);
}
