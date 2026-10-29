package com.motoboutique.compras.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecepcionGuardarDto(
        @NotNull Long ordenId,
        @NotEmpty List<@Valid RecepcionDetalleSolicitudDto> detalles,
        @Size(max = 2000) String observaciones) {
}
