package com.motoboutique.reportes.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.motoboutique.reportes.dto.ReporteDto;
import com.motoboutique.shared.web.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExportadorReportesTest {
    private final ExportadorReportes exportador = new ExportadorReportes();
    private final ReporteDto reporte = new ReporteDto("valorizacion", "Empresa", "20123456789",
            Instant.parse("2026-10-28T15:00:00Z"), List.of("Producto"),
            PageResponse.vacia(20), Map.of("valorTotal", "0.00"));

    @Test
    void generaCsvXlsxYPdf() {
        assertTrue(new String(exportador.exportar(reporte, "csv"),
                java.nio.charset.StandardCharsets.UTF_8).startsWith("\uFEFF"));
        byte[] xlsx = exportador.exportar(reporte, "xlsx");
        assertTrue(xlsx[0] == 'P' && xlsx[1] == 'K');
        byte[] pdf = exportador.exportar(reporte, "pdf");
        assertTrue(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF"));
    }
}
