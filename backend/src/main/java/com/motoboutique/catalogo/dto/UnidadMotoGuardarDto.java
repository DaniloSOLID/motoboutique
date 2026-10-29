package com.motoboutique.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnidadMotoGuardarDto(@NotNull Long modeloMotoId, @NotBlank @Size(max=40) String chasis,
                                   @NotBlank @Size(max=40) String motor, @Size(max=40) String color,
                                   String estado, String observaciones) {}
