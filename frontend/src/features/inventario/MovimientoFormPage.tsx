import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { catalogoApi } from '../../api/catalogo'
import { ApiError } from '../../api/client'
import { Badge } from '../../components/Badge'
import { ErrorState } from '../../components/ErrorState'
import { SearchInput } from '../../components/SearchInput'
import { Skeleton } from '../../components/Skeleton'
import { inventarioApi } from './api'

export function MovimientoFormPage({ tipo }: { tipo: 'entrada' | 'salida' }) {
  const queryClient = useQueryClient()
  const [buscar, setBuscar] = useState('')
  const [productoId, setProductoId] = useState('')
  const [almacenId, setAlmacenId] = useState('')
  const [cantidad, setCantidad] = useState('')
  const [costo, setCosto] = useState('')
  const [motivo, setMotivo] = useState('VENTA_MOSTRADOR')
  const [referencia, setReferencia] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: inventarioApi.almacenes })
  const productos = useQuery({
    queryKey: ['productos-opciones', buscar],
    queryFn: () => catalogoApi.productos({ q: buscar || undefined, page: 0, size: 100, activo: true }),
  })
  const selectedProduct = productos.data?.contenido.find((p) => p.id === Number(productoId))
  const stock = useQuery({
    queryKey: ['stock-producto', selectedProduct?.codigo, almacenId],
    queryFn: () => inventarioApi.stock({ q: selectedProduct?.codigo, almacenId: Number(almacenId), page: 0, size: 100 }),
    enabled: tipo === 'salida' && Boolean(selectedProduct && almacenId),
  })
  const recientes = useQuery({
    queryKey: ['movimientos-recientes', tipo],
    queryFn: () => inventarioApi.movimientos({
      tipo: tipo === 'entrada' ? 'ENTRADA' : 'SALIDA', page: 0, size: 10, sort: 'fecha', direction: 'desc',
    }),
  })
  const guardar = useMutation({
    mutationFn: () => tipo === 'entrada'
      ? inventarioApi.entrada({
        productoId: Number(productoId), almacenId: Number(almacenId), cantidad: Number(cantidad),
        costoUnitario: Number(costo), documentoReferencia: referencia || undefined, observaciones: observaciones || undefined,
      })
      : inventarioApi.salida({
        productoId: Number(productoId), almacenId: Number(almacenId), cantidad: Number(cantidad),
        motivo, documentoReferencia: referencia || undefined, observaciones: observaciones || undefined,
      }),
    onSuccess: async (movimiento) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['movimientos-recientes', tipo] }),
        queryClient.invalidateQueries({ queryKey: ['movimientos'] }),
        queryClient.invalidateQueries({ queryKey: ['stock'] }),
        queryClient.invalidateQueries({ queryKey: ['stock-producto'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['inventario'] }),
      ])
      setCantidad('')
      setCosto('')
      setReferencia('')
      setObservaciones('')
      toast.success(`${tipo === 'entrada' ? 'Entrada' : 'Salida'} registrada: ${movimiento.numero}.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo registrar el movimiento.'),
  })
  const stockDisponible = stock.data?.contenido.find((fila) => fila.almacenId === Number(almacenId)
    && fila.codigo === selectedProduct?.codigo)
  const amountValid = Number(cantidad) > 0
  const canSubmit = Boolean(productoId && almacenId && amountValid
    && (tipo === 'entrada' ? Number(costo) >= 0 : stockDisponible && Number(cantidad) <= stockDisponible.cantidad - stockDisponible.reservado))
  if (almacenes.isPending || productos.isPending || recientes.isPending) return <Skeleton />
  if (almacenes.isError || productos.isError || recientes.isError) {
    const error = almacenes.error ?? productos.error ?? recientes.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudieron cargar los datos del movimiento.'}
      reintentar={() => { void almacenes.refetch(); void productos.refetch(); void recientes.refetch() }} />
  }
  return <section className="settings-grid">
    <form className="card form-card" onSubmit={(event) => { event.preventDefault(); if (canSubmit) guardar.mutate() }}>
      <h3>{tipo === 'entrada' ? 'Registrar entrada' : 'Registrar salida'}</h3>
      <label>Buscar producto<SearchInput value={buscar} onChange={setBuscar} placeholder="Código o nombre..." /></label>
      <label>Producto<select required value={productoId} onChange={(event) => setProductoId(event.target.value)}>
        <option value="">Selecciona un producto</option>{productos.data.contenido.map((producto) =>
          <option value={producto.id} key={producto.id}>{producto.codigo} · {producto.nombre}</option>)}
      </select></label>
      <label>Almacén<select required value={almacenId} onChange={(event) => setAlmacenId(event.target.value)}>
        <option value="">Selecciona un almacén</option>{almacenes.data.filter((a) => a.activo).map((almacen) =>
          <option value={almacen.id} key={almacen.id}>{almacen.nombre}</option>)}
      </select></label>
      {tipo === 'salida' && <p className="field-hint">Disponible: {stockDisponible
        ? (stockDisponible.cantidad - stockDisponible.reservado).toLocaleString('es-PE', { minimumFractionDigits: 3 })
        : stock.isPending ? 'consultando…' : '0.000'}</p>}
      {selectedProduct && <p className="field-hint">Stock total actual: {selectedProduct.stockTotal.toLocaleString('es-PE')}</p>}
      <label>Cantidad<input required type="number" min="0.001" step="0.001" value={cantidad}
        onChange={(event) => setCantidad(event.target.value)} /></label>
      {tipo === 'entrada'
        ? <label>Costo unitario (S/)<input required type="number" min="0" step="0.01" value={costo}
          onChange={(event) => setCosto(event.target.value)} /></label>
        : <label>Motivo<select value={motivo} onChange={(event) => setMotivo(event.target.value)}>
          <option value="VENTA_MOSTRADOR">Venta mostrador</option><option value="ORDEN_TALLER">Orden de taller</option>
          <option value="MERMA">Merma</option><option value="OTRO">Otro</option></select></label>}
      <label>Documento de referencia<input maxLength={80} value={referencia} onChange={(event) => setReferencia(event.target.value)} /></label>
      <label>Observaciones<textarea maxLength={255} value={observaciones} onChange={(event) => setObservaciones(event.target.value)} /></label>
      {tipo === 'salida' && amountValid && stockDisponible && Number(cantidad) > stockDisponible.cantidad - stockDisponible.reservado &&
        <small role="alert">La cantidad supera el stock disponible.</small>}
      <button className="btn primary" disabled={!canSubmit || guardar.isPending}>
        {guardar.isPending ? 'Registrando…' : `Registrar ${tipo}`}</button>
    </form>
    <section className="card"><h3>Movimientos recientes</h3>
      {recientes.data.contenido.length === 0 ? <p className="empty-state">Sin movimientos recientes.</p> :
        <div className="scroll"><table><thead><tr><th>Documento</th><th>Fecha</th><th>Producto</th><th>Almacén</th><th>Cantidad</th><th>Estado</th></tr></thead>
          <tbody>{recientes.data.contenido.map((item) => <tr key={item.id}><td>{item.numero}</td>
            <td>{new Date(item.fecha).toLocaleString('es-PE')}</td><td>{item.productoCodigo} · {item.productoNombre}</td>
            <td>{item.almacen}</td><td>{item.cantidad}</td><td><Badge>{item.tipo}</Badge></td></tr>)}</tbody></table></div>}
    </section>
  </section>
}
