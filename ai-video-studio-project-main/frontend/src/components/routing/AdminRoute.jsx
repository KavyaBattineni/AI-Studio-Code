import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import LoadingSpinner from '../common/LoadingSpinner'

export default function AdminRoute() {
  const { isAuthenticated, isAdmin, isLoading } = useAuth()

  if (isLoading) {
    return <LoadingSpinner center size="lg" />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  // Client-side gating is a UX nicety only - every /api/admin/** call is independently
  // enforced server-side by SecurityConfig regardless of what the frontend does here.
  if (!isAdmin) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
