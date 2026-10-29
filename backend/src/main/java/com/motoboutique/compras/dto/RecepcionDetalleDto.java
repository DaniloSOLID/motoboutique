package com.motoboutique.compras.dto;

import java.math.BigDecimal;

public record RecepcionDetalleDto(Long detalleOrdenId, Long productoId, String productoCodigo,
                                  String productoNombre, BigDecimal cantidad) {
}
