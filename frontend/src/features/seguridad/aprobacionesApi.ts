import { apiFetch } from '../../api/client'
import type { Pagina } from '../../api/administracion'

export interface AjusteAprobacion {
  id: number
  numero: string
  productoId: number
  codigoProducto: string
  producto: string
  almacenId: number
  almacen: string
  cantidad: number
  motivo: string
  origen: string
  estado: string
  solicitanteId: number
  solicitante: string
  aprobador: string | null
  comentario: string | null
  fechaSolicitud: string
  fechaResolucion: string | null
  movimientoId: number | null
  stockActual: number
  stockResultante: number
}

export const aprobacionesApi = {
  listar: (filtros: { estado?: string; almacenId?: string; productoId?: string; desde?: string; hasta?: string; solicitanteId?: string; page: number; size: number }) => {
    const parametros = new URLSearchParams()
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== '') parametros.set(clave, String(valor))
    })
    return apiFetch<Pagina<AjusteAprobacion>>(`/api/aprobaciones?${parametros.toString()}`)
  },
  resolver: (id: number, accion: 'aprobar' | 'rechazar', comentario: string) =>
    apiFetch<AjusteAprobacion>(`/api/aprobaciones/${id}/${accion}`, {
      method: 'POST', json: { comentario },
    }),
  resolverLote: (ids: number[], accion: 'APROBAR' | 'RECHAZAR', comentario: string) =>
    apiFetch<AjusteAprobacion[]>('/api/aprobaciones/lote', {
      method: 'POST', json: { ids, accion, comentario },
    }),
}
