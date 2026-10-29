import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from './api'

export function useDashboard(rol: string, dias: number) {
  return useQuery({
    queryKey: ['dashboard', rol, dias],
    queryFn: () => dashboardApi.obtener(rol, dias),
  })
}
