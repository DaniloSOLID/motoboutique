package com.motoboutique.catalogo.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record ImportacionMapeoDto(@NotEmpty Map<String, String> columnas) {
}
