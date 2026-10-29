import { useLocation } from 'react-router-dom'
import { rutas } from '../app/routes'
import { EmptyState } from '../components/EmptyState'

export function PlaceholderPage() {
  const { pathname } = useLocation()
  const ruta = rutas.find((item) => item.path === pathname)
  return <EmptyState title={`${ruta?.label ?? 'Pantalla'} en preparación`}
    description="Esta pantalla aún no está conectada a la API; no se muestran datos de ejemplo." />
}
