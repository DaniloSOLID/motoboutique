package com.motoboutique.compras.dto;

public record ProveedorDto(Long id, String codigo, String razonSocial, String ruc,
                           String telefono, String email, String direccion, boolean activo) {
}
