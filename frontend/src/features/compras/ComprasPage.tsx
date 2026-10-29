import { useState } from 'react'
import { useLocation, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { comprasApi, type OrdenCompra, type Proveedor, type RecepcionGuardar } from '../../api/compras'
import { catalogoApi } from '../../api/catalogo'
import { inventarioApi } from '../inventario/api'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { Badge } from '../../components/Badge'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorState } from '../../components/ErrorState'
import { FormModal } from '../../components/FormModal'
import { Pagination } from '../../components/Pagination'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'

interface FormProveedor {
  razonSocial: string; ruc: string; telefono: string; email: string; direccion: string; activo: boolean
}
interface LineaOrden { productoId: string; cantidad: string; costoUnitario: string }
const proveedorVacio: FormProveedor = { razonSocial: '', ruc: '', telefono: '', email: '', direccion: '', activo: true }

export function ComprasPage() {
  const { pathname } = useLocation()
  if (pathname.endsWith('/proveedores')) return <ProveedoresPage />
  if (pathname.endsWith('/recepciones')) return <RecepcionesPage />
  return <OrdenesPage />
}

function ProveedoresPage() {
  const [params, setParams] = useSearchParams()
  const [form, setForm] = useState<FormProveedor>(proveedorVacio)
  const [editar, setEditar] = useState<Proveedor | 'nuevo' | null>(null)
  const [eliminar, setEliminar] = useState<Proveedor | null>(null)
  const { tienePermiso } = useAuth()
  const puedeEditar = tienePermiso('COMPRA_EDITAR')
  const queryClient = useQueryClient()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const q = params.get('q') ?? ''
  const proveedores = useQuery({
    queryKey: ['proveedores', q, params.get('activo'), page, size],
    queryFn: () => comprasApi.paginaProveedores(new URLSearchParams({
      q, page: String(page), size: String(size), ...(params.get('activo') ? { activo: params.get('activo')! } : {}),
    })),
  })
  const guardar = useMutation({
    mutationFn: () => {
      const datos = { ...form, razonSocial: form.razonSocial.trim(), ruc: form.ruc.trim(),
        telefono: form.telefono.trim() || null, email: form.email.trim() || null, direccion: form.direccion.trim() || null }
      return editar === 'nuevo' || !editar ? comprasApi.crearProveedor(datos) : comprasApi.editarProveedor(editar.id, datos)
    },
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['proveedores'] }); setEditar(null); toast.success('Proveedor guardado.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo guardar el proveedor.'),
  })
  const borrar = useMutation({
    mutationFn: (id: number) => comprasApi.eliminarProveedor(id),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['proveedores'] }); setEliminar(null); toast.success('Proveedor eliminado.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo eliminar el proveedor.'),
  })
  if (proveedores.isPending) return <Skeleton />
  if (proveedores.isError) return <ErrorState mensaje={proveedores.error instanceof ApiError ? proveedores.error.detalle : 'No se pudieron cargar proveedores.'}
    reintentar={() => void proveedores.refetch()} />
  const cambiar = (campo: string, valor: string) => setParams((actual) => {
    const next = new URLSearchParams(actual)
    if (valor) next.set(campo, valor); else next.delete(campo)
    next.delete('page')
    return next
  })
  const abrir = (proveedor: Proveedor | 'nuevo') => {
    setEditar(proveedor)
    setForm(proveedor === 'nuevo' ? proveedorVacio : {
      razonSocial: proveedor.razonSocial, ruc: proveedor.ruc ?? '', telefono: proveedor.telefono ?? '',
      email: proveedor.email ?? '', direccion: '', activo: proveedor.activo,
    })
  }
  return <section className="card">
    <div className="section-title"><div><h3>Proveedores</h3><span>Datos registrados para las compras.</span></div>
      {puedeEditar && <button className="btn primary" onClick={() => abrir('nuevo')}>Nuevo proveedor</button>}</div>
    <div className="filter-bar"><SearchInput value={q} onChange={(value) => cambiar('q', value)} placeholder="Buscar por razón social o RUC..." />
      <select aria-label="Estado" value={params.get('activo') ?? ''} onChange={(event) => cambiar('activo', event.target.value)}>
        <option value="">Todos los estados</option><option value="true">Activo</option><option value="false">Inactivo</option></select></div>
    {proveedores.data.contenido.length === 0 ? <p className="empty-state">No se encontraron proveedores.</p> :
      <div className="scroll"><table><thead><tr><th>Razón social</th><th>RUC</th><th>Teléfono</th><th>Correo</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>{proveedores.data.contenido.map((p) => <tr key={p.id}><td>{p.razonSocial}</td><td>{p.ruc}</td><td>{p.telefono || '—'}</td>
          <td>{p.email || '—'}</td><td><Badge>{p.activo ? 'Activo' : 'Inactivo'}</Badge></td><td>{puedeEditar && <>
            <button className="btn ghost" onClick={() => abrir(p)}>Editar</button><button className="btn danger" onClick={() => setEliminar(p)}>Eliminar</button>
          </>}</td></tr>)}</tbody></table></div>}
    <Pagination page={proveedores.data.numero} totalPages={proveedores.data.totalPaginas} onPage={(siguiente) =>
      setParams((actual) => { const next = new URLSearchParams(actual); next.set('page', String(siguiente)); return next })} />
    {editar && <FormModal title={editar === 'nuevo' ? 'Nuevo proveedor' : 'Editar proveedor'} onClose={() => setEditar(null)}>
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); guardar.mutate() }}>
        <label>Razón social<input required maxLength={180} value={form.razonSocial} onChange={(event) => setForm({ ...form, razonSocial: event.target.value })} /></label>
        <label>RUC<input required inputMode="numeric" pattern="[0-9]{11}" maxLength={11} value={form.ruc} onChange={(event) => setForm({ ...form, ruc: event.target.value })} /></label>
        <label>Teléfono<input maxLength={20} value={form.telefono} onChange={(event) => setForm({ ...form, telefono: event.target.value })} /></label>
        <label>Correo<input type="email" maxLength={150} value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
        <label>Dirección<input maxLength={255} value={form.direccion} onChange={(event) => setForm({ ...form, direccion: event.target.value })} /></label>
        <label><input type="checkbox" checked={form.activo} onChange={(event) => setForm({ ...form, activo: event.target.checked })} /> Activo</label>
        <div className="modal-actions"><button type="button" className="btn secondary" onClick={() => setEditar(null)}>Cancelar</button>
          <button className="btn primary" disabled={guardar.isPending}>Guardar</button></div>
      </form>
    </FormModal>}
    {eliminar && <ConfirmDialog title="Eliminar proveedor" cancelar={() => setEliminar(null)} confirmar={() => borrar.mutate(eliminar.id)}>
      Se eliminará {eliminar.razonSocial}.</ConfirmDialog>}
  </section>
}

