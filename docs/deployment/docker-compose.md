# Docker Compose Deployment

## Prerequisites

- Docker Desktop or Docker Engine
- Docker Compose plugin
- Host port `80` available

## Services

- `mysql`: official MySQL image, internal-only, data persisted in `mysql_data`
- `backend`: built from `easyarchive-starter/Dockerfile`, internal-only, connects to `mysql` over the Compose network
- `frontend`: built from `easyarchive-ui/Dockerfile`, published on host port `80`, reverse proxies `/api/` to `backend:8080`

## Startup

1. Copy `.env.example` to `.env` and adjust credentials if needed.
2. Run `docker compose up -d --build`.
3. Open `http://localhost/`.

## Build And Image Settings

- `MYSQL_IMAGE` defaults to `mysql:8.0`
- `BACKEND_IMAGE` defaults to `easyarchive-backend:local`
- `FRONTEND_IMAGE` defaults to `easyarchive-frontend:local`
- `FRONTEND_PORT` defaults to `80`
- `MYSQL_PORT` defaults to `3306` and is used for backend-to-MySQL container networking

The stack keeps the current exposure policy unchanged: only the frontend publishes a host port. `backend` and `mysql` remain reachable only inside the Compose network.

## Notes

- The frontend is the only published service.
- `/api/v1` is reverse proxied to the backend container by the frontend nginx container.
- MySQL data is persisted in the `mysql_data` named volume.
- A fresh MySQL volume initializes schema from `docs/database/schema.sql`.
- The Compose stack currently uses `mysql:8.0`.

## Common Operations

- Rebuild local images: `docker compose build`
- View service status: `docker compose ps`
- View logs: `docker compose logs -f frontend backend mysql`

## Shutdown

- `docker compose down`
- `docker compose down -v` to remove MySQL data
