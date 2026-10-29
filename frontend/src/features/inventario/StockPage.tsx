import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { inventarioApi } from './api'
import { catalogoApi } from '../../api/catalogo'
import { ApiError } from '../../api/client'
import { Badge } from '../../components/Badge'
import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'

export function StockPage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const filtros = {
    q: params.get('q') ?? undefined,
    almacenId: params.get('almacenId') ? Number(params.get('almacenId')) : undefined,
    categoriaId: params.get('categoriaId') ? Number(params.get('categoriaId')) : undefined,
    estado: params.get('estado') ?? undefined,
    page, size, sort: params.get('sort') ?? 'nombre',
  }
  const stock = useQuery({ queryKey: ['stock', filtros], queryFn: () => inventarioApi.stock(filtros) })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: inventarioApi.almacenes })
  const categorias = useQuery({ queryKey: ['categorias-opciones'], queryFn: () => catalogoApi.categorias({ page: 0, size: 200 }) })
  if (stock.isPending || almacenes.isPending || categorias.isPending) return <Skeleton />
  if (stock.isError || almacenes.isError || categorias.isError) {
    const error = stock.error ?? almacenes.error ?? categorias.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudo cargar el stock.'}
      reintentar={() => { void stock.refetch(); void almacenes.refetch(); void categorias.refetch() }} />
  }
  const cambiar = (campo: string, valor: string) => setParams((actual) => {
    const next = new URLSearchParams(actual)
    if (valor) next.set(campo, valor); else next.delete(campo)
    next.delete('page')
    return next
  })
  return <section className="card">
    <div className="section-title"><div><h3>Stock por almacén</h3><span>Existencias y reservas actuales.</span></div></div>
    <div className="filter-bar">
      <SearchInput value={params.get('q') ?? ''} onChange={(value) => cambiar('q', value)} placeholder="Buscar código o producto..." />
      <select aria-label="Almacén" value={params.get('almacenId') ?? ''} onChange={(event) => cambiar('almacenId', event.target.value)}>
        <option value="">Todos los almacenes</option>{almacenes.data.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}
      </select>
      <select aria-label="Categoría" value={params.get('categoriaId') ?? ''} onChange={(event) => cambiar('categoriaId', event.target.value)}>
        <option value="">Todas las categorías</option>{categorias.data.contenido.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
      </select>
      <select aria-label="Estado" value={params.get('estado') ?? ''} onChange={(event) => cambiar('estado', event.target.value)}>
        <option value="">Todos los estados</option><option value="OK">Normal</option><option value="BAJO">Bajo</option><option value="AGOTADO">Agotado</option>
      </select>
    </div>
    {stock.data.contenido.length === 0 ? <p className="empty-state">No hay existencias con estos filtros.</p> :
      <div className="scroll"><table><thead><tr><th>Código</th><th>Producto</th><th>Categoría</th><th>Almacén</th><th>Stock</th><th>Reservado</th><th>Disponible</th><th>Estado</th></tr></thead>
        <tbody>{stock.data.contenido.map((fila, index) => <tr key={`${fila.productoId}-${fila.almacenId}-${index}`}>
          <td>{fila.codigo}</td><td>{fila.nombre}</td><td>{fila.categoria}</td><td>{fila.almacen}</td>
          <td>{fila.cantidad.toLocaleString('es-PE')}</td><td>{fila.reservado.toLocaleString('es-PE')}</td>
          <td>{(fila.cantidad - fila.reservado).toLocaleString('es-PE')}</td><td><Badge>{fila.estado}</Badge></td>
        </tr>)}</tbody></table></div>}
    <Pagination page={stock.data.numero} totalPages={stock.data.totalPaginas} onPage={(siguiente) =>
      setParams((actual) => { const next = new URLSearchParams(actual); next.set('page', String(siguiente)); return next })} />
  </section>
}
