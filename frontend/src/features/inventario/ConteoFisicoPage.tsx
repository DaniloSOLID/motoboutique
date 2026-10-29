import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ApiError } from '../../api/client'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'
import { inventarioApi } from './api'

export function ConteoFisicoPage() {
  const [params, setParams] = useSearchParams()
  const [almacenId, setAlmacenId] = useState('')
  const [buscar, setBuscar] = useState('')
  const [cantidades, setCantidades] = useState<Record<number, string>>({})
  const [confirmarCierre, setConfirmarCierre] = useState(false)
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
  const conteos = useQuery({
    queryKey: ['conteos', filtros],
    queryFn: () => inventarioApi.conteos(filtros),
  })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: inventarioApi.almacenes })
  const selectedId = params.get('conteoId')
  const detalle = useQuery({
    queryKey: ['conteo', selectedId],
    queryFn: () => inventarioApi.conteo(Number(selectedId)),
    enabled: Boolean(selectedId),
  })
  const refrescar = async () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['conteos'] }),
    queryClient.invalidateQueries({ queryKey: ['conteo'] }),
    queryClient.invalidateQueries({ queryKey: ['ajustes'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
  ])
  const abrir = useMutation({
    mutationFn: () => inventarioApi.abrirConteo(Number(almacenId)),
    onSuccess: async (conteo) => {
      await refrescar()
      setParams((actuales) => { const next = new URLSearchParams(actuales); next.set('conteoId', String(conteo.id)); return next })
      setAlmacenId('')
      setCantidades({})
      toast.success(`Conteo ${conteo.numero} abierto con ${conteo.detalles.length} productos.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo abrir el conteo.'),
  })
  const guardar = useMutation({
    mutationFn: (fila: { productoId: number; cantidad: number }) =>
      inventarioApi.guardarConteo(Number(selectedId), fila.productoId, fila.cantidad),
    onSuccess: async (_conteo, fila) => {
      await refrescar()
      setCantidades((actuales) => { const next = { ...actuales }; delete next[fila.productoId]; return next })
      toast.success('Conteo de producto guardado.')
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo guardar el conteo.'),
  })
  const cerrar = useMutation({
    mutationFn: () => inventarioApi.cerrarConteo(Number(selectedId)),
    onSuccess: async (resultado) => {
      await refrescar()
      const diferencias = resultado.detalles.filter((fila) => fila.diferencia !== 0).length
      setConfirmarCierre(false)
      setParams((actuales) => { const next = new URLSearchParams(actuales); next.delete('conteoId'); return next })
      toast.success(`Conteo cerrado. Se generaron ${diferencias} ajustes pendientes.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo cerrar el conteo.'),
  })
  const cambioPagina = (siguiente: number) => setParams((actuales) => {
    const next = new URLSearchParams(actuales); next.set('page', String(siguiente)); return next
  })
  const filas = useMemo(() => (detalle.data?.detalles ?? []).filter((fila) =>
    `${fila.codigo} ${fila.producto}`.toLocaleLowerCase('es-PE').includes(buscar.toLocaleLowerCase('es-PE'))), [detalle.data, buscar])
  const sesiones = (conteos.data?.contenido ?? []).filter((conteo) =>
    `${conteo.numero} ${conteo.almacen}`.toLocaleLowerCase('es-PE').includes(buscar.toLocaleLowerCase('es-PE')))
  const filtrar = (campo: string, valor: string) => setParams((actuales) => {
    const next = new URLSearchParams(actuales)
    if (valor) next.set(campo, valor); else next.delete(campo)
    next.delete('page')
    return next
  })
  if (conteos.isPending || almacenes.isPending) return <Skeleton />
  if (conteos.isError || almacenes.isError) {
    const error = conteos.error ?? almacenes.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudieron cargar conteos.'}
      reintentar={() => { void conteos.refetch(); void almacenes.refetch() }} />
  }
  if (selectedId && detalle.isError) return <ErrorState mensaje={detalle.error instanceof ApiError
    ? detalle.error.detalle : 'No se pudo abrir el detalle del conteo.'} reintentar={() => void detalle.refetch()} />
  return <section>
    {!selectedId && <section className="card section-space">
      <h3>Iniciar conteo físico</h3><div className="filter-bar">
        <select aria-label="Almacén para conteo" value={almacenId} onChange={(event) => setAlmacenId(event.target.value)}>
          <option value="">Selecciona un almacén</option>{almacenes.data.filter((a) => a.activo).map((a) =>
            <option key={a.id} value={a.id}>{a.nombre}</option>)}</select>
        <button className="btn primary" disabled={!almacenId || abrir.isPending} onClick={() => abrir.mutate()}>
          {abrir.isPending ? 'Abriendo…' : 'Abrir sesión de conteo'}</button>
      </div>
    </section>}
    {selectedId && <section className="card section-space">
      {detalle.isPending ? <Skeleton /> : detalle.data && <>
        <div className="section-title"><div><h3>Conteo {detalle.data.numero} · {detalle.data.almacen}</h3>
          <span>Abierto el {new Date(detalle.data.fechaApertura).toLocaleString('es-PE')}</span></div>
          {detalle.data.estado === 'ABIERTO' && <button className="btn primary"
            disabled={Object.keys(cantidades).length > 0}
            onClick={() => setConfirmarCierre(true)}>Cerrar conteo</button>}
        </div>
        {Object.keys(cantidades).length > 0 && <p className="field-hint" role="status">
          Guarda los conteos editados antes de cerrar la sesión.
        </p>}
        <div className="filter-bar"><SearchInput value={buscar} onChange={setBuscar} placeholder="Buscar código o producto..." /></div>
        <div className="scroll"><table><thead><tr><th>Código</th><th>Producto</th><th>Stock sistema</th><th>Stock contado</th><th>Diferencia</th><th></th></tr></thead>
          <tbody>{filas.map((fila) => {
            const valor = cantidades[fila.productoId] ?? String(fila.stockContado)
            const diferencia = Number(valor) - fila.stockSistema
            return <tr key={fila.productoId}><td>{fila.codigo}</td><td>{fila.producto}</td><td>{fila.stockSistema}</td>
              <td><input aria-label={`Contado: ${fila.producto}`} type="number" min="0" step="0.001" disabled={detalle.data?.estado !== 'ABIERTO'}
                value={valor} onChange={(event) => setCantidades((prev) => ({ ...prev, [fila.productoId]: event.target.value }))} /></td>
              <td>{diferencia.toLocaleString('es-PE', { minimumFractionDigits: 3 })}</td>
              <td><button className="btn secondary" disabled={detalle.data?.estado !== 'ABIERTO' || guardar.isPending || !Number.isFinite(Number(valor)) || Number(valor) < 0}
                onClick={() => guardar.mutate({ productoId: fila.productoId, cantidad: Number(valor) })}>Guardar</button></td></tr>
          })}</tbody></table></div>
        {detalle.data.estado !== 'ABIERTO' && <p>Este conteo está cerrado. Los ajustes generados quedan pendientes de aprobación.</p>}
      </>}
    </section>}
    {!selectedId && <section className="card">
      <h3>Sesiones anteriores</h3>
      <div className="filter-bar"><SearchInput value={buscar} onChange={setBuscar} placeholder="Buscar número o almacén..." />
        <select aria-label="Estado de conteo" value={params.get('estado') ?? ''} onChange={(event) => filtrar('estado', event.target.value)}>
          <option value="">Todos los estados</option><option value="ABIERTO">Abierto</option><option value="CERRADO">Cerrado</option></select>
        <select aria-label="Almacén de conteo" value={params.get('almacenId') ?? ''} onChange={(event) => filtrar('almacenId', event.target.value)}>
          <option value="">Todos los almacenes</option>{almacenes.data.map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}</select>
        <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => filtrar('desde', event.target.value)} /></label>
        <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => filtrar('hasta', event.target.value)} /></label>
      </div>
      {sesiones.length === 0 ? <p className="empty-state">No hay conteos registrados con esos filtros.</p> :
        <div className="scroll"><table><thead><tr><th>Número</th><th>Almacén</th><th>Apertura</th><th>Cierre</th><th>Estado</th><th></th></tr></thead>
          <tbody>{sesiones.map((conteo) => <tr key={conteo.id}><td>{conteo.numero}</td><td>{conteo.almacen}</td>
            <td>{new Date(conteo.fechaApertura).toLocaleString('es-PE')}</td><td>{conteo.fechaCierre ? new Date(conteo.fechaCierre).toLocaleString('es-PE') : '—'}</td>
            <td>{conteo.estado}</td><td><button className="btn ghost" onClick={() =>
              setParams((actuales) => { const next = new URLSearchParams(actuales); next.set('conteoId', String(conteo.id)); return next })}>
              {conteo.estado === 'ABIERTO' ? 'Continuar' : 'Ver detalle'}</button></td></tr>)}</tbody></table></div>}
      <Pagination page={conteos.data.numero} totalPages={conteos.data.totalPaginas} onPage={cambioPagina} />
    </section>}
    {confirmarCierre && detalle.data && <ConfirmDialog title="Cerrar conteo" cancelar={() => setConfirmarCierre(false)}
      confirmar={() => cerrar.mutate()}>Se cerrará la sesión y se crearán {detalle.data.detalles.filter((fila) =>
        (cantidades[fila.productoId] === undefined ? fila.stockContado : Number(cantidades[fila.productoId])) !== fila.stockSistema).length} ajustes pendientes para las diferencias.</ConfirmDialog>}
  </section>
}
