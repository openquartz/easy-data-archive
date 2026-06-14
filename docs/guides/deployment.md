# EasyArchive 快速部署指南

## 1. 部署架构

```
┌──────────┐     ┌──────────────────┐     ┌──────────┐
│  浏览器   │────▶│  Nginx + Vue 3   │────▶│  MySQL   │
│  Chrome   │◀────│  easyarchive-ui  │     │  8.0     │
└──────────┘     └──────────────────┘     └──────────┘
                            │
                     ┌──────▼───────┐
                     │  Spring Boot │────▶ 飞书/企微 Webhook
                     │  :8789       │
                     └──────────────┘
```

---

## 2. Docker Compose 一键部署（推荐）

### 2.1 环境要求

- [Docker](https://www.docker.com/) 20.10+
- [Docker Compose](https://docs.docker.com/compose/) 2.0+

### 2.2 部署步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/easy-archive.git
cd easy-archive

# 2. 可选：自定义配置
cp .env.example .env
# 编辑 .env 文件修改数据库密码等参数

# 3. 一键启动
docker compose up -d --build
```

### 2.3 服务访问

启动完成后，可访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端控制台 | `http://localhost` | Vue 3 Web 管理界面 |
| 后端 API | `http://localhost:8789` | RESTful API 服务 |
| Actuator 健康检查 | `http://localhost:8789/actuator/health` | Spring Boot 健康检查 |
| MySQL 数据库 | `localhost:3306` | MySQL 8.0 |

### 2.4 默认登录信息

| 项目 | 值 |
|------|-----|
| 用户名 | `admin` |
| 密码 | `admin` |

> **安全提醒**：首次登录后请立即修改管理员密码。

### 2.5 常用管理命令

```bash
# 查看服务状态
docker compose ps

# 查看后端日志
docker compose logs -f backend

# 查看数据库日志
docker compose logs -f mysql

# 查看前端日志
docker compose logs -f frontend

# 停止服务
docker compose down

# 停止并删除数据卷（会清除所有数据）
docker compose down -v

# 重新构建并启动
docker compose up -d --build

# 更新配置后重启
docker compose restart backend
```

### 2.6 环境变量配置

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_IMAGE` | `mysql:8.0` | MySQL 镜像 |
| `MYSQL_HOST` | `localhost` | MySQL 主机地址 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_DATABASE` | `easy_archive` | 数据库名 |
| `MYSQL_USER` | `easyarchive` | 数据库用户 |
| `MYSQL_PASSWORD` | `easyarchive123` | 数据库密码 |
| `MYSQL_ROOT_PASSWORD` | `root123456` | root 密码（仅 Docker） |
| `MYSQL_TIMEZONE` | `Asia/Shanghai` | 时区 |
| `BACKEND_PORT` | `8789` | 后端端口 |
| `FRONTEND_PORT` | `80` | 前端端口 |
| `BACKEND_IMAGE` | `easyarchive-backend:local` | 后端镜像名 |
| `FRONTEND_IMAGE` | `easyarchive-frontend:local` | 前端镜像名 |

---

## 3. 本地开发部署

### 3.1 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| Java | 11+ | 后端运行环境 |
| Maven | 3.6+ | 构建工具 |
| MySQL | 5.7+ 或 8.0+ | 数据库 |
| Node.js | 18+ | 前端运行环境 |
| npm | 7+ | 前端包管理 |

### 3.2 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE easy_archive CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行初始化脚本（可选，Docker Compose 方式会自动执行）
mysql -u root -p easy_archive < easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql
```

### 3.3 启动后端

```bash
# 创建并编辑环境变量
cat > .env << EOF
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=easy_archive
MYSQL_USER=root
MYSQL_PASSWORD=your_password
MYSQL_TIMEZONE=Asia/Shanghai
EOF

# 编译项目
mvn clean install -DskipTests

# 启动后端服务
cd easyarchive-starter
mvn spring-boot:run

# 或使用打包后的 jar 启动
# mvn package -DskipTests
# java -jar target/easyarchive-starter-*.jar
```

后端启动成功后，访问 `http://localhost:8789/actuator/health` 确认服务状态：

```json
{
  "status": "UP"
}
```

### 3.4 启动前端

```bash
cd easyarchive-ui

# 安装依赖
npm install

# 开发模式启动（默认 http://localhost:5173）
npm run dev

# 生产构建
npm run build
```

---

## 4. 独立容器化部署

### 4.1 构建后端镜像

```bash
docker build -t easyarchive-backend:local -f easyarchive-starter/Dockerfile ..
```

### 4.2 构建前端镜像

```bash
docker build -t easyarchive-frontend:local -f easyarchive-ui/Dockerfile ./easyarchive-ui
```

### 4.3 单独运行容器

```bash
# 后端
docker run -d \
  --name easyarchive-backend \
  -e MYSQL_HOST=mysql \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DATABASE=easy_archive \
  -e MYSQL_USER=easyarchive \
  -e MYSQL_PASSWORD=easyarchive123 \
  -p 8789:8789 \
  easyarchive-backend:local

# 前端
docker run -d \
  --name easyarchive-frontend \
  -p 80:80 \
  easyarchive-frontend:local
```

---

## 5. 生产环境部署建议

### 5.1 反向代理配置

使用 Nginx 作为反向代理：

```nginx
server {
    listen 80;
    server_name archive.example.com;

    # 前端静态资源
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://backend:8789;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 健康检查
    location /actuator/health {
        proxy_pass http://backend:8789;
    }
}
```

### 5.2 安全加固

| 措施 | 说明 |
|------|------|
| HTTPS | 配置 SSL 证书，强制 HTTPS 访问 |
| 密码强度 | 强制使用强密码，定期轮换 |
| IP 限制 | 通过安全组或防火墙限制管理接口访问 |
| 日志审计 | 开启操作审计日志，定期审查 |
| 数据库备份 | 配置 MySQL 定期备份策略 |

### 5.3 性能调优

```yaml
# application.yml 生产环境推荐配置
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30
      idle-timeout: 600000
      max-lifetime: 1800000

archive:
  task:
    thread-pool:
      core-pool-size: 20
      max-pool-size: 100
      queue-capacity: 200
  rule:
    default-batch-size: 5000
    default-pause-ms: 50
```

### 5.4 监控与告警

- 利用 Actuator 暴露 `health`、`info`、`metrics` 端点
- 接入 Prometheus + Grafana 监控
- 配置归档任务失败告警（通过飞书/企微 Webhook）

---

## 6. 常见问题

### 6.1 数据库连接失败

```
Caused by: java.sql.SQLException: Cannot create PoolableConnectionFactory
```

**解决方法：**
1. 检查 MySQL 服务是否正常运行
2. 确认 `.env` 文件中的数据库连接参数正确
3. 检查 MySQL 用户权限是否正确

### 6.2 前端页面空白

**解决方法：**
1. 确认后端服务是否正常运行
2. 检查 Vite 配置中的 proxy 是否正确
3. 查看浏览器控制台是否有 CORS 错误

### 6.3 Docker 镜像构建失败

```
ERROR: failed to solve: failed to compute cache key
```

**解决方法：**
1. 升级 Docker 到最新版本
2. 清理 Docker 缓存：`docker builder prune -a`
3. 确认 Dockerfile 路径正确

### 6.4 端口冲突

**解决方法：**
修改 `.env` 文件中的端口映射：

```bash
BACKEND_PORT=8790
FRONTEND_PORT=8080
```
