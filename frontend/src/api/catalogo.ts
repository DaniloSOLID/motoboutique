import { apiFetch } from './client'
import type {
  Categoria,
  CategoriaGuardar,
  Marca,
  MarcaGuardar,
  MarcaModeloArbol,
  ModeloMoto,
  ModeloMotoGuardar,
  PageResponse,
  Producto,
  ProductoGuardar,
} from './types'

export interface CatalogoFiltros {
  q?: string
  page?: number
  size?: number
  sort?: string
  activo?: boolean
  categoriaId?: number
  marcaId?: number
  stockBajo?: boolean
  modeloMotoId?: number
  anio?: number
}

function rutaConFiltros(ruta: string, filtros: CatalogoFiltros = {}): string {
  const parametros = new URLSearchParams()
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== '') parametros.set(clave, String(valor))
  })
  const consulta = parametros.toString()
  return consulta ? `${ruta}?${consulta}` : ruta
}

export const catalogoApi = {
  categorias: (filtros?: CatalogoFiltros) =>
    apiFetch<PageResponse<Categoria>>(rutaConFiltros('/api/catalogo/categorias', filtros)),
  crearCategoria: (datos: CategoriaGuardar) => apiFetch<Categoria>('/api/catalogo/categorias', { method: 'POST', json: datos }),
  actualizarCategoria: (id: number, datos: CategoriaGuardar) => apiFetch<Categoria>(`/api/catalogo/categorias/${id}`, { method: 'PUT', json: datos }),
  eliminarCategoria: (id: number) => apiFetch<void>(`/api/catalogo/categorias/${id}`, { method: 'DELETE' }),
  marcas: (filtros?: Pick<CatalogoFiltros, 'q' | 'page' | 'size' | 'sort' | 'activo'>) =>
    apiFetch<PageResponse<Marca>>(rutaConFiltros('/api/catalogo/marcas', filtros)),
  crearMarca: (datos: MarcaGuardar) => apiFetch<Marca>('/api/catalogo/marcas', { method: 'POST', json: datos }),
  actualizarMarca: (id: number, datos: MarcaGuardar) => apiFetch<Marca>(`/api/catalogo/marcas/${id}`, { method: 'PUT', json: datos }),
  eliminarMarca: (id: number) => apiFetch<void>(`/api/catalogo/marcas/${id}`, { method: 'DELETE' }),
  productos: (filtros?: CatalogoFiltros) =>
    apiFetch<PageResponse<Producto>>(rutaConFiltros('/api/catalogo/productos', filtros)),
  producto: (id: number) => apiFetch<Producto>(`/api/catalogo/productos/${id}`),
  crearProducto: (datos: ProductoGuardar) => apiFetch<Producto>('/api/catalogo/productos', { method: 'POST', json: datos }),
  actualizarProducto: (id: number, datos: ProductoGuardar) => apiFetch<Producto>(`/api/catalogo/productos/${id}`, { method: 'PUT', json: datos }),
  eliminarProducto: (id: number) => apiFetch<void>(`/api/catalogo/productos/${id}`, { method: 'DELETE' }),
  modelos: (filtros?: Pick<CatalogoFiltros, 'q' | 'page' | 'size' | 'sort' | 'marcaId' | 'anio'>) =>
    apiFetch<PageResponse<ModeloMoto>>(rutaConFiltros('/api/catalogo/modelos', filtros)),
  crearModelo: (datos: ModeloMotoGuardar) =>
    apiFetch<ModeloMoto>('/api/catalogo/modelos', { method: 'POST', json: datos }),
  actualizarModelo: (id: number, datos: ModeloMotoGuardar) =>
    apiFetch<ModeloMoto>(`/api/catalogo/modelos/${id}`, { method: 'PUT', json: datos }),
  eliminarModelo: (id: number) =>
    apiFetch<void>(`/api/catalogo/modelos/${id}`, { method: 'DELETE' }),
  compatibilidades: (id: number, modeloIds: number[]) =>
    apiFetch<void>(`/api/catalogo/productos/${id}/compatibilidades`, { method: 'PUT', json: modeloIds }),
  productosDeModelo: (id: number) =>
    apiFetch<Producto[]>(`/api/catalogo/modelos/${id}/productos`),
  marcasConModelos: () => apiFetch<MarcaModeloArbol[]>('/api/catalogo/marcas-con-modelos'),
  aniosModelo: (id: number) => apiFetch<number[]>(`/api/catalogo/modelos/${id}/anios`),
}
