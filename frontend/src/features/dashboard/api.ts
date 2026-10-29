import { apiFetch } from '../../api/client'

export interface DashboardData {
  rol: string
  kpis: Record<string, number | string>
  serie: Array<{ periodo: string; entradas: number; salidas: number; compras: number }>
  criticos: Array<{ productoId: number; codigo: string; producto: string; categoria: string; cantidad: number; minimo: number; estado: string }>
  actividad: Array<{ actor: string; accion: string; entidad: string; entidadId: string | null; fecha: string }>
}

export const dashboardApi = {
  obtener: (rol: string, dias = 7) =>
    apiFetch<DashboardData>(`/api/dashboard/${rol}?dias=${dias}`),
}
