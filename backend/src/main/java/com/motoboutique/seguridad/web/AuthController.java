package com.motoboutique.seguridad.web;

import com.motoboutique.seguridad.dto.UsuarioAutenticado;
import com.motoboutique.seguridad.dto.UsuarioSesionDto;
import com.motoboutique.seguridad.service.RecuperacionPasswordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sesión actual. El inicio y el cierre de sesión los resuelve Spring Security
 * (POST /api/auth/login y POST /api/auth/logout, ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {

    private final RecuperacionPasswordService recuperacionService;

    /** Devuelve el usuario con sesión iniciada; sin sesión, Spring Security responde 401. */
    @GetMapping("/me")
    UsuarioSesionDto sesionActual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioSesionDto.de(usuario);
    }

    @PostMapping("/recuperar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RespuestaRecuperacion recuperar(@Valid @RequestBody RecuperacionSolicitud solicitud) {
        recuperacionService.solicitar(solicitud.email());
        return new RespuestaRecuperacion(
                "Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.");
    }

    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void restablecer(@Valid @RequestBody RestablecerSolicitud solicitud) {
        recuperacionService.restablecer(solicitud.token(), solicitud.password());
    }

    record RecuperacionSolicitud(@NotBlank @Email @Size(max = 150) String email) {}
    record RestablecerSolicitud(@NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String password) {}
    record RespuestaRecuperacion(String mensaje) {}
}
