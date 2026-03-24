import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import { AppRoutes } from '@/app/AppRoutes.tsx'
import { ErrorBoundary } from '@/components/ErrorBoundary.tsx'
import { AuthProvider } from '@/contexts/AuthContext.tsx'
import { QueryProvider } from '@/providers/QueryProvider.tsx'

import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <BrowserRouter>
        <QueryProvider>
          <AuthProvider>
            <AppRoutes />
          </AuthProvider>
        </QueryProvider>
      </BrowserRouter>
    </ErrorBoundary>
  </StrictMode>,
)
