import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircle, ExternalLink } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { Badge } from '@/components/ui/badge.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Card } from '@/components/ui/card.tsx'
import { Label } from '@/components/ui/label.tsx'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select.tsx'
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
import type { IncidentDto } from '@/types/api.ts'

const STATUSES = ['INVESTIGATING', 'IDENTIFIED', 'MONITORING', 'RESOLVED'] as const

function normalizeIncidentStatus(s: string | null): (typeof STATUSES)[number] {
  if (s && (STATUSES as readonly string[]).includes(s)) {
    return s as (typeof STATUSES)[number]
  }
  return 'INVESTIGATING'
}

async function fetchIncidents(): Promise<IncidentDto[]> {
  return apiJson<IncidentDto[]>('/api/incidents')
}

export function IncidentsPage() {
  const queryClient = useQueryClient()
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: ['incidents'],
    queryFn: fetchIncidents,
  })

  const resolveMutation = useMutation({
    mutationFn: (id: number) => apiFetch(`/api/incidents/${id}/resolve`, { method: 'POST' }),
    onSuccess: async (r) => {
      if (r.ok) await queryClient.invalidateQueries({ queryKey: ['incidents'] })
    },
  })

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      apiFetch(`/api/incidents/${id}/status`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      }),
    onSuccess: async (r) => {
      if (r.ok) await queryClient.invalidateQueries({ queryKey: ['incidents'] })
    },
  })

  return (
    <div className="w-full space-y-8 pb-12">
      <div>
        <p className="text-muted-foreground text-sm font-medium tracking-wide uppercase">Operations</p>
        <h1 className="text-3xl font-semibold tracking-tight">Incidents</h1>
        <p className="text-muted-foreground mt-1 w-full text-sm">
          Track downtime and updates. Resolve or change status from here.
        </p>
      </div>

      {isError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Couldn’t load incidents</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{error instanceof Error ? error.message : 'Unknown error'}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      <Card className="overflow-hidden">
        {isPending ? (
          <div className="p-6">
            <Skeleton className="h-48 w-full" />
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/40 hover:bg-muted/40">
                <TableHead>Title</TableHead>
                <TableHead className="w-32">Status</TableHead>
                <TableHead className="w-40">Started</TableHead>
                <TableHead className="w-40">Resolved</TableHead>
                <TableHead className="w-36">Endpoint</TableHead>
                <TableHead className="w-52 text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {!data || data.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-muted-foreground py-14 text-center text-sm">
                    No incidents yet.
                  </TableCell>
                </TableRow>
              ) : (
                data.map((inc) => (
                  <TableRow key={inc.id}>
                    <TableCell>
                      <div className="font-medium">{inc.title}</div>
                      {inc.description ? (
                        <p className="text-muted-foreground mt-0.5 line-clamp-2 text-xs">{inc.description}</p>
                      ) : null}
                      {inc.failureReason ? (
                        <p className="text-muted-foreground mt-1 text-xs">Reason: {inc.failureReason}</p>
                      ) : null}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className="font-normal">
                        {inc.status ?? '—'}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {formatShortDate(inc.startedAt)}
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {formatShortDate(inc.resolvedAt)}
                    </TableCell>
                    <TableCell>
                      {inc.endpointId != null ? (
                        <Link
                          to={`/endpoints/${inc.endpointId}`}
                          className="text-primary inline-flex items-center gap-1 text-sm hover:underline"
                        >
                          View
                          <ExternalLink className="size-3" />
                        </Link>
                      ) : (
                        <span className="text-muted-foreground text-sm">—</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex flex-col items-end gap-2 sm:flex-row sm:justify-end">
                        <div className="flex items-center gap-2">
                          <Label className="sr-only" htmlFor={`status-${inc.id}`}>
                            Status
                          </Label>
                          <Select
                            value={normalizeIncidentStatus(inc.status)}
                            onValueChange={(v) => {
                              if (v) statusMutation.mutate({ id: inc.id, status: v })
                            }}
                            disabled={statusMutation.isPending}
                          >
                            <SelectTrigger id={`status-${inc.id}`} className="h-8 w-[140px] text-xs">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {STATUSES.map((s) => (
                                <SelectItem key={s} value={s}>
                                  {s}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                        <Button
                          type="button"
                          variant="secondary"
                          size="sm"
                          disabled={inc.status === 'RESOLVED' || resolveMutation.isPending}
                          onClick={() => resolveMutation.mutate(inc.id)}
                        >
                          Resolve
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </Card>
    </div>
  )
}
