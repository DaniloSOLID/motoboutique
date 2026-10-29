package com.motoboutique.compras.dto;

import java.time.Instant;
import java.util.List;

public record RecepcionDto(Long id, String numero, Long ordenId, String orden,
                           String proveedor, Instant fecha, String observaciones,
                           String responsable, List<RecepcionDetalleDto> detalles) {
}
