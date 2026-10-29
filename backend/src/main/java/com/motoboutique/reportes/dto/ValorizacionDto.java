package com.motoboutique.reportes.dto;

import com.motoboutique.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ValorizacionDto(LocalDate fechaCorte, BigDecimal cantidadTotal,
                              BigDecimal valorTotal, PageResponse<Producto> productos,
                              List<Categoria> categorias) {
    public record Producto(Long productoId, String codigo, String producto, Long categoriaId,
                           String categoria, BigDecimal cantidad, BigDecimal costoPromedio,
                           BigDecimal valor) {
    }

    public record Categoria(Long categoriaId, String categoria, BigDecimal cantidad,
                            BigDecimal valor) {
    }
}
