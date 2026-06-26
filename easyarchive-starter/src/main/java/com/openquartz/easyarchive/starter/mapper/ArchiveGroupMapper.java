package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 归档分组Mapper
 */
@Mapper
public interface ArchiveGroupMapper {

    int insert(ArchiveGroup group);

    int update(ArchiveGroup group);

    int updateStatus(@Param("id") Long id, @Param("enableStatus") Integer enableStatus);

    int updateOwner(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    int deleteById(@Param("id") Long id);

    ArchiveGroup selectById(@Param("id") Long id);

    List<ArchiveGroup> selectByIds(@Param("ids") Collection<Long> ids);

    ArchiveGroup selectAuthorizedById(@Param("userId") Long userId, @Param("id") Long id);

    ArchiveGroup selectByCode(@Param("groupCode") String groupCode);

    List<ArchiveGroup> selectList(@Param("enableStatus") Integer enableStatus);

    List<ArchiveGroup> selectAuthorizedList(@Param("userId") Long userId,
                                            @Param("enableStatus") Integer enableStatus);

    List<ArchiveGroup> selectPage(@Param("enableStatus") Integer enableStatus,
                                  @Param("start") int start,
                                  @Param("size") int size);

    List<ArchiveGroup> selectAuthorizedPage(@Param("userId") Long userId,
                                            @Param("enableStatus") Integer enableStatus,
                                            @Param("start") int start,
                                            @Param("size") int size);

    List<ArchiveGroup> selectPageByOwner(@Param("userId") Long userId,
                                         @Param("enableStatus") Integer enableStatus,
                                         @Param("start") int start,
                                         @Param("size") int size);

    int count(@Param("enableStatus") Integer enableStatus);

    int countAuthorized(@Param("userId") Long userId,
                        @Param("enableStatus") Integer enableStatus);

    int countByOwner(@Param("userId") Long userId,
                     @Param("enableStatus") Integer enableStatus);

    /**
     * 按关键词和负责人筛选分页查询
     */
    List<ArchiveGroup> selectByKeyword(
        @Param("keyword") String keyword,
        @Param("enableStatus") Integer enableStatus,
        @Param("ownerUserId") Long ownerUserId,
        @Param("start") int start,
        @Param("size") int size
    );

    /**
     * 按关键词和负责人筛选计数
     */
    int countByKeyword(
        @Param("keyword") String keyword,
        @Param("enableStatus") Integer enableStatus,
        @Param("ownerUserId") Long ownerUserId
    );
}
