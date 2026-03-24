import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircle, ExternalLink } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Skeleton } from '@/components/ui/skeleton.tsx'
import { apiFetch, apiJson } from '@/lib/apiClient.ts'
import type { StatusSettingsDto } from '@/types/api.ts'

async function fetchStatusSettings(): Promise<StatusSettingsDto> {
  return apiJson<StatusSettingsDto>('/api/settings/status')
}

export function StatusSettingsPage() {
  const queryClient = useQueryClient()
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: ['settings-status'],
    queryFn: fetchStatusSettings,
  })

  const saveMutation = useMutation({
    mutationFn: (body: { statusSlug: string; statusPageTitle: string; statusPageLogoUrl: string }) =>
      apiFetch('/api/settings/status', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          statusSlug: body.statusSlug || undefined,
          statusPageTitle: body.statusPageTitle || undefined,
          statusPageLogoUrl: body.statusPageLogoUrl || undefined,
        }),
      }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['settings-status'] })
        await queryClient.invalidateQueries({ queryKey: ['me'] })
      }
    },
  })

  function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const fd = new FormData(e.currentTarget)
    saveMutation.mutate({
      statusSlug: String(fd.get('statusSlug') ?? '').trim(),
      statusPageTitle: String(fd.get('statusPageTitle') ?? '').trim(),
      statusPageLogoUrl: String(fd.get('statusPageLogoUrl') ?? '').trim(),
    })
  }

  const slug = data?.statusSlug?.trim()

  return (
    <div className="w-full space-y-8 pb-12">
      <div>
        <p className="text-muted-foreground text-sm font-medium tracking-wide uppercase">Settings</p>
        <h1 className="text-3xl font-semibold tracking-tight">Public status page</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Customize your public status URL, title, and logo.
        </p>
      </div>

      {isError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Couldn’t load settings</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{error instanceof Error ? error.message : 'Unknown error'}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {isPending ? (
        <Skeleton className="h-96 w-full rounded-xl" />
      ) : data ? (
        <Card>
          <CardHeader>
            <CardTitle>Branding</CardTitle>
            <CardDescription>
              {slug ? (
                <span className="inline-flex flex-wrap items-center gap-1">
                  Public URL:{' '}
                  <Link
                    to={`/status/${slug}`}
                    className="text-primary inline-flex items-center gap-1 font-medium hover:underline"
                  >
                    /status/{slug}
                    <ExternalLink className="size-3" />
                  </Link>
                </span>
              ) : (
                'Set a slug to enable your public page.'
              )}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={onSubmit}>
              <div className="space-y-2">
                <Label htmlFor="statusSlug">URL slug</Label>
                <Input
                  id="statusSlug"
                  name="statusSlug"
                  placeholder="my-company"
                  defaultValue={data.statusSlug ?? ''}
                  autoComplete="off"
                />
                <p className="text-muted-foreground text-xs">Lowercase letters, numbers, and hyphens.</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="statusPageTitle">Page title</Label>
                <Input
                  id="statusPageTitle"
                  name="statusPageTitle"
                  placeholder="Acme Status"
                  defaultValue={data.statusPageTitle ?? ''}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="statusPageLogoUrl">Logo URL</Label>
                <Input
                  id="statusPageLogoUrl"
                  name="statusPageLogoUrl"
                  type="url"
                  placeholder="https://…"
                  defaultValue={data.statusPageLogoUrl ?? ''}
                />
              </div>
              {saveMutation.isError ? (
                <p className="text-destructive text-sm">Could not save. Try again.</p>
              ) : null}
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Saving…' : 'Save'}
              </Button>
            </form>
          </CardContent>
        </Card>
      ) : null}
    </div>
  )
}
