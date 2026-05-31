package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ArchiveTaskLogMapper {

    int insert(ArchiveTaskLog log);

    List<ArchiveTaskLog> selectByTaskId(@Param("taskId") Long taskId,
                                         @Param("offset") int offset,
                                         @Param("size") int size,
                                         @Param("executePhase") String executePhase);

    int countByTaskId(@Param("taskId") Long taskId,
                      @Param("executePhase") String executePhase);

    int deleteByRetentionDays(@Param("retentionDays") int retentionDays);
}
