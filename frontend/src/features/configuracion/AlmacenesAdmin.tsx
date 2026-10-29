import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ApiError } from '../../api/client'
import { administracionApi, type AlmacenOpcion } from '../../api/administracion'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FormModal } from '../../components/FormModal'
import { ErrorState } from '../../components/ErrorState'
import { useAuth } from '../../auth/AuthContext'

type DatosAlmacen = Omit<AlmacenOpcion, 'id'>

export function AlmacenesAdmin() {
  const { tienePermiso } = useAuth()
  const puedeEditar = tienePermiso('INVENTARIO_EDITAR')
  const queryClient = useQueryClient()
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: administracionApi.almacenes })
  const [editar, setEditar] = useState<AlmacenOpcion | 'nuevo' | null>(null)
  const [desactivar, setDesactivar] = useState<AlmacenOpcion | null>(null)
  const [datos, setDatos] = useState<DatosAlmacen>({ codigo: '', nombre: '', direccion: '', activo: true })
  const guardar = useMutation({
    mutationFn: () => administracionApi.guardarAlmacen(
      editar === 'nuevo' ? null : editar?.id ?? null,
      datos,
    ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['almacenes'] })
      setEditar(null)
      toast.success('Almacén guardado.')
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo guardar el almacén.'),
  })
  const cambiarEstado = useMutation({
    mutationFn: (almacen: AlmacenOpcion) => administracionApi.guardarAlmacen(almacen.id, {
      codigo: almacen.codigo, nombre: almacen.nombre, direccion: almacen.direccion, activo: false,
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['almacenes'] })
      setDesactivar(null)
      toast.success('Almacén desactivado.')
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo desactivar el almacén.'),
  })
  const abrir = (almacen: AlmacenOpcion | 'nuevo') => {
    setEditar(almacen)
    setDatos(almacen === 'nuevo'
      ? { codigo: '', nombre: '', direccion: '', activo: true }
      : { codigo: almacen.codigo, nombre: almacen.nombre, direccion: almacen.direccion ?? '', activo: almacen.activo })
  }
  if (almacenes.isPending) return <p role="status">Cargando almacenes…</p>
  if (almacenes.isError) return <ErrorState mensaje="No se pudieron cargar los almacenes." reintentar={() => void almacenes.refetch()} />
  return <section className="card section-space">
    <div className="section-title"><div><h3>Almacenes</h3><span>Administración de ubicaciones registradas.</span></div>
      {puedeEditar && <button className="btn primary" onClick={() => abrir('nuevo')}>Nuevo almacén</button>}</div>
    {almacenes.data.length === 0 ? <p className="empty-state">No hay almacenes registrados.</p> :
      <div className="scroll"><table><thead><tr><th>Código</th><th>Nombre</th><th>Dirección</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>{almacenes.data.map((almacen) => <tr key={almacen.id}><td>{almacen.codigo}</td><td>{almacen.nombre}</td>
          <td>{almacen.direccion ?? '—'}</td><td>{almacen.activo ? 'Activo' : 'Inactivo'}</td><td>
            {puedeEditar && <><button className="btn ghost" onClick={() => abrir(almacen)}>Editar</button>
              {almacen.activo && <button className="btn danger" onClick={() => setDesactivar(almacen)}>Desactivar</button>}</>}
          </td></tr>)}</tbody></table></div>}
    {editar && <FormModal title={editar === 'nuevo' ? 'Nuevo almacén' : 'Editar almacén'} onClose={() => setEditar(null)}>
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); guardar.mutate() }}>
        <label>Código<input required maxLength={30} value={datos.codigo} onChange={(event) => setDatos({ ...datos, codigo: event.target.value })} /></label>
        <label>Nombre<input required maxLength={120} value={datos.nombre} onChange={(event) => setDatos({ ...datos, nombre: event.target.value })} /></label>
        <label>Dirección<input maxLength={255} value={datos.direccion ?? ''} onChange={(event) => setDatos({ ...datos, direccion: event.target.value })} /></label>
        {editar !== 'nuevo' && <label><input type="checkbox" checked={datos.activo} onChange={(event) => setDatos({ ...datos, activo: event.target.checked })} /> Activo</label>}
        <div className="modal-actions"><button type="button" className="btn secondary" onClick={() => setEditar(null)}>Cancelar</button>
          <button className="btn primary" disabled={guardar.isPending || !datos.codigo.trim() || !datos.nombre.trim()}>{guardar.isPending ? 'Guardando…' : 'Guardar'}</button></div>
      </form>
    </FormModal>}
    {desactivar && <ConfirmDialog title="Desactivar almacén" cancelar={() => setDesactivar(null)}
      confirmar={() => cambiarEstado.mutate(desactivar)}>El almacén {desactivar.nombre} dejará de estar disponible para nuevas operaciones.</ConfirmDialog>}
  </section>
}
