# EasyArchive

<p align="center">
  <strong>📦 企业级数据归档与迁移平台</strong><br>
  <em>优雅、小巧、平滑 — 一站式 MySQL 数据归档解决方案</em>
</p>

<p align="center">
  <a href="https://www.oracle.com/java/technologies/javase/jdk11-archive.html"><img src="https://img.shields.io/badge/Java-11-blue" alt="Java 11"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-green" alt="Spring Boot 2.7.18"></a>
  <a href="https://www.mysql.com/"><img src="https://img.shields.io/badge/MySQL-5.7+/8.0-blue" alt="MySQL 5.7+"></a>
  <a href="https://vuejs.org/"><img src="https://img.shields.io/badge/Vue-3.5-purple" alt="Vue 3.5"></a>
  <a href="https://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="Apache 2.0"></a>
</p>

## 📖 目录

- [项目简介](#-项目简介)
- [核心特性](#-核心特性)
- [项目结构](#️-项目结构)
- [快速开始](#-快速开始)
- [API 概览](#-api-概览)
- [权限模型](#️-权限模型)
- [数据流](#-数据流)
- [配置参考](#️-配置参考)
- [测试](#-测试)
- [容器化部署](#-容器化部署)
- [数据库迁移](#️-数据库迁移)
- [扩展开发](#-扩展开发)
- [贡献指南](#-贡献指南)
- [许可证](#-许可证)

## 📚 使用指南

> 面向用户开发者、运维人员的详细使用指南，深入解析各功能特性。

| 文档 | 说明 |
|------|------|
| [✨ 功能特性详解](docs/guides/features.md) | 数据源管理、归档分组、归档规则、任务监控、表达式引擎、通知系统、权限模型等完整功能说明 |
| [🚀 快速部署指南](docs/guides/deployment.md) | Docker Compose 一键部署、本地开发部署、生产环境部署、安全加固与性能调优 |
| [💡 表达式引擎指南](docs/guides/expression-engine.md) | 表达式语法、13 种内置命令详解、嵌套用法、实战示例 |
| [📡 API 参考文档](docs/guides/api-reference.md) | 完整的 RESTful API 接口说明、参数文档、请求/响应示例、客户端 SDK 用法 |

---

## 📖 项目简介

EasyArchive 是一个面向 MySQL 的企业级数据归档与迁移平台，提供完整的 Web 控制台、细粒度权限管理和实时任务监控。它支持按时间和按 ID 两种归档策略，通过可视化界面即可配置、执行和监控归档任务，让历史数据归档变得简单、高效、安全。

### ✨ 核心特性

| 特性 | 说明 |
|------|------|
| **双策略归档** | 支持按时间范围归档和按 ID 范围归档，满足多种业务场景 |
| **Web 管理控制台** | 基于 Vue 3 + TypeScript 的现代化运维界面，涵盖数据源、分组、规则、任务、用户全生命周期管理 |
| **细粒度权限控制** | 基于 RBAC 模型，支持平台管理员 / 归档管理员 / 审计员 / 观察员四级角色，数据源级读写权限隔离 |
| **实时任务监控** | 任务进度实时追踪、处理速率可视化、心跳机制、分阶段日志记录 |
| **多通道消息通知** | 归档任务完成 / 失败时，通过飞书 / 企业微信群机器人或站内消息通知指定成员 |
| **操作审计日志** | 全量记录所有 CRUD 操作，支持按模块 / 操作类型 / 时间范围检索 |
| **幂等与可靠性** | 终态幂等标记、任务中断恢复、执行阶段原子性保障 |
| **表达式引擎** | 内建 SpEL + 自定义命令树表达式引擎，支持字段映射、时间计算、哈希取模等数据转换 |
| **Docker 一键启动** | Compose 编排 MySQL + 后端 + 前端，三步完成环境搭建 |

## 🏗️ 项目结构

```
easy-archive/
├── easyarchive-common/          # 核心 API 与工具模块
│   └── api/                     #   PageSource, Sink, Writer 等核心接口
│   ├── concurrent/              #   并发锁 (ILock)
│   ├── entity/                  #   BaseEntity, Pair
│   ├── enums/                   #   归档类型 / 任务状态 / 数据源状态等枚举
│   ├── exception/               #   统一异常模型
│   ├── statistic/               #   归档统计信息
│   └── util/                    #   JSON、日期、集合等工具类
├── easyarchive-core/            # 核心业务逻辑模块
│   ├── ArchiveGroupExecutor/    #   归档组并发执行引擎
│   ├── SyncExecutor/            #   单表同步执行器
│   ├── connection/              #   数据源连接管理
│   ├── event/                   #   归档事件 (RuleStart/End, TaskStart/Progress/End)
│   ├── executor/                #   归档执行器抽象
│   ├── expr/                    #   表达式引擎 (SpEL + 自定义命令)
│   ├── listener/                #   归档事件监听器
│   ├── property/                #   配置属性绑定
│   ├── repository/              #   归档日志仓储接口
│   ├── rule/                    #   规则实体与加载器
│   ├── sink/                    #   Sink 实现 (MySQL)
│   └── source/                  #   PageSource 实现 (MySQL)
├── easyarchive-starter/         # Spring Boot 启动模块
│   ├── config/                  #   自动配置 / 安全配置
│   ├── controller/              #   RESTful API 控制器
│   ├── security/                #   JWT 认证 / RBAC 权限
│   ├── service/                 #   业务逻辑层
│   ├── notification/            #   通知体系 (飞书/企微/站内信)
│   ├── operationlog/            #   操作审计日志
│   ├── task/                    #   定时清理任务 (日志/通知)
│   └── resources/
│       ├── db/migration/        #   Flyway 数据库迁移脚本 (V1~V12)
│       └── mapper/              #   MyBatis XML 映射文件
├── easyarchive-ui/              # Vue 3 + TypeScript 前端控制台
│   ├── src/
│   │   ├── api/                 #   后端 API 封装
│   │   ├── views/               #   页面组件
│   │   ├── components/          #   通用组件
│   │   ├── stores/              #   Pinia 状态管理
│   │   ├── router/              #   路由配置
│   │   └── types/               #   TypeScript 类型定义
│   └── Dockerfile               #   容器化构建
├── compose.yaml                 # Docker Compose 一键编排
└── pom.xml                      # 父 POM (Maven)
```

## 🚀 快速开始

### 方式一：Docker Compose（推荐）

**前置条件：** 安装 [Docker](https://www.docker.com/) 和 [Docker Compose](https://docs.docker.com/compose/)。

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/easy-archive.git
cd easy-archive

# 2. 一键启动 (MySQL + 后端 + 前端)
docker compose up -d

# 等待服务就绪后，访问:
# 前端控制台: http://localhost
# 后端 API:    http://localhost:8789
```

首次启动会自动创建数据库 `easy_archive` 并执行全部 Flyway 迁移脚本 (`V1~V12`)。

**默认登录账号：** `admin` / `admin`（首次登录后建议修改密码）

### 方式二：本地开发

#### 环境要求

- **Java** 11+
- **Maven** 3.6+
- **MySQL** 5.7+ 或 8.0+
- **Node.js** 18+ (前端开发)

#### 后端

```bash
# 1. 创建数据库并执行初始化脚本
mysql -u root -p < easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql

# 2. 确认环境变量 / application.yml 中的数据库连接
#    MYSQL_HOST=localhost
#    MYSQL_PORT=3306
#    MYSQL_DATABASE=easy_archive
#    MYSQL_USER=root
#    MYSQL_PASSWORD=your_password

# 3. 编译并启动
mvn clean install -DskipTests
cd easyarchive-starter
mvn spring-boot:run
```

后端启动成功后访问 `http://localhost:8789/actuator/health` 确认服务状态。

#### 前端

```bash
cd easyarchive-ui

# 安装依赖
npm install

# 开发模式 (默认 http://localhost:5173)
npm run dev

# 生产构建
npm run build
```

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_HOST` | `localhost` | MySQL 主机地址 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_DATABASE` | `easy_archive` | 数据库名 |
| `MYSQL_USER` | `easyarchive` | 数据库用户 |
| `MYSQL_PASSWORD` | `easyarchive123` | 数据库密码 |
| `MYSQL_ROOT_PASSWORD` | `root123456` | root 密码 (仅 Docker) |
| `MYSQL_TIMEZONE` | `Asia/Shanghai` | 时区 |
| `BACKEND_PORT` | `8789` | 后端端口 |
| `FRONTEND_PORT` | `80` | 前端端口 |

## 📡 API 概览

所有 API 以 `/api/v1` 为前缀，采用 JSON 响应格式。

### 认证

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `POST` | `/api/v1/auth/login` | 用户登录，返回 JWT Token | 无 |
| `POST` | `/api/v1/auth/logout` | 用户登出 | ✅ |
| `POST` | `/api/v1/auth/me` | 获取当前用户信息 | ✅ |
| `POST` | `/api/v1/auth/change-password` | 修改密码 | ✅ |

### 数据源

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET/POST` | `/api/v1/datasources` | 查询 / 创建数据源 | ✅ |
| `PUT` | `/api/v1/datasources/{id}` | 更新数据源 | ✅ |
| `DELETE` | `/api/v1/datasources/{id}` | 删除数据源 | ✅ |
| `POST` | `/api/v1/datasources/test` | 测试数据源连接 | ✅ |

### 归档分组

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET/POST` | `/api/v1/archive-groups` | 查询 / 创建归档分组 | ✅ |
| `PUT/DELETE` | `/api/v1/archive-groups/{id}` | 更新 / 删除分组 | ✅ |

### 归档规则

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET/POST` | `/api/v1/archive-group-items/by-id` | 按 ID 规则管理 | ✅ |
| `GET/POST` | `/api/v1/archive-group-items/by-time` | 按时间规则管理 | ✅ |

### 任务管理

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET/POST` | `/api/v1/archive-tasks` | 查询 / 触发归档任务 | ✅ |
| `GET` | `/api/v1/archive-tasks/{id}` | 获取任务详情 | ✅ |
| `GET` | `/api/v1/archive-tasks/{id}/logs` | 获取任务日志 | ✅ |
| `POST` | `/api/v1/archive-tasks/{id}/cancel` | 取消任务 | ✅ |

### 监控大盘

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET` | `/api/v1/dashboard/overview` | 获取全局统计概览 | ✅ |

### 用户与权限

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET/POST` | `/api/v1/users` | 用户管理 | ✅ (platform_admin) |
| `GET/POST` | `/api/v1/datasource-permissions` | 用户数据源权限管理 | ✅ (platform_admin) |

### 通知中心

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET` | `/api/v1/notifications` | 获取站内消息列表 | ✅ |
| `GET` | `/api/v1/notifications/unread-count` | 未读消息数 | ✅ |

### 操作日志

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| `GET` | `/api/v1/operation-logs` | 分页查询操作审计日志 | ✅ |

### 响应格式

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "requestId": "xxx-xxx-xxx",
  "data": { ... }
}
```

## 🛡️ 权限模型

系统采用 **RBAC（基于角色的访问控制）** 模型，内置四种角色：

| 角色编码 | 角色名称 | 数据范围 | 说明 |
|----------|---------|---------|------|
| `platform_admin` | 平台管理员 | ALL | 拥有全部权限，可管理系统用户和数据源权限 |
| `archive_admin` | 归档管理员 | ASSIGNED | 可管理数据源权限、创建普通用户、操作被授权的归档分组 |
| `auditor` | 审计员 | VIEW | 仅查看权限，无法修改任何配置 |
| `observer` | 观察员 | VIEW | 仅查看基础信息 |

数据源权限分为两级：

| 权限级别 | 说明 |
|----------|------|
| `MANAGE` | 可编辑数据源配置，并在该数据源上创建归档分组 |
| `USE` | 仅可在该数据源上执行归档任务，不可修改配置 |

## 📊 数据流

```
用户通过 Web 控制台配置数据源和归档规则
        │
        ▼
归档分组 (ArchiveGroup) 包含一组归档规则
        │
        ▼
触发归档任务 (ArchiveTask)
        │
        ├── 规则开始事件 (RuleStartEvent)
        │
        ├── 按 ID / 按时间 分片
        │       │
        │       ▼
        │   SyncExecutor 并行读取 & 写入
        │       │
        │       ├── MysqlSource 分页读取源表
        │       ├── 数据写入目标表 (MysqlSink)
        │       └── 源数据清理 (Cleaner)
        │
        ▼
任务完成 → 进度事件 (TaskProgressEvent) → 任务结束 (RuleEndEvent)
        │
        ▼
触发通知 (飞书 / 企微 / 站内消息) 通知相关人员
```

## ⚙️ 配置参考

### 后端配置 (`application.yml`)

```yaml
server:
  port: 8789

spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:easy_archive}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:123456}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      max-lifetime: 1800000

archive:
  task:
    thread-pool:
      core-pool-size: 10
      max-pool-size: 50
      queue-capacity: 100
  rule:
    default-batch-size: 1000      # 默认批量处理条数
    default-pause-ms: 100         # 每批处理后的暂停时间(ms)
  datasource:
    test-query: SELECT 1          # 连接测试 SQL
  log:
    enabled: true                 # 是否启用归档日志
    retention-days: 30            # 日志保留天数

sync:
  reader:
    load:
      max:
        rows: 5000               # 单次最大加载行数
      unit-time:
        max:
          try:
            frequency: 10000     # 单位时间内最大尝试次数
  archive:
    step:
      interval:
        time: 50                 # 归档步骤间隔(ms)
```

## 🧪 测试

```bash
# 全量测试
mvn test

# 仅测试后端模块
mvn test -pl easyarchive-starter

# 跳过测试编译
mvn install -Dmaven.test.skip=true

# 前端测试
cd easyarchive-ui
npm test

# 完整预检 (编译 + 契约测试 + 构建验证)
./scripts/preflight-check.sh
```

## 🐳 容器化部署

### Docker Compose

```yaml
# compose.yaml 包含三个服务:
#   mysql    - MySQL 8.0 数据库
#   backend  - Java 11 + Spring Boot 后端
#   frontend - Nginx + Vue 3 静态资源
```

```bash
# 构建并启动
docker compose up -d --build

# 查看日志
docker compose logs -f backend

# 停止
docker compose down
```

### 独立构建

```bash
# 后端
docker build -t easyarchive-backend:local -f easyarchive-starter/Dockerfile ..

# 前端
docker build -t easyarchive-frontend:local -f easyarchive-ui/Dockerfile ./easyarchive-ui
```

## 🗄️ 数据库迁移

系统使用 Flyway 进行数据库版本管理，当前包含 12 个迁移脚本：

| 版本 | 描述 |
|------|------|
| V1 | 初始化平台：用户、角色、权限、数据源、归档分组、任务、监控告警 |
| V2 | 归档日志表 |
| V3 | 归档规则明细表 (按 ID / 按时间) |
| V4 | 用户数据源权限 |
| V5 | 扩展操作日志 |
| V6 | 归档分组通知配置 |
| V7 | 数据源状态规范化 |
| V8 | 修复种子数据字符集 |
| V9 | 站内消息 |
| V10 | 移除废弃的通知字段 |
| V11 | 重构数据源授权模型 |
| V12 | 同步遗留用户数据源权限 |

## 📦 扩展开发

### 实现新的数据源

```java
public class CustomSource implements PageSource {
    @Override
    public DataIterator read(Object start, Object end, Integer exePage,
                             int maxLoadRows, int interval) {
        // 分页读取数据
    }

    @Override
    public void clean(List<DataRecord> dataList) {
        // 源数据清理
    }
}
```

### 实现新的数据接收器

```java
public class CustomSink implements Sink {
    @Override
    public void write(List<DataRecord> dataList) {
        // 批量写入数据
    }
}
```

### 添加自定义表达式执行器

实现 `CommandExecutor` 接口并注册到 `ExecutorRegistry`：

```java
@Component
public class MyCustomExecutor implements CommandExecutor {
    @Override
    public Result execute(CommandNode node, Environment env) {
        // 自定义执行逻辑
    }
}
```

## 🤝 贡献指南

1. Fork 本仓库并创建特性分支 (`git checkout -b feature/your-feature`)
2. 遵循现有代码风格和约定
3. 添加必要的单元测试
4. 更新相关文档
5. 提交 Pull Request

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE)。

## 👥 作者

**svnee**
