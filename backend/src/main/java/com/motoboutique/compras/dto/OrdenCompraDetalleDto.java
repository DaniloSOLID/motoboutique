package com.motoboutique.compras.dto;

import java.math.BigDecimal;

public record OrdenCompraDetalleDto(Long id, Long productoId, String productoCodigo,
                                    String productoNombre, BigDecimal cantidad,
                                    BigDecimal cantidadRecibida, BigDecimal cantidadPendiente,
                                    BigDecimal costoUnitario, BigDecimal subtotal) {
}
