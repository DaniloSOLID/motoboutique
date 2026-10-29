package com.motoboutique.inventario.dto;

import java.math.BigDecimal;
import java.util.List;

public record KardexDto(Long productoId, String producto, Long almacenId, String almacen,
                        BigDecimal saldoInicial, List<KardexLineaDto> movimientos) {
}
