package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.entity.InAppNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InAppNotificationMapper {

    int insert(InAppNotification entity);

    InAppNotification selectById(@Param("id") Long id);

    List<InAppNotification> selectByIds(@Param("ids") List<Long> ids);

    int deleteOrphansOlderThan(@Param("retentionDays") int retentionDays);
}
