import { apiFetch } from './client'

export interface RolAdministracion {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  sistema: boolean
  permisos: string[]
}

export interface PermisoAdministracion {
  id: number
  codigo: string
  descripcion: string
  modulo: string
}

export interface ConfiguracionEmpresa {
  razonSocial: string | null
  ruc: string | null
  moneda: string
  igv: number
  almacenDefectoId: number | null
  almacenDefecto: string | null
  stockMinimoDefault: number
  actualizadoEn: string
}

export interface AlmacenOpcion {
  id: number
  codigo: string
  nombre: string
  direccion: string | null
  activo: boolean
}

export interface AuditoriaEvento {
  id?: number
  quien: string
  que: string
  entidad: string
  entidadId: string | null
  detalle: string | null
  cuando: string
}

export interface Pagina<T> {
  contenido: T[]
  numero: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
}

export const administracionApi = {
  roles: () => apiFetch<RolAdministracion[]>('/api/roles'),
  permisos: () => apiFetch<PermisoAdministracion[]>('/api/permisos'),
  actualizarPermisos: (id: number, permisos: string[]) =>
    apiFetch<RolAdministracion>(`/api/roles/${id}/permisos`, {
      method: 'PUT',
      json: { permisos },
    }),
  configuracion: () => apiFetch<ConfiguracionEmpresa>('/api/configuracion'),
  actualizarConfiguracion: (datos: Omit<ConfiguracionEmpresa, 'almacenDefecto' | 'actualizadoEn'>) =>
    apiFetch<ConfiguracionEmpresa>('/api/configuracion', { method: 'PUT', json: datos }),
  almacenes: () => apiFetch<AlmacenOpcion[]>('/api/inventario/almacenes'),
  guardarAlmacen: (id: number | null, datos: Omit<AlmacenOpcion, 'id'>) =>
    apiFetch<AlmacenOpcion>(id === null ? '/api/inventario/almacenes' : `/api/inventario/almacenes/${id}`, {
      method: id === null ? 'POST' : 'PUT',
      json: datos,
    }),
  auditoria: (filtros: { q?: string; usuario?: string; accion?: string; desde?: string; hasta?: string; page: number; size: number }) => {
    const parametros = new URLSearchParams()
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined && valor !== '') parametros.set(clave, String(valor))
    })
    return apiFetch<Pagina<AuditoriaEvento>>(`/api/auditoria?${parametros.toString()}`)
  },
}
