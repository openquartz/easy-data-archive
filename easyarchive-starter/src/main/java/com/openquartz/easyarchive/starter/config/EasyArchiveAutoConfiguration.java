package com.openquartz.easyarchive.starter.config;

import com.openquartz.easyarchive.connection.factory.ArchiveConnectionFactory;
import com.openquartz.easyarchive.connection.property.ConnectionProperties;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import com.openquartz.easyarchive.core.rule.DbArchiveRuleLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * EasyArchive 自动配置类
 *
 * @author svnee
 */
@Configuration
@EnableConfigurationProperties({ArchiveConfig.class, ConnectionProperties.class})
public class EasyArchiveAutoConfiguration {

    private final ArchiveConfig archiveConfig;
    private final ConnectionProperties connectionProperties;

    public EasyArchiveAutoConfiguration(ArchiveConfig archiveConfig, ConnectionProperties connectionProperties) {
        this.archiveConfig = archiveConfig;
        this.connectionProperties = connectionProperties;
    }

    @Bean
    public ArchiveConnection configConnection() {
        return ArchiveConnectionFactory.createConfigConnection(connectionProperties);
    }

    @Bean
    public ArchiveRuleLoader archiveRuleLoader() {
        return new DbArchiveRuleLoader(archiveConfig, configConnection());
    }

}