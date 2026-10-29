package com.motoboutique.catalogo.dto;
import jakarta.validation.constraints.*;
public record CategoriaGuardarDto(@NotBlank @Size(max=100) String nombre, @Size(max=255) String descripcion, Boolean activo) {}
