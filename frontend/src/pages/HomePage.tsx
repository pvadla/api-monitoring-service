import type { ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowRight, Check, Mail, Rocket, Shield } from 'lucide-react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx'
import { Badge } from '@/components/ui/badge.tsx'
import { Button, buttonVariants } from '@/components/ui/button.tsx'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { Separator } from '@/components/ui/separator.tsx'
import { useAuth } from '@/contexts/AuthContext.tsx'
import { cn } from '@/lib/utils.ts'

const steps = [
  {
    step: 'Step 1',
    title: 'Sign in & create your workspace',
    body: 'Sign in with Google in a few seconds. APIWatch creates a private workspace just for you where you manage all your monitored APIs.',
  },
  {
    step: 'Step 2',
    title: 'Add the endpoints you care about',
    body: 'For each API, give it a name, paste the URL, and choose how often to check it (every 1, 5, 10, or 15 minutes). APIWatch pings these URLs in the background and records uptime, status codes, and response times.',
  },
  {
    step: 'Step 3',
    title: 'Get alerts & see incidents',
    body: 'When an endpoint goes down, APIWatch automatically opens an incident and can email you based on your notification settings. When it recovers, the incident is resolved and downtime is calculated for your reports.',
  },
] as const

function CheckRow({ ok, children }: { ok: boolean; children: ReactNode }) {
  return (
    <li className="flex items-center gap-3 text-sm">
      <span
        className={cn(
          'flex size-5 shrink-0 items-center justify-center rounded-full text-xs font-bold',
          ok ? 'bg-emerald-500/15 text-emerald-400' : 'bg-muted text-muted-foreground/50',
        )}
      >
        {ok ? <Check className="size-3" /> : '—'}
      </span>
      <span className={cn(!ok && 'text-muted-foreground/60')}>{children}</span>
    </li>
  )
}

