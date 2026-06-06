package com.openquartz.easyarchive.starter.mapper;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MapperXmlParsingTest {

    private static final Path ARCHIVE_GROUP_EXECUTE_TASK_MAPPER = Path.of(
            "src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml");

    @Test
    void archiveGroupItemMappers_shouldParseSuccessfully() {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        DataSource dataSource = mock(DataSource.class);

        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                resolver.getResource("classpath:mapper/SysOperationLogMapper.xml"),
                resolver.getResource("classpath:mapper/ArchiveGroupItemByIdMapper.xml"),
                resolver.getResource("classpath:mapper/ArchiveGroupItemByTimeMapper.xml")
        );

        assertDoesNotThrow(factoryBean::getObject);
    }

    @Test
    void inAppNotificationMappers_shouldParseSuccessfully() {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        DataSource dataSource = mock(DataSource.class);

        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                resolver.getResource("classpath:mapper/InAppNotificationMapper.xml"),
                resolver.getResource("classpath:mapper/InAppNotificationRecipientMapper.xml"),
                resolver.getResource("classpath:mapper/ArchiveGroupNotificationUserMapper.xml")
        );

        assertDoesNotThrow(factoryBean::getObject);
    }

    @Test
    void archiveGroupExecuteTaskDailyTrend_shouldUseSameDateExpressionForSelectGroupAndOrder() throws IOException {
        String xml = Files.readString(ARCHIVE_GROUP_EXECUTE_TASK_MAPPER, StandardCharsets.UTF_8);
        String statement = extractSelectStatement(xml, "selectDailyTrend");

        String selectedDateExpr = extract(statement, "SELECT\\s+(.*?)\\s+AS\\s+day,");
        String groupedDateExpr = extract(statement, "GROUP BY\\s+(.*?)\\s+ORDER BY");
        String orderedDateExpr = extract(statement, "ORDER BY\\s+(.*?)\\s+ASC");

        assertTrue(selectedDateExpr.contains("created_time"));
        assertEquals(selectedDateExpr, groupedDateExpr);
        assertEquals(groupedDateExpr, orderedDateExpr);
    }

    private String extractSelectStatement(String xml, String id) {
        Matcher matcher = Pattern.compile(
                "<select\\s+id=\"" + id + "\"[^>]*>(.*?)</select>",
                Pattern.DOTALL
        ).matcher(xml);
        assertTrue(matcher.find(), "Expected mapper statement " + id + " to exist");
        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private String extract(String text, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
        assertTrue(matcher.find(), "Expected pattern to match: " + regex);
        return matcher.group(1).trim();
    }
}
