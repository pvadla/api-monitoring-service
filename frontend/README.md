# APIWatch frontend (Phase 1)

Vite + React + TypeScript + Tailwind CSS v4 + [shadcn/ui](https://ui.shadcn.com/).

## Prerequisites

- Node.js 20+ (LTS recommended)
- Spring Boot backend on `http://localhost:8080` (see project root)

## Development

From this directory:

```bash
npm install
npm run dev
```

### Windows PowerShell: `running scripts is disabled on this system`

PowerShell may run `npm.ps1`, which is blocked by the default execution policy. Use **one** of these:

1. **Call the `.cmd` shim** (no policy change):
   ```powershell
   npm.cmd install
   npm.cmd run dev
   ```
2. **Allow scripts for your user** (one-time, recommended if you use npm often in PowerShell):
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```
   Then `npm run dev` works as usual.
3. Use **Command Prompt** (`cmd.exe`) or **Git Bash** instead of PowerShell.

Open `http://localhost:5173`. The Vite dev server **proxies** these paths to Spring:

- `/api`
- `/oauth2`
- `/login`
- `/logout`
- `/endpoints` (legacy MVC: add/edit/toggle/delete, detail page)
- `/heartbeats` (legacy MVC: create/delete heartbeat monitors)

Use **relative** URLs in code (e.g. `fetch('/api/...')`) so the proxy keeps cookies same-origin.

## Build (validation)

```bash
npm run build
```

Output goes to `frontend/dist/` (gitignored).

### Spring Boot / classpath (repo root)

Maven runs **`npm install`** and **`npm run build`** automatically, then copies **`frontend/dist/**` → `target/classes/static/`**:

```bash
cd ..
mvn compile
```

See [docs/phase-spa-fallback.md](../docs/phase-spa-fallback.md). To skip the Node/npm steps (requires an existing `frontend/dist/`): `mvn compile -Dskip.npm=true`.

## Environment

See [`.env.example`](.env.example). Copy to `.env.local` if you need `VITE_*` variables.

## Phase scope

**Phase 1** scaffolds Vite + Tailwind + shadcn and the dev proxy.

**Phase 2a** adds React Router, `AppShell`, `GET /api/me` session state, protected `/dashboard`, `apiClient` (401 handling), `ErrorBoundary`, and logout.

**Phase 2b** adds **`GET /api/dashboard`**, **TanStack Query** (`QueryProvider` + dashboard/`me` queries), and the **dashboard UI** (stats, endpoints, heartbeats, dialogs) with form posts to the existing Spring MVC routes.

Thymeleaf on port **8080** remains available for routes not yet migrated in the SPA (e.g. endpoint detail).
