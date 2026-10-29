package com.motoboutique.inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AjusteInventarioDto(@NotNull Long productoId, @NotNull Long almacenId,
                                  @NotNull @Digits(integer = 11, fraction = 3) BigDecimal cantidadConSigno,
                                  @DecimalMin(value = "0.00")
                                  @Digits(integer = 12, fraction = 2) BigDecimal costoUnitario,
                                  @NotNull @Size(min = 1, max = 255) String motivo,
                                  @Size(max = 80) String documentoReferencia) {
}
