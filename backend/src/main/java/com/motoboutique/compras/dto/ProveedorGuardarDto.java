package com.motoboutique.compras.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorGuardarDto(
        @NotBlank @Size(max = 180) String razonSocial,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "El RUC debe tener exactamente 11 dígitos.")
        String ruc,
        @Size(max = 20) String telefono,
        @Email @Size(max = 150) String email,
        @Size(max = 255) String direccion,
        Boolean activo) {
}
