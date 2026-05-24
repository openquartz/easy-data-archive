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
     * 源数据库连接
     */
    private String source = "jdbc:mysql://localhost:3306/source_db";
    private String sourceUsername = "root";
    private String sourcePassword = "password";

    /**
     * 目标数据库连接
     */
    private String target = "jdbc:mysql://localhost:3306/target_db";
    private String targetUsername = "root";
    private String targetPassword = "password";

    /**
     * 配置数据库连接
     */
    private String config = "jdbc:mysql://localhost:3306/config_db";
    private String configUsername = "root";
    private String configPassword = "password";

}