export function HomePage() {
  const { user } = useAuth()
  const [searchParams] = useSearchParams()
  const accountDeleted = searchParams.get('accountDeleted') === '1'

  return (
    <div className="w-full space-y-0">
      {accountDeleted ? (
        <Alert className="border-emerald-500/30 bg-emerald-500/5 mb-10">
          <Shield className="text-emerald-400" />
          <AlertTitle className="text-emerald-100">Account removed</AlertTitle>
          <AlertDescription className="text-emerald-100/90">
            Your account and data have been deleted. You can sign in again anytime to start fresh.
          </AlertDescription>
        </Alert>
      ) : null}

      {/* Hero — landing (scroll targets use smooth scroll + scroll-padding on html) */}
      <section
        id="hero"
        className="relative -mx-4 scroll-mt-28 px-4 pb-16 pt-6 sm:-mx-6 sm:px-6 sm:pb-20 lg:-mx-8 lg:px-8"
        aria-labelledby="hero-heading"
      >
        <div className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(ellipse_85%_55%_at_50%_-15%,rgba(56,189,248,0.14),transparent_55%)]" />
        <div className="border-border/80 bg-card/40 w-full rounded-3xl border px-6 py-12 text-center shadow-sm shadow-black/20 backdrop-blur-sm sm:px-10 sm:py-16">
          <Badge
            variant="secondary"
            className="border-primary/25 bg-primary/10 text-primary mb-8 gap-2 px-4 py-1.5 text-sm font-medium"
          >
            <Rocket className="size-3.5" aria-hidden />
            Simple API monitoring
          </Badge>

          <h1
            id="hero-heading"
            className="text-balance text-4xl font-semibold tracking-tight text-foreground sm:text-5xl md:text-6xl md:leading-[1.1]"
          >
            Know when your API{' '}
            <span className="from-primary bg-gradient-to-r to-sky-300 bg-clip-text text-transparent">goes down</span>
            <br />
            before your users do
          </h1>

          <p className="text-muted-foreground mt-6 w-full text-balance text-lg leading-relaxed">
            Monitor your APIs 24/7, get instant alerts when something breaks, and share a beautiful status page with your
            customers. Set up in 60 seconds.
          </p>

          <div className="mt-10 flex flex-col items-stretch justify-center gap-3 sm:flex-row sm:items-center sm:justify-center">
            {user ? (
              <Link
                to="/dashboard"
                className={cn(
                  buttonVariants({ size: 'lg' }),
                  'inline-flex gap-2 shadow-lg shadow-primary/25',
                )}
              >
                Open dashboard
                <ArrowRight className="size-4" />
              </Link>
            ) : (
              <a
                href="/oauth2/authorization/google"
                className={cn(
                  buttonVariants({ size: 'lg' }),
                  'inline-flex gap-2 shadow-lg shadow-primary/25',
                )}
              >
                Start monitoring free
                <ArrowRight className="size-4" />
              </a>
            )}
            <a
              href="#how-it-works"
              className={cn(
                buttonVariants({ variant: 'outline', size: 'lg' }),
                'border-border/80 bg-background/50',
              )}
            >
              See how it works
            </a>
          </div>

          <div className="text-muted-foreground mt-8 flex flex-wrap items-center justify-center gap-x-8 gap-y-2 text-sm">
            <span className="inline-flex items-center gap-2">
              <Check className="size-4 shrink-0 text-emerald-400" aria-hidden />
              Free forever for 5 monitors
            </span>
            <span className="inline-flex items-center gap-2">
              <Check className="size-4 shrink-0 text-emerald-400" aria-hidden />
              No credit card required
            </span>
          </div>
        </div>
      </section>

      <Separator className="opacity-50" />

      {/* How It Works */}
      <section
        id="how-it-works"
        className="scroll-mt-28 bg-muted/20 py-16 -mx-4 px-4 sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8"
      >
        <div className="w-full space-y-12">
          <div className="text-center">
            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
              How <span className="text-primary">It Works</span>
            </h2>
            <p className="text-muted-foreground mt-4 w-full text-balance text-lg">
              APIWatch continuously checks your APIs, creates incidents when something breaks, and shows a clear status
              to your team and customers.
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-3">
            {steps.map((s) => (
              <Card key={s.step} className="border-border/80 bg-card/80 backdrop-blur-sm">
                <CardHeader>
                  <p className="text-primary text-xs font-semibold uppercase tracking-wider">{s.step}</p>
                  <CardTitle className="text-lg leading-snug">{s.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <CardDescription className="text-sm leading-relaxed">{s.body}</CardDescription>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      <Separator className="opacity-50" />

      {/* Pricing */}
      <section id="pricing" className="scroll-mt-28 py-16">
        <div className="w-full space-y-12">
          <div className="text-center">
            <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
              <span className="text-primary">Pricing</span>
            </h2>
            <p className="text-muted-foreground mt-4 text-xl">Start free. Upgrade when you need more.</p>
          </div>

          <div className="grid gap-8 md:grid-cols-3">
            <Card className="border-primary/40 relative overflow-hidden shadow-lg shadow-primary/5">
              <div className="from-primary/10 pointer-events-none absolute inset-0 bg-gradient-to-br to-transparent" />
              <CardHeader>
                <p className="text-muted-foreground text-sm font-medium uppercase tracking-wide">Free</p>
                <div className="flex items-end gap-1">
                  <span className="text-5xl font-bold tabular-nums">$0</span>
                  <span className="text-muted-foreground mb-2 text-sm">/month</span>
                </div>
                <CardDescription>Perfect for personal projects</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <Separator />
                <ul className="space-y-3">
                  <CheckRow ok>5 monitors (HTTP + heartbeat)</CheckRow>
                  <CheckRow ok>1–15 min check interval</CheckRow>
                  <CheckRow ok>Email alerts</CheckRow>
                  <CheckRow ok>Incident history</CheckRow>
                  <CheckRow ok>Public status page</CheckRow>
                  <CheckRow ok={false}>SMS alerts</CheckRow>
                </ul>
                {user ? (
                  <Link to="/dashboard" className={cn(buttonVariants({ variant: 'secondary' }), 'w-full')}>
                    Go to dashboard
                  </Link>
                ) : (
                  <a href="/oauth2/authorization/google" className={cn(buttonVariants(), 'w-full')}>
                    Get started free
                  </a>
                )}
              </CardContent>
            </Card>

            <Card className="border-border/80 bg-muted/10 opacity-95">
              <div className="flex justify-center">
                <Badge variant="secondary" className="-mt-1">
                  Coming soon
                </Badge>
              </div>
              <CardHeader>
                <p className="text-muted-foreground text-sm font-medium uppercase tracking-wide">Starter</p>
                <div className="flex items-end gap-1">
                  <span className="text-5xl font-bold tabular-nums text-muted-foreground">$7</span>
                  <span className="text-muted-foreground mb-2 text-sm">/month</span>
                </div>
                <CardDescription>For growing applications</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <Separator />
                <ul className="space-y-3">
                  <CheckRow ok>25 endpoints</CheckRow>
                  <CheckRow ok>1 min check interval</CheckRow>
                  <CheckRow ok>Email alerts</CheckRow>
                  <CheckRow ok>30 day history</CheckRow>
                  <CheckRow ok>Public status page</CheckRow>
                  <CheckRow ok={false}>SMS alerts</CheckRow>
                </ul>
                <Button className="w-full" disabled variant="secondary">
                  Coming soon
                </Button>
              </CardContent>
            </Card>

            <Card className="border-border/80 bg-muted/10 opacity-95">
              <div className="flex justify-center">
                <Badge variant="secondary" className="-mt-1">
                  Coming soon
                </Badge>
              </div>
              <CardHeader>
                <p className="text-muted-foreground text-sm font-medium uppercase tracking-wide">Pro</p>
                <div className="flex items-end gap-1">
                  <span className="text-5xl font-bold tabular-nums text-muted-foreground">$19</span>
                  <span className="text-muted-foreground mb-2 text-sm">/month</span>
                </div>
                <CardDescription>For production applications</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <Separator />
                <ul className="space-y-3">
                  <CheckRow ok>100 endpoints</CheckRow>
                  <CheckRow ok>30 sec check interval</CheckRow>
                  <CheckRow ok>Email alerts</CheckRow>
                  <CheckRow ok>90 day history</CheckRow>
                  <CheckRow ok>Public status page</CheckRow>
                  <CheckRow ok>SMS alerts</CheckRow>
                </ul>
                <Button className="w-full" disabled variant="secondary">
                  Coming soon
                </Button>
              </CardContent>
            </Card>
          </div>

          <p className="text-muted-foreground text-center text-sm">
            No credit card required · Cancel anytime · Paid plans when available
          </p>
        </div>
      </section>

      <Separator className="opacity-50" />

      {/* About */}
      <section
        id="about"
        className="scroll-mt-28 bg-muted/20 py-16 -mx-4 px-4 sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8"
      >
        <div className="w-full space-y-6 text-center">
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            <span className="text-primary">About</span> APIWatch
          </h2>
          <Card className="border-border/80 text-left">
            <CardContent className="text-muted-foreground space-y-4 pt-6 text-sm leading-relaxed">
              <p>
                APIWatch is built for teams who need reliable API monitoring without the overhead. We focus on clear
                uptime checks, actionable incidents, and optional public status pages so you can communicate with
                customers during outages.
              </p>
              <p>
                Whether you run a few personal endpoints or production APIs, you get a single dashboard for monitors,
                heartbeats, and history—backed by Google sign-in and straightforward settings.
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      <Separator className="opacity-50" />

      {/* Contact */}
      <section id="contact" className="scroll-mt-28 pb-20 pt-16">
        <div className="w-full space-y-8 text-center">
          <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            <span className="text-primary">Contact</span>
          </h2>
          <p className="text-muted-foreground text-lg">
            Questions, feedback, or partnership ideas? We’d love to hear from you.
          </p>
          <Card className="border-border/80">
            <CardContent className="flex flex-col items-center gap-6 pt-8 pb-8">
              <div className="bg-primary/10 text-primary flex size-14 items-center justify-center rounded-full">
                <Mail className="size-7" />
              </div>
              <p className="text-muted-foreground w-full text-sm">
                Use the contact page to send a message. We read every note and aim to reply within a few business days.
              </p>
              <Link to="/contact" className={cn(buttonVariants({ size: 'lg' }))}>
                Go to contact form
              </Link>
            </CardContent>
          </Card>
        </div>
      </section>
    </div>
  )
}
