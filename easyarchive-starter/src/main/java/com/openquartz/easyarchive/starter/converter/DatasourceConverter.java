package com.openquartz.easyarchive.starter.converter;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.model.request.DatasourceCreateRequest;
import com.openquartz.easyarchive.starter.model.request.DatasourceUpdateRequest;
import com.openquartz.easyarchive.starter.model.vo.DatasourceVO;
import org.springframework.stereotype.Component;

/**
 * 数据源转换器：Request ↔ DB实体 ↔ VO
 */
@Component
public class DatasourceConverter {

    /**
     * Request → DB实体（创建）
     */
    public ArchiveConnection toEntity(DatasourceCreateRequest request) {
        if (request == null) return null;
        ArchiveConnection entity = new ArchiveConnection();
        entity.setConnectCode(request.getDatasourceCode());
        entity.setDatasourceName(request.getDatasourceName());
        entity.setConnectType(request.getDatasourceType());
        entity.setUrl(request.getJdbcUrl());
        entity.setUsername(request.getUsername());
        entity.setPasswordCipher(request.getPasswordCipher());
        entity.setRemark(request.getRemark());
        return entity;
    }

    /**
     * Request → DB实体（更新）
     */
    public ArchiveConnection toEntity(DatasourceUpdateRequest request) {
        if (request == null) return null;
        ArchiveConnection entity = new ArchiveConnection();
        entity.setDatasourceName(request.getDatasourceName());
        entity.setConnectType(request.getDatasourceType());
        entity.setUrl(request.getJdbcUrl());
        entity.setUsername(request.getUsername());
        entity.setPasswordCipher(request.getPasswordCipher());
        entity.setRemark(request.getRemark());
        return entity;
    }

    /**
     * DB实体 → VO
     */
    public DatasourceVO toVO(ArchiveConnection entity) {
        if (entity == null) return null;
        return DatasourceVO.builder()
                .id(entity.getId())
                .datasourceCode(entity.getConnectCode())
                .datasourceName(entity.getDatasourceName())
                .datasourceType(entity.getConnectType())
                .jdbcUrl(entity.getUrl())
                .username(entity.getUsername())
                .passwordCipher(entity.getPasswordCipher())
                .status(entity.getStatus())
                .remark(entity.getRemark())
                .ownerUserId(entity.getOwnerUserId())
                .lastCheckTime(entity.getLastCheckTime())
                .createdTime(entity.getCreatedTime())
                .updatedTime(entity.getUpdatedTime())
                .build();
    }
}
