package com.motoboutique.reportes.dto;

import com.motoboutique.shared.web.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReporteDto(String tipo, String razonSocial, String ruc, Instant generadoEn,
                         List<String> columnas, PageResponse<List<String>> datos,
                         Map<String, String> totales) {
}
