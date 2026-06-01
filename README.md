# EasyArchive - 数据归档迁移工具

EasyArchive是一个基于Java和Spring Boot的数据归档和迁移工具，提供了灵活的数据迁移框架，支持MySQL作为源和目标数据库。

## 功能特性

- **多种归档策略**：支持按时间归档和按ID归档两种策略
- **并发处理**：支持多线程并发执行，提高归档效率
- **灵活的配置**：基于数据库配置，支持动态调整归档规则
- **事务安全**：确保数据一致性和完整性
- **监控和日志**：详细的执行日志和进度跟踪
- **Spring Boot集成**：开箱即用的Spring Boot自动配置

## 快速开始

### 1. 环境要求

- Java 11+
- Maven 3.x
- MySQL 5.7+

### 2. 数据库准备

```sql
-- 创建配置数据库
CREATE DATABASE config_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建源数据库
CREATE DATABASE source_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建目标数据库
CREATE DATABASE target_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行表结构脚本
source docs/database/schema.sql
```

### 3. 配置应用

修改 `easyarchive-starter/src/main/resources/application.yml`：

```yaml
sync:
  connection:
    source: jdbc:mysql://localhost:3306/source_db?useUnicode=true&characterEncoding=utf8&useSSL=false
    source-username: root
    source-password: password

    target: jdbc:mysql://localhost:3306/target_db?useUnicode=true&characterEncoding=utf8&useSSL=false
    target-username: root
    target-password: password

    config: jdbc:mysql://localhost:3306/config_db?useUnicode=true&characterEncoding=utf8&useSSL=false
    config-username: root
    config-password: password
```

### 4. 配置归档规则

在配置数据库中插入归档规则：

```sql
-- 按时间归档示例
INSERT INTO archive_config (group_id, archive_type, source_table, target_table, priority, fetch_sql, start_time, keep_day, step_minutes, enable_status) VALUES
(1, 'TIME', 'user_orders', 'user_orders_archive', 1, 'SELECT * FROM user_orders WHERE create_time BETWEEN ? AND ?', '2020-01-01 00:00:00', 365, 60, 0);

-- 按ID归档示例
INSERT INTO archive_config (group_id, archive_type, source_table, target_table, priority, fetch_sql, start_id, end_id, step_count, step_rounds, enable_status) VALUES
(1, 'ID', 'user_logs', 'user_logs_Archive', 2, 'SELECT * FROM user_logs WHERE id BETWEEN ? AND ?', '0', '1000000', 1000, 5000, 0);
```

### 5. 启动应用

```bash
# 编译项目
mvn clean install

# 启动应用
cd easyarchive-starter
mvn spring-boot:run
```

### 6. 启动 UI 控制台（easyarchive-ui）

EasyArchive 提供独立的 Vue 3 运维控制台模块：`easyarchive-ui`。

```bash
# 安装依赖
cd easyarchive-ui
npm install

# 本地开发
npm run dev

# 生产构建验证
npm run build
```

默认 Vite 开发地址为 `http://localhost:5173`。

#### UI 路由

- `/login`：登录页
- `/dashboard`：总览看板
- `/datasources`：数据源管理
- `/tasks`：任务列表
- `/tasks/:taskId`：任务详情
- `/users`：用户管理

#### 后端联调说明

- UI 默认通过 `VITE_API_BASE_URL` 访问后端 API，未配置时默认使用 `/api/v1`。
- 若前后端同源部署，可直接使用默认值。
- 若本地前端单独启动并访问远端/其他端口后端，请在 `easyarchive-ui/.env.development` 中配置：

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

- 后端启动入口：`easyarchive-starter`（Spring Boot）。
- 登录成功后，UI 使用 Bearer Token 鉴权访问以下页面接口：dashboard、datasource、tasks、task detail、users。

#### 提交前固定检查（推荐）

在仓库根目录执行：

```bash
./scripts/preflight-check.sh
```

