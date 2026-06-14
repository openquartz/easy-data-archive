# EasyArchive

<p align="center">
  <strong>📦 Enterprise Data Archiving & Migration Platform</strong><br>
  <em>Elegant, lightweight, seamless — one-stop MySQL data archiving solution</em>
</p>

<p align="center">
  <a href="https://www.oracle.com/java/technologies/javase/jdk11-archive.html"><img src="https://img.shields.io/badge/Java-11-blue" alt="Java 11"></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-green" alt="Spring Boot 2.7.18"></a>
  <a href="https://www.mysql.com/"><img src="https://img.shields.io/badge/MySQL-5.7+/8.0-blue" alt="MySQL 5.7+"></a>
  <a href="https://vuejs.org/"><img src="https://img.shields.io/badge/Vue-3.5-purple" alt="Vue 3.5"></a>
  <a href="https://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="Apache 2.0"></a>
</p>

## 📖 Overview

EasyArchive is an enterprise-grade data archiving and migration platform for MySQL, featuring a full-featured Web console, fine-grained access control, and real-time task monitoring. It supports time-range and ID-range archiving strategies, allowing you to configure, execute, and monitor archival jobs through a visual interface — making historical data archiving simple, efficient, and secure.

### ✨ Key Features

| Feature | Description |
|---------|-------------|
| **Dual Archiving Strategies** | Time-range and ID-range archiving to suit various business scenarios |
| **Web Management Console** | Modern ops UI built with Vue 3 + TypeScript covering datasources, groups, rules, tasks, and user lifecycle management |
| **Fine-Grained RBAC** | Four built-in roles (Platform Admin / Archive Admin / Auditor / Observer) with datasource-level read-write permission isolation |
| **Real-Time Task Monitoring** | Live progress tracking, throughput visualization, heartbeat mechanism, and phase-level log recording |
| **Multi-Channel Notifications** | Notify designated members via Feishu / WeCom webhooks or in-app messages on task completion or failure |
| **Operation Audit Logs** | Full CRUD audit trail with filtering by module, action type, and date range |
| **Idempotency & Reliability** | Terminal-state idempotency markers, task resume on interruption, atomic execution phases |
| **Expression Engine** | Built-in SpEL + custom command-tree engine supporting field mapping, time arithmetic, hash modulo, and more |
| **One-Command Docker Startup** | Compose orchestration of MySQL + Backend + Frontend — up and running in three steps |

## 🏗️ Project Structure

```
easy-archive/
├── easyarchive-common/          # Core APIs & utility module
│   └── api/                     #   Core interfaces: PageSource, Sink, Writer
│   ├── concurrent/              #   Concurrency primitives (ILock)
│   ├── entity/                  #   BaseEntity, Pair
│   ├── enums/                   #   Archiving type / task status / datasource status enums
│   ├── exception/               #   Unified exception model
│   ├── statistic/               #   Archiving statistics
│   └── util/                    #   JSON, date, collection utilities
├── easyarchive-core/            # Core business logic module
│   ├── ArchiveGroupExecutor/    #   Group-level concurrent execution engine
│   ├── SyncExecutor/            #   Per-table sync executor
│   ├── connection/              #   Datasource connection management
│   ├── event/                   #   Archiving events (RuleStart/End, TaskStart/Progress/End)
│   ├── executor/                #   Archive executor abstraction
│   ├── expr/                    #   Expression engine (SpEL + custom commands)
│   ├── listener/                #   Event listeners
│   ├── property/                #   Configuration property binding
│   ├── repository/              #   Archive log repository interface
│   ├── rule/                    #   Rule entities & loaders
│   ├── sink/                    #   Sink implementation (MySQL)
│   └── source/                  #   PageSource implementation (MySQL)
├── easyarchive-starter/         # Spring Boot launcher module
│   ├── config/                  #   Auto-configuration & security config
│   ├── controller/              #   RESTful API controllers
│   ├── security/                #   JWT auth & RBAC
│   ├── service/                 #   Business logic layer
│   ├── notification/            #   Notification system (Feishu/WeCom/In-App)
│   ├── operationlog/            #   Operation audit logging
│   ├── task/                    #   Scheduled cleanup tasks (logs / notifications)
│   └── resources/
│       ├── db/migration/        #   Flyway migration scripts (V1~V12)
│       └── mapper/              #   MyBatis XML mappings
├── easyarchive-ui/              # Vue 3 + TypeScript Web console
│   ├── src/
│   │   ├── api/                 #   Backend API wrappers
│   │   ├── views/               #   Page components
│   │   ├── components/          #   Reusable components
│   │   ├── stores/              #   Pinia state management
│   │   ├── router/              #   Route configuration
│   │   └── types/               #   TypeScript type definitions
│   └── Dockerfile               #   Container build
├── compose.yaml                 # Docker Compose one-command orchestration
└── pom.xml                      # Parent POM (Maven)
```

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

