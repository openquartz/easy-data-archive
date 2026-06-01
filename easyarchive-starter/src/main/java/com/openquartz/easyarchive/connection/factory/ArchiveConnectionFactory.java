package com.openquartz.easyarchive.connection.factory;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.connection.property.ConnectionProperties;

/**
 * ArchiveConnection 工厂类
 *
 * @author svnee
 */
public class ArchiveConnectionFactory {

    public static ArchiveConnection createConfigConnection(ConnectionProperties properties) {
        ArchiveConnection connection = new ArchiveConnection();
        connection.setConnectCode("CONFIG_DB");
        connection.setConnectType("MYSQL");
        connection.setUrl(properties.getConfig());
        connection.setUsername(properties.getConfigUsername());
        connection.setPassword(properties.getConfigPassword());
        connection.setStatus(1);
        return connection;
    }

}