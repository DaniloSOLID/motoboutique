import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { cerrarSesion, consultarSesion, iniciarSesion } from '../api/auth'
import { ApiError, hayTokenCsrf } from '../api/client'
import type { UsuarioSesion } from '../api/types'
import { useQueryClient } from '@tanstack/react-query'

interface AuthContextValue {
  /** Usuario con sesión iniciada, o null si no hay sesión. */
  usuario: UsuarioSesion | null
  /** true mientras se consulta si ya existe una sesión al abrir la aplicación. */
  cargando: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  tieneRol: (codigo: string) => boolean
  tienePermiso: (codigo: string) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null)
  const [cargando, setCargando] = useState(true)

  // Al abrir la aplicación se pregunta al backend si ya hay una sesión activa.
  // Aunque no la haya (401), la respuesta entrega la cookie del token CSRF para el login.
  useEffect(() => {
    let activo = true
    consultarSesion()
      .then((sesion) => activo && setUsuario(sesion))
      .catch((error) => {
        if (activo && !(error instanceof ApiError && error.status === 401)) {
          console.error('No se pudo consultar la sesión', error)
        }
      })
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [])

  useEffect(() => {
    const invalidarSesion = () => {
      setUsuario(null)
      queryClient.clear()
    }
    window.addEventListener('api:unauthorized', invalidarSesion)
    return () => window.removeEventListener('api:unauthorized', invalidarSesion)
  }, [queryClient])

  const login = useCallback(async (email: string, password: string) => {
    if (!hayTokenCsrf()) {
      await consultarSesion().catch(() => undefined)
    }
    await iniciarSesion(email.trim(), password)
    // Spring renueva el token CSRF al iniciar sesión; esta consulta lo recibe y trae los datos.
    setUsuario(await consultarSesion())
  }, [])

  const logout = useCallback(async () => {
    try {
      await cerrarSesion()
    } finally {
      setUsuario(null)
      queryClient.clear()
    }
  }, [queryClient])

  const valor = useMemo<AuthContextValue>(
    () => ({
      usuario,
      cargando,
      login,
      logout,
      tieneRol: (codigo) => usuario?.roles.includes(codigo) ?? false,
      tienePermiso: (codigo) => usuario?.permisos.includes(codigo) ?? false,
    }),
    [usuario, cargando, login, logout],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return contexto
}
