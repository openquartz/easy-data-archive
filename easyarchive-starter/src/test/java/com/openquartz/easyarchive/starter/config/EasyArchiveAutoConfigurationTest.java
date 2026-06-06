package com.openquartz.easyarchive.starter.config;

import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.NoOpArchiveEventPublisher;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.starter.notification.ArchiveNotificationListener;
import com.openquartz.easyarchive.starter.notification.inapp.ArchiveInAppNotificationListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class EasyArchiveAutoConfigurationTest {

    @Test
    void shouldCreateDefaultPublisherWhenNotificationListenerIsUnavailable() {
        EasyArchiveAutoConfiguration configuration = new EasyArchiveAutoConfiguration();
        ArchiveConfig archiveConfig = new ArchiveConfig();
        ReflectionTestUtils.setField(archiveConfig, "logEnabled", true);
        ReflectionTestUtils.setField(configuration, "archiveConfig", archiveConfig);

        ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
        ObjectProvider<ArchiveNotificationListener> listenerProvider =
                new StaticListableBeanFactory().getBeanProvider(ArchiveNotificationListener.class);
        ObjectProvider<ArchiveInAppNotificationListener> inAppListenerProvider =
                new StaticListableBeanFactory().getBeanProvider(ArchiveInAppNotificationListener.class);

        ArchiveEventPublisher publisher =
                configuration.archiveEventPublisher(archiveLogRepository, listenerProvider, inAppListenerProvider);

        assertNotNull(publisher);
    }

    @Test
    void shouldReturnNoOpPublisherWhenLogIsDisabled() {
        EasyArchiveAutoConfiguration configuration = new EasyArchiveAutoConfiguration();
        ArchiveConfig archiveConfig = new ArchiveConfig();
        ReflectionTestUtils.setField(archiveConfig, "logEnabled", false);
        ReflectionTestUtils.setField(configuration, "archiveConfig", archiveConfig);

        ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
        ObjectProvider<ArchiveNotificationListener> listenerProvider =
                new StaticListableBeanFactory().getBeanProvider(ArchiveNotificationListener.class);
        ObjectProvider<ArchiveInAppNotificationListener> inAppListenerProvider =
                new StaticListableBeanFactory().getBeanProvider(ArchiveInAppNotificationListener.class);

        ArchiveEventPublisher publisher =
                configuration.archiveEventPublisher(archiveLogRepository, listenerProvider, inAppListenerProvider);

        assertSame(NoOpArchiveEventPublisher.INSTANCE, publisher);
    }
}
