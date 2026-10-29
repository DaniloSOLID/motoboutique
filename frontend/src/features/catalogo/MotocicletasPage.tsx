import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { apiFetch } from '../../api/client'
import { catalogoApi } from '../../api/catalogo'
import type { ModeloMoto, ModeloMotoGuardar, PageResponse } from '../../api/types'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { Badge } from '../../components/Badge'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorState } from '../../components/ErrorState'
import { FormModal } from '../../components/FormModal'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'

interface UnidadMoto {
  id: number
  modeloMotoId: number
  modeloMoto: string
  chasis: string
  motor: string
  color: string | null
  estado: string
  observaciones: string | null
}
interface UnidadGuardar {
  modeloMotoId: number
  chasis: string
  motor: string
  color: string | null
  estado: string
  observaciones: string | null
}
interface FormModelo {
  marcaId: string; nombre: string; anioInicio: string; anioFin: string; cilindradaCc: string; tipo: string
}
interface FormUnidad {
  modeloMotoId: string; chasis: string; motor: string; color: string; estado: string; observaciones: string
}

const modeloVacio: FormModelo = { marcaId: '', nombre: '', anioInicio: '', anioFin: '', cilindradaCc: '', tipo: '' }
const unidadVacia: FormUnidad = { modeloMotoId: '', chasis: '', motor: '', color: '', estado: 'DISPONIBLE', observaciones: '' }

