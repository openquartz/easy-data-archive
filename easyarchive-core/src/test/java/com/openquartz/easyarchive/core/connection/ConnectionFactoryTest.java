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
        // 使用短超时配置初始化，避免无效连接测试挂起
        ConnectionPoolConfig config = new ConnectionPoolConfig();
        config.setConnectionTimeout(2000);
        ConnectionFactory.init(config);
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

    @Test
    void mysqlDriver_shouldBeAvailableForCoreModule() {
        assertDoesNotThrow(() -> Class.forName("com.mysql.jdbc.Driver"));
    }
}
