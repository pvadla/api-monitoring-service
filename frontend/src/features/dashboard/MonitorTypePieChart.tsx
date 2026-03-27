import { useMemo, useState } from 'react'

import { AlertTriangle, CheckCircle2, Globe, Heart, Lock, XCircle } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { cn } from '@/lib/utils.ts'

// ─── Types ────────────────────────────────────────────────────────────────────

type SegmentId = 'http' | 'heartbeat' | 'ssl'

type SegmentDef = {
  id: SegmentId
  label: string
  count: number
  up: number
  down: number
  pending: number
  /** null when all monitors in this group have never been checked yet */
  successPct: number | null
  color: string
  dimColor: string
}

export type MonitorTypePieChartProps = {
  httpCount: number
  httpUp: number
  httpDown: number
  heartbeatCount: number
  heartbeatUp: number
  heartbeatDown: number
  heartbeatPending: number
  sslCount: number
  sslUp: number
  sslDown: number
  sslPending: number
  httpSuccessPct: number
  /** null = all monitors in this group are pending (never checked yet) */
  heartbeatSuccessPct: number | null
  /** null = all monitors in this group are pending (never checked yet) */
  sslSuccessPct: number | null
  openIncidentCount: number
}

// ─── Geometry helpers ─────────────────────────────────────────────────────────

const rad = (deg: number) => ((deg - 90) * Math.PI) / 180

