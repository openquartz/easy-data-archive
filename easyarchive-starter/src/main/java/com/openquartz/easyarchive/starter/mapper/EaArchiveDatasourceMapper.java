package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.entity.EaArchiveDatasource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据源Mapper
 */
@Mapper
public interface EaArchiveDatasourceMapper {

    int insert(EaArchiveDatasource datasource);

    int update(EaArchiveDatasource datasource);

    int deleteById(Long id);

    EaArchiveDatasource selectById(Long id);

    EaArchiveDatasource selectByCode(String datasourceCode);

    List<EaArchiveDatasource> selectList(@Param("status") Integer status, @Param("ownerUserId") Long ownerUserId);

    List<EaArchiveDatasource> selectPage(@Param("start") int start, @Param("size") int size,
                                        @Param("status") Integer status, @Param("ownerUserId") Long ownerUserId);

    int count(@Param("status") Integer status, @Param("ownerUserId") Long ownerUserId);

}