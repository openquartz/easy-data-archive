package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class OperationLogPresenterBeanCreationTest {

    @Test
    void shouldCreateArchiveGroupOperationLogPresenterBeanWhenMapperIsAvailable() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ArchiveConnectionMapper.class, () -> mock(ArchiveConnectionMapper.class));
            context.register(ArchiveGroupOperationLogPresenter.class);

            context.refresh();

            assertNotNull(context.getBean(ArchiveGroupOperationLogPresenter.class));
        }
    }

    @Test
    void shouldCreateArchiveTaskOperationLogPresenterBeanWhenMapperIsAvailable() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ArchiveGroupMapper.class, () -> mock(ArchiveGroupMapper.class));
            context.register(ArchiveTaskOperationLogPresenter.class);

            context.refresh();

            assertNotNull(context.getBean(ArchiveTaskOperationLogPresenter.class));
        }
    }
}