**Prerequisites:** Install [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/).

```bash
# 1. Clone the project
git clone https://github.com/your-repo/easy-archive.git
cd easy-archive

# 2. Start everything (MySQL + Backend + Frontend)
docker compose up -d

# After services are ready, visit:
# Frontend console:  http://localhost
# Backend API:       http://localhost:8789
```

On first launch, the database `easy_archive` is created automatically and all Flyway migrations (`V1`–`V12`) are applied.

**Default login:** `admin` / `admin` (change password after first login)

### Option 2: Local Development

#### Environment Requirements

- **Java** 11+
- **Maven** 3.6+
- **MySQL** 5.7+ or 8.0+
- **Node.js** 18+ (for frontend development)

#### Backend

```bash
# 1. Create the database and run the initialization script
mysql -u root -p < easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql

# 2. Verify environment variables / application.yml database connection
#    MYSQL_HOST=localhost
#    MYSQL_PORT=3306
#    MYSQL_DATABASE=easy_archive
#    MYSQL_USER=root
#    MYSQL_PASSWORD=your_password

# 3. Build and start
mvn clean install -DskipTests
cd easyarchive-starter
mvn spring-boot:run
```

Verify the backend is running at `http://localhost:8789/actuator/health`.

#### Frontend

```bash
cd easyarchive-ui

# Install dependencies
npm install

# Development mode (default http://localhost:5173)
npm run dev

# Production build
npm run build
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_HOST` | `localhost` | MySQL host address |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_DATABASE` | `easy_archive` | Database name |
| `MYSQL_USER` | `easyarchive` | Database user |
| `MYSQL_PASSWORD` | `easyarchive123` | Database password |
| `MYSQL_ROOT_PASSWORD` | `root123456` | Root password (Docker only) |
| `MYSQL_TIMEZONE` | `Asia/Shanghai` | Timezone |
| `BACKEND_PORT` | `8789` | Backend port |
| `FRONTEND_PORT` | `80` | Frontend port |

## 📡 API Overview

All API endpoints are prefixed with `/api/v1` and return JSON responses.

### Authentication

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/v1/auth/login` | User login, returns JWT Token | None |
| `POST` | `/api/v1/auth/logout` | User logout | Required |
| `POST` | `/api/v1/auth/me` | Get current user info | Required |
| `POST` | `/api/v1/auth/change-password` | Change password | Required |

### Datasources

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET/POST` | `/api/v1/datasources` | List / create datasource | Required |
| `PUT` | `/api/v1/datasources/{id}` | Update datasource | Required |
| `DELETE` | `/api/v1/datasources/{id}` | Delete datasource | Required |
| `POST` | `/api/v1/datasources/test` | Test datasource connection | Required |

### Archive Groups

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET/POST` | `/api/v1/archive-groups` | List / create archive group | Required |
| `PUT/DELETE` | `/api/v1/archive-groups/{id}` | Update / delete group | Required |

### Archive Rules

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET/POST` | `/api/v1/archive-group-items/by-id` | ID-range rule management | Required |
| `GET/POST` | `/api/v1/archive-group-items/by-time` | Time-range rule management | Required |

### Task Management

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET/POST` | `/api/v1/archive-tasks` | List / trigger archive task | Required |
| `GET` | `/api/v1/archive-tasks/{id}` | Get task details | Required |
| `GET` | `/api/v1/archive-tasks/{id}/logs` | Get task logs | Required |
| `POST` | `/api/v1/archive-tasks/{id}/cancel` | Cancel task | Required |

### Dashboard

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/dashboard/overview` | Global statistics overview | Required |

### Users & Permissions

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET/POST` | `/api/v1/users` | User management | Required (platform_admin) |
| `GET/POST` | `/api/v1/datasource-permissions` | User datasource permissions | Required (platform_admin) |

### Notifications

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/notifications` | List in-app notifications | Required |
| `GET` | `/api/v1/notifications/unread-count` | Unread notification count | Required |

### Operation Logs

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/operation-logs` | Paginated audit log query | Required |

### Response Format

```json
{
  "code": "SUCCESS",
  "message": "Operation successful",
  "requestId": "xxx-xxx-xxx",
  "data": { ... }
}
```

## 🛡️ Permission Model

The system uses an **RBAC (Role-Based Access Control)** model with four built-in roles:

