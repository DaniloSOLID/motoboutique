package com.motobutique.shared.exception;

/**
 * Se lanza cuando un registro solicitado no existe (producto, venta, usuario...).
 * Se traduce en una respuesta 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /** Ejemplo: RecursoNoEncontradoException.de("Producto", 15) -> "No se encontró Producto con id 15". */
    public static RecursoNoEncontradoException de(String recurso, Object id) {
        return new RecursoNoEncontradoException("No se encontró " + recurso + " con id " + id);
    }
}
