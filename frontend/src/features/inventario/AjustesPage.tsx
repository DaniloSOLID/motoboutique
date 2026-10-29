import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { catalogoApi } from '../../api/catalogo'
import { ApiError } from '../../api/client'
import { Badge } from '../../components/Badge'
import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'
import { inventarioApi } from './api'

export function AjustesPage() {
  const [params, setParams] = useSearchParams()
  const [buscar, setBuscar] = useState('')
  const [productoId, setProductoId] = useState('')
  const [almacenId, setAlmacenId] = useState('')
  const [tipo, setTipo] = useState<'POSITIVO' | 'NEGATIVO'>('POSITIVO')
  const [cantidad, setCantidad] = useState('')
  const [motivo, setMotivo] = useState('')
  const queryClient = useQueryClient()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const filtros = {
    estado: params.get('estado') ?? undefined,
    almacenId: params.get('almacenId') ? Number(params.get('almacenId')) : undefined,
    desde: params.get('desde') ? new Date(`${params.get('desde')}T00:00:00-05:00`).toISOString() : undefined,
    hasta: params.get('hasta') ? new Date(`${params.get('hasta')}T23:59:59.999-05:00`).toISOString() : undefined,
    page, size,
  }
  const ajustes = useQuery({ queryKey: ['ajustes', filtros], queryFn: () => inventarioApi.ajustes(filtros) })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: inventarioApi.almacenes })
  const productos = useQuery({
    queryKey: ['productos-opciones', buscar],
    queryFn: () => catalogoApi.productos({ q: buscar || undefined, page: 0, size: 100, activo: true }),
  })
  const solicitar = useMutation({
    mutationFn: () => inventarioApi.solicitarAjuste({
      productoId: Number(productoId), almacenId: Number(almacenId),
      cantidad: Number(cantidad) * (tipo === 'NEGATIVO' ? -1 : 1), motivo: motivo.trim(),
    }),
    onSuccess: async (ajuste) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['ajustes'] }),
        queryClient.invalidateQueries({ queryKey: ['aprobaciones'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ])
      setProductoId('')
      setCantidad('')
      setMotivo('')
      toast.success(`Solicitud ${ajuste.numero} creada como pendiente.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo solicitar el ajuste.'),
  })
  const cambiar = (campo: string, valor: string) => {
    setParams((actuales) => {
      const next = new URLSearchParams(actuales)
      if (valor) next.set(campo, valor); else next.delete(campo)
      next.delete('page')
      return next
    })
  }
  if (ajustes.isPending || almacenes.isPending || productos.isPending) return <Skeleton />
  if (ajustes.isError || almacenes.isError || productos.isError) {
    const error = ajustes.error ?? almacenes.error ?? productos.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudieron cargar los ajustes.'}
      reintentar={() => { void ajustes.refetch(); void almacenes.refetch(); void productos.refetch() }} />
  }
  const pagina = ajustes.data
  const formularioValido = Boolean(productoId && almacenId && Number(cantidad) > 0 && motivo.trim())
  return <section className="settings-grid">
    <form className="card form-card" onSubmit={(event) => { event.preventDefault(); if (formularioValido) solicitar.mutate() }}>
      <h3>Solicitar ajuste de inventario</h3>
      <label>Buscar producto<SearchInput value={buscar} onChange={setBuscar} placeholder="Código o nombre..." /></label>
      <label>Producto<select required value={productoId} onChange={(event) => setProductoId(event.target.value)}>
        <option value="">Selecciona un producto</option>{productos.data.contenido.map((producto) =>
          <option key={producto.id} value={producto.id}>{producto.codigo} · {producto.nombre}</option>)}</select></label>
      <label>Almacén<select required value={almacenId} onChange={(event) => setAlmacenId(event.target.value)}>
        <option value="">Selecciona un almacén</option>{almacenes.data.filter((a) => a.activo).map((a) =>
          <option key={a.id} value={a.id}>{a.nombre}</option>)}</select></label>
      <label>Tipo<select value={tipo} onChange={(event) => setTipo(event.target.value as typeof tipo)}>
        <option value="POSITIVO">Incrementar stock</option><option value="NEGATIVO">Disminuir stock</option></select></label>
      <label>Cantidad<input required type="number" min="0.001" step="0.001" value={cantidad}
        onChange={(event) => setCantidad(event.target.value)} /></label>
      <label>Motivo<input required maxLength={255} value={motivo} onChange={(event) => setMotivo(event.target.value)} /></label>
      <p className="field-hint">La solicitud quedará pendiente de aprobación; el stock no cambia hasta aprobarse.</p>
      <button className="btn primary" disabled={!formularioValido || solicitar.isPending}>
        {solicitar.isPending ? 'Enviando…' : 'Solicitar ajuste'}</button>
    </form>
    <section className="card"><h3>Mis solicitudes de ajuste</h3>
      <div className="filter-bar">
        <select aria-label="Filtrar por estado" value={params.get('estado') ?? ''} onChange={(event) => cambiar('estado', event.target.value)}>
          <option value="">Todos los estados</option><option value="PENDIENTE">Pendiente</option>
          <option value="APROBADO">Aprobado</option><option value="RECHAZADO">Rechazado</option></select>
        <select aria-label="Filtrar por almacén" value={params.get('almacenId') ?? ''} onChange={(event) => cambiar('almacenId', event.target.value)}>
          <option value="">Todos los almacenes</option>{almacenes.data.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}
        </select>
        <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => cambiar('desde', event.target.value)} /></label>
        <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => cambiar('hasta', event.target.value)} /></label>
      </div>
      {pagina.contenido.length === 0 ? <p className="empty-state">No tienes solicitudes con estos filtros.</p> :
        <div className="scroll"><table><thead><tr><th>Solicitud</th><th>Fecha</th><th>Producto</th><th>Almacén</th><th>Cantidad</th><th>Motivo</th><th>Estado</th><th>Comentario</th></tr></thead>
          <tbody>{pagina.contenido.map((ajuste) => <tr key={ajuste.id}><td>{ajuste.numero}</td>
            <td>{new Date(ajuste.fechaSolicitud).toLocaleDateString('es-PE')}</td><td>{ajuste.codigoProducto} · {ajuste.producto}</td>
            <td>{ajuste.almacen}</td><td>{ajuste.cantidad > 0 ? '+' : ''}{ajuste.cantidad}</td><td>{ajuste.motivo}</td>
            <td><Badge>{ajuste.estado}</Badge></td><td>{ajuste.comentario ?? '—'}</td></tr>)}</tbody></table></div>}
      <Pagination page={pagina.numero} totalPages={pagina.totalPaginas} onPage={(siguiente) =>
        setParams((actuales) => { const next = new URLSearchParams(actuales); next.set('page', String(siguiente)); return next })} />
      <small className="table-total">{pagina.totalElementos} solicitudes</small>
    </section>
  </section>
}
