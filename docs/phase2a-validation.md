# Phase 2a validation checklist

Phase 2a delivers: **App shell**, **React Router**, **protected routes**, **login / logout**, **`apiClient` + global 401 handling**, **`ErrorBoundary`**, and backend **`GET /api/me`** + **`401` JSON for `/api/**`** (no dashboard data — Phase 2b).

## Automated (run locally)

| Step | Command | Expected |
|------|---------|----------|
| Frontend lint | `cd frontend && npm.cmd run lint` | Exit 0 |
| Frontend build | `cd frontend && npm.cmd run build` | Exit 0; `frontend/dist/` updated |
| Backend compile | `mvn -DskipTests compile` (or IDE build) | No errors; `MeApiController`, `UserResponse`, `SecurityConfig` compile |

## Manual — dev stack

1. **Spring Boot** on `http://localhost:8080`.
2. **Vite** on `http://localhost:5173` (`cd frontend && npm.cmd run dev`).

### Checks

| # | Action | Expected |
|---|--------|----------|
| 1 | Open `http://localhost:5173/` | Home loads inside **AppShell** (nav: Home, About, Contact, Dashboard, Sign in). |
| 2 | Open DevTools → Network; reload | `GET /api/me` → **401** when logged out (JSON / empty body), **no** 302 to Google. |
| 3 | Click **Sign in with Google** | OAuth flow runs (via proxy). After success, Spring may redirect to **`http://localhost:8080/dashboard`** (Thymeleaf). For React dev, open **`http://localhost:5173/dashboard`** manually — session cookie is shared for `localhost`. |
| 4 | On `5173` after session exists, reload | `GET /api/me` → **200** with JSON user; header shows name/email, **Plan**, **Log out**. |
| 5 | Click **Dashboard** when signed in | Protected **Dashboard** with monitor data (see [phase2b-validation.md](phase2b-validation.md)). |
| 6 | Sign out; go to **`/dashboard`** | Redirect to **`/`** (protected route). |
| 7 | With session, call a protected API that returns 401 (e.g. expired session simulated) | `api:unauthorized` clears session UX: user cleared and redirect to `/` (see `AuthContext` + `apiClient`). |

## OAuth dev note

Until the SPA is served from the same origin as Spring in production, Google OAuth redirect URIs typically hit **port 8080**. After login you may need to open the Vite app on **5173** again; the session cookie for `localhost` still applies.

## Out of scope (next phases)

- Phase **2b**: `GET /api/dashboard`, TanStack Query, real dashboard UI.
- Maven copying `frontend/dist` into the JAR, SPA `index.html` forwarding on 8080.
