package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.AjusteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long>,
        JpaSpecificationExecutor<AjusteInventario> {
    long countByEstadoAndEliminadoFalse(String estado);
}
