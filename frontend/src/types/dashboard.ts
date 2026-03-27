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

export type SslMonitorRow = {
  id: number
  name: string
  /** Hostname only, e.g. "example.com" */
  domain: string
  port: number
  alertDaysThreshold: number
  isActive: boolean | null
  /**
   * null  = never checked.
   * true  = cert healthy (daysLeft > alertDaysThreshold).
   * false = cert expired, TLS error, or expiring within threshold.
   */
  isUp: boolean | null
  /** ISO cert expiry timestamp, or null if never checked successfully. */
  sslExpiresAt: string | null
  /** ISO last-check timestamp, or null if never checked. */
  lastCheckedAt: string | null
  /** Last 15 checks, oldest → newest. null = slot not filled yet. */
  recentChecksUp?: (boolean | null)[] | null
}

/** Dashboard monitor filter */
export type MonitorTypeFilter = 'all' | 'endpoint' | 'heartbeat' | 'ssl'

export type DashboardPayload = {
  user: UserDto
  endpointCount: number
  upCount: number
  downCount: number
  endpoints: EndpointRow[]
  heartbeats: HeartbeatRow[]
  sslMonitors: SslMonitorRow[]
  baseUrl: string
  flashSuccess: string | null
  openIncidentCount: number
}
