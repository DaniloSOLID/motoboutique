package com.motoboutique.catalogo.repository;

import com.motoboutique.catalogo.domain.UnidadMoto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UnidadMotoRepository extends JpaRepository<UnidadMoto, Long>, JpaSpecificationExecutor<UnidadMoto> {
    Optional<UnidadMoto> findByIdAndEliminadoFalse(Long id);
    boolean existsByChasisIgnoreCase(String chasis);
    boolean existsByMotorIgnoreCase(String motor);
    boolean existsByChasisIgnoreCaseAndIdNot(String chasis, Long id);
    boolean existsByMotorIgnoreCaseAndIdNot(String motor, Long id);
}
