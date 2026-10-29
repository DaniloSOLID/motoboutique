package com.motoboutique.inventario.dto;

import com.motoboutique.inventario.domain.MotivoSalida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MovimientoSolicitudDto(@NotNull Long productoId, @NotNull Long almacenId,
                                     @NotNull @DecimalMin(value = "0.001")
                                     @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
                                     @DecimalMin(value = "0.00")
                                     @Digits(integer = 12, fraction = 2) BigDecimal costoUnitario,
                                     @Size(max = 80) String documentoReferencia,
                                     MotivoSalida motivo, @Size(max = 255) String observaciones) {
}
