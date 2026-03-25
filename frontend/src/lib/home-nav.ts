/** Home header nav when pathname is `/`. Hash links stay on the landing page; About & Contact use SPA routes. */
export const HOME_SECTION_NAV = [
  { href: '#how-it-works', label: 'How It Works' },
  { href: '#pricing', label: 'Pricing' },
  { href: '/about', label: 'About' },
  { href: '/contact', label: 'Contact' },
] as const
