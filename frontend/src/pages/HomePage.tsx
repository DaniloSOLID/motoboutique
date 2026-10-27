import { useAuth } from '../auth/AuthContext'

/**
 * Inicio provisional tras iniciar sesión. Solo muestra la sesión activa y permite cerrarla;
 * el layout, el menú y los módulos pertenecen a las fases siguientes.
 */
export default function HomePage() {
  const { usuario, logout } = useAuth()

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <main className="w-full max-w-md rounded-xl border border-[#e7e5e4] bg-white p-8 text-center shadow-sm">
        <h1 className="text-xl font-semibold">Hola, {usuario?.nombre}</h1>
        <p className="mt-1 text-sm text-[#57534e]">{usuario?.email}</p>
        <div className="mt-4 flex flex-wrap justify-center gap-2">
          {usuario?.roles.map((rol) => (
            <span key={rol} className="rounded-full bg-[#f6f5f3] px-3 py-1 text-xs font-semibold text-[#c2410c]">
              {rol}
            </span>
          ))}
        </div>
        <button
          type="button"
          onClick={() => void logout()}
          className="mt-6 rounded-lg border border-[#e7e5e4] px-4 py-2 text-sm font-medium hover:bg-[#f6f5f3]"
        >
          Cerrar sesión
        </button>
      </main>
    </div>
  )
}
