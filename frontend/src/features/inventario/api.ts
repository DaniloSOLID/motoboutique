import { apiBlob, apiFetch } from '../../api/client'

export interface Pagina<T> {
  contenido: T[]
  numero: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
}

export interface Almacen {
  id: number
  codigo: string
  nombre: string
  direccion: string | null
  activo: boolean
}

export interface StockFila {
  productoId: number
  codigo: string
  nombre: string
  categoria: string
  cantidadTotal: number
  stockMinimo: number
  estado: string
  almacenId: number
  almacen: string
  cantidad: number
  reservado: number
}

export interface Movimiento {
  id: number
  numero: string
  fecha: string
  tipo: string
  productoId: number
  productoCodigo: string
  productoNombre: string
  almacenId: number
  almacen: string
  cantidad: number
  costoUnitario: number | null
  saldoCantidad: number
  costoPromedio: number | null
  referencia: string | null
  motivo: string | null
  responsable: string
}

export interface Transferencia {
  id: number
  numero: string
  estado: string
  fecha: string
  almacenOrigenId: number
  almacenOrigen: string
  almacenDestinoId: number
  almacenDestino: string
  observaciones: string | null
  detalles: Array<{ productoId: number; codigo: string; producto: string; cantidad: number }>
}

export interface Conteo {
  id: number
  numero: string
  almacenId: number
  almacen: string
  estado: string
  fechaApertura: string
  fechaCierre: string | null
  detalles: Array<{
    id: number
    productoId: number
    codigo: string
    producto: string
    stockSistema: number
    stockContado: number
    diferencia: number
    ajustePendiente: boolean
  }>
}

export interface Ajuste {
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
}

function query(filtros: Record<string, string | number | boolean | undefined>): string {
  const params = new URLSearchParams()
  Object.entries(filtros).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  return params.toString()
}

export const inventarioApi = {
  almacenes: () => apiFetch<Almacen[]>('/api/inventario/almacenes'),
  stock: (filtros: Record<string, string | number | boolean | undefined>) =>
    apiFetch<Pagina<StockFila>>(`/api/inventario/stock?${query(filtros)}`),
  movimientos: (filtros: Record<string, string | number | boolean | undefined>) =>
    apiFetch<Pagina<Movimiento>>(`/api/inventario/movimientos?${query(filtros)}`),
  exportarMovimientos: (filtros: Record<string, string | number | boolean | undefined>) =>
    apiBlob(`/api/inventario/movimientos/exportar?${query(filtros)}`),
  entrada: (datos: { productoId: number; almacenId: number; cantidad: number; costoUnitario: number; documentoReferencia?: string; observaciones?: string }) =>
    apiFetch<Movimiento>('/api/inventario/entradas', { method: 'POST', json: datos }),
  salida: (datos: { productoId: number; almacenId: number; cantidad: number; motivo: string; documentoReferencia?: string; observaciones?: string }) =>
    apiFetch<Movimiento>('/api/inventario/salidas', { method: 'POST', json: datos }),
  transferencias: (filtros: Record<string, string | number | boolean | undefined>) =>
    apiFetch<Pagina<Transferencia>>(`/api/inventario/transferencias?${query(filtros)}`),
  crearTransferencia: (datos: { almacenOrigenId: number; almacenDestinoId: number; detalles: Array<{ productoId: number; cantidad: number }>; observaciones?: string }) =>
    apiFetch<Transferencia>('/api/inventario/transferencias', { method: 'POST', json: datos }),
  despachar: (id: number) => apiFetch<Transferencia>(`/api/inventario/transferencias/${id}/despachar`, { method: 'POST' }),
  recibir: (id: number) => apiFetch<Transferencia>(`/api/inventario/transferencias/${id}/recibir`, { method: 'POST' }),
  cancelar: (id: number) => apiFetch<Transferencia>(`/api/inventario/transferencias/${id}/cancelar`, { method: 'POST' }),
  conteos: (filtros: Record<string, string | number | boolean | undefined>) =>
    apiFetch<Pagina<Conteo>>(`/api/inventario/conteos?${query(filtros)}`),
  abrirConteo: (almacenId: number) => apiFetch<Conteo>('/api/inventario/conteos', { method: 'POST', json: { almacenId } }),
  conteo: (id: number) => apiFetch<Conteo>(`/api/inventario/conteos/${id}`),
  guardarConteo: (id: number, productoId: number, cantidad: number) =>
    apiFetch<Conteo>(`/api/inventario/conteos/${id}/detalles/${productoId}`, { method: 'PUT', json: { cantidad } }),
  cerrarConteo: (id: number) => apiFetch<Conteo>(`/api/inventario/conteos/${id}/cerrar`, { method: 'POST' }),
  ajustes: (filtros: Record<string, string | number | boolean | undefined>) =>
    apiFetch<Pagina<Ajuste>>(`/api/inventario/ajustes?${query(filtros)}`),
  solicitarAjuste: (datos: { productoId: number; almacenId: number; cantidad: number; motivo: string }) =>
    apiFetch<Ajuste>('/api/inventario/ajustes', { method: 'POST', json: datos }),
}

export function descargar(blob: Blob, nombre: string) {
  const enlace = document.createElement('a')
  enlace.href = URL.createObjectURL(blob)
  enlace.download = nombre
  enlace.click()
  URL.revokeObjectURL(enlace.href)
}
