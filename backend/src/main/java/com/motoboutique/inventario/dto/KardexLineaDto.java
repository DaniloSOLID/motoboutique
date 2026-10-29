package com.motoboutique.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record KardexLineaDto(Long movimientoId, Instant fecha, String tipo, String numero,
                             BigDecimal entrada, BigDecimal salida, BigDecimal saldo,
                             BigDecimal costoUnitario, BigDecimal costoPromedio,
                             String documentoReferencia, String motivo) {
}
