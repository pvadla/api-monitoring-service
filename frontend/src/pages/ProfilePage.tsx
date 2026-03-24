import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircle } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Label } from '@/components/ui/label.tsx'
import { Skeleton } from '@/components/ui/skeleton.tsx'
import { apiFetch, apiJson } from '@/lib/apiClient.ts'
import { useAuth } from '@/contexts/AuthContext.tsx'
import type { UserSettingsDto } from '@/types/api.ts'

async function fetchProfile(): Promise<UserSettingsDto> {
  return apiJson<UserSettingsDto>('/api/settings/profile')
}

export function ProfilePage() {
  const queryClient = useQueryClient()
  const { refresh } = useAuth()
  const navigate = useNavigate()

  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: ['settings-profile'],
    queryFn: fetchProfile,
  })

  const saveMutation = useMutation({
    mutationFn: (body: { name: string; notifyOnEndpointDown: boolean; notifyOnEndpointRecovery: boolean }) =>
      apiFetch('/api/settings/profile', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      }),
    onSuccess: async (r) => {
      if (r.ok) {
        await queryClient.invalidateQueries({ queryKey: ['settings-profile'] })
        await queryClient.invalidateQueries({ queryKey: ['me'] })
      }
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => apiFetch('/api/settings/profile/delete-account', { method: 'POST' }),
    onSuccess: async (r) => {
      if (r.ok) {
        await refresh()
        navigate('/?accountDeleted=1')
      }
    },
  })

  function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const fd = new FormData(e.currentTarget)
    saveMutation.mutate({
      name: String(fd.get('name') ?? '').trim(),
      notifyOnEndpointDown: fd.get('notifyOnEndpointDown') === 'on',
      notifyOnEndpointRecovery: fd.get('notifyOnEndpointRecovery') === 'on',
    })
  }

  return (
    <div className="w-full space-y-8 pb-12">
      <div>
        <p className="text-muted-foreground text-sm font-medium tracking-wide uppercase">Settings</p>
        <h1 className="text-3xl font-semibold tracking-tight">Profile</h1>
        <p className="text-muted-foreground mt-1 text-sm">Your account and notification preferences.</p>
      </div>

      {isError ? (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertTitle>Couldn’t load profile</AlertTitle>
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>{error instanceof Error ? error.message : 'Unknown error'}</span>
            <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
              Retry
            </Button>
          </AlertDescription>
        </Alert>
      ) : null}

      {isPending ? (
        <Skeleton className="h-80 w-full rounded-xl" />
      ) : data ? (
        <Card>
          <CardHeader>
            <CardTitle>Account</CardTitle>
            <CardDescription>Signed in as {data.email}</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-6" onSubmit={onSubmit}>
              <div className="space-y-2">
                <Label htmlFor="name">Display name</Label>
                <Input id="name" name="name" defaultValue={data.name ?? ''} autoComplete="name" />
              </div>
              <div className="space-y-3">
                <Label>Notifications</Label>
                <label className="flex cursor-pointer items-center gap-3 text-sm">
                  <input
                    type="checkbox"
                    name="notifyOnEndpointDown"
                    defaultChecked={Boolean(data.notifyOnEndpointDown)}
                    className="border-border size-4 rounded"
                  />
                  Notify when an endpoint goes down
                </label>
                <label className="flex cursor-pointer items-center gap-3 text-sm">
                  <input
                    type="checkbox"
                    name="notifyOnEndpointRecovery"
                    defaultChecked={Boolean(data.notifyOnEndpointRecovery)}
                    className="border-border size-4 rounded"
                  />
                  Notify when an endpoint recovers
                </label>
              </div>
              {saveMutation.isError ? (
                <p className="text-destructive text-sm">Could not save. Try again.</p>
              ) : null}
              <Button type="submit" disabled={saveMutation.isPending}>
                {saveMutation.isPending ? 'Saving…' : 'Save changes'}
              </Button>
            </form>

            <div className="border-border mt-10 border-t pt-8">
              <h3 className="text-destructive font-medium">Danger zone</h3>
              <p className="text-muted-foreground mt-1 text-sm">
                Permanently delete your account and all monitors, incidents, and data.
              </p>
              <Button
                type="button"
                variant="destructive"
                className="mt-4"
                disabled={deleteMutation.isPending}
                onClick={() => {
                  if (
                    window.confirm(
                      'Delete your account and all data? This cannot be undone.',
                    )
                  ) {
                    deleteMutation.mutate()
                  }
                }}
              >
                {deleteMutation.isPending ? 'Deleting…' : 'Delete account'}
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : null}
    </div>
  )
}
