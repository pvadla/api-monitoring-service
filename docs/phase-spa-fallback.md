# SPA static hosting + `index.html` fallback

## Behavior

- `spring.web.resources.add-mappings=false` disables Spring Boot’s default static `/**` mappings so we register **one** resource chain (see `SpaWebMvcConfigurer`).
- `SpaResourceResolver` serves `classpath:/static/index.html` when no file exists for the path, **except** for explicit prefixes (API, OAuth, webhooks, heartbeat ping, dev consoles, etc.).
- Thymeleaf `@Controller` routes (e.g. `/endpoints/{id}`, `/settings/profile`, `/status/{slug}`, `/incidents`) still take precedence over the resource handler via `RequestMappingHandlerMapping` order.

## Exclusions (no SPA fallback)

- `/api/**`
- `/oauth2/**`
- `/login/oauth2/**` (OAuth2 callback; SPA route `/login` still gets `index.html`)
- `/webhooks/**`
- `/heartbeat/**` (public ping URL)
- `/h2-console/**`, `/actuator/**`, `/debug-env*`
- `/css/**`, `/js/**`, `/fonts/**`, `/assets/**`
- Paths whose last segment looks like a static file (`foo.js`, `image.png`, …) except `.html`

## Security (`SecurityConfig`)

Anonymous access for SPA shell and OAuth flow:

- `/oauth2/**`, `/login`, `/login/**`
- `/assets/**`, `/favicon.svg`, `/favicon.ico`
- Existing public routes (`/`, `/about`, `/contact`, `/status/**`, `/api/public/**`, …)

## Build

From the **repository root** (single command):

```bash
mvn compile
```

(or `mvn package`)

1. **`frontend-maven-plugin`** (`generate-resources`): installs Node/npm into `target/node` (first run downloads), runs `npm install` and `npm run build` in `frontend/`, producing **`frontend/dist/`** (gitignored).
2. **`maven-resources-plugin`** (`process-resources`): copies **`frontend/dist/**`** → **`target/classes/static/`** (only in the build output; not committed).

**Skip frontend build** (e.g. Java-only work; you must already have `frontend/dist/`):

```bash
mvn compile -Dskip.npm=true
```

**Daily UI work** still uses `cd frontend && npm run dev` (Vite); Maven is for reproducible builds and CI.

**Note:** `src/main/resources/static/` in Git may only contain legacy files (e.g. `css/`). Do not commit `frontend/dist/`, `index.html`, or `assets/` under `static/` — see root `.gitignore`.

## Removed / legacy

- `HomeController` no longer serves Thymeleaf for `/`, `/about`, `/dashboard` (React owns those routes).
- `ContactController` **GET** removed; **POST** `/contact` remains for legacy forms; SPA should use `POST /api/public/contact`.
