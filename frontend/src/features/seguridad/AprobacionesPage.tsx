import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { administracionApi } from '../../api/administracion'
import { ApiError } from '../../api/client'
import { Badge } from '../../components/Badge'
import { ErrorState } from '../../components/ErrorState'
import { FormModal } from '../../components/FormModal'
import { Pagination } from '../../components/Pagination'
import { Skeleton } from '../../components/Skeleton'
import { aprobacionesApi, type AjusteAprobacion } from './aprobacionesApi'

type AccionResolucion = 'aprobar' | 'rechazar'

export function AprobacionesPage() {
  const [params, setParams] = useSearchParams()
  const [seleccion, setSeleccion] = useState<number[]>([])
  const [resolucion, setResolucion] = useState<{ accion: AccionResolucion; ajuste?: AjusteAprobacion } | null>(null)
  const [comentario, setComentario] = useState('')
  const { usuario, tieneRol } = useAuth()
  const queryClient = useQueryClient()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const filtros = {
    estado: params.get('estado') ?? 'PENDIENTE',
    almacenId: params.get('almacenId') ?? undefined,
    productoId: params.get('productoId') ?? undefined,
    desde: params.get('desde') ? new Date(`${params.get('desde')}T00:00:00-05:00`).toISOString() : undefined,
    hasta: params.get('hasta') ? new Date(`${params.get('hasta')}T23:59:59.999-05:00`).toISOString() : undefined,
    solicitanteId: params.get('solicitanteId') ?? undefined,
    page, size,
  }
  const ajustes = useQuery({ queryKey: ['aprobaciones', filtros], queryFn: () => aprobacionesApi.listar(filtros) })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: administracionApi.almacenes })
  const refrescar = async () => {
    setSeleccion([])
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['aprobaciones'] }),
      queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      queryClient.invalidateQueries({ queryKey: ['stock'] }),
      queryClient.invalidateQueries({ queryKey: ['inventario'] }),
    ])
  }
  const individual = useMutation({
    mutationFn: (datos: { id: number; accion: AccionResolucion; comentario: string }) =>
      aprobacionesApi.resolver(datos.id, datos.accion, datos.comentario),
    onSuccess: async () => { await refrescar(); setResolucion(null); toast.success('Ajuste resuelto.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo resolver el ajuste.'),
  })
  const lote = useMutation({
    mutationFn: (datos: { accion: AccionResolucion; comentario: string }) =>
      aprobacionesApi.resolverLote(seleccion, datos.accion === 'aprobar' ? 'APROBAR' : 'RECHAZAR', datos.comentario),
    onSuccess: async () => { await refrescar(); setResolucion(null); toast.success('Ajustes resueltos.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudieron resolver los ajustes seleccionados.'),
  })
  const cambiar = (campo: string, valor: string) => setParams((actuales) => {
    setSeleccion([])
    const nuevos = new URLSearchParams(actuales)
    if (valor) nuevos.set(campo, valor); else nuevos.delete(campo)
    nuevos.delete('page')
    return nuevos
  })
  const abrirResolucion = (accion: AccionResolucion, ajuste?: AjusteAprobacion) => {
    setComentario('')
    setResolucion({ accion, ajuste })
  }
  const confirmar = () => {
    if (!resolucion) return
    if (resolucion.accion === 'rechazar' && !comentario.trim()) {
      toast.error('El comentario es obligatorio para rechazar.')
      return
    }
    if (resolucion.ajuste) individual.mutate({
      id: resolucion.ajuste.id, accion: resolucion.accion, comentario: comentario.trim(),
    })
    else lote.mutate({ accion: resolucion.accion, comentario: comentario.trim() })
  }
  const filas = ajustes.data?.contenido ?? []
  const ocupada = individual.isPending || lote.isPending
  if (ajustes.isPending || almacenes.isPending) return <Skeleton />
  if (ajustes.isError || almacenes.isError) {
    const error = ajustes.error ?? almacenes.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudieron cargar las aprobaciones.'}
      reintentar={() => { void ajustes.refetch(); void almacenes.refetch() }} />
  }
  return <section>
    <div className="section-title"><div><h3>Bandeja de aprobaciones</h3><span>Verifica el impacto en existencias antes de resolver.</span></div></div>
    <div className="filter-bar">
      <select aria-label="Estado" value={params.get('estado') ?? 'PENDIENTE'} onChange={(event) => cambiar('estado', event.target.value)}>
        <option value="PENDIENTE">Pendiente</option><option value="APROBADO">Aprobado</option><option value="RECHAZADO">Rechazado</option>
      </select>
      <select aria-label="Almacén" value={params.get('almacenId') ?? ''} onChange={(event) => cambiar('almacenId', event.target.value)}>
        <option value="">Todos los almacenes</option>{almacenes.data.map((almacen) =>
          <option key={almacen.id} value={almacen.id}>{almacen.nombre}</option>)}
      </select>
      <input aria-label="Producto ID" type="number" min="1" placeholder="ID producto" value={params.get('productoId') ?? ''} onChange={(event) => cambiar('productoId', event.target.value)} />
      <input aria-label="Solicitante ID" type="number" min="1" placeholder="ID solicitante" value={params.get('solicitanteId') ?? ''} onChange={(event) => cambiar('solicitanteId', event.target.value)} />
      <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => cambiar('desde', event.target.value)} /></label>
      <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => cambiar('hasta', event.target.value)} /></label>
    </div>
    {seleccion.length > 0 && <div className="toolbar-actions">
      <span>{seleccion.length} seleccionados</span>
      <button className="btn primary" disabled={ocupada || (!tieneRol('ADMIN') && ajustes.data.contenido.some((fila) => seleccion.includes(fila.id) && fila.solicitanteId === usuario?.id))}
        title={!tieneRol('ADMIN') && ajustes.data.contenido.some((fila) => seleccion.includes(fila.id) && fila.solicitanteId === usuario?.id)
          ? 'No puedes aprobar un ajuste solicitado por ti.' : undefined}
        onClick={() => abrirResolucion('aprobar')}>Aprobar seleccionados</button>
      <button className="btn danger" disabled={ocupada} onClick={() => abrirResolucion('rechazar')}>Rechazar seleccionados</button>
    </div>}
    {filas.length === 0 ? <p className="empty-state">No hay ajustes para estos filtros.</p> :
      <div className="scroll"><table><thead><tr><th><input type="checkbox" aria-label="Seleccionar página"
        checked={filas.length > 0 && filas.every((item) => seleccion.includes(item.id))}
        onChange={(event) => setSeleccion(event.target.checked
          ? [...new Set([...seleccion, ...filas.map((item) => item.id)])]
          : seleccion.filter((id) => !filas.some((item) => item.id === id)))} /></th>
        <th>Número</th><th>Producto</th><th>Almacén</th><th>Stock actual → resultante</th><th>Motivo</th><th>Solicitante</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>{filas.map((ajuste) => <tr key={ajuste.id}>
          <td><input type="checkbox" aria-label={`Seleccionar ${ajuste.numero}`} checked={seleccion.includes(ajuste.id)}
            onChange={(event) => setSeleccion(event.target.checked ? [...seleccion, ajuste.id] : seleccion.filter((id) => id !== ajuste.id))} /></td>
          <td>{ajuste.numero}</td><td>{ajuste.codigoProducto} · {ajuste.producto}<small>{ajuste.cantidad > 0 ? '+' : ''}{ajuste.cantidad}</small></td>
          <td>{ajuste.almacen}</td><td>{ajuste.stockActual} → {ajuste.stockResultante}</td><td>{ajuste.motivo}</td>
          <td>{ajuste.solicitante}</td><td><Badge>{ajuste.estado}</Badge></td><td>
            {ajuste.estado === 'PENDIENTE' && <><button className="btn ghost" disabled={ocupada || (!tieneRol('ADMIN') && ajuste.solicitanteId === usuario?.id)}
              title={!tieneRol('ADMIN') && ajuste.solicitanteId === usuario?.id ? 'No puedes aprobar un ajuste solicitado por ti.' : undefined}
              onClick={() => abrirResolucion('aprobar', ajuste)}>Aprobar</button>
              <button className="btn danger" disabled={ocupada} onClick={() => abrirResolucion('rechazar', ajuste)}>Rechazar</button></>}
          </td>
        </tr>)}</tbody></table></div>}
    {ajustes.data && <><Pagination page={ajustes.data.numero} totalPages={ajustes.data.totalPaginas} onPage={(siguiente) =>
      { setSeleccion([]); setParams((actuales) => { const nuevos = new URLSearchParams(actuales); nuevos.set('page', String(siguiente)); return nuevos }) }} />
      <small className="table-total">{ajustes.data.totalElementos} ajustes</small></>}
    {resolucion && <FormModal title={`${resolucion.accion === 'aprobar' ? 'Aprobar' : 'Rechazar'} ajustes`} onClose={() => setResolucion(null)}>
      {resolucion.ajuste && <p>{resolucion.ajuste.producto}: stock {resolucion.ajuste.stockActual} → {resolucion.ajuste.stockResultante}.</p>}
      {!resolucion.ajuste && <p>Se resolverán {seleccion.length} ajustes seleccionados.</p>}
      <label>Comentario{resolucion.accion === 'rechazar' ? ' (obligatorio)' : ' (opcional)'}
        <textarea maxLength={2000} value={comentario} onChange={(event) => setComentario(event.target.value)} /></label>
      <div className="modal-actions"><button className="btn secondary" onClick={() => setResolucion(null)}>Cancelar</button>
        <button className={resolucion.accion === 'aprobar' ? 'btn primary' : 'btn danger'} disabled={ocupada || (resolucion.accion === 'rechazar' && !comentario.trim())}
          onClick={confirmar}>{ocupada ? 'Procesando…' : 'Confirmar'}</button></div>
    </FormModal>}
  </section>
}
