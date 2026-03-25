import { Link } from 'react-router-dom'

import { Badge } from '@/components/ui/badge.tsx'
import { buttonVariants } from '@/components/ui/button.tsx'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card.tsx'
import { cn } from '@/lib/utils.ts'

export function AboutPage() {
  return (
    <div className="w-full space-y-12 pb-8">
      <section className="space-y-4 text-center">
        <h1 className="text-foreground text-3xl font-semibold tracking-tight sm:text-4xl">
          About <span className="text-primary">APIWatch</span>
        </h1>
        <p className="text-muted-foreground mx-auto max-w-3xl text-base leading-relaxed sm:text-lg">
          APIWatch is a simple monitoring tool built for developers who want signal, not noise. It helps you catch
          outages before your users do, understand incidents clearly, and share transparent status pages.
        </p>
      </section>

      <section className="grid gap-8 md:grid-cols-2 md:items-start">
        <div className="space-y-3">
          <h2 className="text-foreground text-xl font-semibold tracking-tight">Why we built APIWatch</h2>
          <p className="text-muted-foreground text-sm leading-relaxed">
            Most monitoring tools are heavy, noisy, or designed for large ops teams. APIWatch focuses on the essentials:
            uptime, response times, incidents, and a status page your customers can actually understand.
          </p>
          <p className="text-muted-foreground text-sm leading-relaxed">
            Our goal is to give solo developers and small teams production-grade monitoring without the complexity and
            cost of enterprise tools.
          </p>
        </div>
        <Card className="border-border/80">
          <CardHeader className="pb-2">
            <CardTitle className="text-base">Road so far</CardTitle>
          </CardHeader>
          <CardContent className="text-muted-foreground space-y-3 text-sm leading-relaxed">
            <p>
              <span className="text-foreground font-semibold">MVP:</span> HTTP uptime checks, incidents, email alerts,
              status page.
            </p>
            <p>
              <span className="text-foreground font-semibold">Phase 2:</span> Heartbeat monitoring, SSL expiry alerts,
              body assertions.
            </p>
            <p>
              <span className="text-foreground font-semibold">Today:</span> Polishing the dashboard, notifications, and
              subscription features.
            </p>
          </CardContent>
        </Card>
      </section>

      <section className="space-y-6">
        <div>
          <h2 className="text-foreground text-xl font-semibold tracking-tight">Upcoming features</h2>
          <p className="text-muted-foreground mt-1 max-w-2xl text-sm">
            Here is what we are actively working on next. Priorities may shift with your feedback.
          </p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <FeatureCard
            title="Team access and collaboration"
            body="Invite teammates, share dashboards, and manage incidents together with roles and simple access control."
            badge={{ label: 'In planning', className: 'border-amber-500/30 bg-amber-500/10 text-amber-600 dark:text-amber-400' }}
          />
          <FeatureCard
            title="Advanced alerting"
            body="Smarter rules (only alert on repeated failures), quiet hours, and more channels beyond email (Slack, webhooks)."
            badge={{ label: 'On the roadmap', className: 'border-sky-500/30 bg-sky-500/10 text-sky-600 dark:text-sky-400' }}
          />
          <FeatureCard
            title="Richer status pages"
            body="Custom branding, incident communication templates, and historical uptime charts you can embed anywhere."
            badge={{ label: 'Coming soon', className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' }}
          />
          <FeatureCard
            title="Deeper metrics"
            body="Percentiles (p95 / p99), error-rate views, and simple SLO reporting to track reliability over time."
            badge={{ label: 'Exploring', className: 'bg-muted text-muted-foreground border-border' }}
          />
          <FeatureCard
            title="More regions and checks"
            body="Run checks from multiple regions, and add DNS/TLS and port checks alongside HTTP monitors."
            badge={{ label: 'Future', className: 'bg-muted text-muted-foreground border-border' }}
          />
          <FeatureCard
            title="AI incident summaries"
            body="Let AI read your incident history and checks to generate clear summaries for your team or post-mortems."
            badge={{ label: 'Upcoming', className: 'border-violet-500/30 bg-violet-500/10 text-violet-600 dark:text-violet-400' }}
          />
        </div>
      </section>

      <section className="border-border/80 space-y-4 border-t pt-10">
        <h2 className="text-foreground text-xl font-semibold tracking-tight">Help shape the roadmap</h2>
        <p className="text-muted-foreground max-w-2xl text-sm leading-relaxed">
          APIWatch is still early and your feedback matters. If there is a feature you would love to see next, reach out
          and tell us how you monitor your APIs today.
        </p>
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <Link to="/contact" className={cn(buttonVariants({ size: 'default' }))}>
            Share a feature request
          </Link>
          <span className="text-muted-foreground">
            or email{' '}
            <a href="mailto:support@apiwatch.app" className="text-foreground font-medium hover:underline">
              support@apiwatch.app
            </a>
          </span>
        </div>
      </section>
    </div>
  )
}

function FeatureCard({
  title,
  body,
  badge,
}: {
  title: string
  body: string
  badge: { label: string; className: string }
}) {
  return (
    <Card className="border-border/80 flex h-full flex-col">
      <CardHeader className="pb-2">
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent className="text-muted-foreground flex flex-1 flex-col gap-4 text-sm leading-relaxed">
        <p className="flex-1">{body}</p>
        <Badge variant="outline" className={cn('w-fit font-medium', badge.className)}>
          {badge.label}
        </Badge>
      </CardContent>
    </Card>
  )
}
