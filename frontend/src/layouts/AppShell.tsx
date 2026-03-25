import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'

import { AppBackdrop } from '@/components/AppBackdrop.tsx'
import { BrandWordmark } from '@/components/BrandWordmark.tsx'
import { buttonVariants } from '@/components/ui/button'
import { useAuth } from '@/contexts/AuthContext'
import { HOME_SECTION_NAV } from '@/lib/home-nav.ts'
import { cn } from '@/lib/utils'

function navClass(isActive: boolean) {
  return cn(
    'text-sm font-medium transition-colors',
    isActive ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
  )
}

function sectionLinkClass() {
  return cn('text-sm font-medium text-muted-foreground transition-colors hover:text-foreground')
}

async function postLogout(refresh: () => Promise<void>) {
  await fetch('/logout', {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'text/html,application/json' },
  })
  await refresh()
}

export function AppShell() {
  const { user, loading, refresh } = useAuth()
  const { pathname } = useLocation()
  const isHome = pathname === '/'

  return (
    <div className="relative flex min-h-dvh w-full max-w-[100vw] flex-col overflow-x-hidden">
      <AppBackdrop />
      <div className="relative z-10 flex min-h-dvh w-full min-w-0 flex-1 flex-col">
        <header className="border-b border-white/10 bg-background/80 backdrop-blur-md supports-[backdrop-filter]:bg-background/60 sticky top-0 z-50 shrink-0">
          <div className="flex w-full min-w-0 flex-wrap items-center gap-x-4 gap-y-3 px-4 py-3 sm:px-5 lg:px-6">
            <div className="shrink-0">
              <BrandWordmark asLink showIcon />
            </div>
            <nav className="flex min-w-0 flex-1 flex-wrap items-center justify-center gap-x-3 gap-y-2 sm:gap-x-4 lg:gap-x-5">
              {isHome ? (
                <>
                  {HOME_SECTION_NAV.map(({ href, label }) =>
                    href.startsWith('#') ? (
                      <a key={href} href={href} className={sectionLinkClass()}>
                        {label}
                      </a>
                    ) : (
                      <Link key={href} to={href} className={sectionLinkClass()}>
                        {label}
                      </Link>
                    ),
                  )}
                </>
              ) : (
                <>
                  <NavLink to="/" end className={({ isActive }) => navClass(isActive)}>
                    Home
                  </NavLink>
                  <NavLink to="/about" className={({ isActive }) => navClass(isActive)}>
                    About
                  </NavLink>
                  <NavLink to="/contact" className={({ isActive }) => navClass(isActive)}>
                    Contact
                  </NavLink>
                  <NavLink to="/dashboard" className={({ isActive }) => navClass(isActive)}>
                    Dashboard
                  </NavLink>
                  <NavLink to="/incidents" className={({ isActive }) => navClass(isActive)}>
                    Incidents
                  </NavLink>
                  <NavLink to="/settings/profile" className={({ isActive }) => navClass(isActive)}>
                    Profile
                  </NavLink>
                  <NavLink to="/settings/status" className={({ isActive }) => navClass(isActive)}>
                    Status page
                  </NavLink>
                  {user?.statusSlug ? (
                    <NavLink
                      to={`/status/${user.statusSlug}`}
                      className={({ isActive }) => navClass(isActive)}
                    >
                      Public status
                    </NavLink>
                  ) : null}
                </>
              )}
            </nav>
            <div className="flex shrink-0 items-center gap-3">
              {loading ? (
                <span className="text-muted-foreground text-xs">…</span>
              ) : user ? (
                <>
                  <span className="text-muted-foreground hidden text-sm sm:inline" title={user.email}>
                    {user.name ?? user.email}
                  </span>
                  <span
                    className="border-border bg-muted/50 hidden rounded-full border px-2 py-0.5 text-xs font-medium sm:inline"
                    title="Subscription tier"
                  >
                    Plan: {user.subscriptionTier}
                  </span>
                  <button
                    type="button"
                    className={cn(buttonVariants({ variant: 'outline', size: 'sm' }))}
                    onClick={() => void postLogout(refresh)}
                  >
                    Log out
                  </button>
                </>
              ) : isHome ? (
                <a
                  href="/oauth2/authorization/google"
                  className={cn(buttonVariants({ size: 'sm' }), 'gap-2')}
                >
                  Sign in with Google
                </a>
              ) : (
                <Link to="/login" className={cn(buttonVariants({ size: 'sm' }))}>
                  Sign in
                </Link>
              )}
            </div>
          </div>
        </header>
        <main className="w-full min-w-0 flex-1 px-4 py-6 sm:px-5 lg:px-6 lg:py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
