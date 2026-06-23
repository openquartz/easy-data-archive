package com.openquartz.easyarchive.starter.converter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.starter.model.request.ArchiveGroupItemByTimeRequest;
import com.openquartz.easyarchive.starter.model.vo.ArchiveGroupItemByTimeVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 按时间归档分组明细转换器：Request ↔ DB实体 ↔ VO
 */
@Component
public class ArchiveGroupItemByTimeConverter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Request → DB实体
     */
    public ArchiveGroupItemByTime toEntity(ArchiveGroupItemByTimeRequest request) {
        if (request == null) return null;
        ArchiveGroupItemByTime entity = new ArchiveGroupItemByTime();
        entity.setSourceTable(request.getSourceTable());
        entity.setTargetTable(request.getTargetTable());
        entity.setPriority(request.getPriority());
        entity.setFetchSql(request.getFetchSql());
        entity.setDeleteWhere(request.getDeleteWhere());
        entity.setStartTime(parseDate(request.getStartTime()));
        entity.setKeepDay(request.getKeepDay());
        entity.setStepMinutes(request.getStepMinutes());
        entity.setStepCount(request.getStepCount() != null ? request.getStepCount() : 1000);
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
    public ArchiveGroupItemByTimeVO toVO(ArchiveGroupItemByTime entity) {
        if (entity == null) return null;
        return ArchiveGroupItemByTimeVO.builder()
                .id(entity.getId())
                .sourceTable(entity.getSourceTable())
                .targetTable(entity.getTargetTable())
                .groupId(entity.getGroupId())
                .priority(entity.getPriority())
                .fetchSql(entity.getFetchSql())
                .deleteWhere(entity.getDeleteWhere())
                .startTime(formatDate(entity.getStartTime()))
                .keepDay(entity.getKeepDay())
                .stepMinutes(entity.getStepMinutes())
                .stepCount(entity.getStepCount())
                .pauseMs(entity.getPauseMs())
                .enableClean(entity.getEnableClean())
                .enableWrite(entity.getEnableWrite())
                .enableStatus(entity.getEnableStatus())
                .idColumn(entity.getIdColumn())
                .createdTime(entity.getCreatedTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr.trim(), DATE_TIME_FORMATTER);
        return Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    private String formatDate(Date date) {
        if (date == null) return null;
        return DATE_TIME_FORMATTER.format(date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }
}
