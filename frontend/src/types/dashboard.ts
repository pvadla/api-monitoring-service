import type { UserDto } from '@/types/user'

export type EndpointRow = {
  id: number
  name: string
  url: string
  checkInterval: number
  isActive: boolean
  isUp: boolean
  failureCount: number | null
  lastChecked: string | null
  showOnStatusPage: boolean
  expectedBodySubstring: string | null
  sslExpiresAt: string | null
  /** Last 15 checks, oldest → newest (API may pad leading slots with null). */
  recentChecksUp?: (boolean | null)[] | null
}

export type HeartbeatRow = {
  id: number
  name: string
  token: string
  expectedIntervalMinutes: number
  lastPingAt: string | null
  isActive: boolean | null
  /** null = pending (never pinged, not yet overdue); true = up; false = down/missed */
  isUp: boolean | null
  /** Last 15 scheduler evaluations, oldest → newest. null = slot not filled yet. */
  recentChecksUp?: (boolean | null)[] | null
}

/** Dashboard monitor filter: HTTP checks vs inbound heartbeat pings. */
export type MonitorTypeFilter = 'all' | 'endpoint' | 'heartbeat'

export type DashboardPayload = {
  user: UserDto
  endpointCount: number
  upCount: number
  downCount: number
  endpoints: EndpointRow[]
  heartbeats: HeartbeatRow[]
  baseUrl: string
  flashSuccess: string | null
  openIncidentCount: number
}
