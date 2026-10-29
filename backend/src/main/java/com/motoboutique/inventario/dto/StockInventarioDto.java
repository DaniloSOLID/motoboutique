package com.motoboutique.inventario.dto;

import java.math.BigDecimal;

public record StockInventarioDto(Long productoId, String codigo, String nombre, String categoria,
                                 BigDecimal cantidadTotal, BigDecimal stockMinimo,
                                 String estado, Long almacenId, String almacen,
                                 BigDecimal cantidad, BigDecimal reservado) {
}
