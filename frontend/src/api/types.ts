// Tipos que reflejan los DTO del backend (com.motoboutique.seguridad.dto y shared.web).

export interface UsuarioSesion {
  id: number
  nombre: string
  email: string
  /** Códigos de rol sin prefijo: ADMIN, ALMACENERO, CONTADOR, CONSULTA... */
  roles: string[]
  /** Códigos de permiso: USUARIO_VER, ROL_EDITAR... */
  permisos: string[]
}

export interface UsuarioDto {
  id: number
  nombre: string
  email: string
  telefono: string | null
  activo: boolean
  roles: string[]
}

export interface RolDto {
  id: number
  codigo: string
  nombre: string
  descripcion: string | null
  sistema: boolean
  permisos: string[]
}

export interface PermisoDto {
  id: number
  codigo: string
  descripcion: string
  modulo: string
}

/** Equivale a shared.web.PageResponse del backend. Número de página base 0. */
export interface PageResponse<T> {
  contenido: T[]
  numero: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
  paginaActual: number
  primera: boolean
  ultima: boolean
  vacia: boolean
  desde: number
  hasta: number
}

export interface Categoria {
  id: number
  nombre: string
  descripcion: string | null
  activo: boolean
}

export interface CategoriaGuardar {
  nombre: string
  descripcion?: string | null
  activo?: boolean
}

export interface Marca {
  id: number
  nombre: string
  descripcion: string | null
  activo: boolean
}

export interface MarcaGuardar {
  nombre: string
  descripcion?: string | null
  activo?: boolean
}

export interface Producto {
  id: number
  categoriaId: number
  categoria: string
  marcaId: number | null
  marca: string | null
  codigo: string
  nombre: string
  descripcion: string | null
  precioCompra: number
  precioVenta: number
  stockMinimo: number
  unidad: string
  activo: boolean
  compatibilidadIds: number[]
  compatibilidades: ModeloMoto[]
  imagenUrl: string | null
  especificaciones: Record<string, unknown>
  stockTotal: number
  stockPorAlmacen: StockProducto[]
}

export interface StockProducto {
  almacenId: number
  almacen: string | null
  cantidad: number
  reservado: number
}

export interface ModeloMoto {
  id: number
  marcaId: number
  marca: string
  nombre: string
  anioInicio: number | null
  anioFin: number | null
  cilindradaCc: number | null
  tipo: string | null
}

export interface ModeloMotoGuardar {
  marcaId: number
  nombre: string
  anioInicio?: number | null
  anioFin?: number | null
  cilindradaCc?: number | null
  tipo?: string | null
}

export interface MarcaModeloArbol {
  id: number
  nombre: string
  modelos: ModeloMoto[]
}

export interface ProductoGuardar {
  categoriaId: number
  marcaId?: number | null
  codigo: string
  nombre: string
  descripcion?: string | null
  precioCompra: number
  precioVenta: number
  stockMinimo: number
  unidad: string
  activo?: boolean
  imagenUrl?: string | null
  especificaciones?: Record<string, unknown>
  compatibilidadIds?: number[]
}

/** Error de la API (ProblemDetail, RFC 9457). */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  /** Errores de validación por campo (solo en 400). */
  errores?: Record<string, string>
  /** Código para reportar errores 500. */
  referencia?: string
}