function donutSectorPath(
  cx: number, cy: number,
  outerR: number, innerR: number,
  startDeg: number, endDeg: number,
): string {
  // clamp a full circle to avoid degenerate arcs
  const delta = Math.min(endDeg - startDeg, 359.99)
  const end = startDeg + delta
  const large = delta > 180 ? 1 : 0
  const ox1 = cx + outerR * Math.cos(rad(startDeg))
  const oy1 = cy + outerR * Math.sin(rad(startDeg))
  const ox2 = cx + outerR * Math.cos(rad(end))
  const oy2 = cy + outerR * Math.sin(rad(end))
  const ix1 = cx + innerR * Math.cos(rad(end))
  const iy1 = cy + innerR * Math.sin(rad(end))
  const ix2 = cx + innerR * Math.cos(rad(startDeg))
  const iy2 = cy + innerR * Math.sin(rad(startDeg))
  return [
    `M ${ox1} ${oy1}`,
    `A ${outerR} ${outerR} 0 ${large} 1 ${ox2} ${oy2}`,
    `L ${ix1} ${iy1}`,
    `A ${innerR} ${innerR} 0 ${large} 0 ${ix2} ${iy2}`,
    'Z',
  ].join(' ')
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function ProgressBar({ pct, color }: { pct: number; color: string }) {
  return (
    <div className="bg-muted h-1.5 w-full overflow-hidden rounded-full">
      <div
        className="h-full rounded-full transition-all duration-500"
        style={{ width: `${pct}%`, background: color }}
      />
    </div>
  )
}

function StatPill({
  icon,
  label,
  value,
  variant,
}: {
  icon: React.ReactNode
  label: string
  value: number
  variant: 'green' | 'red' | 'muted'
}) {
  const styles = {
    green: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    red: 'bg-red-500/10 text-red-400 border-red-500/20',
    muted: 'bg-muted/60 text-muted-foreground border-border/60',
  }
  return (
    <div
      className={cn(
        'flex items-center gap-1.5 rounded-md border px-2.5 py-1.5 text-xs font-medium',
        styles[variant],
      )}
    >
      <span className="size-3.5 shrink-0">{icon}</span>
      <span className="tabular-nums">{value}</span>
      <span className="opacity-70">{label}</span>
    </div>
  )
}

// ─── Main component ───────────────────────────────────────────────────────────

export function MonitorTypePieChart({
  httpCount,
  httpUp,
  httpDown,
  heartbeatCount,
  heartbeatUp,
  heartbeatDown,
  heartbeatPending,
  sslCount,
  sslUp,
  sslDown,
  sslPending,
  httpSuccessPct,
  heartbeatSuccessPct,
  sslSuccessPct,
  openIncidentCount,
}: MonitorTypePieChartProps) {
  const [hover, setHover] = useState<SegmentId | null>(null)
  const total = httpCount + heartbeatCount + sslCount

  const HTTP_COLOR = '#3b82f6'
  const HB_COLOR = '#a855f7'
  const SSL_COLOR = '#f59e0b'

  const segments = useMemo<SegmentDef[]>(() => {
    if (total === 0) return []
    const segs: SegmentDef[] = []
    if (httpCount > 0) {
      segs.push({
        id: 'http',
        label: 'HTTP / Endpoint',
        count: httpCount,
        up: httpUp,
        down: httpDown,
        pending: 0,
        successPct: httpSuccessPct,
        color: HTTP_COLOR,
        dimColor: '#1d4ed8',
      })
    }
    if (heartbeatCount > 0) {
      segs.push({
        id: 'heartbeat',
        label: 'Heartbeat',
        count: heartbeatCount,
        up: heartbeatUp,
        down: heartbeatDown,
        pending: heartbeatPending,
        successPct: heartbeatSuccessPct,
        color: HB_COLOR,
        dimColor: '#7e22ce',
      })
    }
    if (sslCount > 0) {
      segs.push({
        id: 'ssl',
        label: 'SSL Certificate',
        count: sslCount,
        up: sslUp,
        down: sslDown,
        pending: sslPending,
        successPct: sslSuccessPct,
        color: SSL_COLOR,
        dimColor: '#b45309',
      })
    }
    return segs
  }, [
    total, httpCount, httpUp, httpDown,
    heartbeatCount, heartbeatUp, heartbeatDown, heartbeatPending,
    sslCount, sslUp, sslDown, sslPending,
    httpSuccessPct, heartbeatSuccessPct, sslSuccessPct,
  ])

  const activeSegment = hover ? segments.find((s) => s.id === hover) ?? null : null
  const totalUp = httpUp + heartbeatUp + sslUp
  const totalDown = httpDown + heartbeatDown + sslDown

  // Arc angles per segment
  const angles = useMemo(() => {
    const map: Record<SegmentId, { start: number; end: number }> = {
      http: { start: 0, end: 0 },
      heartbeat: { start: 0, end: 0 },
      ssl: { start: 0, end: 0 },
    }
    let cursor = 0
    for (const s of segments) {
      const deg = (s.count / total) * 360
      map[s.id] = { start: cursor, end: cursor + deg }
      cursor += deg
    }
    return map
  }, [segments, total])

  return (
    <Card className="border-border/80 overflow-hidden shadow-sm">
      <CardHeader className="pb-1 pt-4">
        <CardTitle className="text-sm font-semibold tracking-tight">Monitor Health</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 px-4 pb-4 pt-2">

        {/* ── Donut chart ── */}
        <div className="relative mx-auto w-full max-w-[200px]">
          <svg viewBox="0 0 100 100" className="aspect-square w-full" aria-label="Monitor type distribution">
            {segments.length === 1 ? (
              // Single type — full donut ring
              <>
                <circle cx="50" cy="50" r="36" fill="none" stroke={segments[0].color} strokeWidth="14"
                  className={cn('cursor-pointer transition-opacity duration-150', hover && hover !== segments[0].id && 'opacity-30')}
                  onMouseEnter={() => setHover(segments[0].id)}
                  onMouseLeave={() => setHover(null)}
                />
              </>
            ) : (
              segments.map((s) => {
                const { start, end } = angles[s.id]
                return (
                  <path
                    key={s.id}
                    d={donutSectorPath(50, 50, 43, 29, start, end)}
                    fill={hover && hover !== s.id ? s.dimColor : s.color}
                    className={cn(
                      'cursor-pointer transition-all duration-150',
                      hover && hover !== s.id && 'opacity-25',
                      hover === s.id && 'drop-shadow-[0_0_6px_rgba(255,255,255,0.25)]',
                    )}
                    stroke="oklch(0.15 0.02 260)"
                    strokeWidth={0.8}
                    onMouseEnter={() => setHover(s.id)}
                    onMouseLeave={() => setHover(null)}
                  />
                )
              })
            )}
            {/* Center: total count */}
            <text x="50" y="46" textAnchor="middle" className="fill-foreground" fontSize="12" fontWeight="700">
              {total}
            </text>
            <text x="50" y="57" textAnchor="middle" className="fill-muted-foreground" fontSize="6.5">
              monitors
            </text>
          </svg>
        </div>

        {/* ── Hover detail panel ── */}
        <div
          className={cn(
            'rounded-lg border px-3 py-3 text-sm transition-all duration-200',
            activeSegment
              ? 'border-border/80 bg-muted/40'
              : 'border-border/40 bg-muted/20',
          )}
          role="status"
          aria-live="polite"
        >
          {activeSegment ? (
            <div className="space-y-3">
              {/* Header row */}
              <div className="flex items-center gap-2">
                <span
                  className="size-2.5 shrink-0 rounded-full"
                  style={{ background: activeSegment.color }}
                />
                <span className="font-semibold">{activeSegment.label}</span>
                <span className="text-muted-foreground ml-auto tabular-nums text-xs">
                  {activeSegment.count} monitor{activeSegment.count !== 1 ? 's' : ''}
                </span>
              </div>

              {/* Up / Down pills */}
              <div className="flex flex-wrap gap-1.5">
                <StatPill
                  icon={<CheckCircle2 className="size-3.5" />}
                  label="up"
                  value={activeSegment.up}
                  variant="green"
                />
                <StatPill
                  icon={<XCircle className="size-3.5" />}
                  label="down"
                  value={activeSegment.down}
                  variant="red"
                />
                {activeSegment.pending > 0 && (
                  <StatPill
                    icon={<span className="block size-2.5 rounded-full bg-current opacity-40" />}
                    label="pending"
                    value={activeSegment.pending}
                    variant="muted"
                  />
                )}
              </div>

              {/* Success / failure bars — only when we have real data */}
              {activeSegment.successPct !== null ? (
                <>
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="text-muted-foreground">Success</span>
                      <span className="font-semibold tabular-nums text-emerald-400">{activeSegment.successPct}%</span>
                    </div>
                    <ProgressBar pct={activeSegment.successPct} color="#10b981" />
                  </div>
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="text-muted-foreground">Failure</span>
                      <span className="font-semibold tabular-nums text-red-400">{100 - activeSegment.successPct}%</span>
                    </div>
                    <ProgressBar pct={100 - activeSegment.successPct} color="#ef4444" />
                  </div>
                </>
              ) : (
                <p className="text-muted-foreground text-xs">No checks run yet — first check pending.</p>
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center gap-1 py-1 text-center">
              <p className="text-muted-foreground text-xs">Hover a slice for details</p>
            </div>
          )}
        </div>

        {/* ── Legend / type breakdown ── */}
        <div className="space-y-1.5">
          {segments.map((s) => (
            <button
              key={s.id}
              type="button"
              className={cn(
                'flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-xs transition-colors',
                'hover:bg-muted/50',
                hover === s.id && 'bg-muted/40',
              )}
              onMouseEnter={() => setHover(s.id)}
              onMouseLeave={() => setHover(null)}
            >
              <span className="size-2.5 shrink-0 rounded-full" style={{ background: s.color }} />
              <span className="flex items-center gap-1 font-medium">
                {s.id === 'http' ? <Globe className="size-3 opacity-60" /> :
                 s.id === 'heartbeat' ? <Heart className="size-3 opacity-60" /> :
                 <Lock className="size-3 opacity-60" />}
                {s.label}
              </span>
              <span className="ml-auto tabular-nums text-muted-foreground">{s.count}</span>
              <span
                className={cn(
                  'w-12 text-right tabular-nums font-semibold text-xs',
                  s.successPct === null
                    ? 'text-muted-foreground'
                    : s.successPct >= 80
                    ? 'text-emerald-400'
                    : s.successPct >= 50
                    ? 'text-amber-400'
                    : 'text-red-400',
                )}
              >
                {s.successPct === null ? 'pending' : `${s.successPct}%`}
              </span>
            </button>
          ))}
        </div>

        {/* ── Divider ── */}
        <div className="border-t border-border/40" />

        {/* ── Overall status row ── */}
        <div className="grid grid-cols-2 gap-2 text-xs">
          <div className="rounded-md bg-emerald-500/10 px-3 py-2 text-center">
            <p className="text-emerald-400 font-bold tabular-nums text-base">{totalUp}</p>
            <p className="text-emerald-400/70 mt-0.5">Up</p>
          </div>
          <div className="rounded-md bg-red-500/10 px-3 py-2 text-center">
            <p className="text-red-400 font-bold tabular-nums text-base">{totalDown}</p>
            <p className="text-red-400/70 mt-0.5">Down</p>
          </div>
        </div>

        {/* ── Open incidents badge ── */}
        {openIncidentCount > 0 ? (
          <div className="flex items-center gap-2 rounded-md border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-xs">
            <AlertTriangle className="size-3.5 shrink-0 text-amber-400" />
            <span className="text-amber-300 font-medium">
              {openIncidentCount} open incident{openIncidentCount !== 1 ? 's' : ''}
            </span>
            <a
              href="/incidents"
              className="ml-auto text-amber-400 underline-offset-2 hover:underline"
            >
              View
            </a>
          </div>
        ) : (
          <div className="flex items-center gap-2 rounded-md border border-emerald-500/20 bg-emerald-500/5 px-3 py-2 text-xs">
            <CheckCircle2 className="size-3.5 shrink-0 text-emerald-400" />
            <span className="text-emerald-400/80">No open incidents</span>
          </div>
        )}

      </CardContent>
    </Card>
  )
}