function OrdenesPage() {
  const [params, setParams] = useSearchParams()
  const [crear, setCrear] = useState(false)
  const [edicion, setEdicion] = useState<OrdenCompra | null>(null)
  const [proveedorId, setProveedorId] = useState('')
  const [almacenId, setAlmacenId] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const [lineas, setLineas] = useState<LineaOrden[]>([{ productoId: '', cantidad: '', costoUnitario: '' }])
  const { tienePermiso } = useAuth()
  const puedeEditar = tienePermiso('COMPRA_EDITAR')
  const queryClient = useQueryClient()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const proveedores = useQuery({ queryKey: ['proveedores-opciones'], queryFn: () => comprasApi.proveedores('activo=true') })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: inventarioApi.almacenes })
  const productos = useQuery({ queryKey: ['productos-opciones'], queryFn: () => catalogoApi.productos({ page: 0, size: 200, activo: true }) })
  const ordenes = useQuery({
    queryKey: ['ordenes-compra', params.toString()],
    queryFn: () => comprasApi.paginaOrdenes(new URLSearchParams({
      page: String(page), size: String(size),
      ...(params.get('q') ? { q: params.get('q')! } : {}),
      ...(params.get('estado') ? { estado: params.get('estado')! } : {}),
      ...(params.get('proveedorId') ? { proveedorId: params.get('proveedorId')! } : {}),
      ...(params.get('desde') ? { desde: new Date(`${params.get('desde')}T00:00:00-05:00`).toISOString() } : {}),
      ...(params.get('hasta') ? { hasta: new Date(`${params.get('hasta')}T23:59:59.999-05:00`).toISOString() } : {}),
    })),
  })
  const guardar = useMutation({
    mutationFn: () => {
      const datos = {
        proveedorId: Number(proveedorId), almacenId: almacenId ? Number(almacenId) : null,
        detalles: lineas.map((linea) => ({ productoId: Number(linea.productoId), cantidad: Number(linea.cantidad), costoUnitario: Number(linea.costoUnitario) })),
        observaciones: observaciones.trim() || null,
      }
      return edicion ? comprasApi.editarOrden(edicion.id, datos) : comprasApi.crearOrden(datos)
    },
    onSuccess: async (orden) => {
      await queryClient.invalidateQueries({ queryKey: ['ordenes-compra'] })
      setCrear(false); setEdicion(null); setLineas([{ productoId: '', cantidad: '', costoUnitario: '' }]); setObservaciones('')
      toast.success(`Orden ${orden.numero} guardada. Total: S/ ${orden.total.toLocaleString('es-PE', { minimumFractionDigits: 2 })}.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo crear la orden.'),
  })
  const accion = useMutation({
    mutationFn: ({ id, tipo }: { id: number; tipo: 'confirmar' | 'anular' }) =>
      tipo === 'confirmar' ? comprasApi.confirmarOrden(id) : comprasApi.anularOrden(id),
    onSuccess: async (orden) => {
      await queryClient.invalidateQueries({ queryKey: ['ordenes-compra'] })
      toast.success(`Orden ${orden.numero}: ${orden.estado}.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo actualizar la orden.'),
  })
  if (proveedores.isPending || almacenes.isPending || productos.isPending || ordenes.isPending) return <Skeleton />
  if (proveedores.isError || almacenes.isError || productos.isError || ordenes.isError) return <ErrorState mensaje="No se pudieron cargar las órdenes."
    reintentar={() => { void proveedores.refetch(); void almacenes.refetch(); void productos.refetch(); void ordenes.refetch() }} />
  const subtotal = lineas.reduce((suma, linea) => suma + Number(linea.cantidad || 0) * Number(linea.costoUnitario || 0), 0)
  const filtros = (campo: string, valor: string) => setParams((actual) => {
    const next = new URLSearchParams(actual); if (valor) next.set(campo, valor); else next.delete(campo); next.delete('page'); return next
  })
  return <section className="card">
    <div className="section-title"><div><h3>Órdenes de compra</h3><span>Importes e IGV definitivos se calculan y persisten en el servidor.</span></div>
      {puedeEditar && <button className="btn primary" onClick={() => setCrear(true)}>Nueva orden</button>}</div>
    <div className="filter-bar"><SearchInput value={params.get('q') ?? ''} onChange={(value) => filtros('q', value)} placeholder="Buscar número o proveedor..." />
      <select aria-label="Estado" value={params.get('estado') ?? ''} onChange={(event) => filtros('estado', event.target.value)}>
        <option value="">Todos los estados</option>{['BORRADOR', 'CONFIRMADA', 'RECIBIDA', 'ANULADA'].map((estado) => <option key={estado}>{estado}</option>)}</select>
      <select aria-label="Proveedor" value={params.get('proveedorId') ?? ''} onChange={(event) => filtros('proveedorId', event.target.value)}>
        <option value="">Todos los proveedores</option>{proveedores.data.map((p) => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}</select></div>
    <div className="filter-bar"><label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => filtros('desde', event.target.value)} /></label>
      <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => filtros('hasta', event.target.value)} /></label></div>
    {ordenes.data.contenido.length === 0 ? <p className="empty-state">No hay órdenes de compra.</p> :
      <div className="scroll"><table><thead><tr><th>Número</th><th>Fecha</th><th>Proveedor</th><th>Almacén</th><th>Subtotal</th><th>IGV</th><th>Total</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>{ordenes.data.contenido.map((orden) => <tr key={orden.id}><td>{orden.numero}</td><td>{new Date(orden.fecha).toLocaleDateString('es-PE')}</td>
          <td>{orden.proveedor}</td><td>{orden.almacen}</td><td>S/ {orden.subtotal.toFixed(2)}</td><td>S/ {orden.igv.toFixed(2)}</td><td>S/ {orden.total.toFixed(2)}</td>
          <td><Badge>{orden.estado}</Badge></td><td>{puedeEditar && <>
            {orden.estado === 'BORRADOR' && <>
              <button className="btn ghost" onClick={() => {
                setEdicion(orden); setProveedorId(String(orden.proveedorId)); setAlmacenId(orden.almacenId ? String(orden.almacenId) : '')
                setObservaciones(orden.observaciones ?? '')
                setLineas(orden.detalles.map((detalle) => ({ productoId: String(detalle.productoId), cantidad: String(detalle.cantidad), costoUnitario: String(detalle.costoUnitario) })))
                setCrear(true)
              }}>Editar</button>
              <button className="btn primary" disabled={accion.isPending} onClick={() => accion.mutate({ id: orden.id, tipo: 'confirmar' })}>Confirmar</button>
            </>}
            {(orden.estado === 'BORRADOR' || orden.estado === 'CONFIRMADA') && <button className="btn danger" disabled={accion.isPending} onClick={() => accion.mutate({ id: orden.id, tipo: 'anular' })}>Anular</button>}
          </>}</td></tr>)}</tbody></table></div>}
    <Pagination page={ordenes.data.numero} totalPages={ordenes.data.totalPaginas} onPage={(siguiente) =>
      setParams((actual) => { const next = new URLSearchParams(actual); next.set('page', String(siguiente)); return next })} />
    {crear && <FormModal title={edicion ? `Editar orden ${edicion.numero}` : 'Crear orden de compra'} onClose={() => { setCrear(false); setEdicion(null) }}>
      <form className="form-grid" onSubmit={(event) => { event.preventDefault(); guardar.mutate() }}>
        <label>Proveedor<select required value={proveedorId} onChange={(event) => setProveedorId(event.target.value)}><option value="">Selecciona proveedor</option>
          {proveedores.data.map((p) => <option key={p.id} value={p.id}>{p.razonSocial} · {p.ruc}</option>)}</select></label>
        <label>Almacén<select value={almacenId} onChange={(event) => setAlmacenId(event.target.value)}><option value="">Predeterminado</option>
          {almacenes.data.filter((a) => a.activo).map((a) => <option key={a.id} value={a.id}>{a.nombre}</option>)}</select></label>
        {lineas.map((linea, index) => <div className="form-grid" key={index}>
          <label>Producto<select required value={linea.productoId} onChange={(event) => setLineas((prev) => prev.map((item, i) => i === index ? { ...item, productoId: event.target.value } : item))}>
            <option value="">Selecciona</option>{productos.data.contenido.map((p) => <option key={p.id} value={p.id}>{p.codigo} · {p.nombre}</option>)}</select></label>
          <label>Cantidad<input required type="number" min="0.001" step="0.001" value={linea.cantidad} onChange={(event) => setLineas((prev) => prev.map((item, i) => i === index ? { ...item, cantidad: event.target.value } : item))} /></label>
          <label>Costo unitario<input required type="number" min="0" step="0.01" value={linea.costoUnitario} onChange={(event) => setLineas((prev) => prev.map((item, i) => i === index ? { ...item, costoUnitario: event.target.value } : item))} /></label>
          {lineas.length > 1 && <button type="button" className="btn ghost" onClick={() => setLineas((prev) => prev.filter((_, i) => i !== index))}>Quitar línea</button>}
        </div>)}
        <button type="button" className="btn secondary" onClick={() => setLineas((prev) => [...prev, { productoId: '', cantidad: '', costoUnitario: '' }])}>Agregar línea</button>
        <label>Observaciones<textarea value={observaciones} onChange={(event) => setObservaciones(event.target.value)} /></label>
        <p>Subtotal referencial: S/ {subtotal.toLocaleString('es-PE', { minimumFractionDigits: 2 })}. IGV y total se calcularán con la configuración vigente del servidor.</p>
        <div className="modal-actions"><button type="button" className="btn secondary" onClick={() => { setCrear(false); setEdicion(null) }}>Cancelar</button>
          <button className="btn primary" disabled={!proveedorId || !lineas.every((linea) => linea.productoId && Number(linea.cantidad) > 0 && Number(linea.costoUnitario) >= 0) || guardar.isPending}>{edicion ? 'Guardar borrador' : 'Crear borrador'}</button></div>
      </form>
    </FormModal>}
  </section>
}

