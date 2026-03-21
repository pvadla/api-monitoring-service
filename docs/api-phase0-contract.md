# Phase 0 — API contract (Thymeleaf → React migration)

This document defines the **target JSON API** for replacing server-rendered Thymeleaf pages. No implementation work beyond this spec is implied by Phase 0.

**Auth:** Session cookies (Spring Security OAuth2). Browser calls use `credentials: 'include'`. Login remains `GET /oauth2/authorization/google` (or your configured provider).

**Conventions:**

- JSON field names: **camelCase**.
- Timestamps: **ISO-8601** strings (e.g. `2026-03-19T14:30:00`) unless noted; align with existing `LocalDateTime` / `Instant` serialization when implementing.
- Errors: `{ "error": "message" }` or `{ "success": false, "error": "..." }` consistent with existing `/api/payment/*` style.

---

## 1. Already implemented (keep stable for React)

These exist today; React should call them as-is.

**Phase 2a addition:** `GET /api/me` returns the signed-in user JSON (see `UserResponse`); anonymous requests to `/api/**` receive **401** without an OAuth redirect (see `SecurityConfig` + `MeApiController`).

**Phase 3–5 addition:** JSON REST for endpoints, heartbeats, incidents, profile & status settings, and public status + contact — see §2 and [phase3-5-validation.md](phase3-5-validation.md).

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| `POST` | `/api/payment/create-order` | User | `plan` query param — returns `orderId`, `amount`, `keyId`, `currency` |
| `POST` | `/api/payment/verify` | User | Body: `razorpay_payment_id`, `razorpay_order_id`, `plan` |
| `POST` | `/api/payment/create-subscription-checkout` | User | `plan` query param — returns `subscriptionId`, `keyId`, `planSlug` |
| `POST` | `/api/payment/verify-subscription` | User | Body: `razorpay_payment_id`, `razorpay_subscription_id`, `razorpay_signature` |
| `GET` | `/api/subscription/current` | User | Returns `subscriptions[]`, `tier` |
| `POST` | `/api/subscription/cancel` | User | Body: `razorpaySubscriptionId`, `atPeriodEnd` |
| `POST` | `/api/subscription/change-plan` | User | Body: `newPlanSlug` |

**Subscription item shape** (from `GET /api/subscription/current`):

```json
{
  "id": 1,
  "razorpaySubscriptionId": "sub_xxx",
  "planSlug": "STARTER",
  "status": "active",
  "currentStart": "",
  "currentEnd": "",
  "cancelAtPeriodEnd": false
}
```

---

## 2. Screen → route mapping (MVC today → proposed REST)

| Screen | Current MVC | Proposed REST (new) |
|--------|-------------|---------------------|
| Landing | `GET /` | `GET /api/public/landing` (optional) — see §4 |
| About | `GET /about` | Static in React only, **or** `GET /api/public/about` if CMS later |
| Contact | `GET/POST /contact` | `POST /api/public/contact` |
| Dashboard | `GET /dashboard` | `GET /api/me` + `GET /api/dashboard` |
| Endpoint detail | `GET /endpoints/{id}` | `GET /api/endpoints/{id}` |
| Add endpoint | `POST /endpoints/add` | `POST /api/endpoints` |
| Edit endpoint | `POST /endpoints/{id}/edit` | `PUT /api/endpoints/{id}` |
| Delete / toggle / status visibility | `POST .../delete`, `/toggle`, `/toggle-status-visibility` | `DELETE /api/endpoints/{id}`, `POST /api/endpoints/{id}/toggle`, `POST /api/endpoints/{id}/toggle-status-visibility` |
| Heartbeats | `POST /heartbeats`, `POST /heartbeats/{id}/delete` | `POST /api/heartbeats`, `DELETE /api/heartbeats/{id}` |
| Incidents | `GET /incidents`, posts for resolve/status | `GET /api/incidents`, `POST /api/incidents/{id}/resolve`, `POST /api/incidents/{id}/status` |
| Profile | `GET/POST /settings/profile`, delete account | `GET/PUT /api/settings/profile`, `POST /api/settings/profile/delete-account` |
| Public status | `GET /status/{slug}` | `GET /api/public/status/{slug}` |
| Status settings | `GET/POST /settings/status` | `GET/PUT /api/settings/status` |

**Note:** Public heartbeat ping `GET|POST /heartbeat/{token}` stays **non-JSON** integration surface; do not fold into SPA API.

---

## 3. Proposed DTOs (response shapes)

### 3.1 `UserDto` (subset for clients; no secrets)

Used in `/api/me`, `/api/dashboard`, and nested where needed.

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | |
| `email` | string | |
| `name` | string | nullable |
| `picture` | string | nullable URL |
| `subscriptionTier` | string | e.g. `FREE`, `STARTER`, `PRO` |
| `statusSlug` | string | nullable |
| `statusPageTitle` | string | nullable |
| `statusPageLogoUrl` | string | nullable |
| `notifyOnEndpointDown` | boolean | |
| `notifyOnEndpointRecovery` | boolean | |

Omit internal fields (`razorpayCustomerId`, etc.) unless a product needs them.

