package com.motoboutique.inventario.domain;

import java.util.Set;

public enum TipoMovimiento {
    ENTRADA,
    SALIDA,
    TRANSFERENCIA_SALIDA,
    TRANSFERENCIA_ENTRADA,
    AJUSTE_POSITIVO,
    AJUSTE_NEGATIVO,
    RECEPCION;

    private static final Set<TipoMovimiento> ENTRADAS =
            Set.of(ENTRADA, TRANSFERENCIA_ENTRADA, AJUSTE_POSITIVO, RECEPCION);

    public boolean esEntrada() {
        return ENTRADAS.contains(this);
    }
}
