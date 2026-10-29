package com.motoboutique.compras.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrdenCompraDetalleGuardarDto(
        @NotNull Long productoId,
        @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal cantidad,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal costoUnitario) {
}
