export function AboutPage() {
  return (
    <div className="w-full space-y-2">
      <h1 className="text-foreground text-2xl font-semibold tracking-tight">About</h1>
      <p className="text-muted-foreground w-full text-sm leading-relaxed">
        Public page (Thymeleaf equivalent:{' '}
        <code className="bg-muted text-foreground rounded px-1.5 py-0.5 text-xs">/about</code>). Content will be migrated
        from the legacy template in a later phase.
      </p>
    </div>
  )
}
