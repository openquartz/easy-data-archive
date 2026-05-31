# ConnectionFactory 连接池化与连接测试 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `ConnectionFactory` 从每次创建新连接改为使用 HikariCP 连接池管理，并提供 `testConnection()` 方法验证数据库连通性。

**Architecture:** `ConnectionFactory` 内部维护 `ConcurrentHashMap<String, HikariDataSource>`，按 `url|username` 作为 key 缓存池实例。通过 `ConnectionPoolConfig` POJO 在 core 模块内定义池参数，starter 模块负责从 `ConnectionProperties` 转换并调用 `ConnectionFactory.init()`。对调用方完全透明。

**Tech Stack:** Java 11, HikariCP (via Spring Boot 2.7.18 BOM), JUnit 5, Mockito

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|----------|------|
| Modify | `easyarchive-core/pom.xml` | 添加 HikariCP 依赖 |
| Create | `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/connection/ConnectionPoolConfig.java` | 连接池参数 POJO，core 模块内定义 |
| Modify | `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/connection/ConnectionFactory.java` | 池化改造 + testConnection |
| Create | `easyarchive-core/src/test/java/com/openquartz/easyarchive/core/connection/ConnectionFactoryTest.java` | ConnectionFactory 单元测试 |
| Modify | `easyarchive-starter/src/main/java/com/openquartz/easyarchive/connection/property/ConnectionProperties.java` | 添加 pool 配置字段 |
| Modify | `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java` | 调用 ConnectionFactory.init() |

---

### Task 1: 添加 HikariCP 依赖

**Files:**
- Modify: `easyarchive-core/pom.xml`

- [ ] **Step 1: 在 easyarchive-core 的 pom.xml 中添加 HikariCP 依赖**

在 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
```

版本由父 POM 的 `spring-boot-dependencies` BOM 管理（Spring Boot 2.7.18 对应 HikariCP 4.0.3），无需指定版本号。

- [ ] **Step 2: 验证依赖解析**

Run: `mvn dependency:resolve -pl easyarchive-core -q`
Expected: 构建成功，无依赖解析错误

- [ ] **Step 3: Commit**

```bash
git add easyarchive-core/pom.xml
git commit -m "deps: add HikariCP dependency to easyarchive-core"
```

---

### Task 2: 创建 ConnectionPoolConfig POJO

**Files:**
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/connection/ConnectionPoolConfig.java`

此 POJO 用于在 core 模块内定义连接池参数，避免 core 对 starter 的依赖。

- [ ] **Step 1: 创建 ConnectionPoolConfig 类**

```java
package com.openquartz.easyarchive.core.connection;

import lombok.Getter;
import lombok.Setter;

/**
 * 连接池配置
 */
@Getter
@Setter
public class ConnectionPoolConfig {

    /**
     * 最大连接数
     */
    private int maximumPoolSize = 10;

    /**
     * 最小空闲连接数
     */
    private int minimumIdle = 2;

    /**
     * 获取连接超时时间（毫秒）
     */
    private long connectionTimeout = 30000;

    /**
     * 空闲连接存活时间（毫秒）
     */
    private long idleTimeout = 600000;

    /**
     * 连接最大存活时间（毫秒）
     */
    private long maxLifetime = 1800000;
}
```

- [ ] **Step 2: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/connection/ConnectionPoolConfig.java
git commit -m "feat: add ConnectionPoolConfig POJO for pool parameters"
```

---

### Task 3: 改造 ConnectionFactory 实现连接池化

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/connection/ConnectionFactory.java`

- [ ] **Step 1: 重写 ConnectionFactory**

将整个文件替换为以下内容：

