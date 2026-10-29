package com.motoboutique.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoDto(Long id, String numero, Instant fecha, String tipo,
                            Long productoId, String productoCodigo, String productoNombre,
                            Long almacenId, String almacen, BigDecimal cantidad,
                            BigDecimal costoUnitario, BigDecimal saldoCantidad,
                            BigDecimal costoPromedio, String referencia, String motivo,
                            String responsable) {
}
