package com.motoboutique.seguridad.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Rol para mostrar en pantalla. Los permisos se entregan como lista de códigos ordenada. */
@Getter
@Setter
@NoArgsConstructor
public class RolDto {

    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private boolean sistema;
    private List<String> permisos = new ArrayList<>();
}
