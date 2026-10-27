package com.motoboutique.seguridad.dto;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;

/**
 * Datos de la sesión actual que necesita el frontend: quién es el usuario, sus roles
 * (sin el prefijo "ROLE_") y sus permisos. Nunca incluye la contraseña.
 */
public record UsuarioSesionDto(Long id, String nombre, String email,
                               List<String> roles, List<String> permisos) {

    private static final String PREFIJO_ROL = "ROLE_";

    public static UsuarioSesionDto de(UsuarioAutenticado usuario) {
        List<String> autoridades = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        List<String> roles = autoridades.stream()
                .filter(autoridad -> autoridad.startsWith(PREFIJO_ROL))
                .map(autoridad -> autoridad.substring(PREFIJO_ROL.length()))
                .sorted()
                .toList();
        List<String> permisos = autoridades.stream()
                .filter(autoridad -> !autoridad.startsWith(PREFIJO_ROL))
                .sorted()
                .toList();
        return new UsuarioSesionDto(usuario.getId(), usuario.getNombre(), usuario.getEmail(), roles, permisos);
    }
}
