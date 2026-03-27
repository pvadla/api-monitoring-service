import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Activity, Globe, Lock } from 'lucide-react'
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
  const [tab, setTab] = useState<'http' | 'heartbeat' | 'ssl'>('http')
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

  const sslMutation = useMutation({
    mutationFn: async (fields: {
      name: string
      domain: string
      port: number
      alertDaysThreshold: number
    }) => {
      try {
        const res = await apiFetch('/api/ssl-monitors', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(fields),
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
        setError(result.error ?? 'Could not add SSL monitor.')
      }
    },
  })

  function onSslSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    const fd = new FormData(e.currentTarget)
    const port = Number(fd.get('port'))
    const alertDays = Number(fd.get('alertDaysThreshold'))
    sslMutation.mutate({
      name: String(fd.get('name') ?? '').trim(),
      domain: String(fd.get('domain') ?? '').trim(),
      port: Number.isFinite(port) && port > 0 ? port : 443,
      alertDaysThreshold: Number.isFinite(alertDays) && alertDays > 0 ? alertDays : 30,
    })
  }

  const busy = httpMutation.isPending || hbMutation.isPending || sslMutation.isPending

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
              Track an HTTP endpoint, a cron-style heartbeat, or an SSL certificate.
            </DialogDescription>
          </DialogHeader>
        </div>

        <div className="px-6 py-5">
          <Tabs value={tab} onValueChange={(v) => setTab(v as 'http' | 'heartbeat' | 'ssl')}>
            <TabsList className="grid h-10 w-full grid-cols-3 rounded-lg bg-muted/80 p-1">
              <TabsTrigger value="http" className="gap-1.5 rounded-md data-active:shadow-sm">
                <Globe className="size-3.5 opacity-70" />
                HTTP
              </TabsTrigger>
              <TabsTrigger value="heartbeat" className="gap-1.5 rounded-md data-active:shadow-sm">
                <Activity className="size-3.5 opacity-70" />
                Heartbeat
              </TabsTrigger>
              <TabsTrigger value="ssl" className="gap-1.5 rounded-md data-active:shadow-sm">
                <Lock className="size-3.5 opacity-70" />
                SSL
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

            <TabsContent value="ssl" className="mt-5 space-y-4 outline-none">
              <form onSubmit={onSslSubmit} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="ssl-name">Name</Label>
                  <Input id="ssl-name" name="name" placeholder="Production API cert" required autoComplete="off" />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="ssl-domain">Domain</Label>
                  <Input
                    id="ssl-domain"
                    name="domain"
                    placeholder="example.com"
                    required
                    autoComplete="off"
                    autoCapitalize="none"
                  />
                  <p className="text-xs text-muted-foreground">
                    Hostname only — no https:// prefix, no trailing slash.
                  </p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-2">
                    <Label htmlFor="ssl-port">Port</Label>
                    <Input
                      id="ssl-port"
                      name="port"
                      type="number"
                      min={1}
                      max={65535}
                      defaultValue={443}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ssl-threshold">{'Alert when < (days)'}</Label>
                    <Input
                      id="ssl-threshold"
                      name="alertDaysThreshold"
                      type="number"
                      min={1}
                      defaultValue={30}
                      required
                    />
                  </div>
                </div>
                <p className="text-xs text-muted-foreground">
                  We alert when the certificate expires within the configured number of days. Checks run every 6 hours.
                </p>
                <DialogFooter className="gap-2 pt-2 sm:justify-end">
                  <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={busy}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={busy}>
                    {busy ? 'Adding…' : 'Add SSL monitor'}
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
