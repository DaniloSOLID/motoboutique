package com.motoboutique.compras.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrdenCompraDto(Long id, String numero, String estado, Instant fecha,
                             Long proveedorId, String proveedor, String rucProveedor,
                             Long almacenId, String almacen, String observaciones,
                             BigDecimal subtotal, BigDecimal igv, BigDecimal total,
                             List<OrdenCompraDetalleDto> detalles) {
}
