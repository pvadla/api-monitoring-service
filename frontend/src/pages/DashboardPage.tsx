
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Activity,
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Copy,
  ExternalLink,
  Info,
  Pause,
  Pencil,
  Play,
  Plus,
  RefreshCw,
  Trash2,
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { AddMonitorDialog } from '@/features/dashboard/AddMonitorDialog.tsx'
import { CheckSparkline } from '@/features/dashboard/CheckSparkline.tsx'
import { EditEndpointDialog } from '@/features/dashboard/EditEndpointDialog.tsx'
import { MonitorTypePieChart } from '@/features/dashboard/MonitorTypePieChart.tsx'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { Badge } from '@/components/ui/badge.tsx'
import { Button, buttonVariants } from '@/components/ui/button.tsx'
import { Card } from '@/components/ui/card.tsx'
import { Skeleton } from '@/components/ui/skeleton.tsx'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table.tsx'
import { apiFetch, apiJson, parseApiErrorBody } from '@/lib/apiClient.ts'
import { formatShortDate } from '@/lib/format.ts'
import { cn } from '@/lib/utils.ts'
import type { DashboardPayload, EndpointRow, MonitorTypeFilter } from '@/types/dashboard.ts'

// ─── query helpers ────────────────────────────────────────────────────────────

function dashboardQueryKey(subscription: string | null) {
  return ['dashboard', subscription ?? ''] as const
}

async function fetchDashboard(subscription: string | null): Promise<DashboardPayload> {
  const q = subscription ? `?subscription=${encodeURIComponent(subscription)}` : ''
  return apiJson<DashboardPayload>(`/api/dashboard${q}`)
}

// ─── small status pill ────────────────────────────────────────────────────────

function SystemStatusPill({ data }: { data: DashboardPayload | undefined }) {
  if (!data) return null
  const total = data.endpoints.length + data.heartbeats.length
  if (total === 0) return null
  const hasDown =
    data.endpoints.some((e) => !e.isUp) ||
    data.heartbeats.some((h) => h.isUp === false)
  const hasIncidents = (data.openIncidentCount ?? 0) > 0
  if (hasDown || hasIncidents) {
    return (
      <span className="inline-flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-2.5 py-1 text-xs font-medium text-red-400">
        <span className="size-1.5 animate-pulse rounded-full bg-red-400" />
        Issues detected
      </span>
    )
  }
  return (
    <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-xs font-medium text-emerald-400">
      <span className="size-1.5 rounded-full bg-emerald-400" />
      All systems operational
    </span>
  )
}

// ─── summary pills ────────────────────────────────────────────────────────────

function SummaryPills({ data }: { data: DashboardPayload }) {
  const total = data.endpoints.length + data.heartbeats.length
  const up =
    data.endpoints.filter((e) => e.isUp).length +
    data.heartbeats.filter((h) => h.isUp === true).length
  const down =
    data.endpoints.filter((e) => !e.isUp).length +
    data.heartbeats.filter((h) => h.isUp === false).length
  const incidents = data.openIncidentCount ?? 0

  return (
    <div className="flex flex-wrap items-center gap-2 text-xs">
      <span className="text-muted-foreground tabular-nums">
        {total} monitor{total !== 1 ? 's' : ''}
      </span>
      <span className="text-muted-foreground/40">·</span>
      <span className="font-medium tabular-nums text-emerald-400">{up} up</span>
      {down > 0 && (
        <>
          <span className="text-muted-foreground/40">·</span>
          <span className="font-medium tabular-nums text-red-400">{down} down</span>
        </>
      )}
      {incidents > 0 && (
        <>
          <span className="text-muted-foreground/40">·</span>
          <Link to="/incidents" className="font-medium tabular-nums text-amber-400 hover:underline">
            {incidents} open incident{incidents !== 1 ? 's' : ''}
          </Link>
        </>
      )}
    </div>
  )
}

// ─── contextual issue banner ──────────────────────────────────────────────────

