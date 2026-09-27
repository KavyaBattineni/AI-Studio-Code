# docker/

The backend and frontend each ship their own `Dockerfile` right next to their source
(`backend/Dockerfile`, `frontend/Dockerfile`), and `docker-compose.yml` at the repo root
wires them together with MySQL. This folder is reserved for any shared, cross-service
Docker assets you add later (e.g. a standalone reverse-proxy/nginx config in front of both
containers, shared compose overrides for staging vs. production, etc.) - there's nothing
required here to run `docker compose up --build`.
