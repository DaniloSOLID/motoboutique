import { Link } from 'react-router-dom'
import { detalleHttp, tituloHttp } from '../api/mensajesHttp'

interface Props {
  codigo: number
  titulo?: string
  detalle?: string
  /** Código corto para reportar errores 500. */
  referencia?: string
}

/**
 * Página de error. Migración de templates/error/general.html (Thymeleaf):
 * mismo diseño, colores, textos y botones "Volver" e "Ir al inicio".
 */
export default function ErrorPage({ codigo, titulo, detalle, referencia }: Props) {
  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <main className="w-full max-w-md rounded-xl border border-[#e7e5e4] bg-white p-8 text-center shadow-sm">
        <p className="text-5xl font-bold text-[#c2410c] tabular-nums">{codigo}</p>
        <h1 className="mt-3 text-xl font-semibold">{titulo ?? tituloHttp(codigo)}</h1>
        <p className="mt-2 text-sm text-[#57534e]">{detalle ?? detalleHttp(codigo)}</p>
        {referencia && (
          <p className="mt-4 text-xs text-[#57534e]">
            Referencia: <span className="font-mono font-semibold">{referencia}</span>
          </p>
        )}
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <button
            type="button"
            onClick={() => window.history.back()}
            className="rounded-lg border border-[#e7e5e4] px-4 py-2 text-sm font-medium hover:bg-[#f6f5f3]"
          >
            Volver
          </button>
          <Link
            to="/"
            className="rounded-lg bg-[#c2410c] px-4 py-2 text-sm font-medium text-white hover:bg-[#ea580c]"
          >
            Ir al inicio
          </Link>
        </div>
      </main>
    </div>
  )
}
