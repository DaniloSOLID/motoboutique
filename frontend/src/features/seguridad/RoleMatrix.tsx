import { toast } from 'sonner'
import { ApiError } from '../../api/client'
import { useRolesYPermisos } from './hooks'

export function RoleMatrix() {
  const { roles, permisos, actualizar } = useRolesYPermisos()
  if (roles.isPending || permisos.isPending) return <p role="status">Cargando roles y permisos…</p>
  if (roles.isError || permisos.isError) {
    const error = roles.error ?? permisos.error
    return <p role="alert" className="login-error">
      {error instanceof ApiError ? error.detalle : 'No se pudieron cargar los roles y permisos.'}
    </p>
  }
  const listaRoles = roles.data ?? []
  const listaPermisos = permisos.data ?? []
  const modulos = [...new Set(listaPermisos.map((permiso) => permiso.modulo))]

  const alternar = async (id: number, asignados: string[], permiso: string) => {
    const siguientes = asignados.includes(permiso)
      ? asignados.filter((codigo) => codigo !== permiso)
      : [...asignados, permiso]
    try {
      await actualizar.mutateAsync({ id, permisos: siguientes, codigo: permiso })
      toast.success('Permisos actualizados.')
    } catch (error) {
      toast.error(error instanceof ApiError ? error.detalle : 'No se pudieron guardar los permisos.')
    }
  }

  return <div className="matrix card">
    <div className="matrix-head"><b>Matriz de permisos</b><span>Los cambios se guardan inmediatamente; las sesiones activas podrían requerir volver a iniciar sesión.</span></div>
    {listaPermisos.length === 0 || listaRoles.length === 0
      ? <p className="empty-state">No hay roles o permisos disponibles.</p>
      : <div className="scroll"><table>
        <thead><tr><th>Módulo y permiso</th>{listaRoles.map((rol) => <th key={rol.id}>{rol.nombre}</th>)}</tr></thead>
        <tbody>{modulos.flatMap((modulo) => listaPermisos.filter((permiso) => permiso.modulo === modulo)
          .map((permiso) => <tr key={permiso.id}><td><b>{modulo}</b><small>{permiso.descripcion}</small></td>
            {listaRoles.map((rol) => {
              const permitido = rol.permisos.includes(permiso.codigo)
              const guardando = actualizar.isPending && actualizar.variables?.id === rol.id
                && actualizar.variables.codigo === permiso.codigo
              return <td key={rol.id}><input type="checkbox"
                aria-label={`${permiso.codigo} para ${rol.nombre}`} checked={permitido}
                disabled={actualizar.isPending || rol.codigo === 'ADMIN'}
                onChange={() => void alternar(rol.id, rol.permisos, permiso.codigo)} />
                {guardando && <small role="status">Guardando…</small>}</td>
            })}</tr>))}
        </tbody>
      </table></div>}
  </div>
}