该脚本会顺序执行：

1. `easyarchive-starter` 编译校验（跳过单测）
2. Dashboard 接口契约测试（`MockMvc`）
3. `easyarchive-ui` smoke 检查（包含构建与关键路由/HTTP契约检查）

## 架构说明

### 核心组件

1. **ArchiveGroupExecutor**：归档组执行器，管理整个归档流程
2. **SyncExecutor**：同步执行器，处理具体的数据迁移操作
3. **PageSource**：数据源接口，定义数据读取行为
4. **Sink**：数据接收器接口，定义数据写入行为
5. **ArchiveRuleLoader**：规则加载器，从数据库加载归档配置

### 数据流

```
配置加载 → 规则解析 → 并发执行 → 数据读取 → 数据写入 → 源数据清理
```

### 扩展点

#### 自定义数据源

实现 `PageSource` 接口：

```java
public class CustomSource implements PageSource {
    @Override
    public DataIterator read(Object start, Object end, Integer exePage, int maxLoadRows, int interval) {
        // 实现数据读取逻辑
    }

    @Override
    public void clean(List<DataRecord> dataList) {
        // 实现数据清理逻辑
    }
}
```

#### 自定义数据接收器

实现 `Sink` 接口：

```java
public class CustomSink implements Sink {
    @Override
    public void write(List<DataRecord> dataList) {
        // 实现数据写入逻辑
    }
}
```

## 配置参数

### 性能配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| sync.reader.load.max.rows | 5000 | 一次最大加载行数 |
| sync.reader.load.unit-time.max.try.frequency | 10000 | 单位时间内最大尝试加载次数 |
| sync.archive.step.interval.time | 50 | 归档间隔时间（毫秒） |

### 数据库连接配置

| 参数 | 说明 |
|------|------|
| sync.connection.source | 源数据库连接URL |
| sync.connection.target | 目标数据库连接URL |
| sync.connection.config | 配置数据库连接URL |

## 归档规则配置

### 按时间归档

| 字段 | 说明 |
|------|------|
| archive_type | 固定为 'TIME' |
| start_time | 归档开始时间 |
| keep_day | 数据保留天数 |
| step_minutes | 时间步长（分钟） |

### 按ID归档

| 字段 | 说明 |
|------|------|
| archive_type | 固定为 'ID' |
| start_id | 开始ID |
| end_id | 结束ID |
| step_rounds | ID步长 |

## 监控和调试

### 日志级别

```yaml
logging:
  level:
    com.openquartz.easyarchive: DEBUG  # 调试级别
    com.openquartz.easyarchive: INFO   # 信息级别（推荐）
```

### 关键日志信息

- `ArchiveGroupExecutor`：归档组执行情况
- `SyncExecutor`：同步执行详情
- `MysqlSource`：数据读取日志
- `MysqlSink`：数据写入日志

## 最佳实践

1. **合理设置批处理大小**：根据数据量和系统资源调整 `maxLoadRows`
2. **使用合适的步长**：避免过小的步长导致频繁数据库操作
3. **监控执行进度**：定期检查归档任务的执行情况
4. **备份重要数据**：在执行归档前备份关键数据
5. **测试环境验证**：先在测试环境验证归档规则的正确性

## 故障排除

### 常见问题

1. **连接失败**：检查数据库连接配置和网络连通性
2. **性能问题**：调整批处理大小和并发参数
3. **数据不一致**：检查归档规则SQL的正确性
4. **内存溢出**：减小批处理大小，增加JVM内存

### 调试技巧

1. 开启DEBUG日志级别查看详细执行过程
2. 使用小批量数据测试归档规则
3. 检查数据库表索引是否合理
4. 监控数据库连接池使用情况

## 贡献指南

1. Fork项目并创建特性分支
2. 遵循现有的代码风格和约定
3. 添加适当的单元测试
4. 更新相关文档
5. 提交Pull Request

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](../LICENSE) 文件。
