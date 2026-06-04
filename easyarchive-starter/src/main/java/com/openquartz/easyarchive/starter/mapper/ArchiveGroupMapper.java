package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 归档分组Mapper
 */
@Mapper
public interface ArchiveGroupMapper {

    int insert(ArchiveGroup group);

    int update(ArchiveGroup group);

    int updateStatus(@Param("id") Long id, @Param("enableStatus") Integer enableStatus);

    int deleteById(@Param("id") Long id);

    ArchiveGroup selectById(@Param("id") Long id);

    ArchiveGroup selectAuthorizedById(@Param("userId") Long userId, @Param("id") Long id);

    ArchiveGroup selectByCode(@Param("groupCode") String groupCode);

    List<ArchiveGroup> selectList(@Param("enableStatus") Integer enableStatus);

    List<ArchiveGroup> selectAuthorizedList(@Param("userId") Long userId,
                                            @Param("enableStatus") Integer enableStatus);
}
