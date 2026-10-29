import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { administracionApi } from '../../api/administracion'
import { ApiError } from '../../api/client'
import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'

export function AuditoriaAdmin() {
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const filtros = {
    q: params.get('q') ?? undefined,
    usuario: params.get('usuario') ?? undefined,
    accion: params.get('accion') ?? undefined,
    desde: params.get('desde') ? new Date(`${params.get('desde')}T00:00:00-05:00`).toISOString() : undefined,
    hasta: params.get('hasta') ? new Date(`${params.get('hasta')}T23:59:59.999-05:00`).toISOString() : undefined,
    page,
    size: Number(params.get('size') ?? 20),
  }
  const consulta = useQuery({
    queryKey: ['auditoria', filtros],
    queryFn: () => administracionApi.auditoria(filtros),
  })
  const cambiar = (campo: string, valor: string) => setParams((actuales) => {
    const nuevos = new URLSearchParams(actuales)
    if (valor) nuevos.set(campo, valor); else nuevos.delete(campo)
    nuevos.delete('page')
    return nuevos
  })
  if (consulta.isPending) return <section className="card section-space"><p role="status">Cargando auditoría…</p></section>
  if (consulta.isError) return <ErrorState mensaje={consulta.error instanceof ApiError ? consulta.error.detalle : 'No se pudo cargar la auditoría.'}
    reintentar={() => void consulta.refetch()} />
  const pagina = consulta.data
  return <section className="card section-space">
    <div className="section-title"><div><h3>Auditoría</h3><span>Eventos de administración del sistema.</span></div></div>
    <div className="filter-bar">
      <SearchInput value={params.get('q') ?? ''} onChange={(valor) => cambiar('q', valor)} />
      <input aria-label="Filtrar por usuario" placeholder="Usuario" value={params.get('usuario') ?? ''} onChange={(event) => cambiar('usuario', event.target.value)} />
      <input aria-label="Filtrar por acción" placeholder="Acción" value={params.get('accion') ?? ''} onChange={(event) => cambiar('accion', event.target.value)} />
      <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => cambiar('desde', event.target.value)} /></label>
      <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => cambiar('hasta', event.target.value)} /></label>
    </div>
    {pagina.contenido.length === 0 ? <p className="empty-state">No hay eventos con esos filtros.</p> :
      <div className="scroll"><table><thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Entidad</th><th>Detalle</th></tr></thead>
        <tbody>{pagina.contenido.map((evento, indice) => <tr key={`${evento.cuando}-${evento.quien}-${evento.que}-${indice}`}>
          <td>{new Date(evento.cuando).toLocaleString('es-PE')}</td><td>{evento.quien}</td><td>{evento.que}</td>
          <td>{evento.entidad}{evento.entidadId ? ` · ${evento.entidadId}` : ''}</td><td>{evento.detalle ?? '—'}</td>
        </tr>)}</tbody></table></div>}
    <Pagination page={pagina.numero} totalPages={pagina.totalPaginas} onPage={(siguiente) =>
      setParams((actuales) => { const nuevos = new URLSearchParams(actuales); nuevos.set('page', String(siguiente)); return nuevos })} />
    <small className="table-total">{pagina.totalElementos} eventos</small>
  </section>
}
