package com.motoboutique.catalogo.dto;

import java.time.Instant;

public record ImportacionDto(Long id, String archivo, Instant fecha, int filasValidas,
                             int errores, String estado, Long usuarioId) {
}
