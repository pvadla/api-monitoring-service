import { useState, type FormEvent } from 'react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { Button } from '@/components/ui/button.tsx'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Label } from '@/components/ui/label.tsx'
import { apiFetch, parseApiErrorBody } from '@/lib/apiClient.ts'
import { cn } from '@/lib/utils.ts'

const textareaClass = cn(
  'border-input bg-background placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50',
  'dark:bg-input/30',
  'min-h-[140px] w-full resize-y rounded-lg border px-2.5 py-2 text-sm outline-none transition-colors',
  'focus-visible:ring-3 disabled:cursor-not-allowed disabled:opacity-50',
)

export function ContactPage() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  async function onSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    setSubmitting(true)
    try {
      const res = await apiFetch('/api/public/contact', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: name.trim(),
          email: email.trim(),
          subject: subject.trim() || null,
          message: message.trim(),
        }),
      })
      if (!res.ok) {
        setError(await parseApiErrorBody(res))
        return
      }
      setSuccess(true)
      setName('')
      setEmail('')
      setSubject('')
      setMessage('')
    } catch {
      setError('Something went wrong. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto w-full max-w-xl space-y-8 pb-8">
      <div className="space-y-2 text-center sm:text-left">
        <h1 className="text-foreground text-3xl font-semibold tracking-tight">Contact us</h1>
        <p className="text-muted-foreground text-sm leading-relaxed">
          Questions about APIWatch, billing, or feature requests? Send a message and we’ll get back to you.
        </p>
      </div>

      <Card className="border-border/80">
        <CardHeader>
          <CardTitle className="text-lg">Send a message</CardTitle>
          <CardDescription>
            Uses a secure API—no legacy form post. We read every submission and aim to reply within a few business days.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {success ? (
            <Alert className="border-emerald-500/30 bg-emerald-500/5">
              <AlertTitle className="text-emerald-200">Message sent</AlertTitle>
              <AlertDescription className="text-emerald-100/90">
                Thanks for reaching out. If you need anything else, you can send another message below.
              </AlertDescription>
            </Alert>
          ) : null}

          {error ? (
            <Alert variant="destructive" className="mb-4">
              <AlertTitle>Couldn’t send</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          ) : null}

          <form onSubmit={(e) => void onSubmit(e)} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="contact-name">Name</Label>
              <Input
                id="contact-name"
                name="name"
                autoComplete="name"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your name"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="contact-email">Email</Label>
              <Input
                id="contact-email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="contact-subject">Subject (optional)</Label>
              <Input
                id="contact-subject"
                name="subject"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder="e.g. Billing question"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="contact-message">Message</Label>
              <textarea
                id="contact-message"
                name="message"
                required
                rows={5}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="Tell us about your question or idea…"
                className={textareaClass}
              />
            </div>
            <Button type="submit" disabled={submitting} className="w-full sm:w-auto">
              {submitting ? 'Sending…' : 'Send message'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <p className="text-muted-foreground text-center text-xs sm:text-left">
        Prefer email directly?{' '}
        <a href="mailto:support@apiwatch.app" className="text-primary font-medium hover:underline">
          support@apiwatch.app
        </a>
      </p>
    </div>
  )
}
