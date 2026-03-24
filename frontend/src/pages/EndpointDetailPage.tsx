import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { AlertCircle, ArrowLeft, ExternalLink, Info, Pause, Play, Trash2 } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { EditEndpointDialog } from '@/features/dashboard/EditEndpointDialog.tsx'
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
import { apiFetch, apiJson } from '@/lib/apiClient.ts'
import { formatShortDate } from '@/lib/format.ts'
import { cn } from '@/lib/utils.ts'
import type { EndpointDetailPayload } from '@/types/api.ts'
import type { EndpointRow } from '@/types/dashboard.ts'

async function fetchDetail(id: number): Promise<EndpointDetailPayload> {
  return apiJson<EndpointDetailPayload>(`/api/endpoints/${id}`)
}

export function EndpointDetailPage() {
  const { id: idParam } = useParams()
  const id = Number(idParam)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: ['endpoint', id],
    queryFn: () => fetchDetail(id),
    enabled: Number.isFinite(id) && id > 0,
  })

  const toggleMutation = useMutation({
    mutationFn: () => apiFetch(`/api/endpoints/${id}/toggle`, { method: 'POST' }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['endpoint', id] })
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      }
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => apiFetch(`/api/endpoints/${id}`, { method: 'DELETE' }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
        navigate('/dashboard')
      }
    },
  })

  const toggleStatusMutation = useMutation({
    mutationFn: () => apiFetch(`/api/endpoints/${id}/toggle-status-visibility`, { method: 'POST' }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['endpoint', id] })
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      }
    },
  })

  const [editOpen, setEditOpen] = useState(false)

  if (!Number.isFinite(id) || id <= 0) {
    return (
      <Alert variant="destructive">
        <AlertCircle />
        <AlertTitle>Invalid endpoint</AlertTitle>
        <AlertDescription>
          <Link to="/dashboard" className="text-primary underline">
            Back to dashboard
          </Link>
        </AlertDescription>
      </Alert>
    )
  }

  const ep = data?.endpoint
  const editRow: EndpointRow | null = ep ?? null

  return (
    <div className="w-full space-y-8 pb-12">
      <div className="text-muted-foreground flex flex-wrap items-center gap-2 text-sm">
        <Link
          to="/dashboard"
          className={cn(buttonVariants({ variant: 'ghost', size: 'sm' }), 'gap-1 pl-0')}
        >
          <ArrowLeft className="size-4" />
          Dashboard
        </Link>
        <span>/</span>
        <span className="text-foreground font-medium">{ep?.name ?? '…'}</span>
      </div>

      {isError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Couldn’t load endpoint</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{error instanceof Error ? error.message : 'Unknown error'}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {isPending ? (
        <Skeleton className="h-48 w-full rounded-xl" />
      ) : data && ep ? (
        <>
          <div className="flex flex-col gap-6 md:flex-row md:items-start md:justify-between">
            <div className="flex items-start gap-4">
              <span
                className={cn(
                  'mt-1 inline-block size-3 shrink-0 rounded-full ring-4',
                  ep.isUp ? 'bg-emerald-500 ring-emerald-500/20' : 'bg-red-500 ring-red-500/20',
                )}
              />
              <div>
                <h1 className="text-3xl font-semibold tracking-tight">{ep.name}</h1>
                <a
                  href={ep.url}
                  target="_blank"
                  rel="noreferrer"
                  className="text-primary mt-1 inline-flex items-center gap-1 text-sm hover:underline"
                >
                  {ep.url}
                  <ExternalLink className="size-3.5 opacity-70" />
                </a>
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button type="button" variant="outline" size="sm" onClick={() => setEditOpen(true)}>
                Edit
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={toggleMutation.isPending}
                onClick={() => toggleMutation.mutate()}
              >
                {ep.isActive ? (
                  <>
                    <Pause className="size-4" /> Pause
                  </>
                ) : (
                  <>
                    <Play className="size-4" /> Resume
                  </>
                )}
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={toggleStatusMutation.isPending}
                onClick={() => toggleStatusMutation.mutate()}
                title="Show or hide on your public status page"
              >
                {ep.showOnStatusPage ? 'Hide from status page' : 'Show on status page'}
              </Button>
              <Button
                type="button"
                variant="destructive"
                size="sm"
                disabled={deleteMutation.isPending}
                onClick={() => {
                  if (window.confirm('Delete this endpoint? This cannot be undone.')) {
                    deleteMutation.mutate()
                  }
                }}
              >
                <Trash2 className="size-4" /> Delete
              </Button>
            </div>
          </div>

          {!ep.isActive ? (
            <Alert>
              <Info className="size-4" />
              <AlertTitle>Monitoring paused</AlertTitle>
              <AlertDescription>Checks are not running until you resume.</AlertDescription>
            </Alert>
          ) : null}

          <div className="grid gap-4 sm:grid-cols-3">
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>Uptime</CardDescription>
                <CardTitle className="text-3xl tabular-nums">{data.uptimePct}%</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-xs">Across recorded checks</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>Avg response</CardDescription>
                <CardTitle className="text-3xl tabular-nums">{data.avgResponseMs} ms</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-xs">Successful checks only</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardDescription>Status</CardDescription>
                <CardTitle className="text-2xl">
                  <Badge
                    variant="outline"
                    className={cn(
                      ep.isUp
                        ? 'border-emerald-500/40 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300'
                        : 'border-red-500/40 bg-red-500/10 text-red-700 dark:text-red-300',
                    )}
                  >
                    {ep.isUp ? 'UP' : 'DOWN'}
                  </Badge>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground text-xs">Last check: {formatShortDate(ep.lastChecked)}</p>
              </CardContent>
            </Card>
          </div>

          <section className="space-y-3">
            <h2 className="text-lg font-semibold tracking-tight">Recent checks</h2>
            <Card className="overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/40 hover:bg-muted/40">
                    <TableHead>Time</TableHead>
                    <TableHead className="w-24">HTTP</TableHead>
                    <TableHead className="w-28">Latency</TableHead>
                    <TableHead className="w-24">Result</TableHead>
                    <TableHead>Notes</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.checks.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} className="text-muted-foreground py-10 text-center text-sm">
                        No checks yet.
                      </TableCell>
                    </TableRow>
                  ) : (
                    data.checks.map((c) => (
                      <TableRow key={c.id}>
                        <TableCell className="text-muted-foreground text-sm">
                          {formatShortDate(c.checkedAt)}
                        </TableCell>
                        <TableCell className="font-mono text-sm">{c.statusCode ?? '—'}</TableCell>
                        <TableCell className="text-sm">
                          {c.responseTimeMs != null ? `${c.responseTimeMs} ms` : '—'}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant="outline"
                            className={cn(
                              c.isUp
                                ? 'border-emerald-500/40 text-emerald-700 dark:text-emerald-300'
                                : 'border-red-500/40 text-red-700 dark:text-red-300',
                            )}
                          >
                            {c.isUp ? 'OK' : 'Fail'}
                          </Badge>
                        </TableCell>
                        <TableCell className="max-w-md truncate text-sm text-muted-foreground">
                          {c.errorMessage ?? '—'}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </Card>
          </section>

          <EditEndpointDialog
            endpoint={editRow}
            open={editOpen}
            onOpenChange={(v) => setEditOpen(v)}
          />
        </>
      ) : null}
    </div>
  )
}
