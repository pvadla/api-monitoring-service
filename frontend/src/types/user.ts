/** Mirrors {@code UserResponse} from Spring {@code GET /api/me}. */
export type UserDto = {
  id: number
  email: string
  name: string | null
  picture: string | null
  subscriptionTier: string
  statusSlug: string | null
  statusPageTitle: string | null
  statusPageLogoUrl: string | null
  notifyOnEndpointDown: boolean | null
  notifyOnEndpointRecovery: boolean | null
}
