import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Activity, Globe } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { apiFetch, parseApiErrorBody, UnauthorizedError } from '@/lib/apiClient.ts'

const INTERVALS = [
  { value: '1', label: '1 minute' },
  { value: '5', label: '5 minutes' },
  { value: '10', label: '10 minutes' },
  { value: '15', label: '15 minutes' },
] as const

type Props = {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function AddMonitorDialog({ open, onOpenChange }: Props) {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState<'http' | 'heartbeat'>('http')
  const [error, setError] = useState<string | null>(null)

  const httpMutation = useMutation({
    mutationFn: async (fields: {
      name: string
      url: string
      checkInterval: string
      expectedBodySubstring?: string
    }) => {
      const checkInterval = Number.parseInt(fields.checkInterval, 10)
      if (!Number.isFinite(checkInterval) || checkInterval < 1) {
        return { ok: false as const, error: 'Invalid check interval.' }
      }
      try {
        const res = await apiFetch('/api/endpoints', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: fields.name,
            url: fields.url,
            checkInterval,
            ...(fields.expectedBodySubstring
              ? { expectedBodySubstring: fields.expectedBodySubstring }
              : {}),
          }),
        })
        if (res.status === 201) {
          return { ok: true as const }
        }
        return { ok: false as const, error: await parseApiErrorBody(res) }
      } catch (e) {
        if (e instanceof UnauthorizedError) {
          return { ok: false as const, error: 'Please sign in to add a monitor.' }
        }
        throw e
      }
    },
    onSuccess: async (result) => {
      if (result.ok) {
        setError(null)
        onOpenChange(false)
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
        await queryClient.invalidateQueries({ queryKey: ['me'] })
      } else {
        setError(result.error ?? 'Could not add monitor.')
      }
    },
  })

  const hbMutation = useMutation({
    mutationFn: async (fields: { name: string; expectedIntervalMinutes: number }) => {
      try {
        const res = await apiFetch('/api/heartbeats', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: fields.name,
            expectedIntervalMinutes: fields.expectedIntervalMinutes,
          }),
        })
        if (res.status === 201) {
          return { ok: true as const }
        }
        return { ok: false as const, error: await parseApiErrorBody(res) }
      } catch (e) {
        if (e instanceof UnauthorizedError) {
          return { ok: false as const, error: 'Please sign in to add a heartbeat monitor.' }
        }
        throw e
      }
    },
    onSuccess: async (result) => {
      if (result.ok) {
        setError(null)
        onOpenChange(false)
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
        await queryClient.invalidateQueries({ queryKey: ['me'] })
      } else {
        setError(result.error ?? 'Could not add heartbeat.')
      }
    },
  })

  function onHttpSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    const fd = new FormData(e.currentTarget)
    httpMutation.mutate({
      name: String(fd.get('name') ?? '').trim(),
      url: String(fd.get('url') ?? '').trim(),
      checkInterval: String(fd.get('checkInterval') ?? '5'),
      expectedBodySubstring: String(fd.get('expectedBodySubstring') ?? '').trim() || undefined,
    })
  }

  function onHbSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    const fd = new FormData(e.currentTarget)
    const mins = Number(fd.get('expectedIntervalMinutes'))
    hbMutation.mutate({
      name: String(fd.get('name') ?? '').trim(),
      expectedIntervalMinutes: Number.isFinite(mins) && mins >= 1 ? mins : 5,
    })
  }

  const busy = httpMutation.isPending || hbMutation.isPending

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (!v) setError(null)
        onOpenChange(v)
      }}
    >
      <DialogContent className="gap-0 overflow-hidden border-border/80 p-0 sm:max-w-lg">
        <div className="border-b border-border/80 bg-muted/30 px-6 py-5">
          <DialogHeader className="gap-1">
            <DialogTitle className="text-xl">Add monitor</DialogTitle>
            <DialogDescription>
              Track an HTTP endpoint or a cron-style heartbeat URL.
            </DialogDescription>
          </DialogHeader>
        </div>

        <div className="px-6 py-5">
          <Tabs value={tab} onValueChange={(v) => setTab(v as 'http' | 'heartbeat')}>
            <TabsList className="grid h-10 w-full grid-cols-2 rounded-lg bg-muted/80 p-1">
              <TabsTrigger value="http" className="gap-2 rounded-md data-active:shadow-sm">
                <Globe className="size-4 opacity-70" />
                HTTP / Uptime
              </TabsTrigger>
              <TabsTrigger value="heartbeat" className="gap-2 rounded-md data-active:shadow-sm">
                <Activity className="size-4 opacity-70" />
                Cron / Heartbeat
              </TabsTrigger>
            </TabsList>

            <TabsContent value="http" className="mt-5 space-y-4 outline-none">
              <form onSubmit={onHttpSubmit} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="add-name">Name</Label>
                  <Input id="add-name" name="name" placeholder="Production API" required autoComplete="off" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="add-url">URL</Label>
                  <Input
                    id="add-url"
                    name="url"
                    type="url"
                    placeholder="https://api.example.com/health"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="add-expected">Expected body text (optional)</Label>
                  <Input
                    id="add-expected"
                    name="expectedBodySubstring"
                    placeholder='e.g. "status": "ok"'
                    autoComplete="off"
                  />
                  <p className="text-xs text-muted-foreground">Check passes only if the body contains this substring.</p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="add-interval">Check every</Label>
                  <select
                    id="add-interval"
                    name="checkInterval"
                    className="border-input bg-background ring-offset-background focus-visible:ring-ring flex h-10 w-full rounded-md border px-3 py-2 text-sm focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none"
                    defaultValue="5"
                  >
                    {INTERVALS.map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                </div>
                <DialogFooter className="gap-2 pt-2 sm:justify-end">
                  <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={busy}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={busy}>
                    {busy ? 'Adding…' : 'Add HTTP monitor'}
                  </Button>
                </DialogFooter>
              </form>
            </TabsContent>

            <TabsContent value="heartbeat" className="mt-5 space-y-4 outline-none">
              <form onSubmit={onHbSubmit} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="hb-name">Name</Label>
                  <Input id="hb-name" name="name" placeholder="Nightly backup job" required autoComplete="off" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="hb-interval">Expected interval (minutes)</Label>
                  <Input
                    id="hb-interval"
                    name="expectedIntervalMinutes"
                    type="number"
                    min={1}
                    defaultValue={5}
                    required
                  />
                  <p className="text-xs text-muted-foreground">
                    You’ll ping a secret URL; we alert if a ping is missed beyond this window.
                  </p>
                </div>
                <DialogFooter className="gap-2 pt-2 sm:justify-end">
                  <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={busy}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={busy}>
                    {busy ? 'Adding…' : 'Add heartbeat monitor'}
                  </Button>
                </DialogFooter>
              </form>
            </TabsContent>
          </Tabs>

          {error ? (
            <p className="text-destructive mt-4 rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm">
              {error}
            </p>
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
  )
}
