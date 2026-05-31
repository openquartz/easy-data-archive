package com.openquartz.easyarchive.core.connection;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链接工厂
 * 使用 HikariCP 连接池管理数据库连接
 */
@Slf4j
public class ConnectionFactory {

    private static final ConcurrentHashMap<String, HikariDataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();
    private static volatile ConnectionPoolConfig poolConfig;

    /**
     * 初始化连接池配置，由 Spring 容器启动时调用
     */
    public static void init(ConnectionPoolConfig config) {
        poolConfig = config;
    }

    /**
     * 从连接池获取连接
     */
    public static Connection create(ArchiveConnection archiveConnection) {
        try {
            HikariDataSource ds = DATA_SOURCE_MAP.computeIfAbsent(
                buildPoolKey(archiveConnection),
                key -> createDataSource(archiveConnection)
            );
            return ds.getConnection();
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 测试连接连通性，更新 ArchiveConnection.status
     *
     * @return true 连接正常，false 连接异常
     */
    public static boolean testConnection(ArchiveConnection archiveConnection) {
        try (Connection connection = create(archiveConnection)) {
            boolean valid = connection.isValid(5);
            archiveConnection.setStatus(valid ? 1 : 2);
            return valid;
        } catch (Exception e) {
            log.warn("[ConnectionFactory#testConnection] connection test failed for {}", archiveConnection.getUrl(), e);
            archiveConnection.setStatus(2);
            return false;
        }
    }

    private static HikariDataSource createDataSource(ArchiveConnection conn) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(conn.getUrl());
        config.setUsername(conn.getUsername());
        config.setPassword(conn.getPassword());
        config.setDriverClassName("com.mysql.jdbc.Driver");

        if (poolConfig != null) {
            config.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
            config.setMinimumIdle(poolConfig.getMinimumIdle());
            config.setConnectionTimeout(poolConfig.getConnectionTimeout());
            config.setIdleTimeout(poolConfig.getIdleTimeout());
            config.setMaxLifetime(poolConfig.getMaxLifetime());
        }

        config.setConnectionTestQuery("SELECT 1");
        log.info("[ConnectionFactory#createDataSource] creating pool for {}", conn.getUrl());
        return new HikariDataSource(config);
    }

    private static String buildPoolKey(ArchiveConnection conn) {
        return conn.getUrl() + "|" + conn.getUsername();
    }
}
