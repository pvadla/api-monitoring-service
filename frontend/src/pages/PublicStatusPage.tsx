import { useQuery } from '@tanstack/react-query'
import { AlertCircle } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { Badge } from '@/components/ui/badge.tsx'
import { Card, CardContent } from '@/components/ui/card.tsx'
import { Skeleton } from '@/components/ui/skeleton.tsx'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table.tsx'
import { apiFetch } from '@/lib/apiClient.ts'
import { formatShortDate } from '@/lib/format.ts'
import { cn } from '@/lib/utils.ts'
import type { PublicStatusPayload } from '@/types/api.ts'

async function fetchPublicStatus(slug: string): Promise<PublicStatusPayload> {
  const res = await apiFetch(`/api/public/status/${encodeURIComponent(slug)}`)
  if (res.status === 404) {
    throw new Error('Status page not found')
  }
  if (!res.ok) {
    throw new Error('Could not load status page')
  }
  return res.json() as Promise<PublicStatusPayload>
}

export function PublicStatusPage() {
  const { slug } = useParams()
  const safeSlug = slug?.trim() ?? ''

  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: ['public-status', safeSlug],
    queryFn: () => fetchPublicStatus(safeSlug),
    enabled: safeSlug.length > 0,
  })

  if (!safeSlug) {
    return (
      <Alert variant="destructive">
        <AlertCircle />
        <AlertTitle>Missing slug</AlertTitle>
        <AlertDescription>
          <Link to="/" className="text-primary underline">
            Home
          </Link>
        </AlertDescription>
      </Alert>
    )
  }

  return (
    <div className="w-full space-y-10 pb-16 pt-4">
      {isError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Not found</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{error instanceof Error ? error.message : 'Unknown error'}</span>
            <button
              type="button"
              className="text-sm underline"
              onClick={() => void refetch()}
            >
              Retry
            </button>
          </AlertDescription>
        </Alert>
      ) : null}

      {isPending ? (
        <div className="space-y-6">
          <Skeleton className="mx-auto h-12 w-12 rounded-lg" />
          <Skeleton className="h-10 w-64 mx-auto" />
          <Skeleton className="h-40 w-full rounded-xl" />
        </div>
      ) : data ? (
        <>
          <header className="text-center">
            {data.logoUrl ? (
              <img src={data.logoUrl} alt="" className="mx-auto mb-4 h-12 object-contain" />
            ) : null}
            <h1 className="text-3xl font-semibold tracking-tight">{data.pageTitle}</h1>
            <p className="text-muted-foreground mt-1 text-sm">Service status and incident history</p>
          </header>

          <Card>
            <CardContent className="flex items-center gap-3 pt-6">
              <span
                className={cn(
                  'size-3 shrink-0 rounded-full',
                  data.statusKind === 'all-up' && 'bg-emerald-500',
                  data.statusKind === 'issues' && 'bg-amber-500',
                  data.statusKind === 'none' && 'bg-muted-foreground/40',
                )}
              />
              <span className="text-lg font-semibold">{data.overallStatusLabel}</span>
            </CardContent>
          </Card>

          <section className="space-y-3">
            <h2 className="text-lg font-semibold">Components</h2>
            <Card className="overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/40 hover:bg-muted/40">
                    <TableHead>Name</TableHead>
                    <TableHead className="w-28">Status</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.endpoints.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={2} className="text-muted-foreground py-10 text-center text-sm">
                        No components on this page yet.
                      </TableCell>
                    </TableRow>
                  ) : (
                    data.endpoints.map((ep) => (
                      <TableRow key={ep.id}>
                        <TableCell className="font-medium">{ep.name}</TableCell>
                        <TableCell>
                          <Badge
                            variant="outline"
                            className={cn(
                              ep.isUp
                                ? 'border-emerald-500/40 text-emerald-700 dark:text-emerald-300'
                                : 'border-red-500/40 text-red-700 dark:text-red-300',
                            )}
                          >
                            {ep.isUp ? 'Operational' : 'Outage'}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </Card>
          </section>

          <section className="space-y-3">
            <h2 className="text-lg font-semibold">Recent incidents</h2>
            <Card className="overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted/40 hover:bg-muted/40">
                    <TableHead>Title</TableHead>
                    <TableHead className="w-32">Status</TableHead>
                    <TableHead className="w-44">Started</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.incidents.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={3} className="text-muted-foreground py-10 text-center text-sm">
                        No incidents reported.
                      </TableCell>
                    </TableRow>
                  ) : (
                    data.incidents.map((inc) => (
                      <TableRow key={inc.id}>
                        <TableCell>
                          <div className="font-medium">{inc.title}</div>
                          {inc.description ? (
                            <p className="text-muted-foreground mt-0.5 line-clamp-2 text-xs">{inc.description}</p>
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
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </Card>
          </section>

          <p className="text-muted-foreground text-center text-xs">
            Powered by{' '}
            <Link to="/" className="text-primary hover:underline">
              APIWatch
            </Link>
          </p>
        </>
      ) : null}
    </div>
  )
}
