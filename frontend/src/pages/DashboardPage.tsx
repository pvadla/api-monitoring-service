
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Activity,
  AlertCircle,
  CheckCircle2,
  Copy,
  ExternalLink,
  Info,
  Pause,
  Pencil,
  Play,
  Plus,
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
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card.tsx'
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


function dashboardQueryKey(subscription: string | null) {
  return ['dashboard', subscription ?? ''] as const
}

async function fetchDashboard(subscription: string | null): Promise<DashboardPayload> {
  const q = subscription ? `?subscription=${encodeURIComponent(subscription)}` : ''
  return apiJson<DashboardPayload>(`/api/dashboard${q}`)
}

export function DashboardPage() {
  const queryClient = useQueryClient()
  const [searchParams] = useSearchParams()
  const subscriptionParam = searchParams.get('subscription')

  const subscription = useMemo(() => {
    return subscriptionParam === 'success' ? 'success' : null
  }, [subscriptionParam])

  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: dashboardQueryKey(subscription),
    queryFn: () => fetchDashboard(subscription),
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

  return (
    <div className="w-full space-y-10 pb-12">
      {/* Hero */}
      <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-2">
          <p className="text-muted-foreground text-sm font-medium tracking-wide uppercase">Overview</p>
          <h1 className="text-3xl font-semibold tracking-tight md:text-4xl">
            Welcome back,{' '}
            <span className="from-primary bg-gradient-to-r to-sky-400 bg-clip-text text-transparent">
              {userName}
            </span>{' '}
            <span aria-hidden>👋</span>
          </h1>
          <p className="text-muted-foreground w-full text-sm leading-relaxed">
            Here’s your API monitoring snapshot—uptime, checks, and heartbeat pings in one place.
          </p>
        </div>
        <Button size="lg" className="shrink-0 gap-2 shadow-sm" onClick={() => setAddOpen(true)}>
          <Plus className="size-4" />
          Add monitor
        </Button>
      </div>

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

      {isError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Couldn’t load dashboard</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{error instanceof Error ? error.message : 'Unknown error'}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        {isPending ? (
          <>
            <Skeleton className="h-32 rounded-xl" />
            <Skeleton className="h-32 rounded-xl" />
            <Skeleton className="h-32 rounded-xl" />
          </>
        ) : data ? (
          <>
            <Card className="border-border/80 shadow-sm transition-shadow hover:shadow-md">
              <CardHeader className="pb-2">
                <CardDescription>Total endpoints</CardDescription>
                <CardTitle className="text-4xl font-bold tabular-nums">{data.endpointCount}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-xs">Monitored HTTP checks</p>
              </CardContent>
            </Card>
            <Card className="border-border/80 shadow-sm transition-shadow hover:shadow-md">
              <CardHeader className="pb-2">
                <CardDescription>Currently up</CardDescription>
                <CardTitle className="text-4xl font-bold tabular-nums text-emerald-600">{data.upCount}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-xs">Healthy endpoints</p>
              </CardContent>
            </Card>
            <Card className="border-border/80 shadow-sm transition-shadow hover:shadow-md">
              <CardHeader className="pb-2">
                <CardDescription>Currently down</CardDescription>
                <CardTitle className="text-4xl font-bold tabular-nums text-red-600">{data.downCount}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-xs">Need attention</p>
              </CardContent>
            </Card>
          </>
        ) : null}
      </div>

      {/* Monitors: table left, pie chart right */}
      <section className="space-y-4">
        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(240px,300px)] lg:items-start lg:gap-10">
          <div className="min-w-0 space-y-4">
            <div className="flex items-start gap-2">
              <Activity className="text-muted-foreground mt-0.5 size-5 shrink-0" />
              <div>
                <h2 className="text-xl font-semibold tracking-tight">Monitors</h2>
                <p className="text-muted-foreground text-sm">
                  HTTP uptime checks and heartbeat ping URLs in one list.
                </p>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <label htmlFor="monitor-type-filter" className="text-muted-foreground text-sm whitespace-nowrap">
                Filter by type
              </label>
              <select
                id="monitor-type-filter"
                value={monitorTypeFilter}
                onChange={(e) => setMonitorTypeFilter(e.target.value as MonitorTypeFilter)}
                className={cn(
                  'border-input bg-background text-foreground h-9 min-w-[11rem] rounded-lg border px-3 py-1 text-sm shadow-sm outline-none',
                  'focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-3',
                  'dark:bg-input/30',
                )}
              >
                <option value="all">All types</option>
                <option value="endpoint">HTTP / Endpoint</option>
                <option value="heartbeat">Heartbeat</option>
              </select>
            </div>
            <Card className="border-border/80 w-full max-w-full overflow-hidden shadow-sm">
            {isPending ? (
              <div className="p-6">
                <Skeleton className="h-40 w-full" />
              </div>
            ) : data ? (
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/40 hover:bg-muted/40">
                    <TableHead className="w-28">Type</TableHead>
                    <TableHead className="w-[7.5rem]">Status</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Target</TableHead>
                    <TableHead className="w-44 text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.endpoints.length === 0 && data.heartbeats.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} className="text-muted-foreground py-14 text-center text-sm">
                        No monitors yet. Use <strong>Add monitor</strong> for HTTP uptime or Cron / Heartbeat.
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
                          Show all types
                        </button>
                      </TableCell>
                    </TableRow>
                ) : (
                  unifiedRows.map((row) => {
                    if (row.kind === 'endpoint') {
                      const ep = row.ep
                      return (
                        <TableRow key={`ep-${ep.id}`} className="group">
                          <TableCell>
                            <Badge variant="secondary" className="font-medium">
                              HTTP
                            </Badge>
                          </TableCell>
                          <TableCell className="align-middle">
                            <CheckSparkline checks={ep.recentChecksUp} />
                          </TableCell>
                          <TableCell className="font-medium">
                            <Link to={`/endpoints/${ep.id}`} className="text-primary hover:underline">
                              {ep.name}
                            </Link>
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
                                <span>
                                  <span className="text-muted-foreground/80">Interval</span>
                                  {' · '}
                                  every {ep.checkInterval} min
                                </span>
                                <span>
                                  <span className="text-muted-foreground/80">Last activity</span>
                                  {' · '}
                                  {formatShortDate(ep.lastChecked)}
                                </span>
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
                                  'inline-flex size-9 items-center justify-center rounded-lg',
                                )}
                              >
                                <Info className="size-4" />
                              </Link>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="size-9"
                                title="Edit"
                                onClick={() => {
                                  setEditEndpoint(ep)
                                  setEditOpen(true)
                                }}
                              >
                                <Pencil className="size-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="size-9"
                                title={ep.isActive ? 'Pause' : 'Resume'}
                                disabled={toggleMutation.isPending}
                                onClick={() => toggleMutation.mutate(ep.id)}
                              >
                                {ep.isActive ? <Pause className="size-4" /> : <Play className="size-4" />}
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="text-muted-foreground hover:text-destructive size-9"
                                title="Delete"
                                disabled={deleteEndpointMutation.isPending}
                                onClick={() => {
                                  if (window.confirm('Delete this endpoint? This cannot be undone.')) {
                                    deleteEndpointMutation.mutate(ep.id)
                                  }
                                }}
                              >
                                <Trash2 className="size-4" />
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      )
                    }
                    const hb = row.hb
                    const pingUrl = `${data.baseUrl.replace(/\/$/, '')}/heartbeat/${hb.token}`
                    return (
                      <TableRow key={`hb-${hb.id}`}>
                        <TableCell>
                          <Badge
                            variant="outline"
                            className="border-sky-500/40 bg-sky-500/10 font-medium text-sky-800 dark:text-sky-300"
                          >
                              Heartbeat
                          </Badge>
                        </TableCell>
                        <TableCell className="align-middle">
                          <CheckSparkline checks={hb.recentChecksUp} />
                        </TableCell>
                        <TableCell className="font-medium">{hb.name}</TableCell>
                        <TableCell className="max-w-[min(320px,45vw)]">
                          <div className="flex flex-col gap-1.5">
                            <code className="bg-muted text-foreground/90 inline-flex max-w-full items-center gap-2 rounded-md px-2 py-1 text-sm break-all">
                              {pingUrl}
                              <button
                                type="button"
                                className="text-muted-foreground hover:text-foreground shrink-0"
                                title="Copy URL"
                                onClick={() => void navigator.clipboard.writeText(pingUrl)}
                              >
                                <Copy className="size-3.5" />
                              </button>
                            </code>
                            <div className="text-muted-foreground flex flex-col gap-0.5 text-xs leading-snug sm:flex-row sm:flex-wrap sm:gap-x-4 sm:gap-y-0.5">
                              <span>
                                <span className="text-muted-foreground/80">Interval</span>
                                {' · '}
                                every {hb.expectedIntervalMinutes} min
                              </span>
                              <span>
                                <span className="text-muted-foreground/80">Last activity</span>
                                {' · '}
                                {formatShortDate(hb.lastPingAt)}
                              </span>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-muted-foreground hover:text-destructive"
                            disabled={deleteHbMutation.isPending}
                            onClick={() => {
                              if (window.confirm('Delete this heartbeat monitor?')) {
                                deleteHbMutation.mutate(hb.id)
                              }
                            }}
                          >
                            Delete
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
