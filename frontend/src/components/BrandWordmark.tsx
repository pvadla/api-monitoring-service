import { Shield } from 'lucide-react'
import { Link } from 'react-router-dom'

import { cn } from '@/lib/utils'

type Props = {
  className?: string
  /** Show icon in a tinted square (matches reference sign-in card). */
  showIcon?: boolean
  /** Wrap logo in a Link to home. */
  asLink?: boolean
}

export function BrandWordmark({ className, showIcon = true, asLink = false }: Props) {
  const inner = (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      {showIcon ? (
        <span className="bg-primary/15 text-primary flex size-9 shrink-0 items-center justify-center rounded-lg">
          <Shield className="size-[1.15rem]" strokeWidth={2.25} aria-hidden />
        </span>
      ) : null}
      <span className="text-lg font-bold tracking-tight">
        <span className="text-foreground">API</span>
        <span className="text-primary">Watch</span>
      </span>
    </span>
  )

  if (asLink) {
    return (
      <Link to="/" className="ring-offset-background rounded-md outline-none focus-visible:ring-2 focus-visible:ring-ring">
        {inner}
      </Link>
    )
  }
  return inner
}
