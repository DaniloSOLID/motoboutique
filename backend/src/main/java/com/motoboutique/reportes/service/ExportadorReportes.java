package com.motoboutique.reportes.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.motoboutique.reportes.dto.ReporteDto;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExportadorReportes {
    public byte[] exportar(ReporteDto reporte, String formato) {
        return switch (formato.toLowerCase(java.util.Locale.ROOT)) {
            case "csv" -> csv(reporte);
            case "xlsx" -> xlsx(reporte);
            case "pdf" -> pdf(reporte);
            default -> throw new IllegalArgumentException("Formato de exportación no soportado.");
        };
    }

    private byte[] csv(ReporteDto reporte) {
        StringBuilder contenido = new StringBuilder("\uFEFF");
        contenido.append(csv(reporte.razonSocial())).append("\r\n")
                .append(csv(reporte.ruc())).append("\r\n")
                .append(csv("Generado: " + reporte.generadoEn())).append("\r\n");
        appendCsvRow(contenido, reporte.columnas());
        reporte.datos().getContenido().forEach(row -> appendCsvRow(contenido, row));
        reporte.totales().forEach((nombre, valor) ->
                contenido.append(csv(nombre)).append(',').append(csv(valor)).append("\r\n"));
        return contenido.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] xlsx(ReporteDto reporte) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet(reporte.tipo());
            int numeroFila = 0;
            numeroFila = fila(hoja.createRow(numeroFila++), reporte.razonSocial());
            numeroFila = fila(hoja.createRow(numeroFila++), reporte.ruc());
            numeroFila = fila(hoja.createRow(numeroFila++), "Generado: " + reporte.generadoEn());
            Row encabezado = hoja.createRow(numeroFila++);
            for (int i = 0; i < reporte.columnas().size(); i++) {
                encabezado.createCell(i).setCellValue(reporte.columnas().get(i));
            }
            for (var valores : reporte.datos().getContenido()) {
                Row row = hoja.createRow(numeroFila++);
                for (int i = 0; i < valores.size(); i++) {
                    row.createCell(i).setCellValue(valores.get(i));
                }
            }
            for (var total : reporte.totales().entrySet()) {
                Row row = hoja.createRow(numeroFila++);
                row.createCell(0).setCellValue(total.getKey());
                row.createCell(1).setCellValue(total.getValue());
            }
            for (int i = 0; i < reporte.columnas().size(); i++) {
                hoja.autoSizeColumn(i);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo XLSX.", e);
        }
    }

    private byte[] pdf(ReporteDto reporte) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document();
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph(reporte.razonSocial() == null ? "" : reporte.razonSocial()));
            documento.add(new Paragraph("RUC: " + (reporte.ruc() == null ? "" : reporte.ruc())));
            documento.add(new Paragraph("Generado: " + reporte.generadoEn()));
            PdfPTable tabla = new PdfPTable(Math.max(1, reporte.columnas().size()));
            for (String columna : reporte.columnas()) {
                tabla.addCell(new Phrase(columna));
            }
            for (var row : reporte.datos().getContenido()) {
                for (String valor : row) {
                    tabla.addCell(new Phrase(valor));
                }
            }
            documento.add(tabla);
            for (var total : reporte.totales().entrySet()) {
                documento.add(new Paragraph(total.getKey() + ": " + total.getValue()));
            }
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo PDF.", e);
        }
    }

    private int fila(Row row, String valor) {
        row.createCell(0).setCellValue(valor == null ? "" : valor);
        return row.getRowNum() + 1;
    }

    private void appendCsvRow(StringBuilder csv, java.util.List<String> values) {
        csv.append(values.stream().map(this::csv).collect(java.util.stream.Collectors.joining(",")))
                .append("\r\n");
    }

    private String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }
}