```java
package com.openquartz.easyarchive.core.connection;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链接工厂
 * 使用 HikariCP 连接池管理数据库连接
 */
@Slf4j
public class ConnectionFactory {

    private static final ConcurrentHashMap<String, HikariDataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();
    private static volatile ConnectionPoolConfig poolConfig;

    /**
     * 初始化连接池配置，由 Spring 容器启动时调用
     */
    public static void init(ConnectionPoolConfig config) {
        poolConfig = config;
    }

    /**
     * 从连接池获取连接
     */
    public static Connection create(ArchiveConnection archiveConnection) {
        try {
            HikariDataSource ds = DATA_SOURCE_MAP.computeIfAbsent(
                buildPoolKey(archiveConnection),
                key -> createDataSource(archiveConnection)
            );
            return ds.getConnection();
        } catch (Exception e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * 测试连接连通性，更新 ArchiveConnection.status
     *
     * @return true 连接正常，false 连接异常
     */
    public static boolean testConnection(ArchiveConnection archiveConnection) {
        try (Connection connection = create(archiveConnection)) {
            boolean valid = connection.isValid(5);
            archiveConnection.setStatus(valid ? 1 : 2);
            return valid;
        } catch (Exception e) {
            log.warn("[ConnectionFactory#testConnection] connection test failed for {}", archiveConnection.getUrl(), e);
            archiveConnection.setStatus(2);
            return false;
        }
    }

    private static HikariDataSource createDataSource(ArchiveConnection conn) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(conn.getUrl());
        config.setUsername(conn.getUsername());
        config.setPassword(conn.getPassword());
        config.setDriverClassName("com.mysql.jdbc.Driver");

        if (poolConfig != null) {
            config.setMaximumPoolSize(poolConfig.getMaximumPoolSize());
            config.setMinimumIdle(poolConfig.getMinimumIdle());
            config.setConnectionTimeout(poolConfig.getConnectionTimeout());
            config.setIdleTimeout(poolConfig.getIdleTimeout());
            config.setMaxLifetime(poolConfig.getMaxLifetime());
        }

        config.setConnectionTestQuery("SELECT 1");
        log.info("[ConnectionFactory#createDataSource] creating pool for {}", conn.getUrl());
        return new HikariDataSource(config);
    }

    private static String buildPoolKey(ArchiveConnection conn) {
        return conn.getUrl() + "|" + conn.getUsername();
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `mvn compile -pl easyarchive-core -q`
Expected: 编译成功

- [ ] **Step 3: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/connection/ConnectionFactory.java
git commit -m "feat: refactor ConnectionFactory with HikariCP connection pooling"
```

---

### Task 4: 为 ConnectionFactory 编写单元测试

**Files:**
- Create: `easyarchive-core/src/test/java/com/openquartz/easyarchive/core/connection/ConnectionFactoryTest.java`

需要先确认 `easyarchive-core` 模块有测试依赖。如果没有，需要在 pom.xml 中添加 JUnit 5 和 Mockito 依赖。

- [ ] **Step 1: 添加测试依赖（如果缺失）**

在 `easyarchive-core/pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

版本由 Spring Boot BOM 管理。

- [ ] **Step 2: 创建 ConnectionFactoryTest**

```java
package com.openquartz.easyarchive.core.connection;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConnectionFactory 单元测试
 * 注意：这些测试验证配置和逻辑，不连接真实数据库
 */
class ConnectionFactoryTest {

    @BeforeEach
    void setUp() {
        // 使用默认配置初始化
        ConnectionFactory.init(new ConnectionPoolConfig());
    }

    @Test
    void testConnection_withInvalidUrl_shouldReturnFalseAndUpdateStatus() {
        ArchiveConnection conn = new ArchiveConnection();
        conn.setUrl("jdbc:mysql://invalid-host:3306/nonexistent");
        conn.setUsername("test");
        conn.setPassword("test");
        conn.setStatus(0);

        boolean result = ConnectionFactory.testConnection(conn);

        assertFalse(result);
        assertEquals(2, conn.getStatus());
    }

    @Test
    void testInit_withCustomConfig_shouldNotThrow() {
        ConnectionPoolConfig config = new ConnectionPoolConfig();
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);

