import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// En desarrollo, Vite reenvía /api al backend: para el navegador todo es el mismo origen,
// así la cookie de sesión y el token CSRF funcionan sin configurar CORS.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const destino = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8081'

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      proxy: {
        '/api': { target: destino, changeOrigin: false },
      },
    },
  }
})
