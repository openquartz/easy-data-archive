# Docker Compose Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-style Docker Compose deployment for EasyArchive that runs the Vue frontend, Spring Boot backend, and MySQL together while exposing only port `80`.

**Architecture:** The stack will use three Compose services: `mysql`, `backend`, and `frontend`. The backend will stop hard-coding localhost JDBC settings and instead read environment-backed Spring properties with local defaults. The frontend will be built into static assets and served by `nginx`, which will also reverse proxy `/api/v1` to the backend container.

**Tech Stack:** Docker Compose, Docker multi-stage builds, nginx, Vue 3 + Vite, Spring Boot, Maven, MySQL

---

## File Structure

- Create: `compose.yaml`
- Create: `.env.example`
- Create: `easyarchive-ui/Dockerfile`
- Create: `easyarchive-ui/nginx.conf`
- Create: `easyarchive-starter/Dockerfile`
- Modify: `easyarchive-starter/src/main/resources/application.yml`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/config/ContainerDatasourcePropertiesTest.java`
- Create: `docs/deployment/docker-compose.md`

Responsibility split:

- `application.yml` owns runtime property placeholders and keeps local defaults working
- backend config test locks the placeholder contract so future edits do not reintroduce hard-coded container-host assumptions
- frontend Dockerfile and nginx config own static serving and reverse proxy behavior
- backend Dockerfile owns multi-module jar packaging and runtime image setup
- `compose.yaml` owns service topology, health checks, ports, and persistent volume wiring
- `.env.example` and deployment docs explain how operators provide credentials and initialize schema

### Task 1: Make Backend Database Settings Container-Aware

**Files:**
- Modify: `easyarchive-starter/src/main/resources/application.yml`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/config/ContainerDatasourcePropertiesTest.java`

- [ ] **Step 1: Write the failing configuration contract test**

Create `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/config/ContainerDatasourcePropertiesTest.java` with assertions that the starter resolves datasource settings from environment-style properties while preserving sensible defaults:

```java
package com.openquartz.easyarchive.starter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.env.PropertySource;

class ContainerDatasourcePropertiesTest {

    @Test
    void resolvesLocalDefaultsWhenOverridesAreMissing() throws Exception {
        PropertySourcesPropertyResolver resolver = createResolver(Map.of());
        assertEquals("jdbc:mysql://localhost:3306/openquartz?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai",
                resolver.getProperty("spring.datasource.url"));
        assertEquals("root", resolver.getProperty("spring.datasource.username"));
        assertEquals("123456", resolver.getProperty("spring.datasource.password"));
        assertEquals("jdbc:mysql://localhost:3306/openquartz?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
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
                "MYSQL_ROOT_PASSWORD", "root123"
        ));
        assertEquals("jdbc:mysql://mysql:3306/easy_archive?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai",
                resolver.getProperty("spring.datasource.url"));
        assertEquals("easyarchive", resolver.getProperty("spring.datasource.username"));
        assertEquals("easyarchive123", resolver.getProperty("spring.datasource.password"));
        assertEquals("jdbc:mysql://mysql:3306/easy_archive?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
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
```

- [ ] **Step 2: Run the test to verify the current hard-coded config fails**

Run:

```bash
mvn -pl easyarchive-starter -Dtest=ContainerDatasourcePropertiesTest test
```

Expected: FAIL because `application.yml` still hard-codes `localhost`, `openquartz`, `root`, and `123456`.

- [ ] **Step 3: Replace hard-coded datasource properties with environment-backed placeholders**

Update `easyarchive-starter/src/main/resources/application.yml` so both `spring.datasource` and `sync.connection.config` are built from overridable placeholders:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:openquartz}?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=${MYSQL_TIMEZONE:Asia/Shanghai}
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:123456}

sync:
  connection:
    config: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:openquartz}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=${MYSQL_TIMEZONE:Asia/Shanghai}
    config-username: ${MYSQL_USER:root}
    config-password: ${MYSQL_PASSWORD:123456}
