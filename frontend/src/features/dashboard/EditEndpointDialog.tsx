import { useMutation, useQueryClient } from '@tanstack/react-query'
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
import { apiFetch, parseApiErrorBody, UnauthorizedError } from '@/lib/apiClient.ts'
import type { EndpointRow } from '@/types/dashboard'

const INTERVALS = [
  { value: '1', label: '1 minute' },
  { value: '5', label: '5 minutes' },
  { value: '10', label: '10 minutes' },
  { value: '15', label: '15 minutes' },
] as const

type Props = {
  endpoint: EndpointRow | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function EditEndpointDialog({ endpoint, open, onOpenChange }: Props) {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: async (fields: {
      id: number
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
        const res = await apiFetch(`/api/endpoints/${fields.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: fields.name,
            url: fields.url,
            checkInterval,
            expectedBodySubstring: fields.expectedBodySubstring?.trim()
              ? fields.expectedBodySubstring.trim()
              : null,
          }),
        })
        if (res.ok) {
          return { ok: true as const }
        }
        return { ok: false as const, error: await parseApiErrorBody(res) }
      } catch (e) {
        if (e instanceof UnauthorizedError) {
          return { ok: false as const, error: 'Please sign in to save changes.' }
        }
        throw e
      }
    },
    onSuccess: async (result) => {
      if (result.ok) {
        setError(null)
        onOpenChange(false)
        await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
        if (endpoint) {
          await queryClient.invalidateQueries({ queryKey: ['endpoint', endpoint.id] })
        }
      } else {
        setError(result.error ?? 'Could not save changes.')
      }
    },
  })

  function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!endpoint) return
    setError(null)
    const fd = new FormData(e.currentTarget)
    mutation.mutate({
      id: endpoint.id,
      name: String(fd.get('name') ?? '').trim(),
      url: String(fd.get('url') ?? '').trim(),
      checkInterval: String(fd.get('checkInterval') ?? '5'),
      expectedBodySubstring: String(fd.get('expectedBodySubstring') ?? '').trim() || undefined,
    })
  }

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
            <DialogTitle className="text-xl">Edit endpoint</DialogTitle>
            <DialogDescription>Update how this URL is checked.</DialogDescription>
          </DialogHeader>
        </div>

        {endpoint ? (
          <form key={endpoint.id} onSubmit={onSubmit} className="space-y-4 px-6 py-5">
            <div className="space-y-2">
              <Label htmlFor="edit-name">Name</Label>
              <Input
                id="edit-name"
                name="name"
                defaultValue={endpoint.name}
                required
                autoComplete="off"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-url">URL</Label>
              <Input id="edit-url" name="url" type="url" defaultValue={endpoint.url} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-expected">Expected body text (optional)</Label>
              <Input
                id="edit-expected"
                name="expectedBodySubstring"
                defaultValue={endpoint.expectedBodySubstring ?? ''}
                placeholder='e.g. "healthy"'
                autoComplete="off"
              />
              <p className="text-xs text-muted-foreground">Check passes only if the body contains this substring.</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-interval">Check every</Label>
              <select
                id="edit-interval"
                name="checkInterval"
                className="border-input bg-background ring-offset-background focus-visible:ring-ring flex h-10 w-full rounded-md border px-3 py-2 text-sm focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none"
                defaultValue={String(endpoint.checkInterval)}
              >
                {INTERVALS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
            {error ? (
              <p className="text-destructive rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm">
                {error}
              </p>
            ) : null}
            <DialogFooter className="gap-2 pt-2 sm:justify-end">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={mutation.isPending}>
                Cancel
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? 'Saving…' : 'Save changes'}
              </Button>
            </DialogFooter>
          </form>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}
