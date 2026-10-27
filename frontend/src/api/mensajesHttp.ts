// Textos de error en español por código HTTP. Reflejan shared.exception.MensajesHttp del backend
// y se usan cuando la respuesta no trae título o detalle.

export function tituloHttp(codigo: number): string {
  switch (codigo) {
    case 400:
      return 'Solicitud no válida'
    case 401:
      return 'Sesión requerida'
    case 403:
      return 'Acceso denegado'
    case 404:
      return 'Página no encontrada'
    case 405:
      return 'Método no permitido'
    case 409:
      return 'Conflicto al guardar'
    case 415:
      return 'Formato no soportado'
    case 422:
      return 'No se pudo completar la operación'
    default:
      return codigo >= 500 ? 'Error interno del sistema' : 'No se pudo procesar la solicitud'
  }
}

export function detalleHttp(codigo: number): string {
  switch (codigo) {
    case 400:
      return 'La solicitud contiene datos no válidos.'
    case 401:
      return 'Inicia sesión para continuar.'
    case 403:
      return 'No tienes permiso para realizar esta acción.'
    case 404:
      return 'La página o el registro que buscas no existe o fue movido.'
    case 405:
      return 'Esta operación no está permitida en esta dirección.'
    case 409:
      return 'El registro fue modificado por otro usuario o ya existe. Recarga la página e inténtalo de nuevo.'
    case 415:
      return 'El formato de los datos enviados no es compatible.'
    default:
      return codigo >= 500
        ? 'Ocurrió un problema inesperado. Si se repite, comunica la referencia al administrador.'
        : 'No se pudo procesar la solicitud.'
  }
}
