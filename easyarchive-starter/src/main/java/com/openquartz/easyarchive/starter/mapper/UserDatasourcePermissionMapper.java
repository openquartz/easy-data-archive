package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.entity.UserDatasourcePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDatasourcePermissionMapper {

    int insert(UserDatasourcePermission permission);

    int batchInsert(@Param("list") List<UserDatasourcePermission> list);

    int deleteByUserIdAndDatasourceId(@Param("userId") Long userId,
                                      @Param("datasourceId") Long datasourceId,
                                      @Param("updaterId") String updaterId);

    int deleteByUserId(@Param("userId") Long userId,
                       @Param("updaterId") String updaterId);

    List<UserDatasourcePermission> selectByUserId(@Param("userId") Long userId);

    List<Long> selectDatasourceIdsByUserId(@Param("userId") Long userId,
                                           @Param("permissionType") String permissionType);

    int countByUserIdAndDatasourceId(@Param("userId") Long userId,
                                     @Param("datasourceId") Long datasourceId,
                                     @Param("permissionType") String permissionType);
}
