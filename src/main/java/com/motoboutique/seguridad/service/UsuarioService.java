package com.motoboutique.seguridad.service;

import com.motoboutique.seguridad.dto.UsuarioCrearDto;
import com.motoboutique.seguridad.dto.UsuarioDto;
import java.util.Optional;

/** Interfaz pública del módulo seguridad para gestionar usuarios. */
public interface UsuarioService {

    /**
     * Crea un usuario con la contraseña cifrada (BCrypt) y el correo normalizado.
     *
     * @throws com.motoboutique.shared.exception.NegocioException si el correo ya existe, la
     *         contraseña no cumple las reglas o algún rol no existe
     */
    UsuarioDto crear(UsuarioCrearDto datos);

    /** @throws com.motoboutique.shared.exception.RecursoNoEncontradoException si no existe */
    UsuarioDto obtenerPorId(Long id);

    Optional<UsuarioDto> buscarPorEmail(String email);

    boolean existeEmail(String email);

    /**
     * Reemplaza la contraseña del usuario. Lo usa el restablecimiento por token.
     *
     * @throws com.motoboutique.shared.exception.NegocioException si no cumple las reglas
     */
    void cambiarPassword(Long usuarioId, String passwordNueva);
}
