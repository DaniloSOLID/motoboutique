package com.motoboutique.seguridad.web;

import com.motoboutique.seguridad.dto.UsuarioAutenticado;
import com.motoboutique.seguridad.dto.UsuarioSesionDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sesión actual. El inicio y el cierre de sesión los resuelve Spring Security
 * (POST /api/auth/login y POST /api/auth/logout, ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    /** Devuelve el usuario con sesión iniciada; sin sesión, Spring Security responde 401. */
    @GetMapping("/me")
    UsuarioSesionDto sesionActual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioSesionDto.de(usuario);
    }
}
