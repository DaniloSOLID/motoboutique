import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function RequirePermiso({ permiso }: { permiso: string }) {
  const { tienePermiso } = useAuth()
  return tienePermiso(permiso) ? <Outlet /> : <Navigate to="/403" replace />
}
