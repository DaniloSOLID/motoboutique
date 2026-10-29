package com.motoboutique.reportes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardDto(String rol, Map<String, Object> kpis, List<Serie> serie,
                           List<Critico> criticos, List<Actividad> actividad) {
    public record Serie(LocalDate periodo, BigDecimal entradas, BigDecimal salidas,
                        BigDecimal compras) {
    }

    public record Critico(Long productoId, String codigo, String producto, String categoria,
                          BigDecimal cantidad, BigDecimal minimo, String estado) {
    }

    public record Actividad(String actor, String accion, String entidad, String entidadId,
                            Instant fecha) {
    }
}
