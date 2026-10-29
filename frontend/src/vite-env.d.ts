/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_ADMIN_EMAIL?: string
  readonly VITE_ALMACENERO_EMAIL?: string
  readonly VITE_CONTADOR_EMAIL?: string
  readonly VITE_CONSULTA_EMAIL?: string
  readonly VITE_ADMIN_PASSWORD?: string
  readonly VITE_DEMO_PASSWORD?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
