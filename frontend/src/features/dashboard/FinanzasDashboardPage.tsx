import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { dashboardApi } from './api'
import { ErrorState } from '../../components/ErrorState'
import { Metric } from '../../components/Metric'
import { Skeleton } from '../../components/Skeleton'

const moneda = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' })
const cantidad = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 })

export function FinanzasDashboardPage() {
  const dashboard = useQuery({
    queryKey: ['dashboard', 'finanzas', 7],
    queryFn: () => dashboardApi.obtener('finanzas', 7),
  })
  if (dashboard.isPending) return <Skeleton />
  if (dashboard.isError) return <ErrorState mensaje="No se pudo cargar el dashboard financiero."
    reintentar={() => void dashboard.refetch()} />
  const kpis = dashboard.data.kpis
  const serie = dashboard.data.serie
  const meses = new Intl.DateTimeFormat('es-PE', { month: 'short', year: '2-digit' })
  return <section>
    <div className="metrics">
      <Metric label="Valor del inventario" value={moneda.format(Number(kpis.valorInventario ?? 0))} icon={<span>S/</span>} />
      <Metric label="Compras del mes" value={moneda.format(Number(kpis.comprasDelMes ?? 0))} icon={<span>↗</span>} />
      <Metric label="Rotación últimos 12 meses" value={`${cantidad.format(Number(kpis.rotacion12Meses ?? 0))}×`} icon={<span>⟳</span>} />
    </div>
    <section className="card chart-card section-space"><div className="section-title"><div><h3>Compras mensuales</h3>
      <span>Últimos 12 meses desde órdenes de compra registradas.</span></div></div>
      {serie.length === 0 ? <p className="empty-state">No hay información mensual disponible.</p> :
        <ResponsiveContainer width="100%" height={300}><BarChart data={serie}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
          <XAxis dataKey="periodo" tickFormatter={(value: string) => meses.format(new Date(`${value}T12:00:00`))} />
          <YAxis tickFormatter={(value: number) => moneda.format(value)} />
          <Tooltip labelFormatter={(value) => meses.format(new Date(`${String(value)}T12:00:00`))}
            formatter={(value) => moneda.format(Number(value))} />
          <Bar dataKey="compras" name="Compras" fill="var(--link)" radius={[4, 4, 0, 0]} />
        </BarChart></ResponsiveContainer>}
    </section>
  </section>
}
