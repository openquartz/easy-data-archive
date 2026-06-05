# Docker Compose Deployment

## Prerequisites

- Docker Desktop or Docker Engine
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
- A fresh MySQL volume initializes schema from `docs/database/schema.sql`.
- The Compose stack uses `mysql:5.7` to stay compatible with the current JDBC driver in this repository.

## Shutdown

- `docker compose down`
- `docker compose down -v` to remove MySQL data
