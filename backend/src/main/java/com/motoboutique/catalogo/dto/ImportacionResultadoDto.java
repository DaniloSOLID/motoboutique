package com.motoboutique.catalogo.dto;

import java.util.List;
import java.util.Map;

public record ImportacionResultadoDto(String archivo, List<String> columnas, List<Map<String, String>> vistaPrevia,
                                      List<Map<String, String>> filasValidas, List<ImportacionErrorDto> errores,
                                      Long importacionId) {}
