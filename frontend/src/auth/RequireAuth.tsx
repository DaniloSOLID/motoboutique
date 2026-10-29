import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { Skeleton } from '../components/Skeleton'

export function RequireAuth() {
  const { usuario, cargando } = useAuth()
  const location = useLocation()
  if (cargando) return <Skeleton />
  return usuario ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />
}
