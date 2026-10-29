import { useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import {
  Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from 'recharts'
import { catalogoApi } from '../../api/catalogo'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'
import { administracionApi } from '../../api/administracion'
import { descargarArchivo, finanzasApi, instanteDesdeFecha } from './api'
import { toast } from 'sonner'

const reportes = [
  { tipo: 'por-categoria', nombre: 'Por categoría', grafica: 'pie' },
  { tipo: 'stock-bajo', nombre: 'Stock bajo', grafica: 'bar' },
  { tipo: 'movimientos-por-periodo', nombre: 'Movimientos por período', grafica: 'line' },
  { tipo: 'compras-por-proveedor', nombre: 'Compras por proveedor', grafica: 'bar' },
] as const
const colores = ['var(--link)', 'var(--accent)', 'var(--success)', 'var(--warning)', '#8B5CF6', '#06B6D4']
const numero = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 })
const moneda = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
type DatoGrafica = { nombre: string; cantidad?: number; valor?: number; minimo?: number; movimientos?: number }

export function ReportesPage() {
  const [params, setParams] = useSearchParams()
  const { tienePermiso } = useAuth()
  const tipo = params.get('tipo') ?? 'por-categoria'
  const config = reportes.find((reporte) => reporte.tipo === tipo) ?? reportes[0]
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Math.min(100, Math.max(1, Number(params.get('size') ?? 20)))
  const filtros = {
    fechaCorte: params.get('fechaCorte') ?? undefined,
    almacenId: params.get('almacenId') ? Number(params.get('almacenId')) : undefined,
    categoriaId: params.get('categoriaId') ? Number(params.get('categoriaId')) : undefined,
    q: params.get('q') ?? undefined,
    desde: instanteDesdeFecha(params.get('desde') ?? ''),
    hasta: instanteDesdeFecha(params.get('hasta') ?? '', true),
    page, size,
  }
  const reporte = useQuery({ queryKey: ['reporte', config.tipo, filtros], queryFn: () => finanzasApi.reporte(config.tipo, filtros) })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: administracionApi.almacenes })
  const categorias = useQuery({ queryKey: ['categorias-opciones'], queryFn: () => catalogoApi.categorias({ page: 0, size: 200 }) })
  if (reporte.isPending || almacenes.isPending || categorias.isPending) return <Skeleton />
  if (reporte.isError || almacenes.isError || categorias.isError) {
    const error = reporte.error ?? almacenes.error ?? categorias.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudo cargar el reporte.'}
      reintentar={() => { void reporte.refetch(); void almacenes.refetch(); void categorias.refetch() }} />
  }
  const data = reporte.data
  const indice = (nombre: string) => data.columnas.findIndex((columna) =>
    columna.toLocaleLowerCase('es-PE').includes(nombre.toLocaleLowerCase('es-PE')))
  const filas = data.datos.contenido
  const chartData: DatoGrafica[] = filas.map((fila) => {
    if (config.tipo === 'por-categoria') return { nombre: fila[0] ?? '', cantidad: Number(fila[1]) || 0, valor: Number(fila[2]) || 0 }
    if (config.tipo === 'stock-bajo') return { nombre: fila[1] ?? fila[0] ?? '', cantidad: Number(fila[3]) || 0, minimo: Number(fila[4]) || 0 }
    if (config.tipo === 'compras-por-proveedor') return { nombre: fila[0] ?? '', valor: Number(fila[2]) || 0 }
    const fechaIndex = indice('fecha')
    const fecha = fila[fechaIndex] ?? ''
    return { nombre: fecha ? new Date(fecha).toLocaleDateString('es-PE') : '—', movimientos: 1 }
  })
  const movimientosAgrupados: DatoGrafica[] = config.tipo === 'movimientos-por-periodo'
    ? Object.values(chartData.reduce<Record<string, { nombre: string; movimientos: number }>>((acum, fila) => {
      const clave = fila.nombre
      acum[clave] = { nombre: clave, movimientos: (acum[clave]?.movimientos ?? 0) + 1 }
      return acum
    }, {}))
    : chartData
  const cambiar = (campo: string, valor: string) => setParams((actual) => {
    const next = new URLSearchParams(actual)
    if (valor) next.set(campo, valor); else next.delete(campo)
    next.delete('page')
    return next
  })
  const exportar = async (formato: 'csv' | 'xlsx' | 'pdf') => {
    try {
      const archivo = await finanzasApi.exportar(config.tipo, { ...filtros, page: 0, size: 100 }, formato)
      descargarArchivo(archivo, `reporte-${config.tipo}.${formato}`)
    } catch (error) {
      toast.error(error instanceof ApiError ? error.detalle : 'No se pudo exportar el reporte.')
    }
  }
  return <section>
    <div className="card section-space">
      <div className="section-title"><div><h3>Reportes</h3><span>Consulta de inventario y compras con filtros del servidor.</span></div></div>
      <div className="filter-bar">
        <label>Tipo de reporte<select aria-label="Tipo de reporte" value={config.tipo}
          onChange={(event) => cambiar('tipo', event.target.value)}>
          {reportes.map((item) => <option key={item.tipo} value={item.tipo}>{item.nombre}</option>)}
        </select></label>
        <SearchInput value={params.get('q') ?? ''} onChange={(value) => cambiar('q', value)} placeholder="Buscar por código, producto o proveedor..." />
        <select aria-label="Almacén" value={params.get('almacenId') ?? ''} onChange={(event) => cambiar('almacenId', event.target.value)}>
          <option value="">Todos los almacenes</option>{almacenes.data.map((almacen) =>
            <option key={almacen.id} value={almacen.id}>{almacen.nombre}</option>)}</select>
        <select aria-label="Categoría" value={params.get('categoriaId') ?? ''} onChange={(event) => cambiar('categoriaId', event.target.value)}>
          <option value="">Todas las categorías</option>{categorias.data.contenido.map((categoria) =>
            <option key={categoria.id} value={categoria.id}>{categoria.nombre}</option>)}</select>
        {config.tipo === 'por-categoria' && <label>Fecha de corte<input type="date" value={params.get('fechaCorte') ?? ''}
          onChange={(event) => cambiar('fechaCorte', event.target.value)} /></label>}
        {config.tipo !== 'por-categoria' && <>
          <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => cambiar('desde', event.target.value)} /></label>
          <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => cambiar('hasta', event.target.value)} /></label>
        </>}
        {tienePermiso('REPORTE_EXPORTAR') && <div className="actions">{(['csv', 'xlsx', 'pdf'] as const).map((formato) =>
          <button key={formato} className="btn secondary" onClick={() => void exportar(formato)}>Exportar {formato.toUpperCase()}</button>)}</div>}
      </div>
    </div>
    <section className="card chart-card section-space"><div className="section-title"><div><h3>{config.nombre}</h3>
      <span>Gráfica basada en los resultados reales de la consulta.</span></div></div>
      {chartData.length === 0 ? <p className="empty-state">No hay datos para graficar con estos filtros.</p> :
        <ResponsiveContainer width="100%" height={280}>
          {config.grafica === 'pie'
            ? <PieChart><Tooltip formatter={(value) => numero.format(Number(value))} /><Pie data={chartData}
              dataKey="valor" nameKey="nombre" label>{chartData.map((item, index) =>
                <Cell key={item.nombre} fill={colores[index % colores.length]} />)}</Pie></PieChart>
            : config.grafica === 'line'
              ? <LineChart data={movimientosAgrupados}><CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="nombre" /><YAxis allowDecimals={false} /><Tooltip />
                <Line type="monotone" dataKey="movimientos" name="Movimientos" stroke="var(--link)" /></LineChart>
              : <BarChart data={chartData} margin={{ bottom: 30 }}><CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="nombre" interval={0} angle={-15} textAnchor="end" />
                <YAxis tickFormatter={(value: number) => config.tipo === 'compras-por-proveedor'
                  ? moneda.format(value) : numero.format(value)} />
                <Tooltip formatter={(value) => config.tipo === 'compras-por-proveedor'
                  ? moneda.format(Number(value)) : numero.format(Number(value))} />
                <Bar dataKey={config.tipo === 'stock-bajo' ? 'cantidad' : 'valor'} name={config.tipo === 'stock-bajo' ? 'Stock' : 'Total'}
                  fill="var(--link)" radius={[4, 4, 0, 0]} /></BarChart>}
        </ResponsiveContainer>}
    </section>
    <section className="card section-space"><div className="section-title"><h3>Detalle del reporte</h3></div>
      {filas.length === 0 ? <p className="empty-state">No hay registros con los filtros seleccionados.</p> :
        <div className="scroll"><table><thead><tr>{data.columnas.map((columna) => <th key={columna}>{columna}</th>)}</tr></thead>
          <tbody>{filas.map((fila, filaIndex) => <tr key={`${filaIndex}-${fila.join('|')}`}>{data.columnas.map((columna, columnaIndex) =>
            <td key={`${columna}-${columnaIndex}`}>{fila[columnaIndex] ?? ''}</td>)}</tr>)}</tbody></table></div>}
      <Pagination page={data.datos.numero} totalPages={data.datos.totalPaginas} onPage={(siguiente) =>
        setParams((actual) => { const next = new URLSearchParams(actual); next.set('page', String(siguiente)); return next })} />
      <small className="table-total">{data.datos.totalElementos} registros</small>
      {Object.keys(data.totales).length > 0 && <div className="report-totals">{Object.entries(data.totales).map(([clave, valor]) =>
        <p key={clave}><span>{clave}</span><b>{valor}</b></p>)}</div>}
    </section>
  </section>
}
