const SCRIPT_URL = 'https://checkout.razorpay.com/v1/checkout.js'
const SCRIPT_ID = 'razorpay-checkout-js'

let loadPromise: Promise<void> | null = null

function scriptEl(): HTMLScriptElement | null {
  return document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
}

export function loadRazorpayScript(): Promise<void> {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('Razorpay can only load in the browser'))
  }
  if (window.Razorpay) {
    return Promise.resolve()
  }
  if (loadPromise) {
    return loadPromise
  }
  loadPromise = new Promise((resolve, reject) => {
    const finishOk = () => {
      if (window.Razorpay) resolve()
      else reject(new Error('Razorpay global missing after script load'))
    }

    const existing = scriptEl()
    if (existing) {
      if (window.Razorpay) {
        resolve()
        return
      }
      existing.addEventListener('load', () => finishOk())
      existing.addEventListener('error', () => reject(new Error('Razorpay script failed')))
      return
    }

    const s = document.createElement('script')
    s.id = SCRIPT_ID
    s.src = SCRIPT_URL
    s.async = true
    s.onload = () => finishOk()
    s.onerror = () => reject(new Error('Failed to load Razorpay script'))
    document.head.appendChild(s)
  })
  return loadPromise
}

export function __resetRazorpayScriptCacheForTests(): void {
  loadPromise = null
}
