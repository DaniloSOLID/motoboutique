package com.motoboutique.catalogo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public record ProductoGuardarDto(
        @NotNull Long categoriaId,
        Long marcaId,
        @NotBlank @Size(max = 60) String codigo,
        @NotBlank @Size(max = 180) String nombre,
        @Size(max = 5000) String descripcion,
        @NotNull @DecimalMin("0") BigDecimal precioCompra,
        @NotNull @DecimalMin("0") BigDecimal precioVenta,
        @NotNull @DecimalMin("0") BigDecimal stockMinimo,
        @NotBlank @Size(max = 20) String unidad,
        Boolean activo,
        @Size(max = 2048) String imagenUrl,
        Map<String, Object> especificaciones,
        Set<Long> compatibilidadIds) {
    public ProductoGuardarDto(Long categoriaId, Long marcaId, String codigo, String nombre,
                              String descripcion, BigDecimal precioCompra, BigDecimal precioVenta,
                              BigDecimal stockMinimo, String unidad, Boolean activo,
                              Set<Long> compatibilidadIds) {
        this(categoriaId, marcaId, codigo, nombre, descripcion, precioCompra, precioVenta,
                stockMinimo, unidad, activo, null, Map.of(), compatibilidadIds);
    }
}

