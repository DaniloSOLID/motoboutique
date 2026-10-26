package com.motoboutique.seguridad.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuario para mostrar en pantalla. NO incluye la contraseña, ni siquiera su hash.
 * Los roles se entregan como lista de códigos ordenada.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioDto {

    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private boolean activo;
    private List<String> roles = new ArrayList<>();
}
