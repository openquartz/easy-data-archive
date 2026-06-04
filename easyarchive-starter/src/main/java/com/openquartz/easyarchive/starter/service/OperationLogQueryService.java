package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryRequest;

import java.util.Map;

public interface OperationLogQueryService {

    Map<String, Object> query(OperationLogQueryRequest request, int page, int size);
}
