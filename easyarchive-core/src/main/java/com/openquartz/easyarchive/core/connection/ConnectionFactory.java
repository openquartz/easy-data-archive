package com.openquartz.easyarchive.core.connection;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.common.util.ExceptionUtils;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 链接工厂
 * 创建链接
 */
public class ConnectionFactory {

    public static Connection create(ArchiveConnection archiveConnection) {
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.jdbc.Driver");

            // 创建数据库连接
            return DriverManager.getConnection(
                archiveConnection.getUrl(),
                archiveConnection.getUsername(),
                archiveConnection.getPassword()
            );
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

}
