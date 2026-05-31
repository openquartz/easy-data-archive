package com.openquartz.easyarchive.starter.support;

import com.openquartz.easyarchive.starter.model.entity.EaArchiveDatasource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 数据源连接测试器
 */
@Slf4j
@Component
public class DatasourceConnectionTester {

    public boolean testConnection(EaArchiveDatasource datasource) {
        Connection connection = null;
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.jdbc.Driver");

            // 建立连接
            connection = DriverManager.getConnection(
                    datasource.getJdbcUrl(),
                    datasource.getUsername(),
                    "******" // 实际应该解密密码
            );

            // 测试基础SQL
            boolean isValid = connection.isValid(5);
            log.info("数据源连接测试成功: {}", datasource.getDatasourceCode());
            return isValid;

        } catch (Exception e) {
            log.error("数据源连接测试失败: {}，错误: {}", datasource.getDatasourceCode(), e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("关闭连接时发生错误", e);
                }
            }
        }
    }
}