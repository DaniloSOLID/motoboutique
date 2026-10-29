import { apiFetch } from './client'

export interface Proveedor {
  id: number
  codigo: string
  razonSocial: string
  ruc: string | null
  telefono: string
  email: string
  activo: boolean
}

export interface OrdenCompra {
  id: number
  numero: string
  estado: string
  proveedor: string
  proveedorId: number
  rucProveedor: string | null
  almacenId: number
  almacen: string
  fecha: string
  observaciones: string | null
  subtotal: number
  igv: number
  total: number
  detalles: OrdenCompraDetalle[]
}

export interface OrdenCompraDetalle {
  id: number
  productoId: number
  productoCodigo: string
  productoNombre: string
  cantidad: number
  cantidadRecibida: number
  cantidadPendiente: number
  costoUnitario: number
  subtotal: number
}

export interface RecepcionCompra {
  id: number
  numero: string
  ordenId: number
  orden: string
  proveedor: string
  fecha: string
  observaciones: string | null
  responsable: string
  detalles: Array<{
    detalleOrdenId: number
    productoId: number
    productoCodigo: string
    productoNombre: string
    cantidad: number
  }>
}

export interface PageResponse<T> {
  contenido: T[]
  numero: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
}

export interface ProveedorGuardar {
  razonSocial: string
  ruc: string
  telefono?: string | null
  email?: string | null
  direccion?: string | null
  activo?: boolean
}

export interface OrdenCompraDetalleGuardar {
  productoId: number
  cantidad: number
  costoUnitario: number
}

export interface OrdenCompraGuardar {
  proveedorId: number
  almacenId?: number | null
  detalles: OrdenCompraDetalleGuardar[]
  observaciones?: string | null
}

export interface RecepcionDetalleGuardar {
  detalleOrdenId: number
  cantidad: number
}

export interface RecepcionGuardar {
  ordenId: number
  detalles: RecepcionDetalleGuardar[]
  observaciones?: string | null
}

export const comprasApi = {
  paginaProveedores: (parametros = new URLSearchParams()) => {
    const consulta = new URLSearchParams(parametros)
    if (!consulta.has('size')) consulta.set('size', '20')
    return apiFetch<PageResponse<Proveedor>>(`/api/compras/proveedores?${consulta.toString()}`)
  },
  proveedores: async (filtros = '') => (await apiFetch<PageResponse<Proveedor>>(
    `/api/compras/proveedores?size=100${filtros ? `&${filtros}` : ''}`,
  )).contenido,
  crearProveedor: (proveedor: ProveedorGuardar) => apiFetch<Proveedor>(
    '/api/compras/proveedores', { method: 'POST', json: proveedor },
  ),
  editarProveedor: (id: number, proveedor: ProveedorGuardar) => apiFetch<Proveedor>(
    `/api/compras/proveedores/${id}`, { method: 'PUT', json: proveedor },
  ),
  eliminarProveedor: (id: number) => apiFetch<void>(
    `/api/compras/proveedores/${id}`, { method: 'DELETE' },
  ),
  paginaOrdenes: (parametros = new URLSearchParams()) => {
    const consulta = new URLSearchParams(parametros)
    if (!consulta.has('size')) consulta.set('size', '20')
    return apiFetch<PageResponse<OrdenCompra>>(`/api/compras/ordenes?${consulta.toString()}`)
  },
  ordenes: async (filtros = '') => (await apiFetch<PageResponse<OrdenCompra>>(
    `/api/compras/ordenes?size=100${filtros ? `&${filtros}` : ''}`,
  )).contenido,
  orden: (id: number) => apiFetch<OrdenCompra>(`/api/compras/ordenes/${id}`),
  crearOrden: (orden: OrdenCompraGuardar) => apiFetch<OrdenCompra>(
    '/api/compras/ordenes', { method: 'POST', json: orden },
  ),
  editarOrden: (id: number, orden: OrdenCompraGuardar) => apiFetch<OrdenCompra>(
    `/api/compras/ordenes/${id}`, { method: 'PUT', json: orden },
  ),
  confirmarOrden: (id: number) => apiFetch<OrdenCompra>(
    `/api/compras/ordenes/${id}/confirmar`, { method: 'POST' },
  ),
  anularOrden: (id: number) => apiFetch<OrdenCompra>(
    `/api/compras/ordenes/${id}/anular`, { method: 'POST' },
  ),
  paginaRecepciones: (parametros = new URLSearchParams()) => {
    const consulta = new URLSearchParams(parametros)
    if (!consulta.has('size')) consulta.set('size', '20')
    return apiFetch<PageResponse<RecepcionCompra>>(`/api/compras/recepciones?${consulta.toString()}`)
  },
  recepciones: async (filtros = '') => (await apiFetch<PageResponse<RecepcionCompra>>(
    `/api/compras/recepciones?size=100${filtros ? `&${filtros}` : ''}`,
  )).contenido,
  recibirOrden: (recepcion: RecepcionGuardar) => apiFetch<RecepcionCompra>(
    '/api/compras/recepciones', { method: 'POST', json: recepcion },
  ),
}
