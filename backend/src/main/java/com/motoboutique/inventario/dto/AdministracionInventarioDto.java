package com.motoboutique.inventario.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AdministracionInventarioDto {

    private AdministracionInventarioDto() {}

    public record ConteoLineaDto(Long id, Long productoId, String codigo, String producto,
            BigDecimal stockSistema, BigDecimal stockContado, BigDecimal diferencia,
            boolean ajustePendiente) {}

    public record ConteoDto(Long id, String numero, Long almacenId, String almacen, String estado,
            Instant fechaApertura, Instant fechaCierre, List<ConteoLineaDto> detalles) {}

    public record AjusteSolicitud(Long productoId, Long almacenId, BigDecimal cantidad,
            String motivo) {}

    public record AjusteDto(Long id, String numero, Long productoId, String codigoProducto,
            String producto, Long almacenId, String almacen, BigDecimal cantidad, String motivo,
            String origen, String estado, Long solicitanteId, String solicitante, String aprobador,
            String comentario, Instant fechaSolicitud, Instant fechaResolucion, Long movimientoId,
            BigDecimal stockActual, BigDecimal stockResultante) {}
}
