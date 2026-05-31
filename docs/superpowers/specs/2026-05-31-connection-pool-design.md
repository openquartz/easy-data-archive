# ConnectionFactory 连接池化与连接测试设计

## 背景

当前 `ConnectionFactory.create(ArchiveConnection)` 每次调用都通过 `DriverManager.getConnection()` 创建新的 JDBC 连接，存在以下问题：

1. **连接浪费**：每次操作都创建/销毁连接，开销大
2. **无法验证连通性**：缺少连接测试机制，无法提前发现数据库不可用
3. **status 字段未利用**：`ArchiveConnection.status`（0-未测试，1-正常，2-异常）未被自动更新

## 目标

- 引入 HikariCP 连接池，避免重复创建连接
- 提供 `testConnection()` 方法，验证数据库连通性并更新 `ArchiveConnection.status`
- 连接池参数通过 `application.properties` 可配置化
- 对现有调用方完全透明，无需修改调用方代码

## 架构设计

### 整体方案

采用方案 A：`ConnectionFactory` 直接内嵌池管理。`ConnectionFactory` 内部维护 `ConcurrentHashMap<String, HikariDataSource>`，按 `url + "|" + username` 作为 key 缓存连接池实例。

### 依赖变更

`easyarchive-core/pom.xml` 新增：

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
```

版本由父 POM 的 Spring Boot 2.3.2 BOM 管理（HikariCP 3.4.5）。

### 配置参数

在现有 `ConnectionProperties` 类（`sync.connection.*` 前缀）中新增连接池参数：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `sync.connection.pool.maximum-pool-size` | 最大连接数 | 10 |
| `sync.connection.pool.minimum-idle` | 最小空闲连接数 | 2 |
| `sync.connection.pool.connection-timeout` | 获取连接超时时间（ms） | 30000 |
| `sync.connection.pool.idle-timeout` | 空闲连接存活时间（ms） | 600000 |
| `sync.connection.pool.max-lifetime` | 连接最大存活时间（ms） | 1800000 |

`ConnectionProperties` 新增内部类 `PoolProperties`：

```java
@Getter
@Setter
public static class PoolProperties {
    private int maximumPoolSize = 10;
    private int minimumIdle = 2;
    private long connectionTimeout = 30000;
    private long idleTimeout = 600000;
    private long maxLifetime = 1800000;
}
```

字段声明：

```java
private PoolProperties pool = new PoolProperties();
```

### ConnectionFactory 改造

```java
public class ConnectionFactory {

    private static final ConcurrentHashMap<String, HikariDataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();
    private static volatile ConnectionProperties connectionProperties;

    /**
     * 初始化连接池配置，由 Spring 容器启动时调用
     */
    public static void init(ConnectionProperties properties) {
        connectionProperties = properties;
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
     * @return true 连接正常，false 连接异常
     */
    public static boolean testConnection(ArchiveConnection archiveConnection) {
        try (Connection connection = create(archiveConnection)) {
            boolean valid = connection.isValid(5);
            archiveConnection.setStatus(valid ? 1 : 2);
            return valid;
        } catch (Exception e) {
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

        if (connectionProperties != null) {
            ConnectionProperties.PoolProperties pool = connectionProperties.getPool();
            config.setMaximumPoolSize(pool.getMaximumPoolSize());
            config.setMinimumIdle(pool.getMinimumIdle());
            config.setConnectionTimeout(pool.getConnectionTimeout());
            config.setIdleTimeout(pool.getIdleTimeout());
            config.setMaxLifetime(pool.getMaxLifetime());
        }

        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    private static String buildPoolKey(ArchiveConnection conn) {
        return conn.getUrl() + "|" + conn.getUsername();
    }
}
```

### 初始化时机

在 `EasyArchiveAutoConfiguration` 中注入 `ConnectionFactory.init(connectionProperties)`：

```java
@Bean
public ConnectionFactoryInitializer connectionFactoryInitializer(ConnectionProperties connectionProperties) {
    ConnectionFactory.init(connectionProperties);
    return new ConnectionFactoryInitializer();
}
```

或使用 `@PostConstruct` / `InitializingBean` 模式确保初始化顺序。

### 调用方影响

现有 4 个调用方**无需修改**：

| 调用方 | 当前行为 | 池化后行为 |
|--------|----------|------------|
| `MysqlSink` | 字段缓存单个连接 | 从池获取，close() 归还池 |
| `MysqlSource` | 字段缓存单个连接 | 从池获取，close() 归还池 |
| `DbArchiveRuleLoader` | try-with-resources | 从池获取，close() 归还池 |
| `SqlExecutor` | try-with-resources | 从池获取，close() 归还池 |

HikariCP 返回的 `Connection` 包装了 `close()` 方法，调用 `connection.close()` 时自动归还到池而非真正关闭，对调用方完全透明。

### 测试连接使用场景

```java
// 在需要验证连通性时调用
boolean ok = ConnectionFactory.testConnection(archiveConnection);
if (!ok) {
    log.error("数据库连接失败: {}", archiveConnection.getUrl());
}
```

## 错误处理

- `create()` 失败时通过 `ExceptionUtils.rethrow()` 向上抛出，保持现有行为
- `testConnection()` 捕获所有异常，返回 `false` 并设置 `status = 2`
- `init()` 支持重复调用（幂等），后续调用更新配置但不重建已有池

## 测试策略

- 单元测试：mock `HikariDataSource`，验证 `create()` 从池获取连接
- 单元测试：验证 `testConnection()` 在连接正常/异常时正确更新 status
- 单元测试：验证相同 url+username 复用同一池实例
- 集成测试：使用嵌入式数据库验证连接池端到端工作

## 不在范围内

- 连接池监控和指标暴露
- 多数据源类型的连接池（如 PostgreSQL）
- 连接池动态调整（运行时修改参数）
