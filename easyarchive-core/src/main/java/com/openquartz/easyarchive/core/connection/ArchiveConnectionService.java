package com.openquartz.easyarchive.core.connection;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;

/**
 * 数据连接
 */
public interface ArchiveConnectionService {

    ArchiveConnection getByConnectionCode(String connectionCode);
}
