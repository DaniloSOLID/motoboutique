import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { ApiError } from '../../api/client'
import { ErrorState } from '../../components/ErrorState'
import { useConfiguracionEmpresa } from './hooks'
import { AlmacenesAdmin } from './AlmacenesAdmin'
import { AuditoriaAdmin } from './AuditoriaAdmin'
import { useAuth } from '../../auth/AuthContext'

const esquema = z.object({
  razonSocial: z.string().max(180),
  ruc: z.string().regex(/^$|^\d{11}$/, 'El RUC debe tener 11 dígitos.'),
  moneda: z.string().length(3, 'La moneda debe tener 3 caracteres.'),
  igv: z.coerce.number().min(0).max(100),
  almacenDefectoId: z.string(),
  stockMinimoDefault: z.coerce.number().min(0),
})
type FormularioEntrada = z.input<typeof esquema>
type Formulario = z.output<typeof esquema>

export function ConfiguracionEmpresaForm() {
  const { tienePermiso } = useAuth()
  const puedeEditar = tienePermiso('CONFIG_EDITAR')
  const { empresa, almacenes, guardar } = useConfiguracionEmpresa()
  const form = useForm<FormularioEntrada, unknown, Formulario>({
    resolver: zodResolver(esquema),
    defaultValues: {
      razonSocial: '', ruc: '', moneda: '', igv: Number.NaN,
      almacenDefectoId: '', stockMinimoDefault: Number.NaN,
    },
  })
  const { reset } = form
  useEffect(() => {
    if (!empresa.data) return
    reset({
      razonSocial: empresa.data.razonSocial ?? '',
      ruc: empresa.data.ruc ?? '',
      moneda: empresa.data.moneda,
      igv: empresa.data.igv,
      almacenDefectoId: empresa.data.almacenDefectoId === null ? '' : String(empresa.data.almacenDefectoId),
      stockMinimoDefault: empresa.data.stockMinimoDefault,
    })
  }, [empresa.data, reset])

  if (empresa.isPending || almacenes.isPending) return <p role="status">Cargando configuración…</p>
  if (empresa.isError || almacenes.isError) {
    const error = empresa.error ?? almacenes.error
    return <ErrorState mensaje={error instanceof ApiError ? error.detalle : 'No se pudo cargar la configuración.'}
      reintentar={() => { void empresa.refetch(); void almacenes.refetch() }} />
  }
  const submit = form.handleSubmit(async (datos) => {
    try {
      await guardar.mutateAsync({
        razonSocial: datos.razonSocial.trim() || null,
        ruc: datos.ruc.trim() || null,
        moneda: datos.moneda.trim().toUpperCase(),
        igv: datos.igv,
        almacenDefectoId: datos.almacenDefectoId ? Number(datos.almacenDefectoId) : null,
        stockMinimoDefault: datos.stockMinimoDefault,
      })
      toast.success('Configuración guardada.')
    } catch (error) {
      toast.error(error instanceof ApiError ? error.detalle : 'No se pudo guardar la configuración.')
    }
  })
  return <><div className="settings-grid"><form className="card form-card" onSubmit={(event) => void submit(event)}>
    <h3>Datos de la empresa</h3>
    <fieldset disabled={!puedeEditar} className="form-grid">
    <label>Razón social<input {...form.register('razonSocial')} /></label>
    {form.formState.errors.razonSocial && <small role="alert">{form.formState.errors.razonSocial.message}</small>}
    <label>RUC<input inputMode="numeric" maxLength={11} {...form.register('ruc')} /></label>
    {form.formState.errors.ruc && <small role="alert">{form.formState.errors.ruc.message}</small>}
    <label>Moneda<input maxLength={3} {...form.register('moneda')} /></label>
    {form.formState.errors.moneda && <small role="alert">{form.formState.errors.moneda.message}</small>}
    <label>IGV (%)<input type="number" min="0" max="100" step="0.01" {...form.register('igv')} /></label>
    <label>Almacén por defecto<select {...form.register('almacenDefectoId')}><option value="">Ninguno</option>
      {almacenes.data.filter((almacen) => almacen.activo).map((almacen) =>
        <option key={almacen.id} value={almacen.id}>{almacen.nombre}</option>)}</select></label>
    <label>Stock mínimo por defecto<input type="number" min="0" step="0.001" {...form.register('stockMinimoDefault')} /></label>
    </fieldset>
    {puedeEditar && <button className="btn primary" type="submit" disabled={!form.formState.isDirty || guardar.isPending || form.formState.isSubmitting}>
      {guardar.isPending ? 'Guardando…' : 'Guardar cambios'}</button>
    }
  </form></div><AlmacenesAdmin /><AuditoriaAdmin /></>
}
