/** Format API ISO timestamps (e.g. LocalDateTime) for display. */
export function formatShortDate(iso: string | null | undefined): string {
  if (iso == null || iso === '') return 'Not yet'
  const d = new Date(iso.includes('T') ? iso : `${iso.replace(' ', 'T')}`)
  if (Number.isNaN(d.getTime())) return iso
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(d)
}
