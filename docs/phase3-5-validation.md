# Phase 3–5 validation (REST JSON APIs)

Implements the **Phase 0 contract** routes for: **endpoints CRUD + detail**, **heartbeats JSON**, **incidents**, **profile + delete account**, **status settings**, **public status + contact** — see [api-phase0-contract.md](api-phase0-contract.md) §2–§3.

## Security

- `GET|POST /api/public/**` — **permitAll** (see `SecurityConfig`: `/api/public/**`).
- All other `/api/**` routes below require an authenticated session (same as existing `/api/me`); anonymous calls get **401 JSON** (no OAuth redirect).

## New endpoints (summary)

| Method | Path | Notes |
|--------|------|--------|
| `GET` | `/api/endpoints/{id}` | `EndpointDetailResponse` (checks, uptime %, avg ms) |
| `POST` | `/api/endpoints` | JSON body `CreateEndpointRequest` → **201** + `EndpointResponse` |
| `PUT` | `/api/endpoints/{id}` | `UpdateEndpointRequest` |
| `DELETE` | `/api/endpoints/{id}` | **204**; deletes checks first |
| `POST` | `/api/endpoints/{id}/toggle` | pause/resume |
| `POST` | `/api/endpoints/{id}/toggle-status-visibility` | status page visibility |
| `POST` | `/api/heartbeats` | `CreateHeartbeatRequest` → **201** |
| `DELETE` | `/api/heartbeats/{id}` | **204** |
| `GET` | `/api/incidents` | list (latest 100) |
| `POST` | `/api/incidents/{id}/resolve` | sets **RESOLVED** |
| `POST` | `/api/incidents/{id}/status` | body `{ "status": "INVESTIGATING" \| … }` |
| `GET` | `/api/settings/profile` | `UserResponse` |
| `PUT` | `/api/settings/profile` | `ProfileUpdateRequest` |
| `POST` | `/api/settings/profile/delete-account` | deletes data + session; `{ "success": true }` |
| `GET` | `/api/settings/status` | `StatusSettingsResponse` |
| `PUT` | `/api/settings/status` | `StatusSettingsUpdateRequest` |
| `GET` | `/api/public/status/{slug}` | `PublicStatusResponse` (404 if unknown slug) |
| `POST` | `/api/public/contact` | `ContactRequest` → `{ "success": true }` |

## Automated

```bash
mvn -DskipTests compile
```

## Manual (with session cookie)

1. **Authenticated:** `GET /api/endpoints/1`, `GET /api/incidents`, `GET /api/settings/profile` (replace IDs as needed).
2. **Public (no auth):** `GET /api/public/status/{slug}`, `POST /api/public/contact` with JSON body.
3. **Errors:** invalid payload → **400** + `{ "error": "..." }`; free-tier monitor limit → **409** for create endpoint/heartbeat.

## Notes

- Legacy **Thymeleaf/MVC** routes (`/endpoints/...`, `/incidents`, etc.) remain unchanged.
- Account deletion via API also removes **heartbeat monitors** (in addition to endpoints/checks/incidents).
