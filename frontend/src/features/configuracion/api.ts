import { administracionApi } from '../../api/administracion'

export const configuracionApi = {
  empresa: administracionApi.configuracion,
  almacenes: administracionApi.almacenes,
  guardar: administracionApi.actualizarConfiguracion,
}
