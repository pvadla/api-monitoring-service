/**
 * Fixed canvas behind the app: deep navy + subtle grid + soft cyan vignette.
 */
export function AppBackdrop() {
  return (
    <div
      aria-hidden
      className="pointer-events-none fixed inset-0 -z-10 bg-background"
      style={{
        backgroundImage: `
          radial-gradient(ellipse 90% 60% at 50% -15%, rgb(56 189 248 / 14%), transparent 55%),
          linear-gradient(rgb(255 255 255 / 4%) 1px, transparent 1px),
          linear-gradient(90deg, rgb(255 255 255 / 4%) 1px, transparent 1px),
          radial-gradient(ellipse 80% 50% at 50% 100%, rgb(0 0 0 / 45%), transparent)
        `,
        backgroundSize: '100% 100%, 40px 40px, 40px 40px, 100% 100%',
        backgroundPosition: 'center, center, center, center',
      }}
    />
  )
}
