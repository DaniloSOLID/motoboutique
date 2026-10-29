package com.motoboutique.catalogo.dto;

public record ModeloMotoDto(Long id, Long marcaId, String marca, String nombre,
                            Integer anioInicio, Integer anioFin, Integer cilindradaCc, String tipo) {}
