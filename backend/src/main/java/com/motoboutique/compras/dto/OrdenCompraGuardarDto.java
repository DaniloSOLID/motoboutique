package com.motoboutique.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrdenCompraGuardarDto(
        @NotNull Long proveedorId,
        Long almacenId,
        @NotEmpty List<@Valid OrdenCompraDetalleGuardarDto> detalles,
        @Size(max = 2000) String observaciones) {
}
