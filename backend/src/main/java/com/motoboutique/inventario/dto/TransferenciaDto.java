package com.motoboutique.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransferenciaDto(Long id, String numero, String estado, Instant fecha,
                               Long almacenOrigenId, String almacenOrigen,
                               Long almacenDestinoId, String almacenDestino,
                               String observaciones, List<Detalle> detalles) {
    public record Detalle(Long productoId, String codigo, String producto, BigDecimal cantidad) {
    }
}
