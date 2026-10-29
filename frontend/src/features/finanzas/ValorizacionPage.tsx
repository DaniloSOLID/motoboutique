import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { catalogoApi } from '../../api/catalogo'
import { ApiError } from '../../api/client'
import { administracionApi } from '../../api/administracion'
import { ErrorState } from '../../components/ErrorState'
import { Metric } from '../../components/Metric'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'
import { descargarArchivo, finanzasApi, promedioPonderado } from './api'
import { toast } from 'sonner'

const moneda = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const cantidadFormato = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })

export function ValorizacionPage() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const filtros = {
    fechaCorte: params.get('fechaCorte') ?? undefined,
    almacenId: params.get('almacenId') ? Number(params.get('almacenId')) : undefined,
    categoriaId: params.get('categoriaId') ? Number(params.get('categoriaId')) : undefined,
    q: params.get('q') ?? undefined,
    page, size,
  }
  const valorizacion = useQuery({ queryKey: ['valorizacion', filtros], queryFn: () => finanzasApi.valorizacion(filtros) })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: administracionApi.almacenes })
  const categorias = useQuery({ queryKey: ['categorias-opciones'], queryFn: () => catalogoApi.categorias({ page: 0, size: 200 }) })
  const error = valorizacion.error ?? almacenes.error ?? categorias.error
  if (valorizacion.isPending || almacenes.isPending || categorias.isPending) return <Skeleton />
  if (valorizacion.isError || almacenes.isError || categorias.isError) {
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudo cargar la valorización.'}
      reintentar={() => { void valorizacion.refetch(); void almacenes.refetch(); void categorias.refetch() }} />
  }
  const data = valorizacion.data
  const promedio = promedioPonderado(data.valorTotal, data.cantidadTotal)
  const cambiar = (campo: string, valor: string) => setParams((actual) => {
    const next = new URLSearchParams(actual)
    if (valor) next.set(campo, valor); else next.delete(campo)
    next.delete('page')
    return next
  })
  const exportar = async (formato: 'csv' | 'xlsx' | 'pdf') => {
    try {
      const blob = await finanzasApi.exportar('valorizacion', filtros, formato)
      descargarArchivo(blob, `valorizacion-${data.fechaCorte}.${formato}`)
    } catch (cause) {
      toast.error(cause instanceof ApiError ? cause.detalle : 'No se pudo exportar la valorización.')
    }
  }
  return <section>
    <div className="card section-space">
      <div className="section-title"><div><h3>Valorización del inventario</h3>
        <span>Saldo y costo promedio al corte seleccionado, calculados desde movimientos reales.</span></div></div>
      <div className="filter-bar">
        <label>Fecha de corte<input aria-label="Fecha de corte" type="date" value={params.get('fechaCorte') ?? ''}
          onChange={(event) => cambiar('fechaCorte', event.target.value)} /></label>
        <select aria-label="Almacén" value={params.get('almacenId') ?? ''} onChange={(event) => cambiar('almacenId', event.target.value)}>
          <option value="">Todos los almacenes</option>{almacenes.data.filter((almacen) => almacen.activo).map((almacen) =>
            <option key={almacen.id} value={almacen.id}>{almacen.nombre}</option>)}</select>
        <select aria-label="Categoría" value={params.get('categoriaId') ?? ''} onChange={(event) => cambiar('categoriaId', event.target.value)}>
          <option value="">Todas las categorías</option>{categorias.data.contenido.map((categoria) =>
            <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}</select>
        <SearchInput value={params.get('q') ?? ''} onChange={(value) => cambiar('q', value)} placeholder="Buscar código o producto..." />
        <div className="actions">{(['csv', 'xlsx', 'pdf'] as const).map((formato) =>
          <button key={formato} className="btn secondary" onClick={() => void exportar(formato)}>Exportar {formato.toUpperCase()}</button>)}</div>
      </div>
    </div>
    <div className="metrics section-space">
      <Metric label="Valor total del inventario" value={moneda.format(data.valorTotal)} icon={<span>S/</span>} />
      <Metric label="Unidades valorizadas" value={cantidadFormato.format(data.cantidadTotal)} icon={<span>∑</span>} />
      <Metric label="Costo promedio general" value={moneda.format(promedio)} icon={<span>Ø</span>} />
    </div>
    <div className="dashboard-grid section-space">
      <section className="card chart-card"><div className="section-title"><div><h3>Valor por categoría</h3>
        <span>Importes valorizados al corte, según costo promedio.</span></div></div>
        {data.categorias.length === 0 ? <p className="empty-state">No hay existencias valorizadas para estos filtros.</p> :
          <ResponsiveContainer width="100%" height={280}><BarChart data={data.categorias} margin={{ left: 12, right: 12, bottom: 30 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis dataKey="categoria" interval={0} angle={-20} textAnchor="end" />
            <YAxis tickFormatter={(value: number) => moneda.format(value)} />
            <Tooltip formatter={(value) => moneda.format(Number(value))} />
            <Bar dataKey="valor" name="Valor" fill="var(--link)" radius={[4, 4, 0, 0]} />
          </BarChart></ResponsiveContainer>}
      </section>
      <section className="card"><h3>Resumen por categoría</h3>
        {data.categorias.length === 0 ? <p className="empty-state">Sin datos para agrupar.</p> :
          <div className="scroll"><table><thead><tr><th>Categoría</th><th>Unidades</th><th>Valor</th></tr></thead>
            <tbody>{data.categorias.map((categoria) => <tr key={categoria.categoriaId}>
              <td>{categoria.categoria}</td><td>{cantidadFormato.format(categoria.cantidad)}</td><td>{moneda.format(categoria.valor)}</td>
            </tr>)}</tbody></table></div>}
      </section>
    </div>
    <section className="card section-space"><div className="section-title"><div><h3>Detalle valorizado</h3>
      <span>Corte: {new Date(`${data.fechaCorte}T12:00:00`).toLocaleDateString('es-PE')}</span></div></div>
      {data.productos.contenido.length === 0 ? <p className="empty-state">No hay productos valorizados en esta página.</p> :
        <div className="scroll"><table><thead><tr><th>Código</th><th>Producto</th><th>Categoría</th>
          <th>Unidades</th><th>Costo promedio</th><th>Valor</th></tr></thead>
          <tbody>{data.productos.contenido.map((producto) => <tr key={producto.productoId}>
            <td>{producto.codigo}</td><td>{producto.producto}</td><td>{producto.categoria}</td>
            <td>{cantidadFormato.format(producto.cantidad)}</td><td>{moneda.format(producto.costoPromedio)}</td><td>{moneda.format(producto.valor)}</td>
          </tr>)}</tbody></table></div>}
      <Pagination page={data.productos.numero} totalPages={data.productos.totalPaginas} onPage={(siguiente) =>
        setParams((actual) => { const next = new URLSearchParams(actual); next.set('page', String(siguiente)); return next })} />
      <small className="table-total">{data.productos.totalElementos} productos · total general {moneda.format(data.valorTotal)}</small>
    </section>
  </section>
}
