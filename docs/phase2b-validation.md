# Phase 2b validation checklist

Phase 2b delivers: **`GET /api/dashboard`** (JSON), **TanStack Query** (`QueryClientProvider`, `['me']` + `['dashboard']` queries), **dashboard UI parity** with the Thymeleaf dashboard (stats, endpoints table, heartbeats, add/edit/toggle/delete via legacy MVC POST), and **Vite proxy** for `/endpoints` and `/heartbeats` so form posts work from `:5173`.

## Automated (run locally)

| Step | Command | Expected |
|------|---------|----------|
| Frontend lint | `cd frontend && npm.cmd run lint` | Exit 0 |
| Frontend build | `cd frontend && npm.cmd run build` | Exit 0; `frontend/dist/` updated |
| Backend compile | `mvn -DskipTests compile` (or IDE build) | No errors; `DashboardApiController`, DTOs compile |

## Manual — dev stack

1. **Spring Boot** on `http://localhost:8080`.
2. **Vite** on `http://localhost:5173` (`cd frontend && npm.cmd run dev`).
3. Sign in (Google OAuth) and open **`http://localhost:5173/dashboard`**.

### Checks

| # | Action | Expected |
|---|--------|----------|
| 1 | DevTools → Network: reload dashboard | `GET /api/dashboard` → **200** JSON with `user`, `endpointCount`, `upCount`, `downCount`, `endpoints`, `heartbeats`, `baseUrl`, `flashSuccess`. |
| 2 | UI | Welcome line, **Add monitor**, three stat cards, **Your endpoints** table (or empty state), **Heartbeat monitors** section. |
| 3 | **Add monitor** → HTTP | Form posts to `/endpoints/add` (proxied); table refreshes; success flash may appear on next full-page Thymeleaf redirect (SPA uses query invalidation). |
| 4 | **Add monitor** → Heartbeat | Posts to `/heartbeats`; new row with copyable ping URL `baseUrl + /heartbeat/{token}`. |
| 5 | Row actions | **View** opens `/endpoints/{id}` (Thymeleaf detail, proxied). **Edit** opens dialog; save posts `/endpoints/{id}/edit`. **Pause/Resume** posts toggle. **Delete** confirms and posts delete. |
| 6 | Heartbeat **Delete** | Posts `/heartbeats/{id}/delete`; list updates. |
| 7 | `?subscription=success` | Open `http://localhost:5173/dashboard?subscription=success` → green success alert (subscription activated copy from API). |
| 8 | Session expiry on API | If another `/api/**` returns 401, `api:unauthorized` clears session and redirects to `/` (unchanged from 2a). |

## Notes

- **MVC vs SPA**: Add/edit/toggle/delete still hit Spring `@Controller` routes (302 → `/dashboard`). The SPA treats **302** as success and **invalidates** `['dashboard']` (no full navigation to Thymeleaf dashboard).
- **Endpoint detail** (`/endpoints/{id}`) remains **Thymeleaf** until a later phase.
- **Production**: When the SPA is served from the same origin as Spring, the same relative URLs apply without Vite.

## Out of scope (later phases)

- REST CRUD for monitors (replace form posts).
- Bundling SPA into the JAR / single-origin deploy.
