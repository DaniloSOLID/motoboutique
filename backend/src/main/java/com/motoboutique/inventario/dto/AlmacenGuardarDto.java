package com.motoboutique.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlmacenGuardarDto(@NotBlank @Size(max = 30) String codigo,
                               @NotBlank @Size(max = 120) String nombre,
                               @Size(max = 255) String direccion,
                               Boolean activo) {
}
