import { useCallback, useRef } from 'react'
import type { MutableRefObject, RefObject } from 'react'

import { openRazorpayCheckout } from '@/lib/razorpay/checkout.ts'
import type { RazorpayCheckoutCallbacks } from '@/lib/razorpay/checkout.ts'
import { loadRazorpayScript } from '@/lib/razorpay/script.ts'
import type { RazorpayInstance, RazorpayOptions, RazorpaySuccessResponse } from '@/lib/razorpay/types.ts'

export type UseRazorpayCheckoutResult = {
  /**
   * Optional DOM ref (e.g. hidden anchor). Classic Razorpay Checkout does **not** mount into this node;
   * attach for accessibility or future embedded/card flows.
   */
  mountRef: RefObject<HTMLDivElement | null>
  /** Same ref object you pass to {@link openRazorpayCheckout} — update `.current` before opening. */
  callbacksRef: MutableRefObject<RazorpayCheckoutCallbacks>
  /** Last Razorpay instance from `open` (for debugging; modal manages its own lifecycle). */
  lastInstanceRef: MutableRefObject<RazorpayInstance | null>
  /** Preloads script (optional UX before user clicks Pay). */
  preload: () => Promise<void>
  /**
   * Opens checkout. Pass options **without** `handler` / `modal.ondismiss` — those come from `callbacksRef`.
   * Set `callbacksRef.current = { onSuccess, ... }` immediately before calling `open`.
   */
  open: (
    options: Omit<RazorpayOptions, 'handler'> & {
      modal?: Omit<NonNullable<RazorpayOptions['modal']>, 'ondismiss'>
    },
  ) => Promise<RazorpayInstance>
}

/**
 * React hook that keeps Razorpay success/failure/dismiss handlers on a **ref** so callbacks always run
 * **outside** a stale React closure and are scheduled with `queueMicrotask` after Razorpay invokes them.
 *
 * Pattern:
 * ```tsx
 * const { callbacksRef, open, mountRef } = useRazorpayCheckout()
 * callbacksRef.current = {
 *   onSuccess: (r) => { void verifyOnServer(r) },
 *   onPaymentFailed: () => toast.error('Payment failed'),
 *   onDismiss: () => setBusy(false),
 * }
 * await open({ key: data.keyId, order_id: data.orderId, amount: data.amount, currency: 'INR', name: 'APIWatch' })
 * ```
 */
export function useRazorpayCheckout(): UseRazorpayCheckoutResult {
  const mountRef = useRef<HTMLDivElement | null>(null)
  const callbacksRef = useRef<RazorpayCheckoutCallbacks>({
    onSuccess: () => {},
  })
  const lastInstanceRef = useRef<RazorpayInstance | null>(null)

  const preload = useCallback(() => loadRazorpayScript(), [])

  const open = useCallback(
    async (
      options: Omit<RazorpayOptions, 'handler'> & {
        modal?: Omit<NonNullable<RazorpayOptions['modal']>, 'ondismiss'>
      },
    ) => {
      const instance = await openRazorpayCheckout({ options, callbacksRef })
      lastInstanceRef.current = instance
      return instance
    },
    [],
  )

  return {
    mountRef,
    callbacksRef,
    lastInstanceRef,
    preload,
    open,
  }
}

export type { RazorpaySuccessResponse }
