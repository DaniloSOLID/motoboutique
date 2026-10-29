package com.motoboutique.inventario.domain;

import java.math.BigDecimal;

public enum EstadoStock {
    OK,
    BAJO,
    AGOTADO;

    public static EstadoStock de(BigDecimal cantidadTotal, BigDecimal stockMinimo) {
        if (cantidadTotal.signum() == 0) {
            return AGOTADO;
        }
        return cantidadTotal.compareTo(stockMinimo) <= 0 ? BAJO : OK;
    }
}
