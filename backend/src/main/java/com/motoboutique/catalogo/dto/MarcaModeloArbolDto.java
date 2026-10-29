package com.motoboutique.catalogo.dto;

import java.util.List;

public record MarcaModeloArbolDto(Long id, String nombre, List<ModeloMotoDto> modelos) {}
