package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.dto.InAppNotificationRecipientView;
import com.openquartz.easyarchive.starter.model.entity.InAppNotificationRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InAppNotificationRecipientMapper {

    int insertBatch(@Param("items") List<InAppNotificationRecipient> items);

    int countUnreadByUserId(@Param("userId") Long userId);

    List<InAppNotificationRecipientView> selectLatestByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    int markRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);

    int deleteReadModelOlderThan(@Param("retentionDays") int retentionDays);
}
