package com.openquartz.easyarchive.core.connection;

import lombok.Getter;
import lombok.Setter;

/**
 * 连接池配置
 */
@Getter
@Setter
public class ConnectionPoolConfig {

    /**
     * 最大连接数
     */
    private int maximumPoolSize = 10;

    /**
     * 最小空闲连接数
     */
    private int minimumIdle = 2;

    /**
     * 获取连接超时时间（毫秒）
     */
    private long connectionTimeout = 30000;

    /**
     * 空闲连接存活时间（毫秒）
     */
    private long idleTimeout = 600000;

    /**
     * 连接最大存活时间（毫秒）
     */
    private long maxLifetime = 1800000;
}
