package com.openquartz.easyarchive.starter.config;

import com.openquartz.easyarchive.connection.factory.ArchiveConnectionFactory;
import com.openquartz.easyarchive.connection.property.ConnectionProperties;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.DefaultArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.NoOpArchiveEventPublisher;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import com.openquartz.easyarchive.core.rule.DbArchiveRuleLoader;
import com.openquartz.easyarchive.starter.listener.DbArchiveLogListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EasyArchive 自动配置类
 *
 * @author svnee
 */
@Configuration
@EnableScheduling
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

    @Bean
    public ArchiveEventPublisher archiveEventPublisher(ArchiveLogRepository archiveLogRepository) {
        if (!archiveConfig.isLogEnabled()) {
            return NoOpArchiveEventPublisher.INSTANCE;
        }
        DefaultArchiveEventPublisher publisher = new DefaultArchiveEventPublisher();
        publisher.registerListener(new DbArchiveLogListener(archiveLogRepository));
        return publisher;
    }

}