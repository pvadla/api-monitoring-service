import { Eye, Lock, Mail } from 'lucide-react'
import { Navigate, Link } from 'react-router-dom'

import { BrandWordmark } from '@/components/BrandWordmark.tsx'
import { buttonVariants } from '@/components/ui/button.tsx'
import { Card, CardContent, CardFooter, CardHeader } from '@/components/ui/card.tsx'
import { Input } from '@/components/ui/input.tsx'
import { Label } from '@/components/ui/label.tsx'
import { useAuth } from '@/contexts/AuthContext.tsx'
import { cn } from '@/lib/utils.ts'

export function LoginPage() {
  const { user, loading } = useAuth()

  if (!loading && user) {
    return <Navigate to="/dashboard" replace />
  }

  return (
    <div className="flex min-h-[calc(100dvh-8rem)] w-full flex-col items-center justify-center px-4 py-10">
      <Card className="border-white/10 bg-card/95 w-full max-w-lg rounded-3xl shadow-2xl shadow-black/40 backdrop-blur-sm">
        <CardHeader className="space-y-6 pb-2">
          <div className="flex justify-center">
            <BrandWordmark showIcon />
          </div>
          <div className="text-muted-foreground flex items-center justify-center gap-2 text-center text-xs">
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-40" />
              <span className="relative inline-flex size-2 rounded-full bg-emerald-500" />
            </span>
            <span>All systems operational · 99.98% uptime</span>
          </div>
          <div className="border-border space-y-1 border-t pt-4 text-center">
            <h1 className="text-foreground text-2xl font-semibold tracking-tight">Welcome back</h1>
            <p className="text-muted-foreground text-sm">Sign in to monitor your APIs</p>
          </div>
        </CardHeader>

        <CardContent className="space-y-5 pt-2">
          <p className="text-muted-foreground text-center text-xs">
            Sign-in is via Google only. Fields below are for layout preview.
          </p>

          <div className="space-y-2">
            <Label htmlFor="login-email">Email</Label>
            <div className="relative">
              <Mail className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
              <Input
                id="login-email"
                type="email"
                placeholder="you@company.com"
                className="bg-background/50 pl-10"
                disabled
                autoComplete="off"
                aria-disabled
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="login-password">Password</Label>
            <div className="relative">
              <Lock className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2" />
              <Input
                id="login-password"
                type="password"
                placeholder="••••••••"
                className="bg-background/50 pr-10 pl-10"
                disabled
                autoComplete="off"
                aria-disabled
              />
              <button
                type="button"
                className="text-muted-foreground hover:text-foreground absolute top-1/2 right-3 -translate-y-1/2"
                disabled
                aria-hidden
                tabIndex={-1}
              >
                <Eye className="size-4" />
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 opacity-60">
              <input
                id="remember"
                type="checkbox"
                disabled
                className="border-input bg-background accent-primary size-4 rounded border"
              />
              <Label htmlFor="remember" className="text-muted-foreground text-sm font-normal">
                Remember me
              </Label>
            </div>
            <Link
              to="/contact"
              className="text-primary text-sm font-medium hover:underline"
            >
              Forgot password?
            </Link>
          </div>

          <div className="relative py-2">
            <div className="border-border absolute inset-0 flex items-center">
              <span className="bg-card w-full border-t" />
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="text-muted-foreground bg-card px-3">or continue with</span>
            </div>
          </div>

          <a
            href="/oauth2/authorization/google"
            className={cn(
              buttonVariants({ variant: 'outline', size: 'lg' }),
              'bg-background/60 text-foreground hover:bg-background/80 w-full gap-2 border-white/15 font-semibold',
            )}
          >
            <svg className="size-5 shrink-0" viewBox="0 0 24 24" aria-hidden>
              <path
                fill="#4285F4"
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
              />
              <path
                fill="#34A853"
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
              />
              <path
                fill="#FBBC05"
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
              />
              <path
                fill="#EA4335"
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
              />
            </svg>
            Sign in with Google
          </a>
        </CardContent>

        <CardFooter className="text-muted-foreground flex flex-col gap-2 border-t border-white/10 pt-6 text-center text-sm">
          <p>
            Don&apos;t have an account?{' '}
            <Link to="/contact" className="text-primary font-medium hover:underline">
              Start free trial
            </Link>
          </p>
        </CardFooter>
      </Card>
    </div>
  )
}
