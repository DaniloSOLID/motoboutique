import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { inventarioApi, type Movimiento } from './api'
import { catalogoApi } from '../../api/catalogo'
import { ApiError } from '../../api/client'
import { Badge } from '../../components/Badge'
import { ErrorState } from '../../components/ErrorState'
import { FormModal } from '../../components/FormModal'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'
import { descargar } from './api'

export function MovimientosPage() {
  const [params, setParams] = useSearchParams()
  const [detalle, setDetalle] = useState<Movimiento | null>(null)
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const filtros = {
    q: params.get('q') ?? undefined,
    tipo: params.get('tipo') ?? undefined,
    almacenId: params.get('almacenId') ? Number(params.get('almacenId')) : undefined,
    productoId: params.get('productoId') ? Number(params.get('productoId')) : undefined,
    desde: params.get('desde') ? new Date(`${params.get('desde')}T00:00:00-05:00`).toISOString() : undefined,
    hasta: params.get('hasta') ? new Date(`${params.get('hasta')}T23:59:59.999-05:00`).toISOString() : undefined,
    page, size, sort: params.get('sort') ?? 'fecha', direction: params.get('direction') ?? 'desc',
  }
  const movimientos = useQuery({ queryKey: ['movimientos', filtros], queryFn: () => inventarioApi.movimientos(filtros) })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: inventarioApi.almacenes })
  const productos = useQuery({
    queryKey: ['productos-opciones', params.get('productoQ')],
    queryFn: () => catalogoApi.productos({ q: params.get('productoQ') ?? undefined, size: 100, activo: true }),
  })
  const cambiar = (campo: string, valor: string) => setParams((actuales) => {
    const nuevos = new URLSearchParams(actuales)
    if (valor) nuevos.set(campo, valor); else nuevos.delete(campo)
    nuevos.delete('page')
    return nuevos
  })
  const exportar = async () => {
    try { descargar(await inventarioApi.exportarMovimientos(filtros), 'movimientos-inventario.csv') }
    catch (error) { toast.error(error instanceof ApiError ? error.detalle : 'No se pudo exportar el archivo.') }
  }
  if (movimientos.isPending || almacenes.isPending || productos.isPending) return <Skeleton />
  if (movimientos.isError || almacenes.isError || productos.isError) {
    const error = movimientos.error ?? almacenes.error ?? productos.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudieron cargar los movimientos.'}
      reintentar={() => { void movimientos.refetch(); void almacenes.refetch(); void productos.refetch() }} />
  }
  const pagina = movimientos.data
  return <section className="card">
    <div className="section-title"><div><h3>Historial de movimientos</h3><span>Consulta de kardex con filtros del servidor.</span></div></div>
    <div className="filter-bar">
      <SearchInput value={params.get('q') ?? ''} onChange={(value) => cambiar('q', value)} placeholder="Buscar código o documento..." />
      <select aria-label="Tipo de movimiento" value={params.get('tipo') ?? ''} onChange={(event) => cambiar('tipo', event.target.value)}>
        <option value="">Todos los tipos</option>{['ENTRADA', 'SALIDA', 'TRANSFERENCIA_SALIDA', 'TRANSFERENCIA_ENTRADA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO', 'RECEPCION'].map((tipo) =>
          <option key={tipo} value={tipo}>{tipo.replaceAll('_', ' ')}</option>)}
      </select>
      <select aria-label="Almacén" value={params.get('almacenId') ?? ''} onChange={(event) => cambiar('almacenId', event.target.value)}>
        <option value="">Todos los almacenes</option>{almacenes.data.map((almacen) =>
          <option key={almacen.id} value={almacen.id}>{almacen.nombre}</option>)}
      </select>
      <input aria-label="Buscar producto" placeholder="Producto: código o nombre" value={params.get('productoQ') ?? ''}
        onChange={(event) => cambiar('productoQ', event.target.value)} />
      <select aria-label="Producto" value={params.get('productoId') ?? ''} onChange={(event) => cambiar('productoId', event.target.value)}>
        <option value="">Todos los productos</option>{productos.data.contenido.map((producto) =>
          <option key={producto.id} value={producto.id}>{producto.codigo} · {producto.nombre}</option>)}
      </select>
      <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => cambiar('desde', event.target.value)} /></label>
      <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => cambiar('hasta', event.target.value)} /></label>
      <button className="btn secondary" onClick={() => void exportar()}>Exportar CSV</button>
    </div>
    {pagina.contenido.length === 0 ? <p className="empty-state">No hay movimientos con esos filtros.</p> :
      <div className="scroll"><table><thead><tr>{[
        ['fecha', 'Fecha'], ['tipo', 'Tipo'], ['producto.codigo', 'Producto'],
        ['almacen.nombre', 'Almacén'], ['cantidad', 'Cantidad'],
      ].map(([sort, label]) => <th key={sort}><button className="table-sort" onClick={() => {
        const nextDirection = params.get('sort') === sort && params.get('direction') === 'asc' ? 'desc' : 'asc'
        setParams((actuales) => {
          const next = new URLSearchParams(actuales)
          next.set('sort', sort)
          next.set('direction', nextDirection)
          next.delete('page')
          return next
        })
      }}>{label}</button></th>)}<th>Detalle</th></tr></thead>
        <tbody>{pagina.contenido.map((item) => <tr key={item.id}><td>{item.numero}</td>
          <td>{new Date(item.fecha).toLocaleString('es-PE')}</td><td><Badge>{item.tipo}</Badge></td>
          <td>{item.productoCodigo} · {item.productoNombre}</td><td>{item.almacen}</td><td>{item.cantidad}</td>
          <td>{item.responsable}</td><td><button className="btn ghost" onClick={() => setDetalle(item)}>Ver</button></td></tr>)}</tbody>
      </table></div>}
    <Pagination page={pagina.numero} totalPages={pagina.totalPaginas} onPage={(siguiente) =>
      setParams((actuales) => { const next = new URLSearchParams(actuales); next.set('page', String(siguiente)); return next })} />
    <small className="table-total">{pagina.totalElementos} movimientos</small>
    {detalle && <FormModal title={`Movimiento ${detalle.numero}`} onClose={() => setDetalle(null)}>
      <dl className="detail-grid"><dt>Producto</dt><dd>{detalle.productoCodigo} · {detalle.productoNombre}</dd>
        <dt>Tipo</dt><dd>{detalle.tipo}</dd><dt>Almacén</dt><dd>{detalle.almacen}</dd>
        <dt>Cantidad</dt><dd>{detalle.cantidad}</dd><dt>Saldo</dt><dd>{detalle.saldoCantidad}</dd>
        <dt>Costo unitario</dt><dd>{detalle.costoUnitario === null ? '—' : `S/ ${detalle.costoUnitario.toLocaleString('es-PE', { minimumFractionDigits: 2 })}`}</dd>
        <dt>Costo promedio</dt><dd>{detalle.costoPromedio === null ? '—' : `S/ ${detalle.costoPromedio.toLocaleString('es-PE', { minimumFractionDigits: 2 })}`}</dd>
        <dt>Referencia</dt><dd>{detalle.referencia ?? '—'}</dd><dt>Motivo</dt><dd>{detalle.motivo ?? '—'}</dd>
        <dt>Responsable</dt><dd>{detalle.responsable}</dd></dl>
    </FormModal>}
  </section>
}
