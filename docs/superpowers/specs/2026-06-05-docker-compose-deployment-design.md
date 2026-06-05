# EasyArchive Docker Compose Deployment Design

## Goal

Add a production-oriented Docker Compose deployment for EasyArchive that runs the frontend, backend, and MySQL together with one command.

The deployed stack should expose only the frontend on port `80`. The frontend container should serve built static assets and reverse proxy `/api` traffic to the backend container. The backend should run as a packaged Spring Boot jar and use a Compose-managed MySQL instance with persistent storage.

## Scope

In scope:

- root-level Compose deployment files
- frontend containerization for production static asset serving
- backend containerization for packaged jar execution
- MySQL service orchestration with persistent data storage
- backend configuration changes required to replace hard-coded localhost database settings with environment-driven values
- deployment documentation for build, startup, and default credentials

Out of scope:

- Kubernetes manifests
- CI/CD pipeline changes
- application feature changes
- database schema redesign
- frontend development-server containerization
- multi-environment release orchestration

## Success Criteria

- `docker compose up -d --build` starts `frontend`, `backend`, and `mysql`
- only port `80` is published to the host
- frontend static assets are served by `nginx`
- requests from the browser to `/api/v1` are proxied to the backend container without CORS configuration changes
- backend database connectivity uses container service discovery rather than `localhost`
- MySQL data persists across container restarts through a named volume
- the deployment can be configured without editing source files by changing environment variables

## Deployment Architecture

### Service Topology

The Compose stack will contain three services:

1. `mysql`
2. `backend`
3. `frontend`

Traffic flow:

- browser requests `http://host/`
- `frontend` serves built Vue assets
- browser requests to `/api/v1/**` go to `frontend`
- `frontend` proxies `/api/v1/**` to `backend:8080`
- `backend` connects to `mysql:3306`

This keeps the browser-facing surface to one port and avoids exposing the backend directly.

### Network Model

Use the default Compose bridge network created for the application. Explicit custom network naming is not required because all three services only need private intra-stack communication and Compose service-name DNS is sufficient.

### Persistence Model

Use one named volume for MySQL data, mounted at the image’s standard data directory. Frontend and backend do not require persistent writable volumes for this first deployment design.

## Frontend Container Design

### Runtime Shape

The frontend should use a multi-stage Docker build:

1. Node build stage:
   - install dependencies
   - run `npm run build`
2. Nginx runtime stage:
   - copy built assets into the nginx document root
   - use a custom nginx config

This keeps the runtime image small and avoids shipping the Node toolchain in production.

### Nginx Responsibilities

The custom nginx config should:

- serve the built SPA files
- use `try_files` fallback to `index.html` for client-side routing
- reverse proxy `/api/` to `http://backend:8080`
- forward standard proxy headers such as `Host`, `X-Real-IP`, `X-Forwarded-For`, and `X-Forwarded-Proto`

No separate frontend environment variable for API base URL is needed in Compose because the browser will continue to call relative `/api/v1` paths and nginx will forward them internally.

## Backend Container Design

### Runtime Shape

The backend should use a multi-stage Docker build:

1. Maven build stage:
   - copy project files required for the multi-module build
   - run a packaged build for `easyarchive-starter`
2. JRE runtime stage:
   - copy the built starter jar
   - run it with `java -jar`

This matches the existing Maven module structure and produces a production-style runtime image.

### Configuration Strategy

The backend currently hard-codes MySQL host, database name, username, and password in `application.yml`. That prevents containerized deployment because `localhost` inside the backend container is not the MySQL container.

The design is to replace hard-coded JDBC values with Spring property placeholders backed by environment variables, while preserving the current local-development defaults.

Required configurable values:

- datasource host
- datasource port
- datasource database name
- datasource username
- datasource password
- sync config JDBC values if they target the same management database

Recommended default pattern:

- default host remains `localhost`
- default port remains `3306`
- default database remains the current management database
- default credentials remain current local defaults

This keeps non-container local startup behavior intact while allowing Compose to override the values for container use.

### Startup Ordering

The backend should depend on MySQL with a health-based condition. Compose startup order alone is not enough because the database container can be running before MySQL accepts connections.

The MySQL service will provide a healthcheck. The backend service will use `depends_on` with `condition: service_healthy` so the backend starts after MySQL is ready.

## MySQL Service Design

Use the official MySQL image with:

- database name from environment variables
- root password from environment variables
- optional application username and password from environment variables
- named volume for `/var/lib/mysql`
- healthcheck using `mysqladmin ping`

The first design iteration assumes one MySQL instance serves the management database used by `spring.datasource` and `sync.connection.config`.

This design does not attempt to create or orchestrate separate source and target archive databases. That remains an application-level concern configured later through the UI or data setup process.

## Compose File Design

### File Layout

Planned files:

- `compose.yaml`
- `easyarchive-ui/Dockerfile`
- `easyarchive-ui/nginx.conf`
- `easyarchive-starter/Dockerfile`
- optional `.env.example`

### Compose Responsibilities

The root `compose.yaml` should define:

- service `mysql` using an official image
- service `backend` with local build context
- service `frontend` with local build context
- named volume for MySQL persistence

The Compose file should use:

- `build` for local image creation
- `environment` or `env_file` for runtime configuration
- `depends_on` with MySQL health condition for backend
- host port mapping only for frontend `80:80`

No backend host port publishing is needed in the approved deployment shape.

## Environment Variable Design

The deployment should support a small set of top-level environment variables so operators can change database credentials without editing YAML internals.

Recommended variables:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_HOST`
- `MYSQL_PORT`

In Compose, `MYSQL_HOST` should resolve to `mysql` for the backend service. In local non-container startup, the Spring defaults can continue to use `localhost`.

If `.env.example` is added, it should document safe placeholder values and the relationship between MySQL runtime variables and Spring datasource overrides.

## Error Handling And Risk Control

### Known Risks

- MySQL 8 compatibility with older JDBC defaults should be verified during implementation because this project currently uses the older MySQL connector artifact naming and driver class.
- The backend may require additional JDBC parameters if authentication or timezone behavior differs in containerized MySQL.
- Startup can still fail if the application expects schema state that is not initialized in a fresh database.

### Mitigations

- keep JDBC query parameters aligned with the current working local configuration
- add a MySQL healthcheck before backend startup
- document any required schema initialization step in deployment instructions
- avoid exposing backend directly, reducing accidental bypass of the reverse proxy path

## Testing Strategy

Minimum verification for the deployment implementation:

1. build the images through Compose
2. start the stack with Compose
3. verify `frontend`, `backend`, and `mysql` containers are healthy or running as expected
4. open `http://localhost/` and confirm the frontend loads
5. call a backend endpoint through the frontend proxy path such as `/api/v1/...` and confirm the request reaches the backend
6. restart the stack and verify MySQL data remains present

If the application requires a seeded schema to become usable, verification should distinguish between:

- infrastructure success: containers start and can connect
- product readiness: required tables and seed data exist

## Implementation Outline

1. Add a root `compose.yaml` for `frontend`, `backend`, and `mysql`.
2. Add a production `Dockerfile` for `easyarchive-ui`.
3. Add an `nginx.conf` for SPA serving and `/api` reverse proxying.
4. Add a production `Dockerfile` for `easyarchive-starter`.
5. Update `easyarchive-starter/src/main/resources/application.yml` to use environment-driven datasource values with local defaults.
6. Add a small deployment readme or `.env.example` if needed for operator clarity.
7. Run Compose-based verification and adjust JDBC or proxy details if runtime issues surface.
