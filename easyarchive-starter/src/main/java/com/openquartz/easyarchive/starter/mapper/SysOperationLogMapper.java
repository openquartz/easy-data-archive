package com.openquartz.easyarchive.starter.mapper;

import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryItem;
import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryRequest;
import com.openquartz.easyarchive.starter.model.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysOperationLogMapper {

    int insert(SysOperationLog log);

    List<OperationLogQueryItem> selectPage(@Param("request") OperationLogQueryRequest request,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    int count(@Param("request") OperationLogQueryRequest request);
}
