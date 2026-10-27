import { useState, type FormEvent } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

/**
 * Pantalla de acceso. El backend original no tenía una propia (usaba la página generada por
 * Spring Security), así que se mantienen los mismos campos (correo y contraseña) con el estilo
 * de la página de error.
 */
export default function LoginPage() {
  const { usuario, cargando, login } = useAuth()
  const ubicacion = useLocation()
  const destino = (ubicacion.state as { desde?: string } | null)?.desde ?? '/'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  if (!cargando && usuario) return <Navigate to={destino} replace />

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await login(email, password)
    } catch (e) {
      setError(e instanceof ApiError ? e.detalle : 'No se pudo iniciar sesión.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <main className="w-full max-w-md rounded-xl border border-[#e7e5e4] bg-white p-8 shadow-sm">
        <h1 className="text-center text-2xl font-bold text-[#c2410c]">Moto Boutique</h1>
        <p className="mt-1 text-center text-sm text-[#57534e]">Control de inventario</p>

        <form onSubmit={enviar} className="mt-6 space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium">
              Correo
            </label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 w-full rounded-lg border border-[#e7e5e4] px-3 py-2 text-sm outline-none focus:border-[#c2410c]"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium">
              Contraseña
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full rounded-lg border border-[#e7e5e4] px-3 py-2 text-sm outline-none focus:border-[#c2410c]"
            />
          </div>

          {error && (
            <p role="alert" className="rounded-lg border border-[#e7e5e4] bg-[#f6f5f3] px-3 py-2 text-sm text-[#c2410c]">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={enviando}
            className="w-full rounded-lg bg-[#c2410c] px-4 py-2 text-sm font-medium text-white hover:bg-[#ea580c] disabled:opacity-60"
          >
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>
      </main>
    </div>
  )
}
