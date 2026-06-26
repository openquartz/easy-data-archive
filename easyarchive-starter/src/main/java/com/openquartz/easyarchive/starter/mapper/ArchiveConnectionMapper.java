package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据源Mapper
 */
@Mapper
public interface ArchiveConnectionMapper {

    int insert(ArchiveConnection datasource);

    int update(ArchiveConnection datasource);

    int deleteById(Long id);

    ArchiveConnection selectById(Long id);

    ArchiveConnection selectByCode(String datasourceCode);

    ArchiveConnection selectAuthorizedById(@Param("userId") Long userId, @Param("id") Long id);

    List<ArchiveConnection> selectList(@Param("status") Integer status, @Param("ownerUserId") Long ownerUserId);

    List<ArchiveConnection> selectAuthorizedList(@Param("userId") Long userId,
                                                 @Param("status") Integer status);

    List<ArchiveConnection> selectAuthorizedListByIds(@Param("ids") Set<Long> ids);

    List<ArchiveConnection> selectPage(@Param("start") int start, @Param("size") int size,
                                        @Param("status") Integer status, @Param("ownerUserId") Long ownerUserId);

    int count(@Param("status") Integer status, @Param("ownerUserId") Long ownerUserId);

    List<Map<String, Object>> countByStatus();

    Long countByKeyword(@Param("keyword") String keyword, @Param("status") Integer status);

    List<ArchiveConnection> selectByKeyword(@Param("keyword") String keyword,
                                            @Param("status") Integer status,
                                            @Param("start") int start,
                                            @Param("size") int size);

}
