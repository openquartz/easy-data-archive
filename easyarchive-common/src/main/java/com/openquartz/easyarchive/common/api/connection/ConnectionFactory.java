package com.openquartz.easyarchive.common.api.connection;

import com.openquartz.easyarchive.common.util.ExceptionUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;

/**
 * ConnectionFactory
 *
 * @author svnee
 */
@Slf4j
public class ConnectionFactory {

    public static Connection create(String cntStr) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.error("[ConnectionFactory#create] connection-factory create error!", e);
        }
        try {
            return DriverManager.getConnection(cntStr);
        } catch (SQLException e) {
            log.error("[ConnectionFactory#create] connection-factory create error!", e);
            return ExceptionUtils.rethrow(e);
        }
    }
}
