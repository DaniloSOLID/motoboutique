import { apiFetch } from './client'

export function cargarEstadoModulo<T>(modulo: string): Promise<T> {
  return apiFetch<T>(`/api/estado/${encodeURIComponent(modulo)}`)
}

export function guardarEstadoModulo(modulo: string, datos: unknown): Promise<void> {
  return apiFetch<void>(`/api/estado/${encodeURIComponent(modulo)}`, {
    method: 'PUT',
    json: datos,
  })
}
