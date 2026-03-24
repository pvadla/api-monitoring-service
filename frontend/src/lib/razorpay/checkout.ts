import { loadRazorpayScript } from '@/lib/razorpay/script.ts'
import type { RazorpayInstance, RazorpayOptions, RazorpaySuccessResponse } from '@/lib/razorpay/types.ts'

export type RazorpayCheckoutCallbacks = {
  onSuccess: (response: RazorpaySuccessResponse) => void
  onPaymentFailed?: (error: unknown) => void
  onDismiss?: () => void
}

/**
 * Schedules work after Razorpay’s synchronous handler returns — avoids React 18 Strict Mode
 * double-invocation issues and keeps side effects outside the Razorpay call stack.
 */
function schedule(callback: () => void): void {
  queueMicrotask(callback)
}

export type OpenCheckoutParams = {
  /** Checkout options **without** `handler` / `modal.ondismiss` — those are wired from `callbacksRef`. */
  options: Omit<RazorpayOptions, 'handler'> & {
    modal?: Omit<NonNullable<RazorpayOptions['modal']>, 'ondismiss'>
  }
  /**
   * Ref whose `.current` is read when Razorpay fires events (always latest handlers, no stale closures).
   * Update with `callbacksRef.current = { ... }` before each `open`.
   */
  callbacksRef: { current: RazorpayCheckoutCallbacks }
}

/**
 * Loads script if needed, builds `new Razorpay({...})`, wires handlers via ref + `queueMicrotask`,
 * and calls `open()`.
 *
 * Classic Checkout opens a **modal overlay**; it does not render into a React tree. `mountRef` in the
 * hook is reserved for optional layout/a11y or future embedded flows — see docs.
 */
export async function openRazorpayCheckout(params: OpenCheckoutParams): Promise<RazorpayInstance> {
  await loadRazorpayScript()
  const Razorpay = window.Razorpay
  if (!Razorpay) {
    throw new Error('Razorpay is not available on window')
  }

  const { options, callbacksRef } = params

  const checkoutOptions: RazorpayOptions = {
    ...options,
    handler: (response: RazorpaySuccessResponse) => {
      schedule(() => callbacksRef.current.onSuccess(response))
    },
    modal: {
      ...options.modal,
      ondismiss: () => {
        schedule(() => callbacksRef.current.onDismiss?.())
      },
    },
  }

  const rzp = new Razorpay(checkoutOptions)

  rzp.on('payment.failed', (err: unknown) => {
    schedule(() => callbacksRef.current.onPaymentFailed?.(err))
  })

  rzp.open()
  return rzp
}
