# Repository Guidelines

## Project Structure & Module Organization
NavBI Pro combines a Spring Boot backend with a Vue 3 frontend. Backend code lives in `backend/src/main/java/com/navbi`, grouped by `auth`, `nav`, `track`, `bi`, `download`, `apilog`, `counter`, `config`, and `common`. Backend tests mirror this under `backend/src/test/java`; SQL, profiles, and seed data are in `backend/src/main/resources`. Frontend code is in `frontend/src`: routes in `router`, API helpers in `api`, Pinia state in `stores`, public screens in `views`, and admin screens in `views/admin`. Deployment files include `docker-compose.yml`, both `Dockerfile`s, and `frontend/nginx.conf`.

## Build, Test, and Development Commands
- `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local`: run the API with the local H2 profile; Redis can be absent because counters degrade to database reads.
- `cd backend && mvn test`: run backend unit and MockMvc integration tests.
- `cd frontend && npm install`: install Vite/Vue dependencies from `package-lock.json`.
- `cd frontend && npm run dev`: start the frontend at `http://localhost:5173`.
- `cd frontend && npm run build`: produce production frontend assets.
- `docker compose up -d --build`: build and run Postgres, Redis, backend, and frontend.

## Coding Style & Naming Conventions
Use Java 17 and Spring Boot conventions: constructor injection, 4-space indentation, PascalCase classes, and lowerCamelCase methods and fields. Keep Java code in the matching domain package under `com.navbi`. Vue single-file components use 2-space indentation, PascalCase view files, and Composition API with `<script setup>`. No frontend lint script is configured, so match nearby formatting.

## Testing Guidelines
Backend tests use JUnit, Spring Boot Test, MockMvc, and the H2 profile from `application-test.yml`. Name tests `*Test.java` and place them beside the relevant domain package. Add or update tests before production changes; target at least 80% coverage for changed backend logic even though no coverage gate is configured. The frontend has no test runner, so verify UI changes with `npm run build` and a manual Vite check.

## Commit & Pull Request Guidelines
Git history uses Conventional Commits, often with scopes, such as `feat(nav): ...` and `fix(frontend): ...`. Keep commits focused and avoid mixing backend, frontend, and deployment changes unless one feature requires it. Pull requests should include a short summary, linked issue when applicable, commands run for verification, and screenshots for visible UI changes.

## Security & Configuration Tips
Do not commit secrets. Production configuration is supplied through `.env` and `docker-compose.yml` variables such as `NAVBI_ADMIN_PASSWORD`, `NAVBI_JWT_SECRET`, and `NAVBI_DB_PASSWORD`. Keep JWT secrets at least 32 bytes.
Do not commit binary installers. Authenticated client downloads are served from `NAVBI_DOWNLOAD_DIR` and mounted read-only in Docker from `./downloads` to `/opt/navbi/downloads`.

## Production Deployment Context
- **Server IP**: 150.230.47.207
- **SSH Key**: `/Users/weizhang/Downloads/ssh-key-2026-03-27.key`
- **Project Path**: `/home/ubuntu/NavBI`
- **Deploy Command**:
  ```bash
  ssh -i /Users/weizhang/Downloads/ssh-key-2026-03-27.key -o StrictHostKeyChecking=no ubuntu@150.230.47.207 "cd /home/ubuntu/NavBI && git pull && sudo docker compose up -d --build"
  ```
- **Client Downloads**: Upload WARP installers to `/home/ubuntu/NavBI/downloads` on the server. Expected filenames are `Cloudflare_WARP_2026.6.822.0.msi` and `Cloudflare_WARP_2026.6.822.0.pkg`; the backend exposes them through authenticated `/api/downloads/client/windows` and `/api/downloads/client/macos`.
- **Firewall & Security**: Cloudflare Access protects `/admin`. IPTables whitelist is configured to only allow Cloudflare IPs to access ports 80 and 443 (persisted via `iptables-persistent`). Ports 8080, 5432, 6379 are not exposed publicly.
