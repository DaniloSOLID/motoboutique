import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

/** Protege las rutas internas: sin sesión, redirige al login y vuelve después de entrar. */
export default function RequireAuth() {
  const { usuario, cargando } = useAuth()
  const ubicacion = useLocation()

  if (cargando) {
    return (
      <main className="flex min-h-screen items-center justify-center text-sm text-[#57534e]">
        Cargando…
      </main>
    )
  }
  if (!usuario) {
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname + ubicacion.search }} />
  }
  return <Outlet />
}
