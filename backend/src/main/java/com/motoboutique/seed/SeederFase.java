package com.motoboutique.seed;

/**
 * Una fase de datos iniciales. Cada fase del proyecto agrega su propia clase (seguridad,
 * catálogo, inventario...) y {@link DataSeeder} las ejecuta ordenadas con @Order.
 *
 * <p>Toda implementación debe ser idempotente: se ejecuta en cada arranque, así que tiene que
 * crear solo lo que falta y nunca pisar lo que el usuario ya configuró.</p>
 */
public interface SeederFase {

    void ejecutar();
}
