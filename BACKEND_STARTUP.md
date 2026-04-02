# Backend Startup

## Recommended Mode
- This repository now standardizes on full Docker Compose deployment.
- Main compose file: [`docker-compose.full.yml`](./docker-compose.full.yml)
- Helper scripts:
  - `powershell -ExecutionPolicy Bypass -File .\docker-start-all.ps1`
  - `powershell -ExecutionPolicy Bypass -File .\docker-stop-all.ps1`
  - `powershell -ExecutionPolicy Bypass -File .\docker-status-all.ps1`

## Direct Commands
- Start:
  - `docker compose -f .\docker-compose.full.yml up --build -d`
- Status:
  - `docker compose -f .\docker-compose.full.yml ps`
- Stop:
  - `docker compose -f .\docker-compose.full.yml down`
- Stop and remove volumes:
  - `docker compose -f .\docker-compose.full.yml down -v`

## Services Included
- `MySQL`
- `Redis`
- `Nacos`
- `RocketMQ`
- `lifeos-user-service`
- `lifeos-task-service`
- `lifeos-note-service`
- `lifeos-ai-service`
- `lifeos-behavior-service`
- `lifeos-gateway`
- `lifeos-web`
- `lifeos-admin-service`
- `lifeos-admin-web`

## Access
- Frontend: `http://127.0.0.1:5173`
- Admin: `http://127.0.0.1:5174`
- Gateway: `http://127.0.0.1:8080`
- Swagger: `http://127.0.0.1:8080/swagger-ui.html`
- Dockerized MySQL host port: `127.0.0.1:13306`

## Admin Console
- Admin UI route: `http://127.0.0.1:5174`
- Admin API route: `/admin-api/**`
- Admin Swagger route: `http://127.0.0.1:8080/service-docs/admin`
- Seed admin account comes from `.env`:
  - `LIFEOS_ADMIN_DEFAULT_USERNAME`
  - `LIFEOS_ADMIN_DEFAULT_PASSWORD`

## Notes
- Container-to-container communication uses internal hostnames such as `mysql`, `redis`, `nacos`, and `rocketmq-namesrv`.
- MySQL initializes schema and seed data automatically on the first startup of a fresh volume.
- Keep `.env` local only. `LIFEOS_JWT_SECRET` should be at least 32 characters. If `LIFEOS_AI_API_KEY` is empty, `lifeos-ai-service` falls back to local mock output.
