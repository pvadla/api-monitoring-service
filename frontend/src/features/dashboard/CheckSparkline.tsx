import { cn } from '@/lib/utils.ts'

const BAR_COUNT = 15

function normalize(checks: (boolean | null)[] | null | undefined): (boolean | null)[] {
  if (checks != null && checks.length === BAR_COUNT) {
    return checks
  }
  const src = checks ?? []
  const out: (boolean | null)[] = []
  for (let i = 0; i < BAR_COUNT; i++) {
    out.push(src[i] ?? null)
  }
  return out
}

/**
 * Fifteen vertical bars: oldest check left, newest right. Green = up, red = down, short gray = no check yet.
 */
export function CheckSparkline({ checks }: { checks: (boolean | null)[] | null | undefined }) {
  const series = normalize(checks)
  return (
    <div
      className="flex h-8 w-full max-w-[6.5rem] shrink-0 items-end justify-between gap-px"
      role="img"
      aria-label="Last 15 checks, oldest on the left. Green is success, red is failure."
    >
      {series.map((ok, i) => (
        <div
          key={i}
          title={ok === null ? 'No check' : ok ? 'Up' : 'Down'}
          className={cn(
            'w-full min-w-[3px] flex-1 rounded-[1px] self-end',
            ok === null && 'bg-muted h-2',
            ok === true && 'h-6 bg-emerald-500',
            ok === false && 'h-6 bg-red-500',
          )}
        />
      ))}
    </div>
  )
}
