package com.openquartz.easyarchive.starter.converter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.model.request.ArchiveGroupCreateRequest;
import com.openquartz.easyarchive.starter.model.request.ArchiveGroupUpdateRequest;
import com.openquartz.easyarchive.starter.model.vo.ArchiveGroupVO;
import org.springframework.stereotype.Component;

/**
 * 归档分组转换器：Request ↔ DB实体 ↔ VO
 */
@Component
public class ArchiveGroupConverter {

    /**
     * Request → DB实体（创建）
     */
    public ArchiveGroup toEntity(ArchiveGroupCreateRequest request) {
        if (request == null) return null;
        ArchiveGroup entity = new ArchiveGroup();
        entity.setParentId(request.getParentId());
        entity.setGroupCode(request.getGroupCode());
        entity.setGroupName(request.getGroupName());
        entity.setGroupLevel(request.getGroupLevel());
        entity.setSourceDatasourceId(request.getSourceDatasourceId());
        entity.setTargetDatasourceId(request.getTargetDatasourceId());
        entity.setOwnerUserId(request.getOwnerUserId());
        entity.setNotifyEnabled(request.getNotifyEnabled());
        entity.setNotifyChannel(request.getNotifyChannel());
        entity.setNotifyWebhookUrl(request.getNotifyWebhookUrl());
        entity.setRemark(request.getRemark());
        return entity;
    }

    /**
     * Request → DB实体（更新）
     */
    public ArchiveGroup toEntity(ArchiveGroupUpdateRequest request) {
        if (request == null) return null;
        ArchiveGroup entity = new ArchiveGroup();
        entity.setGroupName(request.getGroupName());
        entity.setNotifyEnabled(request.getNotifyEnabled());
        entity.setNotifyChannel(request.getNotifyChannel());
        entity.setNotifyWebhookUrl(request.getNotifyWebhookUrl());
        entity.setRemark(request.getRemark());
        return entity;
    }

    /**
     * DB实体 → VO（不含运行时字段）
     */
    public ArchiveGroupVO toVO(ArchiveGroup entity) {
        if (entity == null) return null;
        return ArchiveGroupVO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .groupCode(entity.getGroupCode())
                .groupName(entity.getGroupName())
                .groupPath(entity.getGroupPath())
                .groupLevel(entity.getGroupLevel())
                .sourceDatasourceId(entity.getSourceDatasourceId())
                .targetDatasourceId(entity.getTargetDatasourceId())
                .ownerUserId(entity.getOwnerUserId())
                .enableStatus(entity.getEnableStatus())
                .notifyEnabled(entity.getNotifyEnabled())
                .notifyChannel(entity.getNotifyChannel())
                .notifyWebhookUrl(entity.getNotifyWebhookUrl())
                .remark(entity.getRemark())
                .createdTime(entity.getCreatedTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }
}
