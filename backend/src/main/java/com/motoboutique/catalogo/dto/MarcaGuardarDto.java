package com.motoboutique.catalogo.dto;
import jakarta.validation.constraints.*;
public record MarcaGuardarDto(@NotBlank @Size(max=100) String nombre,@Size(max=255) String descripcion,Boolean activo) {}