| Role Code | Role Name | Data Scope | Description |
|-----------|-----------|------------|-------------|
| `platform_admin` | Platform Admin | ALL | Full system access, manages users and datasource permissions |
| `archive_admin` | Archive Admin | ASSIGNED | Manages datasource permissions, creates normal users, operates authorized archive groups |
| `auditor` | Auditor | VIEW | Read-only access, cannot modify any configuration |
| `observer` | Observer | VIEW | Read-only access to basic information only |

Datasource permissions are two-tiered:

| Permission Level | Description |
|------------------|-------------|
| `MANAGE` | Can edit datasource config and create archive groups on that datasource |
| `USE` | Can only execute archive tasks on that datasource, cannot modify configuration |

## 📊 Data Flow

```
User configures datasources and archive rules via Web console
        │
        ▼
Archive Group (ArchiveGroup) containing a set of archive rules
        │
        ▼
Archive Task triggered (ArchiveTask)
        │
        ├── Rule Start Event (RuleStartEvent)
        │
        ├── Sharded by ID or Time
        │       │
        │       ▼
        │   SyncExecutor reads & writes in parallel
        │       │
        │       ├── MysqlSource paginates source table
        │       ├── Writes to target table (MysqlSink)
        │       └── Cleans source data (Cleaner)
        │
        ▼
Task Complete → Progress Event (TaskProgressEvent) → Task End (RuleEndEvent)
        │
        ▼
Notifications triggered (Feishu / WeCom / In-App) to relevant members
```

## ⚙️ Configuration Reference

### Backend Configuration (`application.yml`)

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
    default-batch-size: 1000      # Default batch size per operation
    default-pause-ms: 100         # Pause between batches (ms)
  datasource:
    test-query: SELECT 1          # Connection test SQL
  log:
    enabled: true                 # Enable archive logging
    retention-days: 30            # Log retention period (days)

sync:
  reader:
    load:
      max:
        rows: 5000               # Max rows per load
      unit-time:
        max:
          try:
            frequency: 10000     # Max attempts per time unit
  archive:
    step:
      interval:
        time: 50                 # Step interval (ms)
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Test backend module only
mvn test -pl easyarchive-starter

# Skip test compilation
mvn install -Dmaven.test.skip=true

# Frontend tests
cd easyarchive-ui
npm test

# Full preflight (compile + contract tests + build verification)
./scripts/preflight-check.sh
```

## 🐳 Container Deployment

### Docker Compose

```yaml
# compose.yaml includes three services:
#   mysql    - MySQL 8.0 database
#   backend  - Java 11 + Spring Boot backend
#   frontend - Nginx + Vue 3 static assets
```

```bash
# Build and start
docker compose up -d --build

# View logs
docker compose logs -f backend

# Stop
docker compose down
```

### Standalone Builds

```bash
# Backend
docker build -t easyarchive-backend:local -f easyarchive-starter/Dockerfile ..

# Frontend
docker build -t easyarchive-frontend:local -f easyarchive-ui/Dockerfile ./easyarchive-ui
```

## 🗄️ Database Migrations

The system uses Flyway for database version management. Currently 12 migration scripts:

| Version | Description |
|---------|-------------|
| V1 | Platform init: users, roles, permissions, datasources, archive groups, tasks, monitoring alerts |
| V2 | Archive log tables |
| V3 | Archive rule detail tables (by ID / by time) |
| V4 | User datasource permissions |
| V5 | Extended operation log |
| V6 | Archive group notification configuration |
| V7 | Datasource status normalization |
| V8 | Fix seed data charset |
| V9 | In-app notifications |
| V10 | Remove deprecated notification fields |
| V11 | Refactor datasource authorization model |
| V12 | Sync legacy user datasource permissions |

## 📦 Extending EasyArchive

### Implement a Custom Data Source

```java
public class CustomSource implements PageSource {
    @Override
    public DataIterator read(Object start, Object end, Integer exePage,
                             int maxLoadRows, int interval) {
        // Paginated data reading logic
    }

    @Override
    public void clean(List<DataRecord> dataList) {
        // Source data cleanup
    }
}
```

### Implement a Custom Sink

```java
public class CustomSink implements Sink {
    @Override
    public void write(List<DataRecord> dataList) {
        // Batch write logic
    }
}
```

### Add a Custom Expression Executor

Implement `CommandExecutor` and register it to `ExecutorRegistry`:

```java
@Component
public class MyCustomExecutor implements CommandExecutor {
    @Override
    public Result execute(CommandNode node, Environment env) {
        // Custom execution logic
    }
}
```

## 🤝 Contributing

1. Fork the repository and create a feature branch (`git checkout -b feature/your-feature`)
2. Follow existing code style and conventions
3. Add unit tests for new functionality
4. Update relevant documentation
5. Submit a Pull Request

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).

## 👥 Author

**svnee**
