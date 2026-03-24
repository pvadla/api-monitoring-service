import { Route, Routes } from 'react-router-dom'

import { ProtectedRoute } from '@/components/ProtectedRoute.tsx'
import { AppShell } from '@/layouts/AppShell.tsx'
import { AboutPage } from '@/pages/AboutPage.tsx'
import { ContactPage } from '@/pages/ContactPage.tsx'
import { DashboardPage } from '@/pages/DashboardPage.tsx'
import { EndpointDetailPage } from '@/pages/EndpointDetailPage.tsx'
import { HomePage } from '@/pages/HomePage.tsx'
import { IncidentsPage } from '@/pages/IncidentsPage.tsx'
import { LoginPage } from '@/pages/LoginPage.tsx'
import { ProfilePage } from '@/pages/ProfilePage.tsx'
import { PublicStatusPage } from '@/pages/PublicStatusPage.tsx'
import { StatusSettingsPage } from '@/pages/StatusSettingsPage.tsx'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<HomePage />} />
        <Route path="about" element={<AboutPage />} />
        <Route path="contact" element={<ContactPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="status/:slug" element={<PublicStatusPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="endpoints/:id" element={<EndpointDetailPage />} />
          <Route path="incidents" element={<IncidentsPage />} />
          <Route path="settings/profile" element={<ProfilePage />} />
          <Route path="settings/status" element={<StatusSettingsPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
