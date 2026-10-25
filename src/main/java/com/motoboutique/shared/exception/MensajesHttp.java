package com.motoboutique.shared.exception;

import java.util.Locale;
import java.util.UUID;

/**
 * Textos de error en español según el código HTTP. Se usa para no exponer mensajes
 * técnicos de los frameworks al usuario.
 */
final class MensajesHttp {

    private MensajesHttp() {
    }

    static String titulo(int codigo) {
        return switch (codigo) {
            case 400 -> "Solicitud no válida";
            case 403 -> "Acceso denegado";
            case 404 -> "Página no encontrada";
            case 405 -> "Método no permitido";
            case 409 -> "Conflicto al guardar";
            case 415 -> "Formato no soportado";
            case 422 -> "No se pudo completar la operación";
            default -> codigo >= 500 ? "Error interno del sistema" : "No se pudo procesar la solicitud";
        };
    }

    static String detalle(int codigo) {
        return switch (codigo) {
            case 400 -> "La solicitud contiene datos no válidos.";
            case 403 -> "No tienes permiso para realizar esta acción.";
            case 404 -> "La página o el registro que buscas no existe o fue movido.";
            case 405 -> "Esta operación no está permitida en esta dirección.";
            case 409 -> "El registro fue modificado por otro usuario o ya existe. Recarga la página e inténtalo de nuevo.";
            case 415 -> "El formato de los datos enviados no es compatible.";
            default -> codigo >= 500
                    ? "Ocurrió un problema inesperado. Si se repite, comunica la referencia al administrador."
                    : "No se pudo procesar la solicitud.";
        };
    }

    /** Código corto para que el usuario lo reporte y se pueda buscar en el log. */
    static String nuevaReferencia() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
