package com.openquartz.easyarchive.starter.config;

import com.openquartz.easyarchive.common.util.ApplicationContextUtils;
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.NoOpArchiveEventPublisher;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.notification.ArchiveNotificationListener;
import com.openquartz.easyarchive.starter.notification.inapp.ArchiveInAppNotificationListener;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.DatasourceOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.service.impl.ArchiveConnectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class EasyArchiveAutoConfigurationTest {

    @Test
    void shouldExposeApplicationContextUtilsAsBean() {
        EasyArchiveAutoConfiguration configuration = new EasyArchiveAutoConfiguration();

        ApplicationContextUtils bean = configuration.applicationContextUtils();

        assertNotNull(bean);
    }

    @Test
    void shouldResolveArchiveConnectionServiceByCoreInterfaceType() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        ArchiveConnectionServiceImpl service = new ArchiveConnectionServiceImpl(
                mock(ArchiveConnectionMapper.class),
                null,
                mock(DataPermissionService.class),
                mock(DatasourceOperationLogPresenter.class),
                mock(OperationLogRecorder.class)
        );
        beanFactory.addBean("archiveConnectionService", service);

        com.openquartz.easyarchive.core.connection.ArchiveConnectionService bean =
                beanFactory.getBean(com.openquartz.easyarchive.core.connection.ArchiveConnectionService.class);

        assertSame(service, bean);
    }

    @Test
    void applicationContextUtilsBeanShouldPopulateStaticContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ApplicationContextUtils.class, ApplicationContextUtils::new);
            context.refresh();

            assertSame(context, ApplicationContextUtils.getContext());
        }
    }

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