export function MotocicletasPage() {
  const [params, setParams] = useSearchParams()
  const [tab, setTab] = useState<'modelos' | 'unidades'>('modelos')
  const [modeloForm, setModeloForm] = useState<FormModelo>(modeloVacio)
  const [unidadForm, setUnidadForm] = useState<FormUnidad>(unidadVacia)
  const [editarModelo, setEditarModelo] = useState<ModeloMoto | 'nuevo' | null>(null)
  const [editarUnidad, setEditarUnidad] = useState<UnidadMoto | 'nuevo' | null>(null)
  const [eliminarModelo, setEliminarModelo] = useState<ModeloMoto | null>(null)
  const [eliminarUnidad, setEliminarUnidad] = useState<UnidadMoto | null>(null)
  const [modeloDetalle, setModeloDetalle] = useState<number | null>(null)
  const { tienePermiso } = useAuth()
  const puedeEditar = tienePermiso('PRODUCTO_EDITAR')
  const queryClient = useQueryClient()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const query = params.get('q') ?? ''
  const marcas = useQuery({ queryKey: ['marcas-opciones'], queryFn: () => catalogoApi.marcas({ page: 0, size: 200 }) })
  const modelos = useQuery({
    queryKey: ['moto-modelos', query, params.get('marcaId'), params.get('anio'), page, size],
    queryFn: () => catalogoApi.modelos({
      q: query || undefined, page, size,
      marcaId: params.get('marcaId') ? Number(params.get('marcaId')) : undefined,
      anio: params.get('anio') ? Number(params.get('anio')) : undefined,
    }),
  })
  const unidades = useQuery({
    queryKey: ['moto-unidades', query, params.get('estado'), params.get('modeloId'), page, size],
    queryFn: () => {
      const filtros = new URLSearchParams({ page: String(page), size: String(size), sort: 'chasis' })
      if (query) filtros.set('q', query)
      if (params.get('estado')) filtros.set('estado', params.get('estado')!)
      if (params.get('modeloId')) filtros.set('modeloId', params.get('modeloId')!)
      return apiFetch<PageResponse<UnidadMoto>>(`/api/motos/unidades?${filtros}`)
    },
    enabled: tab === 'unidades',
  })
  const compatible = useQuery({
    queryKey: ['modelo-productos', modeloDetalle],
    queryFn: () => catalogoApi.productosDeModelo(modeloDetalle ?? 0),
    enabled: modeloDetalle !== null,
  })
  const invalidar = async () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['moto-modelos'] }),
    queryClient.invalidateQueries({ queryKey: ['moto-unidades'] }),
    queryClient.invalidateQueries({ queryKey: ['modelo-productos'] }),
  ])
  const guardarModelo = useMutation({
    mutationFn: (datos: ModeloMotoGuardar) => editarModelo === 'nuevo' || !editarModelo
      ? catalogoApi.crearModelo(datos) : catalogoApi.actualizarModelo(editarModelo.id, datos),
    onSuccess: async () => { await invalidar(); setEditarModelo(null); toast.success('Modelo guardado.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo guardar el modelo.'),
  })
  const guardarUnidad = useMutation({
    mutationFn: (datos: UnidadGuardar) => editarUnidad === 'nuevo' || !editarUnidad
      ? apiFetch<UnidadMoto>('/api/motos/unidades', { method: 'POST', json: datos })
      : apiFetch<UnidadMoto>(`/api/motos/unidades/${editarUnidad.id}`, { method: 'PUT', json: datos }),
    onSuccess: async () => { await invalidar(); setEditarUnidad(null); toast.success('Unidad guardada.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo guardar la unidad.'),
  })
  const borrarModelo = useMutation({
    mutationFn: (id: number) => catalogoApi.eliminarModelo(id),
    onSuccess: async () => { await invalidar(); setEliminarModelo(null); toast.success('Modelo eliminado.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo eliminar el modelo.'),
  })
  const borrarUnidad = useMutation({
    mutationFn: (id: number) => apiFetch<void>(`/api/motos/unidades/${id}`, { method: 'DELETE' }),
    onSuccess: async () => { await invalidar(); setEliminarUnidad(null); toast.success('Unidad eliminada.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo eliminar la unidad.'),
  })
  const cambiarEstado = useMutation({
    mutationFn: ({ unidad, estado }: { unidad: UnidadMoto; estado: string }) =>
      apiFetch<UnidadMoto>(`/api/motos/unidades/${unidad.id}/estado?estado=${encodeURIComponent(estado)}`, { method: 'PATCH' }),
    onSuccess: async () => { await invalidar(); toast.success('Estado de unidad actualizado.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo cambiar el estado.'),
  })
  const cambiar = (campo: string, valor: string) => setParams((actuales) => {
    const next = new URLSearchParams(actuales)
    if (valor) next.set(campo, valor); else next.delete(campo)
    next.delete('page')
    return next
  })
  const abrirModelo = (modelo: ModeloMoto | 'nuevo') => {
    setEditarModelo(modelo)
    setModeloForm(modelo === 'nuevo' ? modeloVacio : {
      marcaId: String(modelo.marcaId), nombre: modelo.nombre,
      anioInicio: modelo.anioInicio === null ? '' : String(modelo.anioInicio),
      anioFin: modelo.anioFin === null ? '' : String(modelo.anioFin),
      cilindradaCc: modelo.cilindradaCc === null ? '' : String(modelo.cilindradaCc),
      tipo: modelo.tipo ?? '',
    })
  }
  const abrirUnidad = (unidad: UnidadMoto | 'nuevo') => {
    setEditarUnidad(unidad)
    setUnidadForm(unidad === 'nuevo' ? unidadVacia : {
      modeloMotoId: String(unidad.modeloMotoId), chasis: unidad.chasis, motor: unidad.motor,
      color: unidad.color ?? '', estado: unidad.estado, observaciones: unidad.observaciones ?? '',
    })
  }
  if (marcas.isPending || modelos.isPending || (tab === 'unidades' && unidades.isPending)) return <Skeleton />
  const fallo = marcas.isError || modelos.isError || (tab === 'unidades' && unidades.isError)
  if (fallo) return <ErrorState mensaje="No se pudieron cargar los datos de motocicletas."
    reintentar={() => { void marcas.refetch(); void modelos.refetch(); void unidades.refetch() }} />
  const pagina = tab === 'modelos' ? modelos.data : unidades.data
  return <section className="card">
    <div className="section-title"><div><h3>Motocicletas</h3><span>Modelos, compatibilidades y unidades serializadas.</span></div>
      {puedeEditar && <button className="btn primary" onClick={() => tab === 'modelos' ? abrirModelo('nuevo') : abrirUnidad('nuevo')}>
        {tab === 'modelos' ? 'Nuevo modelo' : 'Nueva unidad'}</button>}</div>
    <div className="tabs"><button className={tab === 'modelos' ? 'active' : ''} onClick={() => setTab('modelos')}>Modelos</button>
      <button className={tab === 'unidades' ? 'active' : ''} onClick={() => setTab('unidades')}>Unidades</button></div>
    <div className="filter-bar"><SearchInput value={query} onChange={(value) => cambiar('q', value)} placeholder="Buscar marca, modelo, chasis o motor..." />
      {tab === 'modelos' ? <>
        <select aria-label="Marca" value={params.get('marcaId') ?? ''} onChange={(event) => cambiar('marcaId', event.target.value)}>
          <option value="">Todas las marcas</option>{marcas.data.contenido.map((marca) => <option key={marca.id} value={marca.id}>{marca.nombre}</option>)}</select>
        <input aria-label="Año" type="number" placeholder="Año" value={params.get('anio') ?? ''} onChange={(event) => cambiar('anio', event.target.value)} />
      </> : <>
        <select aria-label="Modelo" value={params.get('modeloId') ?? ''} onChange={(event) => cambiar('modeloId', event.target.value)}>
          <option value="">Todos los modelos</option>{modelos.data.contenido.map((modelo) => <option key={modelo.id} value={modelo.id}>{modelo.marca} {modelo.nombre}</option>)}</select>
        <select aria-label="Estado" value={params.get('estado') ?? ''} onChange={(event) => cambiar('estado', event.target.value)}>
          <option value="">Todos los estados</option>{['DISPONIBLE', 'RESERVADA', 'VENDIDA'].map((estado) => <option key={estado}>{estado}</option>)}</select>
      </>}
    </div>
    {tab === 'modelos' ? modelos.data.contenido.length === 0 ? <p className="empty-state">No hay modelos registrados.</p> :
      <div className="scroll"><table><thead><tr><th>Marca</th><th>Modelo</th><th>Años</th><th>Cilindrada</th><th>Tipo</th><th>Compatibles</th><th>Acciones</th></tr></thead>
        <tbody>{modelos.data.contenido.map((modelo) => <tr key={modelo.id}><td>{modelo.marca}</td><td>{modelo.nombre}</td>
          <td>{modelo.anioInicio ?? '—'}{modelo.anioFin ? `–${modelo.anioFin}` : ''}</td><td>{modelo.cilindradaCc ? `${modelo.cilindradaCc} cc` : '—'}</td>
          <td>{modelo.tipo ?? '—'}</td><td><button className="btn ghost" onClick={() => setModeloDetalle(modelo.id)}>Ver repuestos</button></td>
          <td>{puedeEditar && <><button className="btn ghost" onClick={() => abrirModelo(modelo)}>Editar</button>
            <button className="btn danger" onClick={() => setEliminarModelo(modelo)}>Eliminar</button></>}</td></tr>)}</tbody></table></div>
      : unidades?.data?.contenido.length === 0 ? <p className="empty-state">No hay unidades registradas.</p> :
        <div className="scroll"><table><thead><tr><th>Modelo</th><th>Chasis</th><th>Motor</th><th>Color</th><th>Estado</th><th>Acciones</th></tr></thead>
          <tbody>{unidades.data?.contenido.map((unidad) => <tr key={unidad.id}><td>{unidad.modeloMoto}</td><td>{unidad.chasis}</td>
            <td>{unidad.motor}</td><td>{unidad.color ?? '—'}</td><td><Badge>{unidad.estado}</Badge></td>
            <td>{puedeEditar && <><select aria-label={`Cambiar estado ${unidad.chasis}`} value={unidad.estado}
              onChange={(event) => cambiarEstado.mutate({ unidad, estado: event.target.value })}>
              {['DISPONIBLE', 'RESERVADA', 'VENDIDA'].map((estado) => <option key={estado}>{estado}</option>)}</select>
              <button className="btn ghost" onClick={() => abrirUnidad(unidad)}>Editar</button>
              <button className="btn danger" onClick={() => setEliminarUnidad(unidad)}>Eliminar</button></>}</td>
          </tr>)}</tbody></table></div>}
    {pagina && <Pagination page={pagina.numero} totalPages={pagina.totalPaginas} onPage={(siguiente) =>
      setParams((actuales) => { const next = new URLSearchParams(actuales); next.set('page', String(siguiente)); return next })} />}
    {editarModelo && <FormModal title={editarModelo === 'nuevo' ? 'Crear modelo' : 'Editar modelo'} onClose={() => setEditarModelo(null)}>
      <form className="form-grid" onSubmit={(event) => {
        event.preventDefault()
        if (!modeloForm.marcaId || !modeloForm.nombre.trim()) return
        guardarModelo.mutate({
          marcaId: Number(modeloForm.marcaId), nombre: modeloForm.nombre.trim(),
          anioInicio: modeloForm.anioInicio ? Number(modeloForm.anioInicio) : null,
          anioFin: modeloForm.anioFin ? Number(modeloForm.anioFin) : null,
          cilindradaCc: modeloForm.cilindradaCc ? Number(modeloForm.cilindradaCc) : null,
          tipo: modeloForm.tipo.trim() || null,
        })
      }}>
        <label>Marca<select required value={modeloForm.marcaId} onChange={(event) => setModeloForm({ ...modeloForm, marcaId: event.target.value })}>
          <option value="">Selecciona</option>{marcas.data.contenido.map((marca) => <option key={marca.id} value={marca.id}>{marca.nombre}</option>)}</select></label>
        <label>Nombre<input required value={modeloForm.nombre} onChange={(event) => setModeloForm({ ...modeloForm, nombre: event.target.value })} /></label>
        <label>Año desde<input type="number" value={modeloForm.anioInicio} onChange={(event) => setModeloForm({ ...modeloForm, anioInicio: event.target.value })} /></label>
        <label>Año hasta<input type="number" value={modeloForm.anioFin} onChange={(event) => setModeloForm({ ...modeloForm, anioFin: event.target.value })} /></label>
        <label>Cilindrada (cc)<input type="number" min="1" value={modeloForm.cilindradaCc} onChange={(event) => setModeloForm({ ...modeloForm, cilindradaCc: event.target.value })} /></label>
        <label>Tipo<input value={modeloForm.tipo} onChange={(event) => setModeloForm({ ...modeloForm, tipo: event.target.value })} /></label>
        <div className="modal-actions"><button type="button" className="btn secondary" onClick={() => setEditarModelo(null)}>Cancelar</button>
          <button className="btn primary" disabled={guardarModelo.isPending}>Guardar</button></div>
      </form>
    </FormModal>}
    {editarUnidad && <FormModal title={editarUnidad === 'nuevo' ? 'Crear unidad' : 'Editar unidad'} onClose={() => setEditarUnidad(null)}>
      <form className="form-grid" onSubmit={(event) => {
        event.preventDefault()
        if (!unidadForm.modeloMotoId || !unidadForm.chasis.trim() || !unidadForm.motor.trim()) return
        guardarUnidad.mutate({
          modeloMotoId: Number(unidadForm.modeloMotoId), chasis: unidadForm.chasis.trim(), motor: unidadForm.motor.trim(),
          color: unidadForm.color.trim() || null, estado: unidadForm.estado, observaciones: unidadForm.observaciones.trim() || null,
        })
      }}>
        <label>Modelo<select required value={unidadForm.modeloMotoId} onChange={(event) => setUnidadForm({ ...unidadForm, modeloMotoId: event.target.value })}>
          <option value="">Selecciona</option>{modelos.data.contenido.map((modelo) => <option key={modelo.id} value={modelo.id}>{modelo.marca} {modelo.nombre}</option>)}</select></label>
        <label>Chasis<input required value={unidadForm.chasis} onChange={(event) => setUnidadForm({ ...unidadForm, chasis: event.target.value })} /></label>
        <label>Motor<input required value={unidadForm.motor} onChange={(event) => setUnidadForm({ ...unidadForm, motor: event.target.value })} /></label>
        <label>Color<input value={unidadForm.color} onChange={(event) => setUnidadForm({ ...unidadForm, color: event.target.value })} /></label>
        <label>Estado<select value={unidadForm.estado} onChange={(event) => setUnidadForm({ ...unidadForm, estado: event.target.value })}>
          {['DISPONIBLE', 'RESERVADA', 'VENDIDA'].map((estado) => <option key={estado}>{estado}</option>)}</select></label>
        <label>Observaciones<textarea value={unidadForm.observaciones} onChange={(event) => setUnidadForm({ ...unidadForm, observaciones: event.target.value })} /></label>
        <div className="modal-actions"><button type="button" className="btn secondary" onClick={() => setEditarUnidad(null)}>Cancelar</button>
          <button className="btn primary" disabled={guardarUnidad.isPending}>Guardar</button></div>
      </form>
    </FormModal>}
    {eliminarModelo && <ConfirmDialog title="Eliminar modelo" cancelar={() => setEliminarModelo(null)}
      confirmar={() => borrarModelo.mutate(eliminarModelo.id)}>Se eliminará {eliminarModelo.marca} {eliminarModelo.nombre}.</ConfirmDialog>}
    {eliminarUnidad && <ConfirmDialog title="Eliminar unidad" cancelar={() => setEliminarUnidad(null)}
      confirmar={() => borrarUnidad.mutate(eliminarUnidad.id)}>Se eliminará la unidad con chasis {eliminarUnidad.chasis}.</ConfirmDialog>}
    {modeloDetalle !== null && <FormModal title="Repuestos compatibles" onClose={() => setModeloDetalle(null)}>
      {compatible.isPending ? <Skeleton /> : compatible.isError ? <ErrorState mensaje="No se pudieron cargar los repuestos compatibles." />
        : compatible.data.length === 0 ? <p className="empty-state">Este modelo no tiene repuestos compatibles asignados.</p>
          : <ul>{compatible.data.map((producto) => <li key={producto.id}>{producto.codigo} · {producto.nombre}</li>)}</ul>}
    </FormModal>}
  </section>
}
