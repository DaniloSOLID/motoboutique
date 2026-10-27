package com.motoboutique.seguridad.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Permiso para mostrar en pantalla. */
@Getter
@Setter
@NoArgsConstructor
public class PermisoDto {

    private Long id;
    private String codigo;
    private String descripcion;
    private String modulo;
}
