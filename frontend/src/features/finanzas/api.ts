import { apiBlob, apiFetch } from '../../api/client'
import type { Pagina } from '../inventario/api'

export interface ValorizacionProducto {
  productoId: number
  codigo: string
  producto: string
  categoriaId: number
  categoria: string
  cantidad: number
  costoPromedio: number
  valor: number
}

export interface ValorizacionCategoria {
  categoriaId: number
  categoria: string
  cantidad: number
  valor: number
}

export interface Valorizacion {
  fechaCorte: string
  cantidadTotal: number
  valorTotal: number
  productos: Pagina<ValorizacionProducto>
  categorias: ValorizacionCategoria[]
}

export interface KardexLinea {
  movimientoId: number
  fecha: string
  tipo: string
  numero: string
  entrada: number
  salida: number
  saldo: number
  costoUnitario: number | null
  costoPromedio: number | null
  documentoReferencia: string | null
  motivo: string | null
}

export interface Kardex {
  productoId: number
  producto: string
  almacenId: number
  almacen: string
  saldoInicial: number
  movimientos: KardexLinea[]
}

export interface Reporte {
  tipo: string
  razonSocial: string
  ruc: string
  generadoEn: string
  columnas: string[]
  datos: Pagina<string[]>
  totales: Record<string, string>
}

export type FiltrosFinanzas = Record<string, string | number | undefined>

function consulta(filtros: FiltrosFinanzas): string {
  const parametros = new URLSearchParams()
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== '') parametros.set(clave, String(valor))
  })
  return parametros.toString()
}

export const finanzasApi = {
  valorizacion: (filtros: FiltrosFinanzas) =>
    apiFetch<Valorizacion>(`/api/finanzas/valorizacion?${consulta(filtros)}`),
  kardex: (filtros: FiltrosFinanzas & { productoId: number; almacenId: number }) =>
    apiFetch<Kardex>(`/api/inventario/kardex?${consulta(filtros)}`),
  reporte: (tipo: string, filtros: FiltrosFinanzas) =>
    apiFetch<Reporte>(`/api/reportes/${encodeURIComponent(tipo)}?${consulta(filtros)}`),
  exportar: (tipo: string, filtros: FiltrosFinanzas, formato: 'csv' | 'xlsx' | 'pdf') => {
    const parametros = new URLSearchParams(consulta(filtros))
    parametros.set('formato', formato)
    return apiBlob(`/api/reportes/${encodeURIComponent(tipo)}?${parametros.toString()}`)
  },
}

export function descargarArchivo(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = nombre
  enlace.click()
  URL.revokeObjectURL(url)
}

export function instanteDesdeFecha(fecha: string, finDelDia = false): string | undefined {
  if (!fecha) return undefined
  const hora = finDelDia ? '23:59:59.999' : '00:00:00'
  return new Date(`${fecha}T${hora}-05:00`).toISOString()
}

export function promedioPonderado(valor: number, unidades: number): number {
  return unidades === 0 ? 0 : valor / unidades
}