        assertDoesNotThrow(() -> ConnectionFactory.init(config));
    }

    @Test
    void testInit_calledTwice_shouldBeIdempotent() {
        ConnectionPoolConfig config1 = new ConnectionPoolConfig();
        config1.setMaximumPoolSize(5);

        ConnectionPoolConfig config2 = new ConnectionPoolConfig();
        config2.setMaximumPoolSize(20);

        assertDoesNotThrow(() -> ConnectionFactory.init(config1));
        assertDoesNotThrow(() -> ConnectionFactory.init(config2));
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `mvn test -pl easyarchive-core -Dtest=ConnectionFactoryTest`
Expected: 所有测试 PASS

- [ ] **Step 4: Commit**

```bash
git add easyarchive-core/pom.xml easyarchive-core/src/test/java/com/openquartz/easyarchive/core/connection/ConnectionFactoryTest.java
git commit -m "test: add unit tests for ConnectionFactory connection pooling"
```

---

### Task 5: 在 ConnectionProperties 中添加连接池配置字段

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/connection/property/ConnectionProperties.java`

- [ ] **Step 1: 添加 PoolProperties 内部类和 pool 字段**

在 `ConnectionProperties` 类中添加：

```java
/**
 * 连接池配置
 */
@Getter
@Setter
public static class PoolProperties {
    private int maximumPoolSize = 10;
    private int minimumIdle = 2;
    private long connectionTimeout = 30000;
    private long idleTimeout = 600000;
    private long maxLifetime = 1800000;
}

private PoolProperties pool = new PoolProperties();
```

- [ ] **Step 2: 验证编译通过**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: 编译成功

- [ ] **Step 3: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/connection/property/ConnectionProperties.java
git commit -m "feat: add pool configuration properties to ConnectionProperties"
```

---

### Task 6: 在 EasyArchiveAutoConfiguration 中初始化连接池

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java`

- [ ] **Step 1: 添加 ConnectionFactory.init() 调用**

在 `EasyArchiveAutoConfiguration` 构造函数中添加初始化逻辑。需要在 `configConnection()` bean 创建之前完成初始化。在构造函数末尾添加：

```java
import com.openquartz.easyarchive.core.connection.ConnectionFactory;
import com.openquartz.easyarchive.core.connection.ConnectionPoolConfig;

// 在构造函数中添加：
ConnectionFactory.init(toPoolConfig(connectionProperties.getPool()));
```

并添加转换方法：

```java
private ConnectionPoolConfig toPoolConfig(ConnectionProperties.PoolProperties pool) {
    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaximumPoolSize(pool.getMaximumPoolSize());
    config.setMinimumIdle(pool.getMinimumIdle());
    config.setConnectionTimeout(pool.getConnectionTimeout());
    config.setIdleTimeout(pool.getIdleTimeout());
    config.setMaxLifetime(pool.getMaxLifetime());
    return config;
}
```

完整的构造函数变为：

```java
public EasyArchiveAutoConfiguration(ArchiveConfig archiveConfig, ConnectionProperties connectionProperties) {
    this.archiveConfig = archiveConfig;
    this.connectionProperties = connectionProperties;
    ConnectionFactory.init(toPoolConfig(connectionProperties.getPool()));
}
```

- [ ] **Step 2: 验证编译通过**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: 编译成功

- [ ] **Step 3: 全量构建验证**

Run: `mvn clean install -DskipTests -q`
Expected: 构建成功

- [ ] **Step 4: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java
git commit -m "feat: initialize ConnectionFactory pool in auto-configuration"
```

---

### Task 7: 全量验证与最终提交

- [ ] **Step 1: 运行所有测试**

Run: `mvn test -q`
Expected: 所有测试 PASS

- [ ] **Step 2: 验证配置示例**

在 `application.properties` 中可使用以下配置覆盖默认值：

```properties
# 连接池配置（可选，均有默认值）
sync.connection.pool.maximum-pool-size=10
sync.connection.pool.minimum-idle=2
sync.connection.pool.connection-timeout=30000
sync.connection.pool.idle-timeout=600000
sync.connection.pool.max-lifetime=1800000
```
