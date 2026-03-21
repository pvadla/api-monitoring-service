# Razorpay Checkout (React SPA)

This project’s **server** exposes payment endpoints under `/api/payment/*` (see `docs/api-phase0-contract.md`). The **Thymeleaf** landing page loads Razorpay via a static `<script>` tag; the **React** app uses a small wrapper so the script is loaded **on demand** and handlers do not capture **stale React state**.

## Files

| Path | Role |
|------|------|
| `frontend/src/lib/razorpay/script.ts` | Injects `https://checkout.razorpay.com/v1/checkout.js` once, idempotent. |
| `frontend/src/lib/razorpay/checkout.ts` | `openRazorpayCheckout({ options, callbacksRef })` — wires `handler`, `modal.ondismiss`, `payment.failed`. |
| `frontend/src/lib/razorpay/useRazorpayCheckout.ts` | Hook: `mountRef`, `callbacksRef`, `preload`, `open`. |
| `frontend/src/lib/razorpay/types.ts` | Minimal `RazorpayOptions` / `window.Razorpay` typings. |

## Backend contract (summary)

- **One-time order:** `POST /api/payment/create-order?plan=STARTER` → `{ orderId, amount, keyId, currency }`  
  Then open Checkout with `order_id`, `amount` (paise), `key`, `currency`.  
  Success → `POST /api/payment/verify` with `razorpay_payment_id`, `razorpay_order_id`, `plan`.

- **Subscription:** `POST /api/payment/create-subscription-checkout?plan=STARTER` → `{ subscriptionId, keyId, planSlug }`  
  Then open Checkout with `subscription_id`, `key` (no `order_id` / `amount` for pure subscription flow per Razorpay docs).  
  Success → `POST /api/payment/verify-subscription` with payment id, subscription id, signature.

All calls require an authenticated session (`credentials: 'include'` / app cookie).

## Why `callbacksRef` and `queueMicrotask`

Razorpay invokes `handler`, `modal.ondismiss`, and `payment.failed` **outside React’s render/commit cycle**. If you close over `useState` values from an old render, you get **stale data**.

**Pattern:** keep mutable handlers on `useRef`:

```ts
callbacksRef.current = {
  onSuccess: (r) => { /* latest logic */ },
  onPaymentFailed: () => {},
  onDismiss: () => setBusy(false),
}
await open({ key, order_id, amount, currency: 'INR', name: 'APIWatch' })
```

The wrapper reads `callbacksRef.current` when Razorpay fires. Inner work is scheduled with **`queueMicrotask`** so it runs after Razorpay returns and avoids odd interactions with React 18 Strict Mode.

## `mountRef`

Classic **Razorpay Checkout** opens a **modal** and does not render into a React DOM node. The hook still exposes `mountRef` for:

- A hidden focus anchor / screen-reader context if you need it.
- Future **embedded** or **Elements**-style integrations that require a container.

Attach `<div ref={mountRef} className="sr-only" />` only if your UX needs it; it is optional for the standard modal.

## Non-React usage

You can call `openRazorpayCheckout` with any ref-like object:

```ts
const callbacksRef = { current: { onSuccess: () => {} } }
callbacksRef.current = { onSuccess: (r) => verify(r) }
await openRazorpayCheckout({ options: { key, order_id, amount, currency: 'INR' }, callbacksRef })
```

## Security

- Never put `key_secret` in the frontend; only **`key_id`** (public) from the API.
- Always **verify** payments on the server (`/api/payment/verify` or `/verify-subscription`).
