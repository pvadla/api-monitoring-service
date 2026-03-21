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

1. `cd frontend && npm run build`
2. `mvn package` — `maven-resources-plugin` copies `frontend/dist/**` → `target/classes/static/` during `prepare-package`.

`mvn compile` does not copy the SPA; use `mvn package` (or copy `frontend/dist` into `src/main/resources/static` manually for experiments).

## Removed / legacy

- `HomeController` no longer serves Thymeleaf for `/`, `/about`, `/dashboard` (React owns those routes).
- `ContactController` **GET** removed; **POST** `/contact` remains for legacy forms; SPA should use `POST /api/public/contact`.
