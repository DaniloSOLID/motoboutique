package com.motoboutique.catalogo.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ProductoDto(
        Long id, Long categoriaId, String categoria, Long marcaId, String marca,
        String codigo, String nombre, String descripcion, BigDecimal precioCompra,
        BigDecimal precioVenta, BigDecimal stockMinimo, String unidad, boolean activo,
        String imagenUrl, Map<String, Object> especificaciones, Set<Long> compatibilidadIds,
        List<ModeloMotoDto> compatibilidades, BigDecimal stockTotal,
        List<StockProductoDto> stockPorAlmacen) {}