function RecepcionesPage() {
  const [params, setParams] = useSearchParams()
  const [ordenId, setOrdenId] = useState('')
  const [cantidades, setCantidades] = useState<Record<number, string>>({})
  const [observaciones, setObservaciones] = useState('')
  const { tienePermiso } = useAuth()
  const puedeEditar = tienePermiso('COMPRA_EDITAR')
  const queryClient = useQueryClient()
  const page = Math.max(0, Number(params.get('page') ?? 0))
  const size = Number(params.get('size') ?? 20)
  const ordenes = useQuery({ queryKey: ['ordenes-para-recepcion'], queryFn: () => comprasApi.ordenes('estado=CONFIRMADA') })
  const recepciones = useQuery({
    queryKey: ['recepciones-compra', params.toString()],
    queryFn: () => comprasApi.paginaRecepciones(new URLSearchParams({
      page: String(page), size: String(size), ...(params.get('ordenId') ? { ordenId: params.get('ordenId')! } : {}),
      ...(params.get('desde') ? { desde: new Date(`${params.get('desde')}T00:00:00-05:00`).toISOString() } : {}),
      ...(params.get('hasta') ? { hasta: new Date(`${params.get('hasta')}T23:59:59.999-05:00`).toISOString() } : {}),
    })),
  })
  const selectedOrder = ordenes.data?.find((orden) => orden.id === Number(ordenId))
  const recibir = useMutation({
    mutationFn: () => {
      if (!selectedOrder) throw new Error('Selecciona una orden confirmada.')
      const datos: RecepcionGuardar = {
        ordenId: selectedOrder.id,
        detalles: selectedOrder.detalles.filter((detalle) => Number(cantidades[detalle.id] ?? 0) > 0)
          .map((detalle) => ({ detalleOrdenId: detalle.id, cantidad: Number(cantidades[detalle.id]) })),
        observaciones: observaciones.trim() || null,
      }
      if (datos.detalles.length === 0) throw new Error('Indica al menos una cantidad recibida.')
      if (datos.detalles.some((detalle) => detalle.cantidad > (selectedOrder.detalles.find((linea) => linea.id === detalle.detalleOrdenId)?.cantidadPendiente ?? 0))) {
        throw new Error('La cantidad recibida no puede superar lo pendiente.')
      }
      return comprasApi.recibirOrden(datos)
    },
    onSuccess: async (recepcion) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['recepciones-compra'] }),
        queryClient.invalidateQueries({ queryKey: ['ordenes-para-recepcion'] }),
        queryClient.invalidateQueries({ queryKey: ['ordenes-compra'] }),
        queryClient.invalidateQueries({ queryKey: ['movimientos'] }),
        queryClient.invalidateQueries({ queryKey: ['stock'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ])
      setOrdenId(''); setCantidades({}); setObservaciones('')
      toast.success(`Recepción ${recepcion.numero} registrada.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : error instanceof Error ? error.message : 'No se pudo registrar la recepción.'),
  })
  if (ordenes.isPending || recepciones.isPending) return <Skeleton />
  if (ordenes.isError || recepciones.isError) return <ErrorState mensaje="No se pudieron cargar las recepciones."
    reintentar={() => { void ordenes.refetch(); void recepciones.refetch() }} />
  return <section className="settings-grid">
    {puedeEditar && <form className="card form-card" onSubmit={(event) => { event.preventDefault(); recibir.mutate() }}>
      <h3>Registrar recepción parcial</h3>
      <label>Orden confirmada<select required value={ordenId} onChange={(event) => { setOrdenId(event.target.value); setCantidades({}) }}>
        <option value="">Selecciona una orden</option>{ordenes.data.map((orden) => <option key={orden.id} value={orden.id}>{orden.numero} · {orden.proveedor}</option>)}</select></label>
      {selectedOrder?.detalles.filter((detalle) => detalle.cantidadPendiente > 0).map((detalle) => <label key={detalle.id}>
        {detalle.productoCodigo} · {detalle.productoNombre} (pendiente {detalle.cantidadPendiente})
        <input type="number" min="0" max={detalle.cantidadPendiente} step="0.001" value={cantidades[detalle.id] ?? ''}
          onChange={(event) => setCantidades((prev) => ({ ...prev, [detalle.id]: event.target.value }))} /></label>)}
      <label>Observaciones<textarea value={observaciones} onChange={(event) => setObservaciones(event.target.value)} /></label>
      <button className="btn primary" disabled={!selectedOrder || !Object.values(cantidades).some((value) => Number(value) > 0) || recibir.isPending}>
        {recibir.isPending ? 'Registrando…' : 'Registrar recepción'}</button>
    </form>}
    <section className="card"><h3>Historial de recepciones</h3>
      <div className="filter-bar"><input aria-label="Filtrar por orden ID" type="number" min="1" placeholder="ID de orden"
        value={params.get('ordenId') ?? ''} onChange={(event) => setParams((actual) => {
          const next = new URLSearchParams(actual); if (event.target.value) next.set('ordenId', event.target.value); else next.delete('ordenId'); next.delete('page'); return next
        })} />
        <label>Desde<input type="date" value={params.get('desde') ?? ''} onChange={(event) => setParams((actual) => {
          const next = new URLSearchParams(actual); if (event.target.value) next.set('desde', event.target.value); else next.delete('desde'); next.delete('page'); return next
        })} /></label>
        <label>Hasta<input type="date" value={params.get('hasta') ?? ''} onChange={(event) => setParams((actual) => {
          const next = new URLSearchParams(actual); if (event.target.value) next.set('hasta', event.target.value); else next.delete('hasta'); next.delete('page'); return next
        })} /></label></div>
      {recepciones.data.contenido.length === 0 ? <p className="empty-state">No hay recepciones registradas.</p> :
        <div className="scroll"><table><thead><tr><th>Número</th><th>Orden</th><th>Proveedor</th><th>Fecha</th><th>Responsable</th><th>Productos</th></tr></thead>
          <tbody>{recepciones.data.contenido.map((r) => <tr key={r.id}><td>{r.numero}</td><td>{r.orden}</td><td>{r.proveedor}</td>
            <td>{new Date(r.fecha).toLocaleString('es-PE')}</td><td>{r.responsable}</td><td>{r.detalles.map((d) => `${d.productoCodigo}: ${d.cantidad}`).join(', ')}</td></tr>)}</tbody></table></div>}
      <Pagination page={recepciones.data.numero} totalPages={recepciones.data.totalPaginas} onPage={(siguiente) =>
        setParams((actual) => { const next = new URLSearchParams(actual); next.set('page', String(siguiente)); return next })} />
    </section>
  </section>
}
