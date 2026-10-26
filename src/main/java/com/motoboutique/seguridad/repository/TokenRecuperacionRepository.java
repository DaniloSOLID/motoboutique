package com.motoboutique.seguridad.repository;

import com.motoboutique.seguridad.domain.TokenRecuperacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Solo lo usa el módulo seguridad. */
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);
}
