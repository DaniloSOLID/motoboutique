import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { catalogoApi } from '../../api/catalogo'
import { ApiError } from '../../api/client'
import { EmptyState } from '../../components/EmptyState'
import { ErrorState } from '../../components/ErrorState'
import { Metric } from '../../components/Metric'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'
import { inventarioApi } from '../inventario/api'
import { descargarArchivo, finanzasApi, instanteDesdeFecha } from './api'
import { toast } from 'sonner'

const cantidadFormato = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })
const costoFormato = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })

export function KardexPage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const productoId = params.get('productoId') ? Number(params.get('productoId')) : undefined
  const almacenId = params.get('almacenId') ? Number(params.get('almacenId')) : undefined
  const productos = useQuery({
    queryKey: ['kardex-productos', params.get('q')],
    queryFn: () => catalogoApi.productos({ q: params.get('q') ?? undefined, page: 0, size: 100, activo: true }),
  })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: inventarioApi.almacenes })
  const kardex = useQuery({
    queryKey: ['kardex', productoId, almacenId, params.get('desde'), params.get('hasta')],
    queryFn: () => finanzasApi.kardex({
      productoId: productoId!, almacenId: almacenId!,
      desde: instanteDesdeFecha(params.get('desde') ?? ''),
      hasta: instanteDesdeFecha(params.get('hasta') ?? '', true),
    }),
    enabled: Boolean(productoId && almacenId),
  })
  const codigo = productos.data?.contenido.find((producto) => producto.id === productoId)?.codigo
  const stockActual = useQuery({
    queryKey: ['kardex-stock-actual', codigo, almacenId],
    queryFn: () => inventarioApi.stock({ q: codigo, almacenId, page: 0, size: 100 }),
    enabled: Boolean(codigo && almacenId && !params.get('hasta')),
  })
  if (productos.isPending || almacenes.isPending) return <Skeleton />
  if (productos.isError || almacenes.isError) {
    const error = productos.error ?? almacenes.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudieron cargar los filtros del kardex.'}
      reintentar={() => { void productos.refetch(); void almacenes.refetch() }} />
  }
  if (kardex.isError) return <ErrorState mensaje={kardex.error instanceof ApiError ? kardex.error.detalle : 'No se pudo cargar el kardex.'}
    reintentar={() => void kardex.refetch()} />
  if (stockActual.isError) return <ErrorState mensaje={stockActual.error instanceof ApiError ? stockActual.error.detalle : 'No se pudo verificar el stock actual.'}
    reintentar={() => void stockActual.refetch()} />
  const cambiar = (campo: string, valor: string) => setParams((actual) => {
    const next = new URLSearchParams(actual)
    if (valor) next.set(campo, valor); else next.delete(campo)
    next.delete('page')
    return next
  })
  const exportar = async (formato: 'csv' | 'xlsx' | 'pdf') => {
    if (!productoId || !almacenId) {
      toast.error('Selecciona un producto y un almacén para exportar el kardex.')
      return
    }
    try {
      const blob = await finanzasApi.exportar('kardex', {
        productoId, almacenId, desde: instanteDesdeFecha(params.get('desde') ?? ''),
        hasta: instanteDesdeFecha(params.get('hasta') ?? '', true), page: 0, size: 100,
      }, formato)
      descargarArchivo(blob, `kardex-${productoId}-${formato}.${formato}`)
    } catch (error) {
      toast.error(error instanceof ApiError ? error.detalle : 'No se pudo exportar el kardex.')
    }
  }
  const data = kardex.data
  const movimientos = data?.movimientos ?? []
  const finalKardex = movimientos.length > 0 ? movimientos[movimientos.length - 1].saldo : data?.saldoInicial ?? 0
  const filaStock = stockActual.data?.contenido.find((fila) => fila.productoId === productoId && fila.almacenId === almacenId)
  const finalActual = filaStock?.cantidad ?? 0
  const offset = page * size
  const filasPagina = movimientos.slice(offset, offset + size)
  const grafica = data ? [
    { fecha: 'Saldo inicial', saldo: data.saldoInicial },
    ...movimientos.map((movimiento) => ({
      fecha: new Date(movimiento.fecha).toLocaleDateString('es-PE'),
      saldo: movimiento.saldo,
    })),
  ] : []
  return <section>
    <div className="card section-space">
      <div className="section-title"><div><h3>Kardex de inventario</h3>
        <span>Movimientos en orden cronológico y saldo calculado por el backend.</span></div></div>
      <div className="filter-bar">
        <SearchInput value={params.get('q') ?? ''} onChange={(value) => cambiar('q', value)} placeholder="Buscar código o producto..." />
        <select aria-label="Producto" value={params.get('productoId') ?? ''} onChange={(event) => cambiar('productoId', event.target.value)}>
          <option value="">Selecciona un producto</option>{productos.data.contenido.map((producto) =>
            <option key={producto.id} value={producto.id}>{producto.codigo} · {producto.nombre}</option>)}</select>
        <select aria-label="Almacén" value={params.get('almacenId') ?? ''} onChange={(event) => cambiar('almacenId', event.target.value)}>
          <option value="">Selecciona un almacén</option>{almacenes.data.filter((almacen) => almacen.activo).map((almacen) =>
            <option key={almacen.id} value={almacen.id}>{almacen.nombre}</option>)}</select>
        <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => cambiar('desde', event.target.value)} /></label>
        <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => cambiar('hasta', event.target.value)} /></label>
        <div className="actions">{(['csv', 'xlsx', 'pdf'] as const).map((formato) =>
          <button key={formato} className="btn secondary" disabled={!productoId || !almacenId}
            onClick={() => void exportar(formato)}>Exportar {formato.toUpperCase()}</button>)}</div>
      </div>
    </div>
    {!productoId || !almacenId ? <EmptyState title="Selecciona producto y almacén"
      description="El kardex se cargará al elegir ambos filtros." /> :
      kardex.isPending ? <Skeleton /> : data ? <>
        <div className="metrics section-space">
          <Metric label="Saldo inicial del período" value={cantidadFormato.format(data.saldoInicial)} icon={<span>↖</span>} />
          <Metric label="Saldo final del kardex" value={cantidadFormato.format(finalKardex)} icon={<span>∑</span>} />
          {!params.get('hasta') && !stockActual.isPending &&
            <Metric label="Stock actual" value={cantidadFormato.format(finalActual)} icon={<span>□</span>} />}
        </div>
        {!params.get('hasta') && !stockActual.isPending && finalKardex !== finalActual &&
          <p className="field-hint" role="alert">El saldo del kardex ({cantidadFormato.format(finalKardex)}) no coincide con el stock vigente
            ({cantidadFormato.format(finalActual)}). Revisa la consistencia de los movimientos.</p>}
        <section className="card chart-card section-space"><div className="section-title"><div><h3>Evolución del saldo</h3>
          <span>{data.producto} · {data.almacen}</span></div></div>
          {grafica.length <= 1 ? <p className="empty-state">No hay movimientos en el período seleccionado.</p> :
            <ResponsiveContainer width="100%" height={260}><LineChart data={grafica}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="fecha" /><YAxis /><Tooltip formatter={(value) => cantidadFormato.format(Number(value))} />
              <Line type="stepAfter" dataKey="saldo" name="Saldo" stroke="var(--link)" dot={false} />
            </LineChart></ResponsiveContainer>}
        </section>
        <section className="card section-space"><div className="section-title"><h3>Movimientos cronológicos</h3></div>
          <div className="scroll"><table><thead><tr><th>Fecha</th><th>Documento</th><th>Referencia</th><th>Tipo</th>
            <th>Saldo inicial</th><th>Entrada</th><th>Salida</th><th>Saldo</th><th>Costo promedio</th></tr></thead>
            <tbody><tr><td>{params.get('desde') || '—'}</td><td>—</td><td>—</td><td>Saldo inicial</td>
              <td>{cantidadFormato.format(data.saldoInicial)}</td><td>—</td><td>—</td><td>{cantidadFormato.format(data.saldoInicial)}</td><td>—</td></tr>
              {filasPagina.map((movimiento) => <tr key={movimiento.movimientoId}>
                <td>{new Date(movimiento.fecha).toLocaleString('es-PE')}</td><td>{movimiento.numero}</td>
                <td>{movimiento.documentoReferencia ?? '—'}</td><td>{movimiento.tipo}</td><td>—</td>
                <td>{movimiento.entrada ? cantidadFormato.format(movimiento.entrada) : '—'}</td>
                <td>{movimiento.salida ? cantidadFormato.format(movimiento.salida) : '—'}</td>
                <td>{cantidadFormato.format(movimiento.saldo)}</td>
                <td>{movimiento.costoPromedio === null ? '—' : costoFormato.format(movimiento.costoPromedio)}</td>
              </tr>)}</tbody></table></div>
          {movimientos.length === 0 && <p className="empty-state">No hay movimientos dentro del rango seleccionado.</p>}
          <Pagination page={page} totalPages={Math.ceil(movimientos.length / size)} onPage={(siguiente) =>
            setParams((actual) => { const next = new URLSearchParams(actual); next.set('page', String(siguiente)); return next })} />
          <small className="table-total">{movimientos.length} movimientos · saldo final {cantidadFormato.format(finalKardex)}</small>
        </section>
      </> : null}
  </section>
}
