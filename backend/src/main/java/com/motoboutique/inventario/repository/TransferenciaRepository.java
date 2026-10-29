package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.Transferencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface TransferenciaRepository extends JpaRepository<Transferencia, Long>,
        JpaSpecificationExecutor<Transferencia> {
    long countByEstadoAndEliminadoFalse(String estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Transferencia t where t.id = :id and t.eliminado = false")
    Optional<Transferencia> bloquearActiva(@org.springframework.data.repository.query.Param("id") Long id);
}
