package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.entity.ArchiveConnectionPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface ArchiveConnectionPermissionMapper {

    int insert(ArchiveConnectionPermission permission);

    int batchInsert(@Param("list") List<ArchiveConnectionPermission> list);

    int softDeleteByUserIdAndDatasourceId(@Param("userId") Long userId,
                                          @Param("datasourceId") Long datasourceId,
                                          @Param("updaterId") String updaterId);

    int softDeleteByUserId(@Param("userId") Long userId,
                           @Param("updaterId") String updaterId);

    List<String> selectLevelsByUserIdAndDatasourceId(@Param("userId") Long userId,
                                                     @Param("datasourceId") Long datasourceId);

    List<ArchiveConnectionPermission> selectByUserId(@Param("userId") Long userId);

    Set<Long> selectDatasourceIdsByUserIdAndLevel(@Param("userId") Long userId,
                                                  @Param("permissionLevel") String permissionLevel);

    int countByUserIdAndDatasourceIdAndLevel(@Param("userId") Long userId,
                                             @Param("datasourceId") Long datasourceId,
                                             @Param("permissionLevel") String permissionLevel);
}
