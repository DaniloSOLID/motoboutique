import { apiBlob, apiFetch, apiUpload } from '../../api/client'

export interface ImportacionError {
  fila: number
  columna: string
  mensaje: string
}

export interface ImportacionResultado {
  archivo: string
  columnas: string[]
  vistaPrevia: Array<Record<string, string>>
  filasValidas: Array<Record<string, string>>
  errores: ImportacionError[]
  importacionId: number | null
}

export interface ImportacionHistorial {
  id: number
  archivo: string
  fecha: string
  filasValidas: number
  errores: number
  estado: string
  usuarioId: number
}

function formulario(archivo: File, columnas?: Record<string, string>): FormData {
  const form = new FormData()
  form.append('archivo', archivo)
  if (columnas) form.append('mapeo', new Blob([JSON.stringify({ columnas })], { type: 'application/json' }))
  return form
}

export const importacionesApi = {
  detectar: (archivo: File) => apiUpload<ImportacionResultado>('/api/catalogo/importaciones', formulario(archivo)),
  validar: (archivo: File, columnas: Record<string, string>) =>
    apiUpload<ImportacionResultado>('/api/catalogo/importaciones/validar', formulario(archivo, columnas)),
  confirmar: (archivo: File, columnas: Record<string, string>) =>
    apiUpload<ImportacionResultado>('/api/catalogo/importaciones/confirmar', formulario(archivo, columnas)),
  historial: () => apiFetch<ImportacionHistorial[]>('/api/catalogo/importaciones'),
  plantilla: () => apiBlob('/api/catalogo/importaciones/plantilla'),
}
