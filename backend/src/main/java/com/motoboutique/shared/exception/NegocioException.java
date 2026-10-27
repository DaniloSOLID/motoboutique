package com.motoboutique.shared.exception;

/**
 * Se lanza cuando se incumple una regla de negocio (stock insuficiente, transición de estado
 * inválida, producto con movimientos que no se puede eliminar, etc.).
 *
 * <p>El mensaje se muestra tal cual al usuario, así que debe estar redactado en español y sin
 * detalles técnicos. Al ser RuntimeException, revierte la transacción.</p>
 */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }

    public NegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
