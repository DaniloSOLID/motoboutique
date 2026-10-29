package com.motoboutique.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ModeloMotoGuardarDto(@NotNull Long marcaId, @NotBlank @Size(max=120) String nombre,
                                   @Positive Integer anioInicio, @Positive Integer anioFin,
                                   @Positive Integer cilindradaCc,
                                   @Size(max=40) String tipo) {}