function IssueBanner({ data }: { data: DashboardPayload }) {
  const downEndpoints = data.endpoints.filter((e) => !e.isUp)
  const downHeartbeats = data.heartbeats.filter((h) => h.isUp === false)
  const incidents = data.openIncidentCount ?? 0

  if (downEndpoints.length === 0 && downHeartbeats.length === 0 && incidents === 0) return null

  return (
    <div className="rounded-xl border border-red-500/20 bg-red-500/5 px-4 py-3">
      <div className="flex flex-wrap items-start gap-3">
        <AlertTriangle className="mt-0.5 size-4 shrink-0 text-red-400" />
        <div className="min-w-0 flex-1 space-y-2">
          <p className="text-sm font-medium text-red-300">
            {downEndpoints.length + downHeartbeats.length} monitor{downEndpoints.length + downHeartbeats.length !== 1 ? 's' : ''} currently down
            {incidents > 0 && ` · ${incidents} open incident${incidents !== 1 ? 's' : ''}`}
          </p>
          <div className="flex flex-wrap gap-2">
            {downEndpoints.map((ep) => (
              <Link
                key={ep.id}
                to={`/endpoints/${ep.id}`}
                className="inline-flex items-center gap-1 rounded-md border border-red-500/20 bg-red-500/10 px-2 py-0.5 text-xs text-red-300 hover:bg-red-500/20"
              >
                <span className="size-1.5 rounded-full bg-red-400" />
                {ep.name}
              </Link>
            ))}
            {downHeartbeats.map((hb) => (
              <span
                key={hb.id}
                className="inline-flex items-center gap-1 rounded-md border border-red-500/20 bg-red-500/10 px-2 py-0.5 text-xs text-red-300"
              >
                <span className="size-1.5 rounded-full bg-red-400" />
                {hb.name}
              </span>
            ))}
            {incidents > 0 && (
              <Link
                to="/incidents"
                className="inline-flex items-center gap-1 rounded-md border border-amber-500/20 bg-amber-500/10 px-2 py-0.5 text-xs text-amber-300 hover:bg-amber-500/20"
              >
                View incidents →
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

// ─── main page ────────────────────────────────────────────────────────────────

export function DashboardPage() {
  const queryClient = useQueryClient()
  const [searchParams] = useSearchParams()
  const subscriptionParam = searchParams.get('subscription')

  const subscription = useMemo(() => {
    return subscriptionParam === 'success' ? 'success' : null
  }, [subscriptionParam])

  const { data, isPending, isError, error, refetch, dataUpdatedAt, isFetching } = useQuery({
    queryKey: dashboardQueryKey(subscription),
    queryFn: () => fetchDashboard(subscription),
    refetchInterval: 60_000,
  })

  const [addOpen, setAddOpen] = useState(false)
  const [editEndpoint, setEditEndpoint] = useState<EndpointRow | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [flashDismissed, setFlashDismissed] = useState(false)
  const [monitorTypeFilter, setMonitorTypeFilter] = useState<MonitorTypeFilter>('all')

  const unifiedRows = useMemo(() => {
    if (!data) return []
    const endpoints = data.endpoints.map((ep) => ({ kind: 'endpoint' as const, ep }))
    const heartbeats = data.heartbeats.map((hb) => ({ kind: 'heartbeat' as const, hb }))
    const merged = [...endpoints, ...heartbeats].sort((a, b) => {
      const na = a.kind === 'endpoint' ? a.ep.name : a.hb.name
      const nb = b.kind === 'endpoint' ? b.ep.name : b.hb.name
      return na.localeCompare(nb, undefined, { sensitivity: 'base' })
    })
    if (monitorTypeFilter === 'all') return merged
    return merged.filter((row) => row.kind === monitorTypeFilter)
  }, [data, monitorTypeFilter])

  const monitorTypeStats = useMemo(() => {
    if (!data) return null
    const ep = data.endpoints
    const hb = data.heartbeats
    const httpCount = ep.length
    const hbCount = hb.length
    const httpUp = ep.filter((e) => e.isUp).length
    const httpDown = ep.filter((e) => e.isUp === false).length
    const hbUp = hb.filter((h) => h.isUp === true).length
    const hbDown = hb.filter((h) => h.isUp === false).length
    const hbPending = hb.filter((h) => h.isUp === null).length
    const httpSuccessPct = httpCount ? Math.round((httpUp / httpCount) * 100) : 0
    const hbSuccessPct = hbCount ? Math.round((hbUp / hbCount) * 100) : 0
    return {
      httpCount, httpUp, httpDown,
      hbCount, hbUp, hbDown, hbPending,
      httpSuccessPct, hbSuccessPct,
      openIncidentCount: data.openIncidentCount ?? 0,
    }
  }, [data])

  const toggleMutation = useMutation({
    mutationFn: (id: number) => apiFetch(`/api/endpoints/${id}/toggle`, { method: 'POST' }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
        await queryClient.invalidateQueries({ queryKey: ['me'] })
      }
    },
  })

  const deleteEndpointMutation = useMutation({
    mutationFn: (id: number) => apiFetch(`/api/endpoints/${id}`, { method: 'DELETE' }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
        await queryClient.invalidateQueries({ queryKey: ['me'] })
      } else {
        const msg = await parseApiErrorBody(r).catch(() => `HTTP ${r.status}`)
        window.alert(`Could not delete endpoint: ${msg}`)
      }
    },
  })

  const deleteHbMutation = useMutation({
    mutationFn: (id: number) => apiFetch(`/api/heartbeats/${id}`, { method: 'DELETE' }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
        await queryClient.invalidateQueries({ queryKey: ['me'] })
      }
    },
  })

  const userName = data?.user.name?.trim() || data?.user.email || 'there'
  const flash = !flashDismissed && data?.flashSuccess ? data.flashSuccess : null

  const lastUpdated = dataUpdatedAt
    ? new Date(dataUpdatedAt).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : null

  return (
    <div className="w-full space-y-6 pb-12">

      {/* ── Compact header bar ── */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-3">
          <div>
            <p className="text-foreground text-sm font-medium">
              Welcome back,{' '}
              <span className="from-primary bg-gradient-to-r to-sky-400 bg-clip-text font-semibold text-transparent">
                {userName}
              </span>
            </p>
            {data ? <SummaryPills data={data} /> : null}
          </div>
          <SystemStatusPill data={data} />
        </div>

        <div className="flex items-center gap-2">
          {lastUpdated ? (
            <span className="text-muted-foreground hidden items-center gap-1 text-xs sm:flex">
              <Clock className="size-3" />
              {isFetching ? 'Refreshing…' : `Updated ${lastUpdated}`}
            </span>
          ) : null}
          <Button
            variant="ghost"
            size="icon"
            className="size-8"
            title="Refresh now"
            disabled={isFetching}
            onClick={() => void refetch()}
          >
            <RefreshCw className={cn('size-3.5', isFetching && 'animate-spin')} />
          </Button>
          <Button size="sm" className="gap-2 shadow-sm" onClick={() => setAddOpen(true)}>
            <Plus className="size-3.5" />
            Add monitor
          </Button>
        </div>
      </div>

      {/* ── Success flash ── */}
      {flash ? (
        <Alert className="border-emerald-500/30 bg-emerald-500/5">
          <CheckCircle2 className="text-emerald-400" />
          <AlertTitle className="text-emerald-100">Success</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center justify-between gap-2 text-emerald-100/90">
            <span>{flash}</span>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="h-7 text-xs"
              onClick={() => setFlashDismissed(true)}
            >
              Dismiss
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {/* ── Load error ── */}
      {isError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Couldn't load dashboard</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{error instanceof Error ? error.message : 'Unknown error'}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {/* ── Issue banner (only when something is down) ── */}
      {data ? <IssueBanner data={data} /> : null}

      {/* ── Monitors: table left, health chart right ── */}
      <section>
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(240px,300px)] lg:items-start lg:gap-8">

          {/* left: filter + table */}
          <div className="min-w-0 space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Activity className="text-muted-foreground size-4 shrink-0" />
                <h2 className="text-base font-semibold tracking-tight">Monitors</h2>
              </div>
              <div className="flex items-center gap-2">
                <label htmlFor="monitor-type-filter" className="text-muted-foreground text-xs whitespace-nowrap">
                  Filter
                </label>
                <select
                  id="monitor-type-filter"
                  value={monitorTypeFilter}
                  onChange={(e) => setMonitorTypeFilter(e.target.value as MonitorTypeFilter)}
                  className={cn(
                    'border-input bg-background text-foreground h-8 min-w-[10rem] rounded-lg border px-2.5 py-1 text-xs shadow-sm outline-none',
                    'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-2',
                    'dark:bg-input/30',
                  )}
                >
                  <option value="all">All types</option>
                  <option value="endpoint">HTTP / Endpoint</option>
                  <option value="heartbeat">Heartbeat</option>
                </select>
              </div>
            </div>

            <Card className="border-border/80 w-full max-w-full overflow-hidden shadow-sm">
              {isPending ? (
                <div className="space-y-2 p-4">
                  <Skeleton className="h-10 w-full" />
                  <Skeleton className="h-10 w-full" />
                  <Skeleton className="h-10 w-full" />
                </div>
              ) : data ? (
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/40 hover:bg-muted/40">
                      <TableHead className="w-24">Type</TableHead>
                      <TableHead className="w-[7.5rem]">Last 15</TableHead>
                      <TableHead>Name</TableHead>
                      <TableHead>Target</TableHead>
                      <TableHead className="w-44 text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.endpoints.length === 0 && data.heartbeats.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={5} className="text-muted-foreground py-16 text-center text-sm">
                          <div className="flex flex-col items-center gap-3">
                            <Activity className="text-muted-foreground/40 size-10" />
                            <div>
                              <p className="font-medium">No monitors yet</p>
                              <p className="text-muted-foreground/70 mt-1 text-xs">
                                Add an HTTP endpoint to check uptime, or a heartbeat URL for cron jobs.
                              </p>
                            </div>
                            <Button size="sm" className="gap-2" onClick={() => setAddOpen(true)}>
                              <Plus className="size-3.5" />
                              Add your first monitor
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ) : unifiedRows.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={5} className="text-muted-foreground py-14 text-center text-sm">
                          No monitors match this filter.{' '}
                          <button
                            type="button"
                            className="text-primary font-medium underline-offset-4 hover:underline"
                            onClick={() => setMonitorTypeFilter('all')}
                          >
                            Show all
                          </button>
                        </TableCell>
                      </TableRow>
                    ) : (
                      unifiedRows.map((row) => {
                        if (row.kind === 'endpoint') {
                          const ep = row.ep
                          return (
                            <TableRow
                              key={`ep-${ep.id}`}
                              className={cn(
                                'group',
                                !ep.isUp && 'bg-red-500/[0.04]',
                              )}
                            >
                              <TableCell>
                                <div className="flex items-center gap-1.5">
                                  <span
                                    className={cn(
                                      'size-1.5 shrink-0 rounded-full',
                                      ep.isUp ? 'bg-emerald-400' : 'animate-pulse bg-red-400',
                                    )}
                                  />
                                  <Badge variant="secondary" className="font-medium">
                                    HTTP
                                  </Badge>
                                </div>
                              </TableCell>
                              <TableCell className="align-middle">
                                <CheckSparkline checks={ep.recentChecksUp} />
                              </TableCell>
                              <TableCell className="font-medium">
                                <Link to={`/endpoints/${ep.id}`} className="text-primary hover:underline">
                                  {ep.name}
                                </Link>
                                {!ep.isActive && (
                                  <span className="text-muted-foreground ml-1.5 text-xs">(paused)</span>
                                )}
                              </TableCell>
                              <TableCell className="max-w-[min(320px,45vw)]">
                                <div className="flex flex-col gap-1.5">
                                  <a
                                    href={ep.url}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="text-primary inline-flex max-w-full items-center gap-1 truncate text-sm font-medium hover:underline"
                                  >
                                    <span className="truncate">{ep.url}</span>
                                    <ExternalLink className="size-3.5 shrink-0 opacity-60" />
                                  </a>
                                  <div className="text-muted-foreground flex flex-col gap-0.5 text-xs leading-snug sm:flex-row sm:flex-wrap sm:gap-x-4 sm:gap-y-0.5">
                                    <span>every {ep.checkInterval} min</span>
                                    <span>{formatShortDate(ep.lastChecked)}</span>
                                  </div>
                                </div>
                              </TableCell>
                              <TableCell className="text-right">
                                <div className="flex flex-wrap items-center justify-end gap-0.5">
                                  <Link
                                    to={`/endpoints/${ep.id}`}
                                    title="View details"
                                    className={cn(
                                      buttonVariants({ variant: 'ghost', size: 'icon' }),
                                      'inline-flex size-8 items-center justify-center rounded-lg',
                                    )}
                                  >
                                    <Info className="size-3.5" />
                                  </Link>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    title="Edit"
                                    onClick={() => {
                                      setEditEndpoint(ep)
                                      setEditOpen(true)
                                    }}
                                  >
                                    <Pencil className="size-3.5" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="size-8"
                                    title={ep.isActive ? 'Pause' : 'Resume'}
                                    disabled={toggleMutation.isPending}
                                    onClick={() => toggleMutation.mutate(ep.id)}
                                  >
                                    {ep.isActive ? <Pause className="size-3.5" /> : <Play className="size-3.5" />}
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="text-muted-foreground hover:text-destructive size-8"
                                    title="Delete"
                                    disabled={deleteEndpointMutation.isPending}
                                    onClick={() => {
                                      if (window.confirm('Delete this endpoint? This cannot be undone.')) {
                                        deleteEndpointMutation.mutate(ep.id)
                                      }
                                    }}
                                  >
                                    <Trash2 className="size-3.5" />
                                  </Button>
                                </div>
                              </TableCell>
                            </TableRow>
                          )
                        }
                        const hb = row.hb
                        const pingUrl = `${data.baseUrl.replace(/\/$/, '')}/heartbeat/${hb.token}`
                        return (
                          <TableRow
                            key={`hb-${hb.id}`}
                            className={cn(hb.isUp === false && 'bg-red-500/[0.04]')}
                          >
                            <TableCell>
                              <div className="flex items-center gap-1.5">
                                <span
                                  className={cn(
                                    'size-1.5 shrink-0 rounded-full',
                                    hb.isUp === true && 'bg-emerald-400',
                                    hb.isUp === false && 'animate-pulse bg-red-400',
                                    hb.isUp === null && 'bg-muted-foreground/40',
                                  )}
                                />
                                <Badge
                                  variant="outline"
                                  className="border-sky-500/40 bg-sky-500/10 font-medium text-sky-800 dark:text-sky-300"
                                >
                                  Heartbeat
                                </Badge>
                              </div>
                            </TableCell>
                            <TableCell className="align-middle">
                              <CheckSparkline checks={hb.recentChecksUp} />
                            </TableCell>
                            <TableCell className="font-medium">{hb.name}</TableCell>
                            <TableCell className="max-w-[min(320px,45vw)]">
                              <div className="flex flex-col gap-1.5">
                                <code className="bg-muted text-foreground/90 inline-flex max-w-full items-center gap-2 rounded-md px-2 py-1 text-xs break-all">
                                  {pingUrl}
                                  <button
                                    type="button"
                                    className="text-muted-foreground hover:text-foreground shrink-0"
                                    title="Copy URL"
                                    onClick={() => void navigator.clipboard.writeText(pingUrl)}
                                  >
                                    <Copy className="size-3" />
                                  </button>
                                </code>
                                <div className="text-muted-foreground flex flex-col gap-0.5 text-xs leading-snug sm:flex-row sm:flex-wrap sm:gap-x-4 sm:gap-y-0.5">
                                  <span>every {hb.expectedIntervalMinutes} min</span>
                                  <span>{formatShortDate(hb.lastPingAt)}</span>
                                </div>
                              </div>
                            </TableCell>
                            <TableCell className="text-right">
                              <Button
                                variant="ghost"
                                size="icon"
                                className="text-muted-foreground hover:text-destructive size-8"
                                title="Delete"
                                disabled={deleteHbMutation.isPending}
                                onClick={() => {
                                  if (window.confirm('Delete this heartbeat monitor?')) {
                                    deleteHbMutation.mutate(hb.id)
                                  }
                                }}
                              >
                                <Trash2 className="size-3.5" />
                              </Button>
                            </TableCell>
                          </TableRow>
                        )
                      })
                    )}
                  </TableBody>
                </Table>
              ) : null}
            </Card>
          </div>

          {/* right: monitor health chart */}
          {monitorTypeStats && monitorTypeStats.httpCount + monitorTypeStats.hbCount > 0 ? (
            <div className="min-w-0 lg:sticky lg:top-24 lg:self-start">
              <MonitorTypePieChart
                httpCount={monitorTypeStats.httpCount}
                httpUp={monitorTypeStats.httpUp}
                httpDown={monitorTypeStats.httpDown}
                heartbeatCount={monitorTypeStats.hbCount}
                heartbeatUp={monitorTypeStats.hbUp}
                heartbeatDown={monitorTypeStats.hbDown}
                heartbeatPending={monitorTypeStats.hbPending}
                httpSuccessPct={monitorTypeStats.httpSuccessPct}
                heartbeatSuccessPct={monitorTypeStats.hbSuccessPct}
                openIncidentCount={monitorTypeStats.openIncidentCount}
              />
            </div>
          ) : isPending ? (
            <Skeleton className="h-80 rounded-xl" />
          ) : null}

        </div>
      </section>

      <AddMonitorDialog open={addOpen} onOpenChange={setAddOpen} />
      <EditEndpointDialog
        endpoint={editEndpoint}
        open={editOpen}
        onOpenChange={(v) => {
          setEditOpen(v)
          if (!v) setEditEndpoint(null)
        }}
      />
    </div>
  )
}
