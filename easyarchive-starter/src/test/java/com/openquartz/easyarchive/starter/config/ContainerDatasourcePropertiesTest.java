package com.openquartz.easyarchive.starter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

class ContainerDatasourcePropertiesTest {

    @Test
    void resolvesLocalDefaultsWhenOverridesAreMissing() throws Exception {
        PropertySourcesPropertyResolver resolver = createResolver(Map.of());
        assertEquals(
                "jdbc:mysql://localhost:3306/openquartz?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                resolver.getProperty("spring.datasource.url"));
        assertEquals("root", resolver.getProperty("spring.datasource.username"));
        assertEquals("123456", resolver.getProperty("spring.datasource.password"));
        assertEquals(
                "jdbc:mysql://localhost:3306/openquartz?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                resolver.getProperty("sync.connection.config"));
    }

    @Test
    void resolvesContainerOverridesForDatasourceAndSyncConnection() throws Exception {
        PropertySourcesPropertyResolver resolver = createResolver(Map.of(
                "MYSQL_HOST", "mysql",
                "MYSQL_PORT", "3306",
                "MYSQL_DATABASE", "easy_archive",
                "MYSQL_USER", "easyarchive",
                "MYSQL_PASSWORD", "easyarchive123",
                "MYSQL_ROOT_PASSWORD", "root123"));
        assertEquals(
                "jdbc:mysql://mysql:3306/easy_archive?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                resolver.getProperty("spring.datasource.url"));
        assertEquals("easyarchive", resolver.getProperty("spring.datasource.username"));
        assertEquals("easyarchive123", resolver.getProperty("spring.datasource.password"));
        assertEquals(
                "jdbc:mysql://mysql:3306/easy_archive?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                resolver.getProperty("sync.connection.config"));
    }

    private PropertySourcesPropertyResolver createResolver(Map<String, Object> overrides) throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        Resource resource = new ClassPathResource("application.yml");
        PropertySource<?> yaml = loader.load("application", resource).get(0);
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("overrides", new HashMap<>(overrides)));
        sources.addLast(yaml);
        return new PropertySourcesPropertyResolver(sources);
    }
}
