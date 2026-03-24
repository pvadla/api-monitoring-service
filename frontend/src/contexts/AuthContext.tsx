import { useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  type ReactNode,
} from 'react'
import { useNavigate } from 'react-router-dom'

import type { UserDto } from '@/types/user'

type AuthState = {
  user: UserDto | null
  loading: boolean
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

async function fetchMe(): Promise<UserDto | null> {
  const res = await fetch('/api/me', { credentials: 'include', headers: { Accept: 'application/json' } })
  if (res.status === 401) return null
  if (!res.ok) throw new Error('Failed to load session')
  return res.json() as Promise<UserDto>
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: user, isPending: loading } = useQuery({
    queryKey: ['me'],
    queryFn: fetchMe,
    staleTime: 60_000,
  })

  const refresh = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: ['me'] })
  }, [queryClient])

  useEffect(() => {
    const onSessionExpired = () => {
      const current = queryClient.getQueryData<UserDto | null>(['me'])
      if (current) {
        queryClient.setQueryData(['me'], null)
        navigate('/', { replace: true })
      }
    }
    window.addEventListener('api:unauthorized', onSessionExpired)
    return () => window.removeEventListener('api:unauthorized', onSessionExpired)
  }, [navigate, queryClient])

  const value = useMemo(
    () => ({
      user: user ?? null,
      loading,
      refresh,
    }),
    [user, loading, refresh],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
