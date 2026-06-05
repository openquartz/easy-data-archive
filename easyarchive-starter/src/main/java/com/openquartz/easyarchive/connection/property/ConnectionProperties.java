package com.openquartz.easyarchive.connection.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 连接配置属性
 *
 * @author svnee
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "sync.connection")
public class ConnectionProperties {

    /**
     * 配置数据库连接
     */
    private String config = "jdbc:mysql://localhost:3306/config_db?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private String configUsername = "root";
    private String configPassword = "password";

    /**
     * 连接池配置
     */
    private PoolProperties pool = new PoolProperties();

    @Getter
    @Setter
    public static class PoolProperties {
        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private long connectionTimeout = 30000;
        private long idleTimeout = 600000;
        private long maxLifetime = 1800000;
    }

}
