import { usuariosApi } from '../../api/inventario'
import { administracionApi } from '../../api/administracion'

export const seguridadFeatureApi = {
  usuarios: usuariosApi.listar,
  roles: administracionApi.roles,
  permisos: administracionApi.permisos,
  actualizarPermisos: administracionApi.actualizarPermisos,
}