```

Do not change unrelated logging, thread-pool, or management settings.

- [ ] **Step 4: Run the focused config test again**

Run:

```bash
mvn -pl easyarchive-starter -Dtest=ContainerDatasourcePropertiesTest test
```

Expected: PASS with both default-path and override-path assertions green.

- [ ] **Step 5: Commit the backend configuration slice**

```bash
git add easyarchive-starter/src/main/resources/application.yml easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/config/ContainerDatasourcePropertiesTest.java
git commit -m "feat: externalize starter datasource settings"
```

### Task 2: Containerize The Frontend For Static Serving And API Proxying

**Files:**
- Create: `easyarchive-ui/Dockerfile`
- Create: `easyarchive-ui/nginx.conf`

- [ ] **Step 1: Write the nginx config first**

Create `easyarchive-ui/nginx.conf` with SPA fallback and reverse proxy behavior:

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

- [ ] **Step 2: Add the production frontend Dockerfile**

Create `easyarchive-ui/Dockerfile` as a multi-stage build:

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
```

- [ ] **Step 3: Build the frontend image to catch Docker or nginx syntax issues**

Run:

```bash
docker build -t easyarchive-ui:test ./easyarchive-ui
```

Expected: SUCCESS with a final nginx-based image containing `/usr/share/nginx/html/index.html`.

- [ ] **Step 4: Verify the existing frontend build still passes outside Docker**

Run:

```bash
npm --prefix easyarchive-ui run build
```

Expected: PASS with Vite output written to `easyarchive-ui/dist`.

- [ ] **Step 5: Commit the frontend containerization slice**

```bash
git add easyarchive-ui/Dockerfile easyarchive-ui/nginx.conf
git commit -m "feat: containerize frontend for nginx deployment"
```

### Task 3: Containerize The Backend Jar Build And Runtime

**Files:**
- Create: `easyarchive-starter/Dockerfile`

- [ ] **Step 1: Add the multi-stage backend Dockerfile**

Create `easyarchive-starter/Dockerfile` that builds from the repository root context and packages the starter module:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY easyarchive-common ./easyarchive-common
COPY easyarchive-core ./easyarchive-core
COPY easyarchive-starter ./easyarchive-starter

RUN mvn -pl easyarchive-starter -am clean package -DskipTests

FROM eclipse-temurin:11-jre
WORKDIR /app

COPY --from=build /workspace/easyarchive-starter/target/easyarchive-starter.jar /app/easyarchive-starter.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/easyarchive-starter.jar"]
```

When wiring Compose later, make sure the build context is the repository root so this Dockerfile can access sibling modules.

- [ ] **Step 2: Build the backend image directly**

Run:

```bash
docker build -f easyarchive-starter/Dockerfile -t easyarchive-starter:test .
```

Expected: SUCCESS with Maven producing `easyarchive-starter/target/easyarchive-starter.jar` in the build stage and a final JRE image exposing `8080`.

- [ ] **Step 3: Smoke-check that the image starts and fails only on missing database**

Run:

```bash
docker run --rm -e MYSQL_HOST=127.0.0.1 -e MYSQL_PORT=3306 -e MYSQL_DATABASE=openquartz -e MYSQL_USER=root -e MYSQL_PASSWORD=123456 easyarchive-starter:test
```

Expected: the jar starts reading externalized datasource properties. If no local MySQL is available, the process may fail on connection creation, but it must no longer fail because the jar path is wrong or because config placeholders are unresolved.

- [ ] **Step 4: Commit the backend containerization slice**

```bash
git add easyarchive-starter/Dockerfile
git commit -m "feat: add backend runtime image"
```

### Task 4: Wire Compose, Persistence, And Operator Defaults

**Files:**
- Create: `compose.yaml`
- Create: `.env.example`
- Create: `docs/deployment/docker-compose.md`

- [ ] **Step 1: Create the Compose file**

Create `compose.yaml` with three services, MySQL healthcheck, frontend-only port exposure, and a named data volume:

```yaml
services:
  mysql:
    image: mysql:8.0
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE:-easy_archive}
      MYSQL_USER: ${MYSQL_USER:-easyarchive}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-easyarchive123}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123456}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docs/database/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD --silent"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  backend:
    build:
      context: .
      dockerfile: easyarchive-starter/Dockerfile
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: ${MYSQL_DATABASE:-easy_archive}
      MYSQL_USER: ${MYSQL_USER:-easyarchive}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-easyarchive123}
      MYSQL_TIMEZONE: ${MYSQL_TIMEZONE:-Asia/Shanghai}

  frontend:
    build:
      context: ./easyarchive-ui
      dockerfile: Dockerfile
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "80:80"

volumes:
  mysql_data:
