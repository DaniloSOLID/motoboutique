package com.motoboutique.seguridad.repository;

import com.motoboutique.seguridad.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lo usa el módulo seguridad. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Carga roles y permisos en una sola consulta: es lo que necesita el inicio de sesión. */
    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    Optional<Usuario> findByEmailIgnoreCaseAndEliminadoFalse(String email);

    /** Incluye eliminados a propósito: el índice único de correo los cuenta también. */
    boolean existsByEmailIgnoreCase(String email);
}
