import { useEffect, useRef, useState } from 'react'
import { cargarEstadoModulo, guardarEstadoModulo } from '../api/persistencia'

export function useEstadoPersistente<T>(modulo: string, inicial: T) {
  const [datos, setDatos] = useState<T>(inicial)
  const [cargado, setCargado] = useState(false)
  const inicialRef = useRef(inicial)

  useEffect(() => {
    let activo = true
    setCargado(false)
    cargarEstadoModulo<T>(modulo)
      .then((guardado) => {
        if (activo && guardado && typeof guardado === 'object') setDatos(guardado)
      })
      .catch((error: unknown) => {
        console.error(`No se pudo cargar ${modulo}`, error)
      })
      .finally(() => activo && setCargado(true))
    return () => { activo = false }
  }, [modulo])

  useEffect(() => {
    if (!cargado || Object.is(datos, inicialRef.current)) return
    const timer = window.setTimeout(() => {
      void guardarEstadoModulo(modulo, datos).catch((error: unknown) => {
        console.error(`No se pudo guardar ${modulo}`, error)
      })
    }, 350)
    return () => window.clearTimeout(timer)
  }, [cargado, datos, modulo])

  return [datos, setDatos, cargado] as const
}
