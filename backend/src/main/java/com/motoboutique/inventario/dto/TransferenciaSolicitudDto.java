package com.motoboutique.inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TransferenciaSolicitudDto(@NotNull Long almacenOrigenId,
                                        @NotNull Long almacenDestinoId,
                                        @NotEmpty List<@Valid TransferenciaDetalleSolicitudDto> detalles,
                                        @Size(max = 2000) String observaciones) {
}
