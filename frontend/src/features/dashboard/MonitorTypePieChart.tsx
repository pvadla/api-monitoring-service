import { useMemo, useState } from 'react'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { cn } from '@/lib/utils.ts'

type SegmentId = 'http' | 'heartbeat'

function sectorPath(cx: number, cy: number, r: number, startAngle: number, endAngle: number): string {
  const rad = (deg: number) => ((deg - 90) * Math.PI) / 180
  const x1 = cx + r * Math.cos(rad(startAngle))
  const y1 = cy + r * Math.sin(rad(startAngle))
  const x2 = cx + r * Math.cos(rad(endAngle))
  const y2 = cy + r * Math.sin(rad(endAngle))
  const large = endAngle - startAngle > 180 ? 1 : 0
  return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${large} 1 ${x2} ${y2} Z`
}

export type MonitorTypePieChartProps = {
  httpCount: number
  heartbeatCount: number
  /** % of HTTP monitors currently up (0–100). */
  httpSuccessPct: number
  /** % of heartbeats that have received at least one ping (0–100). */
  heartbeatSuccessPct: number
}

/**
 * Pie by monitor type; hover a slice to see that type’s success %.
 */
export function MonitorTypePieChart({
  httpCount,
  heartbeatCount,
  httpSuccessPct,
  heartbeatSuccessPct,
}: MonitorTypePieChartProps) {
  const [hover, setHover] = useState<SegmentId | null>(null)
  const total = httpCount + heartbeatCount

  const segments = useMemo(() => {
    if (total === 0) return []
    const httpShare = httpCount / total
    const httpDeg = httpShare * 360
    const segs: { id: SegmentId; start: number; end: number; label: string; successPct: number; color: string }[] = []
    if (httpCount > 0) {
      segs.push({
        id: 'http',
        start: 0,
        end: httpDeg,
        label: 'HTTP / Endpoint',
        successPct: httpSuccessPct,
        color: 'var(--chart-http, oklch(0.65 0.15 250))',
      })
    }
    if (heartbeatCount > 0) {
      segs.push({
        id: 'heartbeat',
        start: httpDeg,
        end: 360,
        label: 'Heartbeat',
        successPct: heartbeatSuccessPct,
        color: 'var(--chart-hb, oklch(0.7 0.12 220))',
      })
    }
    return segs
  }, [total, httpCount, heartbeatCount, httpSuccessPct, heartbeatSuccessPct])

  const active = hover
    ? segments.find((s) => s.id === hover)
    : null

  return (
    <Card className="border-border/80 h-full shadow-sm">
      <CardHeader className="pb-2">
        <CardTitle className="text-base">Monitors by type</CardTitle>
        <CardDescription>
          HTTP success = currently up. Heartbeat success = received at least one ping. Hover a slice for details.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col items-center gap-4 pt-0">
        {total === 0 ? (
          <p className="text-muted-foreground py-8 text-center text-sm">Add monitors to see the breakdown.</p>
        ) : (
          <>
            <div className="relative w-full max-w-[240px]">
              <svg viewBox="0 0 100 100" className="aspect-square w-full overflow-visible" aria-hidden>
                <title>Monitor types pie chart</title>
                {segments.length === 1 ? (
                  <circle
                    cx="50"
                    cy="50"
                    r="38"
                    fill={segments[0].color}
                    className={cn(
                      'transition-[opacity,filter] duration-150',
                      hover && hover !== segments[0].id && 'opacity-35',
                      hover === segments[0].id && 'brightness-110',
                    )}
                    onMouseEnter={() => setHover(segments[0].id)}
                    onMouseLeave={() => setHover(null)}
                  />
                ) : (
                  segments.map((s) => (
                    <path
                      key={s.id}
                      d={sectorPath(50, 50, 38, s.start, s.end)}
                      fill={s.color}
                      className={cn(
                        'cursor-pointer transition-[opacity,filter] duration-150',
                        hover && hover !== s.id && 'opacity-35',
                        hover === s.id && 'brightness-110',
                      )}
                      stroke="oklch(0.2 0.02 260 / 0.5)"
                      strokeWidth={0.4}
                      onMouseEnter={() => setHover(s.id)}
                      onMouseLeave={() => setHover(null)}
                    />
                  ))
                )}
              </svg>
            </div>
            <div
              className="bg-muted/50 border-border/80 min-h-[4.5rem] w-full rounded-lg border px-3 py-2.5 text-center text-sm"
              role="status"
              aria-live="polite"
            >
              {active ? (
                <>
                  <p className="text-foreground font-medium">{active.label}</p>
                  <p className="text-muted-foreground mt-1 text-xs">
                    Success{' '}
                    <span className="text-foreground font-semibold tabular-nums">{active.successPct}%</span>
                    <span className="text-muted-foreground">
                      {' '}
                      ·{' '}
                      {active.id === 'http' ? httpCount : heartbeatCount} monitor
                      {(active.id === 'http' ? httpCount : heartbeatCount) !== 1 ? 's' : ''}
                    </span>
                  </p>
                  <p className="text-muted-foreground/90 mt-1.5 text-[11px] leading-snug">
                    {active.id === 'http'
                      ? 'Share of HTTP monitors reporting UP on the last check.'
                      : 'Share of heartbeat monitors that have received at least one ping.'}
                  </p>
                </>
              ) : (
                <p className="text-muted-foreground text-xs">Hover the chart to see success % by type.</p>
              )}
            </div>
            <ul className="text-muted-foreground flex w-full flex-wrap justify-center gap-x-4 gap-y-1 text-xs">
              {httpCount > 0 ? (
                <li className="flex items-center gap-1.5">
                  <span className="bg-[oklch(0.65_0.15_250)] size-2.5 shrink-0 rounded-sm" />
                  HTTP ({httpCount})
                </li>
              ) : null}
              {heartbeatCount > 0 ? (
                <li className="flex items-center gap-1.5">
                  <span className="bg-[oklch(0.7_0.12_220)] size-2.5 shrink-0 rounded-sm" />
                  Heartbeat ({heartbeatCount})
                </li>
              ) : null}
            </ul>
          </>
        )}
      </CardContent>
    </Card>
  )
}
