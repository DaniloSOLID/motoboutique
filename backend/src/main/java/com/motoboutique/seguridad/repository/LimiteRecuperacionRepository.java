package com.motoboutique.seguridad.repository;

import com.motoboutique.seguridad.domain.LimiteRecuperacionPassword;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LimiteRecuperacionRepository extends JpaRepository<LimiteRecuperacionPassword, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LimiteRecuperacionPassword l where l.clave = :clave")
    Optional<LimiteRecuperacionPassword> bloquearPorClave(@Param("clave") String clave);
}