### 3.2 `EndpointDto`

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | |
| `name` | string | |
| `url` | string | |
| `checkInterval` | number | minutes |
| `isActive` | boolean | |
| `isUp` | boolean | |
| `failureCount` | number | optional |
| `lastChecked` | string (ISO) | nullable |
| `showOnStatusPage` | boolean | |
| `expectedBodySubstring` | string | nullable |
| `sslExpiresAt` | string (ISO) | nullable |

### 3.3 `HeartbeatMonitorDto`

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | |
| `name` | string | |
| `token` | string | **sensitive** — only for authenticated owner; used to build ping URL |
| `expectedIntervalMinutes` | number | |
| `lastPingAt` | string (ISO) | nullable |
| `isActive` | boolean | optional |

### 3.4 `IncidentDto`

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | |
| `title` | string | |
| `description` | string | nullable |
| `status` | string | `INVESTIGATING` \| `IDENTIFIED` \| `MONITORING` \| `RESOLVED` |
| `startedAt` | string (ISO) | |
| `resolvedAt` | string (ISO) | nullable |
| `failureReason` | string | nullable |
| `downtimeDurationMinutes` | number | nullable |
| `autoGenerated` | boolean | |
| `endpointId` | number | nullable |

### 3.5 `EndpointCheckDto`

| Field | Type | Notes |
|-------|------|--------|
| `id` | number | |
| `checkedAt` | string (ISO) | |
| `responseTimeMs` | number | nullable |
| `statusCode` | number | nullable |
| `isUp` | boolean | |
| `errorMessage` | string | nullable |

### 3.6 `EndpointDetailResponse`

`GET /api/endpoints/{id}`

| Field | Type | Notes |
|-------|------|--------|
| `endpoint` | EndpointDto | |
| `checks` | EndpointCheckDto[] | e.g. last 50, order documented in implementation |
| `uptimePct` | string | e.g. `"99.99"` |
| `avgResponseMs` | string | e.g. `"120"` |

### 3.7 `DashboardResponse`

`GET /api/dashboard`

| Field | Type | Notes |
|-------|------|--------|
| `user` | UserDto | |
| `endpointCount` | number | |
| `upCount` | number | |
| `downCount` | number | |
| `endpoints` | EndpointDto[] | |
| `heartbeats` | HeartbeatMonitorDto[] | |
| `baseUrl` | string | app base for heartbeat URL construction |
| `flashSuccess` | string | optional; maps `?subscription=success` or server message |

### 3.8 `PublicStatusResponse`

`GET /api/public/status/{slug}`

| Field | Type | Notes |
|-------|------|--------|
| `slug` | string | |
| `pageTitle` | string | |
| `logoUrl` | string | nullable |
| `overallStatusLabel` | string | |
| `statusKind` | string | e.g. `all-up` \| `issues` \| `none` |
| `endpoints` | EndpointDto[] | only those visible on status page |
| `incidents` | IncidentDto[] | |

### 3.9 `StatusSettingsResponse` / request

`GET` / `PUT /api/settings/status`

**Response:**

| Field | Type |
|-------|------|
| `statusSlug` | string | nullable |
| `statusPageTitle` | string | nullable |
| `statusPageLogoUrl` | string | nullable |

**PUT body:** same three fields (optional fields).

### 3.10 `ProfileUpdateRequest`

`PUT /api/settings/profile`

| Field | Type |
|-------|------|
| `name` | string | optional |
| `notifyOnEndpointDown` | boolean | optional |
| `notifyOnEndpointRecovery` | boolean | optional |

### 3.11 `ContactRequest`

`POST /api/public/contact`

| Field | Type |
|-------|------|
| `name` | string | required |
| `email` | string | required |
| `subject` | string | optional |
| `message` | string | required |

**Response:** `{ "success": true }` or `{ "error": "..." }`.

### 3.12 `CreateEndpointRequest`

`POST /api/endpoints`

| Field | Type |
|-------|------|
| `name` | string |
| `url` | string |
| `checkInterval` | number |
| `expectedBodySubstring` | string | optional |

### 3.13 `IncidentStatusUpdateRequest`

`POST /api/incidents/{id}/status`

| Field | Type |
|-------|------|
| `status` | string | enum `IncidentStatus` |

---

## 4. Optional public landing payload

`GET /api/public/landing` (if you want pricing parity with current `index.html` without duplicating config)

```json
{
  "starterAmountDisplay": 580
}
```

(`starterAmountDisplay` = rupees display unit from `razorpay.starter-amount-paise / 100`.)

---

## 5. Security matrix (reference)

Align with `[SecurityConfig.java](../src/main/java/com/api/monitor/config/SecurityConfig.java)`:

- **Public:** `/`, `/about`, `/contact` (GET), `/status/**`, `/heartbeat/**`, `/webhooks/**`, static assets.
- **Authenticated:** dashboard, endpoints, incidents, profile, settings, payment APIs.

For new REST: mirror the same rules — e.g. `GET /api/public/**` permitAll; `GET/PUT /api/settings/**` authenticated.

---

## 6. Phase 0 deliverable

This file is the **single source of truth** for Phase 1+ implementation. Update it if the contract changes before coding.

**OpenAPI:** Optional follow-up — generate `openapi.yaml` from this document or from annotations once controllers exist.
