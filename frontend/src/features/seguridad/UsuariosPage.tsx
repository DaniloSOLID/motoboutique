import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { usuariosApi } from '../../api/inventario'
import { DataTable, type DataTableColumn } from '../../components/DataTable'
import type { UsuarioCrear, UsuarioListado } from '../../api/inventario'
import { Badge } from '../../components/Badge'
import { ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { FormModal } from '../../components/FormModal'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { useUsuarios } from './hooks'

const esquema = z.object({
  nombre: z.string().trim().min(1, 'Ingresa el nombre.').max(120),
  email: z.string().email('Ingresa un correo válido.'),
  password: z.string().optional(),
  rol: z.enum(['ADMIN', 'ALMACENERO', 'CONTADOR', 'CONSULTA']),
})
type Formulario = z.infer<typeof esquema>

export function UsuariosPage() {
  const [params, setParams] = useSearchParams()
  const [modal, setModal] = useState<UsuarioListado | 'nuevo' | null>(null)
  const [eliminar, setEliminar] = useState<UsuarioListado | null>(null)
  const [resetTarget, setResetTarget] = useState<UsuarioListado | null>(null)
  const { tienePermiso, tieneRol, usuario } = useAuth()
  const queryClient = useQueryClient()
  const q = params.get('q') ?? ''
  const page = Number(params.get('page') ?? 0)
  const size = Number(params.get('size') ?? 20)
  const rol = params.get('rol') ?? ''
  const activo = params.get('activo') ?? ''
  const sort = params.get('sort') ?? 'nombre'
  const direction = params.get('direction') ?? 'asc'
  const query = useUsuarios({
    q: q || undefined, page, size, rol: rol || undefined,
    activo: activo === '' ? undefined : activo === 'true', sort, direction,
  })
  const administradores = useQuery({
    queryKey: ['usuarios', 'administradores-activos'],
    queryFn: () => usuariosApi.listar({ rol: 'ADMIN', activo: true, page: 0, size: 2 }),
    enabled: tieneRol('ADMIN'),
  })
  const refrescar = () => queryClient.invalidateQueries({ queryKey: ['usuarios'] })
  const form = useForm<Formulario>({
    resolver: zodResolver(esquema),
    defaultValues: { nombre: '', email: '', password: '', rol: 'ALMACENERO' },
  })
  const guardar = useMutation({
    mutationFn: async (datos: Formulario) => {
      if (modal === 'nuevo') {
        if (!datos.password || datos.password.length < 8) {
          form.setError('password', { message: 'La contraseña requiere al menos 8 caracteres.' })
          throw new Error('La contraseña requiere al menos 8 caracteres.')
        }
        const nuevo: UsuarioCrear = { nombre: datos.nombre, email: datos.email, password: datos.password, roles: [datos.rol] }
        return usuariosApi.crear(nuevo)
      }
      if (!modal) return
      return usuariosApi.editar(modal.id, { nombre: datos.nombre, email: datos.email, rol: datos.rol, activo: modal.activo })
    },
    onSuccess: async () => {
      await refrescar()
      toast.success(modal === 'nuevo' ? 'Usuario creado.' : 'Usuario actualizado.')
      setModal(null)
    },
    onError: (error) => {
      if (error instanceof ApiError && error.errores) {
        Object.entries(error.errores).forEach(([campo, mensaje]) => {
          if (campo === 'nombre' || campo === 'email') form.setError(campo, { message: mensaje })
        })
      }
      toast.error(error instanceof ApiError ? error.detalle : error.message)
    },
  })
  const estado = useMutation({
    mutationFn: ({ id, activo: valor }: { id: number; activo: boolean }) => usuariosApi.cambiarEstado(id, valor),
    onSuccess: async () => { await refrescar(); toast.success('Estado actualizado.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo actualizar el estado.'),
  })
  const borrar = useMutation({
    mutationFn: (id: number) => usuariosApi.eliminar(id),
    onSuccess: async () => { await refrescar(); setEliminar(null); toast.success('Usuario eliminado.') },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo eliminar el usuario.'),
  })
  const passwordForm = useForm<{ password: string }>({
    resolver: zodResolver(z.object({ password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres.').max(72) })),
    defaultValues: { password: '' },
  })
  const restablecer = useMutation({
    mutationFn: ({ id, password }: { id: number; password: string }) => usuariosApi.restablecerPassword(id, password),
    onSuccess: async () => {
      setResetTarget(null)
      passwordForm.reset({ password: '' })
      toast.success('Contraseña restablecida.')
    },
    onError: (error) => {
      if (error instanceof ApiError && error.errores?.password) {
        passwordForm.setError('password', { message: error.errores.password })
      }
      toast.error(error instanceof ApiError ? error.detalle : 'No se pudo restablecer la contraseña.')
    },
  })
  const esUltimoAdmin = (registro: UsuarioListado) =>
    registro.activo && registro.roles.includes('ADMIN')
      && (administradores.isPending || administradores.isError || administradores.data?.totalElementos === 1)
  const noPuedeAdministrar = (registro: UsuarioListado) =>
    registro.id === usuario?.id || esUltimoAdmin(registro)
  const editar = (usuario: UsuarioListado) => {
    setModal(usuario)
    form.reset({ nombre: usuario.nombre, email: usuario.email, password: '', rol: usuario.roles[0] as Formulario['rol'] })
  }
  const nuevo = () => {
    setModal('nuevo')
    form.reset({ nombre: '', email: '', password: '', rol: 'ALMACENERO' })
  }
  const actualizarFiltro = (clave: string, valor: string) => {
    setParams((actuales) => {
      const siguientes = new URLSearchParams(actuales)
      if (valor) siguientes.set(clave, valor); else siguientes.delete(clave)
      siguientes.delete('page')
      return siguientes
    })
  }
  const columnas: DataTableColumn<UsuarioListado>[] = [
    { key: 'id', label: 'Código', render: (row) => <span className="code">USR-{String(row.id).padStart(3, '0')}</span> },
    { key: 'nombre', label: 'Usuario', render: (row) => <b>{row.nombre}</b> },
    { key: 'email', label: 'Correo' },
    { key: 'roles', label: 'Rol', render: (row) => <Badge>{row.roles.join(', ')}</Badge> },
    { key: 'activo', label: 'Estado', render: (row) => <Badge>{row.activo ? 'Activo' : 'Inactivo'}</Badge> },
    { key: 'acciones', label: 'Acciones', render: (row) => <div className="user-actions">
      {tienePermiso('USUARIO_EDITAR') && <>
        <button className="btn ghost" onClick={() => editar(row)}>Editar</button>
        <button className="btn ghost" disabled={noPuedeAdministrar(row)}
          title={row.id === usuario?.id ? 'No puedes cambiar tu propio estado.' : esUltimoAdmin(row) ? 'Debe permanecer al menos un administrador activo.' : undefined}
          onClick={() => estado.mutate({ id: row.id, activo: !row.activo })}>{row.activo ? 'Desactivar' : 'Activar'}</button>
        {tieneRol('ADMIN') && <button className="btn ghost" onClick={() => { setResetTarget(row); passwordForm.reset({ password: '' }) }}>Restablecer contraseña</button>}
        <button className="btn danger" onClick={() => setEliminar(row)} disabled={noPuedeAdministrar(row)}
          title={row.id === usuario?.id ? 'No puedes eliminar tu propio usuario.' : esUltimoAdmin(row) ? 'Debe permanecer al menos un administrador activo.' : undefined}>Eliminar</button>
      </>}
    </div> },
  ]
  return <>
    <div className="section-title"><div><h3>Gestión de usuarios</h3><span>Administración de cuentas con datos del servidor.</span></div>
      {tienePermiso('USUARIO_EDITAR') && <button className="btn primary" onClick={nuevo}>Nuevo usuario</button>}
    </div>
    <DataTable rows={query.data?.contenido ?? []} columns={columnas}
      total={query.data?.totalElementos ?? 0} totalPages={query.data?.totalPaginas ?? 0}
      loading={query.isPending} error={query.error instanceof ApiError ? query.error.detalle : query.error ? 'No se pudieron cargar los usuarios.' : undefined}
      onRetry={() => void query.refetch()}
      extraFilters={<><select aria-label="Filtrar por rol" value={rol} onChange={(e) => actualizarFiltro('rol', e.target.value)}>
        <option value="">Todos los roles</option><option value="ADMIN">Administrador</option>
        <option value="ALMACENERO">Almacenero</option><option value="CONTADOR">Contador</option>
        <option value="CONSULTA">Consulta</option></select>
        <select aria-label="Filtrar por estado" value={activo} onChange={(e) => actualizarFiltro('activo', e.target.value)}>
          <option value="">Todos los estados</option><option value="true">Activo</option><option value="false">Inactivo</option>
        </select></>} />
    {modal && <FormModal title={modal === 'nuevo' ? 'Crear usuario' : 'Editar usuario'} onClose={() => setModal(null)}>
      <form className="form-grid" onSubmit={(event) => void form.handleSubmit((value) => guardar.mutate(value))(event)}>
        <label>Nombre<input {...form.register('nombre')} />{form.formState.errors.nombre && <small>{form.formState.errors.nombre.message}</small>}</label>
        <label>Correo<input type="email" {...form.register('email')} />{form.formState.errors.email && <small>{form.formState.errors.email.message}</small>}</label>
        {modal === 'nuevo' && <label>Contraseña<input type="password" autoComplete="new-password" {...form.register('password')} />
          {form.formState.errors.password && <small role="alert">{form.formState.errors.password.message}</small>}</label>}
        <label>Rol{modal !== 'nuevo' && esUltimoAdmin(modal) ? <>
          <select value={form.watch('rol')} disabled><option value="ADMIN">Administrador</option>
            <option value="ALMACENERO">Almacenero</option><option value="CONTADOR">Contador</option><option value="CONSULTA">Consulta</option>
          </select><input type="hidden" {...form.register('rol')} />
        </> : <select {...form.register('rol')}><option value="ADMIN">Administrador</option>
          <option value="ALMACENERO">Almacenero</option><option value="CONTADOR">Contador</option><option value="CONSULTA">Consulta</option>
        </select>}</label>
        <div className="modal-actions"><button type="button" className="btn secondary" onClick={() => setModal(null)}>Cancelar</button>
          <button className="btn primary" disabled={guardar.isPending}>{guardar.isPending ? 'Guardando…' : 'Guardar'}</button></div>
      </form>
    </FormModal>}
    {eliminar && <ConfirmDialog title="Eliminar usuario" cancelar={() => setEliminar(null)}
      confirmar={() => borrar.mutate(eliminar.id)}>Se eliminará el acceso de {eliminar.nombre}.</ConfirmDialog>}
    {resetTarget && <FormModal title={`Restablecer contraseña: ${resetTarget.nombre}`} onClose={() => setResetTarget(null)}>
      <form className="form-grid" onSubmit={(event) => void passwordForm.handleSubmit((value) =>
        restablecer.mutate({ id: resetTarget.id, password: value.password }))(event)}>
        <label>Nueva contraseña<input type="password" autoComplete="new-password" {...passwordForm.register('password')} />
          {passwordForm.formState.errors.password && <small role="alert">{passwordForm.formState.errors.password.message}</small>}</label>
        <div className="modal-actions"><button type="button" className="btn secondary" onClick={() => setResetTarget(null)}>Cancelar</button>
          <button className="btn primary" disabled={restablecer.isPending}>{restablecer.isPending ? 'Guardando…' : 'Restablecer'}</button></div>
      </form>
    </FormModal>}
  </>
}
