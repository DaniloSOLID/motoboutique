package com.motoboutique.catalogo.dto;

import java.math.BigDecimal;

public record StockProductoDto(Long almacenId, String almacen, BigDecimal cantidad, BigDecimal reservado) {}