```

Use the MySQL init mount because a fresh `mysql_data` volume needs schema bootstrap to satisfy the starter’s first connection.

- [ ] **Step 2: Add the environment example file**

Create `.env.example`:

```dotenv
MYSQL_DATABASE=easy_archive
MYSQL_USER=easyarchive
MYSQL_PASSWORD=easyarchive123
MYSQL_ROOT_PASSWORD=root123456
MYSQL_TIMEZONE=Asia/Shanghai
```

Do not commit a real `.env` file with secrets.

- [ ] **Step 3: Add deployment documentation**

Create `docs/deployment/docker-compose.md` with operator steps:

```md
# Docker Compose Deployment

## Prerequisites

- Docker
- Docker Compose plugin
- Host port `80` available

## Startup

1. Copy `.env.example` to `.env` and adjust credentials if needed.
2. Run `docker compose up -d --build`.
3. Open `http://localhost/`.

## Notes

- The frontend is the only published service.
- `/api/v1` is reverse proxied to the backend container.
- MySQL data is persisted in the `mysql_data` named volume.
- The first startup initializes schema from `docs/database/schema.sql`.

## Shutdown

- `docker compose down`
- `docker compose down -v` to remove MySQL data
```

- [ ] **Step 4: Validate the Compose model before bringing up containers**

Run:

```bash
docker compose config
```

Expected: SUCCESS with normalized YAML showing one published port (`80:80`), one named volume (`mysql_data`), and `backend` environment values pointing to the `mysql` service.

- [ ] **Step 5: Commit the orchestration slice**

```bash
git add compose.yaml .env.example docs/deployment/docker-compose.md
git commit -m "feat: add docker compose deployment"
```

### Task 5: Run End-To-End Verification And Adjust Runtime Issues

**Files:**
- Modify if needed after verification: `compose.yaml`
- Modify if needed after verification: `easyarchive-ui/nginx.conf`
- Modify if needed after verification: `easyarchive-starter/src/main/resources/application.yml`
- Modify if needed after verification: `docs/deployment/docker-compose.md`

- [ ] **Step 1: Build and start the stack**

Run:

```bash
docker compose up -d --build
```

Expected: all three services create successfully; `mysql` becomes healthy; `backend` starts after healthcheck passes; `frontend` publishes port `80`.

- [ ] **Step 2: Inspect service status and logs**

Run:

```bash
docker compose ps
docker compose logs mysql --tail=50
docker compose logs backend --tail=100
docker compose logs frontend --tail=50
```

Expected:

- `mysql` status shows healthy
- `backend` logs show Spring Boot started on port `8080`
- `frontend` logs show nginx worker startup without config errors

- [ ] **Step 3: Verify the browser surface and reverse proxy path**

Run:

```bash
curl -I http://localhost/
curl -i http://localhost/api/v1/auth/login
```

Expected:

- `curl -I /` returns `HTTP/1.1 200 OK` from nginx
- `/api/v1/auth/login` reaches the backend through nginx; the exact status may be `400`, `401`, or `405` depending on endpoint contract, but it must not be nginx `404` and must not fail with connection refused

- [ ] **Step 4: Verify MySQL persistence behavior**

Run:

```bash
docker compose down
docker compose up -d
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;"
```

Expected: the configured database still exists after restart because the `mysql_data` named volume was reused.

- [ ] **Step 5: Fix only observed runtime issues, rerun the affected checks, and document the outcome**

If verification exposes issues, apply the smallest corrective change and rerun the narrowest confirming command. Typical corrections:

```yaml
# compose.yaml
backend:
  environment:
    MYSQL_TIMEZONE: Asia/Shanghai
```

```nginx
# easyarchive-ui/nginx.conf
location /api/ {
    proxy_pass http://backend:8080;
}
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:openquartz}?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=${MYSQL_TIMEZONE:Asia/Shanghai}
```

After each fix, rerun only the failing check first, then rerun:

```bash
docker compose ps
curl -I http://localhost/
```

- [ ] **Step 6: Commit the verified final state**

```bash
git add compose.yaml .env.example easyarchive-ui/Dockerfile easyarchive-ui/nginx.conf easyarchive-starter/Dockerfile easyarchive-starter/src/main/resources/application.yml easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/config/ContainerDatasourcePropertiesTest.java docs/deployment/docker-compose.md
git commit -m "feat: ship docker compose deployment"
```
