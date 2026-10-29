package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.ConteoFisico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConteoFisicoRepository extends JpaRepository<ConteoFisico, Long>,
        JpaSpecificationExecutor<ConteoFisico> {
}
