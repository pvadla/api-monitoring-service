/**
 * Minimal typings for Razorpay Checkout.js (classic modal).
 * @see https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/integration-steps
 */

export type RazorpaySuccessResponse = {
  razorpay_payment_id: string
  razorpay_order_id?: string
  razorpay_subscription_id?: string
  razorpay_signature?: string
}

export type RazorpayOptions = {
  key: string
  amount?: number
  currency?: string
  name?: string
  description?: string
  order_id?: string
  subscription_id?: string
  /** Called when payment succeeds (runs in Razorpay's context — keep logic in ref-backed callbacks). */
  handler?: (response: RazorpaySuccessResponse) => void
  prefill?: { name?: string; email?: string; contact?: string }
  notes?: Record<string, string>
  theme?: { color?: string }
  modal?: {
    ondismiss?: () => void
    /** If true, customer can close modal */
    escape?: boolean
  }
}

export type RazorpayInstance = {
  open: () => void
  on: (event: 'payment.failed', handler: (data: unknown) => void) => void
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => RazorpayInstance
  }
}
