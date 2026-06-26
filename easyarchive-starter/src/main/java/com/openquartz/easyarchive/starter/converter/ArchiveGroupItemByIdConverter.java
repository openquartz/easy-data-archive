package com.openquartz.easyarchive.starter.converter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.starter.model.request.ArchiveGroupItemByIdRequest;
import com.openquartz.easyarchive.starter.model.vo.ArchiveGroupItemByIdVO;
import org.springframework.stereotype.Component;

/**
 * 按ID归档分组明细转换器：Request ↔ DB实体 ↔ VO
 */
@Component
public class ArchiveGroupItemByIdConverter {

    /**
     * Request → DB实体
     */
    public ArchiveGroupItemById toEntity(ArchiveGroupItemByIdRequest request) {
        if (request == null) return null;
        ArchiveGroupItemById entity = new ArchiveGroupItemById();
        entity.setSourceTable(request.getSourceTable());
        entity.setTargetTable(request.getTargetTable());
        entity.setPriority(request.getPriority());
        entity.setFetchSql(request.getFetchSql());
        entity.setDeleteWhere(request.getDeleteWhere());
        entity.setStartId(request.getStartId() != null ? request.getStartId() : "0");
        entity.setEndId(request.getEndId() != null ? request.getEndId() : String.valueOf(Long.MAX_VALUE));
        entity.setStepCount(request.getStepCount() != null ? request.getStepCount() : 1000);
        entity.setStepRounds(request.getStepRounds() != null ? request.getStepRounds() : 5000L);
        entity.setPauseMs(request.getPauseMs());
        entity.setEnableClean(request.getEnableClean());
        entity.setEnableWrite(request.getEnableWrite());
        entity.setEnableStatus(request.getEnableStatus());
        entity.setIdColumn(request.getIdColumn() != null ? request.getIdColumn() : "ID");
        return entity;
    }

    /**
     * DB实体 → VO
     */
    public ArchiveGroupItemByIdVO toVO(ArchiveGroupItemById entity) {
        if (entity == null) return null;
        return ArchiveGroupItemByIdVO.builder()
                .id(entity.getId())
                .sourceTable(entity.getSourceTable())
                .targetTable(entity.getTargetTable())
                .groupId(entity.getGroupId())
                .priority(entity.getPriority())
                .fetchSql(entity.getFetchSql())
                .deleteWhere(entity.getDeleteWhere())
                .startId(entity.getStartId())
                .endId(entity.getEndId())
                .stepCount(entity.getStepCount())
                .stepRounds(entity.getStepRounds())
                .pauseMs(entity.getPauseMs())
                .enableClean(entity.getEnableClean())
                .enableWrite(entity.getEnableWrite())
                .enableStatus(entity.getEnableStatus())
                .idColumn(entity.getIdColumn())
                .createdTime(entity.getCreatedTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }
}
