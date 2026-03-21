# Phase 1 validation checklist

Completed when the following pass on a clean clone (after `npm install` in `frontend/`).

| Check | Command / action | Expected |
|-------|------------------|----------|
| Install | `cd frontend && npm install` | Exit 0, no audit errors blocking |
| Typecheck + production build | `cd frontend && npm run build` | Exit 0; `frontend/dist/` contains `index.html` and assets |
| Dev server (manual) | Terminal 1: Spring on `:8080`. Terminal 2: `cd frontend && npm run dev` (Windows PowerShell: use `npm.cmd run dev` if execution policy blocks `npm`) | Browser: `http://localhost:5173` shows APIWatch Phase 1 screen |
| OAuth proxy (manual) | Click “Sign in with Google” on `:5173` | Request goes to Spring via proxy (`/oauth2/authorization/google`); Google login page loads |

**Out of scope for Phase 1:** Maven copying `dist/` into the JAR, SPA fallback in Spring, React Router, `/api/me` — those are later phases.
