/**
 * POST to legacy Spring MVC form endpoints (Thymeleaf-era) with CSRF disabled.
 * Expects 302 redirect to dashboard on success.
 *
 * Note: With {@code redirect: 'manual'}, some browsers return {@code status: 0} (opaque redirect),
 * which surfaces as "HTTP 0". Prefer {@code POST /api/...} JSON from {@code apiFetch} for new UI.
 */
export async function postMvcForm(
  action: string,
  fields: Record<string, string | number | boolean | null | undefined>,
): Promise<{ ok: boolean; error?: string }> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(fields)) {
    if (value === undefined || value === null) continue
    params.set(key, String(value))
  }

  const res = await fetch(action, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      Accept: 'text/html,application/json',
    },
    body: params,
    redirect: 'manual',
  })

  if (res.status === 302 || res.status === 303 || res.status === 301) {
    return { ok: true }
  }
  if (res.ok) {
    return { ok: true }
  }

  const text = await res.text().catch(() => '')
  return { ok: false, error: text ? text.slice(0, 240) : `HTTP ${res.status}` }
}
