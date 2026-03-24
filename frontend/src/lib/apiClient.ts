/**
 * Central fetch wrapper: sends cookies (session) and normalizes 401 for the SPA.
 * - {@code GET /api/me} does not trigger session-expired handling (anonymous is expected).
 * - Other {@code /api/**} 401 responses dispatch {@code api:unauthorized} so auth state can clear.
 */
export class UnauthorizedError extends Error {
  constructor() {
    super('Unauthorized')
    this.name = 'UnauthorizedError'
  }
}

export async function apiFetch(input: string | URL, init?: RequestInit): Promise<Response> {
  const url = typeof input === 'string' ? input : input.toString()
  const res = await fetch(input, {
    ...init,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
  })

  if (res.status === 401) {
    const isMeEndpoint = url.includes('/api/me')
    if (!isMeEndpoint) {
      window.dispatchEvent(new CustomEvent('api:unauthorized'))
    }
    throw new UnauthorizedError()
  }

  return res
}

export async function apiJson<T>(input: string | URL, init?: RequestInit): Promise<T> {
  const res = await apiFetch(input, init)
  return res.json() as Promise<T>
}

/** Reads `{ "error": "..." }` from failed API responses (Spring `ApiMessageResponse`). */
export async function parseApiErrorBody(res: Response): Promise<string> {
  const data = (await res.json().catch(() => null)) as { error?: string } | null
  if (data?.error) return data.error
  return `HTTP ${res.status}`
}
