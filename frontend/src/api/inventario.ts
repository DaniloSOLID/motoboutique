import { apiFetch } from './client'

export interface ProductoInventario {
  id: number
  codigo: string
  nombre: string
  categoria: string
  cantidad: number
  almacen: string
  estado: string
}

export interface MovimientoInventario {
  id: number
  codigo: string
  fecha: string
  tipo: string
  producto: string
  cantidad: number
  almacen: string
  responsable: string
}

interface MovimientoApi {
  id: number
  numero: string
  fecha: string
  tipo: string
  productoNombre: string
  cantidad: number
  almacen: string
  responsable: string
}

interface PageResponse<T> {
  contenido: T[]
}

export interface ResumenInventario {
  productosActivos: number
  stockValorizado: number
  alertasStock: number
}

export interface UsuarioListado {
  id: number
  nombre: string
  email: string
  roles: string[]
  activo: boolean
  ultimoAcceso?: string | null
}

export interface UsuarioPage<T> {
  contenido: T[]
  numero: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
}

export const inventarioApi = {
  productos: () => apiFetch<ProductoInventario[]>('/api/inventario/productos'),
  movimientos: async () => {
    const pagina = await apiFetch<PageResponse<MovimientoApi>>('/api/inventario/movimientos?size=100')
    return pagina.contenido.map((movimiento) => ({
      id: movimiento.id,
      codigo: movimiento.numero,
      fecha: movimiento.fecha,
      tipo: movimiento.tipo,
      producto: movimiento.productoNombre,
      cantidad: movimiento.cantidad,
      almacen: movimiento.almacen,
      responsable: movimiento.responsable,
    }))
  },
  resumen: () => apiFetch<ResumenInventario>('/api/inventario/resumen'),
}

export interface UsuarioCrear {
  nombre: string
  email: string
  password: string
  telefono?: string
  roles: string[]
}

export const usuariosApi = {
  listar: (filtros: { q?: string; activo?: boolean; rol?: string; page?: number; size?: number; sort?: string; direction?: string } = {}) => {
    const parametros = new URLSearchParams()
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== undefined) parametros.set(clave, String(valor))
    })
    if (!parametros.has('size')) parametros.set('size', '200')
    return apiFetch<UsuarioPage<UsuarioListado>>(`/api/usuarios?${parametros.toString()}`)
  },
  crear: (usuario: UsuarioCrear) => apiFetch<UsuarioListado>('/api/usuarios', { method: 'POST', json: usuario }),
  cambiarEstado: (id: number, activo: boolean) => apiFetch<void>(`/api/usuarios/${id}/estado`, { method: 'PATCH', json: { activo } }),
  editar: (id: number, usuario: { nombre: string; email: string; rol: string; activo: boolean }) => apiFetch<void>(`/api/usuarios/${id}`, { method: 'PUT', json: usuario }),
  restablecerPassword: (id: number, password: string) => apiFetch<void>(`/api/usuarios/${id}/password`, { method: 'PUT', json: { password } }),
  eliminar: (id: number) => apiFetch<void>(`/api/usuarios/${id}`, { method: 'DELETE' }),
}
