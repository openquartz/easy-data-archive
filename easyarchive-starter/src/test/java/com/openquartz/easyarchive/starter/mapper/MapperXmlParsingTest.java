package com.openquartz.easyarchive.starter.mapper;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class MapperXmlParsingTest {

    @Test
    void archiveGroupItemMappers_shouldParseSuccessfully() {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        DataSource dataSource = mock(DataSource.class);

        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                resolver.getResource("classpath:mapper/ArchiveGroupItemByIdMapper.xml"),
                resolver.getResource("classpath:mapper/ArchiveGroupItemByTimeMapper.xml")
        );

        assertDoesNotThrow(factoryBean::getObject);
    }
}